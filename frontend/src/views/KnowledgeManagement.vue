<template>
  <div class="knowledge-management">
    <div class="page-header">
      <div class="header-left">
        <h2>知识管理</h2>
        <p class="subtitle">文档审核、维护与管理</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" @click="router.push('/file-upload')" class="add-btn">
          <el-icon class="el-icon--left"><Upload /></el-icon> 上传文档
        </el-button>
      </div>
    </div>

    <el-card class="control-card" shadow="hover">
      <div class="control-bar">
        <div class="search-section">
          <el-autocomplete
            v-model="searchKeyword"
            :fetch-suggestions="fetchSuggestions"
            placeholder="全文/文件名/拼音搜索..."
            class="search-input"
            :trigger-on-focus="false"
            clearable
            size="default"
            @select="handleSelect"
            @keyup.enter="handleSearch"
            popper-class="search-suggestions"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #default="{ item }">
              <div class="suggestion-item">
                <span class="suggestion-title">{{ item.value }}</span>
                <el-tag v-if="item.type === 'knowledge'" size="small" type="info">文档</el-tag>
              </div>
            </template>
          </el-autocomplete>
        </div>

        <div class="filter-section">
          <el-select 
            v-model="filters.status" 
            placeholder="状态" 
            style="width: 120px"
            @change="handleFilterChange"
            clearable
          >
            <el-option label="待审核" value="PENDING" />
            <el-option label="已发布" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          
          <el-input
            v-model="filters.author"
            placeholder="作者"
            clearable
            style="width: 120px"
            @clear="handleFilterChange"
          >
          </el-input>
          
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 220px"
            @change="handleFilterChange"
          />
          
          <div class="filter-actions">
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="clearFilters" :icon="Refresh">重置</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
    <div class="table-toolbar">
      <div class="toolbar-left">
        <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
        <span class="selected-count">已选择 {{ selectedItems && selectedItems.length ? selectedItems.length : 0 }} 项</span>
      </div>
      <div class="toolbar-right">
        <el-button 
          :disabled="!selectedItems || selectedItems.length === 0 || !hasPendingItems"
          type="success" 
          @click="batchApprove"
        >
          <el-icon><Check /></el-icon>
          批量通过
        </el-button>
        <el-button 
          :disabled="!selectedItems || selectedItems.length === 0 || !hasPendingItems"
          type="danger" 
          @click="showBatchRejectDialog"
        >
          <el-icon><Close /></el-icon>
          批量拒绝
        </el-button>
      </div>
    </div>
    <el-table 
      ref="tableRef"
      :data="knowledgeList" 
      style="width: 100%" 
      v-loading="loading"
      @row-click="handleRowClick"
      :row-style="{ cursor: 'pointer' }"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" @click.stop />
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
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)" size="small">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
          <el-tag v-if="scope.row.hasDraft && scope.row.status === 'APPROVED'" type="warning" size="small" style="margin-left: 5px">
            Pending Update
          </el-tag>
        </template>
      </el-table-column>
      <!-- 审核列 -->
      <el-table-column label="审核" width="130" align="center">
        <template #default="scope">
          <div class="audit-cell">
            <!-- 待审核状态 或 有待审核草稿 -->
            <template v-if="scope.row.status === 'PENDING' || scope.row.hasDraft">
              <el-tooltip content="通过" placement="top">
                <el-button type="success" size="small" circle :icon="Check" @click.stop="handleAuditApprove(scope.row)" />
              </el-tooltip>
              <el-tooltip content="驳回" placement="top">
                <el-button type="danger" size="small" circle :icon="Close" @click.stop="handleAuditReject(scope.row)" />
              </el-tooltip>
            </template>
            <!-- 已发布状态（且无草稿） -->
            <template v-else-if="scope.row.status === 'APPROVED'">
              <el-button disabled size="small" circle :icon="Check" class="audit-btn-disabled" />
              <el-tooltip content="撤回发布" placement="top">
                <el-button type="danger" size="small" circle :icon="Close" @click.stop="handleAuditReject(scope.row)" />
              </el-tooltip>
            </template>
            <!-- 已驳回状态 -->
            <template v-else-if="scope.row.status === 'REJECTED'">
              <el-tooltip content="重新发布" placement="top">
                <el-button type="success" size="small" circle :icon="Check" @click.stop="handleRePublish(scope.row)" />
              </el-tooltip>
              <el-button disabled size="small" circle :icon="Close" class="audit-btn-disabled" />
            </template>
          </div>
        </template>
      </el-table-column>
      <!-- 操作列 -->
      <el-table-column label="操作" width="150" align="center">
        <template #default="scope">
          <div class="ops-cell">
            <el-button size="small" link type="primary" @click.stop="collect(scope.row)">收藏</el-button>
            <el-button v-if="canEdit(scope.row)" size="small" link type="warning" @click.stop="editKnowledge(scope.row)">编辑</el-button>
            <el-button v-if="canDelete(scope.row)" size="small" link type="danger" @click.stop="deleteKnowledge(scope.row)">删除</el-button>
          </div>
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





    <!-- 审核通过对话框 -->
    <el-dialog v-model="showApproveDialogVisible" title="审核通过" width="800px">
      <div v-if="currentKnowledge" class="audit-dialog-content">
        <div class="knowledge-preview">
          <h4>知识信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题">{{ currentKnowledge.title }}</el-descriptions-item>
            <el-descriptions-item label="版本">
              <el-tag type="warning" size="small">v{{ currentAudit?.version || currentKnowledge.version }}</el-tag>
              <span v-if="currentKnowledge.publishedVersion && currentKnowledge.publishedVersion !== (currentAudit?.version || currentKnowledge.version)" style="margin-left: 8px; color: #909399; font-size: 12px;">
                (当前发布: v{{ currentKnowledge.publishedVersion }})
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="分类">{{ currentKnowledge.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentKnowledge.author }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ currentKnowledge.department || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交人">{{ currentAudit?.submitUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="摘要" :span="2">{{ currentKnowledge.summary || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="currentKnowledge.fileId" class="preview-link" style="margin-top: 12px;">
            <el-button type="primary" text @click="previewVersionContent">
              <el-icon><View /></el-icon> 预览待审核版本内容
            </el-button>
          </div>
        </div>

        <!-- 合并状态检查 -->
        <div class="merge-status-section" style="margin-top: 20px" v-loading="checkingMerge">
            <el-alert
                v-if="mergeStatus && mergeStatus.hasConflict"
                title="检测到合并冲突"
                type="error"
                description="当前草稿与已发布版本存在冲突，系统无法自动合并。请手动解决冲突后再通过。"
                show-icon
                :closable="false"
            />
            <el-alert
                v-else-if="mergeStatus && mergeStatus.canAutoMerge"
                title="可以自动合并"
                type="success"
                description="系统未检测到冲突，审核通过后将自动合并发布。"
                show-icon
                :closable="false"
            />
        </div>

        <!-- 冲突解决编辑器 -->
        <div v-if="mergeStatus && mergeStatus.hasConflict" class="conflict-resolver" style="margin-top: 20px;">
            <h4>解决冲突</h4>
            
            <!-- 使用高级合并界面按钮 -->
            <div style="margin-bottom: 16px;">
                <el-button 
                    type="primary" 
                    @click="openMergeReviewPage"
                    :icon="Document"
                >
                    使用高级合并界面 (GitHub风格)
                </el-button>
                <span style="margin-left: 12px; color: #909399; font-size: 12px;">
                    推荐：可视化对比并逐块选择要保留的内容
                </span>
            </div>
            
            <el-divider content-position="center">或在下方直接编辑</el-divider>
            
            <el-input
                v-model="resolvedContent"
                type="textarea"
                :rows="15"
                class="conflict-editor"
                placeholder="请在此处手动解决冲突，保留最终内容..."
            />
            <div style="margin-top: 5px; color: #909399; font-size: 12px;">
                请查找并处理所有标准冲突标记 (<<<<<<< HEAD, =======, >>>>>>> Incoming)
            </div>
        </div>


        <el-form-item label="审核意见" style="margin-top: 20px">
          <el-input 
            v-model="approveComment" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入审核意见（可选）"
          />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="showApproveDialogVisible = false">取消</el-button>
        <el-button 
            type="success" 
            @click="doApprove" 
            :loading="auditing"
            :disabled="mergeStatus && mergeStatus.hasConflict && !resolvedContent"
        >
            {{ mergeStatus && mergeStatus.hasConflict ? '解决冲突并通过' : '审核通过' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog v-model="showRejectDialogVisible" title="驳回审核" width="600px">
      <div v-if="currentKnowledge" class="audit-dialog-content">
        <div class="knowledge-preview">
          <h4>知识信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题">{{ currentKnowledge.title }}</el-descriptions-item>
            <el-descriptions-item label="版本">
              <el-tag type="warning" size="small">v{{ currentAudit?.version || currentKnowledge.version }}</el-tag>
              <span v-if="currentKnowledge.publishedVersion && currentKnowledge.publishedVersion !== (currentAudit?.version || currentKnowledge.version)" style="margin-left: 8px; color: #909399; font-size: 12px;">
                (当前发布: v{{ currentKnowledge.publishedVersion }})
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="分类">{{ currentKnowledge.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ currentKnowledge.author }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ currentKnowledge.department || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交人">{{ currentAudit?.submitUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="摘要" :span="2">{{ currentKnowledge.summary || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="currentKnowledge.fileId" class="preview-link" style="margin-top: 12px;">
            <el-button type="primary" text @click="previewVersionContent">
              <el-icon><View /></el-icon> 预览待审核版本内容
            </el-button>
          </div>
        </div>
        <el-form-item label="驳回原因" style="margin-top: 20px">
          <el-input 
            v-model="rejectComment" 
            type="textarea" 
            :rows="3" 
            placeholder="请填写驳回原因（必填）"
          />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="showRejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="doReject" :loading="auditing" :disabled="!rejectComment || rejectComment.trim().length === 0">驳回</el-button>
      </template>
    </el-dialog>

    <!-- 批量拒绝对话框 -->
    <el-dialog v-model="showBatchRejectDialogVisible" title="批量拒绝" width="500px">
      <div class="batch-reject-content">
        <p style="margin-bottom: 16px; color: #606266;">
          即将拒绝 <span style="color: #f56c6c; font-weight: 600;">{{ selectedItems.filter(item => item.status === 'PENDING').length }}</span> 个待审核的知识
        </p>
        <el-form-item label="拒绝原因" required>
          <el-input 
            v-model="batchRejectComment" 
            type="textarea" 
            :rows="4" 
            placeholder="请填写拒绝原因（必填），将应用于所有选中的知识"
          />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="showBatchRejectDialogVisible = false">取消</el-button>
        <el-button 
          type="danger" 
          @click="doBatchReject" 
          :loading="batchAuditing" 
          :disabled="!batchRejectComment || batchRejectComment.trim().length === 0"
        >
          <el-icon><Close /></el-icon>
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { hasPermission, hasRole, ROLE_ADMIN, ROLE_EDITOR } from '../utils/permission'
import { Upload, Search, Location, View, Star, Clock, User, ArrowDown, Check, Close, Refresh, Document } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import CryptoJS from 'crypto-js'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const searchKeyword = ref('')
const filters = ref({
  status: null, // 知识管理页面默认显示全部状态
  author: '',
  dateRange: null
})
const knowledgeList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 批量选择相关
const selectedItems = ref([])
const selectAll = ref(false)
const tableRef = ref(null)

// 批量审核相关
const showBatchRejectDialogVisible = ref(false)
const batchRejectComment = ref('')
const batchAuditing = ref(false)

// 计算是否有待审核的选中项
const hasPendingItems = computed(() => {
  return selectedItems.value.some(item => item.status === 'PENDING' || item.hasDraft)
})

// 搜索历史
const searchHistory = ref([])
const showSearchHistory = ref(false)





// 审核相关
const showApproveDialogVisible = ref(false)
const showRejectDialogVisible = ref(false)
const currentKnowledge = ref(null)
const currentAudit = ref(null)
const approveComment = ref('')
const rejectComment = ref('')
const auditing = ref(false)
const checkingMerge = ref(false)
const mergeStatus = ref(null)
const resolvedContent = ref('')

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

const loadData = async () => {
  loading.value = true
  try {
    // 构建搜索/筛选参数
    const searchParams = {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    
    // 如果有搜索关键词，使用搜索接口
    if (searchKeyword.value && searchKeyword.value.trim()) {
      searchParams.keyword = searchKeyword.value.trim()
      searchParams.searchType = 'AUTO'
      }
    
    // 添加筛选条件
    if (filters.value.status) {
      searchParams.status = filters.value.status
    }
    if (filters.value.author && filters.value.author.trim()) {
      searchParams.author = filters.value.author.trim()
    }
    if (filters.value.dateRange && filters.value.dateRange.length === 2) {
      searchParams.startDate = filters.value.dateRange[0]
      searchParams.endDate = filters.value.dateRange[1]
    }
    
    let res
    if (searchParams.keyword) {
      // 使用搜索接口
      res = await api.post('/knowledge/search', searchParams)
      // 过滤掉文件夹（只显示有fileId的文件）
      const results = (res.data.results || [])

      knowledgeList.value = sortKnowledgeList(results)
      total.value = results.length
    } else {
      // 使用列表接口
      res = await api.get('/knowledge/list', { params: searchParams })
      // 过滤掉文件夹（只显示有fileId的文件）
      const results = (res.data || [])

      knowledgeList.value = sortKnowledgeList(results)
      total.value = results.length
    }
    
    // 加载待审核知识的审核信息
    if (hasPermission(userInfo.value, 'AUDIT')) {
      for (let knowledge of knowledgeList.value) {
        if (knowledge.status === 'PENDING') {
          await loadAuditInfo(knowledge)
        }
      }
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

// 筛选条件变化时自动更新
const handleFilterChange = () => {
  pageNum.value = 1
  loadData()
}

  // 清空筛选
  const clearFilters = () => {
    filters.value = {
      status: null, // null 表示全部状态
      author: '',
      dateRange: null
    }
    searchKeyword.value = ''
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

// 排序知识列表：待审核优先，同状态按创建时间倒序（最新的在前）
const sortKnowledgeList = (list) => {
  if (!list || list.length === 0) return list
  
  return [...list].sort((a, b) => {
    // 首先按状态排序：PENDING 优先
    const statusOrder = { 'PENDING': 0, 'APPROVED': 1, 'REJECTED': 2 }
    const statusA = statusOrder[a.status] ?? 3
    const statusB = statusOrder[b.status] ?? 3
    
    if (statusA !== statusB) {
      return statusA - statusB
    }
    
    // 同状态下，按创建时间倒序（最新的在前）
    const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
    const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
    return timeB - timeA
  })
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

// 批量选择处理
const handleSelectionChange = (selection) => {
  selectedItems.value = selection || []
  selectAll.value = selection && selection.length === knowledgeList.value.length && knowledgeList.value.length > 0
}

const handleSelectAll = (checked) => {
  if (checked) {
    selectedItems.value = knowledgeList.value ? [...knowledgeList.value] : []
    // 同步表格选择状态
    if (tableRef.value && knowledgeList.value) {
      knowledgeList.value.forEach(row => {
        tableRef.value.toggleRowSelection(row, true)
      })
    }
  } else {
    selectedItems.value = []
    // 清除表格选择状态
    if (tableRef.value) {
      tableRef.value.clearSelection()
    }
  }
}

// 批量操作
const batchDownload = async () => {
  if (!selectedItems.value || selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要下载的知识')
    return
  }
  
  try {
    const ids = selectedItems.value.map(item => item.id)
    // 批量下载逻辑
    for (const id of ids) {
      const link = document.createElement('a')
      link.href = `/api/knowledge/${id}/download`
      link.download = ''
      link.click()
      // 添加延迟避免浏览器阻止多个下载
      await new Promise(resolve => setTimeout(resolve, 200))
    }
    ElMessage.success(`已开始下载 ${ids.length} 个文件`)
  } catch (error) {
    ElMessage.error('批量下载失败: ' + (error.message || '未知错误'))
  }
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
  // ADMIN can edit all
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // Allow if author or creator
  if (row.author === userInfo.value.realName || row.createBy === userInfo.value.username) {
     return true
  }
  return false
}

// 发布功能已合并到审核通过，不再需要单独的发布函数

const canSubmitAudit = (row) => {
  if (!userInfo.value) return false
  // 已驳回状态的知识可以重新提交审核
  if (row.status === 'REJECTED') {
    // 所有用户都可以重新提交审核
    return true
  }
  return false
}

const canAudit = (row) => {
  if (!userInfo.value) return false
  // 只有待审核状态的知识才能审核
  if (row.status !== 'PENDING') return false
  // 有审核权限的用户可以审核
  if (hasPermission(userInfo.value, 'AUDIT')) return true
  return false
}

const canDelete = (row) => {
  if (!userInfo.value) return false
  // 管理员可以删除所有知识
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // 任何用户都可以删除自己创建的文档（检查 author 和 createBy）
  if (row.author === userInfo.value.realName || row.createBy === userInfo.value.username) {
    return true
  }
  return false
}

// 状态显示
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

const editKnowledge = (row) => {
  // 跳转到详情页面（编辑模式通过查询参数传递）
  router.push(`/knowledge/${row.id}?edit=true`)
}

// 发布功能已合并到审核通过，不再需要单独的发布函数

// 处理审核操作下拉菜单命令
const handleAuditCommand = (command) => {
  const { action, row } = command
  switch (action) {
    case 'submit':
      submitAudit(row)
      break
    case 'approve':
      showApproveDialog(row)
      break
    case 'reject':
      showRejectDialog(row)
      break
  }
}

const submitAudit = async (row) => {
  try {
    await ElMessageBox.confirm('确定要提交审核吗？提交后知识管理员将进行审核。', '提示', {
      type: 'info'
    })
    const res = await api.post(`/knowledge/${row.id}/submit-audit?userId=${userInfo.value.id}`)
    if (res.code === 200) {
      ElMessage.success('提交审核成功')
      row.status = 'PENDING'
      // 加载审核信息
      await loadAuditInfo(row)
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

// 审核相关函数
// 点击审核通过按钮：直接打开预览对话框
const handleAuditApprove = async (row) => {
  currentKnowledge.value = row
  // 如果没有审核信息，先加载
  if (!row.auditInfo && row.status === 'PENDING') {
    await loadAuditInfo(row)
  }
  currentAudit.value = row.auditInfo
  approveComment.value = ''
  
  mergeStatus.value = null
  resolvedContent.value = ''
  
  showApproveDialogVisible.value = true
  
  if (currentAudit.value && currentAudit.value.id) {
     checkMerge(currentAudit.value.id)
  }
}

// 点击审核驳回按钮：直接打开预览对话框
const handleAuditReject = async (row) => {
  currentKnowledge.value = row
  // 如果没有审核信息，先加载
  if (!row.auditInfo && row.status === 'PENDING') {
    await loadAuditInfo(row)
  }
  currentAudit.value = row.auditInfo
  rejectComment.value = ''
  showRejectDialogVisible.value = true
}

const showApproveDialog = async (row) => {
  currentKnowledge.value = row
  // 如果没有审核信息，先加载
  if (!row.auditInfo && row.status === 'PENDING') {
    await loadAuditInfo(row)
  }
  currentAudit.value = row.auditInfo
  approveComment.value = ''
  
  // Reset merge status
  mergeStatus.value = null
  resolvedContent.value = ''
  
  showApproveDialogVisible.value = true
  
  // Check merge status
  if (currentAudit.value && currentAudit.value.id) {
     checkMerge(currentAudit.value.id)
  }
}

// Check Merge Status
const checkMerge = async (auditId) => {
    checkingMerge.value = true
    try {
        const res = await api.get(`/knowledge/audit/${auditId}/merge-status`)
        if (res.code === 200) {
            mergeStatus.value = res.data
            if (res.data.hasConflict) {
                resolvedContent.value = res.data.mergedContent // Pre-fill with conflict markers
            } else {
                resolvedContent.value = ''
            }
        }
    } catch (e) {
        console.error("Check merge status failed", e)
    } finally {
        checkingMerge.value = false
    }
}

const showRejectDialog = async (row) => {
  currentKnowledge.value = row
  // 如果没有审核信息，先加载
  if (!row.auditInfo && row.status === 'PENDING') {
    await loadAuditInfo(row)
  }
  currentAudit.value = row.auditInfo
  rejectComment.value = ''
  showRejectDialogVisible.value = true
}

// 撤回发布（已废弃，改用驳回功能）
const withdrawPublish = async (row) => {
  try {
    await ElMessageBox.confirm('确定要撤回该知识的发布吗？撤回后将从知识库和知识结构中移除，需要重新审核才能发布。', '提示', {
      type: 'warning'
    })
    const res = await api.put(`/knowledge/${row.id}`, {
      ...row,
      status: 'PENDING'
    })
    if (res.code === 200) {
      ElMessage.success('撤回发布成功')
      row.status = 'PENDING'
      // 重新提交审核
      try {
        await api.post(`/knowledge/${row.id}/submit-audit?userId=${userInfo.value.id}`)
      } catch (error) {
        console.warn('重新提交审核失败', error)
      }
      loadData()
    } else {
      ElMessage.error(res.message || '撤回发布失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤回发布失败')
    }
  }
}

// 已驳回状态重新发布
const handleRePublish = async (row) => {
  try {
    await ElMessageBox.confirm('确定要重新发布该知识吗？', '提示', {
      type: 'info'
    })
    // 使用发布接口，确保更新索引
    const res = await api.post(`/knowledge/${row.id}/publish`)
    if (res.code === 200) {
      ElMessage.success('重新发布成功')
      row.status = 'APPROVED'
      loadData()
    } else {
      ElMessage.error(res.message || '重新发布失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重新发布失败')
    }
  }
}

// 打开高级合并审核页面
const openMergeReviewPage = () => {
  if (!currentKnowledge.value) {
    ElMessage.warning('请选择要审核的知识')
    return
  }
  const version = currentAudit.value?.version || currentKnowledge.value.version
  showApproveDialogVisible.value = false
  router.push({
    path: `/knowledge/${currentKnowledge.value.id}/merge`,
    query: { draftVersion: version }
  })
}

const doApprove = async () => {
  if (!currentKnowledge.value) {
    ElMessage.warning('请选择要审核的知识')
    return
  }
  
  // Conflict sanity check
  if (mergeStatus.value && mergeStatus.value.hasConflict && !resolvedContent.value) {
       ElMessage.warning('检测到冲突，请先解决冲突')
       return
  }
  
  auditing.value = true
  try {
    let res
    
    // 如果有审核记录，使用审核接口
    if (currentAudit.value && currentAudit.value.id) {
      // Pass resolvedContent if exists
      const params = {
          auditorId: userInfo.value.id,
          comment: approveComment.value || '审核通过'
      }
      if (resolvedContent.value) {
          // Pass in body or ensure backend accepts resolvedContent in params?
          // Based on backend impl: approve(id, uid, comment, resolvedContent)
          // We likely need to send it in body or query param. Backend used @RequestParam often but let's check
          // The backend method signature is just Java method params. Let's see Controller.
          // Assuming we need to add it to params for now.
          params.resolvedContent = resolvedContent.value
      }
      
      res = await api.post(`/knowledge/audit/${currentAudit.value.id}/approve`, null, {
        params: params
      })
    } else {
      // 没有审核记录时，先创建审核记录，再审核通过
      // 1. 提交审核创建记录
      const submitRes = await api.post(`/knowledge/${currentKnowledge.value.id}/submit-audit?userId=${userInfo.value.id}`)
      if (submitRes.code === 200 && submitRes.data && submitRes.data.id) {
        // 2. 审核通过
        res = await api.post(`/knowledge/audit/${submitRes.data.id}/approve`, null, {
          params: {
            auditorId: userInfo.value.id,
            comment: approveComment.value || '审核通过'
          }
        })
      } else {
        // 如果提交审核也失败，尝试直接发布
        res = await api.post(`/knowledge/${currentKnowledge.value.id}/publish`)
      }
    }
    
    if (res.code === 200) {
      ElMessage.success('审核通过')
      showApproveDialogVisible.value = false
      currentKnowledge.value = null
      currentAudit.value = null
      approveComment.value = ''
      loadData()
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
  if (!currentKnowledge.value || !currentAudit.value) {
    ElMessage.warning('审核信息不完整，请刷新后重试')
    return
  }
  
  if (!rejectComment.value || rejectComment.value.trim().length === 0) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  
  auditing.value = true
  try {
    const res = await api.post(`/knowledge/audit/${currentAudit.value.id}/reject`, null, {
      params: {
        auditorId: userInfo.value.id,
        comment: rejectComment.value
      }
    })
    
    if (res.code === 200) {
      ElMessage.success('已驳回')
      showRejectDialogVisible.value = false
      currentKnowledge.value = null
      currentAudit.value = null
      rejectComment.value = ''
      loadData()
    } else {
      ElMessage.error(res.message || '驳回失败')
    }
  } catch (error) {
    ElMessage.error('驳回失败: ' + (error.response?.data?.message || error.message))
  } finally {
    auditing.value = false
  }
}

// 加载审核信息
const loadAuditInfo = async (knowledge) => {
  // 只有待审核状态或有草稿的知识才需要加载审核信息
  if (knowledge.status !== 'PENDING' && !knowledge.hasDraft) return
  
  try {
    // 获取所有待审核记录，找到该知识的审核记录
    const res = await api.get('/knowledge/audit/pending')
    if (res.code === 200 && res.data) {
      const audits = Array.isArray(res.data) ? res.data : [res.data]
      // 优先匹配版本号，如果没有版本号则匹配knowledgeId
      let audit = audits.find(a => 
        String(a.knowledgeId) === String(knowledge.id) && 
        a.status === 'PENDING' &&
        (String(a.version) === String(knowledge.version) || !a.version)
      )
      if (!audit) {
        // 如果按版本找不到，尝试只按knowledgeId查找
        audit = audits.find(a => String(a.knowledgeId) === String(knowledge.id) && a.status === 'PENDING')
      }
      if (audit) {
        // 提交人姓名现在从后端返回
        // 设置提交时间
        if (!audit.submitTime && audit.createTime) {
          audit.submitTime = audit.createTime
        }
        knowledge.auditInfo = audit
      }
    }
  } catch (error) {
    console.error('加载审核信息失败', error)
  }
}

// 预览待审核版本内容
const previewVersionContent = () => {
  if (!currentKnowledge.value) return
  
  const knowledgeId = currentKnowledge.value.id
  const version = currentAudit.value?.version || currentKnowledge.value.version
  
  // 跳转到知识详情页，并带上版本参数
  router.push({
    path: `/knowledge/${knowledgeId}`,
    query: { previewVersion: version }
  })
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

// 批量删除
const batchDelete = async () => {
  if (!selectedItems.value || selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要删除的知识')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedItems.value.length} 个知识吗？`, '批量删除', {
      type: 'warning'
    })
    
    // 逐个删除
    let successCount = 0
    let failCount = 0
    for (const item of selectedItems.value) {
      try {
        const res = await api.delete(`/knowledge/${item.id}`)
        if (res.code === 200) {
          successCount++
        } else {
          failCount++
        }
      } catch (error) {
        failCount++
      }
    }
    
    if (failCount === 0) {
      ElMessage.success(`成功删除 ${successCount} 个知识`)
    } else {
      ElMessage.warning(`删除完成：成功 ${successCount} 个，失败 ${failCount} 个`)
    }
    
    // 清空选择
    selectedItems.value = []
    if (tableRef.value) {
      tableRef.value.clearSelection()
    }
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  }
}

// 批量通过审核
const batchApprove = async () => {
  const pendingItems = selectedItems.value.filter(item => item.status === 'PENDING')
  if (pendingItems.length === 0) {
    ElMessage.warning('没有待审核的知识')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要通过选中的 ${pendingItems.length} 个知识的审核吗？`, '批量通过', {
      type: 'success',
      confirmButtonText: '确认通过',
      cancelButtonText: '取消'
    })
    
    batchAuditing.value = true
    let successCount = 0
    let failCount = 0
    
    for (const item of pendingItems) {
      try {
        // 调用审核通过接口
        const res = await api.put(`/knowledge/${item.id}/status`, null, {
          params: { status: 'APPROVED' }
        })
        if (res.code === 200) {
          successCount++
        } else {
          failCount++
        }
      } catch (error) {
        failCount++
      }
    }
    
    if (failCount === 0) {
      ElMessage.success(`成功通过 ${successCount} 个知识的审核`)
    } else {
      ElMessage.warning(`审核完成：成功 ${successCount} 个，失败 ${failCount} 个`)
    }
    
    // 清空选择并刷新
    selectedItems.value = []
    if (tableRef.value) {
      tableRef.value.clearSelection()
    }
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量审核失败')
    }
  } finally {
    batchAuditing.value = false
  }
}

// 显示批量拒绝对话框
const showBatchRejectDialog = () => {
  const pendingItems = selectedItems.value.filter(item => item.status === 'PENDING')
  if (pendingItems.length === 0) {
    ElMessage.warning('没有待审核的知识')
    return
  }
  batchRejectComment.value = ''
  showBatchRejectDialogVisible.value = true
}

// 执行批量拒绝
const doBatchReject = async () => {
  if (!batchRejectComment.value || batchRejectComment.value.trim().length === 0) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  
  const pendingItems = selectedItems.value.filter(item => item.status === 'PENDING')
  if (pendingItems.length === 0) {
    ElMessage.warning('没有待审核的知识')
    return
  }
  
  batchAuditing.value = true
  let successCount = 0
  let failCount = 0
  
  try {
    for (const item of pendingItems) {
      try {
        // 调用审核拒绝接口
        const res = await api.put(`/knowledge/${item.id}/status`, null, {
          params: { 
            status: 'REJECTED',
            comment: batchRejectComment.value
          }
        })
        if (res.code === 200) {
          successCount++
        } else {
          failCount++
        }
      } catch (error) {
        failCount++
      }
    }
    
    if (failCount === 0) {
      ElMessage.success(`成功拒绝 ${successCount} 个知识`)
    } else {
      ElMessage.warning(`拒绝完成：成功 ${successCount} 个，失败 ${failCount} 个`)
    }
    
    // 关闭对话框并刷新
    showBatchRejectDialogVisible.value = false
    batchRejectComment.value = ''
    selectedItems.value = []
    if (tableRef.value) {
      tableRef.value.clearSelection()
    }
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '批量拒绝失败')
  } finally {
    batchAuditing.value = false
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
  
  // 不再强制要求分类，因为主要使用树结构
  
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
      
      // 处理 parentId：如果选择了知识结构，使用选择的 parentId
      let actualParentId = uploadForm.value.parentId
      if (actualParentId !== null && actualParentId !== undefined) {
        // 如果是负数或字符串格式，需要标准化
        if (typeof actualParentId === 'string' && actualParentId.startsWith('dept-')) {
          actualParentId = null
        } else if (typeof actualParentId === 'number' && actualParentId < 0) {
          actualParentId = null
        }
      }
      
      // 所有上传的知识初始状态都是待审核
      const knowledgeRes = await api.post('/knowledge', {
        title: fileNameWithoutExt,
        content: fileContent,
        summary: uploadForm.value.summary || `上传的文件：${file.name}`,
        keywords: uploadForm.value.keywords || '',
        fileId: fileDTO.id,
        parentId: actualParentId,
        author: userInfo.realName || userInfo.username || '未知',
        department: userInfo.department || '未知',
        createBy: userInfo.username,
        status: 'PENDING' // 所有上传的知识都是待审核状态
      })
      
      if (knowledgeRes.code !== 200) {
        throw new Error(knowledgeRes.message || '创建知识条目失败')
      }
      
      // 创建知识后，自动提交审核
      try {
        await api.post(`/knowledge/${knowledgeRes.data.id}/submit-audit?userId=${userInfo.id}`)
      } catch (error) {
        console.warn('提交审核失败，但知识已创建', error)
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
    parentId: null,
    summary: '',
    keywords: '',
    parentPath: ''
  }
  uploading.value = false
  uploadProgress.value = 0
  currentFileName.value = ''
}

// 加载知识树（用于父节点选择器）
const loadKnowledgeTree = async () => {
  if (treeLoading.value) return
  treeLoading.value = true
  try {
    // 使用 listKnowledge API 获取所有状态的知识（包括 PENDING 和 APPROVED）
    // 这样新创建的文件夹（即使是 PENDING 状态）也能显示在文件夹选择器中
    const requestParams = {
      // 不传 status 参数，获取所有状态的知识
      includeFolders: 'true', // 包含文件夹
      pageSize: 10000 // 设置一个较大的值以获取所有数据
    }
    console.log('=== 加载知识树请求参数 ===', requestParams)
    const res = await api.get('/knowledge/list', { params: requestParams })
    console.log('=== 加载知识树响应 ===', {
      code: res.code,
      message: res.message,
      dataLength: res.data?.length,
      data: res.data
    })
    
    if (res.code === 200) {
      const rawData = res.data || []
      console.log('=== 加载知识树原始数据（所有状态）===', {
        totalCount: rawData.length,
        folders: rawData.filter(item => !item.fileId),
        files: rawData.filter(item => item.fileId),
        allData: rawData
      })
      knowledgeTree.value = rawData
      
      // 构建树形结构用于 el-tree-select
      const builtTree = buildTreeForSelect(rawData)
      console.log('=== 构建后的文件夹树 ===', {
        treeRootCount: builtTree.length,
        tree: builtTree
      })
      
      // 强制清空并重新赋值，确保响应式更新
      parentNodeOptions.value = []
      await nextTick()
      parentNodeOptions.value = builtTree
      
      // 更新树组件的key以强制刷新
      folderSelectTreeKey.value++
      
      // 再次等待确保DOM更新
      await nextTick()
      console.log('最终 parentNodeOptions:', parentNodeOptions.value)
    } else {
      console.error('加载知识树失败，响应码:', res.code, res.message)
    }
  } catch (error) {
    console.error('加载知识树失败', error)
    ElMessage.error('加载知识树失败：' + (error.message || '未知错误'))
  } finally {
    treeLoading.value = false
  }
}

// 判断是否为文件夹
const isFolder = (item) => {
  // 部门根节点始终是文件夹
  if (item.isDepartmentRoot || (item.id && String(item.id).startsWith('dept-'))) {
    return true
  }
  // 如果有子节点，肯定是文件夹
  if (item.children && item.children.length > 0) {
    return true
  }
  // 如果没有fileId，说明是文件夹
  return item.fileId == null || item.fileId === undefined
}

// 构建树形结构（用于 el-tree-select，只包含文件夹）
const buildTreeForSelect = (list) => {
  console.log('=== buildTreeForSelect 开始 ===', {
    inputLength: list?.length,
    inputData: list?.map(item => ({
      id: item.id,
      title: item.title,
      parentId: item.parentId,
      fileId: item.fileId,
      status: item.status
    }))
  })
  if (!list || list.length === 0) {
    console.log('buildTreeForSelect: 输入列表为空')
    return []
  }
  
  // 先过滤出所有文件夹（排除部门根节点，因为它们是虚拟节点）
  const folders = list.filter(item => {
    // 排除部门根节点（负数ID或isDepartmentRoot标记）
    if (item.isDepartmentRoot || (item.id && (item.id < 0 || String(item.id).startsWith('dept-')))) {
      console.log('跳过部门根节点:', item.id, item.title)
      return false
    }
    const isFolderResult = isFolder(item)
    if (isFolderResult) {
      console.log('发现文件夹:', { id: item.id, title: item.title, parentId: item.parentId, fileId: item.fileId })
    }
    return isFolderResult
  })
  
  console.log('=== 过滤后的文件夹列表 ===', {
    folderCount: folders.length,
    folders: folders.map(f => ({
      id: f.id,
      title: f.title,
      parentId: f.parentId,
      fileId: f.fileId,
      status: f.status
    }))
  })
  
  const map = {}
  const roots = []
  
  // 创建映射（只包含文件夹）
  folders.forEach(item => {
    map[item.id] = { 
      id: item.id,
      title: item.title,
      parentId: item.parentId,
      sortOrder: item.sortOrder || 0,
      children: [] 
    }
  })
  
  console.log('=== 文件夹映射 ===', {
    mapSize: Object.keys(map).length,
    map: Object.keys(map).reduce((acc, key) => {
      acc[key] = {
        id: map[key].id,
        title: map[key].title,
        parentId: map[key].parentId,
        childrenCount: map[key].children.length
      }
      return acc
    }, {})
  })
  
  // 构建树（只包含文件夹）
  folders.forEach(item => {
    if (item.parentId) {
      const parent = map[item.parentId]
      if (parent) {
        parent.children.push(map[item.id])
        console.log(`文件夹 ${item.title} (${item.id}) 添加到父节点 ${parent.title} (${parent.id})`)
      } else {
        // 如果父节点不在文件夹列表中，这个文件夹应该作为根节点
        console.warn(`文件夹 ${item.title} (${item.id}) 的父节点 ${item.parentId} 不在文件夹列表中，作为根节点`)
        roots.push(map[item.id])
      }
    } else {
      console.log(`文件夹 ${item.title} (${item.id}) 是根节点`)
      roots.push(map[item.id])
    }
  })
  
  console.log('=== 构建的根节点 ===', {
    rootCount: roots.length,
    roots: roots.map(r => ({
      id: r.id,
      title: r.title,
      childrenCount: r.children.length,
      children: r.children.map(c => ({ id: c.id, title: c.title }))
    }))
  })
  
  // 按sortOrder排序
  const sortChildren = (nodes) => {
    nodes.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    nodes.forEach(node => {
      if (node.children && node.children.length > 0) {
        sortChildren(node.children)
      }
    })
  }
  
  sortChildren(roots)
  console.log('排序后的根节点:', roots)
  return roots
}

// 处理文件夹选择（在对话框中选择）
const handleFolderSelect = (data) => {
  selectedFolderId.value = data.id
  // 获取节点路径
  const getNodePath = (nodeId, nodes, path = []) => {
    for (const node of nodes) {
      if (node.id === nodeId) {
        return [...path, node.title].join(' / ')
      }
      if (node.children && node.children.length > 0) {
        const found = getNodePath(nodeId, node.children, [...path, node.title])
        if (found) return found
      }
    }
    return null
  }
  selectedFolderPath.value = getNodePath(data.id, parentNodeOptions.value) || data.title
}

// 确认文件夹选择
const confirmFolderSelection = () => {
  if (selectedFolderId.value) {
    uploadForm.value.parentId = selectedFolderId.value
    uploadForm.value.parentPath = selectedFolderPath.value
  }
  showFolderSelectDialog.value = false
}

// 清除文件夹选择
const clearFolderSelection = () => {
  uploadForm.value.parentId = null
  uploadForm.value.parentPath = ''
  selectedFolderId.value = null
  selectedFolderPath.value = ''
  if (folderSelectTreeRef.value) {
    folderSelectTreeRef.value.setCurrentKey(null)
  }
  if (showFolderSelectDialog.value) {
    showFolderSelectDialog.value = false
  }
}

// 显示文件夹选择器右键菜单
const showFolderContextMenu = (event, node, data) => {
  event.stopPropagation()
  folderContextMenuX.value = event.clientX
  folderContextMenuY.value = event.clientY
  folderContextMenuData.value = data
  console.log('=== 文件夹右键菜单 - 节点数据 ===', {
    nodeData: data,
    nodeId: data?.id,
    nodeTitle: data?.title,
    folderContextMenuData: folderContextMenuData.value
  })
  folderContextMenuVisible.value = true
  
  // 点击其他地方关闭菜单
  const closeMenu = (e) => {
    if (!e.target.closest('.folder-context-menu')) {
      folderContextMenuVisible.value = false
      document.removeEventListener('click', closeMenu)
    }
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 在空白处显示右键菜单
const showFolderContextMenuOnBlank = (event) => {
  folderContextMenuX.value = event.clientX
  folderContextMenuY.value = event.clientY
  folderContextMenuData.value = null
  folderContextMenuVisible.value = true
  
  const closeMenu = (e) => {
    if (!e.target.closest('.folder-context-menu')) {
      folderContextMenuVisible.value = false
      document.removeEventListener('click', closeMenu)
    }
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 处理文件夹选择器右键菜单点击
const handleFolderContextMenuClick = (action) => {
  folderContextMenuVisible.value = false
  
  if (action === 'newFolder') {
    console.log('=== 点击新建文件夹菜单 ===', {
      folderContextMenuData: folderContextMenuData.value,
      folderContextMenuDataId: folderContextMenuData.value?.id,
      folderContextMenuDataTitle: folderContextMenuData.value?.title
    })
    // 设置父节点ID
    if (folderContextMenuData.value) {
      newFolderForm.value.parentId = folderContextMenuData.value.id
      console.log('设置父节点ID:', folderContextMenuData.value.id)
    } else {
      newFolderForm.value.parentId = null
      console.log('父节点数据为空，设置为null')
    }
    console.log('新建文件夹表单:', {
      parentId: newFolderForm.value.parentId,
      title: newFolderForm.value.title
    })
    newFolderForm.value.title = ''
    showNewFolderDialog.value = true
  }
}

// 确认新建文件夹
const confirmNewFolder = async () => {
  if (!newFolderForm.value.title.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  
  try {
    // 获取用户信息
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    
    // 创建文件夹（文件夹没有fileId）
    const knowledgeData = {
      title: newFolderForm.value.title.trim(),
      parentId: newFolderForm.value.parentId,
      summary: '',
      keywords: '',
      fileId: null, // 文件夹没有文件ID
      status: userInfo.role === 'ADMIN' || userInfo.role === 'EDITOR' ? 'APPROVED' : 'PENDING'
    }
    
    console.log('=== 创建文件夹请求数据 ===', JSON.stringify(knowledgeData, null, 2))
    const res = await api.post('/knowledge', knowledgeData)
    console.log('=== 创建文件夹响应数据 ===', JSON.stringify(res, null, 2))
    console.log('=== 创建文件夹响应数据详情 ===', {
      code: res.code,
      message: res.message,
      data: res.data,
      dataId: res.data?.id,
      dataParentId: res.data?.parentId,
      dataFileId: res.data?.fileId,
      dataTitle: res.data?.title,
      dataStatus: res.data?.status
    })
    
    if (res.code === 200) {
      // 如果是待审核状态，自动提交审核
      if (knowledgeData.status === 'PENDING') {
        try {
          await api.post(`/knowledge/${res.data.id}/submit-audit?userId=${userInfo.id}`)
        } catch (error) {
          console.warn('提交审核失败，但文件夹已创建', error)
        }
      }
      
      ElMessage.success('文件夹创建成功')
      showNewFolderDialog.value = false
      const createdFolderId = res.data.id
      const createdParentId = newFolderForm.value.parentId
      console.log('=== 创建的文件夹信息 ===', {
        createdFolderId,
        createdParentId,
        folderData: res.data
      })
      newFolderForm.value = { title: '', parentId: null }
      
      // 强制刷新文件夹树 - 添加延迟确保后端数据已更新
      await new Promise(resolve => setTimeout(resolve, 500))
      await loadKnowledgeTree()
      
      // 等待 DOM 更新，确保树组件数据已刷新
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 200))
      await nextTick()
      
      // 如果文件夹选择对话框是打开的，需要更新树显示
      if (showFolderSelectDialog.value && folderSelectTreeRef.value) {
        console.log('刷新文件夹选择树，当前 parentNodeOptions:', parentNodeOptions.value)
        
        // 如果有父节点，需要展开父节点以显示新创建的子文件夹
        if (createdParentId) {
          console.log('展开父节点:', createdParentId)
          try {
            // 使用 store 的 setExpanded 方法展开父节点
            folderSelectTreeRef.value.store.setExpanded(createdParentId, true)
            // 等待展开动画完成
            await nextTick()
            await new Promise(resolve => setTimeout(resolve, 300))
            
            // 递归展开所有祖先节点（如果需要）
            const expandParentChain = async (parentId) => {
              if (!parentId) return
              try {
                folderSelectTreeRef.value.store.setExpanded(parentId, true)
                await nextTick()
                // 查找父节点的父节点
                const parentNode = folderSelectTreeRef.value.getNode(parentId)
                if (parentNode && parentNode.data && parentNode.data.parentId) {
                  await expandParentChain(parentNode.data.parentId)
                }
              } catch (e) {
                console.warn('展开父节点链失败:', e)
              }
            }
            await expandParentChain(createdParentId)
          } catch (e) {
            console.warn('展开父节点失败:', e)
          }
        }
        
        // 获取节点路径的辅助函数
        const getNodePath = (nodeId, nodes, path = []) => {
          for (const node of nodes) {
            if (node.id === nodeId) {
              return [...path, node.title].join(' / ')
            }
            if (node.children && node.children.length > 0) {
              const found = getNodePath(nodeId, node.children, [...path, node.title])
              if (found) return found
            }
          }
          return null
        }
        
        // 再次等待确保树已经完全更新
        await nextTick()
        await new Promise(resolve => setTimeout(resolve, 200))
        
        // 尝试选中新创建的文件夹
        try {
          selectedFolderId.value = createdFolderId
          selectedFolderPath.value = getNodePath(createdFolderId, parentNodeOptions.value) || res.data.title
          
          // 高亮选中的节点
          folderSelectTreeRef.value.setCurrentKey(createdFolderId)
          
          // 滚动到选中的节点
          await nextTick()
          await new Promise(resolve => setTimeout(resolve, 100))
          const node = folderSelectTreeRef.value.getNode(createdFolderId)
          if (node && node.$el) {
            node.$el.scrollIntoView({ behavior: 'smooth', block: 'center' })
          } else {
            console.warn('无法找到新创建的文件夹节点，可能需要手动刷新')
          }
        } catch (e) {
          console.warn('选中节点失败:', e)
        }
      }
    } else {
      ElMessage.error(res.message || '创建文件夹失败')
    }
  } catch (error) {
    console.error('创建文件夹失败', error)
    ElMessage.error('创建文件夹失败：' + (error.message || '未知错误'))
  }
}

// 操作列宽度已固定为280，不再需要动态计算

onMounted(() => {
  // 知识管理页面显示所有状态的知识
  loadData()
})
</script>

<style scoped>
.knowledge-management {
  padding: 24px;
  background-color: #f6f8fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
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

.control-card {
  margin-bottom: 20px;
  border: none;
}

.control-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
  justify-content: space-between;
}

.search-section {
  flex: 1;
  min-width: 300px;
  max-width: 500px;
}

.search-input {
  width: 100%;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  border: none;
  border-radius: 8px;
}

/* Table Toolbar */
.table-toolbar {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selected-count {
  font-size: 13px;
  color: #909399;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

/* Adjust table styles */
:deep(.el-table) {
  --el-table-header-bg-color: #f8f9fa;
  --el-table-header-text-color: #606266;
}

/* Pagination */
.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: flex-end;
}

/* Suggestion Item */
.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}
.suggestion-title {
  color: #303133;
}

/* Dialogs */
.upload-form {
  margin-top: 20px;
}

.upload-progress {
  margin: 20px 0;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 8px;
}

/* Audit Styles */
.audit-cell {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.ops-cell {
  display: flex;
  justify-content: center;
  gap: 8px;
}

/* Knowledge Details */
.knowledge-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.knowledge-title {
  font-weight: 600;
  color: #303133;
}

.knowledge-meta-tags {
  display: inline-flex;
}

.knowledge-summary {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.5;
}

.knowledge-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Highlight */
.highlight-title :deep(mark),
.highlight-fragment :deep(mark) {
  background-color: #fff566;
  color: #303133;
  padding: 0 2px;
  border-radius: 2px;
}

/* Folder Select */
.folder-select-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.selected-folder-info {
  margin-top: 12px;
  padding: 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Context Menu */
.folder-context-menu {
  position: fixed;
  background: white;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  border-radius: 4px;
  padding: 5px 0;
  z-index: 3000;
}

.context-menu-item {
  padding: 8px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.context-menu-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.match-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
  margin-bottom: 4px;
  background-color: #f0f9ff;
  padding: 2px 6px;
  border-radius: 4px;
  width: fit-content;
}
</style>

