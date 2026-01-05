<template>
  <div class="home-page">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <h1 class="logo">Viewfinder</h1>
          <nav class="nav">
            <router-link to="/home" class="nav-link active">首页</router-link>
            <router-link to="/photos" class="nav-link">照片</router-link>
            <router-link to="/profile" class="nav-link">个人资料</router-link>
          </nav>
          <div class="user-actions">
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
            <button @click="handleLogout" class="btn btn-secondary">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <div class="search-section">
      <SearchBar />
    </div>

    <main class="main-content">
      <div class="container">
        <div class="welcome-section">
          <h2>欢迎回来，{{ userInfo?.nickname || userInfo?.username }}!</h2>
          <p>开始分享你的精彩瞬间吧</p>
        </div>

        <div class="content-area">
          <div class="posts-section">
            <div class="create-post-section">
              <div class="user-info">
                <img 
                  :src="getFullImageUrl(userInfo?.avatarUrl) || '/default-avatar.png'" 
                  alt="Avatar" 
                  class="avatar" 
                />
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
                    <img 
                      :src="getFullImageUrl(post.user?.avatarUrl) || '/default-avatar.png'" 
                      alt="Avatar" 
                      class="avatar" 
                    />
                    <div class="user-details">
                      <h4 @click.stop="goToUserProfile(post.userId)" class="user-name">{{ post.user?.nickname || post.user?.username }}</h4>
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
                  <img 
                    v-if="post.imageUrl" 
                    :src="getFullImageUrl(post.imageUrl)" 
                    alt="Post Image" 
                    class="post-image" 
                  />
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
          
          <div class="sidebar">
            <div class="trending-section">
              <h3>热门内容</h3>
              <div class="trending-item" v-for="post in trendingPosts" :key="post.id" @click="goToPostDetail(post.id)">
                <p>{{ post.title }}</p>
                <small>{{ post.likeCount || 0 }} 赞 · {{ post.commentCount || 0 }} 评论</small>
              </div>
            </div>
            
            <div class="suggested-users">
              <h3>推荐关注</h3>
              <div class="suggested-user" v-for="user in suggestedUsers" :key="user.id">
                <div class="user-info" @click="goToUserProfile(user.id)">
                  <img 
                    v-if="user.avatarUrl" 
                    :src="getFullImageUrl(user.avatarUrl)" 
                    :alt="user.username" 
                    class="avatar"
                  />
                  <div v-else class="avatar-placeholder">
                    {{ user.username?.charAt(0).toUpperCase() }}
                  </div>
                  <div class="user-details">
                    <h4>{{ user.nickname || user.username }}</h4>
                    <p v-if="user.bio">{{ user.bio.substring(0, 30) }}{{ user.bio.length > 30 ? '...' : '' }}</p>
                  </div>
                </div>
                <button 
                  v-if="currentUserId !== user.id" 
                  @click="followUser(user.id, $event)"
                  :class="['btn', user.isFollowed ? 'btn-outline' : 'btn-primary']"
                >
                  {{ user.isFollowed ? '已关注' : '关注' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { postApi } from '@/api/post'
import type { PostDTO, User } from '@/types'
import SearchBar from '@/components/SearchBar.vue'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const posts = ref<PostDTO[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const trendingPosts = ref<PostDTO[]>([])
const suggestedUsers = ref<User[]>([])
const currentUserId = computed(() => userStore.userInfo?.id)

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

const fetchTrendingPosts = async () => {
  // 获取热门帖子 - 这里是模拟实现，实际应用中需要后端提供接口
  try {
    const response = await postApi.getAllPosts(1, 3);
    if (response.data.code === 200) {
      trendingPosts.value = response.data.data.records || [];
    }
  } catch (error) {
    console.error('获取热门帖子失败:', error);
  }
};

const fetchSuggestedUsers = async () => {
  // 获取推荐用户 - 这里是模拟实现，实际应用中需要后端提供接口
  try {
    // 模拟获取推荐用户
    suggestedUsers.value = [
      {
        id: 2,
        username: 'user1',
        nickname: '推荐用户1',
        bio: '这是一个推荐用户的简介',
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
        username: 'user2',
        nickname: '推荐用户2',
        bio: '另一个推荐用户',
        avatarUrl: '/default-avatar.png',
        followCount: 15,
        fanCount: 25,
        postCount: 8,
        status: 1,
        createdAt: new Date().toISOString(),
        isFollowed: false
      },
      {
        id: 4,
        username: 'user3',
        nickname: '推荐用户3',
        bio: '又一个推荐用户',
        avatarUrl: '/default-avatar.png',
        followCount: 8,
        fanCount: 12,
        postCount: 3,
        status: 1,
        createdAt: new Date().toISOString(),
        isFollowed: true
      }
    ];
  } catch (error) {
    console.error('获取推荐用户失败:', error);
  }
};

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const goToPostDetail = (postId: number | undefined) => {
  if (postId) {
    router.push(`/post/${postId}`);
  }
};

const goToUserProfile = (userId: number | undefined) => {
  if (userId && userId !== currentUserId.value) {
    router.push(`/user/${userId}`);
  } else if (userId === currentUserId.value) {
    router.push('/profile');
  }
};

const goToCreatePost = () => {
  router.push('/photos');
};

const loadMore = () => {
  fetchPosts();
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

const followUser = async (userId: number, event: Event) => {
  event.stopPropagation(); // 阻止事件冒泡
  
  const user = suggestedUsers.value.find(u => u.id === userId);
  if (!user) return;
  
  if (user.isFollowed) {
    const success = await userStore.unfollowUser(userId);
    if (success) {
      user.isFollowed = false;
      // 更新粉丝数
      user.fanCount = Math.max(0, user.fanCount! - 1);
    }
  } else {
    const success = await userStore.followUser(userId);
    if (success) {
      user.isFollowed = true;
      // 更新粉丝数
      user.fanCount = (user.fanCount || 0) + 1;
    }
  }
};

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
};

const getFullImageUrl = (imageUrl?: string) => {
  if (!imageUrl) return '';
  if (imageUrl.startsWith('http')) {
    return imageUrl;
  }
  return `http://localhost:28080${imageUrl}`;
};

onMounted(async () => {
  await Promise.all([
    fetchPosts(),
    fetchTrendingPosts(),
    fetchSuggestedUsers()
  ]);
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
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

.welcome-section {
  text-align: center;
  margin-bottom: 3rem;
}

.welcome-section h2 {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  color: #333;
}

.welcome-section p {
  font-size: 1.2rem;
  color: #666;
}

.content-area {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
}

.posts-section {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.create-post-section {
  background: white;
  border-radius: 12px;
  padding: 15px;
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

.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-weight: bold;
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

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.trending-section, .suggested-users {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.trending-section h3, .suggested-users h3 {
  margin: 0 0 1rem;
  color: #333;
}

.trending-item {
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
}

.trending-item:last-child {
  border-bottom: none;
}

.suggested-user {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
}

.suggested-user:last-child {
  border-bottom: none;
}

.suggested-user .user-info {
  flex: 1;
  gap: 0.5rem;
}

.suggested-user .user-details {
  flex: 1;
}

.suggested-user .user-details h4 {
  margin: 0 0 0.25rem 0;
  font-size: 1rem;
}

.suggested-user .user-details p {
  margin: 0;
  font-size: 0.8rem;
  color: #666;
}

.btn-outline {
  padding: 0.25rem 0.75rem;
  border: 1px solid #007bff;
  background: white;
  color: #007bff;
  border-radius: 4px;
  cursor: pointer;
}

.btn-outline:hover {
  background-color: #007bff;
  color: white;
}

@media (max-width: 768px) {
  .content-area {
    grid-template-columns: 1fr;
  }
  
  .sidebar {
    display: none;
  }
}
</style>