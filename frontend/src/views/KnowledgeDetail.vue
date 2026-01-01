<template>
  <div class="knowledge-detail" v-loading="loading">
    <div v-if="knowledge">
      <div class="detail-header">
        <h1 v-if="!isEditMode">{{ knowledge.title }}</h1>
        <div v-else style="flex: 1; margin-right: 20px">
          <el-input v-model="editForm.title" placeholder="请输入标题" />
        </div>
        <div>
          <el-button v-if="isEditMode" @click="cancelEdit">取消</el-button>
          <el-button v-if="isEditMode" type="primary" @click="saveEdit">保存</el-button>
          <el-button 
            v-if="!isEditMode" 
            :type="isCollected ? 'success' : 'primary'"
            :icon="isCollected ? 'StarFilled' : 'Star'"
            @click="handleCollect"
          >
            {{ isCollected ? '已收藏' : '收藏' }}
          </el-button>
          <el-button v-if="!isEditMode" type="primary" @click="showVersionDialog = true">版本历史</el-button>
        </div>
      </div>
      <!-- 编辑模式 -->
      <div v-if="isEditMode" class="edit-form">
        <el-form :model="editForm" label-width="100px">
          <el-form-item label="分类">
            <el-input v-model="editForm.category" placeholder="请输入分类" />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="editForm.summary" type="textarea" :rows="3" placeholder="请输入摘要" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="editForm.keywords" placeholder="请输入关键词，多个用逗号分隔" />
          </el-form-item>
          <el-form-item label="变更说明">
            <el-input v-model="editForm.changeDescription" type="textarea" :rows="2" placeholder="请输入本次变更说明" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="editForm.content" type="textarea" :rows="15" placeholder="请输入内容" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 查看模式 -->
      <div v-else class="detail-layout">
        <div class="main-content">
          <div class="meta-info">
            <div class="meta-row">
              <span><strong>版本：</strong>v{{ knowledge.version }}</span>
              <span><strong>作者：</strong>{{ knowledge.author }}</span>
              <span><strong>分类：</strong>{{ knowledge.category }}</span>
            </div>
            <div class="meta-row">
              <span><strong>点击量：</strong>{{ knowledge.clickCount || 0 }}</span>
              <span><strong>收藏量：</strong>{{ knowledge.collectCount || 0 }}</span>
              <el-tag :type="getStatusType(knowledge.status)" size="default">
                {{ getStatusText(knowledge.status) }}
              </el-tag>
            </div>
          </div>
          
          <div class="content">
            <!-- 如果有文件，显示文件信息和下载链接 -->
            <div v-if="knowledge.fileId && fileInfo" class="file-info">
              <el-card>
                <div class="file-info-content">
                  <div class="file-info-left">
                    <h3>附件文件</h3>
                    <p>文件名：{{ fileInfo.fileName }}</p>
                    <p>文件类型：{{ fileInfo.fileType }}</p>
                    <p>文件大小：{{ formatFileSize(fileInfo.fileSize) }}</p>
                  </div>
                  <div class="file-info-right">
                    <el-button type="primary" @click="downloadFile">下载文件</el-button>
                    <el-button v-if="canPreview(fileInfo.fileType)" @click="previewFile">预览</el-button>
                  </div>
                </div>
              </el-card>
            </div>
            <!-- 显示文本内容 -->
            <div v-if="knowledge.content" class="text-content">
              <pre v-if="isTextContent(knowledge.content)">{{ knowledge.content }}</pre>
              <p v-else>{{ knowledge.content }}</p>
            </div>
            <div v-else-if="!knowledge.fileId" class="empty-content">
              <p style="color: #999;">暂无内容</p>
            </div>
          </div>
          
          <div class="related-knowledge" v-if="relatedKnowledge.length > 0">
            <h3>相关知识</h3>
            <el-row :gutter="20">
              <el-col :span="8" v-for="item in relatedKnowledge" :key="item.id">
                <el-card @click="viewDetail(item.id)" style="cursor: pointer">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.summary }}</p>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
        
        <!-- 右侧信息栏 -->
        <div class="sidebar-info">
          <el-card class="info-card">
            <template #header>
              <span>审核状态</span>
            </template>
            <div v-loading="auditLoading">
              <div v-if="auditHistory.length > 0">
                <div v-for="(item, index) in auditHistory" :key="index" class="audit-item">
                  <div class="audit-header">
                    <el-tag :type="getAuditStatusType(item.action)" size="small">
                      {{ getAuditActionText(item.action) }}
                    </el-tag>
                    <span class="audit-time">{{ formatTime(item.auditTime) }}</span>
                  </div>
                  <div v-if="item.auditorName" class="audit-auditor">
                    审核人：{{ item.auditorName }}
                  </div>
                  <div v-if="item.comment" class="audit-comment">
                    <div class="comment-label">审核意见：</div>
                    <div class="comment-content">{{ item.comment }}</div>
                  </div>
                  <div v-if="item.action === 'REJECT'" class="reject-warning">
                    <el-icon><Warning /></el-icon>
                    <span>知识已被驳回，请根据审核意见修改后重新提交</span>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无审核记录" :image-size="80" />
            </div>
          </el-card>
          
          <el-card class="info-card">
            <template #header>
              <span>版本历史</span>
            </template>
            <div v-loading="versionsLoading">
              <div v-if="versions.length > 0">
                <div v-for="(item, index) in versions.slice(0, 5)" :key="index" class="version-item">
                  <div class="version-header">
                    <span class="version-number">v{{ item.version }}</span>
                    <span class="version-time">{{ formatTime(item.createTime) }}</span>
                  </div>
                  <div v-if="item.changeDescription" class="version-desc">{{ item.changeDescription }}</div>
                  <div class="version-actions">
                    <el-button size="small" text @click="viewVersion(item)">查看</el-button>
                    <el-button 
                      v-if="item.version !== knowledge.version" 
                      size="small" 
                      text 
                      type="primary"
                      @click="compareVersion(item)"
                    >
                      对比
                    </el-button>
                  </div>
                </div>
                <el-button 
                  v-if="versions.length > 5" 
                  text 
                  type="primary" 
                  style="width: 100%; margin-top: 10px"
                  @click="showVersionDialog = true"
                >
                  查看全部版本 ({{ versions.length }})
                </el-button>
              </div>
              <el-empty v-else description="暂无版本记录" :image-size="80" />
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 版本历史对话框 -->
    <el-dialog v-model="showVersionDialog" title="版本历史" width="80%" @close="handleVersionDialogClose">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="版本列表" name="list">
          <div style="margin-bottom: 20px;">
            <el-alert 
              v-if="selectedVersionsForCompare && selectedVersionsForCompare.length === 1" 
              type="info" 
              :closable="false"
              style="margin-bottom: 10px"
            >
              已选择版本 v{{ selectedVersionsForCompare[0].version }}，请再选择一个版本进行对比
            </el-alert>
            <el-button 
              v-if="selectedVersionsForCompare && selectedVersionsForCompare.length > 0" 
              type="warning" 
              size="small"
              @click="clearSelectedVersions"
            >
              清空选择
            </el-button>
          </div>
          <el-table 
            ref="versionTableRef"
            :data="versions" 
            style="width: 100%" 
            v-loading="versionsLoading"
            @selection-change="handleVersionSelectionChange"
          >
            <el-table-column type="selection" width="55" :selectable="checkSelectable" />
            <el-table-column prop="version" label="版本" width="80" />
            <el-table-column prop="commitHash" label="Commit Hash" width="120">
              <template #default="scope">
                <el-tag size="small" type="info">{{ scope.row.commitHash || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="branch" label="分支" width="100">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.branch || 'main' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="commitMessage" label="Commit消息" min-width="200">
              <template #default="scope">
                {{ scope.row.commitMessage || scope.row.changeDescription || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="提交人" width="120" />
            <el-table-column prop="createTime" label="提交时间" width="180" />
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button size="small" @click="viewVersion(scope.row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 20px; text-align: right;">
            <el-button 
              type="primary" 
              :disabled="!selectedVersionsForCompare || selectedVersionsForCompare.length !== 2"
              @click="doCompareVersions"
            >
              对比选中的两个版本
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="版本对比" name="compare" v-if="compareResult">
          <div class="compare-header">
            <div class="version-select" style="display: flex; align-items: center; gap: 20px;">
              <div>
                <span><strong>版本1：</strong>v{{ compareResult.version1 }}</span>
                <el-tag size="small" type="info" style="margin-left: 10px">
                  {{ getVersionInfo(compareResult.version1).commitHash || '-' }}
                </el-tag>
              </div>
              <span style="font-weight: bold">vs</span>
              <div>
                <span><strong>版本2：</strong>v{{ compareResult.version2 }}</span>
                <el-tag size="small" type="info" style="margin-left: 10px">
                  {{ getVersionInfo(compareResult.version2).commitHash || '-' }}
                </el-tag>
              </div>
            </div>
            <div class="compare-stats" v-if="compareResult.stats">
              <span>新增：{{ compareResult.stats.insertCount }}行</span>
              <span>删除：{{ compareResult.stats.deleteCount }}行</span>
              <span>相同：{{ compareResult.stats.equalCount }}行</span>
            </div>
          </div>
          <div class="diff-content">
            <div 
              v-for="(line, index) in compareResult.diffLines" 
              :key="index"
              :class="['diff-line', `diff-${line.type.toLowerCase()}`]"
            >
              <span class="line-number">{{ line.lineNumber1 || '' }}</span>
              <span class="line-number">{{ line.lineNumber2 || '' }}</span>
              <span class="line-content">{{ line.content }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 版本查看对话框 -->
    <el-dialog v-model="showVersionViewDialog" :title="`版本 v${selectedVersion?.version}`" width="70%">
      <div v-if="selectedVersion">
        <div class="version-meta">
          <span>变更说明：{{ selectedVersion.changeDescription || '无' }}</span>
          <span>创建人：{{ selectedVersion.createdBy }}</span>
          <span>创建时间：{{ selectedVersion.createTime }}</span>
        </div>
        <div class="version-content">
          <pre>{{ selectedVersion.content }}</pre>
        </div>
      </div>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog 
      v-model="showFilePreviewDialog" 
      :title="`预览文件 - ${fileInfo?.fileName || ''}`" 
      width="90%" 
      :close-on-click-modal="false"
      @close="handleFilePreviewClose"
    >
      <div v-if="fileInfo" class="file-preview-container">
        <div class="file-preview-toolbar">
          <el-button @click="downloadFile">下载文件</el-button>
          <el-button @click="openInNewWindow">在新窗口打开</el-button>
        </div>
        <div class="file-preview-content">
          <!-- PDF文件 -->
          <iframe 
            v-if="isPdfFile(fileInfo.fileType)"
            :src="previewUrl" 
            class="preview-iframe"
            frameborder="0"
          ></iframe>
          <!-- 图片文件 -->
          <div v-else-if="isImageFile(fileInfo.fileType)" class="image-preview">
            <img :src="previewUrl" :alt="fileInfo.fileName" />
          </div>
          <!-- 文本文件 -->
          <div v-else-if="isTextFile(fileInfo.fileType)" class="text-preview">
            <iframe 
              :src="previewUrl" 
              class="preview-iframe"
              frameborder="0"
            ></iframe>
          </div>
          <!-- 其他文件类型，提示下载 -->
          <div v-else class="unsupported-preview">
            <el-alert
              title="该文件类型不支持在线预览"
              type="warning"
              :closable="false"
              show-icon
            >
              <template #default>
                <p>文件类型：{{ fileInfo.fileType }}</p>
                <p>请下载文件后使用相应的应用程序打开</p>
                <el-button type="primary" @click="downloadFile" style="margin-top: 10px">
                  下载文件
                </el-button>
              </template>
            </el-alert>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const knowledge = ref(null)
const relatedKnowledge = ref([])
const loading = ref(false)
const fileInfo = ref(null)
const isCollected = ref(false)

// 编辑模式
const isEditMode = computed(() => route.query.edit === 'true')
const editForm = ref({
  title: '',
  content: '',
  summary: '',
  category: '',
  keywords: '',
  changeDescription: ''
})

const showVersionDialog = ref(false)
const activeTab = ref('list')
const versions = ref([])
const versionsLoading = ref(false)
const selectedVersion = ref(null)
const showVersionViewDialog = ref(false)
const compareResult = ref(null)
const selectedVersionsForCompare = ref([]) // 用于存储选中的版本（最多2个）
const versionTableRef = ref(null)
const infoTab = ref('audit')
const auditHistory = ref([])
const auditLoading = ref(false)

// 文件预览相关
const showFilePreviewDialog = ref(false)
const previewUrl = ref('')

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}`)
    knowledge.value = res.data
    
    // 如果处于编辑模式，初始化编辑表单
    if (isEditMode.value) {
      editForm.value = {
        title: res.data.title || '',
        content: res.data.content || '',
        summary: res.data.summary || '',
        category: res.data.category || '',
        keywords: res.data.keywords || '',
        changeDescription: ''
      }
    }
    
    // 如果有文件ID，加载文件信息
    if (res.data.fileId) {
      try {
        const fileRes = await api.get(`/file/${res.data.fileId}`)
        if (fileRes.code === 200 && fileRes.data) {
          fileInfo.value = fileRes.data
        }
      } catch (error) {
        console.warn('加载文件信息失败', error)
      }
    }
    
    // 加载相关知识
    const relatedRes = await api.get(`/knowledge/${route.params.id}/related`, {
      params: { limit: 6 }
    })
    relatedKnowledge.value = relatedRes.data || []
    
    // 检查收藏状态
    if (userStore.userInfo && userStore.userInfo.id) {
      try {
        const collectRes = await api.get(`/knowledge/${route.params.id}/collect/status`, {
          params: { userId: userStore.userInfo.id }
        })
        isCollected.value = collectRes.data || false
      } catch (error) {
        console.error('检查收藏状态失败', error)
      }
    }
    
    // 加载审核历史
    loadAuditHistory()
    
    // 加载版本历史
    loadVersions()
  } catch (error) {
    console.error('加载详情失败', error)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const downloadFile = () => {
  if (!fileInfo.value || !fileInfo.value.id) {
    ElMessage.warning('文件信息不存在')
    return
  }
  // 创建下载链接
  const url = `/api/file/download/${fileInfo.value.id}`
  const link = document.createElement('a')
  link.href = url
  link.download = fileInfo.value.fileName || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const previewFile = () => {
  if (!fileInfo.value || !fileInfo.value.id) {
    ElMessage.warning('文件信息不存在')
    return
  }
  // 设置预览URL并打开对话框
  previewUrl.value = `/api/file/preview/${fileInfo.value.id}`
  showFilePreviewDialog.value = true
}

const handleFilePreviewClose = () => {
  previewUrl.value = ''
}

const openInNewWindow = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

const isPdfFile = (fileType) => {
  return fileType && fileType.toUpperCase() === 'PDF'
}

const isImageFile = (fileType) => {
  return fileType && ['IMAGE', 'JPG', 'JPEG', 'PNG', 'GIF', 'BMP', 'WEBP'].includes(fileType.toUpperCase())
}

const isTextFile = (fileType) => {
  return fileType && fileType.toUpperCase() === 'TXT'
}

const canPreview = (fileType) => {
  if (!fileType) return false
  const previewableTypes = ['PDF', 'WORD', 'EXCEL', 'PPT', 'IMAGE', 'TXT']
  return previewableTypes.includes(fileType.toUpperCase())
}

const isTextContent = (content) => {
  if (!content) return false
  // 如果内容以"文件："开头，可能是占位符，不是实际内容
  if (content.startsWith('文件：')) return false
  // 如果内容长度较长且包含换行，可能是文本内容
  return content.length > 50 || content.includes('\n')
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const saveEdit = async () => {
  if (!editForm.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  
  loading.value = true
  try {
    const userInfo = userStore.userInfo
    const res = await api.put(`/knowledge/${route.params.id}`, {
      ...editForm.value,
      updateBy: userInfo?.username
    })
    
    if (res.code === 200) {
      ElMessage.success('保存成功')
      // 重新加载详情
      await loadDetail()
      // 退出编辑模式
      router.replace(`/knowledge/${route.params.id}`)
    }
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const cancelEdit = () => {
  router.replace(`/knowledge/${route.params.id}`)
}

const loadVersions = async () => {
  versionsLoading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}/versions`)
    versions.value = res.data || []
  } catch (error) {
    console.error('加载版本列表失败', error)
    ElMessage.error('加载版本列表失败')
  } finally {
    versionsLoading.value = false
  }
}

const viewVersion = (version) => {
  selectedVersion.value = version
  showVersionViewDialog.value = true
}

const compareVersion = (version) => {
  selectedVersionsForCompare.value = [version]
  activeTab.value = 'compare'
  showVersionDialog.value = true
}

const checkSelectable = (row) => {
  // 如果已经选择了2个版本，且当前行不在已选择列表中，则不可选
  if (selectedVersionsForCompare.value && selectedVersionsForCompare.value.length >= 2) {
    return selectedVersionsForCompare.value.some(v => v.version === row.version)
  }
  return true
}

const handleVersionSelectionChange = (selection) => {
  // 只允许选择最多2个版本
  if (selection.length > 2) {
    ElMessage.warning('最多只能选择2个版本进行对比，已自动保留前2个选择')
    // 保留前2个选择
    const prevSelection = selectedVersionsForCompare.value
    selectedVersionsForCompare.value = selection.slice(0, 2)
    
    // 取消多余的选择
    if (versionTableRef.value) {
      setTimeout(() => {
        selection.slice(2).forEach(row => {
          versionTableRef.value.toggleRowSelection(row, false)
        })
      }, 0)
    }
    return
  }
  selectedVersionsForCompare.value = selection
}

const clearSelectedVersions = () => {
  selectedVersionsForCompare.value = []
  if (versionTableRef.value) {
    versionTableRef.value.clearSelection()
  }
}

const doCompareVersions = async () => {
  if (!selectedVersionsForCompare.value || selectedVersionsForCompare.value.length !== 2) {
    ElMessage.warning('请选择2个版本进行对比')
    return
  }
  
  const v1 = selectedVersionsForCompare.value[0]
  const v2 = selectedVersionsForCompare.value[1]
  
  // 确保版本号顺序正确（较小的在前）
  let version1 = v1.version
  let version2 = v2.version
  if (version1 > version2) {
    [version1, version2] = [version2, version1]
  }
  
  try {
    const res = await api.get(`/knowledge/${route.params.id}/versions/compare`, {
      params: {
        version1: version1,
        version2: version2
      }
    })
    if (res.code === 200) {
      compareResult.value = res.data
      activeTab.value = 'compare'
    } else {
      ElMessage.error(res.message || '对比版本失败')
    }
  } catch (error) {
    console.error('对比版本失败', error)
    ElMessage.error('对比版本失败：' + (error.message || '未知错误'))
  }
}

const handleVersionDialogClose = () => {
  activeTab.value = 'list'
  compareResult.value = null
  selectedVersionsForCompare.value = []
}

const getVersionInfo = (version) => {
  const v = versions.value.find(v => v.version === version)
  return v || {}
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const handleVersionDialogOpen = () => {
  loadVersions()
}

// 监听对话框打开
watch(showVersionDialog, (val) => {
  if (val) {
    handleVersionDialogOpen()
  }
})

// 监听路由变化，如果进入编辑模式，重新加载数据
watch(() => route.query.edit, (newVal) => {
  if (newVal === 'true' && knowledge.value) {
    editForm.value = {
      title: knowledge.value.title || '',
      content: knowledge.value.content || '',
      summary: knowledge.value.summary || '',
      category: knowledge.value.category || '',
      keywords: knowledge.value.keywords || '',
      changeDescription: ''
    }
  }
})

// 处理收藏
const handleCollect = async () => {
  if (!userStore.userInfo || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    if (isCollected.value) {
      // 取消收藏
      const res = await api.delete(`/knowledge/${route.params.id}/collect`, {
        params: { userId: userStore.userInfo.id }
      })
      if (res.code === 200) {
        isCollected.value = false
        knowledge.value.collectCount = Math.max(0, (knowledge.value.collectCount || 0) - 1)
        ElMessage.success('取消收藏成功')
      }
    } else {
      // 收藏
      const res = await api.post(`/knowledge/${route.params.id}/collect`, null, {
        params: { userId: userStore.userInfo.id }
      })
      if (res.code === 200) {
        isCollected.value = true
        knowledge.value.collectCount = (knowledge.value.collectCount || 0) + 1
        ElMessage.success('收藏成功')
      }
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

const getStatusType = (status) => {
  const map = {
    'DRAFT': 'info',
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'DRAFT': '草稿',
    'PENDING': '待审核',
    'APPROVED': '已发布',
    'REJECTED': '已驳回'
  }
  return map[status] || status
}

const getAuditStatusType = (status) => {
  const map = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return map[status] || ''
}

const getAuditStatusText = (status) => {
  const map = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return map[status] || status
}

const getAuditActionText = (action) => {
  const map = {
    'SUBMIT': '提交审核',
    'APPROVE': '审核通过',
    'REJECT': '审核驳回',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return map[action] || action || '未知'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadAuditHistory = async () => {
  if (!route.params.id) return
  auditLoading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}/audit/history`)
    if (res.code === 200 && res.data) {
      auditHistory.value = res.data || []
    }
  } catch (error) {
    console.error('加载审核历史失败', error)
  } finally {
    auditLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.knowledge-detail {
  padding: 20px;
}

.detail-layout {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.sidebar-info {
  width: 320px;
  flex-shrink: 0;
}

.info-card {
  margin-bottom: 20px;
}

.info-card :deep(.el-card__header) {
  padding: 15px 20px;
  font-weight: 600;
  background-color: #f5f7fa;
}

.meta-info {
  background: #f5f7fa;
  padding: 15px 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.meta-row:last-child {
  margin-bottom: 0;
}

.meta-info span {
  color: #606266;
  font-size: 14px;
}

.meta-info strong {
  color: #303133;
  margin-right: 5px;
}

.content {
  margin-top: 20px;
  line-height: 1.8;
}

.audit-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.audit-item:last-child {
  border-bottom: none;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.audit-time {
  font-size: 12px;
  color: #909399;
}

.audit-comment {
  margin-top: 8px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.comment-label {
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.comment-content {
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

.audit-auditor {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.reject-warning {
  margin-top: 8px;
  padding: 8px;
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.version-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.version-item:last-child {
  border-bottom: none;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.version-number {
  font-weight: 600;
  color: #409eff;
  font-size: 14px;
}

.version-time {
  font-size: 12px;
  color: #909399;
}

.version-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.6;
}

.version-actions {
  display: flex;
  gap: 10px;
}

.file-info {
  margin-bottom: 20px;
}

.file-info-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.file-info-left {
  flex: 1;
}

.file-info-left h3 {
  margin-top: 0;
  margin-bottom: 10px;
}

.file-info-left p {
  margin: 5px 0;
  color: #666;
}

.file-info-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}

.text-content {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.text-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: inherit;
}

.empty-content {
  margin: 20px 0;
  padding: 40px;
  text-align: center;
}

.edit-form {
  margin: 20px 0;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.related-knowledge {
  margin-top: 40px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.version-meta {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.version-meta span {
  margin-right: 20px;
}

.version-content {
  margin-top: 20px;
}

.version-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 500px;
  overflow-y: auto;
}

.compare-header {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.version-select {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 500;
}

.compare-stats span {
  margin-right: 20px;
  color: #606266;
}

.diff-content {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 600px;
  overflow-y: auto;
}

.diff-line {
  display: flex;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.diff-line .line-number {
  display: inline-block;
  width: 50px;
  padding: 2px 8px;
  text-align: right;
  background-color: #f5f7fa;
  border-right: 1px solid #dcdfe6;
  color: #909399;
  user-select: none;
}

.diff-line .line-content {
  flex: 1;
  padding: 2px 8px;
  white-space: pre;
}

.diff-equal {
  background-color: #ffffff;
}

.diff-insert {
  background-color: #f0f9ff;
}

.diff-insert .line-content {
  background-color: #e1f3ff;
}

.diff-delete {
  background-color: #fef0f0;
}

.diff-delete .line-content {
  background-color: #fde2e2;
  text-decoration: line-through;
}

.file-preview-container {
  display: flex;
  flex-direction: column;
  height: 70vh;
}

.file-preview-toolbar {
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  gap: 10px;
}

.file-preview-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background-color: #fff;
}

.image-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.text-preview {
  width: 100%;
  height: 100%;
}

.unsupported-preview {
  padding: 40px;
  text-align: center;
}
</style>

