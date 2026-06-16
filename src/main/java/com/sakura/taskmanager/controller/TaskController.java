package com.sakura.taskmanager.controller;

import com.sakura.taskmanager.entity.Result;
import com.sakura.taskmanager.entity.Task;
import com.sakura.taskmanager.service.TaskService;
import com.sakura.taskmanager.utils.PageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 任务控制器类
 * 处理任务相关的HTTP请求，包括添加、查询、更新和删除任务
 */
@RestController
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private TaskService taskService;  // 任务服务，用于处理任务相关的业务逻辑

    @PostMapping
    public Result addTask(@RequestBody @Validated Task task){
        // 获取当前认证的用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = (Integer) auth.getPrincipal();
        // 设置任务的用户ID
        task.setUserId(userId);
        // 调用服务层添加任务
        taskService.addTask(task);
        return Result.success();
    }

    @GetMapping //分页查询
    public Result<PageBean<Task>> list(Integer pageNum,
                                       Integer pageSize,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String deadline){

        PageBean<Task> pageBean = taskService.list(pageNum, pageSize, status, deadline);

         return Result.success(pageBean);
    }
    @PutMapping
    public Result update(@RequestBody Task task){
        Integer id = task.getId();
        // 检查任务ID是否为空
        if(id == null){
            return Result.error("任务id不能为空，更新失败!");
        }
        // 检查任务是否存在
        Task existingTask = taskService.findById(id);
        if(existingTask==null){
            return Result.error("任务不存在,更新失败!");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = (Integer) auth.getPrincipal();
        if(!userId.equals(existingTask.getUserId())){
            return Result.error("无权更新该任务");
        }
        // 调用服务层更新任务
        taskService.update(task);
        return Result.success();
    }
    @DeleteMapping
    public Result deleteById(Integer id){
        // 检查任务是否为空
        Task task = taskService.findById(id);
        if(task==null){
            return Result.error("任务不存在,删除失败!");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = (Integer) auth.getPrincipal();
        if(!userId.equals(task.getUserId())){
            return Result.error("无权删除该任务");
        }
        // 调用服务层删除任务
        taskService.deleteById(id);
        return Result.success();
    }
    @GetMapping("/search")
    public Result<Task> getTaskInfo(@RequestParam(required = false) Integer id){
        if (id == null) {
            return Result.error("任务id不能为空，查询失败!");
        }

        Task task = taskService.findById(id);
        if(task==null){
            return Result.error("任务不存在,查询失败!");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = (Integer) auth.getPrincipal();
        if(!userId.equals(task.getUserId())){
            return Result.error("无权查看该任务");
        }

        return Result.success(task);
    }
}
