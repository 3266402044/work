package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String role;
    private String realName;
    private String phone;
    private String avatarUrl;
    private Integer enabled;
    private Long relatedId;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
