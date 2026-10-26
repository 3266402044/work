package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RollCallResultRequest {
    @NotBlank(message = "请选择点名结果")
    private String result;
    private String performance;
    private String remark;
}
