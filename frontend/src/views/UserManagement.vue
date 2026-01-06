<template>
  <div class="user-management-page">
    <div class="page-header">
      <div class="header-left">
        <h2>用户管理</h2>
        <p class="subtitle">管理系统用户、角色及权限分配</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showAddDialog">
          <el-icon class="el-icon--left"><Plus /></el-icon> 添加用户
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150">
          <template #default="{ row }">
            <span class="username-text">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="department" label="部门" width="150">
          <template #default="scope">
            {{ scope.row.role === 'ADMIN' ? '系统' : (scope.row.department || '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)" effect="plain">
              {{ getRoleText(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="editUser(scope.row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button link type="danger" size="small" @click="deleteUser(scope.row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      class="user-dialog"
    >
      <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%" @change="handleRoleChange">
            <el-option label="普通用户" value="USER" />
            <el-option label="知识管理员" value="EDITOR" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="departmentId" v-if="userForm.role !== 'ADMIN'">
          <el-select v-model="userForm.departmentId" placeholder="请选择部门" style="width: 100%" :loading="loadingDepartments">
            <el-option
              v-for="dept in departmentList"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else>
          <el-alert title="系统管理员拥有所有权限，无需归属特定部门" type="info" :closable="false" show-icon />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import api from '../api'

const userList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref(null)

const userForm = ref({
  id: null,
  username: '',
  password: '',
  realName: '',
  email: '',
  departmentId: null,
  role: 'USER'
})

const departmentList = ref([])
const loadingDepartments = ref(false)

const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '添加用户')

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  departmentId: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

const getRoleType = (role) => {
  const map = {
    'ADMIN': 'danger',
    'EDITOR': 'warning',
    'USER': 'success'
  }
  return map[role] || ''
}

const getRoleText = (role) => {
  const map = {
    'ADMIN': '系统管理员',
    'EDITOR': '知识管理员',
    'USER': '普通用户'
  }
  return map[role] || role
}

const loadUserList = async () => {
  loading.value = true
  try {
    // 这里应该调用实际的用户列表API
    // const res = await api.get('/user/list', { params: { pageNum, pageSize } })
    // userList.value = res.data.list
    // total.value = res.data.total
    
    // 暂时使用模拟数据
    userList.value = []
    total.value = 0
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  userForm.value = {
    id: null,
    username: '',
    password: '',
    realName: '',
    email: '',
    departmentId: null,
    role: 'USER'
  }
  dialogVisible.value = true
  loadDepartments()
}

const editUser = (row) => {
  isEdit.value = true
  userForm.value = {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    email: row.email,
    departmentId: row.departmentId,
    role: row.role
  }
  dialogVisible.value = true
  loadDepartments()
}

const handleRoleChange = (role) => {
  // 如果选择系统管理员，清空部门
  if (role === 'ADMIN') {
    userForm.value.departmentId = null
  }
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

const saveUser = async () => {
  if (!userFormRef.value) return
  
  // 系统管理员不需要验证部门
  const rulesToValidate = { ...rules }
  if (userForm.value.role === 'ADMIN') {
    delete rulesToValidate.departmentId
  }
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          // 编辑用户
          const updateData = {
            realName: userForm.value.realName,
            email: userForm.value.email,
            departmentId: userForm.value.role === 'ADMIN' ? null : userForm.value.departmentId,
            role: userForm.value.role
          }
          await api.put(`/user/${userForm.value.id}`, updateData)
          ElMessage.success('更新用户成功')
        } else {
          // 添加用户
          const createData = {
            username: userForm.value.username,
            password: userForm.value.password,
            realName: userForm.value.realName,
            email: userForm.value.email,
            departmentId: userForm.value.role === 'ADMIN' ? null : userForm.value.departmentId,
            role: userForm.value.role
          }
          await api.post('/user', createData)
          ElMessage.success('添加用户成功')
        }
        dialogVisible.value = false
        loadUserList()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      type: 'warning'
    })
    // await api.delete(`/user/${row.id}`)
    ElMessage.success('删除用户成功')
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除用户失败')
    }
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadUserList()
}

const handlePageChange = (page) => {
  pageNum.value = page
  loadUserList()
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-management-page {
  padding: 24px;
  background-color: #f6f8fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0;
}

.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.table-card {
  border: none;
  border-radius: 8px;
}

.pagination-wrapper {
  padding: 20px 0 0;
  display: flex;
  justify-content: flex-end;
}

.username-text {
  font-weight: 500;
  color: #303133;
}

:deep(.el-table) {
  --el-table-header-bg-color: #f8f9fa;
  --el-table-header-text-color: #606266;
}
</style>

