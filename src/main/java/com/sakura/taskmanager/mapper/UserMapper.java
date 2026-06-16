package com.sakura.taskmanager.mapper;

import com.sakura.taskmanager.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("select * from user where username=#{username}")
    User findByUsername(String username);

    @Insert("insert into user(username,password,create_time,update_time) " +
            "values(#{username},#{password},now(),now()) ")
   void add(String username, String password);
    @Select("select * from user where id=#{userId}")
    User findById(Integer userId);
    @Update("update user set password=#{encodePwd},update_time=now() where id=#{userId}")
    void updatePwd(@Param("encodePwd") String encodePwd, @Param("userId") Integer userId);
}
