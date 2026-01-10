<template>
  <div class="org-management-page">
    <div class="page-header">
      <div class="header-left">
        <h2>组织管理</h2>
        <p class="subtitle">可视化管理与维护企业组织架构与人员分配</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showAddDeptDialog">
          <el-icon class="el-icon--left"><Plus /></el-icon>
          添加部门
        </el-button>
        <el-button type="success" @click="showAddUserDialog">
           <el-icon class="el-icon--left"><User /></el-icon>
          添加用户
        </el-button>
      </div>
    </div>

    <div class="org-content">
      <!-- 左侧：部门树 -->
      <div class="dept-panel">
        <div class="panel-header">
          <span class="panel-title">部门结构</span>
          <el-input
            v-model="deptSearchKeyword"
            placeholder="搜索部门..."
            size="small"
            clearable
            style="width: 150px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="dept-tree-container">
          <!-- 全部用户节点 -->
          <div 
            class="dept-node all-users-node"
            :class="{ 'node-selected': selectedDeptId === 'all' }"
            @click="selectDepartment('all')"
            @dragover.prevent
            @drop="handleDropToAllUsers"
          >
            <div class="dept-node-content">
              <el-icon class="dept-icon"><User /></el-icon>
              <span class="dept-name">全部用户</span>
              <el-tag size="small" type="info">{{ totalUserCount }}</el-tag>
            </div>
          </div>
          
          <!-- 系统管理员节点 -->
          <div 
            class="dept-node admin-node"
            :class="{ 'node-selected': selectedDeptId === 'admin' }"
            @click="selectDepartment('admin')"
          >
            <div class="dept-node-content">
              <el-icon class="dept-icon admin-icon"><Setting /></el-icon>
              <span class="dept-name">系统管理员</span>
              <el-tag size="small" type="danger">{{ adminUserCount }}</el-tag>
            </div>
          </div>

          <!-- 部门列表 -->
          <div 
            v-for="dept in filteredDeptList" 
            :key="dept.id"
            class="dept-node"
            :class="{ 'node-selected': selectedDeptId === dept.id }"
            @click="selectDepartment(dept.id)"
            @dragover.prevent
            @drop="handleDropToDept($event, dept)"

          >
            <div class="dept-node-content">
              <el-icon class="dept-icon"><OfficeBuilding /></el-icon>
              <span class="dept-name">{{ dept.name }}</span>
              <el-tag size="small">{{ getDeptUserCount(dept.id) }}</el-tag>
            </div>
            <div class="dept-actions">
              <el-button size="small" text type="primary" @click.stop="editDepartment(dept)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" text type="danger" @click.stop="deleteDepartment(dept)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 未分配部门 -->
          <div 
            class="dept-node unassigned-node"
            :class="{ 'node-selected': selectedDeptId === 'unassigned' }"
            @click="selectDepartment('unassigned')"
            @dragover.prevent
            @drop="handleDropToUnassigned"
          >
            <div class="dept-node-content">
              <el-icon class="dept-icon"><QuestionFilled /></el-icon>
              <span class="dept-name">未分配部门</span>
              <el-tag size="small" type="warning">{{ unassignedUserCount }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：用户列表 -->
      <div class="user-panel">
        <div class="panel-header">
          <span class="panel-title">
            {{ currentDeptName }}
            <el-tag v-if="selectedDeptId" size="small" style="margin-left: 8px">
              {{ filteredUserList.length }} 人
            </el-tag>
          </span>
          <el-input
            v-model="userSearchKeyword"
            placeholder="搜索用户..."
            size="small"
            clearable
            style="width: 200px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="user-list-container" v-loading="loadingUsers">
          <div class="user-list-tip" v-if="!selectedDeptId">
            <el-icon><InfoFilled /></el-icon>
            <span>请从左侧选择一个部门查看用户</span>
          </div>
          
          <div v-else-if="filteredUserList.length === 0" class="user-list-empty">
            <el-empty description="暂无用户" />
          </div>

          <div v-else class="user-cards">
            <div 
              v-for="user in filteredUserList" 
              :key="user.id"
              class="user-card"
              draggable="true"
              @dragstart="handleDragStart($event, user)"
              @dragend="handleDragEnd"
            >
              <div class="user-avatar">
                <el-avatar :size="48">{{ user.realName?.charAt(0) || user.username?.charAt(0) }}</el-avatar>
              </div>
              <div class="user-info">
                <div class="user-name">{{ user.realName || user.username }}</div>
                <div class="user-username">@{{ user.username }}</div>
                <div class="user-meta">
                  <el-tag :type="getRoleType(user.role)" size="small">{{ getRoleText(user.role) }}</el-tag>
                  <span v-if="user.email" class="user-email">{{ user.email }}</span>
                </div>
              </div>
              <div class="user-actions">
                <el-button size="small" text type="primary" @click="editUser(user)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button size="small" text type="danger" @click="deleteUser(user)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 拖拽提示 -->
    <div v-if="draggingUser" class="drag-indicator">
      <el-icon><Rank /></el-icon>
      拖拽 {{ draggingUser.realName || draggingUser.username }} 到目标部门
    </div>

    <!-- 添加/编辑部门对话框 -->
    <el-dialog
      v-model="deptDialogVisible"
      :title="editingDept ? '编辑部门' : '添加部门'"
      width="500px"
    >
      <el-form :model="deptForm" :rules="deptRules" ref="deptFormRef" label-width="100px" @submit.prevent="saveDepartment">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门描述" prop="description">
          <el-input
            v-model="deptForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入部门描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDepartment">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="editingUser ? '编辑用户' : '添加用户'"
      width="500px"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="80px" @submit.prevent="saveUser">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!editingUser" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%" @change="handleRoleChange">
            <el-option label="普通用户" value="USER" />
            <el-option label="知识管理员" value="EDITOR" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="departmentId" v-if="userForm.role !== 'ADMIN'">
          <el-select v-model="userForm.departmentId" placeholder="请选择部门" style="width: 100%">
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
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, User, Search, OfficeBuilding, Edit, Delete, 
  Setting, QuestionFilled, InfoFilled, Rank 
} from '@element-plus/icons-vue'
import api from '../api'

// 部门相关
const departmentList = ref([])
const loadingDepts = ref(false)
const selectedDeptId = ref('all')
const deptSearchKeyword = ref('')
const deptDialogVisible = ref(false)
const editingDept = ref(null)
const deptFormRef = ref(null)
const deptForm = ref({
  name: '',
  description: ''
})
const deptRules = {
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 2, max: 50, message: '部门名称长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

// 用户相关
const userList = ref([])
const loadingUsers = ref(false)
const userSearchKeyword = ref('')
const userDialogVisible = ref(false)
const editingUser = ref(null)
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

// 用户验证：部门动态验证
const validateDepartment = (rule, value, callback) => {
  if (userForm.value.role === 'ADMIN') {
    callback()
  } else if (!value) {
    callback(new Error('请选择部门'))
  } else {
    callback()
  }
}

const userRules = {
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
    { validator: validateDepartment, trigger: 'change' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 拖拽相关
const draggingUser = ref(null)

// 计算属性
const filteredDeptList = computed(() => {
  if (!deptSearchKeyword.value.trim()) {
    return departmentList.value
  }
  const keyword = deptSearchKeyword.value.toLowerCase()
  return departmentList.value.filter(dept => 
    dept.name.toLowerCase().includes(keyword)
  )
})

const filteredUserList = computed(() => {
  let users = []
  
  if (selectedDeptId.value === 'all') {
    users = userList.value
  } else if (selectedDeptId.value === 'admin') {
    users = userList.value.filter(u => u.role === 'ADMIN')
  } else if (selectedDeptId.value === 'unassigned') {
    users = userList.value.filter(u => !u.departmentId && u.role !== 'ADMIN')
  } else {
    users = userList.value.filter(u => u.departmentId === selectedDeptId.value)
  }
  
  if (!userSearchKeyword.value.trim()) {
    return users
  }
  
  const keyword = userSearchKeyword.value.toLowerCase()
  return users.filter(user => 
    (user.username && user.username.toLowerCase().includes(keyword)) ||
    (user.realName && user.realName.toLowerCase().includes(keyword)) ||
    (user.email && user.email.toLowerCase().includes(keyword))
  )
})

const currentDeptName = computed(() => {
  if (selectedDeptId.value === 'all') return '全部用户'
  if (selectedDeptId.value === 'admin') return '系统管理员'
  if (selectedDeptId.value === 'unassigned') return '未分配部门'
  const dept = departmentList.value.find(d => d.id === selectedDeptId.value)
  return dept ? dept.name : '用户列表'
})

const totalUserCount = computed(() => userList.value.length)
const adminUserCount = computed(() => userList.value.filter(u => u.role === 'ADMIN').length)
const unassignedUserCount = computed(() => 
  userList.value.filter(u => !u.departmentId && u.role !== 'ADMIN').length
)

const getDeptUserCount = (deptId) => {
  return userList.value.filter(u => u.departmentId === deptId).length
}

// 加载数据
const loadDepartments = async () => {
  loadingDepts.value = true
  try {
    const res = await api.get('/department')
    if (res.code === 200 && res.data) {
      departmentList.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载部门列表失败')
  } finally {
    loadingDepts.value = false
  }
}

const loadUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await api.get('/user/list')
    if (res.code === 200 && res.data) {
      userList.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

// 部门操作
const selectDepartment = (deptId) => {
  selectedDeptId.value = deptId
}

const showAddDeptDialog = () => {
  editingDept.value = null
  deptForm.value = { name: '', description: '' }
  deptDialogVisible.value = true
}

const editDepartment = (dept) => {
  editingDept.value = dept
  deptForm.value = {
    name: dept.name,
    description: dept.description || ''
  }
  deptDialogVisible.value = true
}

const saveDepartment = async () => {
  if (!deptFormRef.value) return
  
  await deptFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (editingDept.value) {
          await api.put(`/department/${editingDept.value.id}`, deptForm.value)
          ElMessage.success('更新部门成功')
        } else {
          await api.post('/department', deptForm.value)
          ElMessage.success('添加部门成功')
        }
        deptDialogVisible.value = false
        loadDepartments()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const deleteDepartment = async (dept) => {
  const userCount = getDeptUserCount(dept.id)
  const confirmMsg = userCount > 0 
    ? `该部门下有 ${userCount} 个用户，删除后这些用户将变为未分配状态。确定要删除吗？`
    : '确定要删除该部门吗？'
  
  try {
    await ElMessageBox.confirm(confirmMsg, '提示', { type: 'warning' })
    await api.delete(`/department/${dept.id}`)
    ElMessage.success('删除部门成功')
    if (selectedDeptId.value === dept.id) {
      selectedDeptId.value = 'all'
    }
    loadDepartments()
    loadUsers() // 刷新用户列表
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除部门失败')
    }
  }
}

// 用户操作
const showAddUserDialog = () => {
  editingUser.value = null
  userForm.value = {
    id: null,
    username: '',
    password: '',
    realName: '',
    email: '',
    departmentId: selectedDeptId.value && typeof selectedDeptId.value === 'number' 
      ? selectedDeptId.value 
      : null,
    role: 'USER'
  }
  userDialogVisible.value = true
}

const editUser = (user) => {
  editingUser.value = user
  userForm.value = {
    id: user.id,
    username: user.username,
    password: '',
    realName: user.realName,
    email: user.email,
    departmentId: user.departmentId,
    role: user.role
  }
  userDialogVisible.value = true
}

const handleRoleChange = (role) => {
  if (role === 'ADMIN') {
    userForm.value.departmentId = null
  }
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const data = {
          ...userForm.value,
          departmentId: userForm.value.role === 'ADMIN' ? null : userForm.value.departmentId
        }
        
        if (editingUser.value) {
          await api.put(`/user/${userForm.value.id}`, data)
          ElMessage.success('更新用户成功')
        } else {
          await api.post('/user', data)
          ElMessage.success('添加用户成功')
        }
        userDialogVisible.value = false
        loadUsers()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${user.realName || user.username} 吗？`, '提示', {
      type: 'warning'
    })
    await api.delete(`/user/${user.id}`)
    ElMessage.success('删除用户成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除用户失败')
    }
  }
}

// 拖拽操作
const handleDragStart = (event, user) => {
  draggingUser.value = user
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', JSON.stringify(user))
}

const handleDragEnd = () => {
  draggingUser.value = null
}

const handleDropToDept = async (event, dept) => {
  event.preventDefault()
  if (!draggingUser.value) return
  
  const user = draggingUser.value
  
  // 系统管理员不能分配到部门
  if (user.role === 'ADMIN') {
    ElMessage.warning('系统管理员不能分配到部门')
    return
  }
  
  // 已在该部门
  if (user.departmentId === dept.id) {
    ElMessage.info('用户已在该部门')
    return
  }
  
  try {
    await api.put(`/user/${user.id}`, { departmentId: dept.id })
    ElMessage.success(`已将 ${user.realName || user.username} 移动到 ${dept.name}`)
    loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '移动失败')
  }
}

const handleDropToAllUsers = (event) => {
  event.preventDefault()
  // 全部用户只是视图，不需要处理
}

const handleDropToUnassigned = async (event) => {
  event.preventDefault()
  if (!draggingUser.value) return
  
  const user = draggingUser.value
  
  // 系统管理员不能分配到部门
  if (user.role === 'ADMIN') {
    ElMessage.warning('系统管理员无需分配部门')
    return
  }
  
  // 已经未分配
  if (!user.departmentId) {
    ElMessage.info('用户已经是未分配状态')
    return
  }
  
  try {
    await api.put(`/user/${user.id}`, { departmentId: null })
    ElMessage.success(`已将 ${user.realName || user.username} 移出部门`)
    loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 辅助函数
const getRoleType = (role) => {
  const map = { 'ADMIN': 'danger', 'EDITOR': 'warning', 'USER': 'success' }
  return map[role] || ''
}

const getRoleText = (role) => {
  const map = { 'ADMIN': '系统管理员', 'EDITOR': '知识管理员', 'USER': '普通用户' }
  return map[role] || role
}

onMounted(() => {
  loadDepartments()
  loadUsers()
})
</script>

<style scoped>
.org-management-page {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
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

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.org-content {
  flex: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
}

/* 左侧部门面板 */
.dept-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 右侧用户面板 */
.user-panel {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #fafbfc, #fff);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

/* 部门树 */
.dept-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.dept-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.dept-node:hover {
  background: #f5f7fa;
}

.dept-node.node-selected {
  background: linear-gradient(to right, #ecf5ff, #f5f7fa);
  border-color: #409EFF;
}

.dept-node-content {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.dept-icon {
  font-size: 20px;
  color: #409EFF;
}

.admin-icon {
  color: #f56c6c;
}

.dept-name {
  font-size: 14px;
  color: #303133;
  flex: 1;
}

.dept-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.dept-node:hover .dept-actions {
  opacity: 1;
}

.all-users-node {
  background: linear-gradient(to right, #f0f9eb, #fafbfc);
}

.admin-node {
  background: linear-gradient(to right, #fef0f0, #fafbfc);
}

.unassigned-node {
  background: linear-gradient(to right, #fdf6ec, #fafbfc);
}

/* 用户列表 */
.user-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.user-list-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 200px;
  color: #909399;
  font-size: 14px;
}

.user-list-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.user-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: grab;
  transition: all 0.3s;
}

.user-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.user-card:active {
  cursor: grabbing;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.user-username {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.user-email {
  font-size: 12px;
  color: #606266;
}

.user-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.user-card:hover .user-actions {
  opacity: 1;
}

/* 拖拽提示 */
.drag-indicator {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #409EFF;
  color: #fff;
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  z-index: 1000;
}
</style>

