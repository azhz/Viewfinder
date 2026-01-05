import api from './index';
import type { PostDTO } from '@/types';

// 帖子相关API
export const postApi = {
  // 创建帖子
  createPost: (title: string, description?: string, location?: string, file?: File) => {
    const formData = new FormData();
    formData.append('title', title);
    if (description) formData.append('description', description);
    if (location) formData.append('location', location);
    if (file) formData.append('file', file);

    return api.post<PostDTO>('/posts', formData);
  },

  // 获取帖子详情
  getPost: (postId: number) => {
    return api.get<PostDTO>(`/posts/${postId}`);
  },

  // 获取用户帖子列表
  getUserPosts: (userId: number, page = 1, size = 10) => {
    return api.get<PageResult<PostDTO>>(
      `/posts/user/${userId}`,
      {
        params: { page, size }
      }
    );
  },

  // 获取所有帖子
  getAllPosts: (page = 1, size = 10) => {
    return api.get<PageResult<PostDTO>>(
      '/posts',
      {
        params: { page, size }
      }
    );
  },

  // 搜索帖子
  searchPosts: (keyword: string, page = 1, size = 10) => {
    return api.get<{ records: PostDTO[]; total: number; current: number; size: number }>(
      '/posts',
      {
        params: { 
          page, 
          size,
          keyword
        }
      }
    );
  },

  // 更新帖子
  updatePost: (postId: number, data: Partial<PostDTO>) => {
    return api.put<PostDTO>(`/posts/${postId}`, data);
  },

  // 删除帖子
  deletePost: (postId: number) => {
    return api.delete(`/posts/${postId}`);
  },

  // 点赞/取消点赞
  toggleLike: (postId: number) => {
    return api.post(`/posts/${postId}/like`);
  }
};