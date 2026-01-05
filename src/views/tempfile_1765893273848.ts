const handleAvatarChange = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !userInfo.value?.id) return

  // 添加文件类型验证
  if (!file.type.startsWith('image/')) {
    console.error('请选择图片文件')
    return
  }

  // 添加文件大小限制
  if (file.size > 5 * 1024 * 1024) {
    console.error('图片大小不能超过5MB')
    return
  }

  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    console.log('正在上传文件:', file.name)
    const response = await userApi.uploadAvatar(userInfo.value.id, formData)
    console.log('上传响应:', response)
    if (response.code === 200) {
      await userStore.fetchUserInfo()
      console.log('更新后的用户信息:', userStore.userInfo)
      console.log('头像更新成功')
    } else {
      console.error('头像更新失败:', response.message)
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    alert('头像上传失败，请重试')
  } finally {
    loading.value = false
  }
}
