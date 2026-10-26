package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSaveRequest {
    private Long id;

    @NotBlank(message = "请输入登录名")
    private String username;

    private String password;
    private String role;
    private String realName;
    private String phone;
    private Boolean enabled;
}
