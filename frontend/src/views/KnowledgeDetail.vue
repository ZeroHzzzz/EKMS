<template>
  <div class="knowledge-detail-container" v-loading="loading">
    <div v-if="knowledge" class="detail-wrapper">
      <!-- 头部信息 -->
      <div class="detail-header">
        <div class="header-left">
          <h1 v-if="!isEditMode" class="title">{{ knowledge.title }}</h1>
          <el-input v-else v-model="editForm.title" placeholder="请输入标题" class="title-input" />
        </div>
        <div class="header-actions">
          <template v-if="isEditMode">
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="saveEdit">保存</el-button>
          </template>
          <template v-else>
            <el-button 
              :type="isCollected ? 'warning' : 'default'"
              :icon="isCollected ? 'StarFilled' : 'Star'"
              circle
              @click="handleCollect"
              :title="isCollected ? '取消收藏' : '收藏'"
            />
            <el-button v-if="canEdit(knowledge)" type="primary" icon="Edit" circle @click="enterEditMode" title="编辑" />
          </template>
        </div>
      </div>

      <!-- 主体内容：左右分栏 -->
      <div class="detail-body">
        <!-- 左侧：内容/预览 -->
        <div class="left-panel">
          <!-- 编辑模式表单 -->
          <div v-if="isEditMode" class="edit-form-container">
            <el-form :model="editForm" label-width="80px">
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

          <!-- 查看模式：文件预览/文本内容 -->
          <div v-else class="view-container">
            <!-- 只有纯文本内容 -->
            <div v-if="!knowledge.fileId && knowledge.content" class="text-content-viewer">
              <pre>{{ knowledge.content }}</pre>
            </div>

            <!-- 文件预览区 -->
            <div v-if="knowledge.fileId && fileInfo" class="file-preview-viewer">
              <div class="preview-toolbar">
                <span class="file-name">
                  <el-icon><Document /></el-icon> {{ fileInfo.fileName }}
                </span>
                <div class="preview-actions">
                  <el-button text type="primary" @click="downloadFile">下载</el-button>
                  <el-button text type="primary" @click="openInNewWindow">新窗口打开</el-button>
                </div>
              </div>
              
              <div class="preview-stage">
                <!-- PDF -->
                <iframe 
                  v-if="isPdfFile(fileInfo.fileType)"
                  :src="previewUrl" 
                  class="preview-frame"
                ></iframe>

                <!-- 图片 -->
                <div v-else-if="isImageFile(fileInfo.fileType)" class="image-preview-box">
                  <img :src="previewUrl" :alt="fileInfo.fileName" />
                </div>

                <!-- 视频 -->
                <div v-else-if="isVideoFile(fileInfo.fileType)" class="video-preview-box">
                  <!-- 使用 kkFileView 预览视频 (iframe) -->
                  <iframe 
                    :src="getKkFileViewUrl()" 
                    class="preview-frame"
                    allowfullscreen
                  ></iframe>
                </div>

                 <!-- 音频 -->
                <div v-else-if="isAudioFile(fileInfo.fileType)" class="audio-preview-box">
                  <audio :src="previewUrl" controls class="audio-player"></audio>
                </div>

                <!-- Office / 文本 (kkFileView) -->
                <div v-else-if="isOfficeFile(fileInfo.fileType) || isTextFile(fileInfo.fileType)" class="office-preview-box">
                  <iframe 
                    :src="getKkFileViewUrl()" 
                    class="preview-frame"
                  ></iframe>
                </div>

                <!-- 不支持的类型 -->
                 <div v-else class="unsupported-box">
                  <el-empty description="该格式暂不支持在线预览，请下载查看">
                    <el-button type="primary" @click="downloadFile">下载文件</el-button>
                  </el-empty>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：属性/关联/评论 -->
        <div class="right-panel">
          <el-tabs v-model="activeRightTab" class="right-tabs">
            <el-tab-pane label="属性" name="properties">
              <div class="properties-panel">
                <div class="prop-group">
                  <label>基本信息</label>
                  <div class="prop-item">
                    <span class="label">作者</span>
                    <span class="value">{{ knowledge.author }}</span>
                  </div>
                   <div class="prop-item">
                    <span class="label">版本</span>
                    <span class="value">v{{ knowledge.version }}</span>
                  </div>
                  <div class="prop-item">
                    <span class="label">状态</span>
                    <el-tag size="small" :type="getStatusType(knowledge.status)">{{ getStatusText(knowledge.status) }}</el-tag>
                  </div>
                   <div class="prop-item">
                    <span class="label">分类</span>
                    <span class="value">{{ knowledge.category }}</span>
                  </div>
                   <div class="prop-item">
                    <span class="label">时间</span>
                    <span class="value">{{ formatTime(knowledge.updateTime || knowledge.createTime) }}</span>
                  </div>
                </div>

                <div class="prop-group" v-if="knowledge.summary">
                  <label>摘要</label>
                  <p class="summary-text">{{ knowledge.summary }}</p>
                </div>

                <div class="prop-group" v-if="knowledge.keywords">
                  <label>关键词</label>
                  <div class="tags-wrapper">
                    <el-tag 
                      v-for="tag in (knowledge.keywords || '').split(',')" 
                      :key="tag" 
                      size="small" 
                      class="keyword-tag"
                    >
                      {{ tag.trim() }}
                    </el-tag>
                  </div>
                </div>
                
                 <!-- 附件信息 -->
                 <div class="prop-group" v-if="fileInfo">
                  <label>附件信息</label>
                  <div class="prop-item">
                    <span class="label">大小</span>
                    <span class="value">{{ formatFileSize(fileInfo.fileSize) }}</span>
                  </div>
                  <div class="prop-item">
                    <span class="label">类型</span>
                    <span class="value">{{ fileInfo.fileType }}</span>
                  </div>
                 </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="关联" name="relations">
               <div class="relations-panel">
                 <div class="relation-actions" v-if="canEdit(knowledge)">
                   <el-button type="primary" size="small" plain style="width: 100%" @click="showAddRelationDialog = true">添加关联</el-button>
                 </div>
                 
                 <!-- 知识图谱 -->
                 <div ref="relationGraphRef" class="relation-graph"></div>

                 <!-- 列表视图 -->
                 <div class="relation-list">
                    <div 
                      v-for="relation in knowledgeRelations" 
                      :key="relation.id" 
                      class="relation-list-item"
                       @click="viewDetail(relation.relatedKnowledge?.id)"
                    >
                      <div class="rel-icon">
                        <el-icon><Link /></el-icon>
                      </div>
                      <div class="rel-info">
                        <div class="rel-title">{{ relation.relatedKnowledge?.title }}</div>
                        <el-tag size="small" :type="getRelationTypeTag(relation.relationType)">{{ getRelationTypeText(relation.relationType) }}</el-tag>
                      </div>
                      <el-button v-if="canEdit(knowledge)" size="small" text type="danger" icon="Delete" @click.stop="deleteRelation(relation.id)"></el-button>
                    </div>
                  <!-- 推荐关联 -->
                  <div class="relation-list" v-if="suggestedRelations.length > 0" style="margin-top: 20px;">
                    <div class="section-title">
                      <span>猜你喜欢</span>
                      <el-tag size="small" type="success" effect="plain" style="margin-left:8px">自动推荐</el-tag>
                    </div>
                     <div 
                      v-for="item in suggestedRelations" 
                      :key="item.id" 
                      class="relation-list-item"
                       @click="$router.push(`/knowledge/detail/${item.id}`)"
                    >
                      <div class="rel-icon">
                        <el-icon><Document /></el-icon>
                      </div>
                      <div class="rel-info">
                        <div class="rel-title">{{ item.title }}</div>
                         <div class="rel-meta">
                              <span><el-icon><View /></el-icon> {{ item.clickCount || 0 }}</span>
                              <span><el-icon><User /></el-icon> {{ item.author || '未知' }}</span>
                         </div>
                      </div>
                      <el-button size="small" link type="primary">查看</el-button>
                    </div>
                  </div>
                  
                  <el-empty v-if="knowledgeRelations.length === 0 && suggestedRelations.length === 0" description="暂无关联" image-size="60" />
                 </div>
               </div>
            </el-tab-pane>

            <el-tab-pane label="评论" name="comments">
              <div class="comments-panel">
                <div class="comment-input-box">
                  <el-input
                    v-model="newCommentContent"
                    type="textarea"
                    placeholder="发表看法..."
                    :rows="2"
                  />
                  <div class="comment-btn-row">
                    <el-button type="primary" size="small" @click="submitComment" :loading="commentSubmitting">发送</el-button>
                  </div>
                </div>
                
                <div class="comments-stream" v-loading="commentsLoading">
                  <div v-for="comment in comments" :key="comment.id" class="comment-bubble">
                     <div class="comment-avatar">
                       <el-avatar :size="32" :style="{ backgroundColor: stringToColor(comment.userRealName || comment.userName) }">
                         {{ (comment.userRealName || comment.userName || 'U').charAt(0).toUpperCase() }}
                       </el-avatar>
                     </div>
                     <div class="comment-body">
                       <div class="comment-meta">
                         <span class="comment-user">{{ comment.userRealName || comment.userName }}</span>
                         <span class="comment-time">{{ formatTimeFriendly(comment.createTime) }}</span>
                       </div>
                       <div class="comment-text">{{ comment.content }}</div>
                       <div class="comment-ops">
                         <span class="op-btn" @click="replyToComment(comment)">回复</span>
                         <span class="op-btn delete" v-if="canDeleteComment(comment)" @click="deleteComment(comment.id)">删除</span>
                       </div>
                       
                       <!-- 回复 -->
                       <div v-if="comment.replies && comment.replies.length > 0" class="replies-wrapper">
                         <div v-for="reply in comment.replies" :key="reply.id" class="reply-bubble">
                           <div class="reply-meta">
                             <span class="reply-user">{{ reply.userRealName || reply.userName }}</span>
                             <span class="reply-time">{{ formatTimeFriendly(reply.createTime) }}</span>
                           </div>
                           <div class="reply-text">{{ reply.content }}</div>
                            <div class="comment-ops">
                              <span class="op-btn delete" v-if="canDeleteComment(reply)" @click="deleteComment(reply.id)">删除</span>
                            </div>
                         </div>
                       </div>
                     </div>
                  </div>
                  <el-empty v-if="comments.length === 0" description="暂无评论" image-size="60" />
                </div>
              </div>
            </el-tab-pane>

             <el-tab-pane label="历史" name="history">
               <div class="history-panel">
                 <el-timeline>
                    <el-timeline-item
                      v-for="(version, index) in versions"
                      :key="index"
                      :timestamp="formatTime(version.createTime)"
                      placement="top"
                    >
                      <div class="history-card">
                        <h4>v{{ version.version }}</h4>
                        <p>{{ version.changeDescription || '无变更说明' }}</p>
                        <div class="history-ops">
                           <el-button size="small" link @click="viewVersion(version)">查看</el-button>
                           <el-button size="small" link v-if="version.version !== knowledge.version" @click="compareVersion(version)">对比</el-button>
                        </div>
                      </div>
                    </el-timeline-item>
                 </el-timeline>
               </div>
             </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
    
    <!-- Dialogs -->
    <el-dialog v-model="showAddRelationDialog" title="添加关联" width="500px">
      <el-form label-width="80px">
        <el-form-item label="搜索知识">
            <el-select
            v-model="addRelationForm.relatedKnowledgeId"
            filterable
            remote
            reserve-keyword
            placeholder="请输入关键词搜索"
            :remote-method="remoteMethod"
            :loading="searchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in relationSearchResults"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
         <el-form-item label="关联类型">
           <el-select v-model="addRelationForm.relationType" style="width: 100%">
             <el-option label="相关" value="RELATED" />
             <el-option label="引用" value="CITED" />
             <el-option label="相似" value="SIMILAR" />
           </el-select>
         </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddRelationDialog = false">取消</el-button>
          <el-button type="primary" @click="addKnowledgeRelation">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Version View/Compare Dialogs (Keep simple/same) -->
    <el-dialog v-model="showVersionViewDialog" title="版本详情" width="60%">
       <pre>{{ selectedVersion?.content }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { hasRole, ROLE_ADMIN, ROLE_EDITOR } from '../utils/permission'
import { Star, StarFilled, Edit, Document, Link, Delete } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// State
const activeTab = ref('properties')
const activeRightTab = ref('properties')
const knowledge = ref({})
const fileInfo = ref(null)
const loading = ref(false)
const isCollected = ref(false)
const editForm = ref({})
const comments = ref([])
const newCommentContent = ref('')
const commentsLoading = ref(false)
const commentSubmitting = ref(false)
const knowledgeRelations = ref([])
const suggestedRelations = ref([]) // New state
const showAddRelationDialog = ref(false)
const addRelationForm = ref({ relatedKnowledgeId: null, relationType: 'RELATED' })
const relationSearchResults = ref([])
const searchLoading = ref(false)
const versions = ref([])
const showVersionViewDialog = ref(false)
const selectedVersion = ref(null)
const relationGraphRef = ref(null)

// Computed
const isEditMode = computed(() => route.query.edit === 'true')
const previewUrl = computed(() => {
  if (!fileInfo.value) return ''
  // ... existing previewUrl logic
  return `/api/file/preview/${fileInfo.value.id}`
})

// Methods
const loadDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}`)
    knowledge.value = res.data || {}
    
    // 初始化编辑表单
    editForm.value = { ...knowledge.value }
    
    // 加载文件信息
    if (knowledge.value.fileId) {
      try {
        const fileRes = await api.get(`/file/${knowledge.value.fileId}`)
        if (fileRes.code === 200) fileInfo.value = fileRes.data
      } catch (e) { console.warn('文件信息加载失败', e) }
    }
    
    // 并行加载其他信息
    await Promise.all([
      loadComments(),
      loadKnowledgeRelations(),
      loadVersions(),
      checkCollectStatus()
    ])
    
  } catch (error) {
    console.error('加载详情失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const checkCollectStatus = async () => {
    try {
        const res = await api.get(`/knowledge/${route.params.id}/collect/status?userId=${userStore.userInfo.id}`)
        isCollected.value = res.data
    } catch (e) {}
}

const loadComments = async () => {
    commentsLoading.value = true
    try {
        const res = await api.get(`/knowledge/${route.params.id}/comments`)
        comments.value = res.data || []
    } catch (e) {
        comments.value = []
    } finally {
        commentsLoading.value = false
    }
}

const loadKnowledgeRelations = async () => {
   try {
     // Load manual relations
     const res1 = await api.get(`/knowledge/${route.params.id}/relations`)
     knowledgeRelations.value = res1.code === 200 ? (res1.data || []) : []
     
     // Load auto-suggested relations
     // Note: Backend endpoint is /knowledge/{id}/related
     const res2 = await api.get(`/knowledge/${route.params.id}/related?limit=5`)
     suggestedRelations.value = res2.code === 200 ? (res2.data || []) : []

     if(activeRightTab.value === 'relations') nextTick(() => initRelationGraph())
   } catch(e) {
     console.error("Failed to load relations", e)
   }
}


const handleCollect = async () => {
  if (!userStore.userInfo?.id) return ElMessage.warning('请先登录')
  try {
    if (isCollected.value) {
      await api.delete(`/knowledge/${route.params.id}/collect`, { params: { userId: userStore.userInfo.id } })
      isCollected.value = false
      ElMessage.success('已取消收藏')
    } else {
      await api.post(`/knowledge/${route.params.id}/collect`, null, { params: { userId: userStore.userInfo.id } })
      isCollected.value = true
      ElMessage.success('已收藏')
    }
  } catch (e) { ElMessage.error('操作失败') }
}

const downloadFile = () => {
   if (!fileInfo.value) return
   window.open(`/api/file/download/${fileInfo.value.id}`)
}

const openInNewWindow = () => {
   if(fileInfo.value) window.open(getKkFileViewUrl())
   else window.open(window.location.href)
}

const getKkFileViewUrl = () => {
  if (!fileInfo.value) return ''
  // 核心逻辑：使用 host.docker.internal 替换 localhost，确保容器可以访问后端
  // 假设后端运行在 8080 端口（根据 Spring Boot 默认）
  // 原始下载URL应为后端直接地址
  const backendBase = 'http://host.docker.internal:8080' 
  const fileDownloadUrl = `${backendBase}/api/file/download/${fileInfo.value.id}`
  
  // kkFileView 需要通过 fullfilename 参数识别文件类型（当URL中不包含后缀时）
  // 注意：btoa 不能直接处理中文等 Unicode 字符，必须先对参数值进行 URL 编码
  const fullFileName = fileInfo.value.fileName || ''
  const finalDownloadUrl = `${fileDownloadUrl}?fullfilename=${encodeURIComponent(fullFileName)}`
  
  const kkBase = 'http://localhost:8012' // 浏览器访问 kkFileView 的地址
  // check if kkBase needs to show base64 encoded url
  return `${kkBase}/onlinePreview?url=${encodeURIComponent(btoa(finalDownloadUrl))}`
}

const getFileExtension = (filename) => {
  if (!filename) return ''
  return filename.split('.').pop().toUpperCase()
}

const checkFileType = (type, filename, validTypes) => {
  if (type && validTypes.includes(type.toUpperCase())) return true
  const ext = getFileExtension(filename)
  return validTypes.includes(ext)
}

const isPdfFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['PDF'])
const isImageFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['JPG','JPEG','PNG','GIF','WEBP'])
const isVideoFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['MP4','WEBM','OGG'])
const isAudioFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['MP3','WAV'])
const isOfficeFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['DOC','DOCX','XLS','XLSX','PPT','PPTX'])
const isTextFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['TXT','MD','JSON','XML','LOG', 'JAVA', 'JS', 'VUE', 'HTML', 'CSS'])

// Relations Graph
const initRelationGraph = () => {
  if (!relationGraphRef.value || !knowledge.value) return
  
  const myChart = echarts.init(relationGraphRef.value)
  const nodes = [{
    id: String(knowledge.value.id),
    name: knowledge.value.title,
    symbolSize: 30,
    itemStyle: { color: '#409EFF' }
  }]
  const links = []
  
  knowledgeRelations.value.forEach(rel => {
    if(rel.relatedKnowledge) {
      nodes.push({
        id: String(rel.relatedKnowledge.id),
        name: rel.relatedKnowledge.title,
        symbolSize: 20,
        itemStyle: { color: '#67C23A' },
        categoryId: rel.relatedKnowledge.id // for click
      })
      links.push({
        source: String(knowledge.value.id),
        target: String(rel.relatedKnowledge.id),
        value: getRelationTypeText(rel.relationType)
      })
    }
  })
  
  const option = {
    tooltip: {},
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      roam: true,
      label: { show: true, position: 'bottom' },
      force: { repulsion: 200, edgeLength: 100 },
      lineStyle: { curveness: 0.1 }
    }]
  }
  myChart.setOption(option)
  
  myChart.on('click', params => {
     if(params.data.categoryId) viewDetail(params.data.categoryId)
  })
  
  // Resize handler
  window.addEventListener('resize', () => myChart.resize())
}

watch(activeRightTab, (val) => {
  if(val === 'relations') {
    nextTick(() => initRelationGraph())
  }
})

// Comments
const loadComments = async () => {
  commentsLoading.value = true
  try {
     const res = await api.get(`/knowledge/${route.params.id}/comments`)
     comments.value = res.code === 200 ? (res.data || []) : []
  } catch(e) { comments.value = [] }
  finally { commentsLoading.value = false }
}

const submitComment = async () => {
  if(!newCommentContent.value.trim()) return
  if(!userStore.userInfo?.id) return ElMessage.warning('请登录')
  
  commentSubmitting.value = true
  try {
    await api.post(`/knowledge/${route.params.id}/comments`, { content: newCommentContent.value })
    newCommentContent.value = ''
    loadComments()
    ElMessage.success('评论成功')
  } catch(e) { ElMessage.error('评论失败') }
  finally { commentSubmitting.value = false }
}

// Helpers
const remoteMethod = async (query) => {
  if(!query) return
  searchLoading.value = true
  try {
     const res = await api.get('/knowledge/search', { params: { keyword: query, pageNum: 1, pageSize: 10 } })
     relationSearchResults.value = (res.data.list || []).filter(i => i.id !== route.params.id)
  } finally { searchLoading.value = false }
}

const loadKnowledgeRelations = async () => {
   try {
     const res = await api.get(`/knowledge/${route.params.id}/relations`)
     knowledgeRelations.value = res.code === 200 ? (res.data || []) : []
     if(activeRightTab.value === 'relations') nextTick(() => initRelationGraph())
   } catch(e) {}
}

const addKnowledgeRelation = async () => {
    try {
        const res = await api.post(`/knowledge/${route.params.id}/relations`, addRelationForm.value)
        if(res.code === 200) {
            ElMessage.success('已添加')
            showAddRelationDialog.value = false
            loadKnowledgeRelations()
        }
    } catch(e) { ElMessage.error('失败') }
}

// Other utils (formatTime, canEdit etc - kept simplified for brevity but functional)
const formatTime = (t) => t ? new Date(t).toLocaleString() : '-'
const formatTimeFriendly = (t) => {
   if(!t) return ''
   const date = new Date(t);
   const now = new Date();
   const diff = now - date;
   if(diff < 60000) return '刚刚';
   if(diff < 3600000) return Math.floor(diff/60000) + '分钟前';
   if(diff < 86400000) return Math.floor(diff/3600000) + '小时前';
   return date.toLocaleDateString();
}
const formatFileSize = (s) => {
    if(!s) return '0 B'
    const i = Math.floor(Math.log(s) / Math.log(1024))
    return (s / Math.pow(1024, i)).toFixed(2) + ' ' + ['B','KB','MB','GB'][i]
}
const canEdit = (k) => hasRole(userStore.userInfo, ROLE_ADMIN) || (hasRole(userStore.userInfo, ROLE_EDITOR) && k.author === userStore.userInfo.realName)
const canDeleteComment = (c) => hasRole(userStore.userInfo, ROLE_ADMIN) || c.userId === userStore.userInfo.id
const enterEditMode = () => router.push({ query: { ...route.query, edit: 'true' } })
const cancelEdit = () => router.back()
const viewDetail = (id) => router.push(`/knowledge/${id}`)
const stringToColor = (str) => {
   let hash = 0;
   for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
   const c = (hash & 0x00FFFFFF).toString(16).toUpperCase();
   return '#' + '00000'.substring(0, 6 - c.length) + c;
}

// Missing helper functions
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

const getRelationTypeTag = (type) => {
  const map = {
    'RELATED': 'primary',
    'CITED': 'success',
    'SIMILAR': 'info'
  }
  return map[type] || 'default'
}

const getRelationTypeText = (type) => {
  const map = {
    'RELATED': '相关',
    'CITED': '引用',
    'SIMILAR': '相似'
  }
  return map[type] || type
}

const loadVersions = async () => {
  try {
    const res = await api.get(`/knowledge/${route.params.id}/versions`)
    versions.value = res.data || []
  } catch (error) {
    console.error('加载版本列表失败', error)
  }
}

const viewVersion = (version) => {
  selectedVersion.value = version
  showVersionViewDialog.value = true
}

const compareVersion = (version) => {
  // Logic to handle compare - for now just show message or basic dialog
  // Since we simplified the compare logic in this rewrite, we can just alert or open simple dialog
  ElMessage.info('版本对比功能即将上线')
}

const replyToComment = (comment) => {
    newCommentContent.value = `回复 @${comment.userRealName || comment.userName}: `
    // You might want to track parentId properly if backend supports it
}

// Watchers
watch(() => route.params.id, loadDetail)

onMounted(() => {
    loadDetail()
})
</script>

<style scoped>
.knowledge-detail-container {
  height: calc(100vh - 84px); /* assuming navbar height */
  overflow: hidden;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.detail-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
  gap: 16px;
}

/* Header */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 12px 24px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2329;
}
.title-input {
  max-width: 400px;
}

/* Body */
.detail-body {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
}

/* Left Panel */
.left-panel {
  flex: 2;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.view-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.text-content-viewer {
  padding: 24px;
  overflow-y: auto;
}
.file-preview-viewer {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.preview-toolbar {
  padding: 8px 16px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fcfcfc;
}
.file-name {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}
.preview-stage {
  flex: 1;
  background: #525659; /* dark bg for files */
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: white;
}
.image-preview-box img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain; 
}
.office-preview-box {
    width: 100%;
    height: 100%;
}

/* Right Panel */
.right-panel {
  flex: 1;
  min-width: 320px;
  max-width: 400px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.right-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}
:deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
}
:deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

/* Properties */
.properties-panel {
    display: flex;
    flex-direction: column;
    gap: 20px;
}
.prop-group label {
    display: block;
    color: #8f959e;
    font-size: 12px;
    margin-bottom: 8px;
}
.prop-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 14px;
}
.prop-item .label {
    color: #606266;
}
.prop-item .value {
    color: #1f2329;
    font-weight: 500;
}
.summary-text {
    font-size: 14px;
    color: #606266;
    line-height: 1.6;
    margin: 0;
    background: #f5f7fa;
    padding: 8px;
    border-radius: 4px;
}
.tags-wrapper {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

/* Relations */
.relation-graph {
    width: 100%;
    height: 300px;
    background: #f9f9f9;
    border-radius: 4px;
    margin-bottom: 16px;
}
.relation-list-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px;
    border-radius: 4px;
    cursor: pointer;
    transition: background 0.2s;
}
.relation-list-item:hover {
    background: #f5f7fa;
}
.rel-icon {
    width: 32px;
    height: 32px;
    background: #e6f0ff;
    color: #409eff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}
.rel-info {
    flex: 1;
    overflow: hidden;
}
.rel-title {
    font-size: 14px;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
}

/* Comments */
.comment-input-box {
    margin-bottom: 20px;
}
.comment-btn-row {
    margin-top: 8px;
    text-align: right;
}
.comment-bubble {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
}
.comment-avatar {
    flex-shrink: 0;
}
.comment-body {
    flex: 1;
}
.comment-meta {
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
    display: flex;
    justify-content: space-between;
}
.comment-text {
    font-size: 14px;
    color: #303133;
    line-height: 1.5;
    background: #f5f7fa;
    padding: 8px 12px;
    border-radius: 0 8px 8px 8px;
}
.comment-ops {
    margin-top: 4px;
    font-size: 12px;
}
.op-btn {
    cursor: pointer;
    color: #409eff;
    margin-right: 12px;
}
.op-btn.delete {
    color: #f56c6c;
}
.replies-wrapper {
    margin-top: 8px;
    margin-left: 8px;
    padding-left: 8px;
    border-left: 2px solid #eee;
}
.reply-bubble {
    margin-bottom: 8px;
}
.reply-meta {
    font-size: 12px;
    color: #909399;
}
.reply-text {
    font-size: 13px;
    color: #606266;
}

/* Edit Form */
.edit-form-container {
    padding: 24px;
    max-width: 800px;
    margin: 0 auto;
}
</style>
