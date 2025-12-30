<template>
  <div class="audit-page">
    <el-table :data="auditList" v-loading="loading">
      <el-table-column prop="knowledgeId" label="知识ID" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="180" />
      <el-table-column prop="comment" label="审核意见" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'PENDING'"
            size="small"
            type="success"
            @click="approve(scope.row)"
          >
            通过
          </el-button>
          <el-button
            v-if="scope.row.status === 'PENDING'"
            size="small"
            type="danger"
            @click="reject(scope.row)"
          >
            驳回
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const auditList = ref([])
const loading = ref(false)

const loadAuditList = async () => {
  loading.value = true
  try {
    // 这里应该调用实际的审核列表API
    // const res = await api.get('/audit/list')
    // auditList.value = res.data
    auditList.value = []
  } catch (error) {
    ElMessage.error('加载审核列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return map[status] || status
}

const approve = async (row) => {
  try {
    await ElMessageBox.prompt('请输入审核意见', '审核通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await api.post(`/knowledge/audit/${row.id}/approve`, null, {
      params: {
        auditorId: 1, // 应该从用户信息获取
        comment: '通过'
      }
    })
    
    ElMessage.success('审核通过')
    loadAuditList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const reject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '审核驳回', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await api.post(`/knowledge/audit/${row.id}/reject`, null, {
      params: {
        auditorId: 1,
        comment: value
      }
    })
    
    ElMessage.success('已驳回')
    loadAuditList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadAuditList()
})
</script>

<style scoped>
.audit-page {
  padding: 20px;
}
</style>

