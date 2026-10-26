package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.common.TaskStatus;
import com.classpulse.common.Utils;
import com.classpulse.dto.SignRequest;
import com.classpulse.entity.SignQrToken;
import com.classpulse.entity.SignRecord;
import com.classpulse.entity.SignTask;
import com.classpulse.entity.Student;
import com.classpulse.mapper.SignQrTokenMapper;
import com.classpulse.mapper.SignRecordMapper;
import com.classpulse.mapper.SignTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignService {
    private final SignTaskMapper taskMapper;
    private final SignRecordMapper recordMapper;
    private final SignQrTokenMapper qrTokenMapper;
    private final AccessService access;
    private final TaskService taskService;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService logService;
    private final SseHub sseHub;

    @Transactional
    public Map<String, Object> sign(Long taskId, SignRequest request) {
        Long studentId = SecurityUtils.studentId();
        Student student = access.requireStudent(studentId);
        SignTask task = access.requireTask(taskId);
        access.assertStudentEligible(student, task);

        LocalDateTime now = LocalDateTime.now();
        TaskStatus taskStatus = TaskStatus.of(task, now);
        if (taskStatus == TaskStatus.NOT_STARTED) {
            throw BizException.invalid("签到尚未开始，请等待教师开启");
        }
        if (taskStatus == TaskStatus.CLOSED) {
            throw BizException.invalid("教师已提前关闭本次签到");
        }
        if (taskStatus == TaskStatus.ENDED) {
            throw BizException.invalid("签到已结束，无法提交");
        }

        List<String> modes = Utils.splitModes(task.getSignModes());
        String method = request.getMethod() == null ? "" : request.getMethod().trim().toUpperCase();
        if (!modes.contains(method)) {
            throw BizException.invalid("本次签到未开启该签到方式");
        }

        BigDecimal distance = null;
        if ("QR".equals(method)) {
            validateQrToken(task, request.getQrToken());
        } else if ("PASSCODE".equals(method)) {
            if (request.getPasscode() == null || request.getPasscode().isBlank()) {
                throw BizException.invalid("请输入 6 位签到口令");
            }
            if (task.getPasscodeHash() == null
                    || !passwordEncoder.matches(request.getPasscode().trim(), task.getPasscodeHash())) {
                throw BizException.invalid("签到口令不正确");
            }
        } else if ("LOCATION".equals(method)) {
            distance = validateLocation(task, request);
        }

        SignRecord existing = recordMapper.selectOne(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getTaskId, taskId)
                .eq(SignRecord::getStudentId, studentId)
                .last("limit 1"));
        if (existing != null) {
            throw BizException.conflict("你已签到，请勿重复提交");
        }

        String signStatus = (task.getLateTime() != null && now.isAfter(task.getLateTime())) ? "LATE" : "NORMAL";
        SignRecord record = new SignRecord();
        record.setTaskId(taskId);
        record.setStudentId(studentId);
        record.setSignTime(now);
        record.setStatus(signStatus);
        record.setMethod(method);
        record.setLongitude(request.getLongitude());
        record.setLatitude(request.getLatitude());
        record.setDistanceMeters(distance);
        record.setCreateTime(now);
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            log.warn("duplicate sign detected task={} student={}", taskId, studentId);
            throw BizException.conflict("请勿重复签到");
        }

        logService.log("SIGN", "SIGN", taskId, "学生 " + student.getStudentNo() + " 完成" + method + "签到");
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("taskId", taskId);
        event.put("studentId", studentId);
        event.put("studentName", student.getName());
        event.put("status", signStatus);
        event.put("signTime", now);
        event.put("method", method);
        sseHub.broadcast("sign:new", event);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", record);
        result.put("task", taskService.detail(taskId));
        result.put("message", "签到成功");
        return result;
    }

    private void validateQrToken(SignTask task, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw BizException.invalid("二维码凭证缺失，请重新扫码");
        }
        SignQrToken token = qrTokenMapper.selectOne(new LambdaQueryWrapper<SignQrToken>()
                .eq(SignQrToken::getTaskId, task.getId())
                .eq(SignQrToken::getTokenHash, Utils.sha256(rawToken.trim()))
                .last("limit 1"));
        if (token == null) {
            throw BizException.invalid("二维码无效，请让教师刷新二维码");
        }
        if (token.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw BizException.invalid("二维码已过期，请让教师刷新二维码");
        }
        token.setUsedCount(token.getUsedCount() == null ? 1 : token.getUsedCount() + 1);
        qrTokenMapper.updateById(token);
    }

    private BigDecimal validateLocation(SignTask task, SignRequest request) {
        if (request.getLongitude() == null || request.getLatitude() == null) {
            throw BizException.invalid("未获取到定位信息，请允许浏览器定位后重试");
        }
        if (task.getLongitude() == null || task.getLatitude() == null) {
            throw BizException.invalid("本次签到未配置签到地点，请联系教师");
        }
        BigDecimal distance = Utils.distanceMeters(task.getLongitude(), task.getLatitude(),
                request.getLongitude(), request.getLatitude());
        int radius = task.getRadiusMeters() == null ? 150 : task.getRadiusMeters();
        if (distance.compareTo(BigDecimal.valueOf(radius)) > 0) {
            throw BizException.invalid("距签到点 " + distance.stripTrailingZeros().toPlainString()
                    + " 米，超出允许范围 " + radius + " 米");
        }
        return distance;
    }
}
