package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Notification;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.entity.Report;
import com.example.secondhandplatform.mapper.NotificationMapper;
import com.example.secondhandplatform.mapper.ProductMapper;
import com.example.secondhandplatform.mapper.ReportMapper;
import com.example.secondhandplatform.utils.ProductCacheUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 1. 获取所有待处理的举报
    @GetMapping("/report/pending")
    public Result<?> getPendingReports() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Report> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("status", 0).orderByDesc("create_time");
        return Result.success(reportMapper.selectList(wrapper));
    }

    // 1.1 已处理（历史）举报
    @GetMapping("/report/handled")
    public Result<?> getHandledReports() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Report> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("status", 1).orderByDesc("create_time");
        return Result.success(reportMapper.selectList(wrapper));
    }

    /**
     * 2. 处理举报
     *
     * @param action 1 = 判定违规：下架商品 + 通知卖家；0 = 驳回举报：商品保持原状态，不发通知
     */
    @PutMapping("/report/handle/{reportId}")
    public Result<?> handleReport(@PathVariable Long reportId,
                                  @RequestParam(defaultValue = "1") Integer action) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            return Result.error("举报不存在");
        }
        if (Integer.valueOf(1).equals(report.getStatus())) {
            return Result.error("该举报已经处理过了");
        }

        boolean takeDown = Integer.valueOf(1).equals(action);

        // 无论通过还是驳回，都把举报标记为已处理（status = 1）
        report.setStatus(1);
        reportMapper.updateById(report);

        // 只有“判定违规”才下架商品并通知卖家；驳回举报不打扰卖家
        if (takeDown && Integer.valueOf(1).equals(report.getTargetType())) {
            Product product = productMapper.selectById(report.getTargetId());
            if (product != null) {
                product.setStatus(3); // 3 代表违规下架
                productMapper.updateById(product);

                // 商品下架后不能再出现在在售列表里，立即清缓存
                ProductCacheUtils.clearListCache(redisTemplate);

                Notification notification = new Notification();
                notification.setUserId(product.getUserId());
                notification.setTitle("商品违规下架通知");
                notification.setContent("您的商品《" + product.getTitle() + "》因被举报违规，已被平台强制下架。");
                notification.setIsRead(0);
                notificationMapper.insert(notification);
            }
        }

        return Result.success(takeDown
                ? "举报已处理：商品已下架并通知卖家"
                : "举报已驳回：商品保持原状态，未发送通知");
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
        if (product == null) {
            return Result.error("商品不存在");
        }

        if (pass == 1) {
            product.setStatus(1); // 1 代表审核通过，正常在售
        } else {
            product.setStatus(3); // 3 代表审核拒绝，违规下架
        }
        productMapper.updateById(product);

        // 审核结果会改变在售列表，立即清缓存，避免等 5 分钟 TTL
        ProductCacheUtils.clearListCache(redisTemplate);

        // 给卖家发通知
        Notification notification = new Notification();
        notification.setUserId(product.getUserId());
        notification.setTitle(pass == 1 ? "商品审核通过" : "商品审核未通过");
        notification.setContent(pass == 1
                ? "恭喜！您的商品《" + product.getTitle() + "》已通过审核，现已在集市展示。"
                : "很抱歉，您的商品《" + product.getTitle() + "》未通过审核，请修改后重新发布。");
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        return Result.success(pass == 1 ? "商品已审核通过" : "商品已被驳回并下架");
    }
}
