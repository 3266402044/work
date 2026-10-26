package com.classpulse.vo;

import com.classpulse.entity.SignRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskVO {
    private Long id;
    private String title;
    private Long teacherId;
    private String teacherName;
    private Long courseId;
    private String courseName;
    private String courseColor;
    private Long classId;
    private String className;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime lateTime;
    private LocalDateTime closedAt;
    private LocalDateTime createTime;
    private String taskStatus;
    private String taskStatusLabel;
    private List<String> signModes;
    private String locationName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer radiusMeters;
    private Boolean signed;
    private Boolean canSign;
    private String signHint;
    private SignRecord myRecord;
    private long totalStudents;
    private long signedCount;
    private long normalCount;
    private long lateCount;
    private long leaveCount;
    private long absentCount;
    private double attendanceRate;
    private Long secondsToStart;
    private Long secondsToEnd;
    private String passcode;
}
