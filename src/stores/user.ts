import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userApi } from '@/api/user'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)
  const isLoggedIn = ref<boolean>(!!token.value)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
    isLoggedIn.value = true
  }

  const clearToken = () => {
    token.value = ''
    localStorage.removeItem('token')
    isLoggedIn.value = false
  }

  const setUserInfo = (user: User) => {
    userInfo.value = user
  }

  const login = async (loginData: { username: string; password: string; rememberMe?: boolean }) => {
    try {
      const res = await userApi.login(loginData)
      if (res.data.code === 200) {
        setToken(res.data.data.token)
        setUserInfo(res.data.data.userInfo)
        return true
      }
      return false
    } catch (error) {
      console.error('Login failed:', error)
      return false
    }
  }

  const register = async (registerData: { 
    username: string; 
    password: string; 
    confirmPassword: string; 
    email?: string; 
    phone?: string; 
    code: string 
  }) => {
    try {
      const res = await userApi.register(registerData)
      return res.data.code === 200
    } catch (error) {
      console.error('Register failed:', error)
      return false
    }
  }

  const logout = () => {
    clearToken()
    userInfo.value = null
  }

  const fetchUserInfo = async (userId?: number, updateStore: boolean = true) => {
    const id = userId || userInfo.value?.id;
    if (!id) {
      // 如果没有提供userId且当前用户信息中也没有id，尝试通过token获取用户信息
      if (!userId && token.value && !userInfo.value?.id) {
        return await refreshUserInfo();
      }
      return false
    }

    try {
      const res = await userApi.getUserInfo(id)
      if (res.data.code === 200) {
        // 只有在获取当前登录用户信息或显式要求更新store时才更新store
        if (updateStore && (!userId || userId === userInfo.value?.id)) {
          setUserInfo(res.data.data)
        }
        return res.data.data; // 返回获取到的用户信息
      }
      return false
    } catch (error) {
      console.error('Failed to fetch user info:', error)
      return false
    }
  }

  // 使用现有token刷新当前用户信息
  const refreshUserInfo = async () => {
    try {
      // 获取用户ID，如果当前用户信息中没有ID，可能需要通过其他方式获取
      // 由于我们无法从JWT中直接解析用户ID，需要调用一个获取当前用户信息的API
      // 或者需要一个能通过token获取用户信息的端点
      const res = await userApi.getCurrentUserInfo();
      if (res.data.code === 200) {
        setUserInfo(res.data.data);
        return res.data.data;
      }
      return false;
    } catch (error) {
      console.error('Failed to refresh user info:', error);
      // 如果刷新失败，可能token无效，清除本地存储
      clearToken();
      userInfo.value = null;
      return false;
    }
  }

  const updateUserInfo = async (data: Partial<User>) => {
    if (!userInfo.value?.id) return false

    try {
      const res = await userApi.updateUserInfo(userInfo.value.id, data)
      if (res.data.code === 200) {
        setUserInfo(res.data.data)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to update user info:', error)
      return false
    }
  }

  const changePassword = async (oldPassword: string, newPassword: string) => {
    if (!userInfo.value?.id) return false

    try {
      const res = await userApi.changePassword(userInfo.value.id, {
        oldPassword,
        newPassword,
        confirmPassword: newPassword
      })
      return res.data.code === 200
    } catch (error) {
      console.error('Failed to change password:', error)
      return false
    }
  }

  const sendVerificationCode = async (receiver: string, type: number = 1) => {
    try {
      const res = await userApi.sendVerificationCode(receiver, type)
      return res.data.code === 200
    } catch (error) {
      console.error('Failed to send verification code:', error)
      return false
    }
  }

  const followUser = async (targetUserId: number) => {
    if (!userInfo.value?.id) return false

    try {
      const res = await userApi.followUser(userInfo.value.id, targetUserId)
      if (res.data.code === 200) {
        // 更新关注者信息
        await fetchUserInfo(userInfo.value.id)
        // 更新被关注者信息以更新粉丝数
        await fetchUserInfo(targetUserId, false)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to follow user:', error)
      return false
    }
  }

  const unfollowUser = async (targetUserId: number) => {
    if (!userInfo.value?.id) return false

    try {
      const res = await userApi.unfollowUser(userInfo.value.id, targetUserId)
      if (res.data.code === 200) {
        // 更新关注者信息
        await fetchUserInfo(userInfo.value.id)
        // 更新被关注者信息以更新粉丝数
        await fetchUserInfo(targetUserId, false)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to unfollow user:', error)
      return false
    }
  }

  const uploadAvatar = async (file: File) => {
    if (!userInfo.value?.id) {
      console.error('用户未登录或用户ID不存在')
      return false
    }

    try {
      console.log('调用上传头像API，用户ID:', userInfo.value.id)
      const res = await userApi.uploadAvatar(userInfo.value.id, file)
      console.log('API响应:', res)
      
      if (res.data.code === 200) {
        // 更新用户信息
        await fetchUserInfo()
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to upload avatar:', error)
      return false
    }
  }

  // 获取关注列表
  const getFollowingList = async (userId: number, page: number = 1, size: number = 10) => {
    try {
      const res = await userApi.getFollowingList(userId, page, size)
      if (res.data.code === 200) {
        return res.data.data
      }
      return null
    } catch (error) {
      console.error('Failed to get following list:', error)
      return null
    }
  }

  // 获取粉丝列表
  const getFollowerList = async (userId: number, page: number = 1, size: number = 10) => {
    try {
      const res = await userApi.getFollowerList(userId, page, size)
      if (res.data.code === 200) {
        return res.data.data
      }
      return null
    } catch (error) {
      console.error('Failed to get follower list:', error)
      return null
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    clearToken,
    setUserInfo,
    login,
    register,
    logout,
    fetchUserInfo,
    updateUserInfo,
    changePassword,
    sendVerificationCode,
    followUser,
    unfollowUser,
    uploadAvatar,
    getFollowingList,
    getFollowerList
  }
})