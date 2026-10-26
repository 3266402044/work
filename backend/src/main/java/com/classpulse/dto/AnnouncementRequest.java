package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotBlank(message = "请输入公告内容")
    @Size(max = 1000, message = "公告内容不能超过 1000 字")
    private String content;

    private Boolean pinned;
    private LocalDateTime expireAt;
}
