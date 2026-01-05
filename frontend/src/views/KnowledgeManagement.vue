<template>
  <div class="knowledge-list">
    <div class="page-header">
      <h2>知识管理</h2>
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
          </div>
        </template>
        <template #append>
          <el-button @click="handleSearch" size="large">搜索</el-button>
        </template>
      </el-autocomplete>
    </div>

    <div class="filter-bar">
      <div class="filter-left">
      <!-- 知识管理页面默认显示全部状态 -->
      <el-select 
        v-model="filters.status" 
        placeholder="全部状态" 
        style="width: 150px"
        @change="handleFilterChange"
      >
        <el-option label="全部状态" :value="null" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="已发布" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
        <el-input
          v-model="filters.author"
          placeholder="作者筛选"
          clearable
          style="width: 150px; margin-left: 10px"
          @clear="handleFilterChange"
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 240px; margin-left: 10px"
          @change="handleFilterChange"
        />
        <el-button 
          type="primary" 
          style="margin-left: 10px"
          @click="handleSearch"
        >
          搜索
        </el-button>
        <el-button 
          @click="clearFilters"
          style="margin-left: 10px"
        >
          清空筛选
        </el-button>
      </div>
    </div>

    <el-card class="table-card">
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
        </template>
      </el-table-column>
      <!-- 审核列 -->
      <el-table-column label="审核" width="130" align="center">
        <template #default="scope">
          <div class="audit-cell">
            <!-- 待审核状态 -->
            <template v-if="scope.row.status === 'PENDING'">
              <el-tooltip content="通过" placement="top">
                <el-button type="success" size="small" circle :icon="Check" @click.stop="handleAuditApprove(scope.row)" />
              </el-tooltip>
              <el-tooltip content="驳回" placement="top">
                <el-button type="danger" size="small" circle :icon="Close" @click.stop="handleAuditReject(scope.row)" />
              </el-tooltip>
            </template>
            <!-- 已发布状态 -->
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

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="700px" @close="handleUploadDialogClose" @open="loadKnowledgeTree">
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
        <el-form :model="uploadForm" label-width="120px">
          <el-form-item label="存放的知识结构">
            <el-input 
              v-model="uploadForm.parentPath" 
              placeholder="请选择存放的知识结构（可选）" 
              readonly
              style="cursor: default;"
            >
              <template #append>
                <el-button @click="showFolderSelectDialog = true" :icon="FolderOpened">...</el-button>
              </template>
            </el-input>
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
          :disabled="fileList.length === 0 || uploading"
          :loading="uploading"
        >
          开始上传
        </el-button>
      </template>
    </el-dialog>

    <!-- 文件夹选择对话框 -->
    <el-dialog v-model="showFolderSelectDialog" title="选择存放位置" width="600px">
      <div class="folder-select-container" @contextmenu.prevent="showFolderContextMenuOnBlank">
        <el-tree
          :key="folderSelectTreeKey"
          ref="folderSelectTreeRef"
          :data="parentNodeOptions"
          :props="{ label: 'title', children: 'children' }"
          node-key="id"
          :default-expand-all="false"
          :highlight-current="true"
          @node-click="handleFolderSelect"
          class="folder-select-tree"
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <div 
              class="folder-select-node"
              @contextmenu.prevent="showFolderContextMenu($event, node, data)"
            >
              <el-icon class="folder-icon"><Folder /></el-icon>
              <span class="folder-title">{{ data.title }}</span>
            </div>
          </template>
        </el-tree>
      </div>
      <div v-if="selectedFolderPath" class="selected-folder-info">
        <el-icon><FolderOpened /></el-icon>
        <span>已选择：{{ selectedFolderPath }}</span>
      </div>
      <template #footer>
        <el-button @click="clearFolderSelection">清除选择</el-button>
        <el-button type="primary" @click="confirmFolderSelection">确定</el-button>
      </template>
    </el-dialog>

    <!-- 文件夹选择器右键菜单 -->
    <div
      v-if="folderContextMenuVisible"
      class="folder-context-menu"
      :style="{ left: folderContextMenuX + 'px', top: folderContextMenuY + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="handleFolderContextMenuClick('newFolder')">
        <el-icon><Folder /></el-icon>
        <span>新建文件夹</span>
      </div>
    </div>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" width="400px">
      <el-form :model="newFolderForm" label-width="80px">
        <el-form-item label="文件夹名称" required>
          <el-input 
            v-model="newFolderForm.title" 
            placeholder="请输入文件夹名称"
            @keyup.enter="confirmNewFolder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNewFolderDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmNewFolder" :disabled="!newFolderForm.title.trim()">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核通过对话框 -->
    <el-dialog v-model="showApproveDialogVisible" title="审核通过" width="600px">
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
        <el-button type="success" @click="doApprove" :loading="auditing">审核通过</el-button>
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
import { Upload, UploadFilled, Search, Location, View, Star, Clock, User, ArrowDown, Check, Close, Folder, FolderOpened } from '@element-plus/icons-vue'
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
  return selectedItems.value.some(item => item.status === 'PENDING')
})

// 搜索历史
const searchHistory = ref([])
const showSearchHistory = ref(false)

// 上传相关
const showUploadDialog = ref(false)
const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const currentFileName = ref('')
const uploadForm = ref({
  parentId: null, // 父节点ID，null表示根节点
  summary: '',
  keywords: '',
  parentPath: '' // 存放的知识结构路径（显示用）
})

// 知识树相关
const knowledgeTree = ref([])
const treeLoading = ref(false)
const parentNodeOptions = ref([]) // 父节点选择器选项（只包含文件夹）
const folderSelectTreeRef = ref(null) // 文件夹选择树引用
const folderSelectTreeKey = ref(0) // 树组件的key，用于强制刷新
const showFolderSelectDialog = ref(false) // 文件夹选择对话框
const selectedFolderId = ref(null) // 临时选中的文件夹ID
const selectedFolderPath = ref('') // 临时选中的文件夹路径

// 文件夹选择器右键菜单
const folderContextMenuVisible = ref(false)
const folderContextMenuX = ref(0)
const folderContextMenuY = ref(0)
const folderContextMenuData = ref(null) // 右键点击的节点数据

// 新建文件夹对话框
const showNewFolderDialog = ref(false)
const newFolderForm = ref({
  title: '',
  parentId: null
})

// 审核相关
const showApproveDialogVisible = ref(false)
const showRejectDialogVisible = ref(false)
const currentKnowledge = ref(null)
const currentAudit = ref(null)
const approveComment = ref('')
const rejectComment = ref('')
const auditing = ref(false)

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

      knowledgeList.value = results
      total.value = results.length
    } else {
      // 使用列表接口
      res = await api.get('/knowledge/list', { params: searchParams })
      // 过滤掉文件夹（只显示有fileId的文件）
      const results = (res.data || [])

      knowledgeList.value = results
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
  // ADMIN可以编辑所有知识
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // EDITOR只能编辑自己创建的知识
  if (hasRole(userInfo.value, ROLE_EDITOR) && row.author === userInfo.value.realName) {
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
  // 只有管理员可以删除知识
  return hasRole(userInfo.value, ROLE_ADMIN)
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
  showApproveDialogVisible.value = true
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
  showApproveDialogVisible.value = true
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

const doApprove = async () => {
  if (!currentKnowledge.value) {
    ElMessage.warning('请选择要审核的知识')
    return
  }
  
  auditing.value = true
  try {
    let res
    
    // 如果有审核记录，使用审核接口
    if (currentAudit.value && currentAudit.value.id) {
      res = await api.post(`/knowledge/audit/${currentAudit.value.id}/approve`, null, {
        params: {
          auditorId: userInfo.value.id,
          comment: approveComment.value || '审核通过'
        }
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
        a.knowledgeId === knowledge.id && 
        a.status === 'PENDING' &&
        (a.version === knowledge.version || !a.version)
      )
      if (!audit) {
        // 如果按版本找不到，尝试只按knowledgeId查找
        audit = audits.find(a => a.knowledgeId === knowledge.id && a.status === 'PENDING')
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

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selected-count {
  font-size: 14px;
  color: #606266;
}

.toolbar-right {
  display: flex;
  gap: 10px;
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

.suggestion-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.suggestion-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.history-icon {
  color: #909399;
  font-size: 14px;
}

/* 操作按钮布局 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
}

/* 审核单元格样式 */
.audit-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.audit-cell .el-button {
  width: 40px;
  height: 40px;
  padding: 0;
  font-size: 18px;
}

.audit-cell .audit-btn-disabled {
  background: #e4e7ed !important;
  border-color: #e4e7ed !important;
  color: #c0c4cc !important;
  cursor: not-allowed;
}

/* 操作单元格样式 */
.ops-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-wrap: wrap;
}

/* 审核操作按钮样式（兼容旧代码） */
.audit-action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0;
}

.audit-btn {
  width: 48px;
  height: 48px;
  font-size: 20px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

.audit-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.approve-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
}

.approve-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
}

.reject-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  border: none;
}

.reject-btn:hover {
  background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
}

/* 已发布状态的灰色√按钮（不可点击） */
.approved-btn-disabled {
  background: #c0c4cc !important;
  border-color: #c0c4cc !important;
  cursor: not-allowed;
  opacity: 0.6;
}

.approved-btn-disabled:hover {
  transform: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 已驳回状态的灰色×按钮（不可点击） */
.rejected-btn-disabled {
  background: #c0c4cc !important;
  border-color: #c0c4cc !important;
  cursor: not-allowed;
  opacity: 0.6;
}

.rejected-btn-disabled:hover {
  transform: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.withdraw-btn {
  width: 100%;
  font-weight: 500;
}

.no-audit-action {
  color: #909399;
  font-size: 14px;
  text-align: center;
  display: block;
  width: 100%;
}

/* 审核操作按钮样式 */
.audit-action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0;
}

.audit-btn {
  width: 48px;
  height: 48px;
  font-size: 20px;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

.audit-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.approve-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
}

.approve-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
}

.reject-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  border: none;
}

.reject-btn:hover {
  background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
}

.withdraw-btn {
  width: 100%;
  font-weight: 500;
}

.no-audit-action {
  color: #909399;
  font-size: 14px;
  text-align: center;
  display: block;
  width: 100%;
}

.keyword-icon {
  color: #409eff;
  font-size: 14px;
}

.knowledge-icon {
  color: #67c23a;
  font-size: 14px;
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

/* 文件夹选择器右键菜单样式 */
.folder-context-menu {
  position: fixed;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 3000;
  min-width: 160px;
}

.folder-context-menu .context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: background-color 0.2s;
}

.folder-context-menu .context-menu-item:hover {
  background-color: #f5f7fa;
}

.folder-context-menu .context-menu-item .el-icon {
  font-size: 16px;
  color: #409eff;
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

/* 审核信息样式 */
.audit-info {
  font-size: 12px;
  color: #606266;
}

.audit-info .meta-text {
  color: #909399;
  margin-top: 4px;
}

.audit-dialog-content {
  padding: 10px 0;
}

.audit-dialog-content h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.knowledge-preview {
  margin-bottom: 20px;
}

/* 文件夹选择器样式 */
.folder-select-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px;
  background-color: #fff;
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.folder-select-tree {
  margin: 0;
}

.folder-select-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding: 4px 0;
}

.folder-icon {
  color: #409eff;
  font-size: 16px;
}

.folder-title {
  font-size: 14px;
  color: #303133;
}

.selected-folder-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  font-size: 14px;
  color: #409eff;
  margin-top: 12px;
}

.selected-folder-info .el-icon {
  font-size: 16px;
}
</style>

