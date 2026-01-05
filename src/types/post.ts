export interface Post {
  id?: number;
  userId?: number;
  title: string;
  description?: string;
  imageUrl?: string;
  location?: string;
  likeCount?: number;
  commentCount?: number;
  viewCount?: number;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
  isLiked?: boolean;
  user?: User;
}

export interface PostDTO extends Post {
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
  user: User;
}

export interface Comment {
  id?: number;
  postId: number;
  userId: number;
  parentId?: number;
  content: string;
  createdAt?: string;
  updatedAt?: string;
  user?: User;
}

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