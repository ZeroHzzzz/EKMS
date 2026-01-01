<template>
  <div class="audit-page">
    <div class="page-header">
      <h2>知识审核管理</h2>
      <el-button type="primary" @click="loadAuditList">刷新</el-button>
    </div>
    <el-card>

      <el-table :data="auditList" v-loading="loading" stripe>
        <el-table-column prop="knowledgeId" label="知识ID" width="100" />
        <el-table-column label="知识标题" min-width="200">
          <template #default="scope">
            <div v-if="scope.row.knowledge">
              <div class="knowledge-title" @click="viewKnowledge(scope.row.knowledgeId)" style="cursor: pointer; color: #409eff;">
                {{ scope.row.knowledge.title }}
              </div>
              <div class="knowledge-meta">
                <el-tag size="small" type="info">{{ scope.row.knowledge.category }}</el-tag>
                <span class="meta-text">作者：{{ scope.row.knowledge.author }}</span>
              </div>
            </div>
            <span v-else>加载中...</span>
          </template>
        </el-table-column>
        <el-table-column label="提交人" width="150">
          <template #default="scope">
            <div v-if="scope.row.submitUser">
              <div>{{ scope.row.submitUser.realName || scope.row.submitUser.username }}</div>
              <div class="meta-text">{{ scope.row.submitUser.department || '-' }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="知识状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.knowledge" :type="getKnowledgeStatusType(scope.row.knowledge.status)">
              {{ getKnowledgeStatusText(scope.row.knowledge.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审核状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="审核意见" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="info"
              @click="viewKnowledge(scope.row.knowledgeId)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              size="small"
              type="success"
              @click="showApproveDialog(scope.row)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              size="small"
              type="danger"
              @click="showRejectDialog(scope.row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="auditList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无待审核的知识" />
      </div>
    </el-card>

    <!-- 审核通过对话框 -->
    <el-dialog v-model="showApproveDialogVisible" title="审核通过" width="600px">
      <div v-if="currentAudit && currentAudit.knowledge" class="audit-dialog-content">
        <div class="knowledge-preview">
          <h4>知识信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题">{{ currentAudit.knowledge.title }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ currentAudit.knowledge.category }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentAudit.knowledge.author }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ currentAudit.knowledge.department }}</el-descriptions-item>
            <el-descriptions-item label="摘要" :span="2">{{ currentAudit.knowledge.summary || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="submit-user-info" style="margin-top: 20px;">
          <h4>提交人信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">
              {{ currentAudit.submitUser?.realName || currentAudit.submitUser?.username || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="部门">
              {{ currentAudit.submitUser?.department || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div style="margin-top: 20px;">
          <el-form-item label="审核意见">
            <el-input
              v-model="approveComment"
              type="textarea"
              :rows="4"
              placeholder="请输入审核意见（可选）"
            />
          </el-form-item>
        </div>
      </div>

      <template #footer>
        <el-button @click="showApproveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="doApprove" :loading="auditing">
          确认通过
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核驳回对话框 -->
    <el-dialog v-model="showRejectDialogVisible" title="审核驳回" width="600px">
      <div v-if="currentAudit && currentAudit.knowledge" class="audit-dialog-content">
        <div class="knowledge-preview">
          <h4>知识信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题">{{ currentAudit.knowledge.title }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ currentAudit.knowledge.category }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentAudit.knowledge.author }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ currentAudit.knowledge.department }}</el-descriptions-item>
            <el-descriptions-item label="摘要" :span="2">{{ currentAudit.knowledge.summary || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="submit-user-info" style="margin-top: 20px;">
          <h4>提交人信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">
              {{ currentAudit.submitUser?.realName || currentAudit.submitUser?.username || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="部门">
              {{ currentAudit.submitUser?.department || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div style="margin-top: 20px;">
          <el-form-item label="驳回原因" required>
            <el-input
              v-model="rejectComment"
              type="textarea"
              :rows="4"
              placeholder="请详细说明驳回原因，以便提交人了解并修改"
            />
            <div class="form-tip">驳回原因不能为空，请详细说明问题所在</div>
          </el-form-item>
        </div>
      </div>

      <template #footer>
        <el-button @click="showRejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="doReject" :loading="auditing" :disabled="!rejectComment || rejectComment.trim().length === 0">
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()
const auditList = ref([])
const loading = ref(false)
const auditing = ref(false)

// 审核对话框
const showApproveDialogVisible = ref(false)
const showRejectDialogVisible = ref(false)
const currentAudit = ref(null)
const approveComment = ref('')
const rejectComment = ref('')

const loadAuditList = async () => {
  loading.value = true
  try {
    const res = await api.get('/knowledge/audit/pending')
    if (res.code === 200 && res.data) {
      auditList.value = res.data || []
      // 加载每个审核记录对应的知识详情和提交人信息
      for (let audit of auditList.value) {
        if (audit.knowledgeId) {
          try {
            const knowledgeRes = await api.get(`/knowledge/${audit.knowledgeId}`)
            if (knowledgeRes.code === 200 && knowledgeRes.data) {
              audit.knowledge = knowledgeRes.data
            }
          } catch (error) {
            console.error(`加载知识 ${audit.knowledgeId} 失败`, error)
          }
        }
        
        // 加载提交人信息
        if (audit.submitUserId) {
          try {
            const userRes = await api.get(`/user/${audit.submitUserId}`)
            if (userRes.code === 200 && userRes.data) {
              audit.submitUser = userRes.data
            }
          } catch (error) {
            console.error(`加载提交人信息失败`, error)
          }
        }
      }
    }
  } catch (error) {
    console.error('加载审核列表失败', error)
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

const getKnowledgeStatusType = (status) => {
  const map = {
    'DRAFT': 'info',
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || ''
}

const getKnowledgeStatusText = (status) => {
  const map = {
    'DRAFT': '草稿',
    'PENDING': '待审核',
    'APPROVED': '已发布',
    'REJECTED': '已驳回'
  }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const viewKnowledge = (knowledgeId) => {
  router.push(`/knowledge/${knowledgeId}`)
}

const showApproveDialog = (row) => {
  currentAudit.value = row
  approveComment.value = ''
  showApproveDialogVisible.value = true
}

const showRejectDialog = (row) => {
  currentAudit.value = row
  rejectComment.value = ''
  showRejectDialogVisible.value = true
}

const doApprove = async () => {
  if (!currentAudit.value) return
  
  auditing.value = true
  try {
    const res = await api.post(`/knowledge/audit/${currentAudit.value.id}/approve`, null, {
      params: {
        auditorId: userStore.userInfo?.id || 1,
        comment: approveComment.value || '审核通过'
      }
    })
    
    if (res.code === 200) {
      ElMessage.success('审核通过')
      showApproveDialogVisible.value = false
      currentAudit.value = null
      approveComment.value = ''
      loadAuditList()
    } else {
      ElMessage.error(res.message || '审核失败')
    }
  } catch (error) {
    ElMessage.error('审核失败: ' + (error.response?.data?.message || error.message))
  } finally {
    auditing.value = false
  }
}

const doReject = async () => {
  if (!currentAudit.value) return
  
  if (!rejectComment.value || rejectComment.value.trim().length === 0) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  
  auditing.value = true
  try {
    const res = await api.post(`/knowledge/audit/${currentAudit.value.id}/reject`, null, {
      params: {
        auditorId: userStore.userInfo?.id || 1,
        comment: rejectComment.value
      }
    })
    
    if (res.code === 200) {
      ElMessage.success('已驳回')
      showRejectDialogVisible.value = false
      currentAudit.value = null
      rejectComment.value = ''
      loadAuditList()
    } else {
      ElMessage.error(res.message || '驳回失败')
    }
  } catch (error) {
    ElMessage.error('驳回失败: ' + (error.response?.data?.message || error.message))
  } finally {
    auditing.value = false
  }
}

onMounted(() => {
  loadAuditList()
})
</script>

<style scoped>
.audit-page {
  padding: 24px;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-card) {
  border-radius: 4px;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

.knowledge-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.knowledge-title:hover {
  text-decoration: underline;
}

.knowledge-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.meta-text {
  font-size: 12px;
  color: #909399;
}

.audit-dialog-content h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.knowledge-preview {
  margin-bottom: 20px;
}

.submit-user-info {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
