package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.common.PageResult;
import com.classpulse.dto.ChangePasswordRequest;
import com.classpulse.dto.LoginRequest;
import com.classpulse.dto.ProfileUpdateRequest;
import com.classpulse.entity.SysNotification;
import com.classpulse.service.AuthService;
import com.classpulse.service.NotificationService;
import com.classpulse.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final NotificationService notificationService;

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/auth/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/auth/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok();
    }

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(authService.updateProfile(request));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<SysNotification>> notifications() {
        return ApiResponse.ok(notificationService.list());
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Map<String, Long>> unread() {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount()));
    }

    @GetMapping("/notifications/page")
    public ApiResponse<PageResult<SysNotification>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(notificationService.page(page, size));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.ok();
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok();
    }
}
