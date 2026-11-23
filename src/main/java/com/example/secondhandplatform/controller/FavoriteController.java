package com.example.secondhandplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.entity.Favorite;
import com.example.secondhandplatform.entity.Product;
import com.example.secondhandplatform.mapper.FavoriteMapper;
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
 * 商品收藏
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ProductMapper productMapper;

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // 收藏 / 取消收藏（同一个接口切换）
    @PostMapping("/toggle")
    public Result<?> toggle(HttpServletRequest request, @RequestParam Long productId) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (productMapper.selectById(productId) == null) {
            return Result.error("商品不存在");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        Favorite existing = favoriteMapper.selectOne(wrapper);

        boolean favorited;
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            favorited = false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            favorite.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            favorited = true;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favorited", favorited);
        return Result.success(data);
    }

    // 我收藏的商品 ID 集合（列表页渲染红心用）
    @GetMapping("/ids")
    public Result<?> ids(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<Long> ids = favoriteMapper.selectList(wrapper).stream()
                .map(Favorite::getProductId)
                .collect(Collectors.toList());
        return Result.success(ids);
    }

    // 我的收藏列表（带商品信息）
    @GetMapping("/list")
    public Result<?> list(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        if (!favorites.isEmpty()) {
            List<Long> productIds = favorites.stream()
                    .map(Favorite::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            for (Favorite favorite : favorites) {
                Product product = productMap.get(favorite.getProductId());
                if (product == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", favorite.getId());
                item.put("productId", product.getId());
                item.put("title", product.getTitle());
                item.put("price", product.getPrice());
                item.put("imageUrls", product.getImageUrls());
                item.put("status", product.getStatus());
                item.put("favoritedAt", favorite.getCreateTime());
                result.add(item);
            }
        }

        return Result.success(result);
    }

    // 取消收藏
    @DeleteMapping("/{productId}")
    public Result<?> remove(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = currentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        favoriteMapper.delete(wrapper);
        return Result.success("已取消收藏");
    }
}
