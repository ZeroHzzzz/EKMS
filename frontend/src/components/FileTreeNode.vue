<template>
  <div class="file-tree-node">
    <!-- 当前节点行 -->
    <div 
      class="tree-row"
      :class="{
        'is-selected': selectedIds.includes(node.id),
        'is-folder': isFolder(node),
        'is-file': !isFolder(node),
        'is-search-result': node.isSearchResult || node.highlight,
        'drag-over-before': dragOverId === node.id && dragOverPosition === 'before',
        'drag-over-after': dragOverId === node.id && dragOverPosition === 'after',
        'drag-over-inside': dragOverId === node.id && dragOverPosition === 'inside',
        'is-dragging': isDragging
      }"
      :style="{ paddingLeft: depth * 24 + 12 + 'px' }"
      draggable="true"
      @dragstart="onDragStart"
      @dragover="onDragOver"
      @dragleave="onDragLeave"
      @drop="onDrop"
      @dragend="onDragEnd"
    >
      <!-- 拖拽指示线 - 上方 -->
      <div v-if="dragOverId === node.id && dragOverPosition === 'before'" class="drop-indicator top"></div>
      
      <!-- 选择框 -->
      <el-checkbox 
        :model-value="selectedIds.includes(node.id)"
        @change="() => $emit('toggle-select', node)"
        class="row-checkbox"
        @click.stop
      />
      
      <!-- 展开/折叠按钮 -->
      <div 
        v-if="hasChildren" 
        class="expand-btn"
        :class="{ 'is-expanded': isExpanded }"
        @click.stop="$emit('toggle-expand', node.id)"
      >
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div v-else class="expand-placeholder"></div>
      
      <!-- 图标 -->
      <div class="node-icon" :class="{ 'folder': isFolder(node), 'file': !isFolder(node) }">
        <el-icon>
          <FolderOpened v-if="isFolder(node) && isExpanded" />
          <Folder v-else-if="isFolder(node)" />
          <component v-else :is="getFileIcon(node)" />
        </el-icon>
      </div>
      
      <!-- 名称 -->
      <div class="node-name" @click="handleNameClick">
        <span 
          class="name-text" 
          v-if="node.highlight && node.highlight.title && node.highlight.title.length > 0"
          v-html="node.highlight.title[0]"
        ></span>
        <span class="name-text" v-else>{{ node.title }}</span>
        <el-tag v-if="node.isSearchResult || node.highlight" size="small" type="warning" class="match-badge">
          <el-icon><Search /></el-icon>
          {{ getMatchLabel(node) }}
        </el-tag>
        <el-tag v-if="node.isShared" size="small" type="success" class="shared-badge">共享</el-tag>
      </div>
      
      <!-- 作者 -->
      <div class="node-author">{{ node.author || '-' }}</div>
      
      <!-- 时间 -->
      <div class="node-time">{{ formatTime(node.createTime) }}</div>
      
      <!-- 操作按钮 -->
      <div class="node-actions">
        <el-button 
          v-if="!isFolder(node)" 
          size="small" 
          text 
          type="primary"
          @click.stop="$emit('view', node.id)"
        >
          查看
        </el-button>
        <el-button 
          v-if="isFolder(node)" 
          size="small" 
          text 
          type="success"
          @click.stop="$emit('add-child', node)"
        >
          添加
        </el-button>
        <el-button 
          size="small" 
          text 
          type="warning"
          @click.stop="$emit('edit', node)"
        >
          编辑
        </el-button>
        <el-button 
          size="small" 
          text 
          type="danger"
          @click.stop="$emit('delete', node)"
        >
          删除
        </el-button>
      </div>
      
      <!-- 拖拽指示线 - 下方 -->
      <div v-if="dragOverId === node.id && dragOverPosition === 'after'" class="drop-indicator bottom"></div>
      
      <!-- 拖拽到文件夹内部提示 -->
      <div v-if="dragOverId === node.id && dragOverPosition === 'inside'" class="drop-inside-hint">
        放入文件夹
      </div>
    </div>
    
    <!-- 子节点 -->
    <div v-if="hasChildren && isExpanded" class="tree-children">
      <template v-for="(child, idx) in node.children" :key="child.id">
        <FileTreeNode
          :node="child"
          :depth="depth + 1"
          :index="idx"
          :selectedIds="selectedIds"
          :expandedIds="expandedIds"
          :dragOverId="dragOverId"
          :dragOverPosition="dragOverPosition"
          @toggle-expand="(id) => $emit('toggle-expand', id)"
          @toggle-select="(n) => $emit('toggle-select', n)"
          @view="(id) => $emit('view', id)"
          @edit="(n) => $emit('edit', n)"
          @delete="(n) => $emit('delete', n)"
          @add-child="(n) => $emit('add-child', n)"
          @drag-start="(e, n) => $emit('drag-start', e, n)"
          @drag-over="(e, n, pos) => $emit('drag-over', e, n, pos)"
          @drag-leave="(e, n) => $emit('drag-leave', e, n)"
          @drop="(e, n, pos) => $emit('drop', e, n, pos)"
          @drag-end="(e) => $emit('drag-end', e)"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowRight, Folder, FolderOpened, Document, Picture, VideoPlay, Headset, Grid, Search } from '@element-plus/icons-vue'

const props = defineProps({
  node: {
    type: Object,
    required: true
  },
  depth: {
    type: Number,
    default: 0
  },
  index: {
    type: Number,
    default: 0
  },
  selectedIds: {
    type: Array,
    default: () => []
  },
  expandedIds: {
    type: Array,
    default: () => []
  },
  dragOverId: {
    type: [String, Number],
    default: null
  },
  dragOverPosition: {
    type: String,
    default: null
  }
})

const emit = defineEmits([
  'toggle-expand',
  'toggle-select', 
  'view',
  'edit',
  'delete',
  'add-child',
  'drag-start',
  'drag-over',
  'drag-leave',
  'drop',
  'drag-end'
])

const isDragging = ref(false)

// 计算属性
const hasChildren = computed(() => {
  return props.node.children && props.node.children.length > 0
})

const isExpanded = computed(() => {
  return props.expandedIds.includes(props.node.id)
})

// 判断是否是文件夹
const isFolder = (node) => {
  return !node.fileId && (node.children?.length > 0 || node.hasChildren || node.isDepartmentRoot || node.isFolder)
}

// 获取文件图标
const getFileIcon = (node) => {
  if (!node.fileName && !node.fileType) return Document
  
  const fileName = node.fileName || ''
  const ext = fileName.split('.').pop()?.toLowerCase() || node.fileType?.toLowerCase() || ''
  
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
  const videoExts = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv']
  const audioExts = ['mp3', 'wav', 'flac', 'aac', 'ogg']
  const excelExts = ['xls', 'xlsx', 'csv']
  
  if (imageExts.includes(ext)) return Picture
  if (videoExts.includes(ext)) return VideoPlay
  if (audioExts.includes(ext)) return Headset
  if (excelExts.includes(ext)) return Grid
  
  return Document
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60000) return '刚刚'
  // 小于1小时
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  // 小于24小时
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  // 小于7天
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  
  // 超过7天显示日期
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  if (year === now.getFullYear()) {
    return `${month}-${day}`
  }
  return `${year}-${month}-${day}`
}

const getMatchLabel = (node) => {
  if (node.highlight?.matchLocation) {
    return node.highlight.matchLocation.replace('匹配位置：', '')
  }
  return '匹配'
}

// 点击名称
const handleNameClick = () => {
  if (isFolder(props.node)) {
    emit('toggle-expand', props.node.id)
  } else {
    emit('view', props.node.id)
  }
}

// 拖拽事件
const onDragStart = (e) => {
  isDragging.value = true
  e.dataTransfer.effectAllowed = 'copyMove'
  
  // 设置拖拽数据 - 用于树内部排序
  const dragData = {
    id: props.node.id,
    parentId: props.node.parentId,
    title: props.node.title
  }
  e.dataTransfer.setData('text/plain', JSON.stringify(dragData))
  
  // 如果是文件（非文件夹），也设置为可拖拽到 AI 助手的格式
  if (!isFolder(props.node)) {
    const aiDragData = {
      type: 'knowledge-document',
      document: {
        id: props.node.id,
        title: props.node.title,
        keywords: props.node.keywords || ''
      }
    }
    e.dataTransfer.setData('application/json', JSON.stringify(aiDragData))
  }
  
  emit('drag-start', e, props.node)
}

const onDragOver = (e) => {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
  
  const rect = e.currentTarget.getBoundingClientRect()
  const y = e.clientY - rect.top
  const height = rect.height
  
  let position = 'after'
  if (y < height * 0.25) {
    position = 'before'
  } else if (y > height * 0.75) {
    position = 'after'
  } else if (isFolder(props.node)) {
    position = 'inside'
  }
  
  emit('drag-over', e, props.node, position)
}

const onDragLeave = (e) => {
  emit('drag-leave', e, props.node)
}

const onDrop = (e) => {
  e.preventDefault()
  isDragging.value = false
  
  const rect = e.currentTarget.getBoundingClientRect()
  const y = e.clientY - rect.top
  const height = rect.height
  
  let position = 'after'
  if (y < height * 0.25) {
    position = 'before'
  } else if (y > height * 0.75) {
    position = 'after'
  } else if (isFolder(props.node)) {
    position = 'inside'
  }
  
  emit('drop', e, props.node, position)
}

const onDragEnd = (e) => {
  isDragging.value = false
  emit('drag-end', e)
}
</script>

<style scoped>
.file-tree-node {
  user-select: none;
}

.tree-row {
  display: flex;
  align-items: center;
  height: 46px;
  padding-right: 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  transition: all 0.15s ease;
  position: relative;
  cursor: default;
  gap: 12px;
}

.tree-row:hover {
  background: #f5f7fa;
}

.tree-row.is-selected {
  background: #ecf5ff;
}

.tree-row.is-dragging {
  opacity: 0.5;
  background: #e8f4ff;
}

/* 拖拽指示线 */
.drop-indicator {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: #409eff;
  z-index: 10;
  pointer-events: none;
}

.drop-indicator.top {
  top: 0;
}

.drop-indicator.bottom {
  bottom: 0;
}

.tree-row.drag-over-before {
  border-top: 2px solid #409eff;
}

.tree-row.drag-over-after {
  border-bottom: 2px solid #409eff;
}

.tree-row.drag-over-inside {
  background: #ecf5ff;
  box-shadow: inset 0 0 0 2px #409eff;
}

.drop-inside-hint {
  position: absolute;
  right: 200px;
  top: 50%;
  transform: translateY(-50%);
  background: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.row-checkbox {
  flex-shrink: 0;
}

.expand-btn {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  background: #f5f7fa;
}

.expand-btn:hover {
  background: #e4e7ed;
}

.expand-btn .el-icon {
  font-size: 12px;
  color: #606266;
  transition: transform 0.2s;
}

.expand-btn.is-expanded {
  background: #ecf5ff;
}

.expand-btn.is-expanded .el-icon {
  transform: rotate(90deg);
  color: #409eff;
}

.expand-placeholder {
  width: 22px;
  flex-shrink: 0;
}

.node-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  flex-shrink: 0;
}

.node-icon.folder {
  background: #fdf6ec;
}

.node-icon.folder .el-icon {
  color: #e6a23c;
  font-size: 18px;
}

.node-icon.file {
  background: #ecf5ff;
}

.node-icon.file .el-icon {
  color: #409eff;
  font-size: 18px;
}

.node-name {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  overflow: hidden;
}

.name-text {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tree-row:hover .name-text {
  color: #409eff;
}

/* 搜索结果高亮 */
.tree-row.is-search-result {
  background: #fef9e7;
  border-left: 3px solid #e6a23c;
}

.tree-row.is-search-result:hover {
  background: #fdf6ec;
}

.name-text :deep(mark),
.name-text :deep(em) {
  background: #fff3cd;
  color: #856404;
  padding: 1px 4px;
  border-radius: 2px;
  font-style: normal;
  font-weight: 600;
}

.match-badge {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
}

.match-badge .el-icon {
  font-size: 12px;
}

.shared-badge {
  flex-shrink: 0;
}

.node-author {
  width: 100px;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.node-time {
  width: 120px;
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
  text-align: center;
}

.node-actions {
  width: 180px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
  justify-content: center;
}

.tree-row:hover .node-actions {
  opacity: 1;
}

.tree-children {
  position: relative;
}

/* 连接线 - 垂直线 */
.tree-children::before {
  content: '';
  position: absolute;
  left: 44px;
  top: 0;
  bottom: 22px;
  width: 1px;
  background: #ebeef5;
}
</style>

