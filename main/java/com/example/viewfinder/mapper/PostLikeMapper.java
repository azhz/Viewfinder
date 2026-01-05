package com.example.viewfinder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.viewfinder.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
    /**
     * 检查是否已点赞
     */
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
    
    /**
     * 删除点赞关系
     */
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
}