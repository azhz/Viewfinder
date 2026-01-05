package com.example.viewfinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.viewfinder.common.exception.BusinessException;
import com.example.viewfinder.dto.PostCommentDTO;
import com.example.viewfinder.dto.UserVO;
import com.example.viewfinder.entity.PostComment;
import com.example.viewfinder.entity.User;
import com.example.viewfinder.mapper.PostCommentMapper;
import com.example.viewfinder.mapper.UserMapper;
import com.example.viewfinder.service.PostCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements PostCommentService {

    private final PostCommentMapper postCommentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostCommentDTO createComment(Long userId, Long postId, PostCommentDTO commentDTO) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 创建评论实体
        PostComment comment = new PostComment();
        BeanUtils.copyProperties(commentDTO, comment);
        comment.setUserId(userId);
        comment.setPostId(postId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        // 保存评论
        postCommentMapper.insert(comment);

        // 转换为DTO返回
        PostCommentDTO result = new PostCommentDTO();
        BeanUtils.copyProperties(comment, result);
        
        // 设置用户信息
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatarUrl(user.getAvatarUrl());
        result.setUser(userVO);

        return result;
    }

    @Override
    public Page<PostCommentDTO> getCommentsByPostId(Long postId, Integer page, Integer size) {
        LambdaQueryWrapper<PostComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostComment::getPostId, postId)
                .eq(PostComment::getParentId, 0) // 只获取一级评论，不包括回复
                .orderByDesc(PostComment::getCreatedAt);

        Page<PostComment> commentPage = new Page<>(page, size);
        postCommentMapper.selectPage(commentPage, wrapper);

        // 转换为DTO
        Page<PostCommentDTO> result = new Page<>();
        result.setCurrent(commentPage.getCurrent());
        result.setSize(commentPage.getSize());
        result.setTotal(commentPage.getTotal());

        List<PostCommentDTO> commentDTOList = commentPage.getRecords().stream()
                .map(comment -> {
                    PostCommentDTO commentDTO = new PostCommentDTO();
                    BeanUtils.copyProperties(comment, commentDTO);
                    
                    // 设置用户信息
                    User user = userMapper.selectById(comment.getUserId());
                    if (user != null) {
                        UserVO userVO = new UserVO();
                        userVO.setId(user.getId());
                        userVO.setUsername(user.getUsername());
                        userVO.setNickname(user.getNickname());
                        userVO.setAvatarUrl(user.getAvatarUrl());
                        commentDTO.setUser(userVO);
                    }
                    
                    return commentDTO;
                })
                .collect(Collectors.toList());

        result.setRecords(commentDTOList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostCommentDTO replyComment(Long userId, Long commentId, PostCommentDTO replyDTO) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查原评论是否存在
        PostComment originalComment = postCommentMapper.selectById(commentId);
        if (originalComment == null) {
            throw new BusinessException("原评论不存在");
        }

        // 创建回复评论实体
        PostComment reply = new PostComment();
        BeanUtils.copyProperties(replyDTO, reply);
        reply.setUserId(userId);
        reply.setPostId(originalComment.getPostId()); // 回复的帖子ID与原评论相同
        reply.setParentId(commentId); // 设置父评论ID
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        // 保存回复
        postCommentMapper.insert(reply);

        // 转换为DTO返回
        PostCommentDTO result = new PostCommentDTO();
        BeanUtils.copyProperties(reply, result);
        
        // 设置用户信息
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatarUrl(user.getAvatarUrl());
        result.setUser(userVO);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 检查是否有删除权限（只能删除自己的评论）
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("没有权限删除此评论");
        }

        // 删除评论及其所有回复
        LambdaQueryWrapper<PostComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostComment::getId, commentId)
                .or()
                .eq(PostComment::getParentId, commentId);
        
        postCommentMapper.delete(wrapper);
    }
}