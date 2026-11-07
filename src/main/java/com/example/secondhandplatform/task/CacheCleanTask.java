package com.example.secondhandplatform.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CacheCleanTask {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 核心：Cron 表达式。这里代表每天凌晨 3 点执行一次
    @Scheduled(cron = "0 0 3 * * ?")
    public void clearExpiredCache() {
        System.out.println("====== 定时任务触发：开始清理过期商品缓存 ======");

        // 这里我们可以做一些具体的清理动作，比如把某些临时 key 删掉
        // 比如你想清理所有以 "temp:" 开头的 key
        Set<String> keys = redisTemplate.keys("temp:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            System.out.println("====== 已清理 " + keys.size() + " 个临时缓存 ======");
        }
    }
}