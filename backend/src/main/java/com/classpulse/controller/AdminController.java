package com.classpulse.controller;

import com.classpulse.common.ApiResponse;
import com.classpulse.common.PageResult;
import com.classpulse.dto.ClassSaveRequest;
import com.classpulse.dto.CourseSaveRequest;
import com.classpulse.dto.StudentSaveRequest;
import com.classpulse.dto.TeacherSaveRequest;
import com.classpulse.dto.UserSaveRequest;
import com.classpulse.entity.OperationLog;
import com.classpulse.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(adminService.overview());
    }

    /* ---------- 学生 ---------- */
    @GetMapping("/students")
    public ApiResponse<PageResult<Map<String, Object>>> students(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId) {
        return ApiResponse.ok(adminService.students(page, size, keyword, classId));
    }

    @PostMapping("/students")
    public ApiResponse<Void> createStudent(@Valid @RequestBody StudentSaveRequest request) {
        request.setId(null);
        adminService.saveStudent(request);
        return ApiResponse.ok();
    }

    @PutMapping("/students")
    public ApiResponse<Void> updateStudent(@Valid @RequestBody StudentSaveRequest request) {
        adminService.saveStudent(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/students/{id}")
    public ApiResponse<Void> deleteStudent(@PathVariable Long id) {
        adminService.deleteStudent(id);
        return ApiResponse.ok();
    }

    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudents() {
        byte[] data = adminService.exportStudents();
        String filename = URLEncoder.encode("学生名单.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/students/import")
    public ApiResponse<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminService.importStudents(file));
    }

    /* ---------- 教师 ---------- */
    @GetMapping("/teachers")
    public ApiResponse<PageResult<Map<String, Object>>> teachers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.teachers(page, size, keyword));
    }

    @PostMapping("/teachers")
    public ApiResponse<Void> createTeacher(@Valid @RequestBody TeacherSaveRequest request) {
        request.setId(null);
        adminService.saveTeacher(request);
        return ApiResponse.ok();
    }

    @PutMapping("/teachers")
    public ApiResponse<Void> updateTeacher(@Valid @RequestBody TeacherSaveRequest request) {
        adminService.saveTeacher(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/teachers/{id}")
    public ApiResponse<Void> deleteTeacher(@PathVariable Long id) {
        adminService.deleteTeacher(id);
        return ApiResponse.ok();
    }

    /* ---------- 班级 ---------- */
    @GetMapping("/classes")
    public ApiResponse<List<Map<String, Object>>> classes() {
        return ApiResponse.ok(adminService.classes());
    }

    @PostMapping("/classes")
    public ApiResponse<Void> createClass(@Valid @RequestBody ClassSaveRequest request) {
        request.setId(null);
        adminService.saveClass(request);
        return ApiResponse.ok();
    }

    @PutMapping("/classes")
    public ApiResponse<Void> updateClass(@Valid @RequestBody ClassSaveRequest request) {
        adminService.saveClass(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/classes/{id}")
    public ApiResponse<Void> deleteClass(@PathVariable Long id) {
        adminService.deleteClass(id);
        return ApiResponse.ok();
    }

    /* ---------- 课程 ---------- */
    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses() {
        return ApiResponse.ok(adminService.courses());
    }

    @PostMapping("/courses")
    public ApiResponse<Void> createCourse(@Valid @RequestBody CourseSaveRequest request) {
        request.setId(null);
        adminService.saveCourse(request);
        return ApiResponse.ok();
    }

    @PutMapping("/courses")
    public ApiResponse<Void> updateCourse(@Valid @RequestBody CourseSaveRequest request) {
        adminService.saveCourse(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/courses/{id}")
    public ApiResponse<Void> deleteCourse(@PathVariable Long id) {
        adminService.deleteCourse(id);
        return ApiResponse.ok();
    }

    /* ---------- 账号 ---------- */
    @GetMapping("/users")
    public ApiResponse<PageResult<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return ApiResponse.ok(adminService.users(page, size, keyword, role));
    }

    @PostMapping("/users")
    public ApiResponse<Void> createUser(@Valid @RequestBody UserSaveRequest request) {
        request.setId(null);
        adminService.saveUser(request);
        return ApiResponse.ok();
    }

    @PutMapping("/users")
    public ApiResponse<Void> updateUser(@Valid @RequestBody UserSaveRequest request) {
        adminService.saveUser(request);
        return ApiResponse.ok();
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        adminService.resetPassword(id);
        return ApiResponse.ok();
    }

    /* ---------- 日志与设置 ---------- */
    @GetMapping("/logs")
    public ApiResponse<PageResult<OperationLog>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(adminService.logs(page, size, userId, module, from, to));
    }

    @GetMapping("/settings")
    public ApiResponse<List<Map<String, Object>>> settings() {
        return ApiResponse.ok(adminService.settings());
    }

    @PutMapping("/settings")
    public ApiResponse<Void> updateSettings(@RequestBody Map<String, String> values) {
        adminService.updateSettings(values);
        return ApiResponse.ok();
    }

    @GetMapping("/interactions/export")
    public ResponseEntity<byte[]> exportInteractions() {
        byte[] data = adminService.exportInteraction();
        String filename = URLEncoder.encode("请假记录.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/demo/reset")
    public ApiResponse<Void> resetDemo() {
        adminService.resetDemoData();
        return ApiResponse.ok();
    }
}
