package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Notification;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.entity.Report;
import com.example.secondhandplatform.mapper.NotificationMapper;
import com.example.secondhandplatform.mapper.ProductMapper;
import com.example.secondhandplatform.mapper.ReportMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    // 1. 获取所有待处理的举报
    @GetMapping("/report/pending")
    public Result<?> getPendingReports() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Report> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("status", 0).orderByDesc("create_time");
        return Result.success(reportMapper.selectList(wrapper));
    }

    // 2. 处理举报（并自动下架被举报商品，发送通知）
    @PutMapping("/report/handle/{reportId}")
    public Result<?> handleReport(@PathVariable Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) return Result.error("举报不存在");

        // 标记举报为已处理
        report.setStatus(1);
        reportMapper.updateById(report);

        // 如果是举报商品（targetType == 1），下架商品并通知卖家
        if (report.getTargetType() == 1) {
            Product product = productMapper.selectById(report.getTargetId());
            if (product != null) {
                product.setStatus(3); // 3 代表违规下架
                productMapper.updateById(product);

                // 给卖家发通知
                Notification notification = new Notification();
                notification.setUserId(product.getUserId());
                notification.setTitle("商品违规下架通知");
                notification.setContent("您的商品《" + product.getTitle() + "》因被举报违规，已被平台强制下架。");
                notification.setIsRead(0);
                notificationMapper.insert(notification);
            }
        }

        return Result.success("举报已处理，相关商品已下架并通知用户");
    }

    // 3. 获取所有待审核的商品 (status = 0)
    @GetMapping("/product/pending")
    public Result<?> getPendingProducts() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("status", 0).orderByDesc("create_time");
        return Result.success(productMapper.selectList(wrapper));
    }

    // 4. 审核商品 (通过或拒绝)
    @PutMapping("/product/audit/{productId}")
    public Result<?> auditProduct(@PathVariable Long productId, @RequestParam Integer pass) {
        Product product = productMapper.selectById(productId);
        if (product == null) return Result.error("商品不存在");

        if (pass == 1) {
            product.setStatus(1); // 1 代表审核通过，正常在售
        } else {
            product.setStatus(3); // 3 代表审核拒绝，违规下架
        }
        productMapper.updateById(product);

        // 给卖家发通知
        Notification notification = new Notification();
        notification.setUserId(product.getUserId());
        notification.setTitle(pass == 1 ? "商品审核通过" : "商品审核未通过");
        notification.setContent(pass == 1 ?
                "恭喜！您的商品《" + product.getTitle() + "》已通过审核，现已在集市展示。" :
                "很抱歉，您的商品《" + product.getTitle() + "》未通过审核，请修改后重新发布。");
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        return Result.success(pass == 1 ? "商品已审核通过" : "商品已被驳回并下架");
    }
}