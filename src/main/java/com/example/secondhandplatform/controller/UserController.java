package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.User;
import com.example.secondhandplatform.mapper.UserMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    @Autowired
    private com.example.secondhandplatform.mapper.ProductMapper productMapper;
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody User user) {
        // 1.如果前端没传昵称，给个默认昵称（比如“用户+学号后4位”）
        if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            String defaultNickname = "用户" + user.getStudentId().substring(Math.max(0, user.getStudentId().length() - 4));
            user.setNickname(defaultNickname);
        }

        // 2. 校验学号是否已经被注册过
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("student_id", user.getStudentId());
        Long count = userMapper.selectCount(queryWrapper);

        if (count > 0) {
            return Result.error("注册失败：该学号已被注册！");
        }

        // 3. 密码加密并保存
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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

    @PutMapping("/updateInfo")
    public Result<?> updateInfo(@RequestBody User updateUser, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");

        // 查出现有用户
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) return Result.error("用户不存在");

        // 只允许更新昵称和头像
        if (updateUser.getNickname() != null && !updateUser.getNickname().trim().isEmpty()) {
            dbUser.setNickname(updateUser.getNickname());
        }
        if (updateUser.getAvatar() != null) {
            dbUser.setAvatar(updateUser.getAvatar());
        }

        userMapper.updateById(dbUser);
        return Result.success("个人信息修改成功");
    }

    @PutMapping("/updatePassword")
    public Result<?> updatePassword(@RequestBody java.util.Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }

        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) return Result.error("用户不存在");

        // 校验老密码
        if (!passwordEncoder.matches(oldPassword, dbUser.getPassword())) {
            return Result.error("原密码错误");
        }

        // 加密新密码并保存
        dbUser.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(dbUser);
        return Result.success("密码修改成功，请重新登录");
    }

    @GetMapping("/myProducts")
    public Result<?> getMyProducts(@RequestParam(required = false) Integer status, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.secondhandplatform.entity.Product> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId);

        // 如果前端传了状态（比如 1=在售，2=已售出），就按状态查；没传就查全部
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        java.util.List<com.example.secondhandplatform.entity.Product> list = productMapper.selectList(wrapper);
        return Result.success(list);
    }
}