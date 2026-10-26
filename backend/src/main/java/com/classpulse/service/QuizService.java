package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.dto.QuizCreateRequest;
import com.classpulse.dto.QuizSubmitRequest;
import com.classpulse.entity.Course;
import com.classpulse.entity.Quiz;
import com.classpulse.entity.QuizAnswer;
import com.classpulse.entity.Student;
import com.classpulse.mapper.QuizAnswerMapper;
import com.classpulse.mapper.QuizMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.vo.QuizVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizMapper quizMapper;
    private final QuizAnswerMapper answerMapper;
    private final StudentMapper studentMapper;
    private final AccessService access;
    private final NotificationService notificationService;
    private final OperationLogService logService;
    private final SseHub sseHub;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuizVO create(QuizCreateRequest request) {
        Course course = access.requireCourse(request.getCourseId());
        access.assertCourseManageable(course.getId());
        Long teacherId = SecurityUtils.isAdmin() ? course.getTeacherId() : SecurityUtils.teacherId();
        String type = request.getType().toUpperCase();
        if (!List.of("SINGLE", "MULTIPLE", "TRUE_FALSE").contains(type)) {
            throw BizException.invalid("不支持的小测题型");
        }
        List<String> options = request.getOptions() == null ? new ArrayList<>() : new ArrayList<>(request.getOptions());
        if ("TRUE_FALSE".equals(type) && options.isEmpty()) {
            options = new ArrayList<>(List.of("正确", "错误"));
        }
        if (options.size() < 2) {
            throw BizException.invalid("至少需要两个选项");
        }
        if (request.getCorrectAnswer() == null || request.getCorrectAnswer().isEmpty()) {
            throw BizException.invalid("请设置正确答案");
        }
        for (Integer index : request.getCorrectAnswer()) {
            if (index == null || index < 0 || index >= options.size()) {
                throw BizException.invalid("正确答案下标超出选项范围");
            }
        }

        Quiz quiz = new Quiz();
        quiz.setCourseId(course.getId());
        quiz.setTeacherId(teacherId);
        quiz.setTitle(request.getTitle().trim());
        quiz.setType(type);
        quiz.setOptionsJson(writeJson(options));
        quiz.setCorrectAnswer(request.getCorrectAnswer().stream().map(String::valueOf).collect(Collectors.joining(",")));
        quiz.setStartTime(LocalDateTime.now());
        quiz.setEndTime(LocalDateTime.now().plusSeconds(request.getDurationSeconds()));
        quiz.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        quiz.setStatus("RUNNING");
        quiz.setExplanation(request.getExplanation());
        quiz.setCreateTime(LocalDateTime.now());
        quizMapper.insert(quiz);

        access.courseStudents(course.getId()).forEach(student -> notificationService.send(student.getUserId(),
                "课堂小测开始", "《" + course.getName() + "》发布了新的课堂小测：" + quiz.getTitle(),
                "QUIZ", quiz.getId()));
        sseHub.broadcast("quiz:new", Map.of("quizId", quiz.getId(), "courseId", course.getId(),
                "title", quiz.getTitle(), "endTime", quiz.getEndTime()));
        logService.log("QUIZ", "CREATE", quiz.getId(), "发布课堂小测：" + quiz.getTitle());
        return toVO(quiz, null, true);
    }

    public List<QuizVO> list(Long courseId, LocalDate from, LocalDate to) {
        var user = SecurityUtils.current();
        LambdaQueryWrapper<Quiz> wrapper = new LambdaQueryWrapper<>();
        if ("STUDENT".equals(user.role())) {
            Student student = access.requireStudent(user.relatedId());
            List<Long> courseIds = access.coursesOfStudent(student).stream().map(Course::getId).toList();
            if (courseIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(Quiz::getCourseId, courseIds);
        }
        if (courseId != null) {
            wrapper.eq(Quiz::getCourseId, courseId);
        }
        if (from != null) {
            wrapper.ge(Quiz::getStartTime, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(Quiz::getStartTime, to.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Quiz::getCreateTime).last("limit 50");
        Long studentId = "STUDENT".equals(user.role()) ? user.relatedId() : null;
        boolean teacherView = !"STUDENT".equals(user.role());
        return quizMapper.selectList(wrapper).stream()
                .map(q -> toVO(q, studentId, teacherView))
                .toList();
    }

    public QuizVO detail(Long quizId) {
        Quiz quiz = requireQuiz(quizId);
        var user = SecurityUtils.current();
        Long studentId = null;
        boolean teacherView = !"STUDENT".equals(user.role());
        if ("STUDENT".equals(user.role())) {
            Student student = access.requireStudent(user.relatedId());
            assertStudentInCourse(student, quiz.getCourseId());
            studentId = student.getId();
        }
        return toVO(quiz, studentId, teacherView);
    }

    @Transactional
    public Map<String, Object> submit(Long quizId, QuizSubmitRequest request) {
        Student student = access.requireStudent(SecurityUtils.studentId());
        Quiz quiz = requireQuiz(quizId);
        assertStudentInCourse(student, quiz.getCourseId());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(quiz.getStartTime())) {
            throw BizException.invalid("小测尚未开始");
        }
        if (now.isAfter(quiz.getEndTime())) {
            throw BizException.invalid("小测已截止，无法提交");
        }
        List<String> options = readJson(quiz.getOptionsJson());
        List<Integer> answers = request.getAnswers().stream().filter(Objects::nonNull)
                .distinct().sorted().toList();
        if (answers.isEmpty()) {
            throw BizException.invalid("请至少选择一个答案");
        }
        if (answers.stream().anyMatch(i -> i < 0 || i >= options.size())) {
            throw BizException.invalid("答案选项不合法");
        }
        long existing = answerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quizId).eq(QuizAnswer::getStudentId, student.getId()));
        if (existing > 0) {
            throw BizException.conflict("你已提交过本次小测，不能重复作答");
        }
        List<Integer> correct = parseCorrect(quiz.getCorrectAnswer());
        boolean isCorrect = new java.util.HashSet<>(answers).equals(new java.util.HashSet<>(correct));

        QuizAnswer answer = new QuizAnswer();
        answer.setQuizId(quizId);
        answer.setStudentId(student.getId());
        answer.setAnswerJson(writeJson(answers));
        answer.setIsCorrect(isCorrect ? 1 : 0);
        answer.setAnswerTime(now);
        try {
            answerMapper.insert(answer);
        } catch (DuplicateKeyException ex) {
            throw BizException.conflict("你已提交过本次小测，不能重复作答");
        }
        sseHub.broadcast("quiz:update", Map.of("quizId", quizId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "提交成功");
        result.put("submitted", true);
        result.put("correct", now.isAfter(quiz.getEndTime()) ? isCorrect : null);
        return result;
    }

    public Map<String, Object> results(Long quizId) {
        Quiz quiz = requireQuiz(quizId);
        access.assertCourseManageable(quiz.getCourseId());
        List<QuizAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quizId));
        List<String> options = readJson(quiz.getOptionsJson());
        List<Integer> correct = parseCorrect(quiz.getCorrectAnswer());
        long[] counts = new long[options.size()];
        for (QuizAnswer answer : answers) {
            readJsonInts(answer.getAnswerJson()).forEach(i -> {
                if (i >= 0 && i < counts.length) {
                    counts[i]++;
                }
            });
        }
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i);
            item.put("option", options.get(i));
            item.put("count", counts[i]);
            item.put("correct", correct.contains(i));
            distribution.add(item);
        }
        long correctCount = answers.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
        List<Map<String, Object>> participants = new ArrayList<>();
        if (quiz.getAnonymous() == null || quiz.getAnonymous() == 0) {
            Map<Long, Student> students = answers.isEmpty() ? Map.of()
                    : studentMapper.selectBatchIds(answers.stream().map(QuizAnswer::getStudentId).toList())
                    .stream().collect(Collectors.toMap(Student::getId, s -> s));
            answers.forEach(a -> {
                Student s = students.get(a.getStudentId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("studentId", a.getStudentId());
                item.put("studentNo", s == null ? "" : s.getStudentNo());
                item.put("name", s == null ? "未知学生" : s.getName());
                item.put("answer", a.getAnswerJson());
                item.put("isCorrect", a.getIsCorrect() != null && a.getIsCorrect() == 1);
                item.put("answerTime", a.getAnswerTime());
                participants.add(item);
            });
            participants.sort(Comparator.comparing(m -> (LocalDateTime) m.get("answerTime")));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("answerCount", answers.size());
        summary.put("totalStudents", access.courseStudents(quiz.getCourseId()).size());
        summary.put("correctCount", correctCount);
        summary.put("correctRate", answers.isEmpty() ? 0.0
                : Math.round(correctCount * 1000.0 / answers.size()) / 10.0);
        summary.put("submissionRate", access.courseStudents(quiz.getCourseId()).isEmpty() ? 0.0
                : Math.round(answers.size() * 1000.0 / access.courseStudents(quiz.getCourseId()).size()) / 10.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("quiz", toVO(quiz, null, true));
        result.put("summary", summary);
        result.put("distribution", distribution);
        result.put("participants", participants);
        return result;
    }

    public List<Map<String, Object>> history(Long courseId, LocalDate from, LocalDate to) {
        Long teacherId = SecurityUtils.isAdmin() ? null : SecurityUtils.teacherId();
        LambdaQueryWrapper<Quiz> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(Quiz::getTeacherId, teacherId);
        }
        if (courseId != null) {
            wrapper.eq(Quiz::getCourseId, courseId);
        }
        if (from != null) {
            wrapper.ge(Quiz::getStartTime, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(Quiz::getStartTime, to.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Quiz::getCreateTime).last("limit 50");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Quiz quiz : quizMapper.selectList(wrapper)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", quiz.getId());
            map.put("courseId", quiz.getCourseId());
            map.put("courseName", access.courseName(quiz.getCourseId()));
            map.put("title", quiz.getTitle());
            map.put("type", quiz.getType());
            map.put("startTime", quiz.getStartTime());
            map.put("endTime", quiz.getEndTime());
            map.put("answerCount", answerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                    .eq(QuizAnswer::getQuizId, quiz.getId())));
            map.put("status", statusOf(quiz));
            result.add(map);
        }
        return result;
    }

    private QuizVO toVO(Quiz quiz, Long studentId, boolean teacherView) {
        QuizVO vo = new QuizVO();
        vo.setId(quiz.getId());
        vo.setCourseId(quiz.getCourseId());
        vo.setCourseName(access.courseName(quiz.getCourseId()));
        vo.setTeacherName(access.teacherName(quiz.getTeacherId()));
        vo.setTitle(quiz.getTitle());
        vo.setType(quiz.getType());
        vo.setOptions(readJson(quiz.getOptionsJson()));
        vo.setStartTime(quiz.getStartTime());
        vo.setEndTime(quiz.getEndTime());
        vo.setAnonymous(quiz.getAnonymous() != null && quiz.getAnonymous() == 1);
        vo.setStatus(statusOf(quiz));
        vo.setSecondsLeft(Math.max(0, Duration.between(LocalDateTime.now(), quiz.getEndTime()).getSeconds()));
        boolean ended = LocalDateTime.now().isAfter(quiz.getEndTime());
        boolean showResult = teacherView || ended;
        vo.setShowResult(showResult);
        if (showResult) {
            vo.setCorrectAnswer(parseCorrect(quiz.getCorrectAnswer()));
            vo.setExplanation(quiz.getExplanation());
        }
        long answerCount = answerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quiz.getId()));
        vo.setAnswerCount((int) answerCount);
        vo.setTotalStudents(access.courseStudents(quiz.getCourseId()).size());

        List<QuizAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getQuizId, quiz.getId()));
        if (studentId != null) {
            QuizAnswer mine = answers.stream().filter(a -> a.getStudentId().equals(studentId)).findFirst().orElse(null);
            vo.setAnswered(mine != null);
            if (mine != null) {
                vo.setMyAnswer(readJsonInts(mine.getAnswerJson()));
                vo.setCorrect(mine.getIsCorrect() != null && mine.getIsCorrect() == 1);
            }
        }
        if ("RUNNING".equals(vo.getStatus())) {
            long[] counts = new long[vo.getOptions().size()];
            answers.forEach(a -> readJsonInts(a.getAnswerJson()).forEach(i -> {
                if (i >= 0 && i < counts.length) {
                    counts[i]++;
                }
            }));
            List<Map<String, Object>> distribution = new ArrayList<>();
            for (int i = 0; i < vo.getOptions().size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("option", vo.getOptions().get(i));
                item.put("count", counts[i]);
                distribution.add(item);
            }
            vo.setDistribution(distribution);
        }
        return vo;
    }

    private String statusOf(Quiz quiz) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(quiz.getStartTime())) {
            return "NOT_STARTED";
        }
        if (now.isAfter(quiz.getEndTime())) {
            return "ENDED";
        }
        return "RUNNING";
    }

    private Quiz requireQuiz(Long id) {
        Quiz quiz = quizMapper.selectById(id);
        if (quiz == null) {
            throw BizException.notFound("小测不存在");
        }
        return quiz;
    }

    private void assertStudentInCourse(Student student, Long courseId) {
        boolean inCourse = access.courseStudents(courseId).stream()
                .anyMatch(s -> s.getId().equals(student.getId()));
        if (!inCourse) {
            throw BizException.forbidden("你不在这门课程中，无法参与该互动");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(500, "数据序列化失败");
        }
    }

    private List<String> readJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Integer> readJsonInts(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Integer> parseCorrect(String correct) {
        if (correct == null || correct.isBlank()) {
            return List.of();
        }
        return List.of(correct.split(",")).stream().filter(s -> !s.isBlank()).map(Integer::valueOf).toList();
    }
}
