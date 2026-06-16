package com.sakura.taskmanager.controller;

import com.sakura.taskmanager.entity.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
public class FileUploadController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        //判断非空
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            //创建上传目录
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            //构建文件名
            String filename = buildStoredFilename(file.getOriginalFilename());
            Path targetFile = uploadDir.resolve(filename).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                return Result.error("文件名不合法");
            }

            file.transferTo(targetFile);
            return Result.success("/uploads/" + filename);
        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    private String buildStoredFilename(String originalFilename) {
        //清理非法字符
        String cleanFilename = StringUtils.cleanPath(
                originalFilename == null ? "file" : originalFilename
        );
        String extension = "";
        int dotIndex = cleanFilename.lastIndexOf(".");
        if (dotIndex >= 0 && dotIndex < cleanFilename.length() - 1) {
            extension = cleanFilename.substring(dotIndex);
        }
        return UUID.randomUUID() + extension;
    }
}
