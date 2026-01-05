import axios from 'axios'
import api from './index';
import type { User, LoginRequest, RegisterRequest, UpdateProfileRequest } from '@/types/user';

// 用户相关API
export const userApi = {
  // 用户注册
  register: (data: RegisterRequest) => {
    return api.post('/users/register', data);
  },

  // 用户登录
  login: (data: LoginRequest) => {
    return api.post('/users/login', data);
  },

  // 获取用户信息
  getUserInfo: (userId: number) => {
    return api.get<User>(`/users/${userId}`);
  },

  // 更新用户信息
  updateUserInfo: (userId: number, data: UpdateProfileRequest) => {
    return api.put<User>(`/users/${userId}`, data);
  },

  // 修改密码
  changePassword: (userId: number, data: { oldPassword: string; newPassword: string; confirmPassword: string }) => {
    return api.post(`/users/${userId}/password`, data);
  },

  // 发送验证码
  sendVerificationCode: (receiver: string, type: number) => {
    return api.post('/users/verification-code', null, {
      params: {
        receiver,
        type
      }
    });
  },

  // 关注用户
  followUser: (userId: number, targetUserId: number) => {
    return api.post(`/users/${userId}/follow/${targetUserId}`);
  },

  // 取消关注
  unfollowUser: (userId: number, targetUserId: number) => {
    return api.delete(`/users/${userId}/follow/${targetUserId}`);
  },

  // 上传头像
  uploadAvatar: (userId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/users/${userId}/avatar`, formData);
  },
  
  // 获取关注列表
  getFollowingList: (userId: number, page: number = 1, size: number = 10) => {
    return api.get(`/users/${userId}/following`, {
      params: { page, size }
    });
  },
  
  // 获取粉丝列表
  getFollowerList: (userId: number, page: number = 1, size: number = 10) => {
    return api.get(`/users/${userId}/followers`, {
      params: { page, size }
    });
  },
  
  // 获取当前用户信息
  getCurrentUserInfo: () => {
    return api.get<User>('/users/profile');
  }
};