<template>
  <div class="user-center">
    <div class="page-header">
      <h2>个人中心</h2>
        </div>

    <el-row :gutter="24">
        <!-- 个人信息 -->
      <el-col :span="12">
        <div class="form-section">
          <h3 class="section-title">个人信息</h3>
          <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="100px">
            <el-form-item label="用户名">
              <el-input v-model="userForm.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" />
            </el-form-item>
            <el-form-item label="部门" prop="department">
              <el-input v-model="userForm.department" />
            </el-form-item>
            <el-form-item label="角色">
              <el-input :value="getRoleText(userForm.role)" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

        <!-- 修改密码 -->
      <el-col :span="12">
        <div class="form-section">
          <h3 class="section-title">修改密码</h3>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updatePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import api from '../api'

const userStore = useUserStore()
const userFormRef = ref(null)
const passwordFormRef = ref(null)

const userForm = ref({
  id: null,
  username: '',
  realName: '',
  email: '',
  department: '',
  role: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请输入部门', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const getRoleText = (role) => {
  const map = {
    'ADMIN': '系统管理员',
    'EDITOR': '知识管理员',
    'USER': '普通用户'
  }
  return map[role] || role
}

const loadUserInfo = () => {
  const userInfo = userStore.userInfo
  if (userInfo) {
    userForm.value = {
      id: userInfo.id,
      username: userInfo.username || '',
      realName: userInfo.realName || '',
      email: userInfo.email || '',
      department: userInfo.department || '',
      role: userInfo.role || ''
    }
  }
}

const updateProfile = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await api.put(`/user/${userForm.value.id}`, {
          realName: userForm.value.realName,
          email: userForm.value.email,
          department: userForm.value.department
        })
        if (res.code === 200 && res.data) {
          ElMessage.success('修改成功')
          // 更新store中的用户信息
          userStore.userInfo = res.data
        } else {
          ElMessage.error(res.message || '修改失败')
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || error.message || '修改失败')
      }
    }
  })
}

const updatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await api.put(`/user/${userStore.userInfo.id}/password`, {
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword
        })
        if (res.code === 200) {
          ElMessage.success('密码修改成功')
          passwordForm.value = {
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
          }
        } else {
          ElMessage.error(res.message || '密码修改失败')
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || error.message || '密码修改失败')
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.user-center {
  padding: 24px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.form-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
}

.section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}
</style>
