package com.example.secondhandplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secondhandplatform.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}