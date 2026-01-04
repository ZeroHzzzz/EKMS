<template>
  <div class="knowledge-search-container">
    <!-- 顶部导航/操作栏 (仅在有搜索结果或非初始状态显示) -->
    <div class="top-bar" v-if="hasSearched || !isInitialState">
       <div class="brand" @click="resetSearch">
         <h2>知识库</h2>
       </div>
       <div class="search-box-mini">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索知识..."
            class="search-input-mini"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
       </div>
       <div class="actions">
         <el-button 
            v-if="hasPermission(userInfo, 'UPLOAD')" 
            type="primary" 
            link
            @click="showUploadDialog = true"
          >
            上传文档
          </el-button>
       </div>
    </div>

    <!-- 初始搜索界面 (居中) -->
    <div class="initial-search-view" v-else>
      <div class="brand-large">
        <h1>企业知识库</h1>
        <p class="subtitle">Search anything you need</p>
      </div>
      <div class="search-box-large">
        <el-input
            v-model="searchKeyword"
            placeholder="在此输入关键词搜索..."
            class="search-input-large"
            size="large"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon class="search-icon-large"><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleSearch" class="search-btn-large">搜索</el-button>
            </template>
        </el-input>
      </div>
      <div class="search-tags" v-if="historyTags.length > 0">
        <span class="tag-label">最近搜索：</span>
        <el-tag 
          v-for="tag in historyTags" 
          :key="tag" 
          class="history-tag" 
          effect="plain" 
          round
          @click="quickSearch(tag)"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>

    <!-- 搜索结果展示区 -->
    <div class="search-results-wrapper" v-if="hasSearched">
      <div class="filter-sidebar">
        <div class="filter-group">
           <div class="filter-title">发布时间</div>
           <el-radio-group v-model="timeFilter" class="filter-radio" @change="handleSearch">
             <el-radio label="ALL">不限</el-radio>
             <el-radio label="WEEK">最近一周</el-radio>
             <el-radio label="MONTH">最近一月</el-radio>
             <el-radio label="YEAR">最近一年</el-radio>
           </el-radio-group>
        </div>
        <div class="filter-group">
           <div class="filter-title">文件类型</div>
           <el-radio-group v-model="typeFilter" class="filter-radio" @change="handleSearch">
             <el-radio label="ALL">全部</el-radio>
             <el-radio label="DOCUMENT">文档</el-radio>
             <el-radio label="IMAGE">图片</el-radio>
             <el-radio label="VIDEO">视频</el-radio>
           </el-radio-group>
        </div>
      </div>
      
      <div class="results-content" v-loading="loading">
        <div class="results-header">
           <span class="result-count">找到约 {{ total }} 条结果</span>
        </div>
        
        <div v-if="knowledgeList.length === 0" class="no-results">
           <el-empty description="未找到相关知识" />
        </div>

        <div v-else class="results-list">
           <div v-for="item in knowledgeList" :key="item.id" class="result-card" @click="viewDetail(item.id)">
              <div class="result-title">
                 <el-icon class="file-icon"><Document /></el-icon>
                 <span v-if="item.highlight && item.highlight.title" v-html="item.highlight.title[0]"></span>
                 <span v-else>{{ item.title }}</span>
              </div>
              <div class="result-snippet">
                 <div v-if="item.highlight && item.highlight.content">
                    <span v-for="(fragment, idx) in item.highlight.content.slice(0, 3)" :key="idx" v-html="fragment + '... '"></span>
                 </div>
                 <div v-else>
                    {{ item.summary || (item.content ? item.content.substring(0, 200) + '...' : '暂无摘要') }}
                 </div>
              </div>
              <div class="result-meta">
                 <span class="meta-item"><el-icon><User /></el-icon> {{ item.author }}</span>
                 <span class="meta-item"><el-icon><Clock /></el-icon> {{ formatTime(item.createTime) }}</span>
                 <span class="meta-item"><el-icon><Folder /></el-icon> {{ item.category || '未分类' }}</span>
              </div>
           </div>
        </div>
        
        <div class="pagination-container" v-if="total > 0">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="handleSearch"
          />
        </div>
      </div>
    </div>

    <!-- 上传对话框保持不变，简化显示 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="600px">
       <div style="text-align: center; padding: 20px;">
         <p>请前往 <router-link to="/knowledge-management">知识管理</router-link> 页面进行更完整的管理操作</p>
         <el-button type="primary" @click="$router.push('/knowledge-management')">跳转管理页</el-button>
       </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { hasPermission } from '../utils/permission'
import { Search, Document, User, Clock, Folder } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// State
const isInitialState = ref(true)
const hasSearched = ref(false)
const searchKeyword = ref('')
const loading = ref(false)
const knowledgeList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// Filters
const timeFilter = ref('ALL')
const typeFilter = ref('ALL')

// History
const historyTags = ref([]) // 可以从localStorage加载

// Upload
const showUploadDialog = ref(false)

onMounted(() => {
  const history = localStorage.getItem('searchHistory')
  if (history) {
    historyTags.value = JSON.parse(history).slice(0, 5)
  }
})

const resetSearch = () => {
  isInitialState.value = true
  hasSearched.value = false
  searchKeyword.value = ''
  knowledgeList.value = []
}

const quickSearch = (val) => {
  searchKeyword.value = val
  handleSearch()
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return
  
  hasSearched.value = true
  isInitialState.value = false
  loading.value = true
  
  // Save history
  const tags = new Set([searchKeyword.value, ...historyTags.value])
  historyTags.value = Array.from(tags).slice(0, 5)
  localStorage.setItem('searchHistory', JSON.stringify(historyTags.value))

  try {
    const params = {
      keyword: searchKeyword.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 'APPROVED', // Only show approved
      timeRange: timeFilter.value,
      fileType: typeFilter.value === 'ALL' ? null : typeFilter.value
    }
    
    // Using the search API
    const res = await api.post('/knowledge/search', params)
    if (res.code === 200) {
       // Filter folders if necessary, but search usually returns files
       knowledgeList.value = (res.data.results || []).filter(item => item.fileId != null)
       total.value = res.data.total || knowledgeList.value.length
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const formatTime = (time) => {
   if(!time) return ''
   return new Date(time).toLocaleDateString()
}
</script>

<style scoped>
.knowledge-search-container {
  min-height: 100vh;
  background-color: #fff;
  display: flex;
  flex-direction: column;
}

/* 初始居中样式 */
.initial-search-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin-top: -100px; /* Visual adjustment */
}
.brand-large h1 {
  font-size: 3rem;
  color: #333;
  margin-bottom: 0.5rem;
}
.subtitle {
  color: #666;
  font-size: 1.2rem;
  margin-bottom: 2rem;
  text-align: center;
}
.search-box-large {
  width: 600px;
  max-width: 90%;
}
.search-input-large :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding-left: 15px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  height: 50px;
}
.search-tags {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}
.tag-label {
  color: #999;
}
.history-tag {
  cursor: pointer;
}

/* 顶部栏样式 */
.top-bar {
  height: 64px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  padding: 0 40px;
  background: white;
  position: sticky;
  top: 0;
  z-index: 100;
}
.brand {
  cursor: pointer;
  margin-right: 40px;
  color: #409EFF;
}
.search-box-mini {
  flex: 1;
  max-width: 600px;
}
.actions {
  margin-left: auto;
}

/* 搜索结果布局 */
.search-results-wrapper {
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
  gap: 30px;
}
.filter-sidebar {
  width: 200px;
  flex-shrink: 0;
}
.filter-group {
  margin-bottom: 30px;
}
.filter-title {
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}
.filter-radio {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.results-content {
  flex: 1;
}
.results-header {
  color: #999;
  font-size: 0.9rem;
  margin-bottom: 20px;
}
.result-card {
  margin-bottom: 20px;
  padding: 0 0 20px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.result-card:hover .result-title {
  color: #409EFF;
  text-decoration: underline;
}
.result-title {
  font-size: 1.2rem;
  color: #1a0dab;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-snippet {
  color: #4d5156;
  font-size: 0.95rem;
  line-height: 1.5;
  margin-bottom: 8px;
}
/* Highlight style handled by v-html, usually needs global or deep selector */
:deep(.highlight-text), :deep(em) {
  color: #ea4335;
  font-style: normal;
  font-weight: bold;
}
.result-meta {
  display: flex;
  gap: 15px;
  color: #006621;
  font-size: 0.85rem;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
