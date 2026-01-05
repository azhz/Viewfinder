import api from './index';
import type { PostComment } from '@/types';

// 评论相关API
export const commentApi = {
  // 创建评论
  createComment: (postId: number, content: string) => {
    return api.post<PostComment>('/comments', { content }, {
      params: { postId }
    });
  },

  // 获取帖子评论列表
  getCommentsByPostId: (postId: number, page = 1, size = 10) => {
    return api.get<{ records: PostComment[]; total: number; current: number; size: number }>(
      `/comments/post/${postId}`,
      {
        params: { page, size }
      }
    );
  },

  // 回复评论
  replyComment: (commentId: number, content: string) => {
    return api.post<PostComment>(`/comments/reply/${commentId}`, { content });
  },

  // 删除评论
  deleteComment: (commentId: number) => {
    return api.delete(`/comments/${commentId}`);
  }
};