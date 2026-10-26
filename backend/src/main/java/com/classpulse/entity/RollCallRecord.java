package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("roll_call_record")
public class RollCallRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long studentId;
    private Integer drawOrder;
    private String result;
    private String performance;
    private LocalDateTime drawnAt;
    private LocalDateTime respondedAt;
    private String remark;
}
