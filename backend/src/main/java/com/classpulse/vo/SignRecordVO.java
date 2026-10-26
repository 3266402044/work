package com.classpulse.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SignRecordVO {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long courseId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String className;
    private String courseName;
    private LocalDateTime signTime;
    private String status;
    private String statusLabel;
    private String method;
    private String methodLabel;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal distanceMeters;
    private String remark;
    private LocalDateTime createTime;
}
