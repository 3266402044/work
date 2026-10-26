package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentSaveRequest {
    private Long id;

    @NotBlank(message = "请输入学号")
    private String studentNo;

    @NotBlank(message = "请输入姓名")
    private String name;

    @NotNull(message = "请选择班级")
    private Long classId;

    private String phone;
}
