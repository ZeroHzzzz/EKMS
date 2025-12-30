import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const login = async (username, password) => {
    // 这里应该调用实际的登录API
    // const res = await api.post('/login', { username, password })
    // token.value = res.data.token
    // userInfo.value = res.data.userInfo
    // localStorage.setItem('token', token.value)
    
    // 临时模拟
    token.value = 'mock-token'
    userInfo.value = { id: 1, username, realName: '测试用户' }
    localStorage.setItem('token', token.value)
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

