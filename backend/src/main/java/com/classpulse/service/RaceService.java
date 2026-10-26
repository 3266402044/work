package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.dto.RaceCreateRequest;
import com.classpulse.entity.Course;
import com.classpulse.entity.RaceRecord;
import com.classpulse.entity.RaceSession;
import com.classpulse.entity.Student;
import com.classpulse.mapper.RaceRecordMapper;
import com.classpulse.mapper.RaceSessionMapper;
import com.classpulse.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RaceService {
    private final RaceSessionMapper raceMapper;
    private final RaceRecordMapper raceRecordMapper;
    private final StudentMapper studentMapper;
    private final AccessService access;
    private final NotificationService notificationService;
    private final OperationLogService logService;
    private final SseHub sseHub;

    @Transactional
    public Map<String, Object> create(RaceCreateRequest request) {
        Course course = access.requireCourse(request.getCourseId());
        access.assertCourseManageable(course.getId());
        Long teacherId = SecurityUtils.isAdmin() ? course.getTeacherId() : SecurityUtils.teacherId();
        RaceSession race = new RaceSession();
        race.setCourseId(course.getId());
        race.setTeacherId(teacherId);
        race.setTitle(request.getTitle().trim());
        race.setStartTime(LocalDateTime.now());
        race.setEndTime(LocalDateTime.now().plusSeconds(request.getDurationSeconds()));
        race.setStatus("RUNNING");
        race.setCreateTime(LocalDateTime.now());
        raceMapper.insert(race);

        access.courseStudents(course.getId()).forEach(student -> notificationService.send(student.getUserId(),
                "课堂抢答开始", "《" + course.getName() + "》发布抢答：" + race.getTitle() + "，手快有！",
                "RACE", race.getId()));
        sseHub.broadcast("race:started", Map.of("raceId", race.getId(), "courseId", course.getId(),
                "title", race.getTitle(), "endTime", race.getEndTime(), "courseName", course.getName()));
        logService.log("RACE", "CREATE", race.getId(), "发布课堂抢答：" + race.getTitle());
        return detail(race.getId());
    }

    public List<Map<String, Object>> active() {
        Student student = access.requireStudent(SecurityUtils.studentId());
        List<Long> courseIds = access.coursesOfStudent(student).stream().map(Course::getId).toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<RaceSession> races = raceMapper.selectList(new LambdaQueryWrapper<RaceSession>()
                .in(RaceSession::getCourseId, courseIds)
                .eq(RaceSession::getStatus, "RUNNING")
                .orderByDesc(RaceSession::getCreateTime).last("limit 10"));
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (RaceSession race : races) {
            if (now.isAfter(race.getEndTime())) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("raceId", race.getId());
            map.put("courseName", access.courseName(race.getCourseId()));
            map.put("title", race.getTitle());
            map.put("endTime", race.getEndTime());
            map.put("remainingSeconds", Duration.between(now, race.getEndTime()).getSeconds());
            RaceRecord mine = raceRecordMapper.selectOne(new LambdaQueryWrapper<RaceRecord>()
                    .eq(RaceRecord::getRaceId, race.getId())
                    .eq(RaceRecord::getStudentId, student.getId()).last("limit 1"));
            map.put("myRank", mine == null ? null : mine.getSeq());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> buzz(Long raceId) {
        Student student = access.requireStudent(SecurityUtils.studentId());
        RaceSession race = requireRace(raceId);
        boolean inCourse = access.courseStudents(race.getCourseId()).stream()
                .anyMatch(s -> s.getId().equals(student.getId()));
        if (!inCourse) {
            throw BizException.forbidden("你不在这门课程中，无法参与抢答");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(race.getEndTime())) {
            throw BizException.invalid("抢答已结束");
        }
        long existing = raceRecordMapper.selectCount(new LambdaQueryWrapper<RaceRecord>()
                .eq(RaceRecord::getRaceId, raceId).eq(RaceRecord::getStudentId, student.getId()));
        if (existing > 0) {
            throw BizException.conflict("你已抢答，不能重复提交");
        }
        long count = raceRecordMapper.selectCount(new LambdaQueryWrapper<RaceRecord>()
                .eq(RaceRecord::getRaceId, raceId));
        RaceRecord record = new RaceRecord();
        record.setRaceId(raceId);
        record.setStudentId(student.getId());
        record.setSeq((int) count + 1);
        record.setCreateTime(now);
        try {
            raceRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            throw BizException.conflict("你已抢答，不能重复提交");
        }
        sseHub.broadcast("race:update", Map.of("raceId", raceId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rank", record.getSeq());
        result.put("message", record.getSeq() <= 3 ? "抢答成功，你是第 " + record.getSeq() + " 名" : "抢答已提交");
        result.put("top3", top3(raceId));
        return result;
    }

    public Map<String, Object> detail(Long raceId) {
        RaceSession race = requireRace(raceId);
        var user = SecurityUtils.current();
        if ("STUDENT".equals(user.role())) {
            boolean inCourse = access.courseStudents(race.getCourseId()).stream()
                    .anyMatch(s -> s.getId().equals(user.relatedId()));
            if (!inCourse) {
                throw BizException.forbidden("你不在这门课程中");
            }
        }
        List<RaceRecord> records = raceRecordMapper.selectList(new LambdaQueryWrapper<RaceRecord>()
                .eq(RaceRecord::getRaceId, raceId).orderByAsc(RaceRecord::getSeq));
        Map<Long, Student> students = records.isEmpty() ? Map.of()
                : studentMapper.selectBatchIds(records.stream().map(RaceRecord::getStudentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, s -> s));
        List<Map<String, Object>> list = records.stream().map(r -> {
            Student s = students.get(r.getStudentId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", r.getStudentId());
            item.put("studentNo", s == null ? "" : s.getStudentNo());
            item.put("name", s == null ? "未知学生" : s.getName());
            item.put("rank", r.getSeq());
            item.put("createTime", r.getCreateTime());
            return item;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", race.getId());
        result.put("title", race.getTitle());
        result.put("courseId", race.getCourseId());
        result.put("courseName", access.courseName(race.getCourseId()));
        result.put("startTime", race.getStartTime());
        result.put("endTime", race.getEndTime());
        result.put("status", LocalDateTime.now().isAfter(race.getEndTime()) ? "ENDED" : "RUNNING");
        result.put("remainingSeconds", Math.max(0, Duration.between(LocalDateTime.now(), race.getEndTime()).getSeconds()));
        result.put("records", list);
        result.put("top3", list.stream().limit(3).toList());
        result.put("total", list.size());
        return result;
    }

    public List<Map<String, Object>> history(Long courseId, LocalDate from, LocalDate to) {
        Long teacherId = SecurityUtils.isAdmin() ? null : SecurityUtils.teacherId();
        LambdaQueryWrapper<RaceSession> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(RaceSession::getTeacherId, teacherId);
        }
        if (courseId != null) {
            wrapper.eq(RaceSession::getCourseId, courseId);
        }
        if (from != null) {
            wrapper.ge(RaceSession::getStartTime, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(RaceSession::getStartTime, to.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(RaceSession::getCreateTime).last("limit 50");
        List<Map<String, Object>> result = new ArrayList<>();
        for (RaceSession race : raceMapper.selectList(wrapper)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", race.getId());
            map.put("courseId", race.getCourseId());
            map.put("courseName", access.courseName(race.getCourseId()));
            map.put("title", race.getTitle());
            map.put("startTime", race.getStartTime());
            map.put("endTime", race.getEndTime());
            map.put("status", LocalDateTime.now().isAfter(race.getEndTime()) ? "ENDED" : "RUNNING");
            map.put("total", raceRecordMapper.selectCount(new LambdaQueryWrapper<RaceRecord>()
                    .eq(RaceRecord::getRaceId, race.getId())));
            result.add(map);
        }
        return result;
    }

    private List<Map<String, Object>> top3(Long raceId) {
        List<RaceRecord> records = raceRecordMapper.selectList(new LambdaQueryWrapper<RaceRecord>()
                .eq(RaceRecord::getRaceId, raceId).orderByAsc(RaceRecord::getSeq).last("limit 3"));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> students = studentMapper.selectBatchIds(records.stream().map(RaceRecord::getStudentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, s -> s));
        return records.stream().sorted(Comparator.comparing(RaceRecord::getSeq)).map(r -> {
            Student s = students.get(r.getStudentId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", r.getSeq());
            item.put("name", s == null ? "未知学生" : s.getName());
            item.put("studentNo", s == null ? "" : s.getStudentNo());
            return item;
        }).toList();
    }

    private RaceSession requireRace(Long raceId) {
        RaceSession race = raceMapper.selectById(raceId);
        if (race == null) {
            throw BizException.notFound("抢答不存在");
        }
        return race;
    }
}
