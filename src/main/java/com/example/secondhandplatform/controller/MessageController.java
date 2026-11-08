package com.example.secondhandplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Message;
import com.example.secondhandplatform.entity.User;
import com.example.secondhandplatform.mapper.MessageMapper;
import com.example.secondhandplatform.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

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

    /**
     * 会话列表：把和我聊过天的人聚合出来，每个会话带最后一条消息和未读数。
     * 用于前端「消息」页和顶栏未读红点。
     */
    @GetMapping("/conversations")
    public Result<?> getConversations(HttpServletRequest request) {
        Long myId = (Long) request.getAttribute("userId");
        if (myId == null) {
            return Result.error("获取失败：未登录");
        }

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("sender_id", myId).or().eq("receiver_id", myId))
                .orderByDesc("create_time");
        List<Message> all = messageMapper.selectList(wrapper);

        // 按对方 userId 聚合，顺序沿用 create_time 倒序（最新聊过的在最前面）
        Map<Long, Map<String, Object>> conversations = new LinkedHashMap<>();
        for (Message msg : all) {
            Long friendId = myId.equals(msg.getSenderId()) ? msg.getReceiverId() : msg.getSenderId();
            if (friendId == null) {
                continue;
            }

            Map<String, Object> vo = conversations.get(friendId);
            if (vo == null) {
                vo = new LinkedHashMap<>();
                vo.put("friendId", friendId);
                vo.put("lastContent", msg.getContent());
                vo.put("lastTime", msg.getCreateTime());
                vo.put("unreadCount", 0);

                User friend = userMapper.selectById(friendId);
                if (friend != null) {
                    vo.put("friendNickname", friend.getNickname());
                    vo.put("friendStudentId", friend.getStudentId());
                    vo.put("friendAvatar", friend.getAvatar());
                }
                conversations.put(friendId, vo);
            }

            // 对方发给我、且未读的消息，累加未读数
            if (!myId.equals(msg.getSenderId()) && Integer.valueOf(0).equals(msg.getIsRead())) {
                vo.put("unreadCount", (Integer) vo.get("unreadCount") + 1);
            }
        }

        return Result.success(new ArrayList<>(conversations.values()));
    }

    // 我的总未读数（顶栏红点用）
    @GetMapping("/unread-count")
    public Result<?> getUnreadCount(HttpServletRequest request) {
        Long myId = (Long) request.getAttribute("userId");
        if (myId == null) {
            return Result.error("获取失败：未登录");
        }

        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", myId).eq("is_read", 0);
        Long count = messageMapper.selectCount(wrapper);
        return Result.success(count);
    }

    // 进入某个会话后，把对方发给我的未读消息标记为已读
    @PutMapping("/read")
    public Result<?> markAsRead(HttpServletRequest request, @RequestParam Long friendId) {
        Long myId = (Long) request.getAttribute("userId");
        if (myId == null) {
            return Result.error("获取失败：未登录");
        }

        UpdateWrapper<Message> wrapper = new UpdateWrapper<>();
        wrapper.eq("sender_id", friendId)
                .eq("receiver_id", myId)
                .eq("is_read", 0)
                .set("is_read", 1);
        messageMapper.update(null, wrapper);

        return Result.success("已标记为已读");
    }
}
