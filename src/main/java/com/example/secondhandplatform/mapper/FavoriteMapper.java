package com.example.secondhandplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secondhandplatform.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
