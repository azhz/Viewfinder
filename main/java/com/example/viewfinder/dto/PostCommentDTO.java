package com.example.viewfinder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostCommentDTO {
    private Long id;
    
    private Long postId;
    
    private Long userId;
    
    private Long parentId; // 父评论ID
    
    private String content; // 评论内容
    
    private UserVO user; // 评论用户信息
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}