package com.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studyroom.entity.User;

public interface UserService extends IService<User> {
    String login(String studentNo, String password);
    User getByStudentNo(String studentNo);
}
