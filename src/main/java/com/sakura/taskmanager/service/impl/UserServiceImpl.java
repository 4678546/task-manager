package com.sakura.taskmanager.service.impl;

import com.sakura.taskmanager.mapper.UserMapper;
import com.sakura.taskmanager.entity.User;
import com.sakura.taskmanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        User u= userMapper.findByUsername(username);
        return u;
    }

    @Override
    public void register(String username, String password) {
        String encodePwd = passwordEncoder.encode(password);
        userMapper.add(username, encodePwd);
    }

    @Override
    public User findById(Integer userId) {
        User u=userMapper.findById(userId);
        return u;
    }

    @Override
    public void updatePwd(String newPwd, Integer userId) {
        String encodePwd = passwordEncoder.encode(newPwd);
        userMapper.updatePwd(encodePwd, userId);
    }
}
