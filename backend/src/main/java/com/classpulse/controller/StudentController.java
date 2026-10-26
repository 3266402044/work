package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.common.PageResult;
import com.classpulse.dto.LeaveApplyRequest;
import com.classpulse.dto.SignRequest;
import com.classpulse.entity.Course;
import com.classpulse.service.LeaveService;
import com.classpulse.service.QuizService;
import com.classpulse.service.RaceService;
import com.classpulse.service.RollCallService;
import com.classpulse.service.SignService;
import com.classpulse.service.StudentPortalService;
import com.classpulse.service.TaskService;
import com.classpulse.vo.SignRecordVO;
import com.classpulse.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {
    private final StudentPortalService studentPortalService;
    private final TaskService taskService;
    private final SignService signService;
    private final LeaveService leaveService;
    private final RollCallService rollCallService;
    private final QuizService quizService;
    private final RaceService raceService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(studentPortalService.dashboard());
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResult<TaskVO>> tasks(
            @RequestParam(required = false, defaultValue = "ALL") String scope,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(taskService.studentTasks(scope, courseId, date, keyword, page, size));
    }

    @PostMapping("/tasks/{id}/leave-requests")
    public ApiResponse<Void> applyLeave(@PathVariable Long id, @Valid @RequestBody LeaveApplyRequest request) {
        request.setTaskId(id);
        leaveService.apply(request);
        return ApiResponse.ok();
    }

    @PostMapping("/tasks/{id}/sign")
    public ApiResponse<Map<String, Object>> sign(@PathVariable Long id, @Valid @RequestBody SignRequest request) {
        return ApiResponse.ok(signService.sign(id, request));
    }

    @GetMapping("/records")
    public ApiResponse<PageResult<SignRecordVO>> records(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(studentPortalService.myRecords(status, courseId, from, to, page, size));
    }

    @GetMapping("/leaves")
    public ApiResponse<List<Map<String, Object>>> leaves() {
        return ApiResponse.ok(leaveService.myLeaves());
    }

    @GetMapping("/participation")
    public ApiResponse<Map<String, Object>> participation() {
        return ApiResponse.ok(studentPortalService.participation());
    }

    @GetMapping("/calendar")
    public ApiResponse<List<Map<String, Object>>> calendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        LocalDate start = weekStart == null ? LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1)
                : weekStart;
        return ApiResponse.ok(studentPortalService.calendar(start));
    }

    @GetMapping("/courses")
    public ApiResponse<List<Course>> courses() {
        return ApiResponse.ok(studentPortalService.myCourses());
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Map<String, Object>>> announcements() {
        return ApiResponse.ok(studentPortalService.announcements());
    }

    @GetMapping("/interactions/active")
    public ApiResponse<Map<String, Object>> activeInteractions() {
        return ApiResponse.ok(Map.of(
                "rollCalls", rollCallService.activeForStudent(),
                "quizzes", quizService.list(null, null, null).stream()
                        .filter(q -> "RUNNING".equals(q.getStatus()) && !Boolean.TRUE.equals(q.getAnswered()))
                        .toList(),
                "races", raceService.active()
        ));
    }
}
