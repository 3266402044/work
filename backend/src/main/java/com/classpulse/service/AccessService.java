package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.entity.ClassGroup;
import com.classpulse.entity.Course;
import com.classpulse.entity.CourseClass;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.entity.Teacher;
import com.classpulse.mapper.ClassGroupMapper;
import com.classpulse.mapper.CourseClassMapper;
import com.classpulse.mapper.CourseMapper;
import com.classpulse.mapper.SignTaskMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final ClassGroupMapper classGroupMapper;
    private final SignTaskMapper signTaskMapper;

    public Student requireStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw BizException.notFound("学生档案不存在");
        }
        return student;
    }

    public Teacher requireTeacher(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw BizException.notFound("教师档案不存在");
        }
        return teacher;
    }

    public SignTask requireTask(Long taskId) {
        SignTask task = signTaskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.notFound("签到任务不存在或已被删除");
        }
        return task;
    }

    public Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }
        return course;
    }

    public ClassGroup requireClass(Long classId) {
        ClassGroup group = classGroupMapper.selectById(classId);
        if (group == null) {
            throw BizException.notFound("班级不存在");
        }
        return group;
    }

    public boolean studentInTask(Student student, SignTask task) {
        if (student == null || task == null) {
            return false;
        }
        if (task.getClassId() != null) {
            return task.getClassId().equals(student.getClassId());
        }
        if (task.getCourseId() != null && student.getClassId() != null) {
            return courseClassMapper.selectCount(new LambdaQueryWrapper<CourseClass>()
                    .eq(CourseClass::getCourseId, task.getCourseId())
                    .eq(CourseClass::getClassId, student.getClassId())) > 0;
        }
        return false;
    }

    public void assertStudentEligible(Student student, SignTask task) {
        if (!studentInTask(student, task)) {
            throw BizException.forbidden("你不属于该签到任务的班级或课程，无法签到");
        }
    }

    public void assertTaskReadable(SignTask task) {
        var user = SecurityUtils.current();
        if ("ADMIN".equals(user.role())) {
            return;
        }
        if ("TEACHER".equals(user.role())) {
            if (task.getTeacherId().equals(user.relatedId())) {
                return;
            }
            throw BizException.forbidden("该任务不属于你的授课范围");
        }
        Student student = requireStudent(user.relatedId());
        assertStudentEligible(student, task);
    }

    public void assertTaskManageable(SignTask task) {
        var user = SecurityUtils.current();
        if ("ADMIN".equals(user.role())) {
            return;
        }
        if ("TEACHER".equals(user.role()) && task.getTeacherId().equals(user.relatedId())) {
            return;
        }
        throw BizException.forbidden("只有任务创建者或管理员可以执行该操作");
    }

    public void assertCourseManageable(Long courseId) {
        var user = SecurityUtils.current();
        if ("ADMIN".equals(user.role())) {
            return;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }
        if ("TEACHER".equals(user.role()) && course.getTeacherId().equals(user.relatedId())) {
            return;
        }
        throw BizException.forbidden("你只能管理自己授课的课程");
    }

    /** 任务应到学生名单：优先按班级，其次按课程关联班级 */
    public List<Student> roster(SignTask task) {
        Map<Long, Student> map = new LinkedHashMap<>();
        if (task.getClassId() != null) {
            studentMapper.selectList(new LambdaQueryWrapper<Student>()
                            .eq(Student::getClassId, task.getClassId()).orderByAsc(Student::getStudentNo))
                    .forEach(s -> map.put(s.getId(), s));
        }
        if (task.getCourseId() != null) {
            courseStudents(task.getCourseId()).forEach(s -> map.putIfAbsent(s.getId(), s));
        }
        return new ArrayList<>(map.values());
    }

    public List<Student> courseStudents(Long courseId) {
        Set<Long> classIds = new LinkedHashSet<>();
        courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, courseId))
                .forEach(cc -> classIds.add(cc.getClassId()));
        if (classIds.isEmpty()) {
            return List.of();
        }
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .in(Student::getClassId, classIds).orderByAsc(Student::getStudentNo));
    }

    public List<ClassGroup> classesOfCourse(Long courseId) {
        List<CourseClass> relations = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getCourseId, courseId));
        if (relations.isEmpty()) {
            return List.of();
        }
        return classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                .in(ClassGroup::getId, relations.stream().map(CourseClass::getClassId).toList()));
    }

    public List<Course> coursesOfStudent(Student student) {
        if (student.getClassId() == null) {
            return List.of();
        }
        List<CourseClass> relations = courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getClassId, student.getClassId()));
        if (relations.isEmpty()) {
            return List.of();
        }
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .in(Course::getId, relations.stream().map(CourseClass::getCourseId).toList()));
    }

    public List<Course> coursesOfTeacher(Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            wrapper.eq(Course::getTeacherId, teacherId);
        }
        wrapper.orderByAsc(Course::getCourseNo);
        return courseMapper.selectList(wrapper);
    }

    public String courseName(Long courseId) {
        if (courseId == null) {
            return null;
        }
        Course course = courseMapper.selectById(courseId);
        return course == null ? null : course.getName();
    }

    public String className(Long classId) {
        if (classId == null) {
            return null;
        }
        ClassGroup group = classGroupMapper.selectById(classId);
        return group == null ? null : group.getName();
    }

    public String teacherName(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        Teacher teacher = teacherMapper.selectById(teacherId);
        return teacher == null ? null : teacher.getName();
    }
}
