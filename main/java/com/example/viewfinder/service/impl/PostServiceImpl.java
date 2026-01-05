package com.example.viewfinder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.viewfinder.common.exception.BusinessException;
import com.example.viewfinder.dto.PostDTO;
import com.example.viewfinder.entity.Post;
import com.example.viewfinder.entity.User;
import com.example.viewfinder.mapper.PostMapper;
import com.example.viewfinder.service.PostService;
import com.example.viewfinder.entity.PostComment;
import com.example.viewfinder.entity.PostLike;
import com.example.viewfinder.mapper.PostCommentMapper;
import com.example.viewfinder.mapper.PostLikeMapper;
import com.example.viewfinder.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCommentMapper postCommentMapper;
    private final UserMapper userMapper;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDTO createPost(Long userId, PostDTO postDTO, MultipartFile file) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证文件
        if (file != null && !file.isEmpty()) {
            // 验证文件类型
            if (!isValidImageType(file)) {
                throw new BusinessException("只允许上传 JPG、PNG、GIF 格式的图片");
            }

            // 保存文件
            String imageUrl = saveImageFile(file);
            postDTO.setImageUrl(imageUrl);
        }

        // 创建帖子实体
        Post post = new Post();
        BeanUtils.copyProperties(postDTO, post);
        post.setUserId(userId);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus(1); // 1表示正常状态

        // 保存到数据库
        postMapper.insert(post);

        // 更新用户帖子数量
        user.setPostCount(user.getPostCount() + 1);
        userMapper.updateById(user);

        // 转换为DTO返回
        PostDTO result = new PostDTO();
        BeanUtils.copyProperties(post, result);
        return result;
    }

    @Override
    public PostDTO getPostById(Long postId) {
        return getPostById(postId, null);
    }
    
    public PostDTO getPostById(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        PostDTO postDTO = new PostDTO();
        BeanUtils.copyProperties(post, postDTO);
        
        // 设置点赞状态
        if (userId != null) {
            postDTO.setIsLiked(isPostLiked(postId, userId));
        } else {
            postDTO.setIsLiked(false); // 未登录用户默认未点赞
        }
        
        // 设置评论数
        LambdaQueryWrapper<PostComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(PostComment::getPostId, postId);
        Integer commentCount = postCommentMapper.selectCount(commentWrapper).intValue();
        postDTO.setCommentCount(commentCount);
        
        return postDTO;
    }

    @Override
    public Page<PostDTO> getUserPosts(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1) // 只查询正常状态的帖子
                .orderByDesc(Post::getCreatedAt);

        Page<Post> postPage = new Page<>(page, size);
        postMapper.selectPage(postPage, wrapper);

        // 转换为DTO
        Page<PostDTO> result = new Page<>();
        result.setCurrent(postPage.getCurrent());
        result.setSize(postPage.getSize());
        result.setTotal(postPage.getTotal());

        List<PostDTO> postDTOList = postPage.getRecords().stream()
                .map(post -> {
                    PostDTO postDTO = new PostDTO();
                    BeanUtils.copyProperties(post, postDTO);
                    
                    // 设置评论数
                    LambdaQueryWrapper<PostComment> commentWrapper = new LambdaQueryWrapper<>();
                    commentWrapper.eq(PostComment::getPostId, post.getId());
                    Integer commentCount = postCommentMapper.selectCount(commentWrapper).intValue();
                    postDTO.setCommentCount(commentCount);
                    
                    return postDTO;
                })
                .collect(Collectors.toList());

        result.setRecords(postDTOList);
        return result;
    }

    @Override
    public Page<PostDTO> getAllPosts(Integer page, Integer size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1) // 只查询正常状态的帖子
                .orderByDesc(Post::getCreatedAt);

        Page<Post> postPage = new Page<>(page, size);
        postMapper.selectPage(postPage, wrapper);

        // 转换为DTO
        Page<PostDTO> result = new Page<>();
        result.setCurrent(postPage.getCurrent());
        result.setSize(postPage.getSize());
        result.setTotal(postPage.getTotal());

        List<PostDTO> postDTOList = postPage.getRecords().stream()
                .map(post -> {
                    PostDTO postDTO = new PostDTO();
                    BeanUtils.copyProperties(post, postDTO);
                    
                    // 设置评论数
                    LambdaQueryWrapper<PostComment> commentWrapper = new LambdaQueryWrapper<>();
                    commentWrapper.eq(PostComment::getPostId, post.getId());
                    Integer commentCount = postCommentMapper.selectCount(commentWrapper).intValue();
                    postDTO.setCommentCount(commentCount);
                    
                    return postDTO;
                })
                .collect(Collectors.toList());

        result.setRecords(postDTOList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDTO updatePost(Long postId, Long userId, PostDTO postDTO) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException("没有权限修改此帖子");
        }

        // 更新帖子信息
        BeanUtils.copyProperties(postDTO, post, "id", "userId", "createdAt", "updatedAt");
        post.setUpdatedAt(LocalDateTime.now());

        postMapper.updateById(post);

        PostDTO result = new PostDTO();
        BeanUtils.copyProperties(post, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        if (!post.getUserId().equals(userId)) {
            throw new BusinessException("没有权限删除此帖子");
        }

        // 逻辑删除，将状态设为0
        post.setStatus(0);
        postMapper.updateById(post);
        
        // 更新用户帖子数量
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPostCount(Math.max(0, user.getPostCount() - 1));
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleLike(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        
        // 检查是否已经点赞
        boolean isLiked = postLikeMapper.existsByUserIdAndPostId(userId, postId);
        
        if (isLiked) {
            // 如果已点赞，则取消点赞
            int deleted = postLikeMapper.deleteByUserIdAndPostId(userId, postId);
            if (deleted > 0) {
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                postMapper.updateById(post);
            }
        } else {
            // 如果未点赞，则添加点赞
            PostLike postLike = new PostLike();
            postLike.setUserId(userId);
            postLike.setPostId(postId);
            postLike.setCreatedAt(LocalDateTime.now());
            
            postLikeMapper.insert(postLike);
            
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
        }
    }

    @Override
    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeMapper.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * 保存图片文件
     */
    private String saveImageFile(MultipartFile file) {
        try {
            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    log.error("创建上传目录失败: {}", uploadPath);
                    throw new BusinessException("文件上传目录创建失败");
                }
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = System.currentTimeMillis() + "_" + System.nanoTime() + extension;

            String filePath = uploadPath + File.separator + newFilename;
            file.transferTo(new File(filePath));
            log.info("照片文件保存成功，路径: {}", filePath);

            return "/uploads/" + newFilename;
        } catch (IOException e) {
            log.error("照片文件上传失败", e);
            throw new BusinessException("照片文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 验证图片类型
     */
    private boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif")
        );
    }
}