<template>
  <div class="version-diff-page">
    <!-- 顶部工具栏 -->
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link class="back-btn" @click="goBack">
           <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div class="title-section">
          <h2>版本对比</h2>
          <p class="subtitle">比较不同版本之间的差异与变更</p>
        </div>
      </div>
      <div class="header-center">
         <div class="version-selector-group">
            <el-select v-model="selectedVersion1" placeholder="选择版本1" size="default" @change="loadDiff" class="version-select">
              <el-option 
                v-for="v in versions" 
                :key="v.id" 
                :label="`v${v.version} - ${v.commitMessage || '无说明'}`" 
                :value="v.version"
              />
            </el-select>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            <el-select v-model="selectedVersion2" placeholder="选择版本2" size="default" @change="loadDiff" class="version-select">
              <el-option 
                v-for="v in versions" 
                :key="v.id" 
                :label="`v${v.version} - ${v.commitMessage || '无说明'}`" 
                :value="v.version"
              />
            </el-select>
         </div>
      </div>
      <div class="header-right">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="unified">统一视图</el-radio-button>
          <el-radio-button label="split">并排视图</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 版本信息 -->
    <div class="version-info" v-if="version1Info && version2Info">
      <div class="version-card old">
        <el-tag type="danger" effect="dark">旧版本 v{{ selectedVersion1 }}</el-tag>
        <span class="hash">{{ version1Info.commitHash?.substring(0, 8) }}</span>
        <span class="author">{{ version1Info.createdBy }}</span>
        <span class="time">{{ formatTime(version1Info.createTime) }}</span>
      </div>
      <el-icon class="compare-arrow"><ArrowRight /></el-icon>
      <div class="version-card new">
        <el-tag type="success" effect="dark">新版本 v{{ selectedVersion2 }}</el-tag>
        <span class="hash">{{ version2Info.commitHash?.substring(0, 8) }}</span>
        <span class="author">{{ version2Info.createdBy }}</span>
        <span class="time">{{ formatTime(version2Info.createTime) }}</span>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="diff-stats" v-if="diffData.stats">
      <el-tag type="success" effect="plain">
        <el-icon><Plus /></el-icon> {{ diffData.stats.insertCount }} 行新增
      </el-tag>
      <el-tag type="danger" effect="plain">
        <el-icon><Minus /></el-icon> {{ diffData.stats.deleteCount }} 行删除
      </el-tag>
      <el-tag type="info" effect="plain">
        {{ diffData.stats.equalCount }} 行未变
      </el-tag>
    </div>

    <!-- Diff 内容区域 -->
    <div class="diff-content" v-loading="loading">
      <!-- 统一视图 -->
      <div v-if="viewMode === 'unified'" class="unified-view">
        <div 
          v-for="(line, idx) in diffData.diffLines" 
          :key="idx"
          class="diff-line"
          :class="getDiffLineClass(line.type)"
        >
          <span class="line-number old">{{ line.lineNumber1 || '' }}</span>
          <span class="line-number new">{{ line.lineNumber2 || '' }}</span>
          <span class="line-type">{{ getDiffLineSymbol(line.type) }}</span>
          <span class="line-content">{{ line.content }}</span>
        </div>
        <el-empty v-if="!diffData.diffLines?.length && !loading" description="内容完全相同，无差异" />
      </div>

      <!-- 并排视图 -->
      <div v-if="viewMode === 'split'" class="split-view">
        <div class="split-panel left">
          <div class="panel-header">
            <span>v{{ selectedVersion1 }} (旧版本)</span>
          </div>
          <div class="panel-content">
            <div 
              v-for="(line, idx) in leftLines" 
              :key="idx"
              class="diff-line"
              :class="line.class"
            >
              <span class="line-number">{{ line.lineNumber || '' }}</span>
              <span class="line-content">{{ line.content }}</span>
            </div>
          </div>
        </div>
        <div class="split-panel right">
          <div class="panel-header">
            <span>v{{ selectedVersion2 }} (新版本)</span>
          </div>
          <div class="panel-content">
            <div 
              v-for="(line, idx) in rightLines" 
              :key="idx"
              class="diff-line"
              :class="line.class"
            >
              <span class="line-number">{{ line.lineNumber || '' }}</span>
              <span class="line-content">{{ line.content }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Plus, Minus } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const router = useRouter()

const knowledgeId = ref(route.params.id)
const versions = ref([])
const selectedVersion1 = ref(null)
const selectedVersion2 = ref(null)
const diffData = ref({ diffLines: [], stats: null })
const loading = ref(false)
const viewMode = ref('unified')

const version1Info = computed(() => versions.value.find(v => v.version === selectedVersion1.value))
const version2Info = computed(() => versions.value.find(v => v.version === selectedVersion2.value))

// 生成并排视图的左侧行
const leftLines = computed(() => {
  return diffData.value.diffLines
    .filter(l => l.type === 'DELETE' || l.type === 'EQUAL')
    .map(l => ({
      lineNumber: l.lineNumber1,
      content: l.content,
      class: l.type === 'DELETE' ? 'diff-delete' : 'diff-equal'
    }))
})

// 生成并排视图的右侧行
const rightLines = computed(() => {
  return diffData.value.diffLines
    .filter(l => l.type === 'INSERT' || l.type === 'EQUAL')
    .map(l => ({
      lineNumber: l.lineNumber2,
      content: l.content,
      class: l.type === 'INSERT' ? 'diff-insert' : 'diff-equal'
    }))
})

const goBack = () => router.back()

const loadVersions = async () => {
  try {
    const res = await api.get(`/knowledge/${knowledgeId.value}/versions`)
    if (res.code === 200) {
      versions.value = res.data || []
    }
  } catch (e) {
    console.error('加载版本列表失败', e)
  }
}

const loadDiff = async () => {
  if (!selectedVersion1.value || !selectedVersion2.value) return
  if (selectedVersion1.value === selectedVersion2.value) {
    diffData.value = { diffLines: [], stats: { insertCount: 0, deleteCount: 0, equalCount: 0 } }
    return
  }
  
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${knowledgeId.value}/versions/compare`, {
      params: { version1: selectedVersion1.value, version2: selectedVersion2.value }
    })
    if (res.code === 200 && res.data) {
      diffData.value = res.data
    }
  } catch (e) {
    console.error('加载对比数据失败', e)
  } finally {
    loading.value = false
  }
}

const getDiffLineClass = (type) => {
  const classMap = { 'INSERT': 'diff-insert', 'DELETE': 'diff-delete', 'EQUAL': 'diff-equal' }
  return classMap[type] || ''
}

const getDiffLineSymbol = (type) => {
  const symbolMap = { 'INSERT': '+', 'DELETE': '-', 'EQUAL': ' ' }
  return symbolMap[type] || ' '
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(async () => {
  await loadVersions()
  // 从 URL 参数获取初始版本
  const v1 = parseInt(route.query.v1)
  const v2 = parseInt(route.query.v2)
  if (v1 && v2) {
    selectedVersion1.value = v1
    selectedVersion2.value = v2
    loadDiff()
  } else if (versions.value.length >= 2) {
    // 默认选择最新两个版本
    selectedVersion1.value = versions.value[1]?.version
    selectedVersion2.value = versions.value[0]?.version
    loadDiff()
  }
})
</script>

<style scoped>
.version-diff-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  font-size: 15px;
  color: #606266;
  font-weight: 500;
}

.back-btn:hover {
  color: #409EFF;
}

.title-section h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0;
  line-height: 1.2;
}

.title-section .subtitle {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 40px;
}

.version-selector-group {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f5f7fa;
  padding: 6px 16px;
  border-radius: 8px;
}

.version-select {
  width: 220px;
}

.arrow-icon {
  font-size: 16px;
  color: #909399;
}

.version-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  background: white;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.version-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 6px;
}

.version-card.old {
  background: #fef0f0;
}

.version-card.new {
  background: #f0f9eb;
}

.version-card .hash {
  font-family: monospace;
  color: #666;
}

.version-card .time {
  color: #999;
  font-size: 12px;
}

.compare-arrow {
  font-size: 24px;
  color: #409eff;
}

.diff-stats {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.diff-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.unified-view {
  padding: 16px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
}

.diff-line {
  display: flex;
  padding: 2px 8px;
  white-space: pre;
}

.diff-line.diff-insert {
  background: #e6ffed;
}

.diff-line.diff-delete {
  background: #ffeef0;
}

.diff-line.diff-equal {
  background: transparent;
}

.line-number {
  min-width: 40px;
  text-align: right;
  padding-right: 12px;
  color: #999;
  user-select: none;
}

.line-type {
  width: 20px;
  text-align: center;
  font-weight: bold;
}

.diff-insert .line-type {
  color: #22863a;
}

.diff-delete .line-type {
  color: #cb2431;
}

.line-content {
  flex: 1;
}

.split-view {
  display: flex;
}

.split-panel {
  flex: 1;
  border-right: 1px solid #eee;
}

.split-panel:last-child {
  border-right: none;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  font-weight: 600;
  border-bottom: 1px solid #eee;
}

.panel-content {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 600px;
  overflow: auto;
}

.split-panel .diff-line {
  padding: 2px 16px;
}
</style>
