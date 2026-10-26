package com.classpulse.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String role;
    private String realName;
    private String phone;
    private String avatarUrl;
    private Boolean enabled;
    private Long relatedId;
    private String relatedNo;
    private Long classId;
    private String className;
    private String department;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
