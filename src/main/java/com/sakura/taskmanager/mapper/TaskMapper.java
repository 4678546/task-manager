package com.sakura.taskmanager.mapper;

import com.sakura.taskmanager.entity.Task;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaskMapper {
    @Insert({"insert into task (title,content,user_id,create_time,update_time,deadline,status) " +
            "values (#{title},#{content},#{userId},#{createTime},#{updateTime},#{deadline},#{status})"})
    void addTask(Task task);

    List<Task> list(@Param("userId") Integer userId,
                    @Param("status") String status,
                    @Param("deadline") String deadline);
;
    @Delete("delete from task where id=#{id}")
    void deleteById(Integer id);
    @Select("select * from task where id=#{id}")
    Task findById(Integer id);

    void update(Task task);
}
