<template>
  <div class="profile-page">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <h1 class="logo" @click="goHome">Viewfinder</h1>
          <nav class="nav">
            <router-link to="/home" class="nav-link">首页</router-link>
            <router-link to="/photos" class="nav-link">照片</router-link>
            <router-link to="/profile" class="nav-link active">个人资料</router-link>
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
        <div class="profile-header">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <img 
                v-if="userInfo?.avatarUrl" 
                :src="getFullImageUrl(userInfo.avatarUrl)" 
                alt="Avatar" 
                class="avatar"
                @error="handleImageError"
              />
              <div v-else class="avatar-placeholder">
                {{ userInfo?.username?.charAt(0).toUpperCase() }}
              </div>
              <div class="avatar-overlay" @click="triggerFileInput">
                <button class="change-avatar-btn">更改头像</button>
              </div>
            </div>
            <input
              type="file"
              accept="image/*"
              @change="handleAvatarChange"
              ref="avatarInput"
              style="display: none"
            />
          </div>
          
          <div class="user-basic-info">
            <h2>{{ userInfo?.nickname || userInfo?.username }}</h2>
            <p class="username">@{{ userInfo?.username }}</p>
            <p v-if="userInfo?.bio" class="bio">{{ userInfo.bio }}</p>
            
            <div class="user-stats">
              <div class="stat-item">
                <span class="stat-number">{{ userInfo?.postCount || 0 }}</span>
                <span class="stat-label">照片</span>
              </div>
              <div class="stat-item">
                <span class="stat-number">{{ userInfo?.followCount || 0 }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-item">
                <span class="stat-number">{{ userInfo?.fanCount || 0 }}</span>
                <span class="stat-label">粉丝</span>
              </div>
            </div>
          </div>
        </div>

        <div class="profile-tabs">
          <button 
            :class="['tab-btn', { active: activeTab === 'info' }]"
            @click="activeTab = 'info'"
          >
            个人信息
          </button>
          <button 
            :class="['tab-btn', { active: activeTab === 'posts' }]"
            @click="activeTab = 'posts'"
          >
            我的照片
          </button>
          <button 
            :class="['tab-btn', { active: activeTab === 'following' }]"
            @click="activeTab = 'following'"
          >
            关注
          </button>
          <button 
            :class="['tab-btn', { active: activeTab === 'followers' }]"
            @click="activeTab = 'followers'"
          >
            粉丝
          </button>
        </div>

        <div class="profile-content">
          <!-- 个人信息标签页 -->
          <div v-if="activeTab === 'info'" class="tab-content">
            <div class="section">
              <h3>编辑个人资料</h3>
              
              <div v-if="profileMessage" :class="['alert', profileMessageType === 'success' ? 'alert-success' : 'alert-danger']">
                {{ profileMessage }}
              </div>
              
              <form @submit.prevent="updateProfile" class="profile-form">
                <div class="form-group">
                  <label for="nickname">昵称</label>
                  <input
                    id="nickname"
                    v-model="profileForm.nickname"
                    type="text"
                    class="form-control"
                  />
                </div>
                
                <div class="form-group">
                  <label for="bio">个人简介</label>
                  <textarea
                    id="bio"
                    v-model="profileForm.bio"
                    rows="3"
                    class="form-control"
                    placeholder="介绍一下自己..."
                  ></textarea>
                </div>
                
                <div class="form-row">
                  <div class="form-group">
                    <label for="location">所在地</label>
                    <input
                      id="location"
                      v-model="profileForm.location"
                      type="text"
                      class="form-control"
                    />
                  </div>
                  
                  <div class="form-group">
                    <label for="website">个人网站</label>
                    <input
                      id="website"
                      v-model="profileForm.website"
                      type="url"
                      class="form-control"
                    />
                  </div>
                </div>
                
                <button 
                  type="submit" 
                  class="btn btn-primary"
                  :disabled="updatingProfile"
                >
                  {{ updatingProfile ? '保存中...' : '保存更改' }}
                </button>
              </form>
            </div>

            <div class="section">
              <h3>修改密码</h3>
              
              <div v-if="passwordMessage" :class="['alert', passwordMessageType === 'success' ? 'alert-success' : 'alert-danger']">
                {{ passwordMessage }}
              </div>
              
              <form @submit.prevent="changePassword" class="password-form">
                <div class="form-group">
                  <label for="oldPassword">当前密码</label>
                  <input
                    id="oldPassword"
                    v-model="passwordForm.oldPassword"
                    type="password"
                    class="form-control"
                    required
                  />
                </div>
                
                <div class="form-group">
                  <label for="newPassword">新密码</label>
                  <input
                    id="newPassword"
                    v-model="passwordForm.newPassword"
                    type="password"
                    class="form-control"
                    required
                  />
                </div>
                
                <div class="form-group">
                  <label for="confirmPassword">确认新密码</label>
                  <input
                    id="confirmPassword"
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    class="form-control"
                    required
                  />
                </div>
                
                <button 
                  type="submit" 
                  class="btn btn-primary"
                  :disabled="updatingPassword"
                >
                  {{ updatingPassword ? '修改中...' : '修改密码' }}
                </button>
              </form>
            </div>
          </div>

          <!-- 我的照片标签页 -->
          <div v-if="activeTab === 'posts'" class="tab-content">
            <div v-if="loadingPhotos" class="loading">加载中...</div>
            <div v-else-if="userPhotos.length === 0" class="empty-state">
              <p>还没有照片</p>
            </div>
            <div v-else class="photos-grid">
              <div 
                class="photo-card" 
                v-for="photo in userPhotos" 
                :key="photo.id"
              >
                <img :src="getFullImageUrl(photo.imageUrl)" :alt="photo.title" class="photo-image" />
                <div class="photo-overlay">
                  <div class="photo-stats">
                    <span>❤️ {{ photo.likeCount || 0 }}</span>
                    <span>💬 {{ photo.commentCount || 0 }}</span>
                  </div>
                  <div class="photo-actions">
                    <button @click.stop="goToPostDetail(photo.id)" class="btn btn-sm btn-outline" style="margin-right: 5px;">查看</button>
                    <button @click.stop="deletePhoto(photo.id)" class="btn btn-sm btn-outline danger">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 关注列表标签页 -->
          <div v-if="activeTab === 'following'" class="tab-content">
            <div v-if="loadingFollowing" class="loading">加载中...</div>
            <div v-else-if="followingList.length === 0" class="empty-state">
              <p>还没有关注任何人</p>
            </div>
            <div v-else class="users-list">
              <div 
                v-for="user in followingList" 
                :key="user.id" 
                class="user-card"
              >
                <div class="user-info" @click="goToUserProfile(user.id)">
                  <img 
                    :src="getFullImageUrl(user.avatarUrl)" 
                    :alt="user.username" 
                    class="avatar"
                  />
                  <div class="user-details">
                    <h4>{{ user.nickname || user.username }}</h4>
                    <p v-if="user.bio">{{ user.bio.substring(0, 30) }}{{ user.bio.length > 30 ? '...' : '' }}</p>
                  </div>
                </div>
                <button 
                  @click="unfollowUser(user.id)"
                  class="btn btn-outline danger small"
                >
                  取消关注
                </button>
              </div>
            </div>
          </div>

          <!-- 粉丝列表标签页 -->
          <div v-if="activeTab === 'followers'" class="tab-content">
            <div v-if="loadingFollowers" class="loading">加载中...</div>
            <div v-else-if="followerList.length === 0" class="empty-state">
              <p>还没有粉丝</p>
            </div>
            <div v-else class="users-list">
              <div 
                v-for="user in followerList" 
                :key="user.id" 
                class="user-card"
              >
                <div class="user-info" @click="goToUserProfile(user.id)">
                  <img 
                    :src="getFullImageUrl(user.avatarUrl)" 
                    :alt="user.username" 
                    class="avatar"
                  />
                  <div class="user-details">
                    <h4>{{ user.nickname || user.username }}</h4>
                    <p v-if="user.bio">{{ user.bio.substring(0, 30) }}{{ user.bio.length > 30 ? '...' : '' }}</p>
                  </div>
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
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { postApi } from '@/api/post'
import type { User, PostDTO } from '@/types'

interface ProfileForm {
  nickname: string
  bio: string
  location: string
  website: string
}

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const router = useRouter()
const userStore = useUserStore()

const avatarInput = ref<HTMLInputElement | null>(null)
const updatingProfile = ref(false)
const updatingPassword = ref(false)
const profileMessage = ref('')
const profileMessageType = ref('')
const passwordMessage = ref('')
const passwordMessageType = ref('')
const activeTab = ref('info')
const userPhotos = ref<PostDTO[]>([])
const followingList = ref<User[]>([])
const followerList = ref<User[]>([])
const loadingPhotos = ref(false)
const loadingFollowing = ref(false)
const loadingFollowers = ref(false)

const userInfo = computed(() => userStore.userInfo)

const profileForm = ref<ProfileForm>({
  nickname: '',
  bio: '',
  location: '',
  website: ''
})

const passwordForm = ref<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const goHome = () => {
  router.push('/home')
}

const goBack = () => {
  router.back()
}

const handleImageError = (event: Event) => {
  const imgElement = event.target as HTMLImageElement
  console.error('头像加载失败:', imgElement.src)
}

const triggerFileInput = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const fileInput = event.target as HTMLInputElement
  const file = fileInput.files?.[0]

  // 清除之前的消息
  profileMessage.value = ''
  profileMessageType.value = ''

  // 检查用户是否登录
  if (!userStore.isLoggedIn || !userInfo.value?.id) {
    showProfileMessage('用户未登录，请重新登录', 'error')
    fileInput.value = '' // 清空文件输入框
    return
  }

  if (!file) {
    showProfileMessage('未选择文件', 'error')
    return
  }

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    showProfileMessage('请选择图片文件', 'error')
    fileInput.value = '' // 清空文件输入框
    return
  }

  // 验证文件大小 (最大5MB)
  if (file.size > 5 * 1024 * 1024) {
    showProfileMessage('图片大小不能超过5MB', 'error')
    fileInput.value = '' // 清空文件输入框
    return
  }

  try {
    const success = await userStore.uploadAvatar(file)
    console.log('上传结果:', success)
    
    if (success) {
      showProfileMessage('头像更新成功', 'success')
    } else {
      showProfileMessage('头像更新失败', 'error')
    }
  } catch (error: any) {
    console.error('头像上传异常:', error)
    const errorMessage = error.response?.data?.message || error.message || '未知错误'
    showProfileMessage('头像上传失败: ' + errorMessage, 'error')
  } finally {
    // 清空文件输入框，确保下次选择相同文件也能触发change事件
    fileInput.value = ''
  }
}

const updateProfile = async () => {
  if (!userInfo.value?.id) return

  updatingProfile.value = true
  profileMessage.value = ''
  
  try {
    const success = await userStore.updateUserInfo(profileForm.value)
    if (success) {
      showProfileMessage('个人信息更新成功', 'success')
    } else {
      showProfileMessage('更新失败', 'error')
    }
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || error.message || '未知错误'
    showProfileMessage('更新失败: ' + errorMessage, 'error')
  } finally {
    updatingProfile.value = false
  }
}

const changePassword = async () => {
  if (!userInfo.value?.id) return

  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showPasswordMessage('两次输入的密码不一致', 'error')
    return
  }

  updatingPassword.value = true
  passwordMessage.value = ''
  
  try {
    const success = await userStore.changePassword(
      passwordForm.value.oldPassword,
      passwordForm.value.newPassword
    )
    
    if (success) {
      showPasswordMessage('密码修改成功', 'success')
      passwordForm.value = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    } else {
      showPasswordMessage('密码修改失败', 'error')
    }
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || error.message || '未知错误'
    showPasswordMessage('密码修改失败: ' + errorMessage, 'error')
  } finally {
    updatingPassword.value = false
  }
}

const showProfileMessage = (message: string, type: 'success' | 'error') => {
  profileMessage.value = message
  profileMessageType.value = type
  
  // 3秒后清除消息
  setTimeout(() => {
    profileMessage.value = ''
    profileMessageType.value = ''
  }, 3000)
}

const showPasswordMessage = (message: string, type: 'success' | 'error') => {
  passwordMessage.value = message
  passwordMessageType.value = type
  
  // 3秒后清除消息
  setTimeout(() => {
    passwordMessage.value = ''
    passwordMessageType.value = ''
  }, 3000)
}

const loadUserPhotos = async () => {
  if (!userInfo.value?.id) return
  
  loadingPhotos.value = true
  try {
    const res = await postApi.getUserPosts(userInfo.value.id)
    if (res.data.code === 200) {
      userPhotos.value = res.data.data.records || []
    } else {
      showProfileMessage('加载照片失败: ' + res.data.message, 'error')
    }
  } catch (error: any) {
    console.error('Failed to load user photos:', error)
    showProfileMessage('加载照片失败: ' + (error.message || '未知错误'), 'error')
  } finally {
    loadingPhotos.value = false
  }
}

const loadFollowingList = async () => {
  if (!userInfo.value?.id) return
  
  loadingFollowing.value = true
  try {
    const res = await userStore.getFollowingList(userInfo.value.id)
    if (res) {
      followingList.value = res.records || []
    } else {
      showProfileMessage('加载关注列表失败', 'error')
    }
  } catch (error: any) {
    console.error('Failed to load following list:', error)
    showProfileMessage('加载关注列表失败: ' + (error.message || '未知错误'), 'error')
  } finally {
    loadingFollowing.value = false
  }
}

const loadFollowerList = async () => {
  if (!userInfo.value?.id) return
  
  loadingFollowers.value = true
  try {
    const res = await userStore.getFollowerList(userInfo.value.id)
    if (res) {
      followerList.value = res.records || []
    } else {
      showProfileMessage('加载粉丝列表失败', 'error')
    }
  } catch (error: any) {
    console.error('Failed to load follower list:', error)
    showProfileMessage('加载粉丝列表失败: ' + (error.message || '未知错误'), 'error')
  } finally {
    loadingFollowers.value = false
  }
}

const unfollowUser = async (userId: number) => {
  if (!confirm('确定要取消关注吗？')) {
    return
  }
  
  const success = await userStore.unfollowUser(userId)
  if (success) {
    // 从关注列表中移除该用户
    followingList.value = followingList.value.filter(user => user.id !== userId)
    // 更新用户信息中的关注数
    if (userInfo.value) {
      userInfo.value.followCount = Math.max(0, (userInfo.value.followCount || 0) - 1)
    }
    showProfileMessage('取消关注成功', 'success')
  } else {
    showProfileMessage('取消关注失败', 'error')
  }
}

const goToPostDetail = (postId: number | undefined) => {
  if (postId) {
    router.push(`/post/${postId}`)
  }
}

const deletePhoto = async (postId: number | undefined) => {
  if (!postId) return
  
  if (!confirm('确定要删除这张照片吗？删除后将无法恢复。')) {
    return
  }
  
  try {
    const res = await postApi.deletePost(postId)
    if (res.data.code === 200) {
      // 从本地列表中移除已删除的帖子，避免UI闪烁
      userPhotos.value = userPhotos.value.filter(photo => photo.id !== postId)
      showProfileMessage('照片删除成功', 'success')
      // 如果在其他页面也显示了照片，可能需要更新用户信息中的照片数量
      if (userInfo.value) {
        userInfo.value.postCount = Math.max(0, (userInfo.value.postCount || 0) - 1)
      }
    } else {
      showProfileMessage('删除照片失败: ' + res.data.message, 'error')
    }
  } catch (error: any) {
    console.error('Failed to delete photo:', error)
    showProfileMessage('删除照片失败: ' + (error.message || '未知错误'), 'error')
  }
}

const goToUserProfile = (userId: number | undefined) => {
  if (userId && userId !== userInfo.value?.id) {
    router.push(`/user/${userId}`)
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
  // 确保用户信息是最新的
  await userStore.fetchUserInfo()
  if (userInfo.value) {
    profileForm.value = {
      nickname: userInfo.value.nickname || '',
      bio: userInfo.value.bio || '',
      location: userInfo.value.location || '',
      website: userInfo.value.website || ''
    }
  }
  
  // 根据当前标签页加载相应数据
  if (activeTab.value === 'posts') {
    await loadUserPhotos()
  } else if (activeTab.value === 'following') {
    await loadFollowingList()
  } else if (activeTab.value === 'followers') {
    await loadFollowerList()
  }
})

// 监听标签页变化，加载相应数据
watch(activeTab, async (newTab) => {
  switch(newTab) {
    case 'posts':
      await loadUserPhotos()
      break
    case 'following':
      await loadFollowingList()
      break
    case 'followers':
      await loadFollowerList()
      break
  }
})
</script>

<style scoped>
.profile-page {
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
  position: relative;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  color: #666;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  cursor: pointer;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.change-avatar-btn {
  background: rgba(255, 255, 255, 0.8);
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
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

.profile-tabs {
  display: flex;
  margin-bottom: 2rem;
  border-bottom: 1px solid #ddd;
}

.tab-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 1rem;
  color: #666;
  border-bottom: 2px solid transparent;
}

.tab-btn.active {
  color: #007bff;
  border-bottom: 2px solid #007bff;
}

.profile-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.tab-content {
  min-height: 300px;
}

.section {
  margin-bottom: 2rem;
}

.section:last-child {
  margin-bottom: 0;
}

.section h3 {
  margin: 0 0 1.5rem;
  color: #333;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 1rem;
}

.alert {
  padding: 0.75rem;
  margin-bottom: 1rem;
  border-radius: 4px;
}

.alert-success {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.alert-danger {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-primary:disabled {
  background-color: #007bff;
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: white;
  color: #007bff;
  border: 1px solid #007bff;
}

.btn-outline:hover {
  background-color: #007bff;
  color: white;
}

.btn-outline.danger {
  color: #dc3545;
  border-color: #dc3545;
}

.btn-outline.danger:hover {
  background-color: #dc3545;
  color: white;
}

.btn-block {
  width: 100%;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
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
  padding: 0.5rem;
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.photo-card:hover .photo-overlay {
  transform: translateY(0);
}

.photo-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
}

.photo-actions {
  margin-top: 0.5rem;
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.user-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 8px;
}

.user-card .user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  cursor: pointer;
}

.user-card .avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.user-card .user-details {
  flex: 1;
}

.user-card .user-details h4 {
  margin: 0 0 0.25rem 0;
  font-size: 1rem;
}

.user-card .user-details p {
  margin: 0;
  font-size: 0.8rem;
  color: #666;
}

.loading, .empty-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.loading {
  font-size: 1.2rem;
}

.empty-state {
  font-size: 1.1rem;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  
  .user-stats {
    justify-content: center;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .photos-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
  
  .profile-tabs {
    flex-wrap: wrap;
  }
  
  .tab-btn {
    flex: 1;
    padding: 0.5rem;
    font-size: 0.9rem;
  }
  
  .user-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .user-card .user-info {
    width: 100%;
  }
}
</style>