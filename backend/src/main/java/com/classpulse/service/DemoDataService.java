package com.classpulse.service;

import com.classpulse.entity.ClassAnnouncement;
import com.classpulse.entity.ClassGroup;
import com.classpulse.entity.Course;
import com.classpulse.entity.CourseClass;
import com.classpulse.entity.LeaveRequest;
import com.classpulse.entity.Quiz;
import com.classpulse.entity.QuizAnswer;
import com.classpulse.entity.RaceRecord;
import com.classpulse.entity.RaceSession;
import com.classpulse.entity.RollCallRecord;
import com.classpulse.entity.RollCallSession;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.entity.SysNotification;
import com.classpulse.entity.SysUser;
import com.classpulse.entity.Teacher;
import com.classpulse.mapper.ClassAnnouncementMapper;
import com.classpulse.mapper.ClassGroupMapper;
import com.classpulse.mapper.CourseClassMapper;
import com.classpulse.mapper.CourseMapper;
import com.classpulse.mapper.LeaveRequestMapper;
import com.classpulse.mapper.QuizAnswerMapper;
import com.classpulse.mapper.QuizMapper;
import com.classpulse.mapper.RaceRecordMapper;
import com.classpulse.mapper.RaceSessionMapper;
import com.classpulse.mapper.RollCallRecordMapper;
import com.classpulse.mapper.RollCallSessionMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.mapper.SysNotificationMapper;
import com.classpulse.mapper.SysUserMapper;
import com.classpulse.mapper.TeacherMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {
    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ClassGroupMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final SignTaskMapper taskMapper;
    private final SignRecordMapper recordMapper;
    private final LeaveRequestMapper leaveMapper;
    private final RollCallSessionMapper rollSessionMapper;
    private final RollCallRecordMapper rollRecordMapper;
    private final QuizMapper quizMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final ClassAnnouncementMapper announcementMapper;
    private final SysNotificationMapper notificationMapper;
    private final RaceSessionMapper raceSessionMapper;
    private final RaceRecordMapper raceRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void ensureSeed() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        seed();
    }

    @Transactional
    public void reset() {
        List<String> tables = List.of("race_record", "race_session", "sys_notification", "class_announcement",
                "quiz_answer", "quiz", "roll_call_record", "roll_call_session", "sign_qr_token", "leave_request",
                "sign_record", "sign_task", "course_class", "course", "student", "teacher", "class_group",
                "operation_log", "sys_config", "sys_user");
        tables.forEach(table -> jdbcTemplate.execute("DELETE FROM " + table));
        seed();
        log.info("demo data reset completed");
    }

    private void seed() {
        LocalDateTime now = LocalDateTime.now();

        user("admin", "Admin@123", "ADMIN", "系统管理员", "13800000000");

        ClassGroup c1 = classGroup("计算机科学与技术 2301", "2023", "计算机科学与技术");
        ClassGroup c2 = classGroup("软件工程 2302", "2023", "软件工程");

        Teacher t1 = teacher("T2023001", "王启明", "数学与统计学院", "teacher01", "Teacher@123", "13800000001");
        Teacher t2 = teacher("T2023002", "李思远", "外国语学院", "teacher02", "Teacher@123", "13800000002");

        Student s1 = student("2023010101", "张明", c1.getId(), "student01");
        Student s2 = student("2023010102", "李婉", c1.getId(), "student02");
        Student s3 = student("2023010103", "陈昊", c1.getId(), "student03");
        Student s4 = student("2023010104", "赵磊", c1.getId(), "student04");
        Student s5 = student("2023010105", "周晴", c1.getId(), "student05");
        Student s6 = student("2023010106", "孙浩", c1.getId(), "student06");
        Student s7 = student("2023020201", "吴迪", c2.getId(), "student07");
        Student s8 = student("2023020202", "郑爽", c2.getId(), "student08");

        Course math = course("MATH101", "高等数学", t1.getId(), "2026-2027-1", "4.0", "#38DDF8",
                "教学楼 A-302", new BigDecimal("118.1234567"), new BigDecimal("32.1234567"));
        Course ds = course("CS201", "数据结构", t1.getId(), "2026-2027-1", "3.5", "#2DD4BF",
                "教学楼 B-105", new BigDecimal("118.1245000"), new BigDecimal("32.1240000"));
        Course english = course("ENG102", "大学英语", t2.getId(), "2026-2027-1", "2.0", "#F5B942",
                "教学楼 C-201", new BigDecimal("118.1250000"), new BigDecimal("32.1250000"));
        courseClass(math.getId(), c1.getId());
        courseClass(ds.getId(), c1.getId());
        courseClass(ds.getId(), c2.getId());
        courseClass(english.getId(), c1.getId());

        // 1. 进行中的签到任务
        SignTask ongoing = task("高等数学 · 第 3 次签到", t1.getId(), math.getId(), c1.getId(),
                now.minusMinutes(10), now.plusMinutes(180), now.plusMinutes(30),
                "CLICK,LOCATION,PASSCODE", "教学楼 A-302",
                new BigDecimal("118.1234567"), new BigDecimal("32.1234567"), 150, "123456");
        record(ongoing.getId(), s1.getId(), "NORMAL", "CLICK", now.minusMinutes(8), null, null);

        // 2. 未开始的签到任务
        task("数据结构 · 第 4 次签到", t1.getId(), ds.getId(), c1.getId(),
                now.plusHours(2), now.plusHours(2).plusMinutes(20), now.plusHours(2).plusMinutes(5),
                "CLICK,QR", "教学楼 B-105",
                new BigDecimal("118.1245000"), new BigDecimal("32.1240000"), 150, null);

        // 3. 已结束的签到任务
        LocalDateTime yesterday = now.minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        SignTask ended = task("大学英语 · 第 2 次签到", t2.getId(), english.getId(), c1.getId(),
                yesterday, yesterday.plusMinutes(15), yesterday.plusMinutes(5),
                "CLICK", "教学楼 C-201", null, null, 150, null);
        record(ended.getId(), s1.getId(), "NORMAL", "CLICK", yesterday.plusMinutes(2), null, null);
        record(ended.getId(), s2.getId(), "LATE", "CLICK", yesterday.plusMinutes(9), null, null);
        record(ended.getId(), s3.getId(), "LEAVE", "MANUAL", yesterday.plusMinutes(1), "病假，已提交证明", null);
        leave(s3.getId(), ended.getId(), "发烧就医", "APPROVED", yesterday.minusHours(2), "已核实就医记录");

        // 历史任务与签到记录，用于统计趋势
        List<Student> class1Students = List.of(s1, s2, s3, s4, s5, s6);
        List<Student> allStudents = List.of(s1, s2, s3, s4, s5, s6, s7, s8);
        Random random = new Random(20261024L);
        for (int day = 2; day <= 7; day++) {
            LocalDateTime start = now.minusDays(day).withHour(9).withMinute(0).withSecond(0).withNano(0);
            boolean isMath = day % 2 == 0;
            Course course = isMath ? math : ds;
            List<Student> roster = isMath ? class1Students : allStudents;
            SignTask past = task(course.getName() + " · 第 " + (8 - day) + " 次签到", t1.getId(), course.getId(),
                    isMath ? c1.getId() : null, start, start.plusMinutes(15), start.plusMinutes(5),
                    "CLICK", course.getClassroom(), null, null, 150, null);
            for (int i = 0; i < roster.size(); i++) {
                if (day == 4 && i == roster.size() - 1) {
                    continue; // 制造缺勤
                }
                if (day == 6 && i >= roster.size() - 2) {
                    continue;
                }
                String status = (i == 1 && day % 3 == 0) ? "LATE" : "NORMAL";
                LocalDateTime signTime = start.plusMinutes(status.equals("LATE") ? 8 : 1 + random.nextInt(3));
                record(past.getId(), roster.get(i).getId(), status, "CLICK", signTime, null, null);
            }
        }

        // 请假申请：进行中任务待审批
        leave(s3.getId(), ongoing.getId(), "参加校外竞赛，需请假一次", "PENDING", now.minusMinutes(20), null);

        // 进行中的随机点名会话
        RollCallSession running = rollSession(math.getId(), c1.getId(), t1.getId(), "ALL", 2,
                "DAYS7", 600, "RUNNING", now.minusMinutes(1), null);
        rollRecord(running.getId(), s2.getId(), 1, "PENDING", null, now.minusSeconds(30), null);
        rollRecord(running.getId(), s4.getId(), 2, "RESPONDED", "ACTIVE", now.minusSeconds(30), now.minusSeconds(22));

        // 已结束的点名会话
        RollCallSession finished = rollSession(math.getId(), c1.getId(), t1.getId(), "ALL", 3,
                "DAYS7", 30, "FINISHED", now.minusDays(1).withHour(11).withMinute(0), now.minusDays(1).withHour(11).withMinute(6));
        rollRecord(finished.getId(), s1.getId(), 1, "RESPONDED", "EXCELLENT",
                now.minusDays(1).withHour(11).withMinute(1), now.minusDays(1).withHour(11).withMinute(1).plusSeconds(8));
        rollRecord(finished.getId(), s2.getId(), 2, "NO_RESPONSE", null,
                now.minusDays(1).withHour(11).withMinute(1), null);
        rollRecord(finished.getId(), s3.getId(), 3, "SKIPPED", "NORMAL",
                now.minusDays(1).withHour(11).withMinute(1), null);

        // 进行中的课堂小测
        Quiz quiz = quiz(math.getId(), t1.getId(), "极限与连续 · 随堂小测", "SINGLE",
                List.of("A. 0", "B. 1", "C. ∞", "D. 不存在"), List.of(1),
                now.minusMinutes(1), now.plusMinutes(40), false,
                "重要极限：lim(x→0) sinx/x = 1。");
        quizAnswer(quiz.getId(), s2.getId(), List.of(1), true, now.minusSeconds(40));
        quizAnswer(quiz.getId(), s3.getId(), List.of(0), false, now.minusSeconds(30));
        quizAnswer(quiz.getId(), s4.getId(), List.of(1), true, now.minusSeconds(20));

        // 已结束的小测
        Quiz endedQuiz = quiz(ds.getId(), t1.getId(), "二叉树遍历 · 课堂投票", "SINGLE",
                List.of("前序遍历", "中序遍历", "后序遍历", "层序遍历"), List.of(1),
                now.minusDays(1).withHour(14).withMinute(0), now.minusDays(1).withHour(14).withMinute(5), true,
                "中序遍历二叉搜索树可得到有序序列。");
        quizAnswer(endedQuiz.getId(), s1.getId(), List.of(1), true, now.minusDays(1).withHour(14).withMinute(1));
        quizAnswer(endedQuiz.getId(), s2.getId(), List.of(0), false, now.minusDays(1).withHour(14).withMinute(2));

        // 课堂抢答（已结束，用于互动记录）
        RaceSession race = new RaceSession();
        race.setCourseId(math.getId());
        race.setTeacherId(t1.getId());
        race.setTitle("课堂抢答：导数定义");
        race.setStartTime(now.minusDays(2).withHour(10).withMinute(10));
        race.setEndTime(now.minusDays(2).withHour(10).withMinute(10).plusSeconds(30));
        race.setStatus("ENDED");
        race.setCreateTime(now.minusDays(2).withHour(10).withMinute(9));
        raceSessionMapper.insert(race);
        raceRecord(race.getId(), s2.getId(), 1, now.minusDays(2).withHour(10).withMinute(10).plusSeconds(4));
        raceRecord(race.getId(), s4.getId(), 2, now.minusDays(2).withHour(10).withMinute(10).plusSeconds(6));
        raceRecord(race.getId(), s1.getId(), 3, now.minusDays(2).withHour(10).withMinute(10).plusSeconds(9));

        // 公告
        announcement(math.getId(), t1.getId(), "下周三将进行第二章随堂测验，请提前复习极限与连续章节。", true, now.plusDays(7));
        announcement(math.getId(), t1.getId(), "本周作业提交截止时间为周日 22:00，请按时提交。", false, now.plusDays(3));
        announcement(ds.getId(), t1.getId(), "实验课请自带笔记本电脑，并提前安装好 JDK 21。", true, now.plusDays(5));

        // 消息
        notification(s1.getUserId(), "签到即将开始", "《高等数学》第 3 次签到正在进行，请及时完成签到。", "TASK", ongoing.getId());
        notification(s1.getUserId(), "点名提醒", "你在《高等数学》课堂被点到，请准备回答问题。", "ROLL_CALL", finished.getId());
        notification(s2.getUserId(), "签到提醒", "《高等数学》第 3 次签到正在进行，请及时完成签到。", "TASK", ongoing.getId());

        log.info("demo data seeded: students={}, courses={}, tasks={}", 8, 3, 9);
    }

    /* ===================== 构造辅助 ===================== */

    private SysUser user(String username, String password, String role, String realName, String phone) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEnabled(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private ClassGroup classGroup(String name, String grade, String major) {
        ClassGroup group = new ClassGroup();
        group.setName(name);
        group.setGrade(grade);
        group.setMajor(major);
        group.setCreateTime(LocalDateTime.now());
        group.setDeleted(0);
        classMapper.insert(group);
        return group;
    }

    private Teacher teacher(String no, String name, String department, String username, String password, String phone) {
        SysUser user = user(username, password, "TEACHER", name, phone);
        Teacher teacher = new Teacher();
        teacher.setUserId(user.getId());
        teacher.setTeacherNo(no);
        teacher.setName(name);
        teacher.setDepartment(department);
        teacherMapper.insert(teacher);
        user.setRelatedId(teacher.getId());
        userMapper.updateById(user);
        return teacher;
    }

    private Student student(String no, String name, Long classId, String username) {
        SysUser user = user(username, "Student@123", "STUDENT", name, "139" + no.substring(no.length() - 8));
        Student student = new Student();
        student.setStudentNo(no);
        student.setName(name);
        student.setClassId(classId);
        student.setUserId(user.getId());
        student.setCreateTime(LocalDateTime.now());
        studentMapper.insert(student);
        user.setRelatedId(student.getId());
        userMapper.updateById(user);
        return student;
    }

    private Course course(String no, String name, Long teacherId, String semester, String credit, String color,
                          String classroom, BigDecimal lon, BigDecimal lat) {
        Course course = new Course();
        course.setCourseNo(no);
        course.setName(name);
        course.setTeacherId(teacherId);
        course.setSemester(semester);
        course.setCredit(new BigDecimal(credit));
        course.setCoverColor(color);
        course.setClassroom(classroom);
        course.setLongitude(lon);
        course.setLatitude(lat);
        course.setDefaultRadius(150);
        courseMapper.insert(course);
        return course;
    }

    private void courseClass(Long courseId, Long classId) {
        CourseClass relation = new CourseClass();
        relation.setCourseId(courseId);
        relation.setClassId(classId);
        courseClassMapper.insert(relation);
    }

    private SignTask task(String title, Long teacherId, Long courseId, Long classId,
                          LocalDateTime start, LocalDateTime end, LocalDateTime late,
                          String modes, String locationName, BigDecimal lon, BigDecimal lat,
                          Integer radius, String passcode) {
        SignTask task = new SignTask();
        task.setTitle(title);
        task.setTeacherId(teacherId);
        task.setCourseId(courseId);
        task.setClassId(classId);
        task.setStartTime(start);
        task.setEndTime(end);
        task.setLateTime(late);
        task.setSignModes(modes);
        task.setLocationName(locationName);
        task.setLongitude(lon);
        task.setLatitude(lat);
        task.setRadiusMeters(radius);
        task.setPasscodeHash(passcode == null ? null : passwordEncoder.encode(passcode));
        task.setCreateTime(start.minusHours(1));
        taskMapper.insert(task);
        return task;
    }

    private SignRecord record(Long taskId, Long studentId, String status, String method,
                              LocalDateTime signTime, String remark, Long operatorId) {
        SignRecord record = new SignRecord();
        record.setTaskId(taskId);
        record.setStudentId(studentId);
        record.setStatus(status);
        record.setMethod(method);
        record.setSignTime(signTime);
        record.setRemark(remark);
        record.setOperatorId(operatorId);
        record.setCreateTime(signTime);
        recordMapper.insert(record);
        return record;
    }

    private void leave(Long studentId, Long taskId, String reason, String status,
                       LocalDateTime createTime, String remark) {
        LeaveRequest leave = new LeaveRequest();
        leave.setStudentId(studentId);
        leave.setTaskId(taskId);
        leave.setReason(reason);
        leave.setStatus(status);
        leave.setCreateTime(createTime);
        leave.setRemark(remark);
        if (!"PENDING".equals(status)) {
            leave.setReviewerId(1L);
            leave.setReviewTime(createTime.plusHours(1));
        }
        leaveMapper.insert(leave);
    }

    private RollCallSession rollSession(Long courseId, Long classId, Long teacherId, String scope, int count,
                                        String rule, int deadline, String status,
                                        LocalDateTime startedAt, LocalDateTime endedAt) {
        RollCallSession session = new RollCallSession();
        session.setCourseId(courseId);
        session.setClassId(classId);
        session.setTeacherId(teacherId);
        session.setCandidateScope(scope);
        session.setDrawCount(count);
        session.setAntiRepeatRule(rule);
        session.setExcludeLeave(1);
        session.setResponseDeadlineSeconds(deadline);
        session.setStatus(status);
        session.setSeed(Math.abs(new Random().nextLong() % 1_000_000_000L));
        session.setStartedAt(startedAt);
        session.setEndedAt(endedAt);
        rollSessionMapper.insert(session);
        return session;
    }

    private void rollRecord(Long sessionId, Long studentId, int order, String result, String performance,
                            LocalDateTime drawnAt, LocalDateTime respondedAt) {
        RollCallRecord record = new RollCallRecord();
        record.setSessionId(sessionId);
        record.setStudentId(studentId);
        record.setDrawOrder(order);
        record.setResult(result);
        record.setPerformance(performance);
        record.setDrawnAt(drawnAt);
        record.setRespondedAt(respondedAt);
        rollRecordMapper.insert(record);
    }

    private Quiz quiz(Long courseId, Long teacherId, String title, String type, List<String> options,
                      List<Integer> correct, LocalDateTime start, LocalDateTime end, boolean anonymous,
                      String explanation) {
        Quiz quiz = new Quiz();
        quiz.setCourseId(courseId);
        quiz.setTeacherId(teacherId);
        quiz.setTitle(title);
        quiz.setType(type);
        try {
            quiz.setOptionsJson(objectMapper.writeValueAsString(options));
        } catch (Exception e) {
            quiz.setOptionsJson("[]");
        }
        quiz.setCorrectAnswer(correct.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        quiz.setStartTime(start);
        quiz.setEndTime(end);
        quiz.setAnonymous(anonymous ? 1 : 0);
        quiz.setStatus(end.isBefore(LocalDateTime.now()) ? "ENDED" : "RUNNING");
        quiz.setExplanation(explanation);
        quiz.setCreateTime(start.minusMinutes(2));
        quizMapper.insert(quiz);
        return quiz;
    }

    private void quizAnswer(Long quizId, Long studentId, List<Integer> answers, boolean correct,
                            LocalDateTime time) {
        QuizAnswer answer = new QuizAnswer();
        answer.setQuizId(quizId);
        answer.setStudentId(studentId);
        try {
            answer.setAnswerJson(objectMapper.writeValueAsString(answers));
        } catch (Exception e) {
            answer.setAnswerJson("[]");
        }
        answer.setIsCorrect(correct ? 1 : 0);
        answer.setAnswerTime(time);
        quizAnswerMapper.insert(answer);
    }

    private void announcement(Long courseId, Long teacherId, String content, boolean pinned,
                              LocalDateTime expireAt) {
        ClassAnnouncement announcement = new ClassAnnouncement();
        announcement.setCourseId(courseId);
        announcement.setTeacherId(teacherId);
        announcement.setContent(content);
        announcement.setPinned(pinned ? 1 : 0);
        announcement.setExpireAt(expireAt);
        announcement.setCreateTime(LocalDateTime.now().minusHours(2));
        announcementMapper.insert(announcement);
    }

    private void notification(Long userId, String title, String content, String type, Long relatedId) {
        if (userId == null) {
            return;
        }
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setReadFlag(0);
        notification.setRelatedId(relatedId);
        notification.setCreateTime(LocalDateTime.now().minusMinutes(10));
        notificationMapper.insert(notification);
    }

    private void raceRecord(Long raceId, Long studentId, int seq, LocalDateTime time) {
        RaceRecord record = new RaceRecord();
        record.setRaceId(raceId);
        record.setStudentId(studentId);
        record.setSeq(seq);
        record.setCreateTime(time);
        raceRecordMapper.insert(record);
    }

    @SuppressWarnings("unused")
    private List<Student> emptyIfNull(List<Student> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
