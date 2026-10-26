package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CourseSaveRequest {
    private Long id;

    @NotBlank(message = "请输入课程编号")
    private String courseNo;

    @NotBlank(message = "请输入课程名称")
    private String name;

    @NotNull(message = "请选择授课教师")
    private Long teacherId;

    private String semester;
    private BigDecimal credit;
    private String coverColor;
    private String classroom;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer defaultRadius;
    private List<Long> classIds;
}
