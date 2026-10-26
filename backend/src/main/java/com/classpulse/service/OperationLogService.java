package com.classpulse.service;

import com.classpulse.common.SecurityUtils;
import com.classpulse.entity.OperationLog;
import com.classpulse.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper logMapper;

    public void log(String module, String action, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        var user = SecurityUtils.currentOrNull();
        log.setUserId(user == null ? null : user.userId());
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(clientIp());
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    public void log(Long userId, String module, String action, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(clientIp());
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    private String clientIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "internal";
            }
            var request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
