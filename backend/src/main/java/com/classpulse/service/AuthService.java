package com.classpulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classpulse.common.BizException;
import com.classpulse.common.SecurityUtils;
import com.classpulse.dto.ChangePasswordRequest;
import com.classpulse.dto.LoginRequest;
import com.classpulse.dto.ProfileUpdateRequest;
import com.classpulse.entity.ClassGroup;
import com.classpulse.entity.Student;
import com.classpulse.entity.SysUser;
import com.classpulse.entity.Teacher;
import com.classpulse.mapper.ClassGroupMapper;
import com.classpulse.mapper.StudentMapper;
import com.classpulse.mapper.SysUserMapper;
import com.classpulse.mapper.TeacherMapper;
import com.classpulse.security.JwtService;
import com.classpulse.security.LoginUser;
import com.classpulse.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ClassGroupMapper classGroupMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OperationLogService logService;

    public Map<String, Object> login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()).last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BizException.unauthorized("用户名或密码不正确");
        }
        if (user.getEnabled() != null && user.getEnabled() == 0) {
            throw BizException.forbidden("该账号已被禁用，请联系管理员");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole(),
                user.getRelatedId(), user.getRealName());
        String token = jwtService.generate(loginUser);
        logService.log(user.getId(), "AUTH", "LOGIN", user.getId(), "用户登录");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("expireMinutes", jwtService.expireMinutes());
        result.put("user", toVO(user));
        return result;
    }

    public UserVO me() {
        LoginUser loginUser = SecurityUtils.current();
        SysUser user = userMapper.selectById(loginUser.userId());
        if (user == null) {
            throw BizException.unauthorized("账号不存在");
        }
        return toVO(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        SysUser user = userMapper.selectById(SecurityUtils.current().userId());
        if (user == null) {
            throw BizException.unauthorized("账号不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw BizException.invalid("原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        logService.log("AUTH", "CHANGE_PASSWORD", user.getId(), "修改登录密码");
    }

    public UserVO updateProfile(ProfileUpdateRequest request) {
        SysUser user = userMapper.selectById(SecurityUtils.current().userId());
        if (user == null) {
            throw BizException.unauthorized("账号不存在");
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    public UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setEnabled(user.getEnabled() != null && user.getEnabled() == 1);
        vo.setRelatedId(user.getRelatedId());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        if ("STUDENT".equals(user.getRole()) && user.getRelatedId() != null) {
            Student student = studentMapper.selectById(user.getRelatedId());
            if (student != null) {
                vo.setRelatedNo(student.getStudentNo());
                vo.setClassId(student.getClassId());
                if (student.getClassId() != null) {
                    ClassGroup group = classGroupMapper.selectById(student.getClassId());
                    vo.setClassName(group == null ? null : group.getName());
                }
            }
        } else if ("TEACHER".equals(user.getRole()) && user.getRelatedId() != null) {
            Teacher teacher = teacherMapper.selectById(user.getRelatedId());
            if (teacher != null) {
                vo.setRelatedNo(teacher.getTeacherNo());
                vo.setDepartment(teacher.getDepartment());
            }
        }
        return vo;
    }
}
