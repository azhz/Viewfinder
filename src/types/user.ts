// 用户相关类型定义

export interface User {
  id?: number;
  username: string;
  email?: string;
  phone?: string;
  avatarUrl?: string;
  nickname?: string;
  bio?: string;
  gender?: number;
  birthday?: string;
  location?: string;
  website?: string;
  followCount?: number;
  fanCount?: number;
  postCount?: number;
  status?: number;
  lastLoginTime?: string;
  createdAt?: string;
  isFollowed?: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
  email?: string;
  phone?: string;
  code: string;
}

export interface UpdateProfileRequest {
  nickname?: string;
  bio?: string;
  gender?: number;
  birthday?: string;
  location?: string;
  website?: string;
  avatarUrl?: string;
}

export interface LoginResponse {
  token: string;
  userInfo: User;
}

export type LoginForm = LoginRequest;
export type RegisterForm = RegisterRequest;