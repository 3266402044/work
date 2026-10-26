package com.classpulse.common;

import com.classpulse.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static LoginUser currentOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser user)) {
            return null;
        }
        return user;
    }

    public static LoginUser current() {
        LoginUser user = currentOrNull();
        if (user == null) {
            throw BizException.unauthorized("请先登录");
        }
        return user;
    }

    public static boolean isAdmin() {
        LoginUser user = currentOrNull();
        return user != null && "ADMIN".equals(user.role());
    }

    public static Long studentId() {
        LoginUser user = current();
        if (!"STUDENT".equals(user.role()) || user.relatedId() == null) {
            throw BizException.forbidden("仅学生可执行该操作");
        }
        return user.relatedId();
    }

    public static Long teacherId() {
        LoginUser user = current();
        if (!"TEACHER".equals(user.role()) || user.relatedId() == null) {
            throw BizException.forbidden("仅教师可执行该操作");
        }
        return user.relatedId();
    }
}
