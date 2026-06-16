package com.sakura.taskmanager.service;


import com.sakura.taskmanager.entity.User;

public interface UserService {
    User findByUsername(String username);

    void register(String username, String password);

    User findById(Integer userId);

    void updatePwd(String newPwd, Integer userId);
}
