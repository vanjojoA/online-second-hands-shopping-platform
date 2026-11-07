package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Notification;
import com.example.secondhandplatform.mapper.NotificationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationMapper notificationMapper;

    // 1. 获取我的所有通知（供前端"消息中心"页面调用）
    @GetMapping("/list")
    public Result<?> getMyNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notification> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("create_time"); // 最新的通知在最前面

        List<Notification> list = notificationMapper.selectList(wrapper);
        return Result.success(list);
    }

    // 2. 把某条通知标记为已读
    @PutMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Notification notification = notificationMapper.selectById(id);

        if (notification == null) return Result.error("通知不存在");
        // 防越权：只能改自己的通知
        if (!notification.getUserId().equals(userId)) return Result.error("无权操作");

        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        return Result.success("已标记为已读");
    }
}