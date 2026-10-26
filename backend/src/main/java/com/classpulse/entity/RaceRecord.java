package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("race_record")
public class RaceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long raceId;
    private Long studentId;
    private Integer seq;
    private LocalDateTime createTime;
}
