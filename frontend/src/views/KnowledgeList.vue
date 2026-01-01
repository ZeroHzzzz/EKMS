<template>
  <div class="knowledge-list">
    <div class="page-header">
      <h2>知识库</h2>
      <el-button 
        v-if="hasPermission(userInfo, 'UPLOAD')" 
        type="primary" 
        :icon="Upload"
        @click="showUploadDialog = true"
      >
        上传文档
      </el-button>
    </div>

    <div class="search-bar">
      <el-autocomplete
        v-model="searchKeyword"
        :fetch-suggestions="fetchSuggestions"
        placeholder="请输入关键词搜索（支持全文、拼音、首字母、文件名）"
        class="search-input"
        :trigger-on-focus="false"
        clearable
        size="large"
        @select="handleSelect"
        @keyup.enter="handleSearch"
        popper-class="search-suggestions"
      >
        <template #default="{ item }">
          <div class="suggestion-item">
            <div class="suggestion-title">{{ item.value }}</div>
            <div v-if="item.category" class="suggestion-meta">
              <el-tag size="small" type="info">{{ item.category }}</el-tag>
            </div>
          </div>
        </template>
        <template #append>
          <el-button @click="handleSearch" size="large">搜索</el-button>
        </template>
      </el-autocomplete>
    </div>

    <div class="filter-bar">
      <div class="filter-left">
      <el-select v-model="filters.category" placeholder="分类" clearable style="width: 150px">
        <el-option label="技术文档" value="技术文档" />
        <el-option label="业务文档" value="业务文档" />
        <el-option label="培训资料" value="培训资料" />
      </el-select>
      <el-select 
        v-if="hasPermission(userInfo, 'VIEW_AUDIT') || hasRole(userInfo, ROLE_ADMIN)" 
        v-model="filters.status" 
        placeholder="全部状态" 
        clearable 
        style="width: 150px; margin-left: 10px"
      >
        <el-option label="已发布" value="APPROVED" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
      <!-- EDITOR可以看到草稿和已发布状态 -->
      <el-select 
        v-else-if="hasRole(userInfo, ROLE_EDITOR)"
        v-model="filters.status" 
        placeholder="全部状态" 
        clearable 
        style="width: 150px; margin-left: 10px"
      >
        <el-option label="已发布" value="APPROVED" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
      <!-- 普通用户默认只显示已发布的知识 -->
      <el-select 
        v-else
        v-model="filters.status" 
        placeholder="状态" 
        style="width: 150px; margin-left: 10px"
        disabled
      >
        <el-option label="已发布" value="APPROVED" />
      </el-select>
      </div>
    </div>

    <el-card class="table-card">
    <el-table 
      :data="knowledgeList" 
      style="width: 100%" 
      v-loading="loading"
      @row-click="handleRowClick"
      :row-style="{ cursor: 'pointer' }"
    >
      <el-table-column label="知识信息" min-width="500">
        <template #default="scope">
          <div class="knowledge-item">
            <!-- 标题部分 -->
            <div class="knowledge-title-row">
              <div v-if="scope.row.highlight && scope.row.highlight.title && scope.row.highlight.title.length > 0" 
                   v-html="scope.row.highlight.title[0]" 
                   class="knowledge-title highlight-title"></div>
              <div v-else class="knowledge-title">{{ scope.row.title }}</div>
              <div class="knowledge-meta-tags">
                <el-tag size="small" type="info">{{ scope.row.category }}</el-tag>
                <el-tag v-if="scope.row.highlight" size="small" type="success" class="match-tag">
                  <el-icon><Search /></el-icon>
                  匹配
                </el-tag>
              </div>
            </div>
            
            <!-- 内容摘要部分 -->
            <div class="knowledge-summary">
              <div v-if="scope.row.highlight && scope.row.highlight.content && scope.row.highlight.content.length > 0" 
                   class="highlight-summary">
                <div v-for="(fragment, index) in scope.row.highlight.content.slice(0, 2)" 
                     :key="index" 
                     v-html="fragment" 
                     class="highlight-fragment"></div>
              </div>
              <div v-else-if="scope.row.summary" class="normal-summary">
                {{ scope.row.summary }}
              </div>
              <div v-else-if="scope.row.content" class="normal-summary">
                {{ scope.row.content.substring(0, 150) }}{{ scope.row.content.length > 150 ? '...' : '' }}
              </div>
              <div v-else class="normal-summary text-muted">
                暂无内容摘要
              </div>
            </div>
            
            <!-- 匹配位置信息 -->
            <div v-if="scope.row.highlight" class="match-info">
              <el-icon class="match-icon"><Location /></el-icon>
              <span>{{ getMatchLocation(scope.row.highlight) }}</span>
            </div>
            
            <!-- 其他信息 -->
            <div class="knowledge-footer">
              <span class="footer-item">
                <el-icon><User /></el-icon>
                {{ scope.row.author }}
              </span>
              <span class="footer-item">
                <el-icon><View /></el-icon>
                {{ scope.row.clickCount || 0 }}
              </span>
              <span class="footer-item">
                <el-icon><Star /></el-icon>
                {{ scope.row.collectCount || 0 }}
              </span>
              <span class="footer-item">
                <el-icon><Clock /></el-icon>
                {{ formatTime(scope.row.createTime) }}
              </span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="getActionColumnWidth()" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click.stop="collect(scope.row)">收藏</el-button>
          <el-button 
            v-if="canEdit(scope.row)" 
            size="small" 
            type="warning" 
            @click.stop="editKnowledge(scope.row)"
          >
            编辑
          </el-button>
          <el-button 
            v-if="canPublish(scope.row)" 
            size="small" 
            type="success" 
            @click.stop="publishKnowledge(scope.row)"
          >
            发布
          </el-button>
          <el-button 
            v-if="canSubmitAudit(scope.row)" 
            size="small" 
            type="primary" 
            @click.stop="submitAudit(scope.row)"
          >
            提交审核
          </el-button>
          <el-button 
            v-if="canDelete(scope.row)" 
            size="small" 
            type="danger" 
            @click.stop="deleteKnowledge(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    </el-card>

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

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="600px" @close="handleUploadDialogClose">
      <el-upload
        class="upload-demo"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :file-list="fileList"
        multiple
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 Word、Excel、PDF、图片、视频等格式，支持批量上传
          </div>
        </template>
      </el-upload>

      <!-- 上传信息表单 -->
      <div v-if="fileList.length > 0" class="upload-form" style="margin-top: 20px">
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="分类" required>
            <el-select v-model="uploadForm.category" placeholder="请选择分类" style="width: 100%">
              <el-option label="技术文档" value="技术文档" />
              <el-option label="业务文档" value="业务文档" />
              <el-option label="培训资料" value="培训资料" />
              <el-option label="未分类" value="未分类" />
            </el-select>
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="uploadForm.summary" type="textarea" :rows="3" placeholder="请输入文件摘要" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="uploadForm.keywords" placeholder="请输入关键词，多个用逗号分隔" />
          </el-form-item>
        </el-form>
      </div>

      <div v-if="uploading" class="upload-progress" style="margin: 20px 0">
        <el-progress :percentage="uploadProgress" />
        <p style="margin-top: 10px">上传中：{{ currentFileName }}</p>
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="startUpload" 
          :disabled="fileList.length === 0 || uploading || !uploadForm.category"
          :loading="uploading"
        >
          开始上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { hasPermission, hasRole, ROLE_ADMIN, ROLE_EDITOR } from '../utils/permission'
import { Upload, UploadFilled, Search, Location, View, Star, Clock, User } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import CryptoJS from 'crypto-js'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const searchKeyword = ref('')
const filters = ref({
  category: '',
  status: null // 将在onMounted中根据权限设置
})
const knowledgeList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 上传相关
const showUploadDialog = ref(false)
const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const currentFileName = ref('')
const uploadForm = ref({
  category: '',
  summary: '',
  keywords: ''
})

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

const loadData = async () => {
  loading.value = true
  try {
    if (searchKeyword.value) {
      // 搜索
      const res = await api.post('/knowledge/search', {
        keyword: searchKeyword.value,
        searchType: 'AUTO', // 自动识别搜索类型
        category: filters.value.category,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      knowledgeList.value = res.data.results || []
      total.value = res.data.total || 0
    } else {
      // 列表 - 过滤掉空字符串和null的参数
      const params = {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
      if (filters.value.category) {
        params.category = filters.value.category
      }
      // 只有当status有值时才添加，空字符串、null、undefined都不添加（表示查看所有状态）
      if (filters.value.status) {
        params.status = filters.value.status
      }
      
      const res = await api.get('/knowledge/list', { params })
      knowledgeList.value = res.data || []
      total.value = res.data?.length || 0
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 获取搜索建议
const fetchSuggestions = async (queryString, cb) => {
  if (!queryString || queryString.trim().length < 1) {
    cb([])
    return
  }

  try {
    const res = await api.get('/knowledge/search/suggestions', {
      params: {
        keyword: queryString,
        limit: 10
      }
    })

    const suggestions = []
    
    // 添加标题建议
    if (res.data.suggestions && res.data.suggestions.length > 0) {
      res.data.suggestions.forEach(title => {
        suggestions.push({
          value: title,
          type: 'title'
        })
      })
    }

    // 添加预览结果作为建议
    if (res.data.previewResults && res.data.previewResults.length > 0) {
      res.data.previewResults.forEach(item => {
        // 避免重复
        if (!suggestions.find(s => s.value === item.title)) {
          suggestions.push({
            value: item.title,
            type: 'knowledge',
            category: item.category,
            id: item.id
          })
        }
      })
    }

    cb(suggestions)
  } catch (error) {
    console.error('获取搜索建议失败', error)
    cb([])
  }
}

// 选择建议项
const handleSelect = (item) => {
  searchKeyword.value = item.value
  handleSearch()
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

// 获取匹配位置描述
const getMatchLocation = (highlight) => {
  if (!highlight) return ''
  const locations = []
  if (highlight.title && highlight.title.length > 0) {
    locations.push('标题')
  }
  if (highlight.fileName && highlight.fileName.length > 0) {
    locations.push('文件名')
  }
  if (highlight.keywords && highlight.keywords.length > 0) {
    locations.push('关键词')
  }
  if (highlight.content && highlight.content.length > 0) {
    locations.push('内容')
  }
  return locations.length > 0 ? locations.join('、') : ''
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

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const handleRowClick = (row) => {
  // 点击行时跳转到详情页面
  viewDetail(row.id)
}

const collect = async (row) => {
  try {
    if (!userInfo.value || !userInfo.value.id) {
      ElMessage.warning('请先登录')
      return
    }
    
    const res = await api.post(`/knowledge/${row.id}/collect`, null, {
      params: { userId: userInfo.value.id }
    })
    
    if (res.code === 200) {
    ElMessage.success('收藏成功')
      // 更新收藏数
      row.collectCount = (row.collectCount || 0) + 1
    } else {
      ElMessage.error(res.message || '收藏失败')
    }
  } catch (error) {
    ElMessage.error('收藏失败: ' + (error.response?.data?.message || error.message))
  }
}

const canEdit = (row) => {
  if (!userInfo.value) return false
  // ADMIN可以编辑所有知识
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // EDITOR只能编辑自己创建的知识
  if (hasRole(userInfo.value, ROLE_EDITOR) && row.author === userInfo.value.realName) {
    return true
  }
  return false
}

const canPublish = (row) => {
  if (!userInfo.value) return false
  // 只有草稿状态的知识才能发布
  if (row.status !== 'DRAFT') return false
  // ADMIN和EDITOR可以直接发布知识（不需要审核）
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  if (hasRole(userInfo.value, ROLE_EDITOR)) return true
  return false
}

const canSubmitAudit = (row) => {
  if (!userInfo.value) return false
  // 只有草稿状态的知识才能提交审核
  if (row.status !== 'DRAFT') return false
  // 普通用户可以提交审核
  if (hasRole(userInfo.value, 'USER')) return true
  return false
}

const canDelete = (row) => {
  if (!userInfo.value) return false
  // ADMIN可以删除所有知识
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // EDITOR只能删除自己创建的知识
  if (hasRole(userInfo.value, ROLE_EDITOR) && row.author === userInfo.value.realName) {
    return true
  }
  return false
}

const editKnowledge = (row) => {
  // 跳转到详情页面（编辑模式通过查询参数传递）
  router.push(`/knowledge/${row.id}?edit=true`)
}

const publishKnowledge = async (row) => {
  try {
    await ElMessageBox.confirm('确定要发布该知识吗？发布后普通用户将可以看到。', '提示', {
      type: 'info'
    })
    const res = await api.post(`/knowledge/${row.id}/publish`)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      row.status = 'APPROVED'
      loadData()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发布失败')
    }
  }
}

const submitAudit = async (row) => {
  try {
    await ElMessageBox.confirm('确定要提交审核吗？提交后知识管理员将进行审核。', '提示', {
      type: 'info'
    })
    const res = await api.post(`/knowledge/${row.id}/submit-audit`, null, {
      params: { userId: userInfo.value.id }
    })
    if (res.code === 200) {
      ElMessage.success('提交审核成功')
      row.status = 'PENDING'
      loadData()
    } else {
      ElMessage.error(res.message || '提交审核失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '提交审核失败')
    }
  }
}

const deleteKnowledge = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识吗？', '提示', {
      type: 'warning'
    })
    const res = await api.delete(`/knowledge/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 上传相关方法
const handleFileChange = (file, files) => {
  fileList.value = files
}

const calculateFileHash = (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const wordArray = CryptoJS.lib.WordArray.create(e.target.result)
      const hash = CryptoJS.SHA256(wordArray).toString()
      resolve(hash)
    }
    reader.readAsArrayBuffer(file)
  })
}

const startUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  
  if (!uploadForm.value.category) {
    ElMessage.warning('请选择分类')
    return
  }
  
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    for (let i = 0; i < fileList.value.length; i++) {
      const file = fileList.value[i].raw
      currentFileName.value = file.name
      
      // 计算文件哈希
      const fileHash = await calculateFileHash(file)
      
      // 检查文件是否已存在
      let fileDTO = null
      try {
        const checkRes = await api.get(`/file/check/${fileHash}`)
        if (checkRes.code === 200 && checkRes.data) {
          fileDTO = checkRes.data
          ElMessage.info(`${file.name} 已存在，跳过上传`)
          uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
          continue
        }
      } catch (error) {
        // 文件不存在，继续上传
      }
      
      // 分片上传
      if (!fileDTO) {
        const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
        const uploadId = Date.now().toString() + Math.random().toString(36).substr(2, 9)
        
        for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
          const start = chunkIndex * CHUNK_SIZE
          const end = Math.min(start + CHUNK_SIZE, file.size)
          const chunk = file.slice(start, end)
          
          const formData = new FormData()
          formData.append('file', chunk)
          formData.append('uploadId', uploadId)
          formData.append('chunkIndex', chunkIndex)
          formData.append('totalChunks', totalChunks)
          formData.append('fileName', file.name)
          formData.append('fileHash', fileHash)
          
          await api.post('/file/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          
          uploadProgress.value = Math.round(((chunkIndex + 1) / totalChunks) * 100 * (i + 1) / fileList.value.length)
        }
        
        // 完成上传
        const completeRes = await api.post(`/file/complete/${uploadId}`)
        if (completeRes.code !== 200 || !completeRes.data) {
          throw new Error('文件上传完成失败')
        }
        fileDTO = completeRes.data
      }
      
      // 创建知识条目
      const userInfo = userStore.userInfo
      if (!userInfo) {
        throw new Error('用户信息不存在，请重新登录')
      }
      
      // 提取文件名（不含扩展名）作为标题
      const fileNameWithoutExt = file.name.substring(0, file.name.lastIndexOf('.')) || file.name
      
      // 读取文件内容（仅文本文件）
      let fileContent = `文件：${file.name}`
      if (file.type.startsWith('text/') || file.name.endsWith('.txt')) {
        try {
          const text = await file.text()
          fileContent = text
        } catch (e) {
          console.warn('读取文件内容失败，使用默认内容', e)
        }
      }
      
      const knowledgeRes = await api.post('/knowledge', {
        title: fileNameWithoutExt,
        content: fileContent,
        summary: uploadForm.value.summary || `上传的文件：${file.name}`,
        category: uploadForm.value.category,
        keywords: uploadForm.value.keywords || '',
        fileId: fileDTO.id,
        author: userInfo.realName || userInfo.username || '未知',
        department: userInfo.department || '未知',
        createBy: userInfo.username
      })
      
      if (knowledgeRes.code !== 200) {
        throw new Error(knowledgeRes.message || '创建知识条目失败')
      }
      
      uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
    }
    
    ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)
    handleUploadDialogClose()
    loadData() // 刷新列表
  } catch (error) {
    ElMessage.error(`上传失败：${error.message}`)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    currentFileName.value = ''
  }
}

const handleUploadDialogClose = () => {
  showUploadDialog.value = false
  fileList.value = []
  uploadForm.value = {
    category: '',
    summary: '',
    keywords: ''
  }
  uploading.value = false
  uploadProgress.value = 0
  currentFileName.value = ''
}

const getActionColumnWidth = () => {
  if (hasPermission(userInfo.value, 'EDIT') || hasPermission(userInfo.value, 'DELETE')) {
    return 300
  }
  return 200
}

onMounted(() => {
  // 普通用户（USER）默认只显示已发布的知识
  // EDITOR和ADMIN可以看到所有状态（包括DRAFT），不设置status过滤
  const isEditor = hasRole(userInfo.value, ROLE_EDITOR)
  const isAdmin = hasRole(userInfo.value, ROLE_ADMIN)
  const hasViewAudit = hasPermission(userInfo.value, 'VIEW_AUDIT')
  
  // 只有普通用户才设置默认过滤为APPROVED
  if (!isEditor && !isAdmin && !hasViewAudit) {
    filters.value.status = 'APPROVED'
  }
  // EDITOR、ADMIN 或有 VIEW_AUDIT 权限的用户，不设置 status，这样可以看所有状态
  loadData()
})
</script>

<style scoped>
.knowledge-list {
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

.search-bar {
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.filter-left {
  display: flex;
  gap: 10px;
  flex: 1;
}

.table-card {
  margin-bottom: 16px;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

/* 表格行悬停效果 */
.knowledge-list :deep(.el-table__row) {
  transition: background-color 0.2s;
}

.knowledge-list :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
}

.upload-form {
  margin-top: 20px;
}

.upload-progress {
  margin: 20px 0;
}

/* 搜索建议样式 */
.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.suggestion-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.suggestion-meta {
  margin-left: 10px;
}

/* 搜索建议下拉框样式 */
:deep(.search-suggestions) {
  max-height: 400px;
  overflow-y: auto;
}

:deep(.search-suggestions .el-autocomplete-suggestion__list) {
  padding: 0;
}

/* 知识条目样式 */
.knowledge-item {
  padding: 12px 0;
}

.knowledge-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.knowledge-title {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.knowledge-meta-tags {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.match-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.knowledge-summary {
  margin-bottom: 8px;
  line-height: 1.6;
}

.highlight-summary {
  color: #606266;
}

.highlight-fragment {
  margin-bottom: 4px;
  padding: 4px 0;
}

.highlight-fragment :deep(mark) {
  background-color: #fff566;
  color: #303133;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 600;
}

.normal-summary {
  color: #909399;
  font-size: 14px;
  line-height: 1.6;
}

.text-muted {
  color: #c0c4cc;
  font-style: italic;
}

.match-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
  margin-bottom: 8px;
  padding: 4px 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  width: fit-content;
}

.match-icon {
  font-size: 14px;
}

.knowledge-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.footer-item .el-icon {
  font-size: 14px;
}

/* 高亮标题样式 */
.highlight-title :deep(mark) {
  background-color: #fff566;
  color: #303133;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 700;
}

:deep(.search-suggestions .el-autocomplete-suggestion__list li) {
  padding: 0 15px;
  line-height: normal;
}

/* 高亮样式 */
.highlight-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.highlight-content {
  margin-bottom: 4px;
  line-height: 1.6;
}

.match-location {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 高亮标记样式 */
:deep(mark) {
  background-color: #ffeb3b;
  color: #333;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 600;
}
</style>

