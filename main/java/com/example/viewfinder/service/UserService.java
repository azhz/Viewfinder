package com.example.viewfinder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.viewfinder.dto.*;
import com.example.viewfinder.common.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    
    /**
     * 用户注册
     */
    UserVO register(RegisterDTO dto);
    
    /**
     * 用户登录
     */
    LoginResultDTO login(LoginDTO dto, String ip, String userAgent);
    
    /**
     * 获取用户信息
     */
    UserVO getUserInfo(Long userId);
    
    /**
     * 获取用户信息（带当前用户ID用于关注状态）
     */
    UserVO getUserInfo(Long userId, Long currentUserId);
    
    /**
     * 更新用户信息
     */
    UserVO updateUserInfo(Long userId, UserUpdateDTO dto);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordDTO dto);
    
    /**
     * 发送验证码
     */
    void sendVerificationCode(String receiver, Integer type);
    
    /**
     * 验证验证码
     */
    boolean verifyCode(String receiver, String code, Integer type);
    
    /**
     * 获取用户列表（管理员）
     */
    Page<UserVO> getUserList(PageQueryDTO query);
    
    /**
     * 禁用/启用用户（管理员）
     */
    void toggleUserStatus(Long userId, Integer status);
    
    /**
     * 关注用户
     */
    void followUser(Long userId, Long targetUserId);
    
    /**
     * 取消关注
     */
    void unfollowUser(Long userId, Long targetUserId);
    
    /**
     * 获取关注列表
     */
    PageResult<Object> getFollowingList(Long userId, Integer page, Integer size);
    
    /**
     * 获取粉丝列表
     */
    PageResult<Object> getFollowerList(Long userId, Integer page, Integer size);

    String uploadAvatar(Long userId, MultipartFile file);



}