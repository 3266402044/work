package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.PageResult;
import com.classpulse.common.SecurityUtils;
import com.classpulse.common.TaskStatus;
import com.classpulse.common.Utils;
import com.classpulse.entity.ClassAnnouncement;
import com.classpulse.entity.Course;
import com.classpulse.entity.LeaveRequest;
import com.classpulse.entity.Quiz;
import com.classpulse.entity.QuizAnswer;
import com.classpulse.entity.RaceRecord;
import com.classpulse.entity.RollCallRecord;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.mapper.ClassAnnouncementMapper;
import com.classpulse.mapper.LeaveRequestMapper;
import com.classpulse.mapper.QuizAnswerMapper;
import com.classpulse.mapper.QuizMapper;
import com.classpulse.mapper.RaceRecordMapper;
import com.classpulse.mapper.RollCallRecordMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import com.classpulse.vo.SignRecordVO;
import com.classpulse.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentPortalService {
    private final AccessService access;
    private final TaskService taskService;
    private final SignTaskMapper taskMapper;
    private final SignRecordMapper recordMapper;
    private final LeaveRequestMapper leaveMapper;
    private final QuizMapper quizMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final RaceRecordMapper raceRecordMapper;
    private final RollCallRecordMapper rollCallRecordMapper;
    private final ClassAnnouncementMapper announcementMapper;
    private final ConfigService configService;

    public Map<String, Object> dashboard() {
        Student student = access.requireStudent(SecurityUtils.studentId());
        LocalDate today = LocalDate.now();
        List<SignTask> allEligible = eligibleTasks(student);

        List<TaskVO> todayTasks = allEligible.stream()
                .filter(t -> t.getStartTime().toLocalDate().equals(today))
                .sorted(Comparator.comparing(SignTask::getStartTime))
                .map(t -> taskService.toVO(t, student))
                .toList();

        TaskVO ongoing = allEligible.stream()
                .filter(t -> TaskStatus.of(t, LocalDateTime.now()) == TaskStatus.ONGOING)
                .min(Comparator.comparing(SignTask::getEndTime))
                .map(t -> taskService.toVO(t, student))
                .orElse(null);

        TaskVO upcoming = allEligible.stream()
                .filter(t -> TaskStatus.of(t, LocalDateTime.now()) == TaskStatus.NOT_STARTED)
                .min(Comparator.comparing(SignTask::getStartTime))
                .map(t -> taskService.toVO(t, student))
                .orElse(null);

        List<SignTask> recentTasks = allEligible.stream()
                .filter(t -> t.getEndTime().isBefore(LocalDateTime.now()))
                .filter(t -> t.getEndTime().isAfter(LocalDateTime.now().minusDays(7)))
                .toList();
        long present = 0;
        long normal = 0;
        long late = 0;
        long leave = 0;
        Map<Long, SignRecord> recordMap = recordMap(student.getId());
        for (SignTask task : recentTasks) {
            SignRecord record = recordMap.get(task.getId());
            if (record != null) {
                present++;
                if ("LATE".equals(record.getStatus())) {
                    late++;
                } else if ("LEAVE".equals(record.getStatus())) {
                    leave++;
                } else {
                    normal++;
                }
            }
        }
        long absent = Math.max(recentTasks.size() - present, 0);

        long pendingLeaves = leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, student.getId())
                .eq(LeaveRequest::getStatus, "PENDING"));

        List<Map<String, Object>> announcements = announcementsOfStudent(student, 5);

        List<SignRecordVO> recentRecords = allEligible.stream()
                .filter(t -> recordMap.containsKey(t.getId()))
                .sorted(Comparator.comparing(SignTask::getStartTime).reversed())
                .limit(6)
                .map(t -> recordToVO(recordMap.get(t.getId()), t))
                .collect(Collectors.toList());

        Map<String, Object> attendance = new LinkedHashMap<>();
        attendance.put("total", recentTasks.size());
        attendance.put("present", present);
        attendance.put("normal", normal);
        attendance.put("late", late);
        attendance.put("leave", leave);
        attendance.put("absent", absent);
        attendance.put("rate", Utils.rate(present, recentTasks.size()));

        Map<String, Object> me = new LinkedHashMap<>();
        me.put("id", student.getId());
        me.put("studentNo", student.getStudentNo());
        me.put("name", student.getName());
        me.put("className", access.className(student.getClassId()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("student", me);
        result.put("today", today.toString());
        result.put("ongoingTask", ongoing);
        result.put("upcomingTask", upcoming);
        result.put("todayTasks", todayTasks);
        result.put("attendance7d", attendance);
        result.put("pendingLeaves", pendingLeaves);
        result.put("recentRecords", recentRecords);
        result.put("announcements", announcements);
        return result;
    }

    public PageResult<SignRecordVO> myRecords(String status, Long courseId, LocalDate from, LocalDate to,
                                              long page, long size) {
        Student student = access.requireStudent(SecurityUtils.studentId());
        List<SignRecord> records = recordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getStudentId, student.getId())
                .orderByDesc(SignRecord::getSignTime));
        Map<Long, SignTask> taskMap = taskMapper.selectList(null).stream()
                .collect(Collectors.toMap(SignTask::getId, t -> t, (a, b) -> a));
        List<SignRecordVO> list = records.stream()
                .map(r -> {
                    SignTask task = taskMap.get(r.getTaskId());
                    return task == null ? null : recordToVO(r, task);
                })
                .filter(Objects::nonNull)
                .filter(vo -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || vo.getStatus().equalsIgnoreCase(status))
                .filter(vo -> courseId == null || courseId.equals(vo.getCourseId()))
                .filter(vo -> from == null || !vo.getSignTime().toLocalDate().isBefore(from))
                .filter(vo -> to == null || !vo.getSignTime().toLocalDate().isAfter(to))
                .toList();
        int fromIndex = (int) Math.min((page - 1) * size, list.size());
        int toIndex = (int) Math.min(fromIndex + size, list.size());
        return PageResult.of(list.subList(fromIndex, toIndex), list.size(), page, size);
    }

    public Map<String, Object> participation() {
        Student student = access.requireStudent(SecurityUtils.studentId());
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<SignTask> tasks = eligibleTasks(student).stream()
                .filter(t -> t.getStartTime().isAfter(since) && t.getEndTime().isBefore(LocalDateTime.now()))
                .toList();
        Map<Long, SignRecord> recordMap = recordMap(student.getId());
        long normal = 0;
        long late = 0;
        long leave = 0;
        for (SignTask t : tasks) {
            SignRecord r = recordMap.get(t.getId());
            if (r == null) {
                continue;
            }
            if ("LATE".equals(r.getStatus())) {
                late++;
            } else if ("LEAVE".equals(r.getStatus())) {
                leave++;
            } else {
                normal++;
            }
        }
        long total = tasks.size();

        List<RollCallRecord> rollCalls = rollCallRecordMapper.selectList(new LambdaQueryWrapper<RollCallRecord>()
                .eq(RollCallRecord::getStudentId, student.getId())
                .ge(RollCallRecord::getDrawnAt, since));
        long responded = rollCalls.stream().filter(r -> "RESPONDED".equals(r.getResult())).count();

        List<Long> courseIds = access.coursesOfStudent(student).stream().map(Course::getId).toList();
        List<Quiz> quizzes = courseIds.isEmpty() ? List.of() : quizMapper.selectList(new LambdaQueryWrapper<Quiz>()
                .in(Quiz::getCourseId, courseIds)
                .ge(Quiz::getStartTime, since)
                .le(Quiz::getEndTime, LocalDateTime.now()));
        long answered = quizzes.isEmpty() ? 0
                : quizAnswerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getStudentId, student.getId())
                .in(QuizAnswer::getQuizId, quizzes.stream().map(Quiz::getId).toList()));

        long races = raceRecordMapper.selectCount(new LambdaQueryWrapper<RaceRecord>()
                .eq(RaceRecord::getStudentId, student.getId())
                .ge(RaceRecord::getCreateTime, since));

        double signWeight = configService.getDouble("participation.sign.weight", 40);
        double lateWeight = configService.getDouble("participation.late.weight", 20);
        double rollWeight = configService.getDouble("participation.rollcall.weight", 25);
        double quizWeight = configService.getDouble("participation.quiz.weight", 10);
        double raceWeight = configService.getDouble("participation.race.weight", 5);
        double totalWeight = signWeight + lateWeight + rollWeight + quizWeight + raceWeight;
        if (totalWeight <= 0) {
            totalWeight = 100;
        }

        double signScore = total == 0 ? 100 : (normal + leave) * 100.0 / total;
        double lateScore = total == 0 ? 100 : late * 100.0 / total;
        double rollScore = rollCalls.isEmpty() ? 100 : responded * 100.0 / rollCalls.size();
        double quizScore = quizzes.isEmpty() ? 100 : answered * 100.0 / quizzes.size();
        double raceScore = races == 0 ? 0 : Math.min(100, races * 25.0);

        double score = (signScore * signWeight + lateScore * lateWeight + rollScore * rollWeight
                + quizScore * quizWeight + raceScore * raceWeight) / totalWeight;

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item("签到出勤", round(signScore), signWeight, normal + leave + " / " + total + " 次"));
        items.add(item("迟到控制", round(lateScore), lateWeight, late + " 次迟到"));
        items.add(item("点名响应", round(rollScore), rollWeight, responded + " / " + rollCalls.size() + " 次"));
        items.add(item("小测完成", round(quizScore), quizWeight, answered + " / " + quizzes.size() + " 次"));
        items.add(item("抢答参与", round(raceScore), raceWeight, races + " 次"));

        List<Map<String, Object>> sources = new ArrayList<>();
        rollCalls.forEach(r -> {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type", "ROLL_CALL");
            s.put("title", "课堂点名");
            s.put("detail", "结果：" + TaskService.statusLabel(r.getResult())
                    + (r.getPerformance() == null ? "" : "，表现：" + r.getPerformance()));
            s.put("time", r.getDrawnAt());
            sources.add(s);
        });
        recordMap.forEach((taskId, r) -> {
            tasks.stream().filter(t -> t.getId().equals(taskId)).findFirst().ifPresent(t -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("type", "SIGN");
                s.put("title", t.getTitle());
                s.put("detail", "签到状态：" + TaskService.statusLabel(r.getStatus()));
                s.put("time", r.getSignTime());
                sources.add(s);
            });
        });
        sources.sort((a, b) -> ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", round(score));
        result.put("items", items);
        result.put("sources", sources.stream().limit(40).toList());
        result.put("range", "最近 30 天");
        return result;
    }

    public List<Map<String, Object>> calendar(LocalDate weekStart) {
        Student student = access.requireStudent(SecurityUtils.studentId());
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(7).atStartOfDay();
        List<Course> courses = access.coursesOfStudent(student);
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        LambdaQueryWrapper<SignTask> taskWrapper = new LambdaQueryWrapper<>();
        if (!courseIds.isEmpty()) {
            taskWrapper.and(w -> w.eq(SignTask::getClassId, student.getClassId())
                    .or(n -> n.isNull(SignTask::getClassId).in(SignTask::getCourseId, courseIds)));
        } else {
            taskWrapper.eq(SignTask::getClassId, student.getClassId());
        }
        taskWrapper.ge(SignTask::getStartTime, from).lt(SignTask::getStartTime, to);

        List<Map<String, Object>> events = new ArrayList<>();
        for (SignTask task : taskMapper.selectList(taskWrapper)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", "task-" + task.getId());
            e.put("type", "TASK");
            e.put("title", task.getTitle());
            e.put("start", task.getStartTime());
            e.put("end", task.getEndTime());
            e.put("courseId", task.getCourseId());
            e.put("courseName", access.courseName(task.getCourseId()));
            e.put("status", TaskStatus.of(task, LocalDateTime.now()).name());
            e.put("link", "/student/tasks/" + task.getId());
            events.add(e);
        }
        if (!courseIds.isEmpty()) {
            List<Quiz> quizzes = quizMapper.selectList(new LambdaQueryWrapper<Quiz>()
                    .in(Quiz::getCourseId, courseIds)
                    .ge(Quiz::getStartTime, from).lt(Quiz::getStartTime, to));
            quizzes.forEach(quiz -> {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", "quiz-" + quiz.getId());
                e.put("type", "QUIZ");
                e.put("title", quiz.getTitle());
                e.put("start", quiz.getStartTime());
                e.put("end", quiz.getEndTime());
                e.put("courseId", quiz.getCourseId());
                e.put("courseName", access.courseName(quiz.getCourseId()));
                e.put("status", quiz.getStatus());
                e.put("link", "/student/interaction");
                events.add(e);
            });
            List<ClassAnnouncement> announcements = announcementMapper.selectList(new LambdaQueryWrapper<ClassAnnouncement>()
                    .in(ClassAnnouncement::getCourseId, courseIds)
                    .ge(ClassAnnouncement::getCreateTime, from).lt(ClassAnnouncement::getCreateTime, to));
            announcements.forEach(a -> {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", "ann-" + a.getId());
                e.put("type", "ANNOUNCEMENT");
                e.put("title", a.getContent().length() > 30 ? a.getContent().substring(0, 30) + "..." : a.getContent());
                e.put("start", a.getCreateTime());
                e.put("end", a.getCreateTime());
                e.put("courseId", a.getCourseId());
                e.put("courseName", access.courseName(a.getCourseId()));
                e.put("pinned", a.getPinned());
                e.put("link", "/student/calendar");
                events.add(e);
            });
        }
        events.sort(Comparator.comparing(e -> (LocalDateTime) e.get("start")));
        return events;
    }

    public List<Course> myCourses() {
        Student student = access.requireStudent(SecurityUtils.studentId());
        return access.coursesOfStudent(student);
    }

    public List<Map<String, Object>> announcements() {
        Student student = access.requireStudent(SecurityUtils.studentId());
        return announcementsOfStudent(student, 30);
    }

    private List<Map<String, Object>> announcementsOfStudent(Student student, int limit) {
        List<Course> courses = access.coursesOfStudent(student);
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<ClassAnnouncement> list = announcementMapper.selectList(new LambdaQueryWrapper<ClassAnnouncement>()
                .in(ClassAnnouncement::getCourseId, courseIds)
                .orderByDesc(ClassAnnouncement::getPinned)
                .orderByDesc(ClassAnnouncement::getCreateTime)
                .last("limit " + limit));
        return list.stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("courseId", a.getCourseId());
            map.put("courseName", access.courseName(a.getCourseId()));
            map.put("content", a.getContent());
            map.put("pinned", a.getPinned());
            map.put("createTime", a.getCreateTime());
            map.put("expireAt", a.getExpireAt());
            return map;
        }).toList();
    }

    private List<SignTask> eligibleTasks(Student student) {
        List<Course> courses = access.coursesOfStudent(student);
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        LambdaQueryWrapper<SignTask> wrapper = new LambdaQueryWrapper<>();
        if (!courseIds.isEmpty()) {
            wrapper.and(w -> w.eq(SignTask::getClassId, student.getClassId())
                    .or(n -> n.isNull(SignTask::getClassId).in(SignTask::getCourseId, courseIds)));
        } else {
            wrapper.eq(SignTask::getClassId, student.getClassId());
        }
        wrapper.orderByAsc(SignTask::getStartTime);
        return taskMapper.selectList(wrapper);
    }

    private Map<Long, SignRecord> recordMap(Long studentId) {
        Map<Long, SignRecord> map = new HashMap<>();
        recordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                        .eq(SignRecord::getStudentId, studentId))
                .forEach(r -> map.put(r.getTaskId(), r));
        return map;
    }

    private SignRecordVO recordToVO(SignRecord record, SignTask task) {
        SignRecordVO vo = new SignRecordVO();
        vo.setId(record.getId());
        vo.setTaskId(record.getTaskId());
        vo.setTaskTitle(task.getTitle());
        vo.setCourseId(task.getCourseId());
        vo.setStudentId(record.getStudentId());
        vo.setCourseName(access.courseName(task.getCourseId()));
        vo.setSignTime(record.getSignTime());
        vo.setStatus(record.getStatus());
        vo.setStatusLabel(TaskService.statusLabel(record.getStatus()));
        vo.setMethod(record.getMethod());
        vo.setMethodLabel(TaskService.methodLabel(record.getMethod()));
        vo.setLongitude(record.getLongitude());
        vo.setLatitude(record.getLatitude());
        vo.setDistanceMeters(record.getDistanceMeters());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private Map<String, Object> item(String name, double score, double weight, String detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("score", score);
        map.put("weight", weight);
        map.put("detail", detail);
        return map;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
