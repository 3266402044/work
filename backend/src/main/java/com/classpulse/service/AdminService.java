package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.classpulse.common.BizException;
import com.classpulse.common.PageResult;
import com.classpulse.common.Utils;
import com.classpulse.dto.ClassSaveRequest;
import com.classpulse.dto.CourseSaveRequest;
import com.classpulse.dto.StudentSaveRequest;
import com.classpulse.dto.TeacherSaveRequest;
import com.classpulse.dto.UserSaveRequest;
import com.classpulse.entity.ClassGroup;
import com.classpulse.entity.Course;
import com.classpulse.entity.CourseClass;
import com.classpulse.entity.LeaveRequest;
import com.classpulse.entity.OperationLog;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.entity.SysUser;
import com.classpulse.entity.Teacher;
import com.classpulse.mapper.ClassGroupMapper;
import com.classpulse.mapper.CourseClassMapper;
import com.classpulse.mapper.CourseMapper;
import com.classpulse.mapper.LeaveRequestMapper;
import com.classpulse.mapper.OperationLogMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.mapper.SysUserMapper;
import com.classpulse.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ClassGroupMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final SignTaskMapper taskMapper;
    private final SignRecordMapper recordMapper;
    private final LeaveRequestMapper leaveMapper;
    private final SysUserMapper userMapper;
    private final OperationLogMapper logMapper;
    private final AccessService access;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService logService;
    private final DemoDataService demoDataService;

    /* ===================== 运营总览 ===================== */

    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        List<SignTask> allTasks = taskMapper.selectList(new LambdaQueryWrapper<SignTask>()
                .orderByDesc(SignTask::getStartTime));
        List<SignTask> todayTasks = allTasks.stream()
                .filter(t -> t.getStartTime().toLocalDate().equals(today)).toList();
        Set<Long> activeCourses = todayTasks.stream().map(SignTask::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        long todayExpected = 0;
        long todaySigned = 0;
        for (SignTask task : todayTasks) {
            todayExpected += access.roster(task).size();
            todaySigned += recordMapper.selectCount(new LambdaQueryWrapper<SignRecord>()
                    .eq(SignRecord::getTaskId, task.getId()));
        }

        List<SignTask> recentTasks = allTasks.stream()
                .filter(t -> t.getStartTime().isAfter(LocalDateTime.now().minusDays(7)))
                .toList();
        List<Long> recentTaskIds = recentTasks.stream().map(SignTask::getId).toList();
        List<SignRecord> recentRecords = recentTaskIds.isEmpty() ? List.of()
                : recordMapper.selectList(new LambdaQueryWrapper<SignRecord>().in(SignRecord::getTaskId, recentTaskIds));
        Map<Long, Long> signedByTask = recentRecords.stream()
                .collect(Collectors.groupingBy(SignRecord::getTaskId, Collectors.counting()));

        Map<Long, long[]> classStats = new LinkedHashMap<>();
        for (SignTask task : recentTasks) {
            List<Student> roster = access.roster(task);
            long expected = roster.size();
            long signed = signedByTask.getOrDefault(task.getId(), 0L);
            roster.stream().map(Student::getClassId).filter(Objects::nonNull).distinct().forEach(classId -> {
                long[] stat = classStats.computeIfAbsent(classId, k -> new long[2]);
                stat[0] += expected;
                stat[1] += signed;
            });
        }
        List<Map<String, Object>> classRanking = classStats.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("classId", e.getKey());
                    map.put("className", access.className(e.getKey()));
                    map.put("expected", e.getValue()[0]);
                    map.put("signed", e.getValue()[1]);
                    map.put("rate", Utils.rate(e.getValue()[1], e.getValue()[0]));
                    return map;
                })
                .sorted(Comparator.comparingDouble(m -> -(double) m.get("rate")))
                .toList();

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<SignTask> dayTasks = allTasks.stream()
                    .filter(t -> t.getStartTime().toLocalDate().equals(date)).toList();
            long expected = 0;
            long signed = 0;
            for (SignTask task : dayTasks) {
                expected += access.roster(task).size();
                signed += recordMapper.selectCount(new LambdaQueryWrapper<SignRecord>()
                        .eq(SignRecord::getTaskId, task.getId()));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("label", date.getMonthValue() + "/" + date.getDayOfMonth());
            item.put("expected", expected);
            item.put("signed", signed);
            item.put("rate", Utils.rate(signed, expected));
            trend.add(item);
        }

        List<Map<String, Object>> anomalies = new ArrayList<>();
        classRanking.stream().filter(m -> (double) m.get("rate") < 70).limit(3).forEach(m ->
                anomalies.add(Map.of("type", "CLASS_RATE", "level", "WARN",
                        "title", m.get("className") + " 出勤率偏低",
                        "detail", "近 7 天到课率 " + m.get("rate") + "%")));
        long pendingLeaves = leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStatus, "PENDING"));
        if (pendingLeaves > 0) {
            anomalies.add(Map.of("type", "LEAVE", "level", "INFO",
                    "title", "有 " + pendingLeaves + " 条待审批请假", "detail", "请提醒任课教师及时处理"));
        }
        long ongoing = allTasks.stream()
                .filter(t -> com.classpulse.common.TaskStatus.of(t, LocalDateTime.now()) == com.classpulse.common.TaskStatus.ONGOING)
                .count();
        if (ongoing > 0) {
            anomalies.add(Map.of("type", "ONGOING", "level", "OK",
                    "title", ongoing + " 个签到正在进行", "detail", "可进入实时监控查看签到动态"));
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("activeCourses", activeCourses.size());
        metrics.put("todayTasks", todayTasks.size());
        metrics.put("todaySigns", todaySigned);
        metrics.put("attendanceRate", Utils.rate(todaySigned, todayExpected));
        metrics.put("totalStudents", studentMapper.selectCount(null));
        metrics.put("totalTeachers", teacherMapper.selectCount(null));
        metrics.put("totalCourses", courseMapper.selectCount(null));
        metrics.put("ongoingTasks", ongoing);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metrics", metrics);
        result.put("classRanking", classRanking);
        result.put("trend", trend);
        result.put("anomalies", anomalies);
        return result;
    }

    /* ===================== 学生管理 ===================== */

    public PageResult<Map<String, Object>> students(long page, long size, String keyword, Long classId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Student::getStudentNo, keyword.trim()).or().like(Student::getName, keyword.trim()));
        }
        if (classId != null) {
            wrapper.eq(Student::getClassId, classId);
        }
        wrapper.orderByAsc(Student::getStudentNo);
        Page<Student> p = studentMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(this::studentMap).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    @Transactional
    public void saveStudent(StudentSaveRequest request) {
        access.requireClass(request.getClassId());
        Student existing = request.getId() == null ? null : studentMapper.selectById(request.getId());
        Long duplicate = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, request.getStudentNo().trim())
                .ne(request.getId() != null, Student::getId, request.getId()));
        if (duplicate > 0) {
            throw BizException.conflict("学号已存在，请勿重复添加");
        }
        if (existing == null) {
            Student student = new Student();
            student.setStudentNo(request.getStudentNo().trim());
            student.setName(request.getName().trim());
            student.setClassId(request.getClassId());
            student.setCreateTime(LocalDateTime.now());
            studentMapper.insert(student);

            SysUser user = new SysUser();
            user.setUsername(student.getStudentNo());
            user.setPasswordHash(passwordEncoder.encode("Student@123"));
            user.setRole("STUDENT");
            user.setRealName(student.getName());
            user.setPhone(request.getPhone());
            user.setEnabled(1);
            user.setRelatedId(student.getId());
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);

            student.setUserId(user.getId());
            studentMapper.updateById(student);
            logService.log("ADMIN", "CREATE_STUDENT", student.getId(), "新增学生：" + student.getStudentNo());
        } else {
            existing.setStudentNo(request.getStudentNo().trim());
            existing.setName(request.getName().trim());
            existing.setClassId(request.getClassId());
            studentMapper.updateById(existing);
            SysUser user = existing.getUserId() == null ? null : userMapper.selectById(existing.getUserId());
            if (user != null) {
                user.setRealName(existing.getName());
                user.setPhone(request.getPhone());
                userMapper.updateById(user);
            }
            logService.log("ADMIN", "UPDATE_STUDENT", existing.getId(), "编辑学生：" + existing.getStudentNo());
        }
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw BizException.notFound("学生不存在");
        }
        studentMapper.deleteById(id);
        if (student.getUserId() != null) {
            SysUser user = userMapper.selectById(student.getUserId());
            if (user != null) {
                user.setEnabled(0);
                userMapper.updateById(user);
            }
        }
        logService.log("ADMIN", "DELETE_STUDENT", id, "停用学生：" + student.getStudentNo());
    }

    /* ===================== 教师管理 ===================== */

    public PageResult<Map<String, Object>> teachers(long page, long size, String keyword) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Teacher::getTeacherNo, keyword.trim()).or().like(Teacher::getName, keyword.trim()));
        }
        wrapper.orderByAsc(Teacher::getTeacherNo);
        Page<Teacher> p = teacherMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("teacherNo", t.getTeacherNo());
            map.put("name", t.getName());
            map.put("department", t.getDepartment());
            map.put("userId", t.getUserId());
            map.put("courseCount", courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, t.getId())));
            return map;
        }).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    @Transactional
    public void saveTeacher(TeacherSaveRequest request) {
        Teacher existing = request.getId() == null ? null : teacherMapper.selectById(request.getId());
        long duplicate = teacherMapper.selectCount(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTeacherNo, request.getTeacherNo().trim())
                .ne(request.getId() != null, Teacher::getId, request.getId()));
        if (duplicate > 0) {
            throw BizException.conflict("工号已存在，请勿重复添加");
        }
        if (existing == null) {
            Teacher teacher = new Teacher();
            teacher.setTeacherNo(request.getTeacherNo().trim());
            teacher.setName(request.getName().trim());
            teacher.setDepartment(request.getDepartment());
            teacherMapper.insert(teacher);

            SysUser user = new SysUser();
            user.setUsername(teacher.getTeacherNo());
            user.setPasswordHash(passwordEncoder.encode("Teacher@123"));
            user.setRole("TEACHER");
            user.setRealName(teacher.getName());
            user.setPhone(request.getPhone());
            user.setEnabled(1);
            user.setRelatedId(teacher.getId());
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);

            teacher.setUserId(user.getId());
            teacherMapper.updateById(teacher);
            logService.log("ADMIN", "CREATE_TEACHER", teacher.getId(), "新增教师：" + teacher.getTeacherNo());
        } else {
            existing.setTeacherNo(request.getTeacherNo().trim());
            existing.setName(request.getName().trim());
            existing.setDepartment(request.getDepartment());
            teacherMapper.updateById(existing);
            SysUser user = existing.getUserId() == null ? null : userMapper.selectById(existing.getUserId());
            if (user != null) {
                user.setRealName(existing.getName());
                user.setPhone(request.getPhone());
                userMapper.updateById(user);
            }
            logService.log("ADMIN", "UPDATE_TEACHER", existing.getId(), "编辑教师：" + existing.getTeacherNo());
        }
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw BizException.notFound("教师不存在");
        }
        teacherMapper.deleteById(id);
        if (teacher.getUserId() != null) {
            SysUser user = userMapper.selectById(teacher.getUserId());
            if (user != null) {
                user.setEnabled(0);
                userMapper.updateById(user);
            }
        }
        logService.log("ADMIN", "DELETE_TEACHER", id, "停用教师：" + teacher.getTeacherNo());
    }

    /* ===================== 班级管理 ===================== */

    public List<Map<String, Object>> classes() {
        return classMapper.selectList(new LambdaQueryWrapper<ClassGroup>().orderByAsc(ClassGroup::getName))
                .stream().map(this::classMap).toList();
    }

    @Transactional
    public void saveClass(ClassSaveRequest request) {
        if (request.getId() == null) {
            ClassGroup group = new ClassGroup();
            group.setName(request.getName().trim());
            group.setGrade(request.getGrade());
            group.setMajor(request.getMajor());
            group.setAdvisorId(request.getAdvisorId());
            group.setCreateTime(LocalDateTime.now());
            classMapper.insert(group);
            logService.log("ADMIN", "CREATE_CLASS", group.getId(), "新增班级：" + group.getName());
        } else {
            ClassGroup group = classMapper.selectById(request.getId());
            if (group == null) {
                throw BizException.notFound("班级不存在");
            }
            group.setName(request.getName().trim());
            group.setGrade(request.getGrade());
            group.setMajor(request.getMajor());
            group.setAdvisorId(request.getAdvisorId());
            classMapper.updateById(group);
            logService.log("ADMIN", "UPDATE_CLASS", group.getId(), "编辑班级：" + group.getName());
        }
    }

    @Transactional
    public void deleteClass(Long id) {
        long students = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getClassId, id));
        if (students > 0) {
            throw BizException.invalid("该班级下仍有学生，请先迁移学生后再删除");
        }
        classMapper.deleteById(id);
        logService.log("ADMIN", "DELETE_CLASS", id, "删除班级");
    }

    /* ===================== 课程管理 ===================== */

    public List<Map<String, Object>> courses() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByAsc(Course::getCourseNo))
                .stream().map(this::courseMap).toList();
    }

    @Transactional
    public void saveCourse(CourseSaveRequest request) {
        access.requireTeacher(request.getTeacherId());
        long duplicate = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseNo, request.getCourseNo().trim())
                .ne(request.getId() != null, Course::getId, request.getId()));
        if (duplicate > 0) {
            throw BizException.conflict("课程编号已存在");
        }
        Course course;
        if (request.getId() == null) {
            course = new Course();
            fillCourse(course, request);
            courseMapper.insert(course);
        } else {
            course = courseMapper.selectById(request.getId());
            if (course == null) {
                throw BizException.notFound("课程不存在");
            }
            fillCourse(course, request);
            courseMapper.updateById(course);
            courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>()
                    .eq(CourseClass::getCourseId, course.getId()));
        }
        if (request.getClassIds() != null) {
            for (Long classId : new HashSet<>(request.getClassIds())) {
                access.requireClass(classId);
                CourseClass relation = new CourseClass();
                relation.setCourseId(course.getId());
                relation.setClassId(classId);
                courseClassMapper.insert(relation);
            }
        }
        logService.log("ADMIN", request.getId() == null ? "CREATE_COURSE" : "UPDATE_COURSE",
                course.getId(), "维护课程：" + course.getName());
    }

    @Transactional
    public void deleteCourse(Long id) {
        long tasks = taskMapper.selectCount(new LambdaQueryWrapper<SignTask>().eq(SignTask::getCourseId, id));
        if (tasks > 0) {
            throw BizException.invalid("该课程已有签到任务，不能删除，可将课程归档");
        }
        courseMapper.deleteById(id);
        courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, id));
        logService.log("ADMIN", "DELETE_COURSE", id, "删除课程");
    }

    private void fillCourse(Course course, CourseSaveRequest request) {
        course.setCourseNo(request.getCourseNo().trim());
        course.setName(request.getName().trim());
        course.setTeacherId(request.getTeacherId());
        course.setSemester(request.getSemester());
        course.setCredit(request.getCredit());
        course.setCoverColor(request.getCoverColor());
        course.setClassroom(request.getClassroom());
        course.setLongitude(request.getLongitude());
        course.setLatitude(request.getLatitude());
        course.setDefaultRadius(request.getDefaultRadius());
    }

    /* ===================== 用户管理 ===================== */

    public PageResult<Map<String, Object>> users(long page, long size, String keyword, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword.trim())
                    .or().like(SysUser::getRealName, keyword.trim()));
        }
        if (role != null && !role.isBlank() && !"ALL".equals(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole());
            map.put("realName", u.getRealName());
            map.put("phone", u.getPhone());
            map.put("enabled", u.getEnabled());
            map.put("relatedId", u.getRelatedId());
            map.put("lastLoginTime", u.getLastLoginTime());
            map.put("createTime", u.getCreateTime());
            return map;
        }).toList();
        return PageResult.of(list, p.getTotal(), page, size);
    }

    @Transactional
    public void saveUser(UserSaveRequest request) {
        long duplicate = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername().trim())
                .ne(request.getId() != null, SysUser::getId, request.getId()));
        if (duplicate > 0) {
            throw BizException.conflict("登录名已存在");
        }
        if (request.getId() == null) {
            SysUser user = new SysUser();
            user.setUsername(request.getUsername().trim());
            user.setPasswordHash(passwordEncoder.encode(
                    request.getPassword() == null || request.getPassword().isBlank() ? "Pass@123" : request.getPassword()));
            user.setRole(request.getRole() == null ? "STUDENT" : request.getRole());
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            user.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
            logService.log("ADMIN", "CREATE_USER", user.getId(), "新增账号：" + user.getUsername());
        } else {
            SysUser user = userMapper.selectById(request.getId());
            if (user == null) {
                throw BizException.notFound("账号不存在");
            }
            user.setUsername(request.getUsername().trim());
            user.setRealName(request.getRealName());
            user.setPhone(request.getPhone());
            if (request.getRole() != null) {
                user.setRole(request.getRole());
            }
            if (request.getEnabled() != null) {
                user.setEnabled(request.getEnabled() ? 1 : 0);
            }
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            }
            userMapper.updateById(user);
            logService.log("ADMIN", "UPDATE_USER", user.getId(), "编辑账号：" + user.getUsername());
        }
    }

    @Transactional
    public void resetPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("账号不存在");
        }
        user.setPasswordHash(passwordEncoder.encode("Pass@123"));
        userMapper.updateById(user);
        logService.log("ADMIN", "RESET_PASSWORD", userId, "重置账号密码");
    }

    /* ===================== 系统日志与配置 ===================== */

    public PageResult<OperationLog> logs(long page, long size, Long userId, String module, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        if (module != null && !module.isBlank() && !"ALL".equals(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (from != null) {
            wrapper.ge(OperationLog::getCreateTime, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(OperationLog::getCreateTime, to.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> p = logMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    public List<Map<String, Object>> settings() {
        return configService.list().stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", c.getConfigKey());
            map.put("value", c.getConfigValue());
            map.put("description", c.getDescription());
            return map;
        }).toList();
    }

    public void updateSettings(Map<String, String> values) {
        configService.update(values);
        logService.log("ADMIN", "UPDATE_SETTINGS", null, "更新系统设置：" + values.keySet());
    }

    /* ===================== 导入导出 ===================== */

    public byte[] exportStudents() {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .orderByAsc(Student::getStudentNo));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生名单");
            String[] headers = {"学号", "姓名", "班级", "年级", "专业"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 18 * 256);
            }
            int index = 1;
            for (Student student : students) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(student.getStudentNo());
                row.createCell(1).setCellValue(student.getName());
                ClassGroup group = student.getClassId() == null ? null : classMapper.selectById(student.getClassId());
                row.createCell(2).setCellValue(group == null ? "" : group.getName());
                row.createCell(3).setCellValue(group == null || group.getGrade() == null ? "" : group.getGrade());
                row.createCell(4).setCellValue(group == null || group.getMajor() == null ? "" : group.getMajor());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "导出学生名单失败");
        }
    }

    public Map<String, Object> importStudents(MultipartFile file) {
        List<Map<String, Object>> errors = new ArrayList<>();
        int success = 0;
        Map<String, Long> classByName = classMapper.selectList(null).stream()
                .collect(Collectors.toMap(ClassGroup::getName, ClassGroup::getId, (a, b) -> a));
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String studentNo = cell(row.getCell(0));
                String name = cell(row.getCell(1));
                String className = cell(row.getCell(2));
                int rowNo = i + 1;
                if (studentNo.isBlank() || name.isBlank() || className.isBlank()) {
                    errors.add(Map.of("row", rowNo, "message", "学号、姓名、班级名称均为必填"));
                    continue;
                }
                Long classId = classByName.get(className);
                if (classId == null) {
                    errors.add(Map.of("row", rowNo, "message", "班级不存在：" + className));
                    continue;
                }
                if (studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo)) > 0) {
                    errors.add(Map.of("row", rowNo, "message", "学号已存在：" + studentNo));
                    continue;
                }
                try {
                    StudentSaveRequest request = new StudentSaveRequest();
                    request.setStudentNo(studentNo);
                    request.setName(name);
                    request.setClassId(classId);
                    saveStudent(request);
                    success++;
                } catch (BizException ex) {
                    errors.add(Map.of("row", rowNo, "message", ex.getMessage()));
                }
            }
        } catch (Exception e) {
            throw BizException.invalid("文件解析失败，请使用 .xlsx 模板（学号、姓名、班级名称）");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return result;
    }

    public byte[] exportInteraction() {
        List<LeaveRequest> leaves = leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .orderByDesc(LeaveRequest::getCreateTime));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("请假记录");
            String[] headers = {"学生", "任务ID", "原因", "状态", "备注", "申请时间"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            int index = 1;
            for (LeaveRequest leave : leaves) {
                Student student = studentMapper.selectById(leave.getStudentId());
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(student == null ? String.valueOf(leave.getStudentId()) : student.getName());
                row.createCell(1).setCellValue(leave.getTaskId());
                row.createCell(2).setCellValue(leave.getReason());
                row.createCell(3).setCellValue(leave.getStatus());
                row.createCell(4).setCellValue(leave.getRemark() == null ? "" : leave.getRemark());
                row.createCell(5).setCellValue(leave.getCreateTime() == null ? "" : leave.getCreateTime().toString());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "导出失败");
        }
    }

    public void resetDemoData() {
        demoDataService.reset();
        logService.log("ADMIN", "RESET_DEMO", null, "重置演示数据");
    }

    /* ===================== 映射工具 ===================== */

    private Map<String, Object> studentMap(Student student) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", student.getId());
        map.put("studentNo", student.getStudentNo());
        map.put("name", student.getName());
        map.put("classId", student.getClassId());
        map.put("className", access.className(student.getClassId()));
        map.put("userId", student.getUserId());
        SysUser user = student.getUserId() == null ? null : userMapper.selectById(student.getUserId());
        map.put("phone", user == null ? null : user.getPhone());
        map.put("enabled", user != null && user.getEnabled() != null && user.getEnabled() == 1);
        map.put("createTime", student.getCreateTime());
        return map;
    }

    private Map<String, Object> classMap(ClassGroup group) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        map.put("grade", group.getGrade());
        map.put("major", group.getMajor());
        map.put("advisorId", group.getAdvisorId());
        map.put("createTime", group.getCreateTime());
        map.put("studentCount", studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, group.getId())));
        return map;
    }

    private Map<String, Object> courseMap(Course course) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", course.getId());
        map.put("courseNo", course.getCourseNo());
        map.put("name", course.getName());
        map.put("teacherId", course.getTeacherId());
        map.put("teacherName", access.teacherName(course.getTeacherId()));
        map.put("semester", course.getSemester());
        map.put("credit", course.getCredit());
        map.put("coverColor", course.getCoverColor());
        map.put("classroom", course.getClassroom());
        map.put("longitude", course.getLongitude());
        map.put("latitude", course.getLatitude());
        map.put("defaultRadius", course.getDefaultRadius());
        List<Long> classIds = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, course.getId()))
                .stream().map(CourseClass::getClassId).toList();
        map.put("classIds", classIds);
        map.put("classNames", classIds.stream().map(access::className).toList());
        map.put("studentCount", access.courseStudents(course.getId()).size());
        return map;
    }

    private String cell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
