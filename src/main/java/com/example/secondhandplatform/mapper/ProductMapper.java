package com.example.secondhandplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secondhandplatform.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT COUNT(*) FROM product WHERE DATE(create_time) = CURDATE()")
    long countTodayProducts();

    @Select("SELECT COUNT(*) FROM product WHERE status = 2 AND DATE(sold_time) = CURDATE()")
    long countTodaySold();

    @Select("SELECT COALESCE(SUM(price), 0) FROM product WHERE status = 2 AND DATE(sold_time) = CURDATE()")
    double sumTodayTradeAmount();

    @Select("SELECT COALESCE(SUM(price), 0) FROM product WHERE status = 2")
    double sumAllTradeAmount();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS d, COUNT(*) AS c " +
            "FROM product WHERE create_time >= #{start} GROUP BY d")
    List<Map<String, Object>> countProductsByDay(@Param("start") String start);

    @Select("SELECT DATE_FORMAT(sold_time, '%Y-%m-%d') AS d, COALESCE(SUM(price), 0) AS amount " +
            "FROM product WHERE status = 2 AND sold_time >= #{start} GROUP BY d")
    List<Map<String, Object>> sumTradeByDay(@Param("start") String start);

    @Select("SELECT status, COUNT(*) AS c FROM product GROUP BY status")
    List<Map<String, Object>> countByStatus();
}
