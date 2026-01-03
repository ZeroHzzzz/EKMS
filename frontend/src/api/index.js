import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    const res = response.data
    // 业务成功，直接返回
    if (res.code === 200) {
      return res
    }
    // 业务错误，不在这里显示消息，让调用方处理
    // 返回一个包含错误信息的对象，而不是 reject
    return Promise.reject({
      code: res.code,
      message: res.message || '请求失败',
      data: res.data
    })
  },
  error => {
    // 网络错误或HTTP错误
    if (error.response) {
      // 有响应但状态码不是2xx
      const status = error.response.status
      const message = error.response?.data?.message || `请求失败 (${status})`
      
      // 404 错误不显示错误提示（可能是接口未实现，静默处理）
      if (status === 404) {
        return Promise.reject({
          code: 404,
          message: '接口不存在',
          data: null
        })
      }
      
      // 其他HTTP错误，返回错误信息让调用方处理
      return Promise.reject({
        code: status,
        message: message,
        data: error.response.data
      })
    } else if (error.request) {
      // 请求已发出但没有收到响应（网络错误）
      return Promise.reject({
        code: -1,
        message: '网络错误，请检查网络连接',
        data: null
      })
    } else {
      // 其他错误
      return Promise.reject({
        code: -1,
        message: error.message || '请求失败',
        data: null
      })
    }
  }
)

export default api

