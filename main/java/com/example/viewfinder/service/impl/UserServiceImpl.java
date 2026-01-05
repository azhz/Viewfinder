package com.example.viewfinder.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.viewfinder.common.exception.BusinessException;
import com.example.viewfinder.common.result.PageResult;
import com.example.viewfinder.dto.LoginDTO;
import com.example.viewfinder.dto.RegisterDTO;
import com.example.viewfinder.dto.UserUpdateDTO;
import com.example.viewfinder.dto.UserVO;
import com.example.viewfinder.dto.*;
import com.example.viewfinder.entity.User;
import com.example.viewfinder.entity.UserFollowing;
import com.example.viewfinder.entity.UserLoginLog;
import com.example.viewfinder.mapper.UserFollowingMapper;
import com.example.viewfinder.mapper.UserLoginLogMapper;
import com.example.viewfinder.mapper.UserMapper;
import com.example.viewfinder.service.UserService;
import com.example.viewfinder.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final UserMapper userMapper;
    private final UserLoginLogMapper loginLogMapper;
    private final UserFollowingMapper userFollowingMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String REDIS_VERIFY_CODE_PREFIX = "verify:code:";
    private static final int VERIFY_CODE_EXPIRE_MINUTES = 5;
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        // 验证数据
        validateRegisterData(dto);
        
        // 验证验证码
        if (!verifyCode(dto.getEmail() != null ? dto.getEmail() : dto.getPhone(), 
                       dto.getCode(), 1)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        
        // 使用BCrypt加密密码（自动生成盐值）
        String passwordHash = passwordEncoder.encode(dto.getPassword());
        user.setPasswordHash(passwordHash);
        
        // 设置默认信息
        user.setNickname(dto.getUsername());
        user.setAvatarUrl("/default-avatar.png");
        user.setStatus(1);
        user.setFollowCount(0);
        user.setFanCount(0);
        user.setPostCount(0);
        
        // 保存用户
        if (!save(user)) {
            throw new RuntimeException("用户注册失败");
        }
        
        // 标记验证码已使用
        String receiver = dto.getEmail() != null ? dto.getEmail() : dto.getPhone();
        redisTemplate.delete(REDIS_VERIFY_CODE_PREFIX + receiver + ":1");
        
        log.info("用户注册成功: username={}, id={}", user.getUsername(), user.getId());
        return convertToVO(user);
    }
    
    @Override
    public LoginResultDTO login(LoginDTO dto, String ip, String userAgent) {
        // 查找用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码（使用PasswordEncoder）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        updateById(user);
        
        // 记录登录日志
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user.getId());
        loginLog.setLoginType(1);
        loginLog.setLoginIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setStatus(1);
        loginLogMapper.insert(loginLog);
        
        // 生成JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        // 返回结果
        LoginResultDTO result = new LoginResultDTO();
        result.setToken(token);
        result.setUserInfo(convertToVO(user));
        
        log.info("用户登录成功: username={}, ip={}", user.getUsername(), ip);
        return result;
    }
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToVO(user);
    }
    
    @Override
    public UserVO getUserInfo(Long userId, Long currentUserId) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVO userVO = convertToVO(user);
        if (currentUserId != null && !currentUserId.equals(userId)) {
            // 检查当前用户是否关注了该用户
            boolean isFollowed = userFollowingMapper.existsByFollowerIdAndFollowingId(currentUserId, userId);
            userVO.setIsFollowed(isFollowed);
        }
        return userVO;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUserInfo(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (StrUtil.isNotBlank(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            user.setBirthday(dto.getBirthday());
        }
        if (dto.getLocation() != null) {
            user.setLocation(dto.getLocation());
        }
        if (dto.getWebsite() != null) {
            user.setWebsite(dto.getWebsite());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        
        if (!updateById(user)) {
            throw new RuntimeException("更新用户信息失败");
        }
        
        return convertToVO(user);
    }
    
    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 更新密码（使用BCrypt加密）
        String newPasswordHash = passwordEncoder.encode(dto.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        
        if (!updateById(user)) {
            throw new RuntimeException("修改密码失败");
        }
        
        log.info("用户修改密码成功: userId={}", userId);
    }
    
    @Override
    public void sendVerificationCode(String receiver, Integer type) {
        // 生成6位验证码
        String code = RandomUtil.randomNumbers(6);
        
        // 存储到Redis，5分钟过期
        String key = REDIS_VERIFY_CODE_PREFIX + receiver + ":" + type;
        redisTemplate.opsForValue().set(key, code, VERIFY_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // 这里应该调用邮件或短信服务发送验证码
        // 为简化实现，这里只打印日志
        log.info("发送验证码: receiver={}, code={}, type={}", receiver, code, type);
        
        // 实际项目中应调用：
        // if (type == 1 || type == 3) { // 注册或重置密码
        //     emailService.sendVerificationCode(receiver, code);
        // } else if (type == 2) { // 登录
        //     smsService.sendVerificationCode(receiver, code);
        // }
    }
    
    @Override
    public boolean verifyCode(String receiver, String code, Integer type) {
        String key = REDIS_VERIFY_CODE_PREFIX + receiver + ":" + type;
        String storedCode = (String) redisTemplate.opsForValue().get(key);

        // 调试日志
        log.info("验证验证码 - receiver: {}, type: {}, 输入的code: {}, Redis中的code: {}, key: {}",
                receiver, type, code, storedCode, key);

        return Objects.equals(code, storedCode);
    }

    @Override
    public Page<UserVO> getUserList(PageQueryDTO query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 搜索条件
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.like(User::getUsername, query.getKeyword())
                    .or()
                    .like(User::getNickname, query.getKeyword())
                    .or()
                    .like(User::getEmail, query.getKeyword());
        }

        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }

        // 排序 - 使用Lambda表达式
        if (StrUtil.isNotBlank(query.getSortField())) {
            String sortField = query.getSortField();
            boolean isAsc = "ascend".equals(query.getSortOrder());

            switch (sortField) {
                case "createdAt":
                    if (isAsc) {
                        wrapper.orderByAsc(User::getCreatedAt);
                    } else {
                        wrapper.orderByDesc(User::getCreatedAt);
                    }
                    break;
                case "lastLoginTime":
                    if (isAsc) {
                        wrapper.orderByAsc(User::getLastLoginTime);
                    } else {
                        wrapper.orderByDesc(User::getLastLoginTime);
                    }
                    break;
                case "followCount":
                    if (isAsc) {
                        wrapper.orderByAsc(User::getFollowCount);
                    } else {
                        wrapper.orderByDesc(User::getFollowCount);
                    }
                    break;
                case "fanCount":
                    if (isAsc) {
                        wrapper.orderByAsc(User::getFanCount);
                    } else {
                        wrapper.orderByDesc(User::getFanCount);
                    }
                    break;
                default:
                    wrapper.orderByDesc(User::getCreatedAt);
                    break;
            }
        } else {
            wrapper.orderByDesc(User::getCreatedAt);
        }

        // 分页查询
        Page<User> page = new Page<>(query.getPage(), query.getSize());
        page(page, wrapper);

        // 转换为VO
        Page<UserVO> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());

        List<UserVO> userVOS = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        result.setRecords(userVOS);

        return result;
    }
    
    @Override
    public void toggleUserStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(status);
        if (!updateById(user)) {
            throw new RuntimeException("更新用户状态失败");
        }
        
        log.info("更新用户状态: userId={}, status={}", userId, status);
    }
    
    // 私有方法
    private void validateRegisterData(RegisterDTO dto) {
        if (!Objects.equals(dto.getPassword(), dto.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        if (userMapper.countByUsername(dto.getUsername()) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (StrUtil.isNotBlank(dto.getEmail()) && userMapper.countByEmail(dto.getEmail()) > 0) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        if (StrUtil.isNotBlank(dto.getPhone()) && userMapper.countByPhone(dto.getPhone()) > 0) {
            throw new RuntimeException("手机号已被注册");
        }
    }
    

    private UserVO convertToVO(User user) {
        if (user == null) return null;
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setNickname(user.getNickname());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setLocation(user.getLocation());
        vo.setWebsite(user.getWebsite());
        vo.setFollowCount(user.getFollowCount());
        vo.setFanCount(user.getFanCount());
        vo.setPostCount(user.getPostCount());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setIsFollowed(false); // 需要根据当前登录用户判断
        
        return vo;
    }
    
    private String getSortField(String field) {
        switch (field) {
            case "createdAt": return "created_at";
            case "lastLoginTime": return "last_login_time";
            case "followCount": return "follow_count";
            default: return "created_at";
        }
    }
    

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long userId, Long targetUserId) {
        // 检查是否已经关注
        if (userFollowingMapper.existsByFollowerIdAndFollowingId(userId, targetUserId)) {
            throw new BusinessException("已经关注该用户");
        }
        
        // 检查目标用户是否存在
        User targetUser = getById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }
        
        // 创建关注关系
        UserFollowing following = new UserFollowing();
        following.setFollowerId(userId);
        following.setFollowingId(targetUserId);
        following.setCreatedAt(LocalDateTime.now());
        
        userFollowingMapper.insert(following);
        
        // 更新关注者关注数
        User follower = getById(userId);
        follower.setFollowCount(follower.getFollowCount() + 1);
        updateById(follower);
        
        // 更新被关注者粉丝数
        targetUser.setFanCount(targetUser.getFanCount() + 1);
        updateById(targetUser);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long userId, Long targetUserId) {
        // 检查是否已经关注
        if (!userFollowingMapper.existsByFollowerIdAndFollowingId(userId, targetUserId)) {
            throw new BusinessException("未关注该用户");
        }
        
        // 删除关注关系
        int deleted = userFollowingMapper.deleteByFollowerIdAndFollowingId(userId, targetUserId);
        if (deleted == 0) {
            throw new BusinessException("取消关注失败");
        }
        
        // 更新关注者关注数
        User follower = getById(userId);
        follower.setFollowCount(Math.max(0, follower.getFollowCount() - 1));
        updateById(follower);
        
        // 更新被关注者粉丝数
        User targetUser = getById(targetUserId);
        targetUser.setFanCount(Math.max(0, targetUser.getFanCount() - 1));
        updateById(targetUser);
    }
    
    @Override
    public PageResult<Object> getFollowingList(Long userId, Integer page, Integer size) {
        // 获取用户关注列表
        LambdaQueryWrapper<UserFollowing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowing::getFollowerId, userId)
                .orderByDesc(UserFollowing::getCreatedAt);
        
        Page<UserFollowing> followingPage = new Page<>(page, size);
        userFollowingMapper.selectPage(followingPage, wrapper);
        
        // 获取关注的用户ID列表
        List<Long> followingIds = followingPage.getRecords().stream()
                .map(UserFollowing::getFollowingId)
                .collect(Collectors.toList());
        
        if (followingIds.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L, (long) page, (long) size);
        }
        
        // 查询关注的用户信息
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, followingIds);
        
        List<User> users = userMapper.selectList(userWrapper);
        
        // 转换为VO
        List<UserVO> userVOList = users.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 转换为PageResult<Object>
        List<Object> objectList = userVOList.stream()
                .map(userVO -> (Object) userVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(objectList, followingPage.getTotal(), followingPage.getCurrent(), followingPage.getSize());
    }
    
    @Override
    public PageResult<Object> getFollowerList(Long userId, Integer page, Integer size) {
        // 获取用户粉丝列表
        LambdaQueryWrapper<UserFollowing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollowing::getFollowingId, userId)
                .orderByDesc(UserFollowing::getCreatedAt);
        
        Page<UserFollowing> followerPage = new Page<>(page, size);
        userFollowingMapper.selectPage(followerPage, wrapper);
        
        // 获取粉丝的用户ID列表
        List<Long> followerIds = followerPage.getRecords().stream()
                .map(UserFollowing::getFollowerId)
                .collect(Collectors.toList());
        
        if (followerIds.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L, (long) page, (long) size);
        }
        
        // 查询粉丝用户信息
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, followerIds);
        
        List<User> users = userMapper.selectList(userWrapper);
        
        // 转换为VO
        List<UserVO> userVOList = users.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 转换为PageResult<Object>
        List<Object> objectList = userVOList.stream()
                .map(userVO -> (Object) userVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(objectList, followerPage.getTotal(), followerPage.getCurrent(), followerPage.getSize());
    }
    
    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 检查用户是否存在
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 验证文件类型
        if (!isValidImageType(file)) {
            throw new BusinessException("只允许上传 JPG、PNG、GIF 格式的图片");
        }

        log.info("开始上传头像，用户ID: {}, 文件名: {}, 文件大小: {}", userId, file.getOriginalFilename(), file.getSize());

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = System.currentTimeMillis() + extension;

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
            
            String filePath = uploadPath + File.separator + newFilename;
            file.transferTo(new File(filePath));
            log.info("文件保存成功，路径: {}", filePath);

            String avatarUrl = "/uploads/" + newFilename;
            user.setAvatarUrl(avatarUrl);
            updateById(user);
            log.info("用户头像URL更新成功: {}", avatarUrl);

            return avatarUrl;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    private boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif")
        );
    }
}