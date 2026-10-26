package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.dto.AnnouncementRequest;
import com.classpulse.entity.ClassAnnouncement;
import com.classpulse.entity.Course;
import com.classpulse.mapper.ClassAnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final ClassAnnouncementMapper announcementMapper;
    private final AccessService access;
    private final NotificationService notificationService;
    private final OperationLogService logService;
    private final SseHub sseHub;

    @Transactional
    public ClassAnnouncement create(AnnouncementRequest request) {
        Course course = access.requireCourse(request.getCourseId());
        access.assertCourseManageable(course.getId());
        Long teacherId = SecurityUtils.isAdmin() ? course.getTeacherId() : SecurityUtils.teacherId();
        ClassAnnouncement announcement = new ClassAnnouncement();
        announcement.setCourseId(course.getId());
        announcement.setTeacherId(teacherId);
        announcement.setContent(request.getContent().trim());
        announcement.setPinned(Boolean.TRUE.equals(request.getPinned()) ? 1 : 0);
        announcement.setExpireAt(request.getExpireAt());
        announcement.setCreateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);

        access.courseStudents(course.getId()).forEach(student -> notificationService.send(student.getUserId(),
                "课堂公告", "《" + course.getName() + "》" + announcement.getContent(), "ANNOUNCEMENT", announcement.getId()));
        sseHub.broadcast("announcement:new", java.util.Map.of("courseId", course.getId(),
                "content", announcement.getContent()));
        logService.log("ANNOUNCEMENT", "CREATE", announcement.getId(), "发布课堂公告");
        return announcement;
    }

    public List<ClassAnnouncement> list(Long courseId) {
        if (courseId == null) {
            return List.of();
        }
        return announcementMapper.selectList(new LambdaQueryWrapper<ClassAnnouncement>()
                .eq(ClassAnnouncement::getCourseId, courseId)
                .orderByDesc(ClassAnnouncement::getPinned)
                .orderByDesc(ClassAnnouncement::getCreateTime)
                .last("limit 50"));
    }

    public void delete(Long id) {
        ClassAnnouncement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BizException.notFound("公告不存在");
        }
        access.assertCourseManageable(announcement.getCourseId());
        announcementMapper.deleteById(id);
        logService.log("ANNOUNCEMENT", "DELETE", id, "删除课堂公告");
    }
}
