package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassSaveRequest {
    private Long id;

    @NotBlank(message = "请输入班级名称")
    private String name;

    private String grade;
    private String major;
    private Long advisorId;
}
