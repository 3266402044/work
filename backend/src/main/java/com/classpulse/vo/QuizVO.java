package com.classpulse.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class QuizVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String teacherName;
    private String title;
    private String type;
    private List<String> options;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean anonymous;
    private String status;
    private String explanation;
    private List<Integer> correctAnswer;
    private Boolean answered;
    private List<Integer> myAnswer;
    private Boolean correct;
    private Integer answerCount;
    private Integer totalStudents;
    private List<Map<String, Object>> distribution;
    private Double correctRate;
    private Boolean showResult;
    private Long secondsLeft;
}
