package com.classpulse.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class RollCallVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long classId;
    private String className;
    private String teacherName;
    private String candidateScope;
    private Integer drawCount;
    private String antiRepeatRule;
    private Integer excludeLeave;
    private Integer responseDeadlineSeconds;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<Map<String, Object>> records;
    private Map<String, Object> summary;
}
