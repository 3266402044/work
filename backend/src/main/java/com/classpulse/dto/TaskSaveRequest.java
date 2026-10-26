package com.classpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskSaveRequest {
    @NotBlank(message = "请输入任务标题")
    @Size(max = 128, message = "任务标题不能超过 128 字")
    private String title;

    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotNull(message = "请选择班级")
    private Long classId;

    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;

    private LocalDateTime lateTime;

    @NotEmpty(message = "至少选择一种签到方式")
    private List<String> signModes;

    private String locationName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer radiusMeters;
}
