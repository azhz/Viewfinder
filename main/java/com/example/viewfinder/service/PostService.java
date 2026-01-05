package com.example.viewfinder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.viewfinder.dto.PostDTO;
import com.example.viewfinder.entity.Post;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    
    /**
     * 创建帖子
     */
    PostDTO createPost(Long userId, PostDTO postDTO, MultipartFile file);
    
    /**
     * 获取帖子详情
     */
    PostDTO getPostById(Long postId);
    
    /**
     * 获取帖子详情（带当前用户ID用于点赞状态）
     */
    PostDTO getPostById(Long postId, Long userId);
    
    /**
     * 分页获取用户帖子
     */
    Page<PostDTO> getUserPosts(Long userId, Integer page, Integer size);
    
    /**
     * 分页获取所有帖子（供首页展示）
     */
    Page<PostDTO> getAllPosts(Integer page, Integer size);
    
    /**
     * 更新帖子
     */
    PostDTO updatePost(Long postId, Long userId, PostDTO postDTO);
    
    /**
     * 删除帖子
     */
    void deletePost(Long postId, Long userId);
    
    /**
     * 点赞/取消点赞
     */
    void toggleLike(Long postId, Long userId);
    
    /**
     * 获取帖子点赞状态
     */
    boolean isPostLiked(Long postId, Long userId);
}