package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.User;
import com.example.secondhandplatform.mapper.UserMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        // 1. 简单校验一下：学号和密码不能为空
        if (user.getStudentId() == null || user.getPassword() == null) {
            return Result.error("注册失败：学号和密码不能为空！");
        }

        // 2. 校验学号是否已经被注册过
        // MyBatis-Plus 提供了一种不用写 SQL 的查询方式，叫 QueryWrapper
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("student_id", user.getStudentId());
        Long count = userMapper.selectCount(queryWrapper);

        if (count > 0) {
            return Result.error("注册失败：该学号已被注册！");
        }

        // 把明文密码加密，再存进数据库
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 3. 保存到数据库
        // 注意：这里为了演示，我们暂时存明文密码。下节课老师教你怎么加密。
        userMapper.insert(user);

        return Result.success("注册成功！欢迎加入校园二手集市！");
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {
        // 1. 根据学号查询用户
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("student_id", user.getStudentId());
        User dbUser = userMapper.selectOne(queryWrapper);

        // 2. 判断用户是否存在
        if (dbUser == null) {
            return Result.error("登录失败：该学号未注册！");
        }

        // 3. 校验密码 (这里非常关键！)
        // passwordEncoder.matches(明文, 数据库里的密文) 会自动比对
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return Result.error("登录失败：密码错误！");
        }

        // 4. 校验成功，生成 Token 返回给前端
        String token = com.example.secondhandplatform.utils.JwtUtils.generateToken(dbUser.getId(), dbUser.getStudentId());

        // 使用 Map 包装返回数据
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("token", token);
        data.put("userId", dbUser.getId());
        data.put("nickname", dbUser.getNickname());

        return Result.success(data);
    }

    @GetMapping("/info")
    public Result<?> getUserInfo(HttpServletRequest request) {
        // 1. 从拦截器里拿到 userId
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("获取失败：未登录");
        }

        // 2. 根据 ID 查数据库
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("获取失败：用户不存在");
        }

        // 3. 核心安全操作：绝对不能把密码返回给前端！
        user.setPassword(null);

        return Result.success(user);
    }
}