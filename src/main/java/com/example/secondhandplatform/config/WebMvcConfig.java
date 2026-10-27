package com.example.secondhandplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 访问 /images/** 的请求，映射到本地 D:/secondhand-uploads/ 目录
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:D:/secondhand-uploads/");
    }
}