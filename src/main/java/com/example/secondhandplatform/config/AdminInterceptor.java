package com.example.secondhandplatform.config;

import com.example.secondhandplatform.entity.User;
import com.example.secondhandplatform.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 前置拦截器（JwtInterceptor）已经解析了 userId 并存入 request
        Long userId = (Long) request.getAttribute("userId");

        // 如果没登录，直接被前置拦截器干掉了，这里为了安全再挡一次
        if (userId == null) {
            response.setStatus(401);
            return false;
        }

        // 查数据库，看这个用户的 role 是不是 1（管理员）
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() != 1) {
            // 拒绝访问：403 Forbidden
            response.setStatus(403);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("拒绝访问：你不是管理员！");
            return false;
        }

        return true; // 放行
    }
}