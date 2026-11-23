package com.example.secondhandplatform.controller;

import com.example.secondhandplatform.common.Result;
import com.example.secondhandplatform.mapper.ProductMapper;
import com.example.secondhandplatform.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员数据统计（今日新增、成交额、近 7 天趋势、商品状态分布）
 * 仅 /api/admin/** 下的接口，AdminInterceptor 会校验 role=1
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/overview")
    public Result<?> overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 今日数据
        Map<String, Object> today = new LinkedHashMap<>();
        today.put("newUsers", userMapper.countTodayUsers());
        today.put("newProducts", productMapper.countTodayProducts());
        today.put("soldCount", productMapper.countTodaySold());
        today.put("tradeAmount", round(productMapper.sumTodayTradeAmount()));
        data.put("today", today);

        // 累计数据
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("users", userMapper.selectCount(null));
        totals.put("products", productMapper.selectCount(null));
        totals.put("tradeAmount", round(productMapper.sumAllTradeAmount()));
        data.put("totals", totals);

        // 近 7 天趋势
        LocalDate start = LocalDate.now().minusDays(6);
        String startStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";

        Map<String, Long> usersByDay = toLongMap(userMapper.countUsersByDay(startStr), "c");
        Map<String, Long> productsByDay = toLongMap(productMapper.countProductsByDay(startStr), "c");
        Map<String, Double> amountByDay = toDoubleMap(productMapper.sumTradeByDay(startStr), "amount");

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String date = start.plusDays(i).toString();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date);
            row.put("newUsers", usersByDay.getOrDefault(date, 0L));
            row.put("newProducts", productsByDay.getOrDefault(date, 0L));
            row.put("tradeAmount", round(amountByDay.getOrDefault(date, 0.0)));
            trend.add(row);
        }
        data.put("trend", trend);

        // 商品状态分布（饼图）
        Map<Integer, String> statusNames = new HashMap<>();
        statusNames.put(0, "待审核");
        statusNames.put(1, "在售");
        statusNames.put(2, "已售出");
        statusNames.put(3, "违规下架");

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map<String, Object> row : productMapper.countByStatus()) {
            int status = ((Number) row.get("status")).intValue();
            long count = ((Number) row.get("c")).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", statusNames.getOrDefault(status, "未知"));
            item.put("value", count);
            distribution.add(item);
        }
        data.put("statusDistribution", distribution);

        return Result.success(data);
    }

    private Map<String, Long> toLongMap(List<Map<String, Object>> rows, String valueKey) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("d");
            Object value = row.get(valueKey);
            if (date != null && value != null) {
                map.put(String.valueOf(date), ((Number) value).longValue());
            }
        }
        return map;
    }

    private Map<String, Double> toDoubleMap(List<Map<String, Object>> rows, String valueKey) {
        Map<String, Double> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("d");
            Object value = row.get(valueKey);
            if (date != null && value != null) {
                map.put(String.valueOf(date), ((Number) value).doubleValue());
            }
        }
        return map;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
