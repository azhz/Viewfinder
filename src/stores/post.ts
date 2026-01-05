import { defineStore } from 'pinia'
import { ref } from 'vue'
import { postApi } from '@/api/post'
import type { PostDTO } from '@/types'

export const usePostStore = defineStore('post', () => {
  const currentPost = ref<PostDTO | null>(null)
  const loading = ref(false)

  const fetchPostDetail = async (postId: number) => {
    loading.value = true
    try {
      const res = await postApi.getPost(postId)
      if (res.data.code === 200) {
        currentPost.value = res.data.data
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to fetch post detail:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  const createPost = async (title: string, description?: string, location?: string, file?: File) => {
    try {
      const res = await postApi.createPost(title, description, location, file)
      return res.data.code === 200
    } catch (error) {
      console.error('Failed to create post:', error)
      return false
    }
  }

  const updatePost = async (postId: number, data: Partial<PostDTO>) => {
    try {
      const res = await postApi.updatePost(postId, data)
      if (res.data.code === 200) {
        if (currentPost.value && currentPost.value.id === postId) {
          Object.assign(currentPost.value, res.data.data)
        }
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to update post:', error)
      return false
    }
  }

  const deletePost = async (postId: number) => {
    try {
      const res = await postApi.deletePost(postId)
      if (res.data.code === 200) {
        if (currentPost.value && currentPost.value.id === postId) {
          currentPost.value = null
        }
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to delete post:', error)
      return false
    }
  }

  const toggleLike = async (postId: number) => {
    if (!currentPost.value || currentPost.value.id !== postId) return false

    try {
      const res = await postApi.toggleLike(postId)
      if (res.data.code === 200) {
        // 前端先更新状态，等待后续API更新
        currentPost.value.isLiked = !currentPost.value.isLiked
        if (currentPost.value.isLiked) {
          currentPost.value.likeCount = (currentPost.value.likeCount || 0) + 1
        } else {
          currentPost.value.likeCount = Math.max(0, (currentPost.value.likeCount || 0) - 1)
        }
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to toggle like:', error)
      return false
    }
  }

  return {
    currentPost,
    loading,
    fetchPostDetail,
    createPost,
    updatePost,
    deletePost,
    toggleLike
  }
})