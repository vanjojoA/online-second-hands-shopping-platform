package com.example.secondhandplatform.websocket;

import com.example.secondhandplatform.entity.Message;
import com.example.secondhandplatform.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private JsonMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.addSession(userId, session);
            System.out.println("====== 用户上线，userId: " + userId + "，当前在线人数: " + sessionManager.getOnlineCount() + " ======");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // 前端格式："接收者ID:消息内容"
        String[] parts = payload.split(":", 2);

        if (parts.length == 2) {
            Long receiverId;
            try {
                receiverId = Long.parseLong(parts[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("====== 非法消息格式，已忽略：" + payload + " ======");
                return;
            }
            String content = parts[1];
            Long senderId = getUserIdFromSession(session);

            // 1. 无论对方在不在线，先把消息存进数据库
            Message msg = new Message();
            msg.setSenderId(senderId);
            msg.setReceiverId(receiverId);
            msg.setContent(content);
            msg.setIsRead(0); // 初始未读
            messageMapper.insert(msg);

            // 2. 去路由表找接收者
            WebSocketSession receiverSession = sessionManager.getSession(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                // 在线：推送 JSON，带上发送者 ID，前端才能区分是谁发来的
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("type", "message");
                body.put("messageId", msg.getId());
                body.put("senderId", senderId);
                body.put("receiverId", receiverId);
                body.put("content", content);
                body.put("createTime", (msg.getCreateTime() != null ? msg.getCreateTime() : LocalDateTime.now()).toString());

                receiverSession.sendMessage(new TextMessage(jsonMapper.writeValueAsString(body)));
                System.out.println("====== 消息已实时推送给用户: " + receiverId + "（发送者: " + senderId + "）======");
            } else {
                // 不在线，只存库，等对方上线通过 HTTP 拉取
                System.out.println("====== 用户 " + receiverId + " 不在线，消息已入库 ======");
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.removeSession(userId);
            System.out.println("====== 用户下线，userId: " + userId + " ======");
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.startsWith("userId=")) {
                return Long.parseLong(query.substring(7));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
