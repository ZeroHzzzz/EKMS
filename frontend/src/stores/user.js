import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  // 从localStorage恢复用户信息
  const savedUserInfo = localStorage.getItem('userInfo')
  const userInfo = ref(savedUserInfo ? JSON.parse(savedUserInfo) : null)
  const token = ref(localStorage.getItem('token') || '')

  // 初始化：如果有token但没有userInfo，尝试获取用户信息
  const initUserInfo = async () => {
    if (token.value && !userInfo.value) {
      try {
        // 从token中提取用户ID（token格式：token-{userId}-{timestamp}）
        const tokenParts = token.value.split('-')
        if (tokenParts.length >= 2) {
          const userId = tokenParts[1]
          const res = await api.get(`/user/${userId}`)
          if (res.code === 200 && res.data) {
            userInfo.value = res.data
            localStorage.setItem('userInfo', JSON.stringify(res.data))
          }
        }
      } catch (error) {
        console.error('恢复用户信息失败', error)
        // 如果获取失败，清除token
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      }
    }
  }

  const login = async (username, password) => {
    try {
      const res = await api.post('/auth/login', { username, password })
      // 如果到这里，说明 res.code === 200
      if (res.data) {
        token.value = res.data.token
        userInfo.value = res.data.userInfo
        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
        return { success: true }
      } else {
        return { success: false, message: '登录失败：未返回用户数据' }
      }
    } catch (error) {
      // api拦截器会将错误格式化为 { code, message, data }
      const errorMessage = error.message || '登录失败'
      return { success: false, message: errorMessage }
    }
  }

  const register = async (username, password, realName, email, departmentId, role) => {
    try {
      const res = await api.post('/auth/register', {
        username,
        password,
        realName,
        email,
        departmentId,
        role
      })
      // 如果到这里，说明 res.code === 200
      return { success: true, data: res.data }
    } catch (error) {
      // api拦截器会将错误格式化为 { code, message, data }
      const errorMessage = error.message || '注册失败'
      return { success: false, message: errorMessage }
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    userInfo,
    token,
    login,
    register,
    logout,
    initUserInfo
  }
})

