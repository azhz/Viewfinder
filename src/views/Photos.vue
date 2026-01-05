<template>
  <div class="photos-page">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <h1 class="logo">Viewfinder</h1>
          <nav class="nav">
            <router-link to="/home" class="nav-link">首页</router-link>
            <router-link to="/photos" class="nav-link active">照片</router-link>
            <router-link to="/profile" class="nav-link">个人资料</router-link>
          </nav>
          <div class="user-actions">
            <button @click="handleLogout" class="btn btn-secondary">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h2>我的照片</h2>
          <button class="btn btn-primary" @click="openUploadModal">
            上传照片
          </button>
        </div>

        <div class="photos-grid" v-if="photos.length > 0">
          <div 
            class="photo-card" 
            v-for="photo in photos" 
            :key="photo.id"
          >
            <img :src="getFullImageUrl(photo.imageUrl)" :alt="photo.title" class="photo-image" />
            <div class="photo-info">
              <h3>{{ photo.title }}</h3>
              <p v-if="photo.description">{{ photo.description }}</p>
              <div class="photo-meta">
                <span>{{ photo.likeCount || 0 }} 喜欢</span>
                <span>{{ photo.commentCount || 0 }} 评论</span>
                <span v-if="photo.location" class="location">📍 {{ photo.location }}</span>
              </div>
              <div class="photo-date">{{ formatDate(photo.createdAt) }}</div>
              <div class="photo-actions">
                <button @click="goToPostDetail(photo.id)" class="btn btn-sm btn-outline" style="margin-right: 5px;">查看</button>
                <button @click="deletePhoto(photo.id)" class="btn btn-sm btn-outline danger">删除</button>
              </div>
            </div>
          </div>
        </div>

        <div class="empty-state" v-else>
          <h3>还没有照片</h3>
          <p>上传你的第一张照片，开始分享精彩瞬间</p>
          <button class="btn btn-primary" @click="openUploadModal">
            上传照片
          </button>
        </div>
      </div>
    </main>

    <!-- 上传照片模态框 -->
    <div class="modal" v-if="showUploadModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>上传照片</h3>
          <button class="close-btn" @click="closeUploadModal">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handlePhotoUpload">
            <div class="form-group">
              <label for="photoFile">选择照片</label>
              <input
                id="photoFile"
                type="file"
                accept="image/*"
                @change="handleFileSelect"
                ref="fileInput"
                class="form-control"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="photoTitle">标题</label>
              <input
                id="photoTitle"
                v-model="uploadForm.title"
                type="text"
                class="form-control"
                placeholder="给照片起个名字"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="photoDescription">描述</label>
              <textarea
                id="photoDescription"
                v-model="uploadForm.description"
                rows="3"
                class="form-control"
                placeholder="描述一下这张照片的故事..."
              ></textarea>
            </div>
            
            <div class="form-group">
              <label for="photoLocation">位置</label>
              <input
                id="photoLocation"
                v-model="uploadForm.location"
                type="text"
                class="form-control"
                placeholder="拍摄地点（可选）"
              />
            </div>
            
            <button 
              type="submit" 
              class="btn btn-primary btn-block"
              :disabled="uploading"
            >
              {{ uploading ? '上传中...' : '上传照片' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { postApi } from '@/api/post'
import type { PostDTO } from '@/types'

interface UploadForm {
  title: string
  description: string
  location: string
}

const router = useRouter()
const userStore = useUserStore()

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

const showUploadModal = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const photos = ref<PostDTO[]>([])
const uploadForm = ref<UploadForm>({
  title: '',
  description: '',
  location: ''
})
const selectedFile = ref<File | null>(null)

const openUploadModal = () => {
  showUploadModal.value = true
}

const closeUploadModal = () => {
  showUploadModal.value = false
  uploadForm.value = {
    title: '',
    description: '',
    location: ''
  }
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const handleFileSelect = (event: Event) => {
  const fileInput = event.target as HTMLInputElement
  const file = fileInput.files?.[0]
  
  if (file) {
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件')
      return
    }
    
    selectedFile.value = file
  }
}

const handlePhotoUpload = async () => {
  if (!selectedFile.value) {
    alert('请选择图片文件')
    return
  }

  if (!uploadForm.value.title) {
    alert('请输入照片标题')
    return
  }

  uploading.value = true
  
  try {
    const res = await postApi.createPost(
      uploadForm.value.title,
      uploadForm.value.description,
      uploadForm.value.location,
      selectedFile.value
    )
    
    if (res.data.code === 200) {
      // 重新加载照片列表以确保数据一致性
      await loadUserPhotos()
      closeUploadModal()
    } else {
      alert('上传失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('Upload failed:', error)
    alert('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

const loadUserPhotos = async () => {
  if (!userStore.userInfo?.id) return
  
  try {
    const res = await postApi.getUserPosts(userStore.userInfo.id)
    if (res.data.code === 200) {
      photos.value = res.data.data.records || []
    } else {
      console.error('Failed to load photos:', res.data.message)
    }
  } catch (error) {
    console.error('Failed to load photos:', error)
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
      photos.value = photos.value.filter(photo => photo.id !== postId)
      // 更新用户信息中的照片数量
      if (userStore.userInfo) {
        userStore.userInfo.postCount = Math.max(0, (userStore.userInfo.postCount || 0) - 1)
      }
      alert('照片删除成功')
    } else {
      alert('删除照片失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('Failed to delete photo:', error)
    alert('删除照片失败')
  }
}

const goToPostDetail = (postId: number | undefined) => {
  if (postId) {
    router.push(`/post/${postId}`)
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

const getFullImageUrl = (imageUrl?: string) => {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('http')) {
    return imageUrl
  }
  return `http://localhost:28080${imageUrl}`
}

onMounted(async () => {
  await loadUserPhotos()
})
</script>

<style scoped>
.photos-page {
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 2rem;
}

.photo-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.photo-card:hover {
  transform: translateY(-5px);
}

.photo-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.photo-info {
  padding: 1rem;
}

.photo-info h3 {
  margin: 0 0 0.5rem;
  font-size: 1.1rem;
}

.photo-info p {
  margin: 0 0 1rem;
  color: #666;
  font-size: 0.9rem;
}

.photo-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: #999;
}

.photo-date {
  font-size: 0.8rem;
  color: #999;
  margin-top: 0.5rem;
}

.photo-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.empty-state h3 {
  margin: 0 0 1rem;
}

.empty-state p {
  margin: 0 0 1.5rem;
  color: #666;
}

/* Modal styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 1.5rem;
}

.btn-block {
  width: 100%;
}
</style>