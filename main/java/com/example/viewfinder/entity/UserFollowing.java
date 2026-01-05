package com.example.viewfinder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_following")
public class UserFollowing {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("follower_id")
    private Long followerId; // 关注者ID

    @TableField("following_id")
    private Long followingId; // 被关注者ID

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("created_at")
    private LocalDateTime createdAt;
}