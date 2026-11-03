package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Spring Boot 4 用的是 Jackson 3，自动配置的 Bean 是 tools.jackson.databind.json.JsonMapper
    // （Jackson 2 的 com.fasterxml.jackson.databind.ObjectMapper 在 Boot 4 里不在编译类路径上）
    @Autowired
    private JsonMapper jsonMapper;

    /**
     * 商品数据发生变化（发布 / 改状态）后，清掉所有分页的商品列表缓存。
     * 注意用 SCAN 而不是 KEYS：KEYS 会一次性阻塞 Redis，SCAN 是游标式增量遍历。
     */
    private void clearProductListCache() {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("product:list:*").count(200).build())) {
            Set<String> keys = new HashSet<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            System.out.println("====== 商品数据变更，已清除 " + keys.size() + " 个列表缓存 ======");
        } catch (Exception e) {
            // 清缓存失败不能影响发布 / 改状态本身，记一条日志即可
            System.out.println("====== 清除列表缓存失败（不影响业务）：" + e.getMessage() + " ======");
        }
    }

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
        // 新增了商品，旧的列表缓存就过期了，清掉，保证下次请求能查到新商品
        clearProductListCache();
        return Result.success("发布成功！商品已上架。");
    }

    @GetMapping("/list")
    public Result<?> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        // 1. 拼接 Redis 的 key，比如 "product:list:1:10"
        String redisKey = "product:list:" + page + ":" + size;

        // 2. 先尝试从 Redis 里获取缓存；Redis 不可用时直接跳过缓存，不影响列表接口
        try {
            String cachedJson = redisTemplate.opsForValue().get(redisKey);
            if (cachedJson != null) {
                // 3. 缓存命中！把 JSON 反序列化回对象再包装，保证和未命中时返回的 JSON 结构一致
                System.out.println("====== 缓存命中，直接从 Redis 返回数据 ======");
                return Result.success(jsonMapper.readValue(cachedJson, Map.class));
            }
        } catch (Exception e) {
            System.out.println("====== Redis 读取失败，降级查询数据库：" + e.getMessage() + " ======");
        }

        // 4. 缓存未命中，去数据库查
        System.out.println("====== 缓存未命中，开始查询 MySQL ======");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("status", 1).orderByDesc("create_time");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result =
                productMapper.selectPage(pageParam, queryWrapper);

        // 5. 将查询结果转成 JSON 字符串，存入 Redis（写缓存失败也不影响本次返回）
        try {
            String jsonResult = jsonMapper.writeValueAsString(result);
            // 设置 5 分钟过期时间，防止缓存永远占着内存
            redisTemplate.opsForValue().set(redisKey, jsonResult, Duration.ofMinutes(5));
        } catch (Exception e) {
            System.out.println("====== Redis 写入失败，本次结果未缓存：" + e.getMessage() + " ======");
        }

        // 6. 返回查询结果
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
        // 商品状态变了（已售出，不再出现在在售列表里），同样要清掉列表缓存
        clearProductListCache();

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