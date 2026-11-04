package com.example.secondhandplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Message;
import com.example.secondhandplatform.mapper.MessageMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/message") // 注意：路径改成了 /api/message
public class MessageController {

    @Autowired
    private MessageMapper messageMapper;

    // 获取与某个好友的聊天历史记录
    @GetMapping("/history")
    public Result<?> getHistory(HttpServletRequest request, @RequestParam Long friendId) {
        Long myId = (Long) request.getAttribute("userId");
        if (myId == null) {
            return Result.error("获取失败：未登录");
        }

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        // 严谨逻辑：(我是发送者 AND 他是接收者) OR (他是发送者 AND 我是接收者)
        wrapper.and(w -> {
            w.eq("sender_id", myId).eq("receiver_id", friendId);
        }).or(w -> {
            w.eq("sender_id", friendId).eq("receiver_id", myId);
        }).orderByAsc("create_time");

        List<Message> list = messageMapper.selectList(wrapper);
        return Result.success(list);
    }
}