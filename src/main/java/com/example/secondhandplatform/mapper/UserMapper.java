package com.example.secondhandplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secondhandplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT COUNT(*) FROM sys_user WHERE DATE(create_time) = CURDATE()")
    long countTodayUsers();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS d, COUNT(*) AS c " +
            "FROM sys_user WHERE create_time >= #{start} GROUP BY d")
    List<Map<String, Object>> countUsersByDay(@Param("start") String start);
}
