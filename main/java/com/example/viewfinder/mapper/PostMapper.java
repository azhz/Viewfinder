package com.example.viewfinder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.viewfinder.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}