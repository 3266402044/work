package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.dto.LeaveApplyRequest;
import com.classpulse.entity.LeaveRequest;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.mapper.LeaveRequestMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaveService {
    private final LeaveRequestMapper leaveMapper;
    private final SignRecordMapper recordMapper;
    private final SignTaskMapper taskMapper;
    private final AccessService access;
    private final NotificationService notificationService;
    private final OperationLogService logService;
    private final SseHub sseHub;

    @Transactional
    public void apply(LeaveApplyRequest request) {
        Long studentId = SecurityUtils.studentId();
        Student student = access.requireStudent(studentId);
        SignTask task = access.requireTask(request.getTaskId());
        access.assertStudentEligible(student, task);
        if (task.getEndTime().isBefore(LocalDateTime.now().minusHours(24))) {
            throw BizException.invalid("任务已结束超过 24 小时，无法补交请假申请");
        }
        SignRecord record = recordMapper.selectOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getTaskId, task.getId()).eq(SignRecord::getStudentId, studentId).last("limit 1"));
        if (record != null) {
            throw BizException.conflict("你已签到，无需请假");
        }
        long pending = leaveMapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId)
                .eq(LeaveRequest::getTaskId, task.getId())
                .in(LeaveRequest::getStatus, List.of("PENDING", "APPROVED")));
        if (pending > 0) {
            throw BizException.conflict("该任务已有待处理或已通过的请假申请");
        }
        LeaveRequest leave = new LeaveRequest();
        leave.setStudentId(studentId);
        leave.setTaskId(task.getId());
        leave.setReason(request.getReason().trim());
        leave.setProofUrl(request.getProofUrl());
        leave.setStatus("PENDING");
        leave.setCreateTime(LocalDateTime.now());
        leaveMapper.insert(leave);

        notificationService.send(task.getTeacherId() == null ? null : teacherUserId(task.getTeacherId()),
                "新的请假申请", student.getName() + " 提交了《" + task.getTitle() + "》的请假申请，请及时审批。",
                "LEAVE", leave.getId());
        logService.log("LEAVE", "APPLY", leave.getId(), "提交请假申请");
    }

    public List<Map<String, Object>> myLeaves() {
        Long studentId = SecurityUtils.studentId();
        List<LeaveRequest> list = leaveMapper.selectList(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getStudentId, studentId).orderByDesc(LeaveRequest::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (LeaveRequest leave : list) {
            SignTask task = taskMapper.selectById(leave.getTaskId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", leave.getId());
            map.put("taskId", leave.getTaskId());
            map.put("taskTitle", task == null ? "任务已删除" : task.getTitle());
            map.put("courseName", task == null ? null : access.courseName(task.getCourseId()));
            map.put("startTime", task == null ? null : task.getStartTime());
            map.put("reason", leave.getReason());
            map.put("proofUrl", leave.getProofUrl());
            map.put("status", leave.getStatus());
            map.put("remark", leave.getRemark());
            map.put("createTime", leave.getCreateTime());
            map.put("reviewTime", leave.getReviewTime());
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> teacherLeaves(String status) {
        Long teacherId = SecurityUtils.isAdmin() ? null : SecurityUtils.teacherId();
        LambdaQueryWrapper<SignTask> taskWrapper = new LambdaQueryWrapper<>();
        if (teacherId != null) {
            taskWrapper.eq(SignTask::getTeacherId, teacherId);
        }
        List<Long> taskIds = taskMapper.selectList(taskWrapper).stream().map(SignTask::getId).toList();
        if (taskIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<LeaveRequest>()
                .in(LeaveRequest::getTaskId, taskIds).orderByDesc(LeaveRequest::getCreateTime);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(LeaveRequest::getStatus, status);
        }
        List<LeaveRequest> list = leaveMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LeaveRequest leave : list) {
            SignTask task = taskMapper.selectById(leave.getTaskId());
            Student student = access.requireStudent(leave.getStudentId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", leave.getId());
            map.put("taskId", leave.getTaskId());
            map.put("taskTitle", task == null ? null : task.getTitle());
            map.put("courseName", task == null ? null : access.courseName(task.getCourseId()));
            map.put("studentId", student.getId());
            map.put("studentNo", student.getStudentNo());
            map.put("studentName", student.getName());
            map.put("className", access.className(student.getClassId()));
            map.put("reason", leave.getReason());
            map.put("proofUrl", leave.getProofUrl());
            map.put("status", leave.getStatus());
            map.put("remark", leave.getRemark());
            map.put("createTime", leave.getCreateTime());
            map.put("reviewTime", leave.getReviewTime());
            result.add(map);
        }
        return result;
    }

    @Transactional
    public void review(Long leaveId, boolean approved, String remark) {
        LeaveRequest leave = leaveMapper.selectById(leaveId);
        if (leave == null) {
            throw BizException.notFound("请假申请不存在");
        }
        SignTask task = access.requireTask(leave.getTaskId());
        access.assertTaskManageable(task);
        if (!"PENDING".equals(leave.getStatus())) {
            throw BizException.conflict("该申请已处理，不能重复审批");
        }
        leave.setStatus(approved ? "APPROVED" : "REJECTED");
        leave.setReviewerId(SecurityUtils.current().relatedId());
        leave.setReviewTime(LocalDateTime.now());
        leave.setRemark(remark);
        leaveMapper.updateById(leave);

        Student student = access.requireStudent(leave.getStudentId());
        if (approved) {
            SignRecord record = recordMapper.selectOne(new LambdaQueryWrapper<SignRecord>()
                    .eq(SignRecord::getTaskId, task.getId())
                    .eq(SignRecord::getStudentId, student.getId()).last("limit 1"));
            if (record == null) {
                record = new SignRecord();
                record.setTaskId(task.getId());
                record.setStudentId(student.getId());
                record.setSignTime(LocalDateTime.now());
                record.setStatus("LEAVE");
                record.setMethod("MANUAL");
                record.setRemark("请假审批通过：" + remark);
                record.setOperatorId(SecurityUtils.current().relatedId());
                record.setCreateTime(LocalDateTime.now());
                recordMapper.insert(record);
            }
        }
        notificationService.send(student.getUserId(), "请假审批结果",
                "你在《" + task.getTitle() + "》的请假申请" + (approved ? "已通过" : "被驳回")
                        + (remark == null || remark.isBlank() ? "。" : "，备注：" + remark),
                "LEAVE", leave.getId());
        logService.log("LEAVE", approved ? "APPROVE" : "REJECT", leaveId, "审批请假申请");
        sseHub.send(student.getUserId(), "leave:result", Map.of("leaveId", leaveId, "approved", approved));
        sseHub.broadcast("sign:new", Map.of("taskId", task.getId()));
    }

    private Long teacherUserId(Long teacherId) {
        var teacher = access.requireTeacher(teacherId);
        return teacher.getUserId();
    }
}
