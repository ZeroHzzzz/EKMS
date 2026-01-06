<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <div class="logo-wrapper">
           <el-icon :size="40" color="#409eff"><Reading /></el-icon>
        </div>
        <h2>此间知识库</h2>
        <p class="subtitle">企业级知识管理方案</p>
      </div>
      
      <el-form 
        :model="loginForm" 
        :rules="rules" 
        ref="loginFormRef"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleLogin" 
            :loading="loading"
            size="large"
            class="login-button"
          >
            登录
          </el-button>
        </el-form-item>
        
        <div class="login-footer">
          <span class="footer-text">还没有账号？</span>
          <el-link type="primary" @click="goToRegister" :underline="false">
            立即注册
          </el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Reading } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({
  username: '',
  password: ''
})

const loginFormRef = ref(null)
const loading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await userStore.login(loginForm.value.username, loginForm.value.password)
        if (result.success) {
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          // 登录失败，显示错误消息
          ElMessage.error(result.message || '登录失败')
        }
      } catch (error) {
        // 捕获意外错误
        const errorMsg = error.message || '登录失败，请稍后重试'
        ElMessage.error(errorMsg)
        console.error('登录错误:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
  padding: 20px 10px;
  border: none;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05); /* Softer shadow */
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-wrapper {
  margin-bottom: 16px;
  display: inline-block;
  padding: 12px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 12px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  letter-spacing: 1px;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #909399;
}

.login-form {
  padding: 0 10px;
}

.login-button {
  width: 100%;
  font-weight: 500;
  letter-spacing: 1px;
}

.login-footer {
  text-align: center;
  margin-top: 10px;
}

.footer-text {
  font-size: 14px;
  color: #606266;
  margin-right: 4px;
}
</style>
