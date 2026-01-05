package com.example.viewfinder.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.viewfinder.common.result.ApiResult;
import com.example.viewfinder.dto.PostCommentDTO;
import com.example.viewfinder.service.PostCommentService;
import com.example.viewfinder.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
public class PostCommentController {

    @Autowired
    private PostCommentService postCommentService;
    
    private final JwtUtil jwtUtil;

    /**
     * 创建评论
     */
    @PostMapping
    public ResponseEntity<ApiResult<PostCommentDTO>> createComment(@RequestBody PostCommentDTO commentDTO,
                                                     @RequestParam Long postId,
                                                     HttpServletRequest request) {
        try {
            // 从请求头获取JWT令牌
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResult.error("未授权"));
            }
            token = token.substring(7); // 移除 "Bearer " 前缀

            // 解析JWT令牌获取用户ID
            Claims claims = jwtUtil.parseJWT(token);
            Long userId = Long.valueOf(claims.get("userId").toString());

            // 创建评论
            PostCommentDTO result = postCommentService.createComment(userId, postId, commentDTO);
            return ResponseEntity.ok(ApiResult.success(result));
        } catch (Exception e) {
            log.error("创建评论失败", e);
            return ResponseEntity.ok(ApiResult.error("创建评论失败: " + e.getMessage()));
        }
    }

    /**
     * 获取帖子评论列表
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResult<Page<PostCommentDTO>>> getCommentsByPostId(@PathVariable Long postId,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer size,
                                                                 HttpServletRequest request) {
        try {
            // 从请求头获取JWT令牌
            String token = request.getHeader("Authorization");
            Long userId = null;
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // 移除 "Bearer " 前缀
                try {
                    Claims claims = jwtUtil.parseJWT(token);
                    userId = Long.valueOf(claims.get("userId").toString());
                } catch (Exception e) {
                    // 如果令牌无效，userId保持为null
                }
            }

            // 获取评论列表
            Page<PostCommentDTO> result = postCommentService.getCommentsByPostId(postId, page, size);
            return ResponseEntity.ok(ApiResult.success(result));
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return ResponseEntity.ok(ApiResult.error("获取评论列表失败: " + e.getMessage()));
        }
    }

    /**
     * 回复评论
     */
    @PostMapping("/reply/{commentId}")
    public ResponseEntity<ApiResult<PostCommentDTO>> replyComment(@PathVariable Long commentId,
                                                    @RequestBody PostCommentDTO replyDTO,
                                                    HttpServletRequest request) {
        try {
            // 从请求头获取JWT令牌
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResult.error("未授权"));
            }
            token = token.substring(7); // 移除 "Bearer " 前缀

            // 解析JWT令牌获取用户ID
            Claims claims = jwtUtil.parseJWT(token);
            Long userId = Long.valueOf(claims.get("userId").toString());

            // 回复评论
            PostCommentDTO result = postCommentService.replyComment(userId, commentId, replyDTO);
            return ResponseEntity.ok(ApiResult.success(result));
        } catch (Exception e) {
            log.error("回复评论失败", e);
            return ResponseEntity.ok(ApiResult.error("回复评论失败: " + e.getMessage()));
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResult<String>> deleteComment(@PathVariable Long commentId,
                                             HttpServletRequest request) {
        try {
            // 从请求头获取JWT令牌
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResult.error("未授权"));
            }
            token = token.substring(7); // 移除 "Bearer " 前缀

            // 解析JWT令牌获取用户ID
            Claims claims = jwtUtil.parseJWT(token);
            Long userId = Long.valueOf(claims.get("userId").toString());

            // 删除评论
            postCommentService.deleteComment(commentId, userId);
            return ResponseEntity.ok(ApiResult.success("删除成功"));
        } catch (Exception e) {
            log.error("删除评论失败", e);
            return ResponseEntity.ok(ApiResult.error("删除评论失败: " + e.getMessage()));
        }
    }
}