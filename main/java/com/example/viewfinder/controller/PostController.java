package com.example.viewfinder.controller;

import com.example.viewfinder.common.result.ApiResult;
import com.example.viewfinder.common.result.PageResult;
import com.example.viewfinder.dto.PostDTO;
import com.example.viewfinder.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 创建帖子
     */
    @PostMapping
    public ResponseEntity<ApiResult<PostDTO>> createPost(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        
        Long userId = Long.valueOf(principal.getName()); // 从JWT中获取用户ID
        
        PostDTO postDTO = new PostDTO();
        postDTO.setTitle(title);
        postDTO.setDescription(description);
        postDTO.setLocation(location);

        PostDTO createdPost = postService.createPost(userId, postDTO, file);
        return ResponseEntity.ok(ApiResult.success(createdPost));
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResult<PostDTO>> getPost(@PathVariable Long postId, Principal principal) {
        Long userId = null;
        if (principal != null) {
            try {
                userId = Long.valueOf(principal.getName());
            } catch (NumberFormatException e) {
                // 如果principal.getName()不是有效的Long格式，userId将保持null
            }
        }
        
        PostDTO post = postService.getPostById(postId, userId);
        return ResponseEntity.ok(ApiResult.success(post));
    }

    /**
     * 获取用户帖子列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResult<PageResult<PostDTO>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        var postsPage = postService.getUserPosts(userId, page, size);
        PageResult<PostDTO> pageResult = new PageResult<>(
            postsPage.getRecords(),
            postsPage.getTotal(),
            postsPage.getCurrent(),
            postsPage.getSize()
        );
        return ResponseEntity.ok(ApiResult.success(pageResult));
    }

    /**
     * 获取所有帖子（供首页展示）
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResult<PostDTO>>> getAllPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        var postsPage = postService.getAllPosts(page, size);
        PageResult<PostDTO> pageResult = new PageResult<>(
            postsPage.getRecords(),
            postsPage.getTotal(),
            postsPage.getCurrent(),
            postsPage.getSize()
        );
        return ResponseEntity.ok(ApiResult.success(pageResult));
    }

    /**
     * 更新帖子
     */
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResult<PostDTO>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostDTO postDTO,
            Principal principal) {
        
        Long userId = Long.valueOf(principal.getName()); // 从JWT中获取用户ID
        PostDTO updatedPost = postService.updatePost(postId, userId, postDTO);
        return ResponseEntity.ok(ApiResult.success(updatedPost));
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResult<Void>> deletePost(
            @PathVariable Long postId,
            Principal principal) {
        
        Long userId = Long.valueOf(principal.getName()); // 从JWT中获取用户ID
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResult.success());
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResult<Void>> toggleLike(
            @PathVariable Long postId,
            Principal principal) {
        
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 未认证用户无法点赞
        }
        
        Long userId = Long.valueOf(principal.getName()); // 从JWT中获取用户ID
        postService.toggleLike(postId, userId);
        return ResponseEntity.ok(ApiResult.success());
    }
}