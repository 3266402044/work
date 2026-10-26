package com.classpulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RollCallCreateRequest {
    @NotNull(message = "请选择课程")
    private Long courseId;

    private Long classId;

    /** ALL / SIGNED / UNSIGNED / CUSTOM */
    @NotNull(message = "请选择候选范围")
    private String candidateScope;

    @NotNull(message = "请设置抽取人数")
    @Min(value = 1, message = "抽取人数至少 1 人")
    @Max(value = 10, message = "单次最多抽取 10 人")
    private Integer drawCount;

    /** NONE / SESSION / DAYS7 / LAST_N */
    private String antiRepeatRule;

    private Integer recentSessions;
    private Boolean excludeLeave;
    private Integer responseDeadlineSeconds;
    private Long taskId;
    private List<Long> customStudentIds;
}
