package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.common.Utils;
import com.classpulse.dto.RollCallCreateRequest;
import com.classpulse.dto.RollCallResultRequest;
import com.classpulse.entity.Course;
import com.classpulse.entity.LeaveRequest;
import com.classpulse.entity.RollCallRecord;
import com.classpulse.entity.RollCallSession;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.mapper.LeaveRequestMapper;
import com.classpulse.mapper.RollCallRecordMapper;
import com.classpulse.mapper.RollCallSessionMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.vo.RollCallVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RollCallService {
    private final RollCallSessionMapper sessionMapper;
    private final RollCallRecordMapper recordMapper;
    private final SignRecordMapper signRecordMapper;
    private final SignTaskMapper taskMapper;
    private final StudentMapper studentMapper;
    private final LeaveRequestMapper leaveMapper;
    private final AccessService access;
    private final ConfigService configService;
    private final NotificationService notificationService;
    private final OperationLogService logService;
    private final SseHub sseHub;

    /* ===================== 创建与抽取 ===================== */

    @Transactional
    public RollCallVO create(RollCallCreateRequest request) {
        Course course = access.requireCourse(request.getCourseId());
        access.assertCourseManageable(course.getId());
        Long teacherId = SecurityUtils.isAdmin() ? course.getTeacherId() : SecurityUtils.teacherId();

        String scope = normalizeScope(request.getCandidateScope());
        List<Long> customIds = request.getCustomStudentIds() == null ? List.of() : request.getCustomStudentIds();
        if ("CUSTOM".equals(scope) && customIds.isEmpty()) {
            throw BizException.invalid("自定义抽取范围需要先勾选学生");
        }
        Long taskId = resolveTaskId(request.getTaskId(), course.getId(), scope);
        List<Long> basePool = candidateStudentIds(course.getId(), request.getClassId(), scope, taskId,
                customIds, Boolean.TRUE.equals(request.getExcludeLeave()));

        int drawCount = request.getDrawCount() == null ? 1 : request.getDrawCount();
        if (basePool.size() < drawCount) {
            throw BizException.invalid("候选学生不足，当前符合条件的仅 " + basePool.size() + " 人");
        }

        RollCallSession session = new RollCallSession();
        session.setCourseId(course.getId());
        session.setClassId(request.getClassId());
        session.setTeacherId(teacherId);
        session.setCandidateScope(scope);
        session.setDrawCount(drawCount);
        session.setAntiRepeatRule(normalizeAntiRepeat(request.getAntiRepeatRule()));
        session.setExcludeLeave(Boolean.FALSE.equals(request.getExcludeLeave()) ? 0 : 1);
        session.setTaskId(taskId);
        session.setCustomStudentIds(customIds.isEmpty() ? null
                : customIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        session.setResponseDeadlineSeconds(request.getResponseDeadlineSeconds() == null
                ? configService.getInt("rollcall.response.seconds", 30)
                : Math.max(10, Math.min(120, request.getResponseDeadlineSeconds())));
        session.setStatus("RUNNING");
        session.setSeed(Math.abs(new Random().nextLong() % 1_000_000_000L));
        session.setStartedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        drawInto(session, drawCount);

        access.courseStudents(course.getId()).forEach(student -> notificationService.send(student.getUserId(),
                "课堂随机点名", "《" + course.getName() + "》正在进行随机点名，被点到的同学请在 "
                        + session.getResponseDeadlineSeconds() + " 秒内响应。",
                "ROLL_CALL", session.getId()));
        logService.log("ROLL_CALL", "CREATE", session.getId(), "发起随机点名，抽取 " + drawCount + " 人");
        sseHub.broadcast("rollcall:session", Map.of("sessionId", session.getId(), "action", "CREATE"));
        return detail(session.getId());
    }

    @Transactional
    public RollCallVO drawAgain(Long sessionId, Integer count) {
        RollCallSession session = requireSession(sessionId);
        assertTeacherOwner(session);
        if (!"RUNNING".equals(session.getStatus())) {
            throw BizException.invalid("该点名会话已结束，无法继续抽取");
        }
        int drawCount = count == null ? session.getDrawCount() : Math.max(1, Math.min(10, count));
        drawInto(session, drawCount);
        sseHub.broadcast("rollcall:session", Map.of("sessionId", sessionId, "action", "DRAW_AGAIN"));
        return detail(sessionId);
    }

    private void drawInto(RollCallSession session, int drawCount) {
        List<RollCallRecord> existing = recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                .eq(RollCallRecord::getSessionId, session.getId()));
        Set<Long> drawnIds = existing.stream().map(RollCallRecord::getStudentId).collect(Collectors.toSet());
        int startOrder = existing.stream().mapToInt(RollCallRecord::getDrawOrder).max().orElse(0);

        List<Long> candidateIds = new ArrayList<>(candidateStudentIds(session.getCourseId(), session.getClassId(),
                session.getCandidateScope(), session.getTaskId(), parseIds(session.getCustomStudentIds()),
                session.getExcludeLeave() != null && session.getExcludeLeave() == 1));

        // 同一会话内绝不重复
        candidateIds.removeAll(drawnIds);

        // 防重复规则：从近期点名记录中排除
        Set<Long> recentExcluded = recentDrawnIds(session, candidateIds);
        List<Long> preferred = candidateIds.stream().filter(id -> !recentExcluded.contains(id)).toList();
        List<Long> fallback = candidateIds.stream().filter(recentExcluded::contains).toList();

        Map<Long, LocalDateTime> lastDrawn = lastDrawnTimes(session.getCourseId());
        Random random = new Random(session.getSeed() == null ? System.nanoTime()
                : session.getSeed() + startOrder * 131L + drawCount);
        List<Long> pool = new ArrayList<>();
        List<Long> fresh = preferred.stream().filter(id -> lastDrawn.get(id) == null).collect(Collectors.toList());
        List<Long> touched = preferred.stream().filter(id -> lastDrawn.get(id) != null)
                .sorted(Comparator.comparing(lastDrawn::get)).collect(Collectors.toList());
        Collections.shuffle(fresh, random);
        Collections.shuffle(touched, random);
        pool.addAll(fresh);
        pool.addAll(touched);
        if (pool.size() < drawCount) {
            List<Long> backfill = new ArrayList<>(fallback);
            backfill.sort(Comparator.comparing(id -> lastDrawn.getOrDefault(id, LocalDateTime.MIN)));
            pool.addAll(backfill);
        }
        if (pool.size() < drawCount) {
            throw BizException.invalid("可抽取学生不足，当前剩余 " + pool.size() + " 人");
        }

        List<Long> picked = pool.subList(0, drawCount);
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(picked).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        LocalDateTime now = LocalDateTime.now();
        int order = startOrder;
        for (Long studentId : picked) {
            order++;
            RollCallRecord record = new RollCallRecord();
            record.setSessionId(session.getId());
            record.setStudentId(studentId);
            record.setDrawOrder(order);
            record.setResult("PENDING");
            record.setDrawnAt(now);
            recordMapper.insert(record);

            Student student = studentMap.get(studentId);
            if (student != null && student.getUserId() != null) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("sessionId", session.getId());
                event.put("recordId", record.getId());
                event.put("studentId", studentId);
                event.put("studentName", student.getName());
                event.put("courseName", access.courseName(session.getCourseId()));
                event.put("deadlineSeconds", session.getResponseDeadlineSeconds());
                event.put("drawnAt", now);
                sseHub.send(student.getUserId(), "rollcall:drawn", event);
            }
        }

        Map<String, Object> stageEvent = new LinkedHashMap<>();
        stageEvent.put("sessionId", session.getId());
        stageEvent.put("students", picked.stream().map(id -> {
            Student s = studentMap.get(id);
            return Map.of("studentId", id,
                    "name", s == null ? "" : s.getName(),
                    "studentNo", s == null ? "" : s.getStudentNo());
        }).toList());
        sseHub.broadcast("rollcall:stage", stageEvent);
    }

    /* ===================== 查询 ===================== */

    public RollCallVO detail(Long sessionId) {
        RollCallSession session = requireSession(sessionId);
        var user = SecurityUtils.current();
        if ("STUDENT".equals(user.role())) {
            long count = recordMapper.selectCount(new LambdaQueryWrapper<RollCallRecord>()
                    .eq(RollCallRecord::getSessionId, sessionId)
                    .eq(RollCallRecord::getStudentId, user.relatedId()));
            if (count == 0) {
                throw BizException.forbidden("你不在本次点名名单中");
            }
        } else if (!"ADMIN".equals(user.role())) {
            assertTeacherOwner(session);
        }
        return toVO(session);
    }

    public List<Map<String, Object>> activeForStudent() {
        Long studentId = SecurityUtils.studentId();
        List<RollCallSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<RollCallSession>()
                .eq(RollCallSession::getStatus, "RUNNING").orderByDesc(RollCallSession::getStartedAt).last("limit 20"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RollCallSession session : sessions) {
            RollCallRecord record = recordMapper.selectOne(new LambdaQueryWrapper<RollCallRecord>()
                    .eq(RollCallRecord::getSessionId, session.getId())
                    .eq(RollCallRecord::getStudentId, studentId)
                    .eq(RollCallRecord::getResult, "PENDING")
                    .last("limit 1"));
            if (record == null) {
                continue;
            }
            LocalDateTime deadline = record.getDrawnAt().plusSeconds(session.getResponseDeadlineSeconds());
            if (deadline.isBefore(LocalDateTime.now())) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", session.getId());
            map.put("recordId", record.getId());
            map.put("courseName", access.courseName(session.getCourseId()));
            map.put("teacherName", access.teacherName(session.getTeacherId()));
            map.put("drawnAt", record.getDrawnAt());
            map.put("deadline", deadline);
            map.put("remainingSeconds", Duration.between(LocalDateTime.now(), deadline).getSeconds());
            map.put("performance", record.getPerformance());
            result.add(map);
        }
        return result;
    }

    /* ===================== 响应与结果 ===================== */

    @Transactional
    public Map<String, Object> respond(Long sessionId) {
        Long studentId = SecurityUtils.studentId();
        RollCallSession session = requireSession(sessionId);
        if (!"RUNNING".equals(session.getStatus())) {
            throw BizException.invalid("本次点名已结束");
        }
        RollCallRecord record = recordMapper.selectOne(new LambdaQueryWrapper<RollCallRecord>()
                .eq(RollCallRecord::getSessionId, sessionId)
                .eq(RollCallRecord::getStudentId, studentId)
                .last("limit 1"));
        if (record == null) {
            throw BizException.forbidden("你不在本次点名名单中");
        }
        if (!"PENDING".equals(record.getResult())) {
            throw BizException.conflict("你已处理过本次点名");
        }
        LocalDateTime deadline = record.getDrawnAt().plusSeconds(session.getResponseDeadlineSeconds());
        if (LocalDateTime.now().isAfter(deadline)) {
            record.setResult("NO_RESPONSE");
            recordMapper.updateById(record);
            sseHub.broadcast("rollcall:update", Map.of("sessionId", sessionId));
            throw BizException.invalid("响应已超时");
        }
        record.setResult("RESPONDED");
        record.setRespondedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        sseHub.broadcast("rollcall:update", Map.of("sessionId", sessionId, "recordId", record.getId()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("record", record);
        map.put("message", "已确认，请在课堂上准备回答");
        return map;
    }

    @Transactional
    public void markResult(Long sessionId, Long recordId, RollCallResultRequest request) {
        RollCallSession session = requireSession(sessionId);
        assertTeacherOwner(session);
        RollCallRecord record = recordMapper.selectById(recordId);
        if (record == null || !record.getSessionId().equals(sessionId)) {
            throw BizException.notFound("点名记录不存在");
        }
        if (!List.of("RESPONDED", "NO_RESPONSE", "SKIPPED", "CANCELED").contains(request.getResult())) {
            throw BizException.invalid("不支持的点名结果");
        }
        if (request.getPerformance() != null
                && !List.of("EXCELLENT", "ACTIVE", "NORMAL", "FOLLOW_UP").contains(request.getPerformance())) {
            throw BizException.invalid("不支持的课堂表现评价");
        }
        record.setResult(request.getResult());
        record.setPerformance(request.getPerformance());
        record.setRemark(request.getRemark());
        if ("RESPONDED".equals(request.getResult()) && record.getRespondedAt() == null) {
            record.setRespondedAt(LocalDateTime.now());
        }
        recordMapper.updateById(record);
        Student student = studentMapper.selectById(record.getStudentId());
        if (student != null && student.getUserId() != null) {
            sseHub.send(student.getUserId(), "rollcall:result", Map.of("sessionId", sessionId,
                    "recordId", recordId, "result", record.getResult()));
        }
        sseHub.broadcast("rollcall:update", Map.of("sessionId", sessionId));
        logService.log("ROLL_CALL", "MARK_RESULT", sessionId, "标记点名结果：" + request.getResult());
    }

    @Transactional
    public void finish(Long sessionId) {
        RollCallSession session = requireSession(sessionId);
        assertTeacherOwner(session);
        if ("FINISHED".equals(session.getStatus())) {
            return;
        }
        List<RollCallRecord> pending = recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                .eq(RollCallRecord::getSessionId, sessionId)
                .eq(RollCallRecord::getResult, "PENDING"));
        for (RollCallRecord record : pending) {
            record.setResult("NO_RESPONSE");
            recordMapper.updateById(record);
        }
        session.setStatus("FINISHED");
        session.setEndedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        sseHub.broadcast("rollcall:session", Map.of("sessionId", sessionId, "action", "FINISH"));
        logService.log("ROLL_CALL", "FINISH", sessionId, "结束点名会话");
    }

    @Transactional
    public int syncAttendance(Long sessionId) {
        RollCallSession session = requireSession(sessionId);
        assertTeacherOwner(session);
        if (!configService.getBool("allow.sync.rollcall.attendance", true)) {
            throw BizException.invalid("管理员已关闭点名异常同步出勤功能");
        }
        List<RollCallRecord> records = recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                .eq(RollCallRecord::getSessionId, sessionId)
                .in(RollCallRecord::getResult, List.of("NO_RESPONSE", "PENDING")));
        int count = 0;
        for (RollCallRecord record : records) {
            Student student = studentMapper.selectById(record.getStudentId());
            if (student != null) {
                notificationService.send(student.getUserId(), "课堂点名异常提醒",
                        "你在《" + access.courseName(session.getCourseId()) + "》课堂点名中未及时响应，请课后与教师沟通。",
                        "ROLL_CALL_WARNING", sessionId);
                count++;
            }
        }
        logService.log("ROLL_CALL", "SYNC_ATTENDANCE", sessionId, "同步点名异常出勤提醒，共 " + count + " 人");
        return count;
    }

    /* ===================== 内部工具 ===================== */

    public RollCallSession requireSession(Long sessionId) {
        RollCallSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BizException.notFound("点名会话不存在");
        }
        return session;
    }

    private void assertTeacherOwner(RollCallSession session) {
        var user = SecurityUtils.current();
        if ("ADMIN".equals(user.role())) {
            return;
        }
        if ("TEACHER".equals(user.role()) && session.getTeacherId().equals(user.relatedId())) {
            return;
        }
        throw BizException.forbidden("只有发起教师或管理员可以操作该点名");
    }

    private RollCallVO toVO(RollCallSession session) {
        List<RollCallRecord> records = recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                .eq(RollCallRecord::getSessionId, session.getId()).orderByAsc(RollCallRecord::getDrawOrder));
        Map<Long, Student> students = records.isEmpty() ? Map.of()
                : studentMapper.selectBatchIds(records.stream().map(RollCallRecord::getStudentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, s -> s));

        List<Map<String, Object>> recordMaps = new ArrayList<>();
        long responded = 0;
        long noResponse = 0;
        long pending = 0;
        long skipped = 0;
        long canceled = 0;
        for (RollCallRecord record : records) {
            Student student = students.get(record.getStudentId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("recordId", record.getId());
            map.put("studentId", record.getStudentId());
            map.put("studentNo", student == null ? "" : student.getStudentNo());
            map.put("name", student == null ? "未知学生" : student.getName());
            map.put("className", student == null ? null : access.className(student.getClassId()));
            map.put("avatarText", student == null || student.getName().isEmpty() ? "?" : student.getName().substring(0, 1));
            map.put("drawOrder", record.getDrawOrder());
            map.put("result", record.getResult());
            map.put("resultLabel", TaskService.statusLabel(record.getResult()));
            map.put("performance", record.getPerformance());
            map.put("drawnAt", record.getDrawnAt());
            map.put("respondedAt", record.getRespondedAt());
            map.put("remark", record.getRemark());
            map.put("deadline", record.getDrawnAt().plusSeconds(session.getResponseDeadlineSeconds()));
            recordMaps.add(map);
            switch (record.getResult()) {
                case "RESPONDED" -> responded++;
                case "NO_RESPONSE" -> noResponse++;
                case "PENDING" -> pending++;
                case "SKIPPED" -> skipped++;
                case "CANCELED" -> canceled++;
                default -> {
                }
            }
        }

        RollCallVO vo = new RollCallVO();
        vo.setId(session.getId());
        vo.setCourseId(session.getCourseId());
        vo.setCourseName(access.courseName(session.getCourseId()));
        vo.setClassId(session.getClassId());
        vo.setClassName(access.className(session.getClassId()));
        vo.setTeacherName(access.teacherName(session.getTeacherId()));
        vo.setCandidateScope(session.getCandidateScope());
        vo.setDrawCount(session.getDrawCount());
        vo.setAntiRepeatRule(session.getAntiRepeatRule());
        vo.setExcludeLeave(session.getExcludeLeave());
        vo.setResponseDeadlineSeconds(session.getResponseDeadlineSeconds());
        vo.setStatus(session.getStatus());
        vo.setStartedAt(session.getStartedAt());
        vo.setEndedAt(session.getEndedAt());
        vo.setRecords(recordMaps);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", records.size());
        summary.put("responded", responded);
        summary.put("noResponse", noResponse);
        summary.put("pending", pending);
        summary.put("skipped", skipped);
        summary.put("canceled", canceled);
        summary.put("remainingCandidates", remainingCandidateCount(session));
        vo.setSummary(summary);
        return vo;
    }

    private int remainingCandidateCount(RollCallSession session) {
        Set<Long> drawn = recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                        .eq(RollCallRecord::getSessionId, session.getId()))
                .stream().map(RollCallRecord::getStudentId).collect(Collectors.toSet());
        List<Long> pool = candidateStudentIds(session.getCourseId(), session.getClassId(),
                session.getCandidateScope(), session.getTaskId(), parseIds(session.getCustomStudentIds()),
                session.getExcludeLeave() != null && session.getExcludeLeave() == 1);
        return (int) pool.stream().filter(id -> !drawn.contains(id)).count();
    }

    private List<Long> candidateStudentIds(Long courseId, Long classId, String scope, Long taskId,
                                           List<Long> customIds, boolean excludeLeave) {
        List<Student> students = access.courseStudents(courseId);
        if (classId != null) {
            students = students.stream().filter(s -> classId.equals(s.getClassId())).toList();
        }
        Map<Long, Student> map = students.stream().collect(Collectors.toMap(Student::getId, s -> s, (a, b) -> a));

        if ("CUSTOM".equals(scope)) {
            map.entrySet().removeIf(e -> !customIds.contains(e.getKey()));
        } else if ("SIGNED".equals(scope) || "UNSIGNED".equals(scope)) {
            if (taskId == null) {
                throw BizException.invalid("按签到状态抽取需要指定签到任务");
            }
            Set<Long> signed = signRecordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                            .eq(SignRecord::getTaskId, taskId))
                    .stream().map(SignRecord::getStudentId).collect(Collectors.toSet());
            if ("SIGNED".equals(scope)) {
                map.entrySet().removeIf(e -> !signed.contains(e.getKey()));
            } else {
                Set<Long> signedIds = signed;
                map.entrySet().removeIf(e -> signedIds.contains(e.getKey()));
            }
        }

        if (excludeLeave && taskId != null) {
            Set<Long> leaves = new HashSet<>();
            signRecordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                            .eq(SignRecord::getTaskId, taskId).eq(SignRecord::getStatus, "LEAVE"))
                    .forEach(r -> leaves.add(r.getStudentId()));
            leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                            .eq(LeaveRequest::getTaskId, taskId).eq(LeaveRequest::getStatus, "APPROVED"))
                    .forEach(l -> leaves.add(l.getStudentId()));
            map.entrySet().removeIf(e -> leaves.contains(e.getKey()));
        }
        return new ArrayList<>(map.keySet());
    }

    private Set<Long> recentDrawnIds(RollCallSession session, List<Long> candidateIds) {
        String rule = session.getAntiRepeatRule();
        if (rule == null || "NONE".equals(rule) || "SESSION".equals(rule)) {
            return Set.of();
        }
        List<RollCallSession> sessions;
        if ("DAYS7".equals(rule)) {
            sessions = sessionMapper.selectList(new LambdaQueryWrapper<RollCallSession>()
                    .eq(RollCallSession::getCourseId, session.getCourseId())
                    .ge(RollCallSession::getStartedAt, LocalDateTime.now().minusDays(7)));
        } else {
            sessions = sessionMapper.selectList(new LambdaQueryWrapper<RollCallSession>()
                    .eq(RollCallSession::getCourseId, session.getCourseId())
                    .orderByDesc(RollCallSession::getStartedAt)
                    .last("limit 5"));
        }
        if (sessions.isEmpty()) {
            return Set.of();
        }
        return recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                        .in(RollCallRecord::getSessionId, sessions.stream().map(RollCallSession::getId).toList()))
                .stream().map(RollCallRecord::getStudentId).collect(Collectors.toSet());
    }

    private Map<Long, LocalDateTime> lastDrawnTimes(Long courseId) {
        List<Long> sessionIds = sessionMapper.selectList(new LambdaQueryWrapper<RollCallSession>()
                .eq(RollCallSession::getCourseId, courseId)).stream().map(RollCallSession::getId).toList();
        Map<Long, LocalDateTime> map = new HashMap<>();
        if (sessionIds.isEmpty()) {
            return map;
        }
        recordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                        .in(RollCallRecord::getSessionId, sessionIds))
                .forEach(r -> map.merge(r.getStudentId(), r.getDrawnAt(),
                        (a, b) -> a.isAfter(b) ? a : b));
        return map;
    }

    private Long resolveTaskId(Long requested, Long courseId, String scope) {
        if (requested != null) {
            SignTask task = access.requireTask(requested);
            if (!Objects.equals(task.getCourseId(), courseId)) {
                throw BizException.invalid("签到任务不属于当前课程");
            }
            return requested;
        }
        if ("SIGNED".equals(scope) || "UNSIGNED".equals(scope)) {
            SignTask task = taskMapper.selectOne(new LambdaQueryWrapper<SignTask>()
                    .eq(SignTask::getCourseId, courseId)
                    .orderByDesc(SignTask::getStartTime).last("limit 1"));
            if (task == null) {
                throw BizException.invalid("该课程还没有签到任务，无法按签到状态抽取");
            }
            return task.getId();
        }
        return null;
    }

    private String normalizeScope(String scope) {
        if (scope == null) {
            return "ALL";
        }
        String value = scope.trim().toUpperCase();
        if (!List.of("ALL", "SIGNED", "UNSIGNED", "CUSTOM").contains(value)) {
            throw BizException.invalid("不支持的候选范围");
        }
        return value;
    }

    private String normalizeAntiRepeat(String rule) {
        if (rule == null) {
            return "SESSION";
        }
        String value = rule.trim().toUpperCase();
        if (!List.of("NONE", "SESSION", "DAYS7", "LAST_N").contains(value)) {
            throw BizException.invalid("不支持的防重复规则");
        }
        return value;
    }

    private List<Long> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split(",")).stream().filter(s -> !s.isBlank()).map(Long::valueOf).toList();
    }

    public List<Map<String, Object>> history(Long courseId, LocalDate from, LocalDate to) {
        Long teacherId = SecurityUtils.isAdmin() ? null : SecurityUtils.teacherId();
        LambdaQueryWrapper<RollCallSession> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(RollCallSession::getTeacherId, teacherId);
        }
        if (courseId != null) {
            wrapper.eq(RollCallSession::getCourseId, courseId);
        }
        if (from != null) {
            wrapper.ge(RollCallSession::getStartedAt, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(RollCallSession::getStartedAt, to.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(RollCallSession::getStartedAt).last("limit 100");
        List<RollCallSession> sessions = sessionMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RollCallSession session : sessions) {
            RollCallVO vo = toVO(session);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", vo.getId());
            map.put("courseId", vo.getCourseId());
            map.put("courseName", vo.getCourseName());
            map.put("className", vo.getClassName());
            map.put("candidateScope", vo.getCandidateScope());
            map.put("status", vo.getStatus());
            map.put("startedAt", vo.getStartedAt());
            map.put("endedAt", vo.getEndedAt());
            map.put("summary", vo.getSummary());
            map.put("records", vo.getRecords());
            result.add(map);
        }
        return result;
    }

    public byte[] exportHistory(Long courseId, LocalDate from, LocalDate to) {
        List<Map<String, Object>> sessions = history(courseId, from, to);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("随机点名记录");
            String[] headers = {"会话ID", "课程", "班级", "抽取时间", "候选范围", "学生", "学号",
                    "抽取序号", "结果", "课堂表现", "抽取时间点", "响应时间"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 18 * 256);
            }
            int rowIndex = 1;
            for (Map<String, Object> session : sessions) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> records = (List<Map<String, Object>>) session.get("records");
                for (Map<String, Object> record : records) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(String.valueOf(session.get("id")));
                    row.createCell(1).setCellValue(String.valueOf(session.get("courseName")));
                    row.createCell(2).setCellValue(session.get("className") == null ? "全部班级" : String.valueOf(session.get("className")));
                    row.createCell(3).setCellValue(String.valueOf(session.get("startedAt")));
                    row.createCell(4).setCellValue(String.valueOf(session.get("candidateScope")));
                    row.createCell(5).setCellValue(String.valueOf(record.get("name")));
                    row.createCell(6).setCellValue(String.valueOf(record.get("studentNo")));
                    row.createCell(7).setCellValue(String.valueOf(record.get("drawOrder")));
                    row.createCell(8).setCellValue(TaskService.statusLabel(String.valueOf(record.get("result"))));
                    row.createCell(9).setCellValue(performanceLabel(record.get("performance")));
                    row.createCell(10).setCellValue(String.valueOf(record.get("drawnAt")));
                    row.createCell(11).setCellValue(record.get("respondedAt") == null ? "" : String.valueOf(record.get("respondedAt")));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "导出互动记录失败：" + e.getMessage());
        }
    }

    private String performanceLabel(Object value) {
        if (value == null) {
            return "";
        }
        return switch (String.valueOf(value)) {
            case "EXCELLENT" -> "优秀";
            case "ACTIVE" -> "积极";
            case "NORMAL" -> "一般";
            case "FOLLOW_UP" -> "待跟进";
            default -> String.valueOf(value);
        };
    }
}
