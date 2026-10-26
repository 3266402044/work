package com.classpulse.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String phone;
    private String avatarUrl;
}
