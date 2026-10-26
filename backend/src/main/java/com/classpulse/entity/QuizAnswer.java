package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_answer")
public class QuizAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long quizId;
    private Long studentId;
    private String answerJson;
    private Integer isCorrect;
    private LocalDateTime answerTime;
}
