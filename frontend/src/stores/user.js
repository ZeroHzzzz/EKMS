import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const login = async (username, password) => {
    try {
      const res = await api.post('/auth/login', { username, password })
      if (res.code === 200 && res.data) {
        token.value = res.data.token
        userInfo.value = res.data.userInfo
        localStorage.setItem('token', token.value)
      } else {
        throw new Error(res.message || '登录失败')
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || error.message || '登录失败')
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    userInfo,
    token,
    login,
    logout
  }
})

