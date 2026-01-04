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
        <el-form-item label="用户角色" prop="role">
          <el-select v-model="registerForm.role" placeholder="请选择用户角色" style="width: 100%" @change="handleRoleChange">
            <el-option label="普通用户" value="USER" />
            <el-option label="知识管理员" value="EDITOR" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="departmentId" v-if="registerForm.role !== 'ADMIN'">
          <el-select v-model="registerForm.departmentId" placeholder="请选择部门" style="width: 100%" :loading="loadingDepartments">
            <el-option
              v-for="dept in departmentList"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else>
          <el-text type="info">系统管理员无需选择部门</el-text>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  departmentId: null,
  role: 'USER'
})

const registerFormRef = ref(null)
const departmentList = ref([])
const loadingDepartments = ref(false)

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

// 部门验证：系统管理员不需要部门，其他角色必须选择
const validateDepartment = (rule, value, callback) => {
  if (registerForm.value.role === 'ADMIN') {
    callback() // 系统管理员不需要部门
  } else if (!value) {
    callback(new Error('请选择部门'))
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
  departmentId: [
    { validator: validateDepartment, trigger: 'change' }
  ],
  role: [
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ]
}

const handleRoleChange = (role) => {
  // 如果选择系统管理员，清空部门
  if (role === 'ADMIN') {
    registerForm.value.departmentId = null
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const result = await userStore.register(
          registerForm.value.username,
          registerForm.value.password,
          registerForm.value.realName,
          registerForm.value.email,
          registerForm.value.role === 'ADMIN' ? null : registerForm.value.departmentId,
          registerForm.value.role
        )
        if (result.success) {
          ElMessage.success('注册成功')
          router.push('/login')
        } else {
          // 注册失败，显示错误消息
          ElMessage.error(result.message || '注册失败')
        }
      } catch (error) {
        // 捕获意外错误
        const errorMsg = error.message || '注册失败，请稍后重试'
        ElMessage.error(errorMsg)
        console.error('注册错误:', error)
      }
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}

const loadDepartments = async () => {
  loadingDepartments.value = true
  try {
    const res = await api.get('/department')
    if (res.code === 200 && res.data) {
      departmentList.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载部门列表失败')
  } finally {
    loadingDepartments.value = false
  }
}

onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background-color: #f5f7fa;
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
