package com.sakura.taskmanager.controller;


import com.sakura.taskmanager.entity.Result;
import com.sakura.taskmanager.entity.User;
import com.sakura.taskmanager.service.UserService;
import com.sakura.taskmanager.utils.JwtUtil;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户控制器类，处理用户相关的HTTP请求
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    // 注入用户服务
    @Autowired
    private UserService userService;
    // 注入密码编码器
    @Autowired
    private PasswordEncoder passwordEncoder;
    // 注入Redis模板
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 用户注册接口
     * @param username 用户名，必须符合5-16位非空字符的正则表达式
     * @param password 密码，必须符合5-16位非空字符的正则表达式
     * @return 返回注册结果
     */
    @PostMapping("/register")
    public Result register(@Pattern(regexp = "^\\S{5,16}$") String username,@Pattern(regexp = "^\\S{5,16}$") String password){
        // 查找用户是否已存在
        User u=userService.findByUsername(username);
        if(u==null){
            // 用户不存在，执行注册
            userService.register(username,password);
            return Result.success();
        }else{
            // 用户已存在，返回错误信息
            return Result.error("用户已存在");
        }
    }
    /**
     * 用户登录接口
     * @param username 用户名，必须符合5-16位非空字符的正则表达式
     * @param password 密码，必须符合5-16位非空字符的正则表达式
     * @return 返回登录结果，包含JWT token
     */
    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,16}$")String username,@Pattern(regexp = "^\\S{5,16}$")String password){
        // 查找用户
        User loginUser=userService.findByUsername(username);
        if(loginUser==null){
            return Result.error("用户不存在");
        }
        //成功
        if(passwordEncoder.matches(password,loginUser.getPassword())){
            Map<String,Object> claims=new HashMap<>();
            claims.put("id",loginUser.getId());
            claims.put("username",loginUser.getUsername());
            String token= JwtUtil.genToken(claims);
            //将token存入redis，设置过期时间1小时
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            operations.set(token,token,1, TimeUnit.HOURS);
            //将用户名存入redis
            operations.set("username",loginUser.getUsername());
            return Result.success(token);
        }
        return Result.error("密码错误");
    }
    @GetMapping("/info")
    public Result<User> getUserInfo(){
        //通过redis获取用户名
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String username=operations.get("username");
        //通过用户名获取用户信息
        User user=userService.findByUsername(username);
        return Result.success(user);
    }
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String,String> params){
        //1.校验参数
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if(!StringUtils.hasLength(oldPwd)
                ||!StringUtils.hasLength(newPwd)
                ||!StringUtils.hasLength(rePwd)){
            return Result.error("参数不能为空");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = (Integer) authentication.getPrincipal();
        User loginUser = userService.findById(userId);

        if(!passwordEncoder.matches(oldPwd,loginUser.getPassword())){
            return Result.error("密码错误");
        }
        if(!newPwd.equals(rePwd)){
            return Result.error("两次密码不一致");
        }
        if(newPwd.equals(oldPwd)){
            return Result.error("新密码不能与旧密码相同");
        }
        //2.更新密码
        userService.updatePwd(newPwd, userId);
        System.out.println(loginUser);
        //清除 SecurityContextHolder 中的认证信息
        String token = authentication.getCredentials().toString();
        stringRedisTemplate.delete(token);
        return Result.success();

    }


    @PostMapping("/logout")
    public Result logout(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //清除 SecurityContextHolder 中的认证信息
        String token = authentication.getCredentials().toString();
        stringRedisTemplate.delete(token);
        return Result.success("退出成功!");
    }
}
