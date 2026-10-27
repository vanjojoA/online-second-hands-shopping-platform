package com.example.secondhandplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;       // 发布者的ID
    private String title;      // 商品标题
    private Double price;      // 价格
    private String description;// 描述
    private String imageUrls;  // 图片URL(多个用逗号隔开)
    private Integer status;    // 0-待审核, 1-在售, 2-已售出
    private LocalDateTime createTime;
}