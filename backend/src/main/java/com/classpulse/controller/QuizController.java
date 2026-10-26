package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.dto.QuizCreateRequest;
import com.classpulse.dto.QuizSubmitRequest;
import com.classpulse.service.QuizService;
import com.classpulse.vo.QuizVO;
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
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;

    @PostMapping
    public ApiResponse<QuizVO> create(@Valid @RequestBody QuizCreateRequest request) {
        return ApiResponse.ok(quizService.create(request));
    }

    @GetMapping
    public ApiResponse<List<QuizVO>> list(@RequestParam(required = false) Long courseId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(quizService.list(courseId, from, to));
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(quizService.history(courseId, from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuizVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(quizService.detail(id));
    }

    @PostMapping("/{id}/answers")
    public ApiResponse<Map<String, Object>> submit(@PathVariable Long id,
                                                   @Valid @RequestBody QuizSubmitRequest request) {
        return ApiResponse.ok(quizService.submit(id, request));
    }

    @GetMapping("/{id}/results")
    public ApiResponse<Map<String, Object>> results(@PathVariable Long id) {
        return ApiResponse.ok(quizService.results(id));
    }
}
