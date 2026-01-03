<template>
  <div class="department-management-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" @click="showAddDialog">添加部门</el-button>
        </div>
      </template>

      <el-table :data="departmentList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="部门名称" width="200" />
        <el-table-column prop="description" label="部门描述" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="editDepartment(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteDepartment(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑部门对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="departmentForm" :rules="rules" ref="departmentFormRef" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="departmentForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门描述" prop="description">
          <el-input
            v-model="departmentForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入部门描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDepartment">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const departmentList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const departmentFormRef = ref(null)

const departmentForm = ref({
  id: null,
  name: '',
  description: ''
})

const dialogTitle = computed(() => isEdit.value ? '编辑部门' : '添加部门')

const rules = {
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 2, max: 50, message: '部门名称长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const loadDepartmentList = async () => {
  loading.value = true
  try {
    const res = await api.get('/department')
    if (res.code === 200 && res.data) {
      departmentList.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载部门列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  departmentForm.value = {
    id: null,
    name: '',
    description: ''
  }
  dialogVisible.value = true
}

const editDepartment = (row) => {
  isEdit.value = true
  departmentForm.value = {
    id: row.id,
    name: row.name,
    description: row.description || ''
  }
  dialogVisible.value = true
}

const saveDepartment = async () => {
  if (!departmentFormRef.value) return
  
  await departmentFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          // 编辑部门
          await api.put(`/department/${departmentForm.value.id}`, {
            name: departmentForm.value.name,
            description: departmentForm.value.description
          })
          ElMessage.success('更新部门成功')
        } else {
          // 添加部门
          await api.post('/department', {
            name: departmentForm.value.name,
            description: departmentForm.value.description
          })
          ElMessage.success('添加部门成功')
        }
        dialogVisible.value = false
        loadDepartmentList()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const deleteDepartment = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该部门吗？删除后该部门下的用户将无法正常使用。', '提示', {
      type: 'warning'
    })
    await api.delete(`/department/${row.id}`)
    ElMessage.success('删除部门成功')
    loadDepartmentList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除部门失败')
    }
  }
}

onMounted(() => {
  loadDepartmentList()
})
</script>

<style scoped>
.department-management-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

