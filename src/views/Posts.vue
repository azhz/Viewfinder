<template>
  <div class="posts-page">
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
        <div class="create-post-section">
          <div class="user-info">
            <img :src="getFullImageUrl(currentUser?.avatarUrl) || '/default-avatar.png'" alt="Avatar" class="avatar" />
            <input 
              type="text" 
              placeholder="分享你的动态..." 
              class="post-input" 
              @click="goToCreatePost"
            />
          </div>
        </div>
        
        <div class="posts-list">
          <div 
            v-for="post in posts" 
            :key="post.id" 
            class="post-card"
          >
            <div class="post-header">
              <div class="user-info">
                <img :src="getFullImageUrl(post.user?.avatarUrl) || '/default-avatar.png'" alt="Avatar" class="avatar" />
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
            
            <div class="post-content" @click="goToPostDetail(post.id)">
              <h3>{{ post.title }}</h3>
              <p v-if="post.description">{{ post.description }}</p>
              <img v-if="post.imageUrl" :src="getFullImageUrl(post.imageUrl)" alt="Post Image" class="post-image" />
            </div>
            
            <div class="post-actions">
              <button 
                @click.stop="togglePostLike(post)"
                :class="['action-btn', { 'liked': post.isLiked }]"
              >
                <span>❤️</span>
                <span>{{ post.likeCount || 0 }}</span>
              </button>
              <button 
                @click.stop="goToPostDetail(post.id)" 
                class="action-btn"
              >
                <span>💬</span>
                <span>{{ post.commentCount || 0 }}</span>
              </button>
              <button class="action-btn">
                <span>📤</span>
              </button>
            </div>
          </div>
        </div>
        
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="hasMore" class="load-more">
          <button @click="loadMore" class="load-more-btn">加载更多</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { postApi } from '../api/post';
import type { PostDTO } from '../types';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();

const posts = ref<PostDTO[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const hasMore = ref(true);

const currentUser = computed(() => userStore.userInfo);

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

const getFullImageUrl = (imageUrl?: string) => {
  if (!imageUrl) return '';
  if (imageUrl.startsWith('http')) {
    return imageUrl;
  }
  return `http://localhost:28080${imageUrl}`;
};

const fetchPosts = async () => {
  if (loading.value) return;
  
  loading.value = true;
  try {
    const response = await postApi.getAllPosts(page.value, size.value);
    if (response.data.code === 200) {
      const newPosts = response.data.data.records || [];
      
      if (newPosts.length < size.value) {
        hasMore.value = false;
      }
      
      posts.value = [...posts.value, ...newPosts];
      page.value++;
    } else {
      console.error('获取帖子列表失败:', response.data.message);
    }
  } catch (error) {
    console.error('获取帖子列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const togglePostLike = async (post: PostDTO) => {
  try {
    const response = await postApi.toggleLike(post.id!);
    if (response.data.code === 200) {
      // 切换点赞状态
      post.isLiked = !post.isLiked;
      // 更新点赞数
      if (post.isLiked) {
        post.likeCount = (post.likeCount || 0) + 1;
      } else {
        post.likeCount = Math.max(0, (post.likeCount || 0) - 1);
      }
    }
  } catch (error) {
    console.error('点赞操作失败:', error);
  }
};

const goToPostDetail = (postId: number | undefined) => {
  if (postId) {
    router.push(`/post/${postId}`);
  }
};

const goToCreatePost = () => {
  router.push('/photos'); // 跳转到照片上传页面
};

const loadMore = () => {
  fetchPosts();
};

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
};

onMounted(() => {
  fetchPosts();
});
</script>

<style scoped>
.posts-page {
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
  gap: 1rem;
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

.create-post-section {
  background: white;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.post-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
}

.post-input:focus {
  border-color: #007bff;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
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

.post-content {
  cursor: pointer;
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

.post-actions {
  display: flex;
  gap: 30px;
  padding-top: 15px;
  border-top: 1px solid #eee;
  margin-top: 15px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #666;
  transition: color 0.2s;
}

.action-btn:hover {
  color: #333;
}

.action-btn.liked {
  color: #ff6b6b;
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
</style>
```
