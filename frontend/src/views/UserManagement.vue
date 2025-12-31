<template>
  <div class="user-management-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="showAddDialog">添加用户</el-button>
        </div>
      </template>

      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="department" label="部门" width="150" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)">
              {{ getRoleText(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteUser(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="userForm" :rules="rules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" />
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
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="知识管理员" value="EDITOR" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
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
  department: '',
  role: 'USER'
})

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
  department: [
    { required: true, message: '请输入部门', trigger: 'blur' }
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
    department: '',
    role: 'USER'
  }
  dialogVisible.value = true
}

const editUser = (row) => {
  isEdit.value = true
  userForm.value = {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    email: row.email,
    department: row.department,
    role: row.role
  }
  dialogVisible.value = true
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          // 编辑用户
          // await api.put(`/user/${userForm.value.id}`, userForm.value)
          ElMessage.success('更新用户成功')
        } else {
          // 添加用户
          // await api.post('/user', userForm.value)
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
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

