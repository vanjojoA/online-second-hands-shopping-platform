package com.example.secondhandplatform;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisTest {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testRedis() {
        // 往 Redis 存一个键值对
        redisTemplate.opsForValue().set("test_key", "Hello Redis");
        // 从 Redis 取出来打印
        System.out.println("从 Redis 获取的值是：" + redisTemplate.opsForValue().get("test_key"));
    }
}