// 通用类型定义

export interface User {
  id: number;
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
  followCount: number;
  fanCount: number;
  postCount: number;
  status: number;
  lastLoginTime?: string;
  createdAt: string;
  isFollowed: boolean;
}

export interface Post {
  id: number;
  userId: number;
  title: string;
  description?: string;
  imageUrl?: string;
  location?: string;
  likeCount: number;
  commentCount: number;
  viewCount: number;
  status: number;
  createdAt: string;
  updatedAt?: string;
  isLiked: boolean;
}

export interface PostDTO extends Post {
  user: User;
}

export interface PostComment {
  id: number;
  postId: number;
  userId: number;
  parentId: number;
  content: string;
  user: User;
  createdAt: string;
  updatedAt?: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}