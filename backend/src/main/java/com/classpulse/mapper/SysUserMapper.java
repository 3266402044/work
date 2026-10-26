package com.classpulse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classpulse.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
