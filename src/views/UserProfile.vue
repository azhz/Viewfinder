<template>
  <div class="user-profile-page">
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
            <button @click="handleLogout" class="btn btn-secondary">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <div class="profile-header" v-if="userProfile">
          <div class="avatar-section">
            <img 
              :src="userProfile.avatarUrl ? getFullImageUrl(userProfile.avatarUrl) : 'http://localhost:28080/default-avatar.png'" 
              alt="Avatar" 
              class="avatar"
            />
          </div>
          
          <div class="user-basic-info">
            <h2>{{ userProfile.nickname || userProfile.username }}</h2>
            <p class="username">@{{ userProfile.username }}</p>
            <p v-if="userProfile.bio" class="bio">{{ userProfile.bio }}</p>
            
            <div class="user-stats">
              <div class="stat-item">
                <span class="stat-number">{{ userProfile.postCount || 0 }}</span>
                <span class="stat-label">照片</span>
              </div>
              <div class="stat-item">
                <span class="stat-number">{{ userProfile.followCount || 0 }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-item">
                <span class="stat-number">{{ userProfile.fanCount || 0 }}</span>
                <span class="stat-label">粉丝</span>
              </div>
            </div>
            
            <div class="user-actions-buttons" v-if="currentUserId !== userProfile.id">
              <button 
                v-if="userProfile.isFollowed" 
                @click="handleUnfollow"
                class="btn btn-outline"
              >
                已关注
              </button>
              <button 
                v-else 
                @click="handleFollow"
                class="btn btn-primary"
              >
                关注
              </button>
            </div>
          </div>
        </div>

        <div class="user-photos">
          <h3>照片</h3>
          <div class="photos-grid" v-if="userPhotos.length > 0">
            <div 
              class="photo-card" 
              v-for="photo in userPhotos" 
              :key="photo.id"
              @click="goToPostDetail(photo.id)"
            >
              <img :src="getFullImageUrl(photo.imageUrl)" :alt="photo.title" class="photo-image" />
              <div class="photo-overlay">
                <div class="photo-stats">
                  <span>❤️ {{ photo.likeCount || 0 }}</span>
                  <span>💬 {{ photo.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="empty-state" v-else>
            <p>还没有照片</p>
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
import { postApi } from '@/api/post'
import type { User, PostDTO } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userProfile = ref<User | null>(null)
const userPhotos = ref<PostDTO[]>([])
const loading = ref(true)

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

const loadUserProfile = async () => {
  try {
    const res = await userStore.fetchUserInfo(targetUserId.value)
    if (res && typeof res !== 'boolean') {
      userProfile.value = res  // 直接使用返回的用户信息
    }
  } catch (error) {
    console.error('Failed to load user profile:', error)
  }
}

const loadUserPhotos = async () => {
  try {
    const res = await postApi.getUserPosts(targetUserId.value)
    if (res.data.code === 200) {
      userPhotos.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('Failed to load user photos:', error)
  }
}

const handleFollow = async () => {
  if (!userProfile.value?.id) return
  
  const success = await userStore.followUser(userProfile.value.id)
  if (success) {
    // 重新加载用户信息以更新关注状态
    await loadUserProfile()
  }
}

const handleUnfollow = async () => {
  if (!userProfile.value?.id) return
  
  const success = await userStore.unfollowUser(userProfile.value.id)
  if (success) {
    // 重新加载用户信息以更新关注状态
    await loadUserProfile()
  }
}

const goToPostDetail = (postId: number | undefined) => {
  if (postId) {
    router.push(`/post/${postId}`)
  }
}

const getFullImageUrl = (imageUrl?: string) => {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('http')) {
    return imageUrl
  }
  return `http://localhost:28080${imageUrl}`
}

onMounted(async () => {
  await Promise.all([
    loadUserProfile(),
    loadUserPhotos()
  ])
  loading.value = false
})
</script>

<style scoped>
.user-profile-page {
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

.main-content {
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.profile-header {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  display: flex;
  gap: 2rem;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.user-basic-info {
  flex: 1;
}

.user-basic-info h2 {
  margin: 0 0 0.5rem;
  font-size: 1.8rem;
}

.username {
  color: #666;
  margin: 0 0 1rem;
}

.bio {
  margin: 0 0 1.5rem;
  color: #333;
}

.user-stats {
  display: flex;
  gap: 2rem;
  margin-bottom: 1.5rem;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 1.5rem;
  font-weight: bold;
  color: #007bff;
}

.stat-label {
  color: #666;
  font-size: 0.9rem;
}

.user-actions-buttons {
  display: flex;
  gap: 1rem;
}

.user-photos h3 {
  margin: 0 0 1.5rem;
  font-size: 1.5rem;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
}

.photo-card {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  aspect-ratio: 1/1;
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0,0,0,0.7));
  color: white;
  padding: 1rem;
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.photo-card:hover .photo-overlay {
  transform: translateY(0);
}

.photo-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.9rem;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  
  .user-stats {
    justify-content: center;
  }
  
  .photos-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}
</style>