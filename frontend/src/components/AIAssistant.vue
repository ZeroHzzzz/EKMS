<template>
  <div class="ai-assistant-wrapper">
    <!-- 浮动按钮 -->
    <transition name="fab-fade">
      <div 
        v-if="!isOpen" 
        class="ai-fab"
        :class="{ 'drag-over': isDragOver }"
        @click="toggleOpen"
        @dragover.prevent="handleDragOver"
        @dragleave="handleDragLeave"
        @drop.prevent="handleDrop"
      >
        <div class="fab-pulse"></div>
        <el-icon class="fab-icon"><ChatDotRound /></el-icon>
        <span class="fab-label">{{ isDragOver ? '放开添加文档' : 'AI助手' }}</span>
        <!-- 已选文档数量徽章 -->
        <span v-if="selectedDocuments.length > 0 && !isDragOver" class="fab-badge">
          {{ selectedDocuments.length }}
        </span>
      </div>
    </transition>

    <!-- 对话窗口 -->
    <transition name="chat-slide">
      <div 
        v-if="isOpen" 
        class="ai-chat-window" 
        :class="{ minimized: isMinimized, 'drag-over': isDragOver }"
        @dragover.prevent="handleDragOver"
        @dragleave="handleDragLeave"
        @drop.prevent="handleDrop"
      >
        <!-- 头部 -->
        <div class="chat-header" @dblclick="toggleMinimize">
          <div class="header-left">
            <div class="ai-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="header-info">
              <span class="ai-name">AI 知识助手</span>
              <span class="ai-status">
                <span class="status-dot" :class="{ typing: isLoading }"></span>
                {{ isLoading ? '思考中...' : '在线' }}
              </span>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip content="清空对话" placement="top">
              <el-icon class="action-btn" @click="clearChat"><Delete /></el-icon>
            </el-tooltip>
            <el-tooltip content="最小化" placement="top">
              <el-icon class="action-btn" @click="toggleMinimize"><Minus /></el-icon>
            </el-tooltip>
            <el-tooltip content="关闭" placement="top">
              <el-icon class="action-btn close-btn" @click="toggleOpen"><Close /></el-icon>
            </el-tooltip>
          </div>
        </div>

        <!-- 拖拽提示层 -->
        <div v-if="isDragOver && !isMinimized" class="drop-overlay">
          <div class="drop-content">
            <el-icon class="drop-icon"><DocumentAdd /></el-icon>
            <span>放开添加为参考文档</span>
          </div>
        </div>

        <!-- 已选文档栏 -->
        <div v-show="!isMinimized && selectedDocuments.length > 0" class="selected-docs-bar">
          <div class="docs-bar-header">
            <el-icon><Folder /></el-icon>
            <span>参考文档 ({{ selectedDocuments.length }})</span>
            <el-button link type="danger" size="small" @click="clearSelectedDocuments">清空</el-button>
          </div>
          <div class="docs-bar-list">
            <el-tag
              v-for="doc in selectedDocuments"
              :key="doc.id"
              closable
              size="small"
              @close="removeSelectedDocument(doc.id)"
              class="doc-tag"
            >
              {{ doc.title }}
            </el-tag>
          </div>
        </div>

        <!-- 消息区域 -->
        <div v-show="!isMinimized" class="chat-messages" ref="messagesRef">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-section">
            <div class="welcome-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <h3>你好！我是知识库AI助手</h3>
            <p>我可以帮你搜索文档、基于文档回答问题</p>
            <div class="welcome-tips">
              <div class="tip-item">
                <el-icon><Document /></el-icon>
                <span>拖拽文档到这里作为参考</span>
              </div>
              <div class="tip-item">
                <el-icon><Search /></el-icon>
                <span>输入问题，我会搜索相关文档</span>
              </div>
            </div>
            <div class="quick-questions">
              <div 
                v-for="(q, idx) in quickQuestions" 
                :key="idx" 
                class="quick-btn"
                @click="sendMessage(q)"
              >
                {{ q }}
              </div>
            </div>
          </div>

          <!-- 消息列表 -->
          <template v-for="(msg, idx) in messages" :key="idx">
            <div class="message-item" :class="msg.role">
              <div class="message-avatar">
                <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>
              <div class="message-content">
                <!-- 普通消息 -->
                <div v-if="msg.type !== 'doc-selection'" class="message-bubble" v-html="renderMarkdown(msg.content)"></div>
                
                <!-- 文档选择消息 -->
                <div v-else class="message-bubble doc-selection-bubble">
                  <div class="doc-selection-header">
                    <el-icon><Search /></el-icon>
                    <span>{{ msg.content }}</span>
                  </div>
                  <div class="doc-selection-list">
                    <div 
                      v-for="doc in msg.documents" 
                      :key="doc.id" 
                      class="doc-selection-item"
                      :class="{ selected: isDocPendingSelected(doc.id), disabled: msg.confirmed }"
                      @click="!msg.confirmed && togglePendingDocSelection(doc)"
                    >
                      <div class="doc-checkbox">
                        <el-icon v-if="isDocPendingSelected(doc.id)" class="check-icon"><Select /></el-icon>
                        <span v-else class="checkbox-empty"></span>
                      </div>
                      <div class="doc-info">
                        <div class="doc-title">{{ doc.title }}</div>
                        <div class="doc-meta">
                          <span v-if="doc.keywords" class="doc-keywords">{{ doc.keywords }}</span>
                        </div>
                      </div>
                      <el-button 
                        size="small" 
                        link 
                        type="primary"
                        @click.stop="viewDocument(doc.id)"
                      >
                        预览
                      </el-button>
                    </div>
                  </div>
                  <div v-if="!msg.confirmed" class="doc-selection-actions">
                    <el-button size="small" @click="skipDocSelection(idx)">
                      跳过
                    </el-button>
                    <el-button 
                      type="primary" 
                      size="small" 
                      :disabled="pendingDocSelections.length === 0"
                      @click="confirmDocSelection(idx)"
                    >
                      确认选择 ({{ pendingDocSelections.length }})
                    </el-button>
                  </div>
                  <div v-else class="doc-selection-confirmed">
                    <el-icon><Select /></el-icon>
                    <span v-if="msg.selectedCount > 0">已选择 {{ msg.selectedCount }} 个文档</span>
                    <span v-else>已跳过</span>
                  </div>
                </div>
                
                <!-- 引用的文档 -->
                <div v-if="msg.documents && msg.documents.length > 0 && msg.type !== 'doc-selection'" class="ref-documents">
                  <div class="ref-title">
                    <el-icon><Document /></el-icon>
                    <span>参考文档</span>
                  </div>
                  <div class="ref-list">
                    <div 
                      v-for="doc in msg.documents" 
                      :key="doc.id" 
                      class="ref-item"
                      @click="viewDocument(doc.id)"
                    >
                      <el-icon><Document /></el-icon>
                      <span class="ref-name">{{ doc.title }}</span>
                      <el-icon class="ref-arrow"><ArrowRight /></el-icon>
                    </div>
                  </div>
                </div>

                <div class="message-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>
          </template>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="message-item assistant">
            <div class="message-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-bubble typing">
                <div v-if="loadingPhase === 'searching'" class="loading-status">
                  <div class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                  <span class="loading-text">正在搜索相关文档...</span>
                </div>
                <div v-else-if="loadingPhase === 'analyzing'" class="loading-status">
                  <div class="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                  <span class="loading-text">正在分析文档内容...</span>
                </div>
                <div v-else-if="streamingContent" class="streaming-text" v-html="renderMarkdown(streamingContent)"></div>
                <div v-else class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div v-show="!isMinimized" class="chat-input-area">
          <!-- 等待输入提示 -->
          <div v-if="waitingForQuestion" class="waiting-hint">
            <el-icon><EditPen /></el-icon>
            <span>请输入您的问题</span>
          </div>
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 4 }"
              :placeholder="inputPlaceholder"
              @keydown.enter.exact.prevent="handleSend"
              @keydown.enter.shift.exact="handleNewLine"
              :disabled="isLoading && !streamingContent"
            />
            <el-button 
              v-if="!isLoading"
              type="primary" 
              class="send-btn"
              :disabled="!inputText.trim()"
              @click="handleSend"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
            <el-button 
              v-else
              type="danger" 
              class="send-btn stop-btn"
              @click="stopGeneration"
            >
              <el-icon><VideoPause /></el-icon>
            </el-button>
          </div>
          <div class="input-hint">
            <span>Enter 发送，Shift+Enter 换行</span>
            <span v-if="selectedDocuments.length > 0" class="context-hint">
              · 基于 {{ selectedDocuments.length }} 个文档回答
            </span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ChatDotRound, User, Document, ArrowRight, 
  Delete, Minus, Close, Promotion, Loading,
  Folder, Search, Select, DocumentAdd, EditPen, VideoPause
} from '@element-plus/icons-vue'
import { 
  askAssistantStream, 
  searchKnowledge, 
  getKnowledgeDetail,
  callDeepSeekStream,
  analyzeIntent
} from '../api/aiAssistant'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const router = useRouter()

// 状态
const isOpen = ref(false)
const isMinimized = ref(false)
const isLoading = ref(false)
const inputText = ref('')
const messages = ref([])
const messagesRef = ref(null)

// 流式输出相关
const streamingContent = ref('')
const loadingPhase = ref('')  // 'searching' | 'analyzing' | 'generating'
const abortController = ref(null)

// 拖拽相关
const isDragOver = ref(false)

// 文档选择相关
const selectedDocuments = ref([])  // 已选择的参考文档
const pendingDocSelections = ref([])  // 当前待确认的文档选择
const waitingForQuestion = ref(false)  // 等待用户输入问题

// 快捷问题
const quickQuestions = [
  '帮我找一下最近上传的文档',
  '知识库有哪些分类？',
  '如何使用知识库？'
]

// 计算属性
const inputPlaceholder = computed(() => {
  if (waitingForQuestion.value) {
    return '请输入您的问题...'
  }
  if (selectedDocuments.value.length > 0) {
    return '基于选中的文档提问...'
  }
  return '输入问题，AI会搜索相关文档...'
})

// 拖拽事件处理
const handleDragOver = (e) => {
  const types = e.dataTransfer?.types || []
  if (types.includes('application/json') || types.includes('text/plain')) {
    isDragOver.value = true
  }
}

const handleDragLeave = (e) => {
  // 检查是否真的离开了元素
  const rect = e.currentTarget.getBoundingClientRect()
  if (
    e.clientX < rect.left ||
    e.clientX > rect.right ||
    e.clientY < rect.top ||
    e.clientY > rect.bottom
  ) {
    isDragOver.value = false
  }
}

const handleDrop = (e) => {
  isDragOver.value = false
  
  try {
    // 尝试解析拖拽数据
    const jsonData = e.dataTransfer.getData('application/json')
    if (jsonData) {
      const data = JSON.parse(jsonData)
      if (data.type === 'knowledge-document' && data.document) {
        addDocumentToContext(data.document)
        // 如果窗口没打开，打开它
        if (!isOpen.value) {
          isOpen.value = true
          isMinimized.value = false
        }
        return
      }
    }
    
    // 尝试纯文本
    const textData = e.dataTransfer.getData('text/plain')
    if (textData) {
      try {
        const data = JSON.parse(textData)
        if (data.type === 'knowledge-document' && data.document) {
          addDocumentToContext(data.document)
          if (!isOpen.value) {
            isOpen.value = true
            isMinimized.value = false
          }
        }
      } catch {
        // 不是JSON，忽略
      }
    }
  } catch (error) {
    console.error('处理拖拽数据失败:', error)
  }
}

// 添加文档到上下文
const addDocumentToContext = (doc) => {
  if (!doc || !doc.id) return
  
  // 检查是否已存在
  if (selectedDocuments.value.some(d => d.id === doc.id)) {
    ElMessage.info('该文档已在参考列表中')
    return
  }
  
  selectedDocuments.value.push({
    id: doc.id,
    title: doc.title || '未命名文档',
    keywords: doc.keywords || ''
  })
  
  ElMessage.success(`已添加文档: ${doc.title}`)
  waitingForQuestion.value = true
}

// 移除选中的文档
const removeSelectedDocument = (docId) => {
  selectedDocuments.value = selectedDocuments.value.filter(d => d.id !== docId)
  if (selectedDocuments.value.length === 0) {
    waitingForQuestion.value = false
  }
}

// 清空选中的文档
const clearSelectedDocuments = () => {
  selectedDocuments.value = []
  waitingForQuestion.value = false
}

// 切换打开状态
const toggleOpen = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    isMinimized.value = false
  }
}

// 切换最小化
const toggleMinimize = () => {
  isMinimized.value = !isMinimized.value
}

// 清空对话
const clearChat = () => {
  messages.value = []
  streamingContent.value = ''
  selectedDocuments.value = []
  pendingDocSelections.value = []
  waitingForQuestion.value = false
}

// 发送消息
const handleSend = () => {
  if (!inputText.value.trim() || (isLoading.value && !streamingContent.value)) return
  sendMessage(inputText.value.trim())
  inputText.value = ''
}

// 换行处理
const handleNewLine = () => {
  // Shift+Enter 换行
}

// 停止生成
const stopGeneration = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  
  // 添加停止标记
  if (streamingContent.value) {
    messages.value.push({
      role: 'assistant',
      content: streamingContent.value + '\n\n*[已停止生成]*',
      time: new Date()
    })
  }
  
  isLoading.value = false
  streamingContent.value = ''
  loadingPhase.value = ''
}

// 文档选择相关方法
const isDocPendingSelected = (docId) => {
  return pendingDocSelections.value.some(d => d.id === docId)
}

const togglePendingDocSelection = (doc) => {
  const index = pendingDocSelections.value.findIndex(d => d.id === doc.id)
  if (index > -1) {
    pendingDocSelections.value.splice(index, 1)
  } else {
    pendingDocSelections.value.push(doc)
  }
}

const confirmDocSelection = (msgIdx) => {
  // 将待选文档添加到已选列表
  for (const doc of pendingDocSelections.value) {
    if (!selectedDocuments.value.some(d => d.id === doc.id)) {
      selectedDocuments.value.push(doc)
    }
  }
  
  // 标记消息为已确认
  messages.value[msgIdx].confirmed = true
  messages.value[msgIdx].selectedCount = pendingDocSelections.value.length
  
  // 清空待选列表
  pendingDocSelections.value = []
  
  // 等待用户输入问题
  waitingForQuestion.value = true
  
  scrollToBottom()
}

const skipDocSelection = (msgIdx) => {
  messages.value[msgIdx].confirmed = true
  messages.value[msgIdx].selectedCount = 0
  pendingDocSelections.value = []
  waitingForQuestion.value = true
  scrollToBottom()
}

// 发送消息核心逻辑
const sendMessage = async (text) => {
  if (!text.trim() || isLoading.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    time: new Date()
  })

  scrollToBottom()
  waitingForQuestion.value = false
  
  // 如果已有选中的文档，直接基于文档回答
  if (selectedDocuments.value.length > 0) {
    await answerWithSelectedDocuments(text)
    return
  }
  
  // 否则，先搜索文档，让用户选择
  await searchAndShowDocuments(text)
}

// 搜索并显示文档供选择
const searchAndShowDocuments = async (question) => {
  isLoading.value = true
  loadingPhase.value = 'searching'
  
  try {
    // 分析意图获取关键词
    const intent = await analyzeIntent(question)
    
    if (intent.intent === 'CHAT' || intent.intent === 'HELP') {
      // 闲聊或帮助，直接回答
      loadingPhase.value = 'generating'
      await directAnswer(question, intent)
      return
    }
    
    // 搜索文档
    let documents = []
    if (intent.keywords && intent.keywords.length > 0) {
      for (const keyword of intent.keywords.slice(0, 2)) {
        const results = await searchKnowledge(keyword, 5)
        documents = [...documents, ...results]
      }
      
      // 去重
      const seen = new Set()
      documents = documents.filter(doc => {
        if (seen.has(doc.id)) return false
        seen.add(doc.id)
        return true
      }).slice(0, 5)
    }
    
    loadingPhase.value = ''
    isLoading.value = false
    
    if (documents.length > 0) {
      // 显示文档选择界面
      messages.value.push({
        role: 'assistant',
        type: 'doc-selection',
        content: `找到 ${documents.length} 个相关文档，请选择要参考的文档：`,
        documents: documents.map(doc => ({
          id: doc.id,
          title: doc.title,
          keywords: doc.keywords
        })),
        confirmed: false,
        time: new Date()
      })
    } else {
      // 没找到文档，直接回答
      messages.value.push({
        role: 'assistant',
        content: '未找到相关文档，我将直接回答您的问题：',
        time: new Date()
      })
      await directAnswer(question, intent)
    }
    
  } catch (error) {
    console.error('搜索文档失败:', error)
    isLoading.value = false
    loadingPhase.value = ''
    messages.value.push({
      role: 'assistant',
      content: '抱歉，搜索文档时出现问题。',
      time: new Date()
    })
  }
  
  scrollToBottom()
}

// 基于选中文档回答
const answerWithSelectedDocuments = async (question) => {
  isLoading.value = true
  loadingPhase.value = 'analyzing'
  streamingContent.value = ''
  
  abortController.value = new AbortController()
  
  try {
    // 获取文档详情
    const documentContents = []
    for (const doc of selectedDocuments.value) {
      try {
        const detail = await getKnowledgeDetail(doc.id)
        if (detail) {
          documentContents.push({
            id: doc.id,
            title: detail.title || doc.title,
            content: detail.contentText || detail.content || '',
            keywords: detail.keywords || doc.keywords
          })
        }
      } catch (e) {
        documentContents.push({
          id: doc.id,
          title: doc.title,
          content: '',
          keywords: doc.keywords
        })
      }
    }
    
    loadingPhase.value = 'generating'
    
    // 构建上下文
    let contextPrompt = '\n\n以下是用户选择的参考文档：\n\n'
    const maxContentPerDoc = Math.floor(6000 / documentContents.length)
    
    documentContents.forEach((doc, index) => {
      const contentPreview = (doc.content || '').substring(0, maxContentPerDoc)
      contextPrompt += `【文档${index + 1}】${doc.title}\n`
      contextPrompt += `内容：${contentPreview || '无内容'}\n`
      if (doc.keywords) {
        contextPrompt += `关键词：${doc.keywords}\n`
      }
      contextPrompt += '\n---\n\n'
    })
    
    const systemPrompt = `你是企业知识库的AI助手。你的任务是基于用户选择的参考文档回答问题。

规则：
1. 优先使用参考文档中的内容来回答问题
2. 如果文档中有相关信息，请引用并说明来源（如"根据《XX》文档..."）
3. 如果文档中没有相关信息，请诚实告知
4. 回答要简洁、准确、有条理
5. 可以使用markdown格式使回答更清晰
${contextPrompt}`
    
    const history = messages.value
      .filter(m => m.role !== 'system' && m.type !== 'doc-selection')
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }))
    
    await callDeepSeekStream(
      [
        { role: 'system', content: systemPrompt },
        ...history.slice(0, -1),
        { role: 'user', content: question }
      ],
      (chunk) => {
        streamingContent.value += chunk
        scrollToBottom()
      },
      { signal: abortController.value.signal }
    )
    
    // 添加AI回复
    messages.value.push({
      role: 'assistant',
      content: streamingContent.value,
      documents: selectedDocuments.value.map(d => ({ id: d.id, title: d.title })),
      time: new Date()
    })
    
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('AI回复失败:', error)
      messages.value.push({
        role: 'assistant',
        content: '抱歉，我遇到了一些问题，请稍后再试。',
        time: new Date()
      })
      ElMessage.error('AI服务暂时不可用')
    }
  } finally {
    isLoading.value = false
    streamingContent.value = ''
    loadingPhase.value = ''
    abortController.value = null
    scrollToBottom()
  }
}

// 直接回答（无文档上下文）
const directAnswer = async (question, intent) => {
  isLoading.value = true
  loadingPhase.value = 'generating'
  streamingContent.value = ''
  
  abortController.value = new AbortController()
  
  try {
    if (intent?.intent === 'HELP') {
      const helpText = `我是企业知识库的AI助手，可以帮您：

🔍 **搜索文档** - 告诉我您想找什么，我会帮您搜索
📖 **回答问题** - 基于知识库中的文档回答您的问题
💡 **推荐内容** - 根据您的需求推荐相关文档

**使用技巧：**
- 从知识结构页面拖拽文档到这里作为参考
- 搜索到文档后选择需要的，再提问`
      
      messages.value.push({
        role: 'assistant',
        content: helpText,
        time: new Date()
      })
      isLoading.value = false
      loadingPhase.value = ''
      return
    }
    
    const history = messages.value
      .filter(m => m.role !== 'system' && m.type !== 'doc-selection')
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }))
    
    await callDeepSeekStream(
      [
        { role: 'system', content: '你是企业知识库的AI助手，友好地与用户交流。' },
        ...history.slice(0, -1),
        { role: 'user', content: question }
      ],
      (chunk) => {
        streamingContent.value += chunk
        scrollToBottom()
      },
      { signal: abortController.value.signal }
    )
    
    messages.value.push({
      role: 'assistant',
      content: streamingContent.value,
      time: new Date()
    })
    
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('AI回复失败:', error)
      messages.value.push({
        role: 'assistant',
        content: '抱歉，我遇到了一些问题。',
        time: new Date()
      })
    }
  } finally {
    isLoading.value = false
    streamingContent.value = ''
    loadingPhase.value = ''
    abortController.value = null
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 渲染Markdown
const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    const html = marked.parse(content, { breaks: true })
    return DOMPurify.sanitize(html)
  } catch (e) {
    return content
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

// 查看文档
const viewDocument = (id) => {
  router.push(`/knowledge/${id}`)
}

// 监听消息变化，自动滚动
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// 暴露方法供外部调用（如拖拽）
defineExpose({
  addDocumentToContext,
  isOpen,
  toggleOpen
})
</script>

<style scoped>
/* 容器 */
.ai-assistant-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 浮动按钮 */
.ai-fab {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  border-radius: 50px;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(64, 158, 255, 0.45);
}

.ai-fab.drag-over {
  transform: scale(1.1);
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  box-shadow: 0 6px 28px rgba(103, 194, 58, 0.5);
}

.fab-pulse {
  position: absolute;
  inset: 0;
  border-radius: 50px;
  background: inherit;
  animation: pulse 2s infinite;
  z-index: -1;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.1); opacity: 0; }
  100% { transform: scale(1); opacity: 0; }
}

.fab-icon {
  font-size: 20px;
}

.fab-label {
  font-size: 14px;
  font-weight: 500;
}

.fab-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: #f56c6c;
  color: white;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 对话窗口 */
.ai-chat-window {
  width: 420px;
  height: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
}

.ai-chat-window.minimized {
  height: auto;
}

.ai-chat-window.drag-over {
  box-shadow: 0 8px 40px rgba(103, 194, 58, 0.4);
  border: 2px dashed #67c23a;
}

/* 拖拽提示层 */
.drop-overlay {
  position: absolute;
  inset: 72px 0 0 0;
  background: rgba(103, 194, 58, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.drop-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: white;
}

.drop-icon {
  font-size: 48px;
}

.drop-content span {
  font-size: 16px;
  font-weight: 500;
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  cursor: default;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.ai-name {
  font-weight: 600;
  font-size: 15px;
}

.ai-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  opacity: 0.9;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #4ade80;
  border-radius: 50%;
}

.status-dot.typing {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  font-size: 16px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.3);
}

/* 已选文档栏 */
.selected-docs-bar {
  padding: 12px 16px;
  background: #f0f7ff;
  border-bottom: 1px solid #d9ecff;
}

.docs-bar-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: #409eff;
  font-size: 13px;
}

.docs-bar-header span {
  flex: 1;
}

.docs-bar-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.doc-tag {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9fafb;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

/* 欢迎区域 */
.welcome-section {
  text-align: center;
  padding: 30px 20px;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32px;
}

.welcome-section h3 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 18px;
}

.welcome-section p {
  margin: 0 0 16px;
  color: #6b7280;
  font-size: 14px;
}

.welcome-tips {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.tip-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
}

.tip-item .el-icon {
  color: #409eff;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-btn {
  padding: 10px 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.quick-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

/* 消息项 */
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.message-item.assistant .message-avatar {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
}

.message-item.user .message-avatar {
  background: #e5e7eb;
  color: #6b7280;
}

.message-content {
  max-width: 85%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item.assistant .message-bubble {
  background: white;
  color: #1f2937;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

/* 文档选择样式 */
.doc-selection-bubble {
  background: white !important;
  padding: 16px !important;
}

.doc-selection-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #409eff;
  font-weight: 500;
}

.doc-selection-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.doc-selection-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.doc-selection-item:hover:not(.disabled) {
  background: #ecf5ff;
}

.doc-selection-item.selected {
  background: #ecf5ff;
  border: 1px solid #409eff;
}

.doc-selection-item.disabled {
  cursor: default;
  opacity: 0.7;
}

.doc-checkbox {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkbox-empty {
  width: 16px;
  height: 16px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
}

.check-icon {
  color: #409eff;
  font-size: 18px;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: 14px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.doc-keywords {
  color: #6b7280;
}

.doc-selection-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}

.doc-selection-confirmed {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
  color: #67c23a;
  font-size: 13px;
}

/* Markdown样式 */
.message-bubble :deep(p) {
  margin: 0 0 8px;
}

.message-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.message-bubble :deep(ul), 
.message-bubble :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-bubble :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.message-bubble :deep(pre) {
  background: #1f2937;
  color: #e5e7eb;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-bubble :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.message-bubble :deep(strong) {
  font-weight: 600;
}

/* 打字指示器 */
.message-bubble.typing {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-height: 24px;
}

.loading-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-text {
  color: #6b7280;
  font-size: 13px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #9ca3af;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.streaming-text {
  flex: 1;
}

/* 参考文档 */
.ref-documents {
  margin-top: 12px;
  background: #f9fafb;
  border-radius: 10px;
  padding: 12px;
}

.ref-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 12px;
  margin-bottom: 8px;
}

.ref-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ref-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: #4b5563;
}

.ref-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.ref-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ref-arrow {
  opacity: 0;
  transition: opacity 0.2s;
}

.ref-item:hover .ref-arrow {
  opacity: 1;
}

.message-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
  padding: 0 4px;
}

.message-item.user .message-time {
  text-align: right;
}

/* 输入区域 */
.chat-input-area {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #f3f4f6;
}

.waiting-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  color: #409eff;
  font-size: 13px;
}

.input-wrapper {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  resize: none;
  border-color: #e5e7eb;
  transition: all 0.2s;
}

.input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  padding: 0;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border: none;
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #337ecc 0%, #53a1e6 100%);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.stop-btn {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%) !important;
}

.stop-btn:hover {
  background: linear-gradient(135deg, #e45656 0%, #ef8888 100%) !important;
}

.input-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
}

.context-hint {
  color: #409eff;
}

/* 动画 */
.fab-fade-enter-active,
.fab-fade-leave-active {
  transition: all 0.3s ease;
}

.fab-fade-enter-from,
.fab-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 响应式 */
@media (max-width: 480px) {
  .ai-assistant-wrapper {
    bottom: 16px;
    right: 16px;
    left: 16px;
  }

  .ai-chat-window {
    width: 100%;
    height: calc(100vh - 100px);
    border-radius: 12px;
  }

  .fab-label {
    display: none;
  }

  .ai-fab {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    padding: 0;
    justify-content: center;
  }
}
</style>

