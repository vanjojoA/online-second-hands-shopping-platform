package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Report;
import com.example.secondhandplatform.mapper.ReportMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportMapper reportMapper;

    @PostMapping("/submit")
    public Result<?> submitReport(@RequestBody Report report, HttpServletRequest request) {
        // 1. 获取当前登录人（举报人）
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("请先登录后再举报");
        }

        // 2. 基础校验
        if (report.getTargetType() == null || report.getTargetId() == null || report.getReason() == null) {
            return Result.error("举报信息不完整");
        }

        // 3. 防止恶意刷举报：简单判断原因长度
        if (report.getReason().length() < 5 || report.getReason().length() > 200) {
            return Result.error("举报原因需在 5-200 字之间");
        }

        // 4. 绑定举报人，设置初始状态
        report.setReporterId(userId);
        report.setStatus(0); // 0 代表待处理

        reportMapper.insert(report);
        return Result.success("举报已提交，管理员会尽快核实处理！");
    }
}