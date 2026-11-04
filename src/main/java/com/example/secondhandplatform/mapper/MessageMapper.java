package com.example.secondhandplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secondhandplatform.entity.Message;
import com.example.secondhandplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}