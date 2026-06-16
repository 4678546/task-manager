package com.sakura.taskmanager.service;


import com.sakura.taskmanager.entity.Task;
import com.sakura.taskmanager.utils.PageBean;

import java.util.List;

public interface TaskService {
    void addTask(Task task);

    PageBean<Task> list(Integer pageNum, Integer pageSize, String status, String deadline);

    void deleteById(Integer id);

    Task findById(Integer id);

    void update(Task task);
}
