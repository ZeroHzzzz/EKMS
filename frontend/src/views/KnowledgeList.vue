<template>
  <div class="knowledge-search-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <!-- 初始搜索界面 -->
    <div class="initial-view" v-if="isInitialState">
      <!-- 搜索模式 -->
      <div v-if="initialViewMode === 'search'" class="search-mode-view">
        <div class="hero-section">
          <div class="logo-icon">
            <svg viewBox="0 0 100 100" class="logo-svg">
              <defs>
                <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#409eff"/>
                  <stop offset="50%" style="stop-color:#66b1ff"/>
                  <stop offset="100%" style="stop-color:#a0cfff"/>
                </linearGradient>
                <filter id="glow">
                  <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
                  <feMerge>
                    <feMergeNode in="coloredBlur"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              </defs>
              <circle cx="50" cy="50" r="45" fill="url(#logoGrad)" opacity="0.15"/>
              <path d="M30 35 L50 25 L70 35 L70 55 L50 65 L30 55 Z" fill="none" stroke="url(#logoGrad)" stroke-width="2.5" filter="url(#glow)"/>
              <path d="M50 25 L50 65" stroke="url(#logoGrad)" stroke-width="2" opacity="0.8"/>
              <path d="M30 45 L70 45" stroke="url(#logoGrad)" stroke-width="2" opacity="0.8"/>
              <circle cx="50" cy="75" r="5" fill="url(#logoGrad)" filter="url(#glow)"/>
              <circle cx="30" cy="35" r="3" fill="#66b1ff" opacity="0.8"/>
              <circle cx="70" cy="35" r="3" fill="#66b1ff" opacity="0.8"/>
              <circle cx="50" cy="25" r="3" fill="#a0cfff"/>
            </svg>
          </div>
          <h1 class="hero-title">企业知识库</h1>
          <p class="hero-subtitle">智能搜索，快速定位您需要的知识资源</p>
          
          <div class="search-container">
            <div class="search-box">
              <div class="search-icon">
                <el-icon><Search /></el-icon>
              </div>
              <input
                type="text"
                v-model="searchKeyword"
                placeholder="输入关键词搜索知识..."
                class="search-input"
                @keyup.enter="handleSearch"
              />
              <button class="search-btn" @click="handleSearch">
                <span>搜索</span>
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
          </div>

          <!-- 热门搜索 -->
          <div class="suggestions-area" v-if="hotSearchList.length > 0 || historyTags.length > 0">
            <div class="hot-searches" v-if="hotSearchList.length > 0">
              <div class="section-label">
                <el-icon class="fire-icon"><TrendCharts /></el-icon>
                <span>热门搜索</span>
              </div>
              <div class="tags-container">
                <button
                  v-for="(item, index) in hotSearchList"
                  :key="item"
                  class="tag-btn"
                  :class="{ 'tag-hot': index < 3 }"
                  @click="quickSearch(item)"
                >
                  <span v-if="index < 3" class="hot-rank">{{ index + 1 }}</span>
                  {{ item }}
                </button>
              </div>
            </div>

            <div class="recent-searches" v-if="historyTags.length > 0">
              <div class="section-label">
                <el-icon><Clock /></el-icon>
                <span>最近搜索</span>
              </div>
              <div class="tags-container">
                <button
                  v-for="tag in historyTags"
                  :key="tag"
                  class="tag-btn tag-history"
                  @click="quickSearch(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- AI助手模式 -->
      <div v-else class="ai-mode-view">
        <!-- 初始欢迎界面 -->
        <div v-if="aiChatMessages.length === 0 && !aiSearchLoading" class="hero-section ai-hero">
          <div class="ai-logo-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <h1 class="hero-title">AI 智能助手</h1>
          <p class="hero-subtitle">自然语言交互，智能搜索知识文档</p>
          
          <!-- AI输入框 -->
          <div class="search-container">
            <div class="search-box ai-input-box">
              <div class="search-icon">
                <el-icon><EditPen /></el-icon>
              </div>
              <input
                type="text"
                v-model="aiSearchInput"
                placeholder="输入您的问题，AI将为您智能搜索..."
                class="search-input"
                @keyup.enter="sendAiSearchQuery"
              />
              <button 
                class="search-btn" 
                @click="sendAiSearchQuery" 
                :disabled="!aiSearchInput.trim()"
              >
                <span>发送</span>
                <el-icon><Promotion /></el-icon>
              </button>
            </div>
          </div>

          <!-- 功能提示 -->
          <div class="ai-features">
            <div class="feature-card">
              <el-icon><Search /></el-icon>
              <span>精准搜索知识文档</span>
            </div>
            <div class="feature-card">
              <el-icon><Document /></el-icon>
              <span>智能推荐相关内容</span>
            </div>
            <div class="feature-card">
              <el-icon><ChatDotRound /></el-icon>
              <span>自然语言问答交互</span>
            </div>
          </div>

          <div class="ai-tip">
            💡 试试问我："帮我找一下关于项目管理的文档"
          </div>
        </div>

        <!-- 聊天界面（发送消息后显示） -->
        <div v-else class="ai-chat-mode">
          <!-- 顶部操作栏 -->
          <div class="ai-chat-header">
            <button class="new-chat-btn" @click="clearAiChat">
              <el-icon><Plus /></el-icon>
              新对话
            </button>
            <!-- 已选文档指示器 -->
            <div 
              v-if="aiSelectedDocuments.length > 0" 
              class="selected-docs-indicator"
              @click="showAiSelectedDocs = !showAiSelectedDocs"
            >
              <el-icon><Document /></el-icon>
              <span>{{ aiSelectedDocuments.length }} 个文档</span>
              <el-icon class="arrow-icon" :class="{ expanded: showAiSelectedDocs }"><ArrowRight /></el-icon>
            </div>
          </div>

          <!-- 已选文档面板 -->
          <transition name="slide-down">
            <div v-if="showAiSelectedDocs && aiSelectedDocuments.length > 0" class="selected-docs-panel">
              <div class="panel-title">已选择的参考文档</div>
              <div class="panel-docs-list">
                <div v-for="doc in aiSelectedDocuments" :key="doc.id" class="panel-doc-item">
                  <el-icon><Document /></el-icon>
                  <span class="doc-name">{{ doc.title }}</span>
                  <el-icon class="remove-btn" @click="removeAiSelectedDoc(doc.id)"><Close /></el-icon>
                </div>
              </div>
              <div class="panel-actions">
                <el-button size="small" type="danger" text @click="clearAiSelectedDocs">
                  清空所有
                </el-button>
              </div>
            </div>
          </transition>

          <!-- 聊天消息区域 -->
          <div class="chat-messages-area" ref="aiChatRef">
            <div 
              v-for="(msg, idx) in aiChatMessages" 
              :key="idx" 
              class="chat-msg"
              :class="msg.role"
            >
              <div class="msg-icon">
                <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>
              
              <!-- 普通文本消息 -->
              <div v-if="msg.type !== 'doc-selection'" class="msg-bubble">
                <div v-html="renderAiMarkdown(msg.content)"></div>
                <!-- 引用的文档 -->
                <div v-if="msg.documents && msg.documents.length > 0" class="msg-ref-docs">
                  <div class="ref-docs-title">
                    <el-icon><Document /></el-icon>
                    <span>参考文档</span>
                  </div>
                  <div class="ref-docs-list">
                    <div 
                      v-for="doc in msg.documents" 
                      :key="doc.id" 
                      class="ref-doc-item"
                      @click="viewDetail(doc.id)"
                    >
                      {{ doc.title }}
                      <el-icon><ArrowRight /></el-icon>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 文档选择消息 -->
              <div v-else class="msg-bubble doc-selection-bubble">
                <div class="doc-selection-header">
                  <el-icon><Search /></el-icon>
                  <span>{{ msg.content }}</span>
                </div>
                <div class="doc-selection-list">
                  <div 
                    v-for="doc in msg.documents" 
                    :key="doc.id" 
                    class="doc-selection-item"
                    :class="{ selected: isAiDocSelected(doc.id), disabled: msg.confirmed }"
                    @click="!msg.confirmed && toggleAiDocSelection(doc)"
                  >
                    <div class="doc-checkbox">
                      <el-icon v-if="isAiDocSelected(doc.id)" class="check-icon"><Select /></el-icon>
                      <span v-else class="checkbox-empty"></span>
                    </div>
                    <div class="doc-info">
                      <div class="doc-title">{{ doc.title }}</div>
                      <div class="doc-meta">
                        <span v-if="doc.author">{{ doc.author }}</span>
                        <span v-if="doc.keywords" class="doc-keywords">{{ doc.keywords }}</span>
                      </div>
                    </div>
                    <el-button 
                      size="small" 
                      link 
                      type="primary"
                      @click.stop="previewAiDocument(doc)"
                    >
                      预览
                    </el-button>
                  </div>
                </div>
                <div v-if="!msg.confirmed" class="doc-selection-actions">
                  <el-button size="small" @click="skipAiDocSelection(idx)">
                    跳过，直接回答
                  </el-button>
                  <el-button 
                    type="primary" 
                    size="small" 
                    :disabled="aiPendingDocSelections.length === 0"
                    @click="confirmAiDocSelection(idx)"
                  >
                    确认选择 ({{ aiPendingDocSelections.length }})
                  </el-button>
                </div>
                <div v-else class="doc-selection-confirmed">
                  <el-icon><Select /></el-icon>
                  <span v-if="msg.selectedCount > 0">已选择 {{ msg.selectedCount }} 个文档作为参考</span>
                  <span v-else>已跳过文档选择</span>
                </div>
              </div>
            </div>
            
            <!-- AI正在处理 -->
            <div v-if="aiSearchLoading" class="chat-msg assistant">
              <div class="msg-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="msg-bubble typing-bubble">
                <div v-if="aiLoadingPhase === 'searching'" class="loading-status">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="loading-text">正在搜索相关文档...</span>
                </div>
                <div v-else-if="aiLoadingPhase === 'analyzing'" class="loading-status">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="loading-text">正在分析文档内容...</span>
                </div>
                <div v-else-if="aiStreamingContent" class="streaming-content" v-html="renderAiMarkdown(aiStreamingContent)"></div>
                <div v-else class="loading-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
            
            <!-- AI 建议的搜索词 -->
            <div v-if="aiSuggestions.keywords && aiSuggestions.keywords.length > 0 && !aiWaitingForSelection" class="chat-suggest">
              <span>💡 去搜索：</span>
              <button 
                v-for="kw in aiSuggestions.keywords" 
                :key="kw" 
                class="tag-btn"
                @click="useAiSuggestion(kw)"
              >
                {{ kw }}
              </button>
            </div>
          </div>

          <!-- 底部输入框 -->
          <div class="chat-input-bottom">
            <!-- 文档上下文提示 -->
            <div v-if="aiSelectedDocuments.length > 0" class="context-hint">
              <el-icon><Document /></el-icon>
              <span>基于 {{ aiSelectedDocuments.length }} 个文档回答</span>
            </div>
            <div class="search-box ai-input-box">
              <div class="search-icon">
                <el-icon><EditPen /></el-icon>
              </div>
              <input
                type="text"
                v-model="aiSearchInput"
                placeholder="继续输入您的问题..."
                class="search-input"
                @keyup.enter="sendAiSearchQuery"
                :disabled="aiSearchLoading"
              />
              <!-- 停止生成按钮 -->
              <button 
                v-if="aiIsGenerating"
                class="stop-btn" 
                @click="stopAiGeneration"
              >
                <el-icon><VideoPause /></el-icon>
                <span>停止</span>
              </button>
              <!-- 发送按钮 -->
              <button 
                v-else
                class="search-btn" 
                @click="sendAiSearchQuery" 
                :disabled="aiSearchLoading || !aiSearchInput.trim()"
              >
                <span>发送</span>
                <el-icon><Promotion /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 文档预览对话框 -->
      <el-dialog 
        v-model="aiDocPreviewVisible" 
        :title="aiPreviewDoc?.title || '文档预览'"
        width="600px"
        class="ai-doc-preview-dialog"
      >
        <div class="doc-preview-content">
          <div v-if="aiPreviewLoading" class="preview-loading">
            <el-icon class="loading-spin"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <div v-else-if="aiPreviewDoc" class="preview-body">
            <div class="preview-meta">
              <span v-if="aiPreviewDoc.author"><el-icon><User /></el-icon> {{ aiPreviewDoc.author }}</span>
              <span v-if="aiPreviewDoc.keywords"><el-icon><PriceTag /></el-icon> {{ aiPreviewDoc.keywords }}</span>
            </div>
            <div class="preview-text">
              {{ aiPreviewDoc.content?.substring(0, 1500) || aiPreviewDoc.summary || '暂无内容预览' }}
              <span v-if="aiPreviewDoc.content?.length > 1500" class="more-text">...</span>
            </div>
          </div>
        </div>
        <template #footer>
          <el-button @click="aiDocPreviewVisible = false">关闭</el-button>
          <el-button type="primary" @click="addPreviewDocToAiSelection" :disabled="isAiDocSelected(aiPreviewDoc?.id)">
            <el-icon><Select /></el-icon>
            {{ isAiDocSelected(aiPreviewDoc?.id) ? '已选择' : '选择此文档' }}
          </el-button>
        </template>
      </el-dialog>

      <!-- 底部Tab切换 - 走马灯样式 -->
      <div class="bottom-tabs">
        <div class="tab-slider" :class="{ 'slide-right': initialViewMode === 'ai' }"></div>
        <div class="tab-item" :class="{ active: initialViewMode === 'search' }" @click="initialViewMode = 'search'">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </div>
        <div class="tab-item" :class="{ active: initialViewMode === 'ai' }" @click="initialViewMode = 'ai'">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI助手</span>
        </div>
      </div>
    </div>

    <!-- 搜索结果界面 -->
    <div class="results-view" v-else>
      <!-- 顶部搜索栏 -->
      <header class="results-header">
        <div class="header-content">
          <div class="brand" @click="resetSearch">
            <div class="brand-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <span class="brand-text">知识库</span>
          </div>
          
          <div class="header-search">
            <div class="mini-search-box">
              <el-icon class="search-prefix"><Search /></el-icon>
              <input
                type="text"
                v-model="searchKeyword"
                placeholder="搜索知识..."
                @keyup.enter="handleSearch"
              />
              <button class="mini-search-btn" @click="handleSearch">
                <el-icon><Search /></el-icon>
              </button>
            </div>
          </div>

          <div class="header-actions">
            <el-button 
              v-if="hasPermission(userInfo, 'UPLOAD')" 
              type="primary" 
              class="upload-btn"
              @click="$router.push('/knowledge-management')"
            >
              <el-icon><Upload /></el-icon>
              <span>上传文档</span>
            </el-button>
          </div>
        </div>
      </header>

      <!-- 主内容区 -->
      <main class="results-main">
        <!-- 筛选侧边栏 -->
        <aside class="filter-panel">
          <div class="filter-card">
            <div class="filter-section">
              <h3 class="filter-title">
                <el-icon><Sort /></el-icon>
                排序方式
              </h3>
              <div class="filter-options">
                <label 
                  v-for="opt in sortOptions" 
                  :key="opt.value"
                  class="filter-option"
                  :class="{ active: sortField === opt.value }"
                >
                  <input 
                    type="radio" 
                    :value="opt.value" 
                    v-model="sortField"
                    @change="handleFilterChange"
                  />
                  <span class="option-indicator"></span>
                  <span class="option-text">{{ opt.label }}</span>
                </label>
              </div>
            </div>

            <div class="filter-section">
              <h3 class="filter-title">
                <el-icon><Calendar /></el-icon>
                发布时间
              </h3>
              <div class="filter-options">
                <label 
                  v-for="opt in timeOptions" 
                  :key="opt.value"
                  class="filter-option"
                  :class="{ active: timeFilter === opt.value }"
                >
                  <input 
                    type="radio" 
                    :value="opt.value" 
                    v-model="timeFilter"
                    @change="handleFilterChange"
                  />
                  <span class="option-indicator"></span>
                  <span class="option-text">{{ opt.label }}</span>
                </label>
              </div>
            </div>

            <div class="filter-section">
              <h3 class="filter-title">
                <el-icon><Document /></el-icon>
                文件类型
              </h3>
              <div class="filter-options">
                <label 
                  v-for="opt in typeOptions" 
                  :key="opt.value"
                  class="filter-option"
                  :class="{ active: typeFilter === opt.value }"
                >
                  <input 
                    type="radio" 
                    :value="opt.value" 
                    v-model="typeFilter"
                    @change="handleFilterChange"
                  />
                  <span class="option-indicator"></span>
                  <span class="option-text">{{ opt.label }}</span>
                </label>
              </div>
            </div>

            <button class="reset-filters-btn" @click="resetFilters">
              <el-icon><RefreshRight /></el-icon>
              重置筛选
            </button>
          </div>
        </aside>

        <!-- 结果列表区 -->
        <section class="results-content" v-loading="loading" element-loading-text="正在搜索...">
          <!-- 结果统计和操作栏 -->
          <div class="results-toolbar">
            <div class="toolbar-left">
              <div class="result-stats">
                <span class="stats-keyword">"{{ currentSearchKeyword }}"</span>
                <span class="stats-count">找到 <strong>{{ total }}</strong> 条结果</span>
              </div>
            </div>
            <div class="toolbar-right" v-if="knowledgeList.length > 0">
              <label class="select-all-checkbox">
                <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
                <span class="checkbox-indicator"></span>
                <span>全选</span>
              </label>
              <el-button 
                type="primary" 
                class="batch-download-btn"
                :loading="batchDownloading"
                :disabled="selectedCount === 0"
                @click="batchDownloadResults"
              >
                <el-icon><Download /></el-icon>
                <span>下载选中 ({{ selectedCount }})</span>
              </el-button>
            </div>
          </div>

          <!-- 空结果 -->
          <div v-if="hasSearched && knowledgeList.length === 0 && !loading" class="empty-results">
            <div class="empty-illustration">
              <svg viewBox="0 0 200 200" class="empty-svg">
                <circle cx="100" cy="100" r="80" fill="#f3f4f6" />
                <path d="M70 90 L85 90 L85 130 L70 130 Z" fill="#d1d5db" />
                <path d="M95 80 L110 80 L110 130 L95 130 Z" fill="#9ca3af" />
                <path d="M120 100 L135 100 L135 130 L120 130 Z" fill="#d1d5db" />
                <circle cx="140" cy="60" r="25" fill="none" stroke="#6366f1" stroke-width="4" />
                <path d="M158 78 L175 95" stroke="#6366f1" stroke-width="4" stroke-linecap="round" />
              </svg>
            </div>
            <h3 class="empty-title">未找到相关结果</h3>
            <p class="empty-desc">尝试使用其他关键词或调整筛选条件</p>
            <button class="back-btn" @click="resetSearch">
              <el-icon><Back /></el-icon>
              返回搜索
            </button>
          </div>

          <!-- 结果列表 -->
          <div v-else class="results-list">
            <TransitionGroup name="result-item">
              <div 
                v-for="(item, index) in knowledgeList" 
                :key="item.id" 
                class="result-card"
                :style="{ '--delay': index * 0.05 + 's' }"
                @click="viewDetail(item.id)"
              >
                <div class="card-checkbox" @click.stop>
                  <label class="custom-checkbox">
                    <input type="checkbox" v-model="item.selected" />
                    <span class="checkbox-mark"></span>
                  </label>
                </div>

                <div class="card-icon">
                  <div class="icon-wrapper" :class="getFileTypeClass(item)">
                    <el-icon v-if="getFileType(item) === 'pdf'"><Document /></el-icon>
                    <el-icon v-else-if="getFileType(item) === 'doc'"><Document /></el-icon>
                    <el-icon v-else-if="getFileType(item) === 'image'"><Picture /></el-icon>
                    <el-icon v-else-if="getFileType(item) === 'video'"><VideoCamera /></el-icon>
                    <el-icon v-else><Document /></el-icon>
                  </div>
                </div>

                <div class="card-content">
                  <div class="card-header">
                    <h3 class="card-title">
                      <span v-if="item.highlight && item.highlight.title" v-html="item.highlight.title[0]"></span>
                      <span v-else>{{ item.title }}</span>
                    </h3>
                    <div class="card-badges">
                      <span v-if="item.clickCount > 100" class="badge badge-hot">
                        <el-icon><TrendCharts /></el-icon>
                        热门
                      </span>
                      <span v-if="isRecent(item.createTime)" class="badge badge-new">新</span>
                    </div>
                  </div>

                  <div class="card-snippet">
                    <template v-if="item.highlight && item.highlight.content">
                      <span v-for="(fragment, idx) in item.highlight.content.slice(0, 2)" :key="idx" v-html="fragment + '... '"></span>
                    </template>
                    <template v-else>
                      {{ item.summary || (item.content ? item.content.substring(0, 150) + '...' : '暂无内容摘要') }}
                    </template>
                  </div>

                  <div class="card-meta">
                    <span class="meta-item">
                      <el-icon><User /></el-icon>
                      {{ item.author || '未知作者' }}
                    </span>
                    <span class="meta-item">
                      <el-icon><Clock /></el-icon>
                      {{ formatTime(item.createTime) }}
                    </span>
                    <span class="meta-item">
                      <el-icon><View /></el-icon>
                      {{ formatNumber(item.clickCount || 0) }} 次浏览
                    </span>
                    <span class="meta-item" v-if="item.category">
                      <el-icon><Folder /></el-icon>
                      {{ item.category }}
                    </span>
                  </div>
                </div>

                <div class="card-arrow">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
            </TransitionGroup>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="total > pageSize">
            <div class="custom-pagination">
              <button 
                class="page-btn prev" 
                :disabled="pageNum === 1"
                @click="changePage(pageNum - 1)"
              >
                <el-icon><ArrowLeft /></el-icon>
                上一页
              </button>
              
              <div class="page-numbers">
                <button
                  v-for="page in visiblePages"
                  :key="page"
                  class="page-num"
                  :class="{ active: page === pageNum, ellipsis: page === '...' }"
                  :disabled="page === '...'"
                  @click="page !== '...' && changePage(page)"
                >
                  {{ page }}
                </button>
              </div>

              <button 
                class="page-btn next" 
                :disabled="pageNum >= totalPages"
                @click="changePage(pageNum + 1)"
              >
                下一页
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <div class="page-info">
              第 {{ pageNum }} 页，共 {{ totalPages }} 页
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { hasPermission } from '../utils/permission'
import { 
  Search, Document, User, Clock, Folder, Download, View, TrendCharts,
  ArrowRight, ArrowLeft, Upload, Sort, Calendar, RefreshRight, Back,
  Picture, VideoCamera, DataAnalysis, ChatDotRound, Close, Promotion, EditPen, Plus,
  Select, Loading, PriceTag, VideoPause
} from '@element-plus/icons-vue'
import api from '../api'
import { askGeneral, enhanceSearchQuery } from '../api/ai'
import { 
  searchKnowledge, 
  getKnowledgeDetail, 
  analyzeIntent,
  callDeepSeekStream,
  askAboutMultipleDocumentsStream
} from '../api/aiAssistant'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// State
const isInitialState = ref(true)
const initialViewMode = ref('search') // 'search' 或 'ai'
const hasSearched = ref(false)
const searchKeyword = ref('')
const currentSearchKeyword = ref('')
const loading = ref(false)
const knowledgeList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// Filters
const timeFilter = ref('ALL')
const typeFilter = ref('ALL')
const sortField = ref('')

// Options
const sortOptions = [
  { label: '综合排序', value: '' },
  { label: '热门优先', value: 'clickCount' },
  { label: '最新优先', value: 'createTime' }
]
const timeOptions = [
  { label: '不限时间', value: 'ALL' },
  { label: '最近一周', value: 'WEEK' },
  { label: '最近一月', value: 'MONTH' },
  { label: '最近一年', value: 'YEAR' }
]
const typeOptions = [
  { label: '全部类型', value: 'ALL' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' }
]

// History & Hot Search
const historyTags = ref([])
const hotSearchList = ref([])

// Selection
const selectAll = ref(false)
const selectedCount = computed(() => knowledgeList.value.filter(item => item.selected).length)

// Download
const batchDownloading = ref(false)

// AI Assistant
const aiSearchInput = ref('')
const aiSearchLoading = ref(false)
const aiChatMessages = ref([])
const aiSuggestions = ref({ keywords: [], suggestions: [], intent: '' })
const aiChatRef = ref(null)

// AI 文档选择和问答增强
const aiFoundDocuments = ref([])           // AI搜索到的文档列表
const aiSelectedDocuments = ref([])         // 用户选择的参考文档
const aiPendingDocSelections = ref([])      // 待确认的文档选择
const aiWaitingForSelection = ref(false)    // 是否等待用户选择文档
const aiStreamingContent = ref('')          // 流式输出内容
const aiLoadingPhase = ref('')              // 加载阶段: 'searching' | 'analyzing' | ''
const showAiSelectedDocs = ref(false)       // 是否显示已选文档面板
const aiDocPreviewVisible = ref(false)      // 文档预览对话框
const aiPreviewDoc = ref(null)              // 预览中的文档
const aiPreviewLoading = ref(false)         // 预览加载中
const aiAbortController = ref(null)         // 用于停止生成的控制器
const aiIsGenerating = ref(false)           // 是否正在生成回答

// Pagination
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const visiblePages = computed(() => {
  const pages = []
  const current = pageNum.value
  const total = totalPages.value
  
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 3) {
      pages.push(1, 2, 3, 4, '...', total)
    } else if (current >= total - 2) {
      pages.push(1, '...', total - 3, total - 2, total - 1, total)
    } else {
      pages.push(1, '...', current - 1, current, current + 1, '...', total)
    }
  }
  return pages
})

onMounted(async () => {
  // 加载搜索历史
  const history = localStorage.getItem('searchHistory')
  if (history) {
    historyTags.value = JSON.parse(history).slice(0, 6)
  }
  // 加载热门搜索
  await loadHotSearch()
})

// Watch selectAll
watch(selectAll, (val) => {
  if (knowledgeList.value.length > 0) {
    knowledgeList.value.forEach(item => {
      item.selected = val
    })
  }
})

// 加载热门搜索（使用热门知识的标题作为推荐搜索词）
const loadHotSearch = async () => {
  try {
    // 使用 /knowledge/hot 端点获取热门知识
    const res = await api.get('/knowledge/hot', { params: { limit: 8 } })
    if (res.code === 200 && res.data && res.data.length > 0) {
      hotSearchList.value = res.data
        .filter(k => k.title)
        .map(k => k.title)
        .slice(0, 8)
    }
  } catch (error) {
    // 备用方案：从知识列表获取热门
    try {
      const res = await api.get('/knowledge/list', { 
        params: { pageSize: 8, status: 'APPROVED' } 
      })
      if (res.code === 200 && res.data) {
        const sorted = (res.data || [])
          .filter(k => k.fileId && k.clickCount > 0)
          .sort((a, b) => (b.clickCount || 0) - (a.clickCount || 0))
          .slice(0, 8)
        hotSearchList.value = sorted.map(k => k.title)
      }
    } catch (e) {
      console.log('加载热门搜索失败')
    }
  }
}

const resetSearch = () => {
  isInitialState.value = true
  hasSearched.value = false
  searchKeyword.value = ''
  currentSearchKeyword.value = ''
  knowledgeList.value = []
  pageNum.value = 1
  total.value = 0
}

const quickSearch = (val) => {
  searchKeyword.value = val
  handleSearch()
}

const handleFilterChange = () => {
  pageNum.value = 1
  handleSearch()
}

const resetFilters = () => {
  timeFilter.value = 'ALL'
  typeFilter.value = 'ALL'
  sortField.value = ''
  pageNum.value = 1
  handleSearch()
}

const toggleSelectAll = () => {
  knowledgeList.value.forEach(item => {
    item.selected = selectAll.value
  })
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  pageNum.value = page
  handleSearch(false)
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleSearch = async (resetPage = true) => {
  if (!searchKeyword.value.trim()) return
  
  if (resetPage) {
    pageNum.value = 1
  }
  
  hasSearched.value = true
  isInitialState.value = false
  loading.value = true
  currentSearchKeyword.value = searchKeyword.value
  
  // Save history
  const tags = [searchKeyword.value, ...historyTags.value.filter(t => t !== searchKeyword.value)]
  historyTags.value = tags.slice(0, 6)
  localStorage.setItem('searchHistory', JSON.stringify(historyTags.value))

  try {
    const params = {
      keyword: searchKeyword.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: 'APPROVED',
      timeRange: timeFilter.value,
      fileType: typeFilter.value === 'ALL' ? null : typeFilter.value,
      sortField: sortField.value || null
    }
    
    const res = await api.post('/knowledge/search', params)
    if (res.code === 200) {
      knowledgeList.value = (res.data.results || [])
        .filter(item => item.fileId != null)
        .map(item => ({ ...item, selected: false }))
      total.value = res.data.total || knowledgeList.value.length
      selectAll.value = false
    }
  } catch (error) {
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 批量下载搜索结果
const batchDownloadResults = async () => {
  const selectedItems = knowledgeList.value.filter(item => item.selected && item.fileId)
  if (selectedItems.length === 0) {
    ElMessage.warning('请选择要下载的文件')
    return
  }
  
  batchDownloading.value = true
  ElMessage.info(`正在准备下载 ${selectedItems.length} 个文件...`)
  
  try {
    const fileIds = selectedItems.map(item => item.fileId)
    const res = await api.post('/file/batch-download', { fileIds }, { 
      responseType: 'blob',
      timeout: 300000
    })
    
    const blob = new Blob([res], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `知识库文档_${new Date().toLocaleDateString()}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.warning('正在逐个下载文件...')
    for (const item of selectedItems) {
      try {
        const link = document.createElement('a')
        link.href = `/api/file/download/${item.fileId}`
        link.download = item.title
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        await new Promise(resolve => setTimeout(resolve, 500))
      } catch (e) {
        console.error(`下载 ${item.title} 失败`)
      }
    }
    ElMessage.success('下载完成')
  } finally {
    batchDownloading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

// 停止AI生成
const stopAiGeneration = () => {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
  
  // 如果有流式内容，保存为消息
  if (aiStreamingContent.value) {
    aiChatMessages.value.push({
      role: 'assistant',
      type: 'text',
      content: aiStreamingContent.value + '\n\n*[已停止生成]*',
      documents: aiSelectedDocuments.value.length > 0 
        ? aiSelectedDocuments.value.map(d => ({ id: d.id, title: d.title })) 
        : undefined
    })
  }
  
  aiSearchLoading.value = false
  aiIsGenerating.value = false
  aiLoadingPhase.value = ''
  aiStreamingContent.value = ''
  scrollAiChat()
}

// AI 搜索助手方法
const sendAiSearchQuery = async () => {
  if (!aiSearchInput.value.trim() || aiSearchLoading.value) return
  
  const userQuery = aiSearchInput.value.trim()
  aiSearchInput.value = ''
  
  // 如果之前在等待选择文档，先取消等待状态
  if (aiWaitingForSelection.value) {
    aiWaitingForSelection.value = false
  }
  
  aiChatMessages.value.push({
    role: 'user',
    content: userQuery
  })
  
  aiSearchLoading.value = true
  aiLoadingPhase.value = 'searching'
  aiStreamingContent.value = ''
  aiAbortController.value = new AbortController()
  scrollAiChat()
  
  try {
    // 1. 分析用户意图
    const intent = await analyzeIntent(userQuery)
    
    // 2. 如果是帮助类问题
    if (intent.intent === 'HELP') {
      aiChatMessages.value.push({
        role: 'assistant',
        type: 'text',
        content: getAiHelpMessage()
      })
      return
    }

    // 3. 如果是闲聊
    if (intent.intent === 'CHAT') {
      await generateAiChatResponse(userQuery)
      return
    }

    // 4. 如果已有选中的文档，直接基于文档回答
    if (aiSelectedDocuments.value.length > 0) {
      await generateAiAnswerWithDocs(userQuery)
      return
    }

    // 5. 需要搜索文档
    if (intent.needSearch && intent.keywords?.length > 0) {
      let allDocs = []
      for (const keyword of intent.keywords.slice(0, 2)) {
        const results = await searchKnowledge(keyword, 5)
        allDocs = [...allDocs, ...results]
      }

      // 去重
      const seen = new Set()
      const uniqueDocs = allDocs.filter(doc => {
        if (seen.has(doc.id)) return false
        seen.add(doc.id)
        return true
      }).slice(0, 6)

      if (uniqueDocs.length > 0) {
        aiFoundDocuments.value = uniqueDocs
        aiPendingDocSelections.value = []
        
        // 添加文档选择消息
        aiChatMessages.value.push({
          role: 'assistant',
          type: 'doc-selection',
          content: `找到 ${uniqueDocs.length} 个相关文档，请选择要参考的文档：`,
          documents: uniqueDocs,
          confirmed: false,
          selectedCount: 0
        })
        
        aiWaitingForSelection.value = true
        aiSuggestions.value = { keywords: intent.keywords, suggestions: [], intent: intent.intent }
      } else {
        // 没有找到文档
        aiChatMessages.value.push({
          role: 'assistant',
          type: 'text',
          content: '抱歉，没有找到相关文档。我将根据我的知识来回答您的问题。'
        })
        await generateAiAnswerWithoutDocs(userQuery)
      }
    } else {
      // 不需要搜索，直接回答
      if (aiSelectedDocuments.value.length > 0) {
        await generateAiAnswerWithDocs(userQuery)
      } else {
        await generateAiAnswerWithoutDocs(userQuery)
      }
    }
  } catch (error) {
    console.error('AI搜索失败:', error)
    aiChatMessages.value.push({
      role: 'assistant',
      type: 'text',
      content: '抱歉，AI服务暂时不可用。您可以直接输入关键词进行搜索。'
    })
  } finally {
    aiSearchLoading.value = false
    aiLoadingPhase.value = ''
    aiStreamingContent.value = ''
    scrollAiChat()
  }
}

// 切换文档选择
const toggleAiDocSelection = (doc) => {
  const index = aiPendingDocSelections.value.findIndex(d => d.id === doc.id)
  if (index >= 0) {
    aiPendingDocSelections.value.splice(index, 1)
  } else {
    aiPendingDocSelections.value.push(doc)
  }
}

// 判断文档是否被选中
const isAiDocSelected = (docId) => {
  return aiPendingDocSelections.value.some(d => d.id === docId) ||
         aiSelectedDocuments.value.some(d => d.id === docId)
}

// 确认文档选择
const confirmAiDocSelection = async (msgIndex) => {
  if (aiPendingDocSelections.value.length === 0) {
    ElMessage.warning('请至少选择一个文档')
    return
  }
  
  // 更新消息状态
  aiChatMessages.value[msgIndex].confirmed = true
  aiChatMessages.value[msgIndex].selectedCount = aiPendingDocSelections.value.length
  
  // 显示加载状态
  aiSearchLoading.value = true
  aiLoadingPhase.value = 'analyzing'
  
  // 加载文档内容
  for (const doc of aiPendingDocSelections.value) {
    if (!aiSelectedDocuments.value.some(d => d.id === doc.id)) {
      try {
        const detail = await getKnowledgeDetail(doc.id)
        if (detail) {
          aiSelectedDocuments.value.push({
            ...doc,
            content: detail.contentText || detail.content,
            summary: detail.summary
          })
        } else {
          aiSelectedDocuments.value.push(doc)
        }
      } catch (e) {
        aiSelectedDocuments.value.push(doc)
      }
    }
  }
  
  aiPendingDocSelections.value = []
  aiWaitingForSelection.value = false
  aiSearchLoading.value = false
  aiLoadingPhase.value = ''
  
  // 添加提示消息，等待用户输入
  aiChatMessages.value.push({
    role: 'assistant',
    type: 'text',
    content: `已加载 ${aiSelectedDocuments.value.length} 个文档作为参考。请输入您的问题，我将基于这些文档为您解答。`
  })
  
  scrollAiChat()
}

// 跳过文档选择
const skipAiDocSelection = async (msgIndex) => {
  aiChatMessages.value[msgIndex].confirmed = true
  aiChatMessages.value[msgIndex].selectedCount = 0
  
  aiPendingDocSelections.value = []
  aiWaitingForSelection.value = false
  
  // 添加提示消息，等待用户输入
  aiChatMessages.value.push({
    role: 'assistant',
    type: 'text',
    content: '好的，已跳过文档选择。请继续输入您的问题。'
  })
  
  scrollAiChat()
}

// 预览文档
const previewAiDocument = async (doc) => {
  aiDocPreviewVisible.value = true
  aiPreviewLoading.value = true
  aiPreviewDoc.value = doc
  
  try {
    const detail = await getKnowledgeDetail(doc.id)
    if (detail) {
      aiPreviewDoc.value = {
        ...doc,
        content: detail.contentText || detail.content,
        summary: detail.summary,
        author: detail.author,
        keywords: detail.keywords
      }
    }
  } catch (error) {
    console.error('获取文档详情失败:', error)
  } finally {
    aiPreviewLoading.value = false
  }
}

// 从预览中添加文档
const addPreviewDocToAiSelection = () => {
  if (aiPreviewDoc.value && !isAiDocSelected(aiPreviewDoc.value.id)) {
    aiPendingDocSelections.value.push(aiPreviewDoc.value)
  }
  aiDocPreviewVisible.value = false
}

// 基于选中文档生成回答
const generateAiAnswerWithDocs = async (question) => {
  aiSearchLoading.value = true
  aiIsGenerating.value = true
  aiLoadingPhase.value = 'analyzing'
  aiStreamingContent.value = ''
  
  // 创建新的 AbortController
  aiAbortController.value = new AbortController()

  try {
    // 构建文档上下文
    let contextPrompt = '\n\n以下是用户选择的参考文档：\n\n'
    aiSelectedDocuments.value.forEach((doc, index) => {
      const contentPreview = doc.content?.substring(0, 3000) || doc.summary || '无内容'
      contextPrompt += `【文档${index + 1}】${doc.title}\n`
      contextPrompt += `内容：${contentPreview}\n`
      if (doc.keywords) {
        contextPrompt += `关键词：${doc.keywords}\n`
      }
      contextPrompt += '\n---\n\n'
    })

    const systemPrompt = `你是企业知识库的AI助手。请基于用户选择的参考文档回答问题。

规则：
1. 优先使用提供的文档内容来回答问题
2. 引用信息时请说明来源（如"根据《XX》文档..."）
3. 如果文档中没有相关信息，请诚实告知
4. 回答要简洁、准确、有条理
5. 使用markdown格式使回答更清晰
${contextPrompt}`

    aiLoadingPhase.value = ''
    
    const messageHistory = aiChatMessages.value
      .filter(m => m.type !== 'doc-selection')
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }))

    await callDeepSeekStream([
      { role: 'system', content: systemPrompt },
      ...messageHistory,
      { role: 'user', content: question }
    ], (chunk) => {
      aiStreamingContent.value += chunk
      scrollAiChat()
    }, { signal: aiAbortController.value.signal })

    // 只有没被中断才添加消息
    if (!aiAbortController.value?.signal.aborted && aiStreamingContent.value) {
      aiChatMessages.value.push({
        role: 'assistant',
        type: 'text',
        content: aiStreamingContent.value,
        documents: aiSelectedDocuments.value.map(d => ({ id: d.id, title: d.title }))
      })
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('生成回答失败:', error)
    }
  } finally {
    aiSearchLoading.value = false
    aiIsGenerating.value = false
    aiLoadingPhase.value = ''
    aiStreamingContent.value = ''
    aiAbortController.value = null
    scrollAiChat()
  }
}

// 不使用文档生成回答
const generateAiAnswerWithoutDocs = async (question) => {
  aiSearchLoading.value = true
  aiIsGenerating.value = true
  aiLoadingPhase.value = ''
  aiStreamingContent.value = ''
  
  // 创建新的 AbortController
  aiAbortController.value = new AbortController()

  try {
    const systemPrompt = `你是企业知识库的AI助手。请回答用户的问题。
如果问题涉及具体的公司文档或内部信息，请建议用户搜索相关文档。
回答要简洁、准确、有帮助。`

    const messageHistory = aiChatMessages.value
      .filter(m => m.type !== 'doc-selection')
      .slice(-6)
      .map(m => ({ role: m.role, content: m.content }))

    await callDeepSeekStream([
      { role: 'system', content: systemPrompt },
      ...messageHistory,
      { role: 'user', content: question }
    ], (chunk) => {
      aiStreamingContent.value += chunk
      scrollAiChat()
    }, { signal: aiAbortController.value.signal })

    // 只有没被中断才添加消息
    if (!aiAbortController.value?.signal.aborted && aiStreamingContent.value) {
      aiChatMessages.value.push({
        role: 'assistant',
        type: 'text',
        content: aiStreamingContent.value
      })
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('生成回答失败:', error)
    }
  } finally {
    aiSearchLoading.value = false
    aiIsGenerating.value = false
    aiStreamingContent.value = ''
    aiAbortController.value = null
    scrollAiChat()
  }
}

// 生成闲聊回复
const generateAiChatResponse = async (question) => {
  aiStreamingContent.value = ''
  aiLoadingPhase.value = ''
  aiIsGenerating.value = true
  
  // 创建新的 AbortController
  aiAbortController.value = new AbortController()

  try {
    await callDeepSeekStream([
      { role: 'system', content: '你是企业知识库的AI助手，友好地与用户交流。' },
      { role: 'user', content: question }
    ], (chunk) => {
      aiStreamingContent.value += chunk
      scrollAiChat()
    }, { signal: aiAbortController.value.signal })

    // 只有没被中断才添加消息
    if (!aiAbortController.value?.signal.aborted && aiStreamingContent.value) {
      aiChatMessages.value.push({
        role: 'assistant',
        type: 'text',
        content: aiStreamingContent.value
      })
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      console.error('生成回答失败:', error)
    }
  } finally {
    aiSearchLoading.value = false
    aiIsGenerating.value = false
    aiStreamingContent.value = ''
    aiAbortController.value = null
    scrollAiChat()
  }
}

// 获取帮助消息
const getAiHelpMessage = () => {
  return `我是企业知识库的AI助手，可以帮您：

🔍 **智能搜索文档** - 告诉我您想找什么，我会帮您搜索

📋 **选择参考文档** - 搜索后，您可以选择一个或多个文档作为参考来源

💬 **精准问答** - 基于您选择的文档，我会给出准确的回答

---

试试这样问我：
• "帮我找一下关于XX的文档"
• "公司的XX流程是什么"
• "XX项目的相关资料"`
}

// 渲染Markdown
const renderAiMarkdown = (content) => {
  if (!content) return ''
  try {
    const html = marked.parse(content, { breaks: true })
    return DOMPurify.sanitize(html)
  } catch (e) {
    return content
  }
}

// 移除已选文档
const removeAiSelectedDoc = (docId) => {
  aiSelectedDocuments.value = aiSelectedDocuments.value.filter(d => d.id !== docId)
  if (aiSelectedDocuments.value.length === 0) {
    showAiSelectedDocs.value = false
  }
}

// 清空已选文档
const clearAiSelectedDocs = () => {
  aiSelectedDocuments.value = []
  showAiSelectedDocs.value = false
}

const useAiSuggestion = (keyword) => {
  searchKeyword.value = keyword
  initialViewMode.value = 'search'
  handleSearch()
}

const scrollAiChat = () => {
  nextTick(() => {
    if (aiChatRef.value) {
      aiChatRef.value.scrollTop = aiChatRef.value.scrollHeight
    }
  })
}

// 清除AI对话，开始新对话
const clearAiChat = () => {
  aiChatMessages.value = []
  aiSuggestions.value = { keywords: [], suggestions: [], intent: '' }
  aiSearchInput.value = ''
  aiFoundDocuments.value = []
  aiSelectedDocuments.value = []
  aiPendingDocSelections.value = []
  aiWaitingForSelection.value = false
  aiStreamingContent.value = ''
  aiLoadingPhase.value = ''
  showAiSelectedDocs.value = false
}

const formatTime = (time) => {
  if (!time) return '未知时间'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return date.toLocaleDateString('zh-CN')
}

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

const isRecent = (time) => {
  if (!time) return false
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  return diff < 7 * 24 * 60 * 60 * 1000 // 7天内
}

const getFileType = (item) => {
  const title = item.title || ''
  const ext = title.split('.').pop()?.toLowerCase()
  if (['pdf'].includes(ext)) return 'pdf'
  if (['doc', 'docx', 'txt', 'md'].includes(ext)) return 'doc'
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) return 'image'
  if (['mp4', 'avi', 'mov', 'wmv', 'flv'].includes(ext)) return 'video'
  return 'doc'
}

const getFileTypeClass = (item) => {
  const type = getFileType(item)
  return `icon-${type}`
}
</script>

<style scoped>
/* ===== 基础变量和样式 ===== */
.knowledge-search-page {
  min-height: 100vh;
  background: #f5f7fa;
  position: relative;
  overflow-x: hidden;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

/* 隐藏全局滚动条 */
.knowledge-search-page::-webkit-scrollbar {
  display: none;
}
.knowledge-search-page {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

/* 背景装饰 - 简洁浅色风格 */
.bg-decoration {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.15;
  animation: float-orb 20s ease-in-out infinite;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #409eff 0%, transparent 70%);
  top: -150px;
  right: -100px;
  animation-delay: 0s;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #67c23a 0%, transparent 70%);
  bottom: -100px;
  left: -100px;
  animation-delay: -7s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #409eff 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -14s;
}

@keyframes float-orb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 30px) scale(1.02); }
}

/* 网格背景 - 移除 */
.knowledge-search-page::before {
  display: none;
}

/* ===== 初始搜索界面 ===== */
.initial-view {
  min-height: calc(100vh - 60px);
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  padding-bottom: 80px; /* 为底部Tab留出空间 */
  box-sizing: border-box;
  overflow: hidden;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.initial-view::-webkit-scrollbar {
  display: none;
}

.hero-section {
  text-align: center;
  padding: 2rem;
  max-width: 800px;
  width: 100%;
}

.logo-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 1.5rem;
  animation: pulse-glow 4s ease-in-out infinite;
  position: relative;
}

.logo-icon::before {
  content: '';
  position: absolute;
  inset: -20px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse-ring 3s ease-out infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.5); opacity: 0; }
}

.logo-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 0 15px rgba(64, 158, 255, 0.3));
}

@keyframes pulse-glow {
  0%, 100% { 
    opacity: 1; 
    filter: drop-shadow(0 0 15px rgba(64, 158, 255, 0.3));
  }
  50% { 
    opacity: 0.9; 
    filter: drop-shadow(0 0 25px rgba(64, 158, 255, 0.5));
  }
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #303133;
  margin-bottom: 0.5rem;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 1rem;
  color: #909399;
  margin-bottom: 2.5rem;
  letter-spacing: 0.5px;
}

/* 搜索框 */
.search-container {
  margin-bottom: 2rem;
}

.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 50px;
  padding: 6px 6px 6px 24px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.search-box:focus-within {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
}

.search-icon {
  font-size: 1.2rem;
  color: #909399;
  margin-right: 12px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 1rem;
  color: #303133;
  padding: 12px 0;
}

.search-input::placeholder {
  color: #c0c4cc;
}

.search-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #409eff;
  border: none;
  border-radius: 40px;
  padding: 12px 28px;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.search-btn:hover {
  background: #66b1ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

/* AI 搜索按钮 */
.ai-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  color: #409eff;
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-right: 8px;
}

.ai-search-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
  transform: scale(1.05);
}

/* AI 助手弹窗 */
.ai-assistant-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.ai-assistant-content {
  width: 480px;
  max-height: 600px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-assistant-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}

.ai-header-icon {
  width: 48px;
  height: 48px;
  background: #409eff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.5rem;
}

.ai-header-text {
  flex: 1;
}

.ai-header-text h3 {
  margin: 0;
  color: #303133;
  font-size: 1.1rem;
  font-weight: 600;
}

.ai-header-text p {
  margin: 4px 0 0;
  color: #909399;
  font-size: 0.85rem;
}

.close-btn {
  width: 36px;
  height: 36px;
  background: #f5f7fa;
  border: none;
  border-radius: 8px;
  color: #909399;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #e4e7ed;
  color: #606266;
}

.ai-assistant-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.ai-chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  min-height: 300px;
  max-height: 400px;
}

.ai-welcome-msg {
  color: #606266;
  font-size: 0.95rem;
  line-height: 1.6;
}

.ai-welcome-msg p {
  margin: 0 0 12px;
}

.ai-welcome-msg ul {
  margin: 0;
  padding-left: 20px;
  color: #909399;
}

.ai-welcome-msg li {
  margin-bottom: 8px;
}

.ai-chat-msg {
  margin-bottom: 16px;
}

.ai-chat-msg .msg-content {
  display: inline-block;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 0.95rem;
  line-height: 1.5;
  max-width: 85%;
}

.ai-chat-msg.user {
  text-align: right;
}

.ai-chat-msg.user .msg-content {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-chat-msg.assistant .msg-content {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.ai-suggestions {
  margin-top: 16px;
  padding: 16px;
  background: #ecf5ff;
  border-radius: 8px;
  border: 1px solid #d9ecff;
}

.suggestions-title {
  margin: 0 0 12px;
  color: #606266;
  font-size: 0.85rem;
}

.suggestion-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-tag {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  color: #606266;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-tag:hover {
  background: #409eff;
  border-color: #409eff;
  color: white;
}

.ai-loading {
  display: flex;
  gap: 6px;
  padding: 12px;
}

.ai-loading span {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite;
}

.ai-loading span:nth-child(2) { animation-delay: 0.2s; }
.ai-loading span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-8px); }
}

.ai-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px 24px;
  border-top: 1px solid #ebeef5;
}

.ai-input-area input {
  flex: 1;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px 16px;
  color: #303133;
  font-size: 0.95rem;
  outline: none;
  transition: all 0.2s;
}

.ai-input-area input::placeholder {
  color: #c0c4cc;
}

.ai-input-area input:focus {
  border-color: #409eff;
}

.ai-input-area button {
  width: 48px;
  height: 48px;
  background: #409eff;
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  transition: all 0.2s;
}

.ai-input-area button:hover:not(:disabled) {
  background: #66b1ff;
}

.ai-input-area button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 推荐标签 */
.suggestions-area {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 0.85rem;
  margin-bottom: 10px;
}

.fire-icon {
  color: #e6a23c;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.tag-btn {
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  padding: 8px 16px;
  color: #606266;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.tag-hot {
  border-color: #faecd8;
  background: #fdf6ec;
  color: #e6a23c;
}

.tag-hot:hover {
  background: #fef0e6;
  border-color: #e6a23c;
}

.hot-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: #e6a23c;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.tag-history {
  background: #f5f7fa;
  border-color: #e4e7ed;
  color: #909399;
}

.tag-history:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

/* ===== AI助手模式样式 ===== */
.ai-mode-view {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.ai-mode-view::-webkit-scrollbar {
  display: none;
}

.ai-hero {
  padding-top: 1rem;
}

.ai-logo-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 1.5rem;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  color: #fff;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.3);
  animation: float 4s ease-in-out infinite;
  position: relative;
}

.ai-logo-icon::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.3), rgba(102, 177, 255, 0.3));
  z-index: -1;
  animation: pulse-ring 3s ease-out infinite;
}

.ai-input-box {
  border-color: #dcdfe6;
}

.ai-input-box:focus-within {
  border-color: #409eff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.15);
}

/* AI对话内容 */
.ai-chat-content {
  margin-top: 2rem;
  max-width: 600px;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-chat-bubble {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: fadeInUp 0.3s ease;
}

.ai-chat-bubble.user {
  flex-direction: row-reverse;
}

.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: #fff;
  flex-shrink: 0;
}

.ai-chat-bubble.assistant .bubble-avatar {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
}

.ai-chat-bubble.user .bubble-avatar {
  background: linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%);
}

.bubble-text {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 0.9rem;
  line-height: 1.5;
  max-width: 80%;
}

.ai-chat-bubble.assistant .bubble-text {
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: rgba(255, 255, 255, 0.9);
  border-top-left-radius: 4px;
}

.ai-chat-bubble.user .bubble-text {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble-text.loading {
  display: flex;
  gap: 6px;
  padding: 16px 20px;
}

.bubble-text.loading .dot {
  width: 8px;
  height: 8px;
  background: #a5b4fc;
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite;
}

.bubble-text.loading .dot:nth-child(2) { animation-delay: 0.2s; }
.bubble-text.loading .dot:nth-child(3) { animation-delay: 0.4s; }

/* AI功能卡片 */
.ai-features {
  display: flex;
  gap: 12px;
  margin-top: 2rem;
  flex-wrap: wrap;
  justify-content: center;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  color: #606266;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.feature-card:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.feature-card .el-icon {
  font-size: 1.1rem;
  color: #409eff;
}

/* AI提示 */
.ai-tip {
  margin-top: 1.5rem;
  padding: 12px 20px;
  background: #ecf5ff;
  border-left: 3px solid #409eff;
  border-radius: 8px;
  color: #606266;
  font-size: 0.85rem;
}

/* ===== 聊天模式样式 ===== */
.ai-chat-mode {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 16px 24px;
  box-sizing: border-box;
  position: relative;
}

/* 新对话按钮 - 右上角 */
.new-chat-btn {
  position: absolute;
  top: 16px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  color: #606266;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}

.new-chat-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.chat-messages-area {
  flex: 1;
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 8px 16px 0;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.chat-messages-area::-webkit-scrollbar {
  display: none;
}

.chat-msg {
  display: flex;
  gap: 10px;
  animation: fadeInUp 0.3s ease;
}

.chat-msg.user {
  flex-direction: row-reverse;
}

.msg-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  color: #fff;
  flex-shrink: 0;
}

.chat-msg.assistant .msg-icon {
  background: #409eff;
}

.chat-msg.user .msg-icon {
  background: #67c23a;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 0.9rem;
  line-height: 1.5;
  max-width: 85%;
}

.chat-msg.assistant .msg-bubble {
  background: #fff;
  border: 1px solid #e4e7ed;
  color: #303133;
  border-top-left-radius: 4px;
}

.chat-msg.user .msg-bubble {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 4px;
}

.typing-bubble {
  display: inline-flex;
  gap: 5px;
  padding: 12px 16px !important;
}

.typing-bubble .dot {
  width: 6px;
  height: 6px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite;
}

.typing-bubble .dot:nth-child(2) { animation-delay: 0.2s; }
.typing-bubble .dot:nth-child(3) { animation-delay: 0.4s; }

.chat-suggest {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 14px;
  background: #ecf5ff;
  border-radius: 10px;
  font-size: 0.85rem;
  color: #606266;
}

/* 底部输入框 */
.chat-input-bottom {
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
  padding-top: 16px;
  flex-shrink: 0;
}

/* ===== AI 文档选择增强样式 ===== */

/* 聊天顶部操作栏 */
.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.ai-chat-header .new-chat-btn {
  position: static;
}

.selected-docs-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 20px;
  color: #409eff;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.selected-docs-indicator:hover {
  background: #d9ecff;
}

.selected-docs-indicator .arrow-icon {
  transition: transform 0.2s;
}

.selected-docs-indicator .arrow-icon.expanded {
  transform: rotate(90deg);
}

/* 已选文档面板 */
.selected-docs-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.selected-docs-panel .panel-title {
  padding: 10px 14px;
  font-size: 0.85rem;
  font-weight: 500;
  color: #303133;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.panel-docs-list {
  max-height: 150px;
  overflow-y: auto;
}

.panel-doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 0.85rem;
  color: #606266;
  border-bottom: 1px solid #f5f5f5;
}

.panel-doc-item:last-child {
  border-bottom: none;
}

.panel-doc-item .doc-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-doc-item .remove-btn {
  color: #c0c4cc;
  cursor: pointer;
  transition: color 0.2s;
}

.panel-doc-item .remove-btn:hover {
  color: #f56c6c;
}

.selected-docs-panel .panel-actions {
  padding: 8px 14px;
  text-align: right;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

/* 文档选择气泡 */
.doc-selection-bubble {
  background: #fff !important;
  border: 1px solid #e4e7ed !important;
  padding: 0 !important;
  overflow: hidden;
  max-width: 100% !important;
}

.doc-selection-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #ecf5ff 0%, #e8f4ff 100%);
  color: #409eff;
  font-size: 0.9rem;
  font-weight: 500;
}

.doc-selection-list {
  padding: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.doc-selection-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  margin-bottom: 6px;
}

.doc-selection-item:last-child {
  margin-bottom: 0;
}

.doc-selection-item:hover:not(.disabled) {
  background: #f5f7fa;
}

.doc-selection-item.selected {
  background: #ecf5ff;
  border-color: #409eff;
}

.doc-selection-item.disabled {
  cursor: default;
  opacity: 0.7;
}

.doc-checkbox {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #f0f2f5;
  flex-shrink: 0;
}

.doc-selection-item.selected .doc-checkbox {
  background: #409eff;
}

.doc-checkbox .check-icon {
  color: #fff;
  font-size: 14px;
}

.checkbox-empty {
  width: 12px;
  height: 12px;
  border: 2px solid #c0c4cc;
  border-radius: 2px;
}

.doc-selection-item .doc-info {
  flex: 1;
  min-width: 0;
}

.doc-selection-item .doc-title {
  font-size: 0.9rem;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-selection-item .doc-meta {
  display: flex;
  gap: 10px;
  margin-top: 4px;
  font-size: 0.75rem;
  color: #909399;
}

.doc-selection-item .doc-keywords {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-selection-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 14px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.doc-selection-confirmed {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 14px;
  background: #f0f9eb;
  color: #67c23a;
  font-size: 0.85rem;
}

/* 消息中的参考文档 */
.msg-ref-docs {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.ref-docs-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  color: #909399;
  margin-bottom: 8px;
}

.ref-docs-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ref-doc-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 0.8rem;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.ref-doc-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

/* 加载状态 */
.loading-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-status .loading-text {
  color: #909399;
  font-size: 0.85rem;
}

.loading-dots {
  display: flex;
  gap: 5px;
}

.streaming-content {
  line-height: 1.6;
}

/* Markdown 渲染样式 */
.msg-bubble :deep(p) {
  margin: 0 0 8px;
}

.msg-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.msg-bubble :deep(ul),
.msg-bubble :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.msg-bubble :deep(li) {
  margin-bottom: 4px;
}

.msg-bubble :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.85rem;
  font-family: Consolas, Monaco, monospace;
}

.msg-bubble :deep(pre) {
  background: #1f2937;
  color: #e5e7eb;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.msg-bubble :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.msg-bubble :deep(strong) {
  font-weight: 600;
}

.msg-bubble :deep(h1),
.msg-bubble :deep(h2),
.msg-bubble :deep(h3) {
  margin: 12px 0 8px;
  font-weight: 600;
}

.msg-bubble :deep(hr) {
  border: none;
  border-top: 1px solid #e4e7ed;
  margin: 12px 0;
}

/* 文档上下文提示 */
.context-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  background: #ecf5ff;
  border-radius: 8px;
  font-size: 0.8rem;
  color: #409eff;
}

/* 停止生成按钮 */
.stop-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  border: none;
  border-radius: 40px;
  color: #fff;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.3);
}

.stop-btn:hover {
  background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.4);
}

.stop-btn .el-icon {
  font-size: 1.1rem;
}

/* 文档预览对话框 */
.ai-doc-preview-dialog .doc-preview-content {
  padding: 0;
}

.preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}

.loading-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.preview-body .preview-meta {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.preview-body .preview-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #606266;
}

.preview-body .preview-text {
  padding: 20px;
  font-size: 0.9rem;
  line-height: 1.8;
  color: #303133;
  max-height: 350px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.preview-body .more-text {
  color: #909399;
}

/* 滑动动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-8px) rotate(1deg);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}


/* 底部Tab切换 - 简洁风格 */
.bottom-tabs {
  position: fixed;
  bottom: 20px;
  /* 在主内容区域内居中：考虑左侧220px侧栏 */
  left: calc(50% + 110px);
  transform: translateX(-50%);
  display: inline-flex;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 100;
  padding: 4px;
  overflow: hidden;
  min-width: 200px;
}

.tab-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: #409eff;
  border-radius: 36px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  transition: left 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 0;
}

.tab-slider::before {
  display: none;
}

.tab-slider.slide-right {
  left: 50%;
}

.tab-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #909399;
  font-size: 0.85rem;
  font-weight: 500;
  white-space: nowrap;
  flex: 1;
  z-index: 1;
  border-radius: 36px;
  user-select: none;
}

.tab-item .el-icon {
  font-size: 1rem;
  transition: all 0.2s ease;
}

.tab-item:hover:not(.active) {
  color: #606266;
}

.tab-item.active {
  color: #fff;
}

.tab-item.active .el-icon {
  color: #fff;
}

.search-mode-view {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.search-mode-view::-webkit-scrollbar {
  display: none;
}

/* ===== 搜索结果界面 ===== */
.results-view {
  position: relative;
  z-index: 1;
}

/* 顶部栏 */
.results-header {
  position: sticky;
  top: 0;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.brand:hover {
  opacity: 0.8;
}

.brand-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409eff;
  border-radius: 10px;
  color: #fff;
  font-size: 1.1rem;
}

.brand-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: #303133;
}

.header-search {
  flex: 1;
  max-width: 500px;
}

.mini-search-box {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 4px 4px 4px 14px;
  transition: all 0.2s;
}

.mini-search-box:focus-within {
  border-color: #409eff;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.search-prefix {
  color: #909399;
  margin-right: 8px;
}

.mini-search-box input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #303133;
  font-size: 0.9rem;
  padding: 8px 0;
}

.mini-search-box input::placeholder {
  color: #c0c4cc;
}

.mini-search-btn {
  background: #409eff;
  border: none;
  border-radius: 8px;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.mini-search-btn:hover {
  background: #66b1ff;
}

.header-actions {
  margin-left: auto;
}

.upload-btn {
  background: #409eff;
  border: none;
  border-radius: 8px;
  padding: 10px 18px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.upload-btn:hover {
  background: #66b1ff;
}

/* 主内容区 */
.results-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
}

/* 筛选面板 */
.filter-panel {
  width: 260px;
  flex-shrink: 0;
}

.filter-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  position: sticky;
  top: 80px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.filter-section {
  margin-bottom: 24px;
}

.filter-section:last-of-type {
  margin-bottom: 20px;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.filter-title .el-icon {
  color: #409eff;
}

.filter-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-option:hover {
  background: #f5f7fa;
}

.filter-option.active {
  background: #ecf5ff;
}

.filter-option input {
  display: none;
}

.option-indicator {
  width: 16px;
  height: 16px;
  border: 2px solid #dcdfe6;
  border-radius: 50%;
  position: relative;
  transition: all 0.2s;
}

.filter-option.active .option-indicator {
  border-color: #409eff;
  background: #409eff;
}

.filter-option.active .option-indicator::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  background: #fff;
  border-radius: 50%;
}

.option-text {
  font-size: 0.85rem;
  color: #606266;
}

.filter-option.active .option-text {
  color: #409eff;
}

.reset-filters-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  color: #606266;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.reset-filters-btn:hover {
  background: #ecf5ff;
  color: #409eff;
  border-color: #409eff;
}

/* 结果内容区 */
.results-content {
  flex: 1;
  min-width: 0;
}

.results-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  flex-wrap: wrap;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.result-stats {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-keyword {
  font-size: 0.95rem;
  font-weight: 600;
  color: #409eff;
}

.stats-count {
  font-size: 0.85rem;
  color: #909399;
}

.stats-count strong {
  color: #303133;
  font-weight: 600;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.select-all-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 0.85rem;
}

.select-all-checkbox input {
  display: none;
}

.checkbox-indicator {
  width: 16px;
  height: 16px;
  border: 2px solid #dcdfe6;
  border-radius: 3px;
  position: relative;
  transition: all 0.2s;
}

.select-all-checkbox input:checked + .checkbox-indicator {
  background: #409eff;
  border-color: #409eff;
}

.select-all-checkbox input:checked + .checkbox-indicator::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 11px;
  font-weight: bold;
}

.batch-download-btn {
  background: #67c23a;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.batch-download-btn:hover {
  background: #85ce61;
}

/* 空结果 */
.empty-results {
  text-align: center;
  padding: 60px 20px;
}

.empty-illustration {
  width: 160px;
  height: 160px;
  margin: 0 auto 20px;
  opacity: 0.6;
}

.empty-svg {
  width: 100%;
  height: 100%;
}

.empty-svg circle {
  fill: #f5f7fa;
}

.empty-svg path {
  fill: #dcdfe6;
  stroke: #909399;
}

.empty-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.empty-desc {
  color: #909399;
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 20px;
  color: #606266;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

/* 结果列表 */
.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 结果卡片动画 */
.result-item-enter-active {
  animation: slideIn 0.4s ease forwards;
  animation-delay: var(--delay);
  opacity: 0;
}

.result-item-leave-active {
  animation: slideOut 0.3s ease forwards;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideOut {
  to {
    opacity: 0;
    transform: translateX(-20px);
  }
}

.result-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.result-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.1);
}

.card-checkbox {
  padding-top: 4px;
}

.custom-checkbox {
  display: flex;
  cursor: pointer;
}

.custom-checkbox input {
  display: none;
}

.checkbox-mark {
  width: 18px;
  height: 18px;
  border: 2px solid #dcdfe6;
  border-radius: 4px;
  position: relative;
  transition: all 0.2s;
}

.custom-checkbox input:checked + .checkbox-mark {
  background: #409eff;
  border-color: #409eff;
}

.custom-checkbox input:checked + .checkbox-mark::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 11px;
  font-weight: bold;
}

.card-icon {
  flex-shrink: 0;
}

.icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 1.2rem;
}

.icon-pdf {
  background: #fef0f0;
  color: #f56c6c;
}

.icon-doc {
  background: #ecf5ff;
  color: #409eff;
}

.icon-image {
  background: #f0f9eb;
  color: #67c23a;
}

.icon-video {
  background: #fdf6ec;
  color: #e6a23c;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.card-title {
  flex: 1;
  font-size: 1rem;
  font-weight: 600;
  color: #303133;
  margin: 0;
  line-height: 1.4;
}

.card-title :deep(em),
.card-title :deep(mark),
.card-title :deep(.highlight-text) {
  color: #409eff;
  font-style: normal;
  font-weight: 700;
  background: transparent;
}

.card-badges {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 500;
}

.badge-hot {
  background: #fdf6ec;
  color: #e6a23c;
}

.badge-new {
  background: #f0f9eb;
  color: #67c23a;
}

.card-snippet {
  font-size: 0.85rem;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-snippet :deep(em),
.card-snippet :deep(mark),
.card-snippet :deep(.highlight-text) {
  color: #409eff;
  font-style: normal;
  font-weight: 500;
  background: transparent;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: #909399;
}

.meta-item .el-icon {
  font-size: 0.85rem;
  color: #c0c4cc;
}

.card-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #c0c4cc;
  transition: all 0.2s;
  align-self: center;
}

.result-card:hover .card-arrow {
  color: #409eff;
  transform: translateX(4px);
}

/* 分页 */
.pagination-wrapper {
  margin-top: 32px;
  text-align: center;
}

.custom-pagination {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e4e7ed;
  padding: 6px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.page-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #606266;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #f5f7fa;
  color: #409eff;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-num {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #606266;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.page-num:hover:not(:disabled):not(.ellipsis) {
  background: #f5f7fa;
  color: #409eff;
}

.page-num.active {
  background: #409eff;
  color: #fff;
  font-weight: 500;
}

.page-num.ellipsis {
  cursor: default;
  color: #c0c4cc;
}

.page-info {
  margin-top: 10px;
  font-size: 0.8rem;
  color: #909399;
}

/* 响应式 */
@media (max-width: 1024px) {
  .results-main {
    flex-direction: column;
  }
  
  .filter-panel {
    width: 100%;
  }
  
  .filter-card {
    position: static;
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }
  
  .filter-section {
    margin-bottom: 0;
    flex: 1;
    min-width: 200px;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
  
  .search-box {
    padding: 4px 4px 4px 16px;
  }
  
  .search-btn span {
    display: none;
  }
  
  .search-btn {
    padding: 14px;
    border-radius: 50%;
  }
  
  .header-content {
    padding: 12px 16px;
  }
  
  .brand-text {
    display: none;
  }
  
  .results-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .result-card {
    flex-wrap: wrap;
  }
  
  .card-icon {
    order: -1;
  }
  
  .card-arrow {
    display: none;
  }
}
</style>
