package com.example.secondhandplatform.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {
    // 核心：用 ConcurrentHashMap 保证并发安全。
    // key 是 userId，value 是用户的 WebSocket 连接
    private static final ConcurrentHashMap<Long, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    // 用户上线，存入路由表
    public void addSession(Long userId, WebSocketSession session) {
        SESSION_MAP.put(userId, session);
    }

    // 用户下线，移除路由表
    public void removeSession(Long userId) {
        SESSION_MAP.remove(userId);
    }

    // 根据 userId 获取对方的网线
    public WebSocketSession getSession(Long userId) {
        return SESSION_MAP.get(userId);
    }

    // 获取当前在线人数
    public int getOnlineCount() {
        return SESSION_MAP.size();
    }
}