package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.mapper.ProductMapper;
import com.example.secondhandplatform.mapper.UserMapper;
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

    @Autowired private UserMapper userMapper;

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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        // 1. 拼接 Redis 缓存 Key (注意：参数越多，Key 越长，这里简单拼接演示)
        String redisKey = "product:list:" + page + ":" + size + ":" +
                (keyword == null ? "all" : keyword) + ":" +
                (categoryId == null ? "0" : categoryId) + ":" +
                (minPrice == null ? "0" : minPrice) + ":" +
                (maxPrice == null ? "0" : maxPrice);

        // 2. 先查 Redis
        String cachedJson = redisTemplate.opsForValue().get(redisKey);
        if (cachedJson != null) {
            System.out.println("====== 搜索缓存命中: " + redisKey + " ======");
            try {
                // 核心：把存进去的 JSON 字符串，重新反序列化回 Page 对象
                tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
                // 注意：Jackson 3 反序列化复杂泛型时，可能需要借助 TypeReference 或者明确的 Class
                com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> cachedPage =
                        objectMapper.readValue(cachedJson,
                                new tools.jackson.core.type.TypeReference<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product>>() {});

                // 返回真正的对象，这样前端拿到的 data 就是一个完整的 JSON 对象，而不是带引号的字符串
                return Result.success(cachedPage);
            } catch (Exception e) {
                // 如果反序列化失败（比如缓存格式坏了），不要报错，直接让它穿透去查数据库
                System.out.println("缓存解析失败，穿透查库: " + e.getMessage());
            }
        }

        // 3. 缓存未命中，构建查询条件
        System.out.println("====== 搜索缓存未命中，查询 MySQL ======");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

        // 固定条件：必须是“在售”状态 (status = 1)
        queryWrapper.eq("status", 1);

        // 动态条件：如果有输入，才拼接 SQL
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("title", keyword); // 模糊查询标题
        }
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId); // 注意：你需要先给 product 表加上 category_id 字段
        }
        if (minPrice != null) {
            queryWrapper.ge("price", minPrice); // 大于等于
        }
        if (maxPrice != null) {
            queryWrapper.le("price", maxPrice); // 小于等于
        }

        queryWrapper.orderByDesc("create_time");

        // 4. 执行查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result =
                productMapper.selectPage(pageParam, queryWrapper);

        // 5. 写入 Redis 缓存（适配 Spring Boot 4 的 Jackson 3）
        try {
            // 注意：这里换成了 tools.jackson.databind.ObjectMapper
            tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
            // 注意：Jackson 3 的 writeValueAsString 不再抛出受检异常，所以不需要 try-catch 来捕获它，但为了兼容 Redis 写入异常，外层依然保留 try-catch
            String jsonResult = objectMapper.writeValueAsString(result);

            redisTemplate.opsForValue().set(redisKey, jsonResult, 5, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    @GetMapping("/detail/{id}")
    public Result<?> getProductDetail(@PathVariable Long id) {
        // 1. 查商品
        Product product = productMapper.selectById(id);
        if (product == null) {
            return Result.error("商品不存在或已被删除");
        }

        // 2. 查卖家信息
        com.example.secondhandplatform.entity.User seller = userMapper.selectById(product.getUserId());

        // 3. 脱敏，隐藏卖家密码
        if (seller != null) {
            seller.setPassword(null);
        }

        // 4. 包装返回结果（包含商品和卖家信息）
        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
        resultMap.put("product", product);
        resultMap.put("seller", seller);

        return Result.success(resultMap);
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