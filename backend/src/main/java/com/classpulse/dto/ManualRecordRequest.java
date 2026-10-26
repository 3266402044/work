package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ManualRecordRequest {
    @NotNull(message = "请选择学生")
    private Long studentId;

    @NotBlank(message = "请选择记录状态")
    private String status;

    private String remark;
    private LocalDateTime signTime;
}
