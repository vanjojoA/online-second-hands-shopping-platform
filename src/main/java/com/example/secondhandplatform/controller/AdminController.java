package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ProductMapper productMapper;

    // 这是一个示例接口，由你搭建骨架，P2负责具体实现
    @GetMapping("/product/pending")
    public Result<?> getPendingProducts(@RequestParam(defaultValue = "1") Integer page) {
        // 暂时返回空的分页数据，等 P2 来实现真实逻辑
        return Result.success(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
    }
}