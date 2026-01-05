import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCounterStore = defineStore('counter', () => {
  const count = ref(0)
  const doubleCount = ref(0)

  function increment() {
    count.value++
    doubleCount.value = count.value * 2
  }

  return { count, doubleCount, increment }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { commentApi } from '@/api/comment'
import type { PostComment } from '@/types'

export const useCommentStore = defineStore('comment', () => {
  const comments = ref<PostComment[]>([])
  const loading = ref<boolean>(false)
  const hasMore = ref<boolean>(true)
  const page = ref<number>(1)
  const size = ref<number>(10)

  // 获取帖子评论
  const fetchCommentsByPostId = async (postId: number, reset = false) => {
    if (loading.value) return

    loading.value = true
    try {
      if (reset) {
        page.value = 1
        comments.value = []
      }

      const res = await commentApi.getCommentsByPostId(postId, page.value, size.value)
      if (res.data.code === 200) {
        if (reset) {
          comments.value = res.data.data.records
        } else {
          comments.value = [...comments.value, ...res.data.data.records]
        }
        
        hasMore.value = comments.value.length < res.data.data.total
        page.value++
        
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

  // 创建评论
  const createComment = async (postId: number, content: string) => {
    loading.value = true
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
    } finally {
      loading.value = false
    }
  }

  // 回复评论
  const replyComment = async (commentId: number, content: string) => {
    loading.value = true
    try {
      const res = await commentApi.replyComment(commentId, content)
      if (res.data.code === 200) {
        // 评论回复通常不需要更新本地列表，因为它会显示在原评论下
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to reply comment:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  // 删除评论
  const deleteComment = async (commentId: number) => {
    try {
      const res = await commentApi.deleteComment(commentId)
      if (res.data.code === 200) {
        // 从本地列表中移除评论
        comments.value = comments.value.filter(comment => comment.id !== commentId)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to delete comment:', error)
      return false
    }
  }

  // 重置分页状态
  const resetPagination = () => {
    page.value = 1
    hasMore.value = true
    comments.value = []
  }

  return {
    comments,
    loading,
    hasMore,
    page,
    size,
    fetchCommentsByPostId,
    createComment,
    replyComment,
    deleteComment,
    resetPagination
  }
})