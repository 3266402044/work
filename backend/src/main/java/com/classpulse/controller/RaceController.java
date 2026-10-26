package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.dto.RaceCreateRequest;
import com.classpulse.service.RaceService;
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
@RequestMapping("/api/races")
@RequiredArgsConstructor
public class RaceController {
    private final RaceService raceService;

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody RaceCreateRequest request) {
        return ApiResponse.ok(raceService.create(request));
    }

    @GetMapping("/active")
    public ApiResponse<List<Map<String, Object>>> active() {
        return ApiResponse.ok(raceService.active());
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(raceService.history(courseId, from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(raceService.detail(id));
    }

    @PostMapping("/{id}/buzz")
    public ApiResponse<Map<String, Object>> buzz(@PathVariable Long id) {
        return ApiResponse.ok(raceService.buzz(id));
    }
}
