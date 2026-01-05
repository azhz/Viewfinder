<template>
  <div class="register-page">
    <header class="header">
      <div class="container">
        <div class="header-content">
          <h1 class="logo" @click="goHome">Viewfinder</h1>
          <nav class="nav">
            <router-link to="/home" class="nav-link">首页</router-link>
          </nav>
          <div class="user-actions">
            <button @click="goHome" class="btn btn-outline">主页</button>
            <button @click="goBack" class="btn btn-outline">返回</button>
          </div>
        </div>
      </div>
    </header>
    <div class="register-container">
      <div class="register-card">
        <h2 class="register-title">用户注册</h2>
        
        <div v-if="errorMessage" class="alert alert-danger">
          {{ errorMessage }}
        </div>
        
        <form @submit.prevent="handleRegister" class="register-form">
          <div class="form-group">
            <label for="username">用户名</label>
            <input
              id="username"
              v-model="registerForm.username"
              type="text"
              class="form-control"
              required
              placeholder="请输入用户名"
            />
          </div>
          
          <div class="form-group">
            <label for="email">邮箱</label>
            <input
              id="email"
              v-model="registerForm.email"
              type="email"
              class="form-control"
              required
              placeholder="请输入邮箱"
            />
          </div>
          
          <div class="form-group">
            <label for="phone">手机号</label>
            <input
              id="phone"
              v-model="registerForm.phone"
              type="tel"
              class="form-control"
              placeholder="请输入手机号（可选）"
            />
          </div>
          
          <div class="form-group">
            <label for="password">密码</label>
            <input
              id="password"
              v-model="registerForm.password"
              type="password"
              class="form-control"
              required
              placeholder="请输入密码"
            />
          </div>
          
          <div class="form-group">
            <label for="confirmPassword">确认密码</label>
            <input
              id="confirmPassword"
              v-model="registerForm.confirmPassword"
              type="password"
              class="form-control"
              required
              placeholder="请再次输入密码"
            />
          </div>
          
          <div class="form-group">
            <label for="code">验证码</label>
            <div class="verification-code-group">
              <input
                id="code"
                v-model="registerForm.code"
                type="text"
                class="form-control"
                required
                placeholder="请输入验证码"
              />
              <button
                type="button"
                @click="sendCode"
                :disabled="codeSending || countdown > 0"
                class="btn btn-secondary send-code-btn"
              >
                {{ codeSending ? '发送中...' : (countdown > 0 ? `${countdown}s后重发` : '发送验证码') }}
              </button>
            </div>
          </div>
          
          <button 
            type="submit" 
            class="btn btn-primary btn-block"
            :disabled="loading"
          >
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>
        
        <div class="register-footer">
          <p>已有账号？<router-link to="/login">立即登录</router-link></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

interface RegisterForm {
  username: string
  email: string
  phone: string
  password: string
  confirmPassword: string
  code: string
}

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const registerForm = ref<RegisterForm>({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  code: ''
})

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

const sendCode = async () => {
  if (!registerForm.value.email) {
    errorMessage.value = '请先输入邮箱'
    return
  }

  codeSending.value = true
  try {
    const success = await userStore.sendVerificationCode(registerForm.value.email, 1)
    if (success) {
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      errorMessage.value = '验证码发送失败'
    }
  } catch (error) {
    errorMessage.value = '验证码发送失败，请稍后重试'
  }
  codeSending.value = false
}

const handleRegister = async () => {
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const success = await userStore.register(registerForm.value)
    if (success) {
      router.push('/login')
    } else {
      errorMessage.value = '注册失败，请检查输入信息'
    }
  } catch (err: any) {
    errorMessage.value = err.response?.data?.message || '注册失败，请稍后重试'
    console.error('Register error:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background-color: #f8f9fa;
  padding: 20px;
}

.header {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
  margin-bottom: 2rem;
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

.register-container {
  width: 100%;
  max-width: 450px;
  margin: 0 auto;
}

.register-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.1);
}

.register-title {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
}

.register-form {
  margin-bottom: 1.5rem;
}

.verification-code-group {
  display: flex;
  gap: 10px;
}

.verification-code-group .form-control {
  flex: 1;
}

.send-code-btn {
  white-space: nowrap;
}

.btn-block {
  width: 100%;
}

.register-footer {
  text-align: center;
  border-top: 1px solid #eee;
  padding-top: 1rem;
}

.register-footer a {
  color: #007bff;
  text-decoration: none;
}

.register-footer a:hover {
  text-decoration: underline;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn-outline {
  background-color: transparent;
  border: 1px solid #007bff;
  color: #007bff;
}

.btn-outline:hover {
  background-color: #007bff;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #545b62;
}
</style>
```
