package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.dto.RollCallCreateRequest;
import com.classpulse.dto.RollCallResultRequest;
import com.classpulse.service.RollCallService;
import com.classpulse.vo.RollCallVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/roll-calls")
@RequiredArgsConstructor
public class RollCallController {
    private final RollCallService rollCallService;

    @PostMapping
    public ApiResponse<RollCallVO> create(@Valid @RequestBody RollCallCreateRequest request) {
        return ApiResponse.ok(rollCallService.create(request));
    }

    @GetMapping("/active")
    public ApiResponse<List<Map<String, Object>>> active() {
        return ApiResponse.ok(rollCallService.activeForStudent());
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(rollCallService.history(courseId, from, to));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] data = rollCallService.exportHistory(courseId, from, to);
        String filename = java.net.URLEncoder.encode("随机点名记录.xlsx", java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<RollCallVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(rollCallService.detail(id));
    }

    @PostMapping("/{id}/respond")
    public ApiResponse<Map<String, Object>> respond(@PathVariable Long id) {
        return ApiResponse.ok(rollCallService.respond(id));
    }

    @PostMapping("/{id}/records/{recordId}/result")
    public ApiResponse<Void> markResult(@PathVariable Long id, @PathVariable Long recordId,
                                        @Valid @RequestBody RollCallResultRequest request) {
        rollCallService.markResult(id, recordId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/draw-again")
    public ApiResponse<RollCallVO> drawAgain(@PathVariable Long id,
                                             @RequestParam(required = false) Integer count) {
        return ApiResponse.ok(rollCallService.drawAgain(id, count));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<Void> finish(@PathVariable Long id) {
        rollCallService.finish(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/sync-attendance")
    public ApiResponse<Map<String, Object>> syncAttendance(@PathVariable Long id) {
        int count = rollCallService.syncAttendance(id);
        return ApiResponse.ok(Map.of("count", count, "message", "已同步 " + count + " 条点名异常提醒"));
    }
}
