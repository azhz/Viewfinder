<template>
  <div class="search-results-page">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <h1 class="logo" @click="goHome">Viewfinder</h1>
          <nav class="nav">
            <router-link to="/home" class="nav-link">首页</router-link>
            <router-link to="/photos" class="nav-link">照片</router-link>
            <router-link to="/profile" class="nav-link">个人资料</router-link>
          </nav>
          <div class="user-actions">
            <button @click="goHome" class="btn btn-outline">主页</button>
            <button @click="goBack" class="btn btn-outline">返回</button>
            <button @click="handleLogout" class="btn btn-secondary">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <div class="search-header">
          <h2>搜索结果：{{ keyword }}</h2>
          <div class="search-filters">
            <button 
              :class="['filter-btn', { active: activeFilter === 'all' }]"
              @click="setFilter('all')"
            >
              全部
            </button>
            <button 
              :class="['filter-btn', { active: activeFilter === 'posts' }]"
              @click="setFilter('posts')"
            >
              照片
            </button>
            <button 
              :class="['filter-btn', { active: activeFilter === 'users' }]"
              @click="setFilter('users')"
            >
              用户
            </button>
          </div>
        </div>

        <div class="results-container" v-if="activeFilter === 'posts' || activeFilter === 'all'">
          <div v-if="posts.length === 0 && !loading" class="no-results">
            <p>没有找到相关照片</p>
          </div>
          <div v-else class="posts-results">
            <div 
              v-for="post in posts" 
              :key="post.id" 
              class="post-card"
              @click="goToPostDetail(post.id)"
            >
              <div class="post-header">
                <div class="user-info" @click.stop="goToUserProfile(post.userId)">
                  <img 
                    :src="getFullImageUrl(post.user?.avatarUrl) || '/default-avatar.png'" 
                    alt="Avatar" 
                    class="avatar" 
                  />
                  <div class="user-details">
                    <h4>{{ post.user?.nickname || post.user?.username }}</h4>
                    <p class="post-time">{{ formatDate(post.createdAt) }}</p>
                  </div>
                </div>
                <div v-if="post.location" class="location">
                  <span>📍</span>
                  <span>{{ post.location }}</span>
                </div>
              </div>
              
              <div class="post-content">
                <h3>{{ post.title }}</h3>
                <p v-if="post.description">{{ post.description }}</p>
                <img 
                  v-if="post.imageUrl" 
                  :src="getFullImageUrl(post.imageUrl)" 
                  alt="Post Image" 
                  class="post-image" 
                />
              </div>
              
              <div class="post-stats">
                <span>❤️ {{ post.likeCount || 0 }}</span>
                <span>💬 {{ post.commentCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="results-container" v-if="activeFilter === 'users' || activeFilter === 'all'">
          <div v-if="users.length === 0 && !loadingUsers" class="no-results">
            <p>没有找到相关用户</p>
          </div>
          <div v-else class="users-results">
            <div 
              v-for="user in users" 
              :key="user.id" 
              class="user-card"
              @click="goToUserProfile(user.id)"
            >
              <div class="user-info">
                <img 
                  :src="getFullImageUrl(user.avatarUrl) || '/default-avatar.png'" 
                  alt="Avatar" 
                  class="avatar" 
                />
                <div class="user-details">
                  <h4>{{ user.nickname || user.username }}</h4>
                  <p v-if="user.bio">{{ user.bio.substring(0, 50) }}{{ user.bio.length > 50 ? '...' : '' }}</p>
                  <div class="user-stats">
                    <span>{{ user.postCount || 0 }} 照片</span>
                    <span>{{ user.fanCount || 0 }} 粉丝</span>
                  </div>
                </div>
              </div>
              <button 
                v-if="currentUserId !== user.id" 
                @click.stop="toggleFollow(user)"
                :class="['btn', user.isFollowed ? 'btn-outline' : 'btn-primary']"
              >
                {{ user.isFollowed ? '已关注' : '关注' }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading">搜索中...</div>
        <div v-else-if="hasMore && activeFilter !== 'users'" class="load-more">
          <button @click="loadMore" class="load-more-btn">加载更多</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { postApi } from '@/api/post'
import type { PostDTO, User } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const keyword = computed(() => route.params.keyword as string)
const posts = ref<PostDTO[]>([])
const users = ref<User[]>([])
const loading = ref(false)
const loadingUsers = ref(false)
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const activeFilter = ref('all') // 'all', 'posts', 'users'
const currentUserId = computed(() => userStore.userInfo?.id)

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

const performPostSearch = async (reset = false) => {
  if (reset) {
    page.value = 1
    posts.value = []
    hasMore.value = true
  }
  
  if (loading.value || !hasMore.value) return
  
  loading.value = true
  try {
    const response = await postApi.searchPosts(keyword.value, page.value, size.value)
    if (response.data.code === 200) {
      const newPosts = response.data.data.records || []
      
      if (reset) {
        posts.value = newPosts
      } else {
        posts.value = [...posts.value, ...newPosts]
      }
      
      if (newPosts.length < size.value) {
        hasMore.value = false
      } else {
        page.value++
      }
    } else {
      console.error('搜索帖子失败:', response.data.message)
    }
  } catch (error) {
    console.error('搜索帖子失败:', error)
  } finally {
    loading.value = false
  }
}

const performUserSearch = async () => {
  // 暂时使用推荐用户模拟搜索，实际应用中需要后端提供用户搜索API
  loadingUsers.value = true
  try {
    // 模拟搜索用户
    users.value = [
      {
        id: 2,
        username: 'traveler1',
        nickname: '旅行者1号',
        bio: '热爱旅行和摄影，分享世界各地美景',
        avatarUrl: '/default-avatar.png',
        followCount: 10,
        fanCount: 20,
        postCount: 5,
        status: 1,
        createdAt: new Date().toISOString(),
        isFollowed: false
      },
      {
        id: 3,
        username: 'wanderlust',
        nickname: '漫游者',
        bio: '用镜头记录旅途中的美好瞬间',
        avatarUrl: '/default-avatar.png',
        followCount: 15,
        fanCount: 25,
        postCount: 8,
        status: 1,
        createdAt: new Date().toISOString(),
        isFollowed: true
      }
    ];
  } catch (error) {
    console.error('搜索用户失败:', error)
  } finally {
    loadingUsers.value = false
  }
}

const setFilter = (filter: string) => {
  activeFilter.value = filter
  
  if (filter === 'posts' || filter === 'all') {
    performPostSearch(true)
  }
  
  if (filter === 'users' || filter === 'all') {
    performUserSearch()
  }
}

const loadMore = () => {
  performPostSearch()
}

const goToPostDetail = (postId: number | undefined) => {
  if (postId) {
    router.push(`/post/${postId}`)
  }
}

const goToUserProfile = (userId: number | undefined) => {
  if (userId && userId !== currentUserId.value) {
    router.push(`/user/${userId}`)
  } else if (userId === currentUserId.value) {
    router.push('/profile')
  }
}

const toggleFollow = async (user: User) => {
  if (user.isFollowed) {
    const success = await userStore.unfollowUser(user.id!)
    if (success) {
      user.isFollowed = false
      user.fanCount = Math.max(0, (user.fanCount || 0) - 1)
    }
  } else {
    const success = await userStore.followUser(user.id!)
    if (success) {
      user.isFollowed = true
      user.fanCount = (user.fanCount || 0) + 1
    }
  }
}

const formatDate = (dateString: string | undefined) => {
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
  await performPostSearch()
  await performUserSearch()
})
</script>

<style scoped>
.search-results-page {
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
  cursor: pointer;
}

.logo:hover {
  color: #0056b3;
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

.main-content {
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.search-header {
  margin-bottom: 2rem;
}

.search-header h2 {
  margin: 0 0 1rem;
  font-size: 1.8rem;
}

.search-filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.filter-btn.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.results-container {
  margin-bottom: 2rem;
}

.post-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
  margin-bottom: 1.5rem;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.user-details {
  flex: 1;
}

.user-details h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
}

.post-time {
  margin: 0;
  font-size: 12px;
  color: #888;
}

.location {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 14px;
}

.post-content h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  font-weight: 600;
}

.post-content p {
  margin: 0 0 15px 0;
  color: #333;
  line-height: 1.6;
}

.post-image {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
  border-radius: 8px;
}

.post-stats {
  display: flex;
  gap: 20px;
  color: #666;
}

.user-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  cursor: pointer;
}

.user-card .user-info {
  flex: 1;
  gap: 10px;
}

.user-card .user-details {
  flex: 1;
}

.user-card .user-details h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: 600;
}

.user-card .user-details p {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #666;
}

.user-stats {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #888;
}

.no-results {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.loading, .load-more {
  text-align: center;
  padding: 20px;
}

.load-more-btn {
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.load-more-btn:hover {
  background: #0056b3;
}