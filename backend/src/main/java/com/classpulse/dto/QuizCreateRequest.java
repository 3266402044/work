package com.classpulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizCreateRequest {
    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotBlank(message = "请输入题目")
    private String title;

    /** SINGLE / MULTIPLE / TRUE_FALSE */
    @NotBlank(message = "请选择题型")
    private String type;

    private List<String> options;

    /** 正确选项下标，多选可多个 */
    private List<Integer> correctAnswer;

    @NotNull(message = "请设置答题时长")
    @Min(value = 10, message = "答题时长至少 10 秒")
    @Max(value = 600, message = "答题时长最多 600 秒")
    private Integer durationSeconds;

    private Boolean anonymous;
    private String explanation;
}
