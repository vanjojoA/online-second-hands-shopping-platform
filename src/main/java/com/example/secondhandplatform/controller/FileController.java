package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // 定义图片保存的本地目录（你可以改成你电脑上真实存在的路径，比如 D:/img/）
    private static final String UPLOAD_DIR = "D:/secondhand-uploads/";

    @PostMapping("/upload")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传失败：文件为空");
        }

        try {
            // 1. 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            // 2. 提取文件后缀 (比如 .jpg .png)
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 3. 生成一个唯一的文件名 (防止重名覆盖)
            String newFileName = UUID.randomUUID().toString() + suffix;

            // 4. 确保目录存在，不存在则创建
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 5. 保存文件
            File destFile = new File(UPLOAD_DIR + newFileName);
            file.transferTo(destFile);

            // 6. 返回给前端可以访问的相对路径或 URL
            // 注意：这里返回 /images/文件名，前端拼接上 baseUrl 就能直接访问了
            String imageUrl = "/images/" + newFileName;
            return Result.success(imageUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败：服务器内部错误");
        }
    }
}