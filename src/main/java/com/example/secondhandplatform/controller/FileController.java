package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // 图片保存目录：默认 /www/wwwroot/secondhand-uploads/，可用环境变量 FILE_UPLOAD_DIR 覆盖
    // Docker 部署时记得把这个目录挂载到宿主机，否则容器重建后图片会丢
    @Value("${file.upload-dir:/www/wwwroot/secondhand-uploads/}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传失败：文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') >= 0) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }

        // 只允许常见图片格式，避免上传任意文件
        if (!suffix.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
            return Result.error("上传失败：只支持 jpg / jpeg / png / gif / webp / bmp 图片");
        }

        try {
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.error("上传失败：无法创建上传目录，请检查目录权限");
            }

            String newFileName = UUID.randomUUID().toString() + suffix;
            File destFile = new File(dir, newFileName);
            file.transferTo(destFile);

            // 返回给前端可以访问的相对路径，前端拼接 baseUrl 就能访问
            return Result.success("/images/" + newFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：服务器内部错误");
        }
    }
}
