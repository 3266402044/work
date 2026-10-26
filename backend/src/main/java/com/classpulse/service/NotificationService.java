package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.PageResult;
import com.classpulse.entity.SysNotification;
import com.classpulse.entity.Student;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SysNotificationMapper notificationMapper;
    private final StudentMapper studentMapper;
    private final SseHub sseHub;

    public void send(Long userId, String title, String content, String type, Long relatedId) {
        if (userId == null) {
            return;
        }
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setReadFlag(0);
        notification.setRelatedId(relatedId);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        sseHub.send(userId, "notification", toMap(notification));
    }

    public void sendToClass(Long classId, String title, String content, String type, Long relatedId) {
        if (classId == null) {
            return;
        }
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId));
        students.forEach(s -> send(s.getUserId(), title, content, type, relatedId));
    }

    public List<SysNotification> list() {
        Long userId = com.classpulse.common.SecurityUtils.current().userId();
        return notificationMapper.selectList(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .orderByDesc(SysNotification::getCreateTime)
                .last("limit 100"));
    }

    public long unreadCount() {
        Long userId = com.classpulse.common.SecurityUtils.current().userId();
        return notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getReadFlag, 0));
    }

    public void markRead(Long id) {
        SysNotification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw BizException.notFound("消息不存在");
        }
        if (!notification.getUserId().equals(com.classpulse.common.SecurityUtils.current().userId())) {
            throw BizException.forbidden("无权操作该消息");
        }
        notification.setReadFlag(1);
        notificationMapper.updateById(notification);
    }

    public void markAllRead() {
        Long userId = com.classpulse.common.SecurityUtils.current().userId();
        List<SysNotification> list = notificationMapper.selectList(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId).eq(SysNotification::getReadFlag, 0));
        list.forEach(n -> {
            n.setReadFlag(1);
            notificationMapper.updateById(n);
        });
    }

    public PageResult<SysNotification> page(long page, long size) {
        Long userId = com.classpulse.common.SecurityUtils.current().userId();
        var p = notificationMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<SysNotification>().eq(SysNotification::getUserId, userId)
                        .orderByDesc(SysNotification::getCreateTime));
        return PageResult.of(p.getRecords(), p.getTotal(), page, size);
    }

    private java.util.Map<String, Object> toMap(SysNotification n) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("title", n.getTitle());
        map.put("content", n.getContent());
        map.put("type", n.getType());
        map.put("readFlag", n.getReadFlag());
        map.put("createTime", n.getCreateTime());
        return map;
    }
}
