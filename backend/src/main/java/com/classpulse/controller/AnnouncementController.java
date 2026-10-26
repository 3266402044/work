package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.dto.AnnouncementRequest;
import com.classpulse.entity.ClassAnnouncement;
import com.classpulse.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @PostMapping
    public ApiResponse<ClassAnnouncement> create(@Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(announcementService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ClassAnnouncement>> list(@RequestParam Long courseId) {
        return ApiResponse.ok(announcementService.list(courseId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ApiResponse.ok();
    }
}
