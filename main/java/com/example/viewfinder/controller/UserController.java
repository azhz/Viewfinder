package com.example.viewfinder.controller;

import com.example.viewfinder.common.result.ApiResult;
import com.example.viewfinder.common.result.PageResult;
import com.example.viewfinder.dto.LoginDTO;
import com.example.viewfinder.dto.RegisterDTO;
import com.example.viewfinder.dto.UserUpdateDTO;
import com.example.viewfinder.dto.UserVO;
import com.example.viewfinder.dto.*;
import com.example.viewfinder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * 用户控制器
 * 处理用户相关的HTTP请求，包括注册、登录、用户信息管理、关注等功能
 *
 * @author Viewfinder Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    /** 用户服务层，处理业务逻辑 */
    private final UserService userService;

    /**
     * 用户注册接口
     *
     * @param dto 注册信息DTO，包含用户名、密码、邮箱等信息
     * @return 返回注册成功的用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResult<UserVO>> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO user = userService.register(dto);
        return ResponseEntity.ok(ApiResult.success(user));
    }

    /**
     * 用户登录接口
     *
     * @param dto 登录信息DTO，包含用户名和密码
     * @param request HTTP请求对象，用于获取客户端IP和User-Agent
     * @return 返回登录结果，包含token等信息
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResult<LoginResultDTO>> login(
            @Valid @RequestBody LoginDTO dto,
            HttpServletRequest request) {

        // 获取客户端真实IP地址
        String ip = getClientIp(request);
        // 获取客户端User-Agent信息
        String userAgent = request.getHeader("User-Agent");
        
        LoginResultDTO result = userService.login(dto, ip, userAgent);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取用户信息接口
     *
     * @param userId 用户ID
     * @param principal 当前登录用户信息
     * @return 返回用户详细信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<UserVO>> getUserInfo(@PathVariable Long userId, Principal principal) {
        Long currentUserId = null;
        if (principal != null) {
            try {
                currentUserId = Long.valueOf(principal.getName());
            } catch (NumberFormatException e) {
                // 如果principal.getName()不是有效的Long格式，currentUserId将保持null
            }
        }
        UserVO user = userService.getUserInfo(userId, currentUserId);
        return ResponseEntity.ok(ApiResult.success(user));
    }

    /**
     * 更新用户信息接口
     *
     * @param userId 用户ID
     * @param dto 用户更新信息DTO，包含需要更新的字段
     * @return 返回更新后的用户信息
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResult<UserVO>> updateUserInfo(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateDTO dto) {
        
        UserVO user = userService.updateUserInfo(userId, dto);
        return ResponseEntity.ok(ApiResult.success(user));
    }

    /**
     * 修改密码接口
     *
     * @param userId 用户ID
     * @param dto 修改密码DTO，包含旧密码和新密码
     * @return 返回操作结果
     */
    @PostMapping("/{userId}/password")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordDTO dto) {
        
        userService.changePassword(userId, dto);
        return ResponseEntity.ok(ApiResult.success());
    }

    /**
     * 发送验证码接口
     *
     * @param receiver 接收者（邮箱或手机号）
     * @param type 验证码类型（1-注册，2-登录，3-找回密码等）
     * @return 返回操作结果
     */
    @PostMapping("/verification-code")
    public ResponseEntity<ApiResult<Void>> sendVerificationCode(
            @RequestParam String receiver,
            @RequestParam Integer type) {
        
        userService.sendVerificationCode(receiver, type);
        return ResponseEntity.ok(ApiResult.success());
    }

    /**
     * 关注用户接口
     *
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID（被关注者）
     * @return 返回操作结果
     */
    @PostMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<ApiResult<Void>> followUser(
            @PathVariable Long userId,
            @PathVariable Long targetUserId) {
        
        userService.followUser(userId, targetUserId);
        return ResponseEntity.ok(ApiResult.success());
    }

    /**
     * 取消关注用户接口
     *
     * @param userId 当前用户ID
     * @param targetUserId 目标用户ID（被取消关注者）
     * @return 返回操作结果
     */
    @DeleteMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<ApiResult<Void>> unfollowUser(
            @PathVariable Long userId,
            @PathVariable Long targetUserId,
            Principal principal) {
        
        if (principal == null) {
            return ResponseEntity.ok(ApiResult.error(401, "未认证的用户"));
        }
        
        try {
            Long currentUserId = Long.valueOf(principal.getName());
            // 验证路径参数userId与当前认证用户ID是否一致
            if (!currentUserId.equals(userId)) {
                return ResponseEntity.ok(ApiResult.error(403, "无权限执行此操作"));
            }
            
            userService.unfollowUser(currentUserId, targetUserId);
            return ResponseEntity.ok(ApiResult.success());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResult.error(401, "无效的用户凭证"));
        }
    }

    /**
     * 获取客户端真实IP地址
     * 优先从代理头中获取，如果没有则使用远程地址
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 尝试从X-Forwarded-For头获取IP（适用于经过代理的请求）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 尝试从Proxy-Client-IP头获取
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 尝试从WL-Proxy-Client-IP头获取（WebLogic代理）
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 如果都获取不到，使用远程地址
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    @PostMapping("/{userId}/avatar")
    public ResponseEntity<ApiResult<String>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(ApiResult.success(avatarUrl));
    }

    /**
     * 获取关注列表接口
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 返回关注列表
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResult<PageResult<UserVO>>> getFollowingList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        PageResult<Object> result = userService.getFollowingList(userId, page, size);
        // 将Object类型的PageResult转换为UserVO类型的PageResult
        PageResult<UserVO> convertedResult = new PageResult<>(
            result.getRecords().stream()
                .map(obj -> (UserVO) obj)
                .collect(java.util.stream.Collectors.toList()),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
        return ResponseEntity.ok(ApiResult.success(convertedResult));
    }

    /**
     * 获取粉丝列表接口
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 返回粉丝列表
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResult<PageResult<UserVO>>> getFollowerList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        PageResult<Object> result = userService.getFollowerList(userId, page, size);
        // 将Object类型的PageResult转换为UserVO类型的PageResult
        PageResult<UserVO> convertedResult = new PageResult<>(
            result.getRecords().stream()
                .map(obj -> (UserVO) obj)
                .collect(java.util.stream.Collectors.toList()),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
        return ResponseEntity.ok(ApiResult.success(convertedResult));
    }
    
    /**
     * 获取当前登录用户信息接口
     *
     * @param principal 当前登录用户信息
     * @return 返回当前登录用户详细信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResult<UserVO>> getCurrentUserInfo(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResult.error(401, "未认证的用户"));
        }
        
        try {
            Long userId = Long.valueOf(principal.getName());
            UserVO user = userService.getUserInfo(userId);
            return ResponseEntity.ok(ApiResult.success(user));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResult.error(401, "无效的用户凭证"));
        }
    }
}