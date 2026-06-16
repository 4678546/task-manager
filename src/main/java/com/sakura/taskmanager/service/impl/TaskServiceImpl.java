package com.sakura.taskmanager.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sakura.taskmanager.entity.Task;
import com.sakura.taskmanager.mapper.TaskMapper;
import com.sakura.taskmanager.service.TaskService;
import com.sakura.taskmanager.utils.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class TaskServiceImpl implements TaskService {
    // 注入任务数据访问层
    @Autowired
    private TaskMapper taskMapper;



    @Override
    public Task findById(Integer id) {
        return taskMapper.findById(id);
    }

    @Override
    public void update(Task task) {
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.update(task);
    }

    @Override
    public void addTask(Task task) {
        // 设置分页结果
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.addTask(task);
    }


    @Override
    public PageBean<Task> list(Integer pageNum, Integer pageSize, String status, String deadline) {
        PageBean<Task> pageBean=new PageBean<>();
        PageHelper.startPage(pageNum,pageSize);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = (Integer) auth.getPrincipal();
        System.out.println("userId: " + userId);

        List<Task> task= taskMapper.list(userId,status,deadline);
        PageInfo<Task> pageInfo= new PageInfo<>(task);
        pageBean.setTotal(pageInfo.getTotal());
        pageBean.setItems(pageInfo.getList());
        return pageBean;
    }

    @Override
    public void deleteById(Integer id) {
        taskMapper.deleteById(id);
    }
}
