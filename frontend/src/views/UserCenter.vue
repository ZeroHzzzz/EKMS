<template>
  <div class="user-center">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <el-card class="user-card">
      <el-tabs v-model="activeTab" class="user-tabs">
        <!-- 个人设置 Tab -->
        <el-tab-pane label="个人设置" name="settings">
          <template #label>
            <span class="custom-tabs-label">
              <el-icon><Setting /></el-icon>
              <span>个人设置</span>
            </span>
          </template>
          
          <el-row :gutter="40">
            <!-- 个人信息 -->
            <el-col :span="12">
              <div class="form-section">
                <h3 class="section-title">基本信息</h3>
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
                    <el-tag :type="getRoleTagType(userForm.role)">{{ getRoleText(userForm.role) }}</el-tag>
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
                <h3 class="section-title">安全设置</h3>
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
        </el-tab-pane>

        <!-- 我的收藏 Tab -->
        <el-tab-pane label="我的收藏" name="collections">
          <template #label>
            <span class="custom-tabs-label">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </span>
          </template>
          
          <el-table 
            :data="collections" 
            v-loading="collectionsLoading" 
            stripe
            @row-click="handleRowClick"
            class="data-table"
          >
            <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="author" label="作者" width="120" />
            <el-table-column prop="clickCount" label="点击量" width="100" align="right" />
            <el-table-column prop="collectCount" label="收藏量" width="100" align="right" />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button 
                  size="small" 
                  type="danger" 
                  link
                  @click.stop="cancelCollect(scope.row)"
                >
                  取消收藏
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无收藏的知识" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- 我的上传 Tab -->
        <el-tab-pane label="我的上传" name="uploads">
          <template #label>
            <span class="custom-tabs-label">
              <el-icon><Upload /></el-icon>
              <span>我的上传</span>
            </span>
          </template>
          
          <el-table 
            :data="myUploads" 
            v-loading="uploadsLoading" 
            stripe
            @row-click="handleRowClick"
            class="data-table"
          >
            <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip />
            <el-table-column prop="version" label="版本" width="80" align="center">
               <template #default="scope">
                 <el-tag size="small">v{{ scope.row.version }}</el-tag>
               </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" size="small">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="上传时间" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center">
              <template #default="scope">
                <el-button size="small" link type="primary" @click.stop="viewKnowledge(scope.row)">查看</el-button>
                <el-button size="small" link type="warning" @click.stop="editKnowledge(scope.row)">编辑</el-button>
                <el-button size="small" link type="danger" @click.stop="deleteKnowledge(scope.row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无上传的知识" />
            </template>
          </el-table>
          
          <div class="pagination-wrapper" v-if="uploadsTotal > 0">
             <el-pagination
               v-model:current-page="uploadsPageNum"
               v-model:page-size="uploadsPageSize"
               :total="uploadsTotal"
               layout="total, prev, pager, next"
               @current-change="loadMyUploads"
             />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Star, Upload } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const activeTab = ref('settings')

// --- 个人设置逻辑 ---
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

const getRoleTagType = (role) => {
    const map = {
    'ADMIN': 'danger',
    'EDITOR': 'warning',
    'USER': 'info'
  }
  return map[role] || ''
}

const loadUserInfo = () => {
  const info = userStore.userInfo
  if (info) {
    userForm.value = {
      id: info.id,
      username: info.username || '',
      realName: info.realName || '',
      email: info.email || '',
      department: info.department || '',
      role: info.role || ''
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

// --- 我的收藏逻辑 ---
const collections = ref([])
const collectionsLoading = ref(false)

const loadCollections = async () => {
  if (!userStore.userInfo || !userStore.userInfo.id) return
  
  collectionsLoading.value = true
  try {
    const res = await api.get('/knowledge/my/collections', {
      params: { userId: userStore.userInfo.id }
    })
    collections.value = res.data || []
  } catch (error) {
    console.error('加载收藏列表失败', error)
    ElMessage.error('加载收藏列表失败')
  } finally {
    collectionsLoading.value = false
  }
}

const cancelCollect = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏吗？', '提示', {
      type: 'warning'
    })
    
    const res = await api.delete(`/knowledge/${row.id}/collect`, {
      params: { userId: userStore.userInfo.id }
    })
    
    if (res.code === 200) {
      ElMessage.success('取消收藏成功')
      loadCollections()
    } else {
      ElMessage.error(res.message || '取消收藏失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消收藏失败')
    }
  }
}

// --- 我的上传逻辑 ---
const myUploads = ref([])
const uploadsLoading = ref(false)
const uploadsPageNum = ref(1)
const uploadsPageSize = ref(10)
const uploadsTotal = ref(0)

const loadMyUploads = async () => {
  if (!userStore.userInfo || !userStore.userInfo.realName) return
  
  uploadsLoading.value = true
  try {
    // 复用知识列表接口，通过 author 筛选
    const res = await api.get('/knowledge/list', {
      params: { 
        pageNum: uploadsPageNum.value,
        pageSize: uploadsPageSize.value,
        author: userStore.userInfo.realName
      }
    })
    
    // sort logic aligned with KnowledgeManagement: PENDING first, then by createTime
    let list = res.data || []
    list.sort((a, b) => {
        const statusOrder = { 'PENDING': 0, 'APPROVED': 1, 'REJECTED': 2 }
        const statusA = statusOrder[a.status] ?? 3
        const statusB = statusOrder[b.status] ?? 3
        if (statusA !== statusB) return statusA - statusB
        
        const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
        const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
        return timeB - timeA
    })
    
    myUploads.value = list
    // 注意：当前 list 接口返回的是 List<DTO> 而不是 PageResult，所以这里 total 只能暂时用 length
    // 如果后端支持分页返回 total，这里应该更新 total
    // 假设后端 filter 后返回全量数据或者我们需要自行处理分页显示
    // 观察 KnowledgeManagement.vue 里的实现，看来 list 接口在带筛选时行为可能直接返回列表
    uploadsTotal.value = list.length 
  } catch (error) {
    console.error('加载上传列表失败', error)
    ElMessage.error('加载上传列表失败')
  } finally {
    uploadsLoading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待审核',
    'APPROVED': '已发布',
    'REJECTED': '已驳回'
  }
  return statusMap[status] || status
}

const viewKnowledge = (row) => {
    router.push(`/knowledge/${row.id}`)
}

const editKnowledge = (row) => {
    router.push(`/knowledge/${row.id}?edit=true`)
}

const deleteKnowledge = async (row) => {
   try {
    await ElMessageBox.confirm('确定要删除该知识吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await api.delete(`/knowledge/${row.id}`)
    
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadMyUploads()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
        ElMessage.error(error.message || '删除失败')
    }
  }
}

// --- 通用逻辑 ---

const handleRowClick = (row) => {
  viewKnowledge(row)
}

// 监听 Tab 切换，按需加载数据
watch(activeTab, (val) => {
  if (val === 'collections') {
    loadCollections()
  } else if (val === 'uploads') {
    loadMyUploads()
  }
})

onMounted(() => {
  loadUserInfo()
  // 根据 URL 参数或其他逻辑预选 Tab，这里默认 settings
  // 如果需要支持从外部跳转到特定 Tab，可以读取 router query
  if (route.query.tab) {
      activeTab.value = route.query.tab
  }
  
  if (activeTab.value === 'collections') loadCollections()
  if (activeTab.value === 'uploads') loadMyUploads()
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

.user-card {
  min-height: 600px;
}

.user-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 10px;
}

.user-tabs :deep(.el-tabs__item) {
  height: 50px;
  line-height: 50px;
  font-size: 16px;
}

.custom-tabs-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-section {
  padding: 10px 0;
}

.section-title {
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  padding-left: 10px;
  border-left: 4px solid #409eff;
}

.data-table {
  margin-top: 10px;
}

.data-table :deep(.el-table__row) {
  cursor: pointer;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>


