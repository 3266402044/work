package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.PageResult;
import com.classpulse.common.SecurityUtils;
import com.classpulse.common.TaskStatus;
import com.classpulse.common.Utils;
import com.classpulse.dto.ManualRecordRequest;
import com.classpulse.dto.TaskSaveRequest;
import com.classpulse.entity.ClassGroup;
import com.classpulse.entity.Course;
import com.classpulse.entity.LeaveRequest;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.mapper.LeaveRequestMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskService {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SignTaskMapper taskMapper;
    private final SignRecordMapper recordMapper;
    private final StudentMapper studentMapper;
    private final LeaveRequestMapper leaveMapper;
    private final AccessService access;
    private final NotificationService notificationService;
    private final OperationLogService logService;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final SseHub sseHub;

    /* ===================== 任务创建与维护 ===================== */

    @Transactional
    public TaskVO create(TaskSaveRequest request) {
        validateTimes(request);
        Long teacherId;
        if (SecurityUtils.isAdmin()) {
            Course course = access.requireCourse(request.getCourseId());
            teacherId = course.getTeacherId();
        } else {
            teacherId = SecurityUtils.teacherId();
            access.assertCourseManageable(request.getCourseId());
        }
        ClassGroup group = access.requireClass(request.getClassId());
        validateModeFields(request);

        SignTask task = new SignTask();
        task.setTitle(request.getTitle().trim());
        task.setTeacherId(teacherId);
        task.setCourseId(request.getCourseId());
        task.setClassId(group.getId());
        task.setStartTime(request.getStartTime());
        task.setEndTime(request.getEndTime());
        task.setLateTime(request.getLateTime());
        task.setSignModes(String.join(",", normalizeModes(request.getSignModes())));
        task.setLocationName(request.getLocationName());
        task.setLongitude(request.getLongitude());
        task.setLatitude(request.getLatitude());
        task.setRadiusMeters(request.getRadiusMeters() == null
                ? configService.getInt("default.location.radius", 150) : request.getRadiusMeters());
        task.setCreateTime(LocalDateTime.now());

        String passcode = null;
        if (task.getSignModes().contains("PASSCODE")) {
            passcode = Utils.randomDigits(6);
            task.setPasscodeHash(passwordEncoder.encode(passcode));
        }
        taskMapper.insert(task);

        notificationService.sendToClass(task.getClassId(), "新的签到任务",
                "《" + courseName(task.getCourseId()) + "》" + task.getTitle() + " 已发布，请留意签到时间。",
                "TASK", task.getId());
        logService.log("TASK", "CREATE", task.getId(), "创建签到任务：" + task.getTitle());
        sseHub.broadcast("task:changed", Map.of("taskId", task.getId(), "action", "CREATE"));

        TaskVO vo = toVO(task, null);
        vo.setPasscode(passcode);
        return vo;
    }

    @Transactional
    public TaskVO update(Long taskId, TaskSaveRequest request) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskManageable(task);
        TaskStatus status = TaskStatus.of(task, LocalDateTime.now());
        if (status == TaskStatus.ENDED || status == TaskStatus.CLOSED) {
            throw BizException.invalid("已结束或已关闭的任务不可编辑");
        }
        validateTimes(request);
        validateModeFields(request);
        ClassGroup group = access.requireClass(request.getClassId());

        task.setTitle(request.getTitle().trim());
        task.setCourseId(request.getCourseId());
        task.setClassId(group.getId());
        task.setStartTime(request.getStartTime());
        task.setEndTime(request.getEndTime());
        task.setLateTime(request.getLateTime());
        task.setSignModes(String.join(",", normalizeModes(request.getSignModes())));
        task.setLocationName(request.getLocationName());
        task.setLongitude(request.getLongitude());
        task.setLatitude(request.getLatitude());
        task.setRadiusMeters(request.getRadiusMeters());

        String passcode = null;
        if (task.getSignModes().contains("PASSCODE") && task.getPasscodeHash() == null) {
            passcode = Utils.randomDigits(6);
            task.setPasscodeHash(passwordEncoder.encode(passcode));
        }
        taskMapper.updateById(task);
        logService.log("TASK", "UPDATE", task.getId(), "编辑签到任务：" + task.getTitle());
        TaskVO vo = toVO(task, null);
        vo.setPasscode(passcode);
        return vo;
    }

    @Transactional
    public TaskVO close(Long taskId) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskManageable(task);
        if (task.getClosedAt() != null) {
            throw BizException.conflict("该任务已经提前关闭");
        }
        TaskStatus status = TaskStatus.of(task, LocalDateTime.now());
        if (status == TaskStatus.ENDED) {
            throw BizException.invalid("任务已自然结束，无需关闭");
        }
        task.setClosedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        notificationService.sendToClass(task.getClassId(), "签到已关闭",
                "《" + courseName(task.getCourseId()) + "》" + task.getTitle() + " 已被教师提前关闭。",
                "TASK", task.getId());
        logService.log("TASK", "CLOSE", task.getId(), "提前关闭签到任务");
        sseHub.broadcast("task:changed", Map.of("taskId", task.getId(), "action", "CLOSE"));
        return toVO(task, null);
    }

    @Transactional
    public Map<String, Object> generateQrToken(Long taskId) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskManageable(task);
        if (TaskStatus.of(task, LocalDateTime.now()) != TaskStatus.ONGOING) {
            throw BizException.invalid("只有进行中的任务才能生成二维码");
        }
        String raw = Utils.randomToken();
        var token = new com.classpulse.entity.SignQrToken();
        token.setTaskId(taskId);
        token.setTokenHash(Utils.sha256(raw));
        token.setExpiredAt(LocalDateTime.now().plusSeconds(30));
        token.setUsedCount(0);
        token.setCreateTime(LocalDateTime.now());
        // 清理同任务的历史令牌，仅保留最近一次
        qrTokenMapper.delete(new LambdaQueryWrapper<com.classpulse.entity.SignQrToken>()
                .eq(com.classpulse.entity.SignQrToken::getTaskId, taskId));
        qrTokenMapper.insert(token);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", raw);
        result.put("expiresAt", token.getExpiredAt());
        result.put("taskId", taskId);
        result.put("url", "/student/tasks/" + taskId + "?qr=" + raw);
        return result;
    }

    @Transactional
    public String regeneratePasscode(Long taskId) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskManageable(task);
        if (!Utils.splitModes(task.getSignModes()).contains("PASSCODE")) {
            throw BizException.invalid("该任务未开启口令签到");
        }
        String passcode = Utils.randomDigits(6);
        task.setPasscodeHash(passwordEncoder.encode(passcode));
        taskMapper.updateById(task);
        logService.log("TASK", "PASSCODE", taskId, "重置签到口令");
        return passcode;
    }

    /* ===================== 查询 ===================== */

    public TaskVO detail(Long taskId) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskReadable(task);
        Student student = null;
        var user = SecurityUtils.current();
        if ("STUDENT".equals(user.role())) {
            student = access.requireStudent(user.relatedId());
        }
        return toVO(task, student);
    }

    public PageResult<TaskVO> studentTasks(String scope, Long courseId, LocalDate date, String keyword,
                                          long page, long size) {
        Student student = access.requireStudent(SecurityUtils.studentId());
        List<Course> courses = access.coursesOfStudent(student);
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        LambdaQueryWrapper<SignTask> wrapper = new LambdaQueryWrapper<>();
        if (student.getClassId() != null && !courseIds.isEmpty()) {
            wrapper.and(w -> w.eq(SignTask::getClassId, student.getClassId())
                    .or(n -> n.isNull(SignTask::getClassId).in(SignTask::getCourseId, courseIds)));
        } else if (student.getClassId() != null) {
            wrapper.eq(SignTask::getClassId, student.getClassId());
        } else if (!courseIds.isEmpty()) {
            wrapper.isNull(SignTask::getClassId).in(SignTask::getCourseId, courseIds);
        } else {
            return PageResult.of(List.of(), 0, page, size);
        }
        if (courseId != null) {
            wrapper.eq(SignTask::getCourseId, courseId);
        }
        if (date != null) {
            wrapper.ge(SignTask::getStartTime, date.atStartOfDay())
                    .lt(SignTask::getStartTime, date.plusDays(1).atStartOfDay());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SignTask::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(SignTask::getStartTime);

        LocalDateTime now = LocalDateTime.now();
        List<TaskVO> list = taskMapper.selectList(wrapper).stream()
                .map(t -> toVO(t, student))
                .filter(vo -> matchScope(vo, scope, now))
                .toList();
        return paginate(list, page, size);
    }

    public PageResult<TaskVO> teacherTasks(Long courseId, Long classId, LocalDate date, String scope,
                                          String keyword, long page, long size) {
        Long teacherId = SecurityUtils.isAdmin() ? null : SecurityUtils.teacherId();
        LambdaQueryWrapper<SignTask> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(SignTask::getTeacherId, teacherId);
        }
        if (courseId != null) {
            wrapper.eq(SignTask::getCourseId, courseId);
        }
        if (classId != null) {
            wrapper.eq(SignTask::getClassId, classId);
        }
        if (date != null) {
            wrapper.ge(SignTask::getStartTime, date.atStartOfDay())
                    .lt(SignTask::getStartTime, date.plusDays(1).atStartOfDay());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SignTask::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(SignTask::getStartTime);
        LocalDateTime now = LocalDateTime.now();
        List<TaskVO> list = taskMapper.selectList(wrapper).stream()
                .map(t -> toVO(t, null))
                .filter(vo -> matchScope(vo, scope, now))
                .sorted(Comparator.comparing(TaskVO::getStartTime).reversed())
                .toList();
        return paginate(list, page, size);
    }

    public List<TaskVO> ongoingTasksOfTeacher() {
        Long teacherId = SecurityUtils.isAdmin() ? null : SecurityUtils.teacherId();
        LambdaQueryWrapper<SignTask> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(SignTask::getTeacherId, teacherId);
        }
        wrapper.orderByDesc(SignTask::getStartTime).last("limit 100");
        LocalDateTime now = LocalDateTime.now();
        return taskMapper.selectList(wrapper).stream()
                .filter(t -> TaskStatus.of(t, now) == TaskStatus.ONGOING)
                .map(t -> toVO(t, null))
                .toList();
    }

    public Map<String, Object> records(Long taskId) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskManageable(task);
        List<Student> roster = access.roster(task);
        List<SignRecord> records = recordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getTaskId, taskId));
        Map<Long, SignRecord> recordMap = new HashMap<>();
        records.forEach(r -> recordMap.put(r.getStudentId(), r));

        Map<Long, LeaveRequest> leaveMap = new HashMap<>();
        leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getTaskId, taskId).eq(LeaveRequest::getStatus, "APPROVED"))
                .forEach(l -> leaveMap.put(l.getStudentId(), l));

        List<Map<String, Object>> students = new ArrayList<>();
        long normal = 0;
        long late = 0;
        long leave = 0;
        for (Student student : roster) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", student.getId());
            item.put("studentNo", student.getStudentNo());
            item.put("name", student.getName());
            item.put("className", access.className(student.getClassId()));
            SignRecord record = recordMap.get(student.getId());
            if (record != null) {
                item.put("recordId", record.getId());
                item.put("status", record.getStatus());
                item.put("method", record.getMethod());
                item.put("signTime", record.getSignTime());
                item.put("distanceMeters", record.getDistanceMeters());
                item.put("longitude", record.getLongitude());
                item.put("latitude", record.getLatitude());
                item.put("remark", record.getRemark());
                if ("NORMAL".equals(record.getStatus()) || "MANUAL".equals(record.getStatus())) {
                    normal++;
                } else if ("LATE".equals(record.getStatus())) {
                    late++;
                } else if ("LEAVE".equals(record.getStatus())) {
                    leave++;
                }
            } else if (leaveMap.containsKey(student.getId())) {
                item.put("status", "LEAVE_PENDING_RECORD");
                item.put("remark", leaveMap.get(student.getId()).getReason());
                leave++;
            } else {
                item.put("status", "ABSENT");
            }
            students.add(item);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", roster.size());
        summary.put("signed", recordMap.size());
        summary.put("normal", normal);
        summary.put("late", late);
        summary.put("leave", leave);
        summary.put("absent", Math.max(roster.size() - recordMap.size() - leaveMap.size(), 0));
        summary.put("attendanceRate", Utils.rate(recordMap.size(), roster.size()));
        summary.put("lateRate", Utils.rate(late, roster.size()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", toVO(task, null));
        result.put("summary", summary);
        result.put("students", students);
        return result;
    }

    /* ===================== 补签与导出 ===================== */

    @Transactional
    public void manualRecord(Long taskId, ManualRecordRequest request) {
        SignTask task = access.requireTask(taskId);
        access.assertTaskManageable(task);
        Student student = access.requireStudent(request.getStudentId());
        access.assertStudentEligible(student, task);
        List<String> allowed = List.of("NORMAL", "LATE", "MANUAL", "LEAVE");
        if (!allowed.contains(request.getStatus())) {
            throw BizException.invalid("不支持的手动记录状态");
        }
        LocalDateTime signTime = request.getSignTime() == null ? LocalDateTime.now() : request.getSignTime();
        SignRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getTaskId, taskId).eq(SignRecord::getStudentId, student.getId()).last("limit 1"));
        if (existing != null) {
            existing.setStatus(request.getStatus());
            existing.setMethod("MANUAL");
            existing.setSignTime(signTime);
            existing.setRemark(request.getRemark());
            existing.setOperatorId(SecurityUtils.current().relatedId());
            recordMapper.updateById(existing);
        } else {
            SignRecord record = new SignRecord();
            record.setTaskId(taskId);
            record.setStudentId(student.getId());
            record.setSignTime(signTime);
            record.setStatus(request.getStatus());
            record.setMethod("MANUAL");
            record.setRemark(request.getRemark());
            record.setOperatorId(SecurityUtils.current().relatedId());
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
        notificationService.send(student.getUserId(), "签到记录已更新",
                "教师已为你在《" + task.getTitle() + "》中登记签到，状态：" + statusLabel(request.getStatus()),
                "SIGN", taskId);
        logService.log("TASK", "MANUAL_RECORD", taskId, "手动补签学生：" + student.getStudentNo());
        sseHub.send(student.getUserId(), "sign:updated", Map.of("taskId", taskId));
        sseHub.broadcast("sign:new", Map.of("taskId", taskId));
    }

    public byte[] exportRecords(Long taskId) {
        Map<String, Object> data = records(taskId);
        TaskVO task = (TaskVO) data.get("task");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> students = (List<Map<String, Object>>) data.get("students");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("签到明细");
            String[] headers = {"序号", "学号", "姓名", "班级", "签到状态", "签到方式", "签到时间", "距离(米)", "备注"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowIndex = 1;
            for (Map<String, Object> student : students) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(rowIndex - 1);
                row.createCell(1).setCellValue(String.valueOf(student.get("studentNo")));
                row.createCell(2).setCellValue(String.valueOf(student.get("name")));
                row.createCell(3).setCellValue(String.valueOf(student.get("className")));
                row.createCell(4).setCellValue(statusLabel(String.valueOf(student.get("status"))));
                row.createCell(5).setCellValue(methodLabel(String.valueOf(student.get("method"))));
                Object signTime = student.get("signTime");
                row.createCell(6).setCellValue(signTime == null ? "" : formatTime((LocalDateTime) signTime));
                Object distance = student.get("distanceMeters");
                row.createCell(7).setCellValue(distance == null ? "" : distance.toString());
                Object remark = student.get("remark");
                row.createCell(8).setCellValue(remark == null ? "" : remark.toString());
            }
            workbook.write(out);
            logService.log("TASK", "EXPORT", taskId, "导出签到名单：" + task.getTitle());
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "导出失败：" + e.getMessage());
        }
    }

    /* ===================== VO 与辅助 ===================== */

    public TaskVO toVO(SignTask task, Student student) {
        LocalDateTime now = LocalDateTime.now();
        TaskStatus status = TaskStatus.of(task, now);
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setTeacherId(task.getTeacherId());
        vo.setTeacherName(access.teacherName(task.getTeacherId()));
        vo.setCourseId(task.getCourseId());
        vo.setCourseName(access.courseName(task.getCourseId()));
        Course course = task.getCourseId() == null ? null : access.requireCourse(task.getCourseId());
        vo.setCourseColor(course == null ? "#38DDF8" : course.getCoverColor());
        vo.setClassId(task.getClassId());
        vo.setClassName(access.className(task.getClassId()));
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        vo.setLateTime(task.getLateTime());
        vo.setClosedAt(task.getClosedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setTaskStatus(status.name());
        vo.setTaskStatusLabel(status.getLabel());
        vo.setSignModes(Utils.splitModes(task.getSignModes()));
        vo.setLocationName(task.getLocationName());
        vo.setLongitude(task.getLongitude());
        vo.setLatitude(task.getLatitude());
        vo.setRadiusMeters(task.getRadiusMeters());
        vo.setSecondsToStart(Duration.between(now, task.getStartTime()).getSeconds());
        vo.setSecondsToEnd(Duration.between(now, task.getEndTime()).getSeconds());

        List<Student> roster = access.roster(task);
        List<SignRecord> records = recordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getTaskId, task.getId()));
        long normal = records.stream().filter(r -> "NORMAL".equals(r.getStatus()) || "MANUAL".equals(r.getStatus())).count();
        long late = records.stream().filter(r -> "LATE".equals(r.getStatus())).count();
        long leave = records.stream().filter(r -> "LEAVE".equals(r.getStatus())).count();
        vo.setTotalStudents(roster.size());
        vo.setSignedCount(records.size());
        vo.setNormalCount(normal);
        vo.setLateCount(late);
        vo.setLeaveCount(leave);
        vo.setAbsentCount(Math.max(roster.size() - records.size(), 0));
        vo.setAttendanceRate(Utils.rate(records.size(), roster.size()));

        if (student != null) {
            SignRecord mine = records.stream()
                    .filter(r -> Objects.equals(r.getStudentId(), student.getId()))
                    .findFirst().orElse(null);
            vo.setSigned(mine != null);
            vo.setMyRecord(mine);
            boolean eligible = access.studentInTask(student, task);
            boolean canSign = eligible && status == TaskStatus.ONGOING && mine == null;
            vo.setCanSign(canSign);
            if (!eligible) {
                vo.setSignHint("你不属于该任务的班级或课程");
            } else if (mine != null) {
                vo.setSignHint("你已完成签到");
            } else if (status == TaskStatus.NOT_STARTED) {
                vo.setSignHint("签到尚未开始");
            } else if (status == TaskStatus.CLOSED) {
                vo.setSignHint("教师已提前关闭签到");
            } else if (status == TaskStatus.ENDED) {
                vo.setSignHint("签到已结束");
            }
        }
        return vo;
    }

    public Map<String, Object> summaryOf(SignTask task) {
        List<Student> roster = access.roster(task);
        long signed = recordMapper.selectCount(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getTaskId, task.getId()));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", roster.size());
        map.put("signed", signed);
        map.put("attendanceRate", Utils.rate(signed, roster.size()));
        return map;
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "NORMAL" -> "正常";
            case "LATE" -> "迟到";
            case "MANUAL" -> "补签";
            case "LEAVE" -> "请假";
            case "ABSENT" -> "未签到";
            case "LEAVE_PENDING_RECORD" -> "已批准请假";
            case "PENDING" -> "待响应";
            case "RESPONDED" -> "已响应";
            case "NO_RESPONSE" -> "未响应";
            case "SKIPPED" -> "已跳过";
            case "CANCELED" -> "已撤销";
            default -> status;
        };
    }

    public static String methodLabel(String method) {
        if (method == null) {
            return "";
        }
        return switch (method) {
            case "CLICK" -> "普通签到";
            case "QR" -> "二维码";
            case "LOCATION" -> "定位签到";
            case "PASSCODE" -> "口令签到";
            case "MANUAL" -> "教师补签";
            default -> method;
        };
    }

    private boolean matchScope(TaskVO vo, String scope, LocalDateTime now) {
        if (scope == null || scope.isBlank() || "ALL".equalsIgnoreCase(scope)) {
            return true;
        }
        return vo.getTaskStatus().equalsIgnoreCase(scope);
    }

    private <T> PageResult<T> paginate(List<T> all, long page, long size) {
        int from = (int) Math.min((page - 1) * size, all.size());
        int to = (int) Math.min(from + size, all.size());
        return PageResult.of(all.subList(from, to), all.size(), page, size);
    }

    private void validateTimes(TaskSaveRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw BizException.invalid("结束时间必须晚于开始时间");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw BizException.invalid("开始时间不能早于当前时间 5 分钟以上");
        }
        if (request.getLateTime() != null) {
            if (request.getLateTime().isBefore(request.getStartTime()) || request.getLateTime().isAfter(request.getEndTime())) {
                throw BizException.invalid("迟到截止时间必须位于开始时间与结束时间之间");
            }
        }
    }

    private List<String> normalizeModes(List<String> modes) {
        List<String> allowed = List.of("CLICK", "QR", "LOCATION", "PASSCODE");
        List<String> result = modes.stream().map(m -> m == null ? "" : m.trim().toUpperCase())
                .distinct().toList();
        if (result.isEmpty()) {
            throw BizException.invalid("至少选择一种签到方式");
        }
        for (String mode : result) {
            if (!allowed.contains(mode)) {
                throw BizException.invalid("不支持的签到方式：" + mode);
            }
        }
        return result;
    }

    private void validateModeFields(TaskSaveRequest request) {
        List<String> modes = normalizeModes(request.getSignModes());
        if (modes.contains("LOCATION")) {
            if (request.getLongitude() == null || request.getLatitude() == null) {
                throw BizException.invalid("定位签到需要填写签到地点经纬度");
            }
            if (request.getLongitude().abs().compareTo(BigDecimal.valueOf(180)) > 0
                    || request.getLatitude().abs().compareTo(BigDecimal.valueOf(90)) > 0) {
                throw BizException.invalid("经纬度超出合法范围");
            }
            int radius = request.getRadiusMeters() == null ? 150 : request.getRadiusMeters();
            if (radius < 20 || radius > 5000) {
                throw BizException.invalid("定位半径需在 20-5000 米之间");
            }
        }
    }

    private String courseName(Long courseId) {
        if (courseId == null) {
            return "课程";
        }
        Course course = access.requireCourse(courseId);
        return course.getName();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FMT);
    }

    /* 兼容历史数据调用 */
    private final com.classpulse.mapper.SignQrTokenMapper qrTokenMapper;

    public Map<String, Object> taskBrief(Long taskId) {
        SignTask task = access.requireTask(taskId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("title", task.getTitle());
        map.put("startTime", task.getStartTime().format(SHORT_FMT));
        map.put("endTime", task.getEndTime().format(SHORT_FMT));
        return map;
    }

    @SuppressWarnings("unused")
    private void touch(LambdaUpdateWrapper<SignTask> wrapper) {
        // reserved for future batch updates
    }
}
