package com.example.viewfinder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.viewfinder.entity.UserFollowing;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserFollowingMapper extends BaseMapper<UserFollowing> {
    /**
     * 检查是否已关注
     */
    boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
    
    /**
     * 删除关注关系
     */
    int deleteByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}