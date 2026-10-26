package com.classpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classpulse.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
