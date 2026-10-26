package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeaveApplyRequest {
    @NotNull(message = "请选择请假对应的签到任务")
    private Long taskId;

    @NotBlank(message = "请填写请假原因")
    @Size(max = 255, message = "请假原因不能超过 255 字")
    private String reason;

    private String proofUrl;
}
