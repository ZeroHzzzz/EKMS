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

    <!-- 版本信息和统计 -->
    <div class="version-stats-container" v-if="version1Info && version2Info">
      <!-- 版本卡片区域 -->
      <div class="version-info">
        <div class="version-card old">
          <div class="card-header">
            <el-tag type="danger" effect="dark" size="small">旧版本 v{{ selectedVersion1 }}</el-tag>
          </div>
          <div class="card-meta">
            <span class="hash">{{ version1Info.commitHash?.substring(0, 8) }}</span>
            <span class="separator">•</span>
            <span class="author">{{ version1Info.createdBy }}</span>
            <span class="separator">•</span>
            <span class="time">{{ formatTime(version1Info.createTime) }}</span>
          </div>
        </div>
        <el-icon class="compare-arrow"><ArrowRight /></el-icon>
        <div class="version-card new">
          <div class="card-header">
            <el-tag type="success" effect="dark" size="small">新版本 v{{ selectedVersion2 }}</el-tag>
          </div>
          <div class="card-meta">
            <span class="hash">{{ version2Info.commitHash?.substring(0, 8) }}</span>
            <span class="separator">•</span>
            <span class="author">{{ version2Info.createdBy }}</span>
            <span class="separator">•</span>
            <span class="time">{{ formatTime(version2Info.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 统计信息卡片 -->
      <div class="diff-stats-card" v-if="diffData.stats">
        <div class="stats-item add">
          <el-icon class="stats-icon"><Plus /></el-icon>
          <span class="stats-number">{{ diffData.stats.insertCount }}</span>
          <span class="stats-label">新增</span>
        </div>
        <div class="stats-divider"></div>
        <div class="stats-item delete">
          <el-icon class="stats-icon"><Minus /></el-icon>
          <span class="stats-number">{{ diffData.stats.deleteCount }}</span>
          <span class="stats-label">删除</span>
        </div>
        <div class="stats-divider"></div>
        <div class="stats-item unchanged">
          <span class="stats-number">{{ diffData.stats.equalCount }}</span>
          <span class="stats-label">未变</span>
        </div>
      </div>
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
        <div class="split-header">
           <div class="split-col">v{{ selectedVersion1 }} (旧版本)</div>
           <div class="split-col">v{{ selectedVersion2 }} (新版本)</div>
        </div>
        <div class="split-content">
           <div v-for="(row, idx) in splitRows" :key="idx" class="split-row">
              <!-- Left Side -->
              <div class="split-cell left" :class="row.left.class">
                 <span class="line-number">{{ row.left.lineNumber || '' }}</span>
                 <span class="line-content">{{ row.left.content }}</span>
              </div>
              <!-- Right Side -->
              <div class="split-cell right" :class="row.right.class">
                 <span class="line-number">{{ row.right.lineNumber || '' }}</span>
                 <span class="line-content">{{ row.right.content }}</span>
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

// 生成并排视图的行（包含对齐逻辑）
const splitRows = computed(() => {
  const rows = []
  const lines = diffData.value.diffLines || []
  let i = 0
  
  while (i < lines.length) {
    const line = lines[i]
    if (line.type === 'EQUAL') {
      rows.push({
        left: { ...line, lineNumber: line.lineNumber1, class: 'diff-equal' },
        right: { ...line, lineNumber: line.lineNumber2, class: 'diff-equal' }
      })
      i++
    } else if (line.type === 'DELETE') {
      // 收集连续的删除
      const deletes = []
      while (i < lines.length && lines[i].type === 'DELETE') {
        deletes.push(lines[i])
        i++
      }
      
      // 检查并在同一块中收集连续的插入（修改操作）
      const inserts = []
      while (i < lines.length && lines[i].type === 'INSERT') {
        inserts.push(lines[i])
        i++
      }
      
      // 对齐显示
      const count = Math.max(deletes.length, inserts.length)
      for (let j = 0; j < count; j++) {
        rows.push({
          left: deletes[j] ? { ...deletes[j], lineNumber: deletes[j].lineNumber1, class: 'diff-delete' } : { type: 'EMPTY', content: '', class: 'diff-empty' },
          right: inserts[j] ? { ...inserts[j], lineNumber: inserts[j].lineNumber2, class: 'diff-insert' } : { type: 'EMPTY', content: '', class: 'diff-empty' }
        })
      }
    } else if (line.type === 'INSERT') {
      // 纯插入（没有前置删除）
      const inserts = []
      while (i < lines.length && lines[i].type === 'INSERT') {
        inserts.push(lines[i])
        i++
      }
      for (const ins of inserts) {
        rows.push({
          left: { type: 'EMPTY', content: '', class: 'diff-empty' },
          right: { ...ins, lineNumber: ins.lineNumber2, class: 'diff-insert' }
        })
      }
    }
  }
  return rows
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
/* Main Container */
.version-diff-page {
  padding: 32px;
  background: #f8fafc;
  min-height: 100vh;
  animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Enhanced Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 24px 36px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.page-header:hover {
  box-shadow: 0 6px 28px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(255, 255, 255, 0.6);
  transform: translateY(-1px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 28px;
}

.back-btn {
  font-size: 15px;
  color: #4a5568;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.back-btn:hover {
  color: #3b82f6;
  transform: translateX(-3px);
}

.title-section h2 {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #1a1a1a 0%, #4a5568 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  line-height: 1.2;
}

.title-section .subtitle {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
  font-weight: 400;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 48px;
}

.version-selector-group {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #ffffff;
  padding: 12px 24px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.version-selector-group:hover {
  border-color: rgba(203, 213, 225, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.version-select {
  width: 260px;
}

.arrow-icon {
  font-size: 20px;
  color: #6366f1;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* Version and Stats Container */
.version-stats-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 28px;
}

/* Compact Version Info Cards */
.version-info {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
}

.version-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px;
  border-radius: 12px;
  flex: 1;
  max-width: 340px;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.version-card.old {
  border-left: 4px solid #ef4444;
}

.version-card.old:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-left-color: #dc2626;
}

.version-card.new {
  border-left: 4px solid #22c55e;
}

.version-card.new:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-left-color: #16a34a;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.separator {
  color: #d1d5db;
  font-size: 10px;
}

.hash {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
  color: #1f2937;
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.3px;
}

.author {
  font-weight: 500;
}

.time {
  color: #9ca3af;
  font-size: 11px;
}

.compare-arrow {
  font-size: 24px;
  color: #9ca3af;
  flex-shrink: 0;
}

/* Unified Diff Stats Card */
.diff-stats-card {
  display: flex;
  align-items: center;
  gap: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 12px 20px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.8);
  flex-shrink: 0;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px;
}

.stats-item.add {
  color: #059669;
}

.stats-item.delete {
  color: #dc2626;
}

.stats-item.unchanged {
  color: #6366f1;
}

.stats-icon {
  font-size: 16px;
  font-weight: bold;
}

.stats-number {
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.stats-label {
  font-size: 12px;
  font-weight: 500;
  opacity: 0.8;
}

.stats-divider {
  width: 1px;
  height: 32px;
  background: linear-gradient(to bottom, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
}

/* Enhanced Diff Container */
.diff-content {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

/* Code Font Settings */
.diff-line, .split-content {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 13px;
  line-height: 22px;
}

/* Unified View */
.unified-view {
  padding: 0;
  overflow-x: auto;
}

.diff-line {
  display: flex;
  width: 100%;
  transition: background-color 0.2s ease;
}

.diff-line:hover {
  filter: brightness(0.97);
}

.line-number {
  width: 56px;
  min-width: 56px;
  text-align: right;
  padding: 0 12px;
  color: #94a3b8;
  background-color: #f8fafc;
  border-right: 2px solid #e2e8f0;
  user-select: none;
  font-size: 12px;
  font-weight: 500;
}

.line-number.old {
  border-right: 2px solid #e2e8f0;
}

.line-type {
  padding: 0 12px;
  font-weight: 700;
  user-select: none;
}

.line-content {
  flex: 1;
  padding: 0 16px;
  word-break: break-all;
  white-space: pre-wrap;
}

/* Enhanced Color Schemes with HSL */
.diff-insert {
  background: linear-gradient(90deg, rgba(209, 250, 229, 0.4) 0%, rgba(209, 250, 229, 0.2) 100%);
  border-left: 3px solid #10b981;
}

.diff-insert .line-number {
  background: rgba(187, 247, 208, 0.5);
  color: #065f46;
  font-weight: 600;
}

.diff-insert .line-type {
  color: #059669;
}

.diff-insert .line-content {
  background: transparent;
  color: #064e3b;
}

.diff-delete {
  background: linear-gradient(90deg, rgba(254, 226, 226, 0.4) 0%, rgba(254, 226, 226, 0.2) 100%);
  border-left: 3px solid #ef4444;
}

.diff-delete .line-number {
  background: rgba(254, 202, 202, 0.5);
  color: #991b1b;
  font-weight: 600;
}

.diff-delete .line-type {
  color: #dc2626;
}

.diff-delete .line-content {
  background: transparent;
  color: #7f1d1d;
}

.diff-equal {
  background-color: #ffffff;
}

.diff-equal:hover {
  background-color: #fafbfc;
}

/* Enhanced Split View */
.split-view {
  display: flex;
  flex-direction: column;
}

.split-header {
  display: flex;
  background: linear-gradient(135deg, #f8fafc 0%, #e0e7ff 100%);
  border-bottom: 2px solid #cbd5e1;
  font-weight: 700;
  color: #334155;
  font-size: 14px;
  padding: 2px 0;
}

.split-col {
  flex: 1;
  padding: 14px 20px;
  border-right: 2px solid #cbd5e1;
  text-align: center;
  letter-spacing: 0.5px;
}

.split-col:last-child {
  border-right: none;
}

.split-content {
  overflow-x: auto;
}

.split-row {
  display: flex;
  border-bottom: 1px solid #e2e8f0;
  transition: background-color 0.2s ease;
}

.split-row:hover {
  background-color: #fafbfc;
}

.split-cell {
  flex: 1;
  display: flex;
  overflow: hidden;
  border-right: 2px solid #e2e8f0;
  width: 50%;
}

.split-cell:last-child {
  border-right: none;
}

.split-cell .line-number {
  width: 50px;
  min-width: 50px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
}

.split-cell .line-content {
  padding: 0 16px;
}

/* Enhanced Split Cell Colors */
.split-cell.diff-insert {
  background: linear-gradient(90deg, rgba(209, 250, 229, 0.35) 0%, rgba(209, 250, 229, 0.15) 100%);
}

.split-cell.diff-insert .line-number {
  background: rgba(187, 247, 208, 0.6);
  color: #065f46;
  font-weight: 600;
}

.split-cell.diff-delete {
  background: linear-gradient(90deg, rgba(254, 226, 226, 0.35) 0%, rgba(254, 226, 226, 0.15) 100%);
}

.split-cell.diff-delete .line-number {
  background: rgba(254, 202, 202, 0.6);
  color: #991b1b;
  font-weight: 600;
}

.split-cell.diff-equal {
  background: #ffffff;
}

/* Enhanced Empty Cell Pattern */
.split-cell.diff-empty {
  background-color: #f1f5f9;
  background-image: repeating-linear-gradient(
    -45deg,
    transparent,
    transparent 6px,
    rgba(148, 163, 184, 0.08) 6px,
    rgba(148, 163, 184, 0.08) 12px
  );
}

.split-cell.diff-empty .line-number {
  background: #e2e8f0;
  color: #cbd5e1;
}

/* Loading State Enhancement */
.diff-content[v-loading] {
  min-height: 400px;
}
</style>
