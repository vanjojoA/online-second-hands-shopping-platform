package com.example.secondhandplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;    // 发送者 ID
    private Long receiverId;  // 接收者 ID
    private Long productId;   // 关联的商品 ID (可选)
    private String content;   // 消息内容
    private Integer isRead;   // 0-未读，1-已读
    private LocalDateTime createTime;
}