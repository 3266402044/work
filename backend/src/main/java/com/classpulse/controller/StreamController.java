package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.common.BizException;
import com.classpulse.security.JwtService;
import com.classpulse.service.SseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class StreamController {
    private final JwtService jwtService;
    private final SseHub sseHub;

    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String token) {
        try {
            var user = jwtService.parse(token);
            return sseHub.subscribe(user.userId());
        } catch (Exception e) {
            throw BizException.unauthorized("SSE 连接凭证无效");
        }
    }
}
