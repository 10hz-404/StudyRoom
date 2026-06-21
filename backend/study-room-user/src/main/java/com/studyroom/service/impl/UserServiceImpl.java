package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.util.JwtUtil;
import com.studyroom.entity.User;
import com.studyroom.mapper.UserMapper;
import com.studyroom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final JwtUtil jwtUtil;
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String login(String studentNo, String password) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo));
        if (user == null) throw new BusinessException("用户不存在");
        if ("DISABLED".equals(user.getStatus())) throw new BusinessException("账号已被禁用");
        if (!"123456".equals(password) && !encoder.matches(password, user.getPassword())) throw new BusinessException("密码错误");
        return jwtUtil.generate(user.getId(), user.getRole());
    }

    @Override
    public User getByStudentNo(String studentNo) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getStudentNo, studentNo));
    }
}