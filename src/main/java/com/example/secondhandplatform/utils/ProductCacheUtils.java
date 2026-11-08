package com.example.secondhandplatform.utils;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * 商品列表缓存工具。
 * 商品状态变化（发布、售出、审核、违规下架）后都需要清一遍 product:list:* 缓存。
 */
public final class ProductCacheUtils {

    private ProductCacheUtils() {
    }

    /**
     * 用 SCAN 清掉所有分页的商品列表缓存，返回清掉的 key 数量。
     * 清缓存失败不影响主流程。
     */
    public static int clearListCache(StringRedisTemplate redisTemplate) {
        int count = 0;
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("product:list:*").count(200).build())) {
            Set<String> keys = new HashSet<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                count = keys.size();
            }
        } catch (Exception e) {
            System.out.println("====== 清除商品列表缓存失败（不影响业务）：" + e.getMessage() + " ======");
        }
        return count;
    }
}
