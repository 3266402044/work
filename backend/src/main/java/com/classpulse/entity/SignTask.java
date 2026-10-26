package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sign_task")
public class SignTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long teacherId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime closedAt;
    private Long courseId;
    private Long classId;
    private String signModes;
    private LocalDateTime lateTime;
    private String locationName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer radiusMeters;
    private String passcodeHash;
}
