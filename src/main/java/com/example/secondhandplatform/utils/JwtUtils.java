package com.example.secondhandplatform.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JwtUtils {

    // 这是一个“密钥”，用来给 Token 签名。你可以随便改一长串英文字母，绝不能泄露给前端！
    private static final String SECRET_KEY = "MySecondhandPlatformSecretKey2026ForSYSUSELab";

    // Token 的有效期（比如 7 天，单位是毫秒）
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    // 生成 Token 的方法
    public static String generateToken(Long userId, String studentId) {
        return Jwts.builder()
                .claim("userId", userId)          // 把用户ID放进 Token 里
                .claim("studentId", studentId)    // 把学号放进 Token 里
                .setIssuedAt(new Date())          // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes()) // 用密钥签名
                .compact();
    }

    // 解析 Token，如果解析成功返回用户ID，失败或过期抛出异常
    public static Long parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", Long.class); // 取出我们在生成时塞进去的 userId
    }
}