package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.entity.ClassGroup;
import com.classpulse.entity.Course;
import com.classpulse.service.AccessService;
import com.classpulse.service.LeaveService;
import com.classpulse.service.TeacherPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherPortalService teacherPortalService;
    private final LeaveService leaveService;
    private final AccessService access;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(teacherPortalService.dashboard());
    }

    @GetMapping("/leaves")
    public ApiResponse<List<Map<String, Object>>> leaves(@RequestParam(required = false) String status) {
        return ApiResponse.ok(leaveService.teacherLeaves(status));
    }

    @PostMapping("/leaves/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id,
                                    @RequestParam boolean approved,
                                    @RequestParam(required = false) String remark) {
        leaveService.review(id, approved, remark);
        return ApiResponse.ok();
    }

    @GetMapping("/courses")
    public ApiResponse<List<Course>> courses() {
        if (com.classpulse.common.SecurityUtils.isAdmin()) {
            return ApiResponse.ok(access.coursesOfTeacher(null));
        }
        return ApiResponse.ok(access.coursesOfTeacher(com.classpulse.common.SecurityUtils.teacherId()));
    }

    @GetMapping("/classes")
    public ApiResponse<List<ClassGroup>> classes(@RequestParam(required = false) Long courseId) {
        if (courseId != null) {
            return ApiResponse.ok(access.classesOfCourse(courseId));
        }
        List<Course> courses = com.classpulse.common.SecurityUtils.isAdmin()
                ? access.coursesOfTeacher(null)
                : access.coursesOfTeacher(com.classpulse.common.SecurityUtils.teacherId());
        Map<Long, ClassGroup> map = new LinkedHashMap<>();
        courses.forEach(c -> access.classesOfCourse(c.getId()).forEach(g -> map.put(g.getId(), g)));
        return ApiResponse.ok(new ArrayList<>(map.values()));
    }

    @GetMapping("/classes/{id}/roster")
    public ApiResponse<Map<String, Object>> roster(@PathVariable Long id) {
        return ApiResponse.ok(teacherPortalService.classRoster(id));
    }

    @GetMapping("/warnings")
    public ApiResponse<List<Map<String, Object>>> warnings() {
        return ApiResponse.ok(teacherPortalService.warnings());
    }
}
