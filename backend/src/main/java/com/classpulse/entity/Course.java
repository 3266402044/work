package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseNo;
    private String name;
    private Long teacherId;
    private String semester;
    private BigDecimal credit;
    private String coverColor;
    private String classroom;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer defaultRadius;
    @TableLogic
    private Integer deleted;
}
