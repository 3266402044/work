package com.classpulse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_class")
public class CourseClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long classId;
}
