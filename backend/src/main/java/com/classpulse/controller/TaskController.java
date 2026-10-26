package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.common.PageResult;
import com.classpulse.dto.LeaveApplyRequest;
import com.classpulse.dto.ManualRecordRequest;
import com.classpulse.dto.SignRequest;
import com.classpulse.dto.TaskSaveRequest;
import com.classpulse.service.LeaveService;
import com.classpulse.service.SignService;
import com.classpulse.service.TaskService;
import com.classpulse.vo.TaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final SignService signService;
    private final LeaveService leaveService;

    @GetMapping("/tasks/{id}")
    public ApiResponse<TaskVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(taskService.detail(id));
    }

    @PostMapping("/tasks/{id}/sign")
    public ApiResponse<Map<String, Object>> sign(@PathVariable Long id, @Valid @RequestBody SignRequest request) {
        return ApiResponse.ok(signService.sign(id, request));
    }

    @PostMapping("/tasks/{id}/leave-requests")
    public ApiResponse<Void> leave(@PathVariable Long id, @Valid @RequestBody LeaveApplyRequest request) {
        request.setTaskId(id);
        leaveService.apply(request);
        return ApiResponse.ok();
    }

    @PostMapping("/tasks")
    public ApiResponse<TaskVO> create(@Valid @RequestBody TaskSaveRequest request) {
        return ApiResponse.ok(taskService.create(request));
    }

    @PutMapping("/tasks/{id}")
    public ApiResponse<TaskVO> update(@PathVariable Long id, @Valid @RequestBody TaskSaveRequest request) {
        return ApiResponse.ok(taskService.update(id, request));
    }

    @PostMapping("/tasks/{id}/close")
    public ApiResponse<TaskVO> close(@PathVariable Long id) {
        return ApiResponse.ok(taskService.close(id));
    }

    @PostMapping("/tasks/{id}/qr-token")
    public ApiResponse<Map<String, Object>> qrToken(@PathVariable Long id) {
        return ApiResponse.ok(taskService.generateQrToken(id));
    }

    @PostMapping("/tasks/{id}/passcode")
    public ApiResponse<Map<String, String>> passcode(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("passcode", taskService.regeneratePasscode(id)));
    }

    @GetMapping("/tasks/{id}/records")
    public ApiResponse<Map<String, Object>> records(@PathVariable Long id) {
        return ApiResponse.ok(taskService.records(id));
    }

    @PostMapping("/tasks/{id}/records/manual")
    public ApiResponse<Void> manual(@PathVariable Long id, @Valid @RequestBody ManualRecordRequest request) {
        taskService.manualRecord(id, request);
        return ApiResponse.ok();
    }

    @GetMapping("/tasks/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] data = taskService.exportRecords(id);
        String filename = URLEncoder.encode("签到明细-" + id + ".xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/teacher/tasks")
    public ApiResponse<PageResult<TaskVO>> teacherTasks(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "ALL") String scope,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(taskService.teacherTasks(courseId, classId, date, scope, keyword, page, size));
    }
}
