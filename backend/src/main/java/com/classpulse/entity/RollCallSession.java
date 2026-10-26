package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("roll_call_session")
public class RollCallSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long classId;
    private Long teacherId;
    private String candidateScope;
    private Integer drawCount;
    private String antiRepeatRule;
    private Integer excludeLeave;
    private Long taskId;
    private String customStudentIds;
    private Integer responseDeadlineSeconds;
    private String status;
    private Long seed;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
