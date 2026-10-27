package com.example.secondhandplatform;

import com.example.secondhandplatform.entity.User;
import com.example.secondhandplatform.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testSelect() {
        System.out.println("----- 开始查询所有用户 -----");
        // selectList(null) 意思是查询这张表的所有数据
        List<User> users = userMapper.selectList(null);
        users.forEach(System.out::println);
        System.out.println("----- 查询结束 -----");
    }
}