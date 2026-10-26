package com.classpulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RaceCreateRequest {
    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotBlank(message = "请输入抢答标题")
    private String title;

    @NotNull(message = "请设置抢答时长")
    @Min(value = 10, message = "抢答时长至少 10 秒")
    @Max(value = 60, message = "抢答时长最多 60 秒")
    private Integer durationSeconds;
}
