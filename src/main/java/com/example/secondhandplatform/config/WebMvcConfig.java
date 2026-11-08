package com.example.secondhandplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 和 FileController 使用同一个上传目录，可用 FILE_UPLOAD_DIR 覆盖
    @Value("${file.upload-dir:/www/wwwroot/secondhand-uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        // 访问 /images/** 的请求，映射到本地上传目录
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + location);
    }
}
