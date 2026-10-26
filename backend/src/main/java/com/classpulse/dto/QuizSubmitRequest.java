package com.classpulse.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    @NotNull(message = "请选择答案")
    private List<Integer> answers;
}
