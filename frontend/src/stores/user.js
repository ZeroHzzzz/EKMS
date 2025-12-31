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
      if (res.code === 200 && res.data) {
        token.value = res.data.token
        userInfo.value = res.data.userInfo
        localStorage.setItem('token', token.value)
        localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
      } else {
        throw new Error(res.message || '登录失败')
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || error.message || '登录失败')
    }
  }

  const register = async (username, password, realName, email, department, role) => {
    try {
      const res = await api.post('/auth/register', {
        username,
        password,
        realName,
        email,
        department,
        role
      })
      if (res.code === 200 && res.data) {
        return res.data
      } else {
        throw new Error(res.message || '注册失败')
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || error.message || '注册失败')
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

