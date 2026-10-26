package com.classpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classpulse.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
