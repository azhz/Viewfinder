import { defineStore } from 'pinia'
import { ref } from 'vue'
import { commentApi } from '@/api/comment'
import type { PostComment } from '@/types'

export const useCommentStore = defineStore('comment', () => {
  const comments = ref<PostComment[]>([])
  const loading = ref(false)
  const hasMore = ref(true)
  const page = ref(1)
  const size = ref(10)

  const fetchCommentsByPostId = async (postId: number, reset = false) => {
    if (reset) {
      page.value = 1
      comments.value = []
      hasMore.value = true
    }
    
    if (loading.value || !hasMore.value) return
    
    loading.value = true
    try {
      const res = await commentApi.getCommentsByPostId(postId, page.value, size.value)
      if (res.data.code === 200) {
        const newComments = res.data.data.records || []
        if (reset) {
          comments.value = newComments
        } else {
          comments.value = [...comments.value, ...newComments]
        }
        
        // 检查是否还有更多数据
        if (newComments.length < size.value) {
          hasMore.value = false
        } else {
          page.value++
        }
        
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to fetch comments:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  const createComment = async (postId: number, content: string) => {
    try {
      const res = await commentApi.createComment(postId, content)
      if (res.data.code === 200) {
        // 添加新评论到列表开头
        comments.value.unshift(res.data.data)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to create comment:', error)
      return false
    }
  }

  const replyComment = async (commentId: number, content: string) => {
    try {
      const res = await commentApi.replyComment(commentId, content)
      return res.data.code === 200
    } catch (error) {
      console.error('Failed to reply comment:', error)
      return false
    }
  }

  const deleteComment = async (commentId: number) => {
    try {
      const res = await commentApi.deleteComment(commentId)
      if (res.data.code === 200) {
        // 从列表中移除评论
        comments.value = comments.value.filter(comment => comment.id !== commentId)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to delete comment:', error)
      return false
    }
  }

  const resetComments = () => {
    comments.value = []
    page.value = 1
    hasMore.value = true
    loading.value = false
  }

  return {
    comments,
    loading,
    hasMore,
    fetchCommentsByPostId,
    createComment,
    replyComment,
    deleteComment,
    resetComments
  }
})