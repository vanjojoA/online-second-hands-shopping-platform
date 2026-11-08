package com.example.secondhandplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.BrowseHistory;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.mapper.BrowseHistoryMapper;
import com.example.secondhandplatform.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 浏览记录（跨设备同步，存 MySQL）
 */
@RestController
@RequestMapping("/api/history")
public class BrowseHistoryController {

    @Autowired
    private BrowseHistoryMapper historyMapper;

    @Autowired
    private ProductMapper productMapper;

    // 记录一次浏览：同一用户同一商品只保留一条，更新时间
    @PostMapping("/record")
    public Result<?> record(HttpServletRequest request, @RequestParam Long productId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("获取失败：未登录");
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.error("商品不存在");
        }

        QueryWrapper<BrowseHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        BrowseHistory existing = historyMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setViewTime(LocalDateTime.now());
            historyMapper.updateById(existing);
        } else {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setProductId(productId);
            history.setViewTime(LocalDateTime.now());
            historyMapper.insert(history);
        }

        return Result.success("已记录浏览");
    }

    // 我的浏览记录（最近 50 条，带商品信息）
    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("获取失败：未登录");
        }

        QueryWrapper<BrowseHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("view_time").last("LIMIT 50");
        List<BrowseHistory> records = historyMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        if (!records.isEmpty()) {
            List<Long> productIds = records.stream()
                    .map(BrowseHistory::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            for (BrowseHistory record : records) {
                Product product = productMap.get(record.getProductId());
                if (product == null) {
                    continue; // 商品已被删除就不展示了
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", record.getId());
                item.put("productId", product.getId());
                item.put("title", product.getTitle());
                item.put("price", product.getPrice());
                item.put("imageUrls", product.getImageUrls());
                item.put("viewedAt", record.getViewTime());
                result.add(item);
            }
        }

        return Result.success(result);
    }

    // 删除一条浏览记录
    @DeleteMapping("/{id}")
    public Result<?> remove(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("获取失败：未登录");
        }

        BrowseHistory history = historyMapper.selectById(id);
        if (history == null || !history.getUserId().equals(userId)) {
            return Result.error("记录不存在");
        }

        historyMapper.deleteById(id);
        return Result.success("已删除该浏览记录");
    }

    // 清空我的浏览记录
    @DeleteMapping("/clear")
    public Result<?> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("获取失败：未登录");
        }

        QueryWrapper<BrowseHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        historyMapper.delete(wrapper);
        return Result.success("浏览记录已清空");
    }
}
