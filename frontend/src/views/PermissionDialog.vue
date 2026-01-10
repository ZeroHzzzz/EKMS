<template>
  <el-dialog
    title="文档权限管理"
    v-model="visible"
    width="600px"
    @close="handleClose"
  >
    <div class="permission-dialog-content">
      <!-- 隐私设置开关 -->
      <div class="setting-group">
        <div class="setting-item">
          <div class="setting-label">
            <span class="title">私有文档</span>
            <span class="desc">开启后，只有被邀请的用户才能查看此文档</span>
          </div>
          <el-switch
            v-model="isPrivate"
            @change="handlePrivacyChange"
            :loading="loading"
          />
        </div>
      </div>

      <el-divider />

      <!-- 用户权限列表 -->
      <div class="permissions-list">
        <div class="list-header">
          <span>协作者 ({{ permissions.length }})</span>
          <el-button type="primary" size="small" @click="showAddUser = true">
            添加协作者
          </el-button>
        </div>

        <el-table :data="permissions" style="width: 100%" v-loading="loading">
          <el-table-column label="用户">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :size="32">{{ (row.realName || row.username || '?')[0] }}</el-avatar>
                <span>{{ row.realName || row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="权限" width="150">
            <template #default="{ row }">
              <el-select 
                v-model="row.permissionType" 
                size="small" 
                @change="(val) => handlePermissionChange(row, val)"
              >
                <el-option label="仅查看" value="VIEW" />
                <el-option label="可编辑" value="EDIT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button 
                type="text" 
                class="delete-btn"
                @click="removePermission(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 添加用户弹窗 -->
      <el-dialog
        v-model="showAddUser"
        title="添加协作者"
        width="400px"
        append-to-body
      >
        <div class="add-user-form">
          <el-select
            v-model="selectedUser"
            filterable
            remote
            placeholder="搜索用户..."
            :remote-method="searchUsers"
            :loading="searchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.realName ? `${user.realName} (${user.username})` : user.username"
              :value="user.id"
            />
          </el-select>
          <div style="margin-top: 16px">
            <el-radio-group v-model="newPermissionType">
              <el-radio label="VIEW">仅查看</el-radio>
              <el-radio label="EDIT">可编辑</el-radio>
            </el-radio-group>
          </div>
        </div>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="showAddUser = false">取消</el-button>
            <el-button type="primary" @click="confirmAddUser">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  knowledgeId: {
    type: [String, Number],
    required: true
  },
  currentIsPrivate: Boolean
})

const emit = defineEmits(['update:modelValue', 'privacy-changed'])

const visible = ref(false)
const loading = ref(false)
const permissions = ref([])
const isPrivate = ref(false)

// 添加用户相关
const showAddUser = ref(false)
const searchLoading = ref(false)
const selectedUser = ref(null)
const userOptions = ref([])
const newPermissionType = ref('VIEW')

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    isPrivate.value = props.currentIsPrivate
    fetchPermissions()
  }
})

const handleClose = () => {
  emit('update:modelValue', false)
}

// 获取权限列表
const fetchPermissions = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${props.knowledgeId}/permissions`)
    if (res.code === 200) {
      permissions.value = res.data || []
    }
  } catch (error) {
    console.error('获取权限列表失败', error)
  } finally {
    loading.value = false
  }
}

// 切换隐私状态
const handlePrivacyChange = async (val) => {
  loading.value = true
  try {
    const res = await api.put(`/knowledge/${props.knowledgeId}`, {
      isPrivate: val
    })
    if (res.code === 200) {
      ElMessage.success(val ? '已设置为私有文档' : '已设置为公开文档')
      emit('privacy-changed', val)
    } else {
      isPrivate.value = !val // Revert on failure
    }
  } catch (error) {
    isPrivate.value = !val
    console.error('设置隐私状态失败', error)
  } finally {
    loading.value = false
  }
}

// 修改权限类型
const handlePermissionChange = async (row, newType) => {
  try {
    const res = await api.post(`/knowledge/${props.knowledgeId}/permissions`, null, {
      params: {
        userId: row.userId,
        permissionType: newType
      }
    })
    if (res.code === 200) {
      ElMessage.success('权限更新成功')
    }
  } catch (error) {
    console.error('更新权限失败', error)
    fetchPermissions() // Revert data
  }
}

// 移除权限
const removePermission = async (row) => {
  try {
    await ElMessageBox.confirm('确定要移除该用户的权限吗？', '提示', { type: 'warning' })
    const res = await api.delete(`/knowledge/${props.knowledgeId}/permissions`, {
      params: { userId: row.userId }
    })
    if (res.code === 200) {
      ElMessage.success('已移除权限')
      fetchPermissions()
    }
  } catch (error) {
    if (error !== 'cancel') console.error('移除权限失败', error)
  }
}

// 搜索用户
const searchUsers = async (query) => {
  if (!query) return
  searchLoading.value = true
  try {
    const res = await api.get('/user/list', { 
        params: { username: query, pageSize: 20 } 
    })
    // 适配不同的用户列表API返回结构
    const list = res.data.list || res.data || []
    userOptions.value = list
  } catch (error) {
    console.error('搜索用户失败', error)
  } finally {
    searchLoading.value = false
  }
}

// 确认添加用户
const confirmAddUser = async () => {
  if (!selectedUser.value) {
    ElMessage.warning('请选择用户')
    return
  }
  
  try {
    const res = await api.post(`/knowledge/${props.knowledgeId}/permissions`, null, {
        params: {
            userId: selectedUser.value,
            permissionType: newPermissionType.value
        }
    })
    
    if (res.code === 200) {
      ElMessage.success('添加成功')
      showAddUser.value = false
      selectedUser.value = null
      fetchPermissions()
    }
  } catch (error) {
    console.error('添加权限失败', error)
  }
}
</script>

<style scoped>
.permission-dialog-content {
  padding: 0 10px;
}
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.setting-label {
    display: flex;
    flex-direction: column;
}
.setting-label .title {
    font-weight: 500;
    color: #1f2329;
}
.setting-label .desc {
    font-size: 12px;
    color: #8f959e;
    margin-top: 4px;
}
.list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-weight: 500;
}
.user-info {
    display: flex;
    align-items: center;
    gap: 8px;
}
.delete-btn {
    color: #f56c6c;
}
.delete-btn:hover {
    color: #f78989;
}
</style>
