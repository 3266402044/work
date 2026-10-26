package com.classpulse.security;

public record LoginUser(Long userId, String username, String role, Long relatedId, String realName) {
}
