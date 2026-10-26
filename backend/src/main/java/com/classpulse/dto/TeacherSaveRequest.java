package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeacherSaveRequest {
    private Long id;

    @NotBlank(message = "请输入工号")
    private String teacherNo;

    @NotBlank(message = "请输入姓名")
    private String name;

    private String department;
    private String phone;
}
