package com.example.secondhandplatform.config;

import com.example.secondhandplatform.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 如果是浏览器的预检请求 (OPTIONS)，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从请求头 (Header) 中获取 Token
        String token = request.getHeader("Authorization");

        // 3. 如果没带 Token，直接打回
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401); // 401 代表未授权
            response.setContentType("text/plain;charset=UTF-8"); // 加上这行，解决乱码
            response.getWriter().write("未登录，请先登录！");
            return false;
        }

        // 4. 去掉 "Bearer " 前缀，拿到真正的 Token
        token = token.substring(7);

        // 【核心新增】：检查该 Token 是否在黑名单中
        Boolean isBlacklisted = redisTemplate.hasKey("token:blacklist:" + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            response.setStatus(401);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("该账号已退出登录，请重新登录！");
            return false;
        }

        // 5. 校验 Token 是否合法
        try {
            Long userId = JwtUtils.parseToken(token);
            request.setAttribute("userId", userId);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Token 无效或已过期，请重新登录！");
            return false;
        }
    }
}