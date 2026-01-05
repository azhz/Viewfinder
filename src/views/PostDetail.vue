<template>
  <div class="post-detail-page">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <h1 class="logo">Viewfinder</h1>
          <nav class="nav">
            <router-link to="/home" class="nav-link">首页</router-link>
            <router-link to="/photos" class="nav-link">照片</router-link>
            <router-link to="/profile" class="nav-link">个人资料</router-link>
          </nav>
          <div class="user-actions">
            <button @click="goHome" class="btn btn-outline">主页</button>
            <button @click="goBack" class="btn btn-outline">返回</button>
            <router-link v-if="userStore.userInfo" to="/profile" class="user-profile">
              <img 
                v-if="userStore.userInfo.avatarUrl" 
                :src="getFullImageUrl(userStore.userInfo.avatarUrl)" 
                :alt="userStore.userInfo.username" 
                class="user-avatar"
              />
              <div v-else class="user-avatar-placeholder">
                {{ userStore.userInfo.username?.charAt(0).toUpperCase() }}
              </div>
            </router-link>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <div class="post-detail-container">
          <div class="post-content">
            <div v-if="loading" class="loading">加载中...</div>
            <div v-else-if="postStore.currentPost" class="post-card">
              <div class="post-header">
                <div class="user-info">
                  <img 
                    v-if="postStore.currentPost.user?.avatarUrl" 
                    :src="getFullImageUrl(postStore.currentPost.user.avatarUrl)" 
                    :alt="postStore.currentPost.user.username" 
                    class="avatar"
                  />
                  <div v-else class="avatar-placeholder">
                    {{ postStore.currentPost.user?.username?.charAt(0).toUpperCase() }}
                  </div>
                  <div class="user-details">
                    <h3>{{ postStore.currentPost.user?.nickname || postStore.currentPost.user?.username }}</h3>
                    <p class="post-time">{{ formatDate(postStore.currentPost.createdAt) }}</p>
                  </div>
                </div>
                <div v-if="userStore.userInfo?.id === postStore.currentPost.userId" class="post-actions">
                  <button @click="editPost" class="btn btn-outline">编辑</button>
                  <button @click="deletePost" class="btn btn-outline danger">删除</button>
                </div>
              </div>

              <div class="post-body">
                <h2>{{ postStore.currentPost.title }}</h2>
                <p v-if="postStore.currentPost.description" class="post-description">{{ postStore.currentPost.description }}</p>
                
                <div v-if="postStore.currentPost.imageUrl" class="post-image">
                  <img :src="getFullImageUrl(postStore.currentPost.imageUrl)" :alt="postStore.currentPost.title" />
                </div>
                
                <div v-if="postStore.currentPost.location" class="post-location">
                  <span class="location-icon">📍</span>
                  <span>{{ postStore.currentPost.location }}</span>
                </div>
              </div>

              <div class="post-footer">
                <div class="post-stats">
                  <button 
                    @click="toggleLike" 
                    :class="['like-btn', { liked: postStore.currentPost.isLiked }]"
                  >
                    <span class="like-icon">{{ postStore.currentPost.isLiked ? '❤️' : '🤍' }}</span>
                    <span>{{ postStore.currentPost.likeCount || 0 }}</span>
                  </button>
                  <span class="comment-count">{{ postStore.currentPost.commentCount || 0 }} 条评论</span>
                </div>
              </div>
            </div>
            <div v-else class="no-post">帖子不存在或已删除</div>
          </div>

          <div class="comments-section">
            <div v-if="postStore.currentPost" class="comment-form">
              <div class="user-avatar">
                <img 
                  v-if="userStore.userInfo?.avatarUrl" 
                  :src="getFullImageUrl(userStore.userInfo.avatarUrl)" 
                  :alt="userStore.userInfo?.username" 
                />
                <div v-else class="avatar-placeholder">
                  {{ userStore.userInfo?.username?.charAt(0).toUpperCase() }}
                </div>
              </div>
              <div class="comment-input-container">
                <textarea 
                  v-model="newComment" 
                  placeholder="写下你的评论..." 
                  class="comment-input"
                  rows="3"
                ></textarea>
                <button 
                  @click="submitComment" 
                  :disabled="!newComment.trim() || submittingComment"
                  class="submit-btn"
                >
                  {{ submittingComment ? '提交中...' : '评论' }}
                </button>
              </div>
            </div>

            <div class="comments-list">
              <div v-if="commentStore.loading && commentStore.comments.length === 0" class="loading-comments">加载评论中...</div>
              <div v-else-if="commentStore.comments.length === 0" class="no-comments">暂无评论</div>
              <div v-else>
                <div 
                  v-for="comment in commentStore.comments" 
                  :key="comment.id" 
                  class="comment-item"
                >
                  <div class="comment-header">
                    <div class="user-info">
                      <img 
                        v-if="comment.user?.avatarUrl" 
                        :src="getFullImageUrl(comment.user.avatarUrl)" 
                        :alt="comment.user.username" 
                        class="avatar"
                      />
                      <div v-else class="avatar-placeholder">
                        {{ comment.user?.username?.charAt(0).toUpperCase() }}
                      </div>
                      <div class="user-details">
                        <h4>{{ comment.user?.nickname || comment.user?.username }}</h4>
                        <p class="comment-time">{{ formatDate(comment.createdAt) }}</p>
                      </div>
                    </div>
                    <div v-if="userStore.userInfo?.id === comment.userId" class="comment-actions">
                      <button @click="deleteComment(comment.id)" class="btn btn-outline danger small">删除</button>
                    </div>
                  </div>
                  
                  <div class="comment-content">
                    <p>{{ comment.content }}</p>
                  </div>
                </div>

                <div v-if="commentStore.hasMore" class="load-more">
                  <button 
                    @click="loadMoreComments" 
                    :disabled="commentStore.loading"
                    class="btn btn-outline"
                  >
                    {{ commentStore.loading ? '加载中...' : '加载更多' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePostStore } from '@/stores/post'
import { useCommentStore } from '@/stores/comment'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const postStore = usePostStore()
const commentStore = useCommentStore()

const loading = ref(true)
const newComment = ref('')
const submittingComment = ref(false)
const showLoginPrompt = ref(false)

const userInfo = computed(() => userStore.userInfo)

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const goHome = () => {
  router.push('/home')
}

const goBack = () => {
  router.go(-1) // 返回上一页，如果上一页不存在则跳转到首页
}

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (!postStore.currentPost) return
  
  await postStore.toggleLike(postStore.currentPost.id)
}

const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (!newComment.value.trim()) return
  
  if (!postStore.currentPost) return
  
  submittingComment.value = true
  try {
    const success = await commentStore.createComment(postStore.currentPost.id, newComment.value)
    if (success) {
      newComment.value = ''
      // 更新帖子的评论数
      if (postStore.currentPost) {
        postStore.currentPost.commentCount = (postStore.currentPost.commentCount || 0) + 1
      }
    }
  } catch (error) {
    console.error('Failed to submit comment:', error)
  } finally {
    submittingComment.value = false
  }
}

const loadMoreComments = async () => {
  if (!postStore.currentPost) return
  await commentStore.fetchCommentsByPostId(postStore.currentPost.id)
}

const deleteComment = async (commentId: number) => {
  if (!confirm('确定要删除这条评论吗？')) return
  
  const success = await commentStore.deleteComment(commentId)
  if (success) {
    // 更新帖子的评论数
    if (postStore.currentPost) {
      postStore.currentPost.commentCount = Math.max(0, (postStore.currentPost.commentCount || 0) - 1)
    }
  }
}

const deletePost = async () => {
  if (!confirm('确定要删除这个帖子吗？')) return
  
  if (!postStore.currentPost) return
  
  const success = await postStore.deletePost(postStore.currentPost.id)
  if (success) {
    router.push('/home')
  }
}

const editPost = () => {
  if (postStore.currentPost) {
    // 这里可以导航到编辑页面，如果有的话
    console.log('Edit post:', postStore.currentPost.id)
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

const getFullImageUrl = (imageUrl?: string) => {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('http')) {
    return imageUrl
  }
  return `http://localhost:28080${imageUrl}`
}

onMounted(async () => {
  // 检查postId是否有效
  const postId = parseInt(route.params.postId as string)
  if (isNaN(postId) || route.params.postId === 'undefined') {
    console.error('Invalid postId:', route.params.postId)
    router.push('/home')
    return
  }
  
  // 加载帖子详情
  await postStore.fetchPostDetail(postId)
  
  // 加载评论
  if (postStore.currentPost) {
    await commentStore.fetchCommentsByPostId(postStore.currentPost.id, true)
  }
  
  // 设置loading为false以确保页面正确渲染
  loading.value = false
})
</script>

<style scoped>
.post-detail-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.header {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #007bff;
  margin: 0;
}

.nav {
  display: flex;
  gap: 2rem;
}

.nav-link {
  text-decoration: none;
  color: #333;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.nav-link:hover,
.nav-link.active {
  background-color: #007bff;
  color: white;
}

.user-actions {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  text-decoration: none;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 1rem;
}

.main-content {
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.post-detail-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
}

.post-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading {
  padding: 2rem;
  text-align: center;
  font-size: 1.2rem;
  color: #666;
}

.post-card {
  padding: 1.5rem;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-weight: bold;
}

.user-details h3 {
  margin: 0;
  font-size: 1.1rem;
}

.user-details .post-time {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.post-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-outline {
  padding: 0.5rem 1rem;
  border: 1px solid #ccc;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-outline:hover {
  border-color: #007bff;
  color: #007bff;
}

.btn-outline.danger {
  color: #dc3545;
  border-color: #dc3545;
}

.btn-outline.danger:hover {
  background-color: #dc3545;
  color: white;
}

.post-body h2 {
  margin: 0 0 1rem;
  font-size: 1.5rem;
}

.post-description {
  margin: 1rem 0;
  line-height: 1.6;
  color: #333;
}

.post-image {
  margin: 1rem 0;
}

.post-image img {
  width: 100%;
  border-radius: 8px;
  max-height: 500px;
  object-fit: cover;
}

.post-location {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  font-size: 0.9rem;
  margin-top: 1rem;
}

.post-footer {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.post-stats {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  color: #333;
}

.like-btn.liked {
  color: #e74c3c;
}

.comment-count {
  color: #666;
}

.comments-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  height: fit-content;
  position: sticky;
  top: 70px;
}

.comment-form {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.comment-form .user-avatar {
  width: 40px;
  height: 40px;
}

.comment-input-container {
  flex: 1;
}

.comment-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.comment-input:focus {
  outline: none;
  border-color: #007bff;
}

.submit-btn {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.submit-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.comments-list {
  border-top: 1px solid #eee;
  padding-top: 1rem;
}

.comment-item {
  padding: 1rem 0;
  border-bottom: 1px solid #eee;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.comment-header .user-info {
  gap: 0.5rem;
}

.comment-header .avatar {
  width: 30px;
  height: 30px;
}

.comment-header .avatar-placeholder {
  width: 30px;
  height: 30px;
  font-size: 0.8rem;
}

.comment-header .user-details h4 {
  margin: 0;
  font-size: 1rem;
}

.comment-header .user-details .comment-time {
  margin: 0;
  font-size: 0.8rem;
  color: #666;
}

.comment-actions {
  display: flex;
  gap: 0.5rem;
}

.comment-actions .btn-outline.small {
  padding: 0.25rem 0.5rem;
  font-size: 0.8rem;
}

.comment-content p {
  margin: 0;
  line-height: 1.5;
  color: #333;
}

.load-more {
  text-align: center;
  margin-top: 1rem;
}

.no-comments {
  text-align: center;
  color: #666;
  padding: 1rem;
}

.loading-comments {
  text-align: center;
  color: #666;
  padding: 1rem;
}

@media (max-width: 768px) {
  .post-detail-container {
    grid-template-columns: 1fr;
  }
  
  .post-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .comment-form {
    flex-direction: column;
  }
}
</style>