package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz")
public class Quiz {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long teacherId;
    private String title;
    private String type;
    private String optionsJson;
    private String correctAnswer;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer anonymous;
    private String status;
    private String explanation;
    private LocalDateTime createTime;
}
