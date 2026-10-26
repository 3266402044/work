package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.SecurityUtils;
import com.classpulse.common.TaskStatus;
import com.classpulse.common.Utils;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherPortalService {
    private final AccessService access;
    private final TaskService taskService;
    private final SignTaskMapper taskMapper;
    private final SignRecordMapper recordMapper;
    private final StudentMapper studentMapper;
    private final LeaveRequestMapper leaveMapper;
    private final ConfigService configService;

    public Map<String, Object> dashboard() {
        Long teacherId = SecurityUtils.teacherId();
        List<Course> courses = access.coursesOfTeacher(teacherId);
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        LocalDate today = LocalDate.now();

        List<SignTask> myTasks = courseIds.isEmpty() ? List.of()
                : taskMapper.selectList(new LambdaQueryWrapper<SignTask>()
                .in(SignTask::getCourseId, courseIds)
                .orderByDesc(SignTask::getStartTime));

        List<SignTask> todayTasks = myTasks.stream()
                .filter(t -> t.getStartTime().toLocalDate().equals(today)).toList();
        List<TaskVO> ongoing = myTasks.stream()
                .filter(t -> TaskStatus.of(t, LocalDateTime.now()) == TaskStatus.ONGOING)
                .map(t -> taskService.toVO(t, null)).toList();

        List<Long> taskIds = myTasks.stream().map(SignTask::getId).toList();
        long pendingLeaves = taskIds.isEmpty() ? 0 : leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getTaskId, taskIds).eq(LeaveRequest::getStatus, "PENDING"));

        long totalStudents = myTasks.stream()
                .mapToLong(t -> access.roster(t).size()).sum();
        long totalSigned = taskIds.isEmpty() ? 0 : recordMapper.selectCount(new LambdaQueryWrapper<SignRecord>()
                .in(SignRecord::getTaskId, taskIds));
        double attendanceRate = Utils.rate(totalSigned, totalStudents);

        List<Map<String, Object>> trend = myTasks.stream()
                .filter(t -> !t.getStartTime().isAfter(LocalDateTime.now()))
                .limit(7)
                .sorted(Comparator.comparing(SignTask::getStartTime))
                .map(t -> {
                    TaskVO vo = taskService.toVO(t, null);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", t.getStartTime().toLocalDate() + " " + t.getTitle());
                    item.put("rate", vo.getAttendanceRate());
                    item.put("signed", vo.getSignedCount());
                    item.put("total", vo.getTotalStudents());
                    item.put("date", t.getStartTime());
                    return item;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> todayCourses = courses.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("classroom", c.getClassroom());
            map.put("color", c.getCoverColor());
            map.put("studentCount", access.courseStudents(c.getId()).size());
            map.put("taskCountToday", myTasks.stream().filter(t -> c.getId().equals(t.getCourseId())
                    && t.getStartTime().toLocalDate().equals(today)).count());
            return map;
        }).toList();

        List<Map<String, Object>> warnings = warningsForCourses(courses);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("todayCourses", courses.size());
        metrics.put("todayTasks", todayTasks.size());
        metrics.put("ongoingTasks", ongoing.size());
        metrics.put("pendingLeaves", pendingLeaves);
        metrics.put("attendanceRate", attendanceRate);
        metrics.put("totalSigned", totalSigned);
        metrics.put("totalStudents", totalStudents);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metrics", metrics);
        result.put("courses", todayCourses);
        result.put("ongoingTasks", ongoing);
        result.put("trend", trend);
        result.put("warnings", warnings);
        result.put("date", today.toString());
        return result;
    }

    public Map<String, Object> courseStatistics(Long courseId, Long classId, LocalDate from, LocalDate to) {
        access.assertCourseManageable(courseId);
        Course course = access.requireCourse(courseId);
        List<Student> students = access.courseStudents(courseId);
        if (classId != null) {
            students = students.stream().filter(s -> classId.equals(s.getClassId())).toList();
        }
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());

        LambdaQueryWrapper<SignTask> wrapper = new LambdaQueryWrapper<SignTask>()
                .eq(SignTask::getCourseId, courseId)
                .orderByAsc(SignTask::getStartTime);
        if (from != null) {
            wrapper.ge(SignTask::getStartTime, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(SignTask::getStartTime, to.plusDays(1).atStartOfDay());
        }
        if (classId != null) {
            wrapper.eq(SignTask::getClassId, classId);
        }
        List<SignTask> tasks = taskMapper.selectList(wrapper);
        List<Long> taskIds = tasks.stream().map(SignTask::getId).toList();
        List<SignRecord> records = taskIds.isEmpty() ? List.of()
                : recordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .in(SignRecord::getTaskId, taskIds).in(SignRecord::getStudentId, studentIds));

        Map<Long, List<SignRecord>> byStudent = records.stream()
                .collect(Collectors.groupingBy(SignRecord::getStudentId));
        Map<Long, List<SignRecord>> byTask = records.stream()
                .collect(Collectors.groupingBy(SignRecord::getTaskId));

        List<Map<String, Object>> taskStats = new ArrayList<>();
        long totalSigned = 0;
        long totalLate = 0;
        long totalExpected = 0;
        for (SignTask task : tasks) {
            List<SignRecord> taskRecords = byTask.getOrDefault(task.getId(), List.of());
            long late = taskRecords.stream().filter(r -> "LATE".equals(r.getStatus())).count();
            long leave = taskRecords.stream().filter(r -> "LEAVE".equals(r.getStatus())).count();
            int expected = task.getClassId() == null ? students.size()
                    : (int) students.stream().filter(s -> task.getClassId().equals(s.getClassId())).count();
            totalSigned += taskRecords.size();
            totalLate += late;
            totalExpected += expected;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", task.getId());
            item.put("title", task.getTitle());
            item.put("startTime", task.getStartTime());
            item.put("status", TaskStatus.of(task, LocalDateTime.now()).name());
            item.put("expected", expected);
            item.put("signed", taskRecords.size());
            item.put("late", late);
            item.put("leave", leave);
            item.put("absent", Math.max(expected - taskRecords.size(), 0));
            item.put("rate", Utils.rate(taskRecords.size(), expected));
            taskStats.add(item);
        }

        List<Map<String, Object>> studentStats = new ArrayList<>();
        for (Student student : students) {
            List<SignRecord> list = byStudent.getOrDefault(student.getId(), List.of());
            long normal = list.stream().filter(r -> "NORMAL".equals(r.getStatus()) || "MANUAL".equals(r.getStatus())).count();
            long late = list.stream().filter(r -> "LATE".equals(r.getStatus())).count();
            long leave = list.stream().filter(r -> "LEAVE".equals(r.getStatus())).count();
            long absent = Math.max(tasks.size() - list.size(), 0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", student.getId());
            item.put("studentNo", student.getStudentNo());
            item.put("name", student.getName());
            item.put("className", access.className(student.getClassId()));
            item.put("normal", normal);
            item.put("late", late);
            item.put("leave", leave);
            item.put("absent", absent);
            item.put("total", tasks.size());
            item.put("rate", Utils.rate(list.size(), tasks.size()));
            item.put("lateRate", Utils.rate(late, Math.max(list.size(), 1)));
            studentStats.add(item);
        }
        studentStats.sort(Comparator.comparingDouble(m -> (double) m.get("rate")));

        List<Map<String, Object>> warnings = buildWarnings(tasks, students, byStudent);

        List<Map<String, Object>> trend = taskStats.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("label", ((LocalDateTime) t.get("startTime")).toLocalDate().toString());
            map.put("rate", t.get("rate"));
            map.put("signed", t.get("signed"));
            map.put("total", t.get("expected"));
            return map;
        }).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("taskCount", tasks.size());
        summary.put("studentCount", students.size());
        summary.put("attendanceRate", Utils.rate(totalSigned, totalExpected));
        summary.put("lateRate", Utils.rate(totalLate, Math.max(totalSigned, 1)));
        summary.put("absentCount", Math.max(totalExpected - totalSigned, 0));
        summary.put("warningCount", warnings.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("course", course);
        result.put("classes", access.classesOfCourse(courseId));
        result.put("summary", summary);
        result.put("trend", trend);
        result.put("taskStats", taskStats);
        result.put("studentStats", studentStats);
        result.put("warnings", warnings);
        return result;
    }

    public List<Map<String, Object>> warnings() {
        Long teacherId = SecurityUtils.teacherId();
        return warningsForCourses(access.coursesOfTeacher(teacherId));
    }

    private List<Map<String, Object>> warningsForCourses(List<Course> courses) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (courses.isEmpty()) {
            return result;
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<SignTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<SignTask>()
                .in(SignTask::getCourseId, courseIds)
                .le(SignTask::getEndTime, LocalDateTime.now())
                .orderByAsc(SignTask::getStartTime));
        List<Student> students = studentMapper.selectList(null);
        List<Long> taskIds = tasks.stream().map(SignTask::getId).toList();
        List<SignRecord> records = taskIds.isEmpty() ? List.of()
                : recordMapper.selectList(new LambdaQueryWrapper<SignRecord>().in(SignRecord::getTaskId, taskIds));
        Map<Long, List<SignRecord>> byStudent = records.stream()
                .collect(Collectors.groupingBy(SignRecord::getStudentId));
        return buildWarnings(tasks, students, byStudent);
    }

    private List<Map<String, Object>> buildWarnings(List<SignTask> tasks, List<Student> students,
                                                    Map<Long, List<SignRecord>> byStudent) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        int absenceThreshold = configService.getInt("rollcall.absence.warning", 3);
        double lateThreshold = configService.getDouble("late.rate.warning", 30);
        for (Student student : students) {
            List<SignRecord> records = byStudent.getOrDefault(student.getId(), List.of());
            Set<Long> signedTaskIds = records.stream().map(SignRecord::getTaskId).collect(Collectors.toSet());
            int consecutive = 0;
            int maxConsecutive = 0;
            for (SignTask task : tasks) {
                if (task.getClassId() != null && !task.getClassId().equals(student.getClassId())) {
                    continue;
                }
                if (signedTaskIds.contains(task.getId())) {
                    consecutive = 0;
                } else {
                    consecutive++;
                    maxConsecutive = Math.max(maxConsecutive, consecutive);
                }
            }
            long late = records.stream().filter(r -> "LATE".equals(r.getStatus())).count();
            double lateRate = records.isEmpty() ? 0 : late * 100.0 / records.size();
            if (maxConsecutive >= absenceThreshold || lateRate > lateThreshold) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("studentId", student.getId());
                map.put("studentNo", student.getStudentNo());
                map.put("name", student.getName());
                map.put("className", access.className(student.getClassId()));
                map.put("consecutiveAbsences", maxConsecutive);
                map.put("lateRate", Math.round(lateRate * 10.0) / 10.0);
                List<String> reasons = new ArrayList<>();
                if (maxConsecutive >= absenceThreshold) {
                    reasons.add("连续 " + maxConsecutive + " 次未签到");
                }
                if (lateRate > lateThreshold) {
                    reasons.add("迟到率 " + Math.round(lateRate) + "%");
                }
                map.put("reasons", reasons);
                warnings.add(map);
            }
        }
        return warnings;
    }

    public Map<String, Object> classRoster(Long classId) {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId).orderByAsc(Student::getStudentNo));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("className", access.className(classId));
        map.put("students", students);
        map.put("total", students.size());
        return map;
    }
}
