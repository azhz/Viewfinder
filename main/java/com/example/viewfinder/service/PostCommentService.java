package com.example.viewfinder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.viewfinder.dto.PostCommentDTO;

public interface PostCommentService {
    
    /**
     * 创建评论
     */
    PostCommentDTO createComment(Long userId, Long postId, PostCommentDTO commentDTO);
    
    /**
     * 获取帖子评论列表
     */
    Page<PostCommentDTO> getCommentsByPostId(Long postId, Integer page, Integer size);
    
    /**
     * 回复评论
     */
    PostCommentDTO replyComment(Long userId, Long commentId, PostCommentDTO replyDTO);
    
    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);
}