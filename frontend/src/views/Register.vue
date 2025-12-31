<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>用户注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="registerForm.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="用户角色" prop="role">
          <el-select v-model="registerForm.role" placeholder="请选择用户角色" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="知识管理员" value="EDITOR" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%">注册</el-button>
        </el-form-item>
        <el-form-item>
          <div style="text-align: center; width: 100%">
            <el-link type="primary" @click="goToLogin">已有账号？去登录</el-link>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  department: '',
  role: 'USER'
})

const registerFormRef = ref(null)

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (value && value !== '') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(value)) {
      callback(new Error('请输入有效的邮箱地址'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请输入部门', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await userStore.register(
          registerForm.value.username,
          registerForm.value.password,
          registerForm.value.realName,
          registerForm.value.email,
          registerForm.value.department,
          registerForm.value.role
        )
        ElMessage.success('注册成功')
        router.push('/login')
      } catch (error) {
        const errorMsg = error.message || '注册失败'
        ElMessage.error(errorMsg)
        console.error('注册错误:', error)
      }
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 500px;
  padding: 40px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
</style>

