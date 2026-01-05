<template>
  <div class="avatar-upload">
    <div class="avatar-container" @click="triggerFileInput">
      <img :src="avatarUrl" alt="avatar" class="avatar-image" />
      <div class="upload-overlay">
        <span>更换头像</span>
      </div>
    </div>
    <input
      type="file"
      ref="fileInput"
      @change="handleFileChange"
      accept="image/*"
      style="display: none"
    />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi } from '@/api'

const fileInput = ref<HTMLInputElement>()
const avatarUrl = ref('')
const userInfo = ref()

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await userApi.uploadAvatar(formData)
    avatarUrl.value = response.data
  } catch (error) {
    console.error('上传失败:', error)
  }
}

const getUserInfo = async () => {
  try {
    const response = await userApi.getUserInfo()
    if (response.code === 200) {
      userInfo.value = response.data
      avatarUrl.value = response.data.avatarUrl
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

onMounted(() => {
  getUserInfo()
})
</script>


<style scoped>
.avatar-upload {
  position: relative;
  width: 120px;
  height: 120px;
}

.avatar-container {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-container:hover .upload-overlay {
  opacity: 1;
}
</style>
