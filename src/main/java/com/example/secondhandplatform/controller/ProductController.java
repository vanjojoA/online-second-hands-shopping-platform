package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @PostMapping("/publish")
    public Result<?> publishProduct(@RequestBody Product product, HttpServletRequest request) {
        // 核心：从拦截器存入的 request 中取出 userId
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            return Result.error("发布失败：无法获取用户信息！");
        }

        // 绑定发布者ID，并设置初始状态为在售(1)
        product.setUserId(userId);
        product.setStatus(1);

        productMapper.insert(product);
        return Result.success("发布成功！商品已上架。");
    }

    @GetMapping("/list")
    public Result<?> getProductList(
            @RequestParam(defaultValue = "1") Integer page,   // 当前第几页，默认第1页
            @RequestParam(defaultValue = "10") Integer size   // 每页显示几条，默认10条
    ) {
        // 1. 创建分页对象
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);

        // 2. 执行查询，按照创建时间倒序排列（最新的商品在最前面）
        // 这里的 QueryWrapper 限制了只查询状态为 1（在售）的商品
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("status", 1).orderByDesc("create_time");

        // 3. 调用 Mapper 的分页查询方法
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result =
                productMapper.selectPage(pageParam, queryWrapper);

        // 4. 返回分页数据（包含总条数、当前页数据列表等）
        return Result.success(result);
    }

    @PutMapping("/updateStatus/{id}")
    public Result<?> updateProductStatus(@PathVariable("id") Long id, HttpServletRequest request) {
        // 1. 从拦截器中获取当前登录的 userId
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("操作失败：用户未登录");
        }

        // 2. 根据商品 ID 去数据库里查这件商品
        Product dbProduct = productMapper.selectById(id);

        // 3. 判断商品是否存在
        if (dbProduct == null) {
            return Result.error("操作失败：商品不存在");
        }

        // 4. 核心防越权逻辑：判断当前登录的用户，是不是这件商品的发布者
        if (!dbProduct.getUserId().equals(userId)) {
            return Result.error("操作失败：你没有权限修改别人的商品状态！");
        }

        // 5. 校验通过，修改状态为 2（已售出）
        dbProduct.setStatus(2);
        productMapper.updateById(dbProduct);

        return Result.success("商品已成功标记为售出！");
    }

    @GetMapping("/my")
    public Result<?> getMyProducts(HttpServletRequest request) {
        // 1. 从拦截器里拿到当前用户的 userId
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("获取失败：未登录");
        }

        // 2. 去数据库里查，条件是 user_id 等于当前登录人，按时间倒序
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("user_id", userId).orderByDesc("create_time");

        // 3. 返回列表（个人中心的商品不需要分页，因为一个人发不了几千条）
        java.util.List<Product> list = productMapper.selectList(queryWrapper);

        return Result.success(list);
    }
}