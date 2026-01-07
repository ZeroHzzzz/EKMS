<template>
  <div class="knowledge-detail-container" v-loading="loading">
    <div v-if="knowledge" class="detail-wrapper">
      <!-- 草稿/版本状态提示横幅 -->
      <div v-if="showDraftBanner" class="draft-banner" :class="draftBannerType">
        <el-icon><Warning /></el-icon>
        <span>{{ draftBannerMessage }}</span>
        <el-button v-if="canViewDraft" size="small" type="primary" text @click="toggleViewMode">
          {{ isViewingDraft ? '查看已发布版本' : '查看待审核草稿' }}
        </el-button>
      </div>
      
      <!-- 头部信息 -->
      <div class="detail-header" :class="{ 'edit-mode-header': isEditMode }">
        <div class="header-left">
          <el-button class="back-btn" @click="goBack" :icon="ArrowLeft" circle />
          
          <!-- 阅读模式标题 -->
          <h1 v-if="!isEditMode" class="title">
            {{ displayContent.title }}
            <el-tag v-if="isViewingDraft" size="small" type="warning" effect="dark" style="margin-left: 8px;">草稿</el-tag>
          </h1>
          
          <!-- 编辑模式：沉浸式标题输入 -->
          <div v-else class="immersive-title-editor">
             <el-input 
               v-model="editForm.title" 
               placeholder="请输入文档标题" 
               class="title-input-immersive"
               :input-style="{ fontSize: '24px', fontWeight: '600', padding: '0', border: 'none', boxShadow: 'none', background: 'transparent' }"
             />
          </div>
        </div>
        
        <div class="header-actions">
          <template v-if="isEditMode">
            <span class="unsaved-hint" v-if="editFormDirty">未保存</span>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
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
        <div class="left-panel" :class="{ 'is-editing': isEditMode }">
          <!-- 编辑模式表单 -->
          <div v-if="isEditMode" class="edit-canvas">
            <div class="edit-card meta-info-card">
              <div class="card-title">基本信息</div>
              <el-form :model="editForm" label-position="top" class="edit-form-grid">
                 <el-row :gutter="20">
                   <el-col :span="12">
                     <el-form-item label="分类">
                        <el-input v-model="editForm.category" placeholder="选择或输入分类" />
                      </el-form-item>
                   </el-col>
                   <el-col :span="12">
                     <el-form-item label="关键词">
                        <el-input v-model="editForm.keywords" placeholder="关键词，多个用逗号分隔" />
                      </el-form-item>
                   </el-col>
                 </el-row>
                 
                <el-form-item label="摘要">
                  <el-input 
                    v-model="editForm.summary" 
                    type="textarea" 
                    :rows="3" 
                    placeholder="文档内容的简要概述..." 
                    resize="none"
                  />
                </el-form-item>
                
                <el-form-item label="变更说明" class="change-desc-item">
                  <el-input 
                    v-model="editForm.changeDescription" 
                    type="textarea" 
                    :rows="2" 
                    placeholder="描述本次修改的内容（可选）..." 
                    resize="none"
                  />
                </el-form-item>
              </el-form>
            </div>


          </div>

          <!-- 查看模式：文件预览/文本内容 -->
          <div v-else class="view-container">
            <!-- 只有纯文本内容 -->
            <div v-if="!displayContent.fileId && displayContent.content" class="text-content-viewer">
              <pre>{{ displayContent.content }}</pre>
            </div>

            <!-- 文件预览区 -->
            <div v-if="currentFileId && fileInfo" class="file-preview-viewer">
              <div class="preview-toolbar">
                <span class="file-name">
                  <el-icon><Document /></el-icon> {{ fileInfo.fileName }}
                </span>
                <div class="preview-actions">
                  <!-- 提交审核按钮：有未提交的草稿时显示 -->
                  <el-button 
                    v-if="canSubmitForReview" 
                    type="warning" 
                    size="small" 
                    @click="submitForReview"
                  >
                    <el-icon><Upload /></el-icon> 提交审核
                  </el-button>
                  <!-- 只有查看最新版本（草稿）时才显示在线编辑按钮 -->
                  <el-button v-if="canEditOffice(fileInfo.fileType) && (isViewingDraft || !hasDraft)" type="primary" size="small" @click="goToOfficeEdit('edit')">
                    <el-icon><Edit /></el-icon> 在线编辑
                  </el-button>
                  <el-button text type="primary" @click="downloadFile">下载</el-button>
                  <el-button text type="primary" @click="openInNewWindow">新窗口打开</el-button>
                </div>
              </div>
              
              <div class="preview-stage" :key="previewKey">
                <!-- PDF -->
                <iframe 
                  v-if="isPdfFile(fileInfo.fileType)"
                  :src="previewUrl" 
                  :key="'pdf-' + previewKey"
                  class="preview-frame"
                ></iframe>

                <!-- 图片 -->
                <div v-else-if="isImageFile(fileInfo.fileType)" class="image-preview-box">
                  <img :src="previewUrl" :key="'img-' + previewKey" :alt="fileInfo.fileName" />
                </div>

                <!-- 视频 -->
                <div v-else-if="isVideoFile(fileInfo.fileType)" class="video-preview-box">
                  <!-- 使用 kkFileView 预览视频 (iframe) -->
                  <iframe 
                    :src="kkFileViewUrl" 
                    :key="'video-' + previewKey"
                    class="preview-frame"
                    allowfullscreen
                  ></iframe>
                </div>

                 <!-- 音频 -->
                <div v-else-if="isAudioFile(fileInfo.fileType)" class="audio-preview-box">
                  <audio :src="previewUrl" :key="'audio-' + previewKey" controls class="audio-player"></audio>
                </div>

                <!-- Office / 文本 (kkFileView) -->
                <div v-else-if="isOfficeFile(fileInfo.fileType) || isTextFile(fileInfo.fileType)" class="office-preview-box">
                  <iframe 
                    :src="kkFileViewUrl" 
                    :key="'office-' + previewKey"
                    class="preview-frame"
                  ></iframe>
                </div>

                 <!-- 不支持的类型 -->
                 <div v-else class="unsupported-box">
                  <el-empty description="该格式暂不支持直接嵌入预览，请尝试新窗口打开或下载">
                    <div class="unsupported-actions">
                        <el-button type="primary" @click="downloadFile">下载文件</el-button>
                        <el-button type="success" @click="openInNewWindow">新窗口预览</el-button>
                    </div>
                  </el-empty>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：属性/关联/评论/AI -->
        <div class="right-panel">
          <el-tabs v-model="activeRightTab" class="right-tabs">
            <el-tab-pane label="属性" name="properties">
              <div class="properties-panel">
                <div class="prop-group">
                  <label>基本信息</label>
                  <div class="prop-item">
                    <span class="label">作者</span>
                    <span class="value">{{ displayContent.author }}</span>
                  </div>
                   <div class="prop-item">
                    <span class="label">版本</span>
                    <span class="value">
                      v{{ displayContent.version || knowledge.version }}
                      <el-tag v-if="isViewingDraft" size="small" type="warning" style="margin-left: 4px;">草稿</el-tag>
                      <el-tag v-else-if="hasDraft" size="small" type="success" style="margin-left: 4px;">已发布</el-tag>
                    </span>
                  </div>
                  <div class="prop-item">
                    <span class="label">状态</span>
                    <el-tag size="small" :type="getStatusType(knowledge.status)">{{ getStatusText(knowledge.status) }}</el-tag>
                    <el-tag v-if="hasDraft" size="small" type="warning" effect="plain" style="margin-left: 4px;">有草稿</el-tag>
                  </div>
                   <div class="prop-item">
                    <span class="label">分类</span>
                    <span class="value">{{ displayContent.category }}</span>
                  </div>
                   <div class="prop-item">
                    <span class="label">时间</span>
                    <span class="value">{{ formatTime(displayContent.createTime || knowledge.updateTime || knowledge.createTime) }}</span>
                  </div>
                  <div class="prop-item" style="opacity: 0.6; font-size: 11px;">
                    <span class="label">Debug</span>
                    <span class="value">FileID: {{ fileInfo?.id || '?' }} | Ver: {{ currentViewingVersion || knowledge.version }}</span>
                  </div>
                </div>

                <div class="prop-group" v-if="displayContent.summary">
                  <label>摘要</label>
                  <p class="summary-text">{{ displayContent.summary }}</p>
                </div>

                <div class="prop-group" v-if="displayContent.keywords">
                  <label>关键词</label>
                  <div class="tags-wrapper">
                    <el-tag 
                      v-for="tag in (displayContent.keywords || '').split(',')" 
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

            <!-- AI 助手 Tab -->
            <el-tab-pane label="AI助手" name="ai">
              <div class="ai-chat-panel">
                <!-- 文档上下文提示 -->
                <div class="doc-context-hint">
                  <el-icon><Document /></el-icon>
                  <span>基于当前文档内容回答</span>
                </div>

                <!-- 欢迎信息 -->
                <div v-if="aiMessages.length === 0 && !aiLoading" class="ai-welcome">
                  <el-icon class="welcome-icon"><ChatDotRound /></el-icon>
                  <h3>文档智能问答</h3>
                  <p>直接向我询问关于这篇文档的任何问题</p>
                  <div class="quick-questions">
                    <el-button 
                      v-for="q in quickQuestions" 
                      :key="q" 
                      size="small"
                      @click="askQuickQuestion(q)"
                    >
                      {{ q }}
                    </el-button>
                  </div>
                </div>

                <!-- 对话列表 -->
                <div v-else class="ai-messages" ref="aiMessagesRef">
                  <div 
                    v-for="(msg, idx) in aiMessages" 
                    :key="idx" 
                    class="ai-message"
                    :class="msg.role"
                  >
                    <div class="message-avatar">
                      <el-icon v-if="msg.role === 'assistant'"><ChatDotRound /></el-icon>
                      <span v-else>{{ (userStore.userInfo?.realName || 'U').charAt(0) }}</span>
                    </div>
                    <div class="message-content">
                      <div class="message-text" v-html="formatMessageContent(msg.content)"></div>
                    </div>
                  </div>
                  
                  <!-- 流式输出中 -->
                  <div v-if="aiLoading" class="ai-message assistant">
                    <div class="message-avatar">
                      <el-icon><ChatDotRound /></el-icon>
                    </div>
                    <div class="message-content">
                      <div v-if="aiStreamingContent" class="message-text streaming" v-html="formatMessageContent(aiStreamingContent)"></div>
                      <div v-else class="typing-indicator">
                        <span></span><span></span><span></span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 输入区 -->
                <div class="ai-input-area">
                  <el-input
                    v-model="aiInput"
                    type="textarea"
                    :rows="2"
                    placeholder="输入您的问题... (Enter发送, Shift+Enter换行)"
                    @keydown.enter.exact.prevent="sendAiMessage"
                    :disabled="aiLoading"
                  />
                  <div class="ai-input-actions">
                    <span class="hint">Enter 发送</span>
                    <el-button 
                      v-if="!aiLoading"
                      type="primary" 
                      @click="sendAiMessage" 
                      :disabled="!aiInput.trim()"
                    >
                      发送
                    </el-button>
                    <el-button 
                      v-else
                      type="danger" 
                      @click="stopAiGeneration"
                    >
                      停止
                    </el-button>
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
                       @click="$router.push(`/knowledge/${item.id}`)"
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
                  
                  <el-empty v-if="knowledgeRelations.length === 0 && suggestedRelations.length === 0" description="暂无关联" :image-size="60" />
                 </div>
               </div>
            </el-tab-pane>

            <el-tab-pane label="评论" name="comments">
              <div class="comments-panel">
                <div class="comment-input-box">
                  <el-input
                    v-model="newCommentContent"
                    type="textarea"
                    placeholder="发表看法... (Enter发送, Shift+Enter换行)"
                    :rows="2"
                    @keydown.enter.exact.prevent="submitComment"
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
                 <!-- Git风格的commit列表 -->
                 <div class="git-log">
                    <div 
                      v-for="(version, index) in versions" 
                      :key="version.id"
                      class="commit-item"
                      :class="{ 'is-current': version.version === knowledge.version, 'is-viewing': currentViewingVersion === version.version }"
                      @click.stop="switchToVersion(version)"
                    >
                      <!-- Commit线和节点 -->
                      <div class="commit-graph">
                        <div class="commit-line" v-if="index < versions.length - 1"></div>
                        <div class="commit-node" :class="{ 'is-current': version.version === knowledge.version }">
                          <el-icon v-if="version.version === knowledge.version"><CircleCheckFilled /></el-icon>
                        </div>
                      </div>
                      
                      <!-- Commit内容 -->
                      <div class="commit-content">
                        <div class="commit-header">
                        <div class="commit-hash-row">
                          <span class="commit-hash" :title="version.commitHash">
                            {{ version.commitHash ? version.commitHash.substring(0, 8) : '无hash' }}
                          </span>
                          <el-tag v-if="version.isPublished" size="small" type="success" effect="dark">当前版本</el-tag>
                          <el-tag v-else-if="version.status === 'PENDING'" size="small" type="warning" effect="dark">待审核</el-tag>
                          <el-tag v-else-if="version.status === 'REJECTED'" size="small" type="danger" effect="plain">已驳回</el-tag>
                          <el-tag v-else size="small" type="info" effect="plain">未发布</el-tag>
                          <el-tag v-if="version.branch" size="small" effect="plain">{{ version.branch }}</el-tag>
                        </div>
                          <span class="commit-time">{{ formatTimeFriendly(version.createTime) }}</span>
                        </div>
                        
                        <div class="commit-message">
                          {{ version.commitMessage || version.changeDescription || '无变更说明' }}
                        </div>
                        
                        <div class="commit-meta">
                          <span class="commit-author" :style="{ color: getUserColor(version.createdBy) }">
                            <span class="author-avatar" :style="{ background: getUserColor(version.createdBy) }">
                              {{ getAuthorInitial(version.createdBy) }}
                            </span>
                            {{ version.createdBy || '未知' }}
                          </span>
                          <span class="commit-version">v{{ version.version }}</span>
                        </div>
                        
                        <div class="commit-actions">
                          <el-button 
                            size="small" 
                            :type="currentViewingVersion === version.version ? 'success' : 'primary'"
                            text 
                            @click.stop="switchToVersion(version)"
                          >
                            <el-icon><View /></el-icon> 
                            {{ currentViewingVersion === version.version ? '当前查看' : '切换预览' }}
                          </el-button>
                          <el-button size="small" text type="info" @click="viewVersionDetails(version)">
                            <el-icon><Document /></el-icon> 详情
                          </el-button>
                          <el-button 
                            size="small" 
                            text 
                            type="warning"
                            v-if="version.version !== knowledge.version"
                            @click="compareVersion(version)"
                          >
                            <el-icon><Refresh /></el-icon> 对比
                          </el-button>
                          <el-button 
                            size="small" 
                            text 
                            type="danger"
                            v-if="isAdmin(userStore.userInfo) && version.isPublished && version.version !== knowledge.version"
                            @click="revertToVersion(version)"
                          >
                            <el-icon><RefreshLeft /></el-icon> 回退
                          </el-button>
                        </div>
                      </div>
                    </div>
                    
                    <el-empty v-if="versions.length === 0" description="暂无版本历史" :image-size="60" />
                 </div>
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

    <!-- Version View Dialog -->
    <el-dialog v-model="showVersionViewDialog" title="版本详情" width="70%">
       <div class="version-view-dialog">
         <!-- 版本状态提示 -->
         <div v-if="selectedVersion && !selectedVersion.isPublished" class="version-status-banner">
           <el-alert 
             :title="getVersionStatusMessage(selectedVersion)" 
             :type="selectedVersion.status === 'PENDING' ? 'warning' : (selectedVersion.status === 'REJECTED' ? 'error' : 'info')"
             :closable="false"
             show-icon
           />
         </div>
         
         <div class="version-info-header">
           <div class="version-info-item">
             <span class="label">版本号</span>
             <span class="value">
               v{{ selectedVersion?.version }}
               <el-tag v-if="selectedVersion?.isPublished" size="small" type="success" style="margin-left:4px">已发布</el-tag>
               <el-tag v-else-if="selectedVersion?.status === 'PENDING'" size="small" type="warning" style="margin-left:4px">待审核</el-tag>
               <el-tag v-else-if="selectedVersion?.status === 'REJECTED'" size="small" type="danger" style="margin-left:4px">已驳回</el-tag>
               <el-tag v-else size="small" type="info" style="margin-left:4px">未发布</el-tag>
             </span>
           </div>
           <div class="version-info-item">
             <span class="label">Commit</span>
             <span class="value commit-hash">{{ selectedVersion?.commitHash }}</span>
           </div>
           <div class="version-info-item">
             <span class="label">分支</span>
             <span class="value">{{ selectedVersion?.branch || 'main' }}</span>
           </div>
           <div class="version-info-item">
             <span class="label">创建者</span>
             <span class="value">{{ selectedVersion?.createdBy }}</span>
           </div>
           <div class="version-info-item">
             <span class="label">创建时间</span>
             <span class="value">{{ formatTime(selectedVersion?.createTime) }}</span>
           </div>
           <div class="version-info-item" v-if="selectedVersion?.fileId">
             <span class="label">关联文件</span>
             <span class="value">
               <el-button size="small" type="primary" text @click="previewVersionFile(selectedVersion)">
                 <el-icon><Document /></el-icon> 预览文件
               </el-button>
               <el-button size="small" type="success" text @click="downloadVersionFile(selectedVersion)">
                 <el-icon><Download /></el-icon> 下载
               </el-button>
             </span>
           </div>
         </div>
         <div class="version-message">
           <strong>变更说明：</strong>{{ selectedVersion?.commitMessage || selectedVersion?.changeDescription || '无' }}
         </div>
         
         <!-- 操作按钮 -->
         <div class="version-actions" style="margin-top: 16px; display: flex; gap: 12px;">
           <el-button type="primary" @click="switchToVersionFromDialog(selectedVersion)">
             <el-icon><View /></el-icon> 切换到此版本预览
           </el-button>
         </div>
         
         <el-divider>文本内容</el-divider>
         <div class="version-content-wrapper">
           <pre class="version-content">{{ selectedVersion?.content || '(无文本内容，请查看文件预览)' }}</pre>
         </div>
       </div>
    </el-dialog>
    
    <!-- Version Compare Dialog -->
    <el-dialog v-model="showCompareDialog" title="版本对比" width="85%" class="compare-dialog">
       <div class="compare-header">
         <div class="compare-version old">
           <el-tag type="danger" effect="dark">旧版本 v{{ compareData.version1 }}</el-tag>
           <span class="compare-hash">{{ compareData.hash1?.substring(0, 8) }}</span>
         </div>
         <el-icon class="compare-arrow"><ArrowRight /></el-icon>
         <div class="compare-version new">
           <el-tag type="success" effect="dark">新版本 v{{ compareData.version2 }}</el-tag>
           <span class="compare-hash">{{ compareData.hash2?.substring(0, 8) }}</span>
         </div>
       </div>
       
       <div class="compare-stats" v-if="compareData.stats">
         <el-tag type="success" effect="plain">
           <el-icon><Plus /></el-icon> {{ compareData.stats.insertCount }} 行新增
         </el-tag>
         <el-tag type="danger" effect="plain">
           <el-icon><Minus /></el-icon> {{ compareData.stats.deleteCount }} 行删除
         </el-tag>
         <el-tag type="info" effect="plain">
           {{ compareData.stats.equalCount }} 行未变
         </el-tag>
       </div>
       
       <div class="diff-container" v-loading="compareLoading">
         <div class="diff-view">
           <div 
             v-for="(line, idx) in compareData.diffLines" 
             :key="idx"
             class="diff-line"
             :class="getDiffLineClass(line.type)"
           >
             <span class="line-number old">{{ line.lineNumber1 || '' }}</span>
             <span class="line-number new">{{ line.lineNumber2 || '' }}</span>
             <span class="line-type">{{ getDiffLineSymbol(line.type) }}</span>
             <span class="line-content">{{ line.content }}</span>
           </div>
           <el-empty v-if="!compareData.diffLines?.length && !compareLoading" description="内容完全相同，无差异" />
         </div>
       </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import api from '../api'
import { sendMessageStream } from '../api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { hasRole, isAdmin, ROLE_ADMIN, ROLE_EDITOR } from '../utils/permission'
import { Star, StarFilled, Edit, Document, Link, Delete, ArrowLeft, User, View, ChatDotRound, CircleCheckFilled, Refresh, TopRight, ArrowRight, Plus, Minus, Warning, RefreshLeft, Download, Upload } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { watermark } from '../utils/watermark'

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
const suggestedRelations = ref([])
const editFormDirty = ref(false)
const isEditingContent = ref(false) // Control content editor visibility
const saving = ref(false) // Add saving state
const showAddRelationDialog = ref(false)

const addRelationForm = ref({ relatedKnowledgeId: null, relationType: 'RELATED' })
const relationSearchResults = ref([])
const searchLoading = ref(false)
const versions = ref([])
const showVersionViewDialog = ref(false)
const selectedVersion = ref(null)
const relationGraphRef = ref(null)
const currentViewingVersion = ref(null)  // 当前查看的版本号
const currentVersionContent = ref(null)  // 当前查看版本的内容

// 版本对比相关状态
const showCompareDialog = ref(false)
const compareLoading = ref(false)
const compareData = ref({
  version1: null,
  version2: null,
  hash1: null,
  hash2: null,
  diffLines: [],
  stats: null
})

// 草稿/发布版本相关状态
const publishedVersionContent = ref(null)  // 已发布版本的内容
const isViewingDraft = ref(false)          // 是否正在查看草稿版本

// AI Chat State
const aiMessages = ref([])
const aiInput = ref('')
const aiLoading = ref(false)
const aiMessagesRef = ref(null)
const aiStreamingContent = ref('')
const aiAbortController = ref(null)
const quickQuestions = [
  '主要内容是什么？',
  '总结关键要点',
  '有哪些重要概念？'
]


// Computed
const isEditMode = computed(() => route.query.edit === 'true')

// Watch for changes in editForm to set dirty flag
watch(editForm, (newVal) => {
  if (!knowledge.value) return
  // 如果不在编辑模式，不设置脏标记
  if (!isEditMode.value) return 
  console.log('DEBUG: editForm changed', newVal)
  editFormDirty.value = true
  console.log('DEBUG: editFormDirty set to true')
}, { deep: true })

// Watch isEditMode to reload data when exiting edit mode
watch(isEditMode, async (val) => {
  if (!val) {
    // Exiting edit mode
    console.log('DEBUG: Exiting edit mode, reloading detail...')
    editFormDirty.value = false // Reset dirty flag
    await loadDetail()
  } else {
    // Entering edit mode
    console.log('DEBUG: Entering edit mode, initializing form...')
    if (knowledge.value) {
        editForm.value = { 
            ...knowledge.value,
            changeDescription: '' 
        }
        nextTick(() => {
            editFormDirty.value = false
            console.log('DEBUG: editFormDirty reset to false')
        })
    }
  }
})

watch(isEditMode, (val) => {
  if (val) {
    isEditingContent.value = false // Default to collapsed when entering edit mode
    // Ensure editForm is synced if not already
    if (!editForm.value.id && knowledge.value.id) {
       editForm.value = { ...knowledge.value, changeDescription: '' }
    }
  }
})

// 用于强制刷新 iframe 的时间戳
const previewTimestamp = ref(Date.now())

const previewUrl = computed(() => {
  if (!fileInfo.value) return ''
  return `/api/file/preview/${fileInfo.value.id}?t=${previewTimestamp.value}`
})

// 用于强制刷新 iframe 的 key（当文件 ID 变化或版本切换时更新）
const previewKey = computed(() => {
  const fileId = fileInfo.value?.id || currentFileId.value || 'none'
  const version = currentViewingVersion.value || 'current'
  return `${fileId}-${version}-${previewTimestamp.value}`
})

// kkFileView 预览 URL（计算属性，响应式更新）
const kkFileViewUrl = computed(() => {
  if (!fileInfo.value) return ''
  const backendBase = 'http://host.docker.internal:8080' 
  const fileDownloadUrl = `${backendBase}/api/file/download/${fileInfo.value.id}`
  const fullFileName = fileInfo.value.fileName || ''
  const version = currentViewingVersion.value || (knowledge.value ? knowledge.value.version : 'latest')
  const versionPrefix = `v${version}_`
  const finalDownloadUrl = `${fileDownloadUrl}?fullfilename=${encodeURIComponent(versionPrefix + fullFileName)}&t=${previewTimestamp.value}`
  const kkBase = 'http://localhost:8012'
  return `${kkBase}/onlinePreview?url=${encodeURIComponent(btoa(finalDownloadUrl))}`
})

// 预览版本内容（通过URL参数指定版本）
const previewVersionContent = ref(null)

// 是否有待审核草稿
const hasDraft = computed(() => {
  return knowledge.value?.hasDraft === true && 
         knowledge.value?.publishedVersion != null &&
         knowledge.value?.version !== knowledge.value?.publishedVersion
})

// 是否可以查看草稿（作者或管理员）
const canViewDraft = computed(() => {
  return hasDraft.value && canEdit(knowledge.value)
})

// 是否可以提交审核
// 条件：1. 初始 PENDING 版本（还没发布过） 2. 已发布后有草稿
const canSubmitForReview = computed(() => {
  if (!knowledge.value) return false
  
  const isPending = knowledge.value?.status === 'PENDING'
  const hasDraftFlag = knowledge.value?.hasDraft === true
  const hasPublishedVersion = knowledge.value?.publishedVersion != null && knowledge.value?.publishedVersion > 0
  
  // 初始 PENDING 版本（还没有发布过）：可以提交
  if (isPending && !hasPublishedVersion) {
    return canEdit(knowledge.value)
  }
  
  // 已发布后有草稿：可以提交
  if (hasPublishedVersion && hasDraftFlag) {
    return canEdit(knowledge.value)
  }
  
  return false
})

// 是否显示横幅
const showDraftBanner = computed(() => {
  // 查看历史版本、预览特定版本、或有草稿时显示横幅
  return isViewingHistoryVersion.value || hasDraft.value || (previewVersionContent.value && isViewingDraft.value)
})

// 横幅类型
const draftBannerType = computed(() => {
  if (isViewingHistoryVersion.value) {
    return 'banner-history'
  }
  if (previewVersionContent.value && isViewingDraft.value) {
    return 'banner-draft'
  }
  if (canViewDraft.value) {
    return isViewingDraft.value ? 'banner-draft' : 'banner-info'
  }
  return 'banner-info'
})

// 横幅消息
const draftBannerMessage = computed(() => {
  // 如果正在查看历史版本
  if (isViewingHistoryVersion.value && currentVersionContent.value) {
    const versionStatus = currentVersionContent.value.status
    const statusText = versionStatus === 'APPROVED' ? '已发布' : 
                       versionStatus === 'PENDING' ? '待审核' : 
                       versionStatus === 'REJECTED' ? '已驳回' : '历史'
    return `您正在查看历史版本 v${currentViewingVersion.value}（${statusText}），点击"切换预览"可返回当前版本。`
  }
  // 如果是通过URL参数预览特定版本
  if (previewVersionContent.value && isViewingDraft.value) {
    return `您正在预览版本 v${previewVersionContent.value.version}，这是待审核的版本。`
  }
  if (canViewDraft.value) {
    if (isViewingDraft.value) {
      return '您正在查看待审核的草稿版本，审核通过后将发布。'
    } else {
      return '该文章有待审核的新版本，您当前查看的是已发布版本。'
    }
  }
  return '该文章有更新版本正在审核中，您当前查看的是已发布版本。'
})

// 当前显示的内容（根据选择的版本决定）
const displayContent = computed(() => {
  // 如果选择了特定版本查看
  if (currentVersionContent.value && currentViewingVersion.value) {
    return currentVersionContent.value
  }
  // 如果有通过URL参数指定的预览版本，优先显示
  if (previewVersionContent.value && isViewingDraft.value) {
    return previewVersionContent.value
  }
  if (hasDraft.value && !isViewingDraft.value && publishedVersionContent.value) {
    // 显示已发布版本
    return publishedVersionContent.value
  }
  // 显示最新版本（草稿或无草稿时的当前版本）
  return knowledge.value || {}
})

// 当前应该显示的文件ID（根据显示的版本决定）
const currentFileId = computed(() => {
  return displayContent.value?.fileId || knowledge.value?.fileId
})

// 是否正在查看历史版本
const isViewingHistoryVersion = computed(() => {
  return currentViewingVersion.value && currentViewingVersion.value !== knowledge.value?.version
})


// 提交审核
const submitForReview = async () => {
  try {
    // 让用户输入提交信息（选填，默认为"用户x更新知识内容"）
    const defaultMessage = `${userStore.userInfo?.realName || userStore.userInfo?.username || '用户'}更新知识内容`
    let commitMessage = defaultMessage
    
    try {
      const { value } = await ElMessageBox.prompt(
        '请输入提交说明（将显示在版本历史中）：',
        '提交审核',
        {
          confirmButtonText: '提交',
          cancelButtonText: '取消',
          inputPlaceholder: '例如：修复了格式问题，更新了第三章内容',
          inputValue: defaultMessage // 设置默认值
        }
      )
      if (value && value.trim().length > 0) {
        commitMessage = value.trim()
      }
    } catch (e) {
      if (e === 'cancel') {
        return // 用户取消
      }
    }
    
    const res = await api.post(`/knowledge/${knowledge.value.id}/submit-audit`, null, {
      params: { 
        userId: userStore.userInfo.id,
        commitMessage: commitMessage
      }
    })
    
    if (res.code === 200) {
      ElMessage.success('提交审核成功，请等待审核')
      loadDetail() // 重新加载详情
      loadVersions() // 刷新版本列表
    } else {
      ElMessage.error(res.message || '提交审核失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交审核失败', error)
      ElMessage.error('提交审核失败')
    }
  }
}

// Methods
const loadDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}?_t=${Date.now()}`)
    knowledge.value = res.data || {}
    
    editForm.value = { 
        ...knowledge.value,
        changeDescription: '' // Reset change description for new edit
    }
    nextTick(() => {
        editFormDirty.value = false
    })
    
    // 检查是否有预览特定版本的请求
    const previewVersion = route.query.previewVersion
    if (previewVersion) {
      try {
        const versionRes = await api.get(`/knowledge/${route.params.id}/versions/${previewVersion}?_t=${Date.now()}`)
        if (versionRes.code === 200 && versionRes.data) {
          previewVersionContent.value = versionRes.data
          isViewingDraft.value = true  // 显示预览版本
        }
      } catch (e) { 
        console.warn('加载预览版本失败', e) 
        previewVersionContent.value = null
      }
    } else {
      previewVersionContent.value = null
    }
    
    // 如果有草稿，加载已发布版本的内容
    if (knowledge.value.hasDraft && knowledge.value.publishedVersion) {
      try {
        const publishedRes = await api.get(`/knowledge/${route.params.id}/versions/${knowledge.value.publishedVersion}`)
        if (publishedRes.code === 200 && publishedRes.data) {
          publishedVersionContent.value = publishedRes.data
        }
      } catch (e) { 
        console.warn('加载已发布版本失败', e) 
      }
      
      // 判断默认显示哪个版本：
      if (!previewVersion) {
        // 如果有编辑权限，默认加载最新非发布版本（草稿）
        if (canEdit(knowledge.value)) {
            isViewingDraft.value = true
            // 主动加载最新Draft版本的详细内容
            const draftVersion = knowledge.value.version
            if (draftVersion) {
                 try {
                    const draftRes = await api.get(`/knowledge/${route.params.id}/versions/${draftVersion}`)
                    if (draftRes.code === 200 && draftRes.data) {
                        previewVersionContent.value = draftRes.data
                        currentVersionContent.value = draftRes.data // 设置为当前显示内容
                        currentViewingVersion.value = draftVersion
                    }
                 } catch (e) {}
            }
        }
      }
    } else {
      publishedVersionContent.value = null
      if (!previewVersion) {
        isViewingDraft.value = false
      }
    }
    
    // 加载文件信息（根据当前显示版本的fileId）
    await loadFileInfo()
    
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

// 切换查看草稿/已发布版本
const toggleViewMode = async () => {
  isViewingDraft.value = !isViewingDraft.value
  // 切换版本后重新加载文件信息
  await loadFileInfo()
}

// 加载文件信息（根据当前显示版本的fileId）
const loadFileInfo = async () => {
  const fileIdToLoad = currentFileId.value
  console.log('loadFileInfo called, fileIdToLoad:', fileIdToLoad, 'currentViewingVersion:', currentViewingVersion.value)
  
  if (!fileIdToLoad) {
    fileInfo.value = null
    return
  }
  
  try {
    const fileRes = await api.get(`/file/${fileIdToLoad}?_t=${Date.now()}`)
    if (fileRes.code === 200) {
      fileInfo.value = fileRes.data
      // 更新时间戳以强制刷新 iframe
      previewTimestamp.value = Date.now()
    console.log('DEBUG: loadFileInfo success. ID:', fileRes.data.id, 'Hash:', fileRes.data.fileHash)
    }
  } catch (e) { 
    console.warn('文件信息加载失败', e)
    fileInfo.value = null
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
        comments.value = res.code === 200 ? (res.data || []) : []
    } catch (e) {
        comments.value = []
    } finally {
        commentsLoading.value = false
    }
}

const loadKnowledgeRelations = async () => {
   try {
     const res1 = await api.get(`/knowledge/${route.params.id}/relations`)
     knowledgeRelations.value = res1.code === 200 ? (res1.data || []) : []
     
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
   if(fileInfo.value) window.open(kkFileViewUrl.value)
   else window.open(window.location.href)
}

// 保留函数版本用于特殊场景（如版本对话框中的预览）
const getKkFileViewUrl = () => {
  return kkFileViewUrl.value
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
const isOfficeFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['DOC','DOCX','XLS','XLSX','PPT','PPTX','CSV','OFD'])
const isTextFile = (type) => checkFileType(type, fileInfo.value?.fileName, ['TXT','MD','JSON','XML','LOG', 'JAVA', 'JS', 'VUE', 'HTML', 'CSS', 'SQL', 'PROPERTIES', 'YML', 'YAML', 'INI', 'CONF', 'SH', 'BAT'])

// 检查是否支持 OnlyOffice 编辑
const canEditOffice = (type) => {
  // 支持编辑的格式：docx, xlsx, pptx 等
  const editableTypes = ['DOCX', 'XLSX', 'PPTX', 'DOC', 'XLS', 'PPT']
  return checkFileType(type, fileInfo.value?.fileName, editableTypes)
}

// 跳转到 OnlyOffice 编辑页面
const goToOfficeEdit = (mode = 'edit') => {
  if (!fileInfo.value?.id) {
    ElMessage.warning('文件信息不完整')
    return
  }
  router.push({
    path: `/office-edit/${fileInfo.value.id}`,
    query: {
      mode: mode,
      fileName: fileInfo.value.fileName
    }
  })
}

// AI Chat Methods
const sendAiMessage = async () => {
  if (!aiInput.value.trim() || aiLoading.value) return
  
  const userMessage = aiInput.value.trim()
  aiInput.value = ''
  
  aiMessages.value.push({ role: 'user', content: userMessage })
  
  aiLoading.value = true
  aiStreamingContent.value = ''
  aiAbortController.value = new AbortController()
  scrollToBottom()
  
  try {
    // 获取文档内容 - 优先使用 contentText（提取的文本），其次是 content、summary
    const documentContent = knowledge.value.contentText || knowledge.value.content || knowledge.value.summary || ''
    
    const systemPrompt = `你是一个专业的文档问答助手。你的任务是基于提供的文档内容回答用户问题。

文档标题：${knowledge.value.title}
${knowledge.value.keywords ? `关键词：${knowledge.value.keywords}` : ''}

文档内容：
${documentContent.substring(0, 8000)}${documentContent.length > 8000 ? '\n...(内容已截断)' : ''}

规则：
1. 只基于提供的文档内容回答问题，不要编造信息
2. 如果文档中没有相关信息，请明确告知用户
3. 回答要简洁、准确、专业
4. 可以使用 Markdown 格式使回答更清晰`
    
    const history = aiMessages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content }))
    
    const messages = [
      { role: 'system', content: systemPrompt },
      ...history.slice(-6),
      { role: 'user', content: userMessage }
    ]
    
    await sendMessageStream(messages, (chunk) => {
      aiStreamingContent.value += chunk
      scrollToBottom()
    }, { signal: aiAbortController.value.signal })
    
    aiMessages.value.push({ role: 'assistant', content: aiStreamingContent.value })
  } catch (error) {
    if (error.name === 'AbortError') {
      // 用户主动停止
      if (aiStreamingContent.value) {
        aiMessages.value.push({ role: 'assistant', content: aiStreamingContent.value + '\n\n*[已停止生成]*' })
      }
    } else {
      console.error('AI请求失败:', error)
      aiMessages.value.push({ role: 'assistant', content: '抱歉，AI服务暂时不可用，请稍后重试。' })
    }
  } finally {
    aiLoading.value = false
    aiStreamingContent.value = ''
    aiAbortController.value = null
    scrollToBottom()
  }
}

const stopAiGeneration = () => {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
  }
}

const askQuickQuestion = (question) => {
  aiInput.value = question
  sendAiMessage()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (aiMessagesRef.value) {
      aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
    }
  })
}

const formatMessageContent = (content) => {
  return content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\*(.*?)\*/g, '<em>$1</em>').replace(/\n/g, '<br>')
}

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
        categoryId: rel.relatedKnowledge.id
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
  
  window.addEventListener('resize', () => myChart.resize())
}

// Watermark Lifecycle moved to top-level setup


watch(activeRightTab, (val) => {
  if(val === 'relations') {
    nextTick(() => initRelationGraph())
  }
})

const submitComment = async () => {
  if(!newCommentContent.value.trim()) return
  if(!userStore.userInfo?.id) return ElMessage.warning('请登录')
  
  commentSubmitting.value = true
  try {
    await api.post(`/knowledge/${route.params.id}/comments`, { 
        content: newCommentContent.value,
        userId: userStore.userInfo.id
    })
    newCommentContent.value = ''
    loadComments()
    ElMessage.success('评论成功')
  } catch(e) { ElMessage.error('评论失败') }
  finally { commentSubmitting.value = false }
}

const deleteComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
    await api.delete(`/knowledge/comments/${commentId}`)
    ElMessage.success('删除成功')
    loadComments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const deleteRelation = async (relationId) => {
  try {
    await ElMessageBox.confirm('确定删除这个关联吗？', '提示', { type: 'warning' })
    await api.delete(`/knowledge/relations/${relationId}`)
    ElMessage.success('删除成功')
    loadKnowledgeRelations()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// Helpers
const remoteMethod = async (query) => {
  if(!query) return
  searchLoading.value = true
  try {
     const res = await api.post('/knowledge/search', { keyword: query, pageNum: 1, pageSize: 10 })
     relationSearchResults.value = (res.data.list || []).filter(i => i.id !== route.params.id)
  } finally { searchLoading.value = false }
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

const saveEdit = async () => {
  saving.value = true
  try {
    const originalStatus = knowledge.value.status
    const res = await api.put(`/knowledge/${route.params.id}`, {
      ...editForm.value,
      baseVersion: knowledge.value.version, // 传递当前版本号作为基准版本
      updateBy: userStore.userInfo?.username
    })
    
    if (res.code === 200 && res.data) {
      const newStatus = res.data.status
      
      // 如果状态从已发布变为待审核，提示用户
      if (originalStatus === 'APPROVED' && newStatus === 'PENDING') {
        ElMessage.success({
          message: '保存成功！由于内容已修改，文章需要重新审核后才能发布。',
          duration: 5000
        })
      } else {
        ElMessage.success('保存成功')
      }
    } else {
      ElMessage.success('保存成功')
    }
    
    router.replace({ query: {} })
    loadDetail()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

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

// 用户颜色映射（用于版本历史中区分不同用户）
const userColorMap = ref({})
const colorPalette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#00d4aa', '#8b5cf6', '#ec4899']
const getUserColor = (username) => {
  if (!username) return '#909399'
  if (!userColorMap.value[username]) {
    const index = Object.keys(userColorMap.value).length % colorPalette.length
    userColorMap.value[username] = colorPalette[index]
  }
  return userColorMap.value[username]
}

// 获取作者名首字母（用于头像）
const getAuthorInitial = (author) => {
  if (!author) return '?'
  return author.charAt(0).toUpperCase()
}
const formatFileSize = (s) => {
    if(!s) return '0 B'
    const i = Math.floor(Math.log(s) / Math.log(1024))
    return (s / Math.pow(1024, i)).toFixed(2) + ' ' + ['B','KB','MB','GB'][i]
}
const canEdit = (k) => hasRole(userStore.userInfo, ROLE_ADMIN) || (hasRole(userStore.userInfo, ROLE_EDITOR) && k.author === userStore.userInfo.realName)
const canDeleteComment = (c) => hasRole(userStore.userInfo, ROLE_ADMIN) || c.userId === userStore.userInfo.id
const enterEditMode = () => router.push({ query: { ...route.query, edit: 'true' } })
const cancelEdit = async () => {
  console.log('DEBUG: cancelEdit called, dirty:', editFormDirty.value)
  if (editFormDirty.value) {
    try {
      await ElMessageBox.confirm('有未保存的修改，确定要离开吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      router.back()
    } catch (e) {
      // cancel
    }
  } else {
    router.back()
  }
}

// 路由守卫：未保存拦截 (离开组件/路由)
onBeforeRouteLeave(async (to, from, next) => {
  console.log('DEBUG: onBeforeRouteLeave, isEditMode:', isEditMode.value, 'dirty:', editFormDirty.value)
  if (isEditMode.value && editFormDirty.value) {
    try {
      await ElMessageBox.confirm('有未保存的修改，确定要离开吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      next()
    } catch (e) {
      next(false)
    }
  } else {
    next()
  }
})

// 路由守卫：未保存拦截 (同组件参数变更，如 query param)
onBeforeRouteUpdate(async (to, from, next) => {
  console.log('DEBUG: onBeforeRouteUpdate, isEditMode:', isEditMode.value, 'dirty:', editFormDirty.value)
  // 如果是从编辑模式离开 (即当前是编辑模式，目标不是)
  if (isEditMode.value && to.query.edit !== 'true' && editFormDirty.value) {
    try {
      await ElMessageBox.confirm('有未保存的修改，确定要离开吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      next()
    } catch (e) {
      next(false) // 阻止跳转
    }
  } else {
    next()
  }
})
const goBack = () => router.back()
const viewDetail = (id) => router.push(`/knowledge/${id}`)
const stringToColor = (str) => {
   let hash = 0;
   for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
   const c = (hash & 0x00FFFFFF).toString(16).toUpperCase();
   return '#' + '00000'.substring(0, 6 - c.length) + c;
}

const getStatusType = (status) => {
  const map = { 'DRAFT': 'info', 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { 'DRAFT': '草稿', 'PENDING': '待审核', 'APPROVED': '已发布', 'REJECTED': '已驳回' }
  return map[status] || status
}

const getRelationTypeTag = (type) => {
  const map = { 'RELATED': 'primary', 'CITED': 'success', 'SIMILAR': 'info' }
  return map[type] || 'default'
}

const getRelationTypeText = (type) => {
  const map = { 'RELATED': '相关', 'CITED': '引用', 'SIMILAR': '相似' }
  return map[type] || type
}

const loadVersions = async () => {
  try {
    // 传入用户信息实现版本权限过滤：普通用户只看到已发布版本+自己的草稿
    const username = userStore.userInfo?.username
    const admin = isAdmin(userStore.userInfo)
    
    const res = await api.get(`/knowledge/${route.params.id}/versions`, {
      params: { username, isAdmin: admin, _t: Date.now() }
    })
    versions.value = res.data || []
    // 调试：打印版本列表和每个版本的 fileId
    console.log('加载版本列表:', versions.value.map(v => ({ version: v.version, fileId: v.fileId, title: v.title, isPublished: v.isPublished })))
  } catch (error) {
    console.error('加载版本列表失败', error)
  }
}

// 切换到指定版本预览
const switchToVersion = async (version) => {
  if (currentViewingVersion.value === version.version) {
    return
  }
  
  try {
    // 从版本列表中获取版本数据
    const versionData = versions.value.find(v => v.version === version.version)
    console.log('switchToVersion - version:', version.version, 'versionData:', versionData)
    
    if (versionData) {
      // 设置当前查看的版本
      currentVersionContent.value = versionData
      currentViewingVersion.value = version.version
      
      // 使用版本的 fileId 加载文件信息
      const targetFileId = versionData.fileId
      console.log('切换到版本的 fileId:', targetFileId)
      
      if (targetFileId) {
        try {
          const fileRes = await api.get(`/file/${targetFileId}`)
          if (fileRes.code === 200) {
            fileInfo.value = fileRes.data
            previewTimestamp.value = Date.now()
            console.log('版本文件信息已加载:', fileRes.data.fileName)
          }
        } catch (e) {
          console.warn('加载版本文件失败', e)
        }
      } else {
        // 如果版本没有 fileId，使用知识的默认 fileId
        await loadFileInfo()
      }
      
      ElMessage.success(`已切换到版本 v${version.version}`)
    }
  } catch (e) {
    console.error('切换版本失败', e)
    ElMessage.error('切换版本失败')
  }
}

// 查看版本详情（弹窗）
const viewVersionDetails = (version) => {
  selectedVersion.value = version
  showVersionViewDialog.value = true
}

// 原来的 viewVersion 方法保留为别名
const viewVersion = (version) => {
  viewVersionDetails(version)
}

// 从对话框中切换到版本预览
const switchToVersionFromDialog = async (version) => {
  showVersionViewDialog.value = false
  await switchToVersion(version)
}

// 预览版本文件
const previewVersionFile = (version) => {
  if (!version?.fileId) {
    ElMessage.warning('该版本没有关联文件')
    return
  }
  // 使用 kkFileView 预览
  const backendBase = 'http://host.docker.internal:8080'
  const fullFileName = version.title || 'file.docx' // Fallback title
  const versionPrefix = `v${version.version}_`
  const fileDownloadUrl = `${backendBase}/api/file/download/${version.fileId}?fullfilename=${encodeURIComponent(versionPrefix + fullFileName)}&t=${Date.now()}`
  const kkBase = 'http://localhost:8012'
  const previewUrl = `${kkBase}/onlinePreview?url=${encodeURIComponent(btoa(fileDownloadUrl))}`
  window.open(previewUrl, '_blank')
}

// 下载版本文件
const downloadVersionFile = (version) => {
  if (!version?.fileId) {
    ElMessage.warning('该版本没有关联文件')
    return
  }
  window.open(`/api/file/download/${version.fileId}`, '_blank')
}

// 获取版本状态消息
const getVersionStatusMessage = (version) => {
  if (!version) return ''
  if (version.isPublished) return '这是当前发布的版本'
  if (version.status === 'PENDING') return '此版本正在等待审核，审核通过后将发布'
  if (version.status === 'REJECTED') return '此版本审核未通过，不会发布'
  return '此版本尚未发布，仅管理员和作者可见'
}

// 回退到指定版本（仅管理员）- 会删除该版本之后的所有历史记录
const revertToVersion = async (version) => {
  // 计算将要删除的版本数量
  const versionsToDelete = versions.value.filter(v => v.version > version.version).length
  
  try {
    await ElMessageBox.confirm(
      `确定要回退到版本 v${version.version} 吗？\n\n⚠️ 警告：这将永久删除 v${version.version} 之后的 ${versionsToDelete} 个版本历史记录，此操作不可恢复！`,
      '版本回退',
      {
        confirmButtonText: '确定回退',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: false
      }
    )
    
    const res = await api.post(`/knowledge/${route.params.id}/versions/${version.version}/revert`, null, {
      params: { operatorUsername: userStore.userInfo?.username }
    })
    
    if (res.code === 200) {
      ElMessage.success(`已成功回退到版本 v${version.version}，删除了 ${versionsToDelete} 个后续版本`)
      // 清除当前查看的版本状态
      currentViewingVersion.value = null
      currentVersionContent.value = null
      loadDetail()
      loadVersions() // 刷新版本列表，确保UI显示正确
    } else {
      ElMessage.error(res.message || '回退失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('版本回退失败', e)
      ElMessage.error(e.message || '版本回退失败')
    }
  }
}

const compareVersion = (version) => {
  // 跳转到独立的版本对比页面
  router.push({
    path: `/knowledge/${route.params.id}/diff`,
    query: { v1: version.version, v2: knowledge.value.version }
  })
}

const compareWithPrevious = async (version, index) => {
  // 与上一版本对比
  if (index < versions.value.length - 1) {
    const previousVersion = versions.value[index + 1]
    await doCompare(previousVersion.version, version.version)
  }
}

const doCompare = async (v1, v2) => {
  showCompareDialog.value = true
  compareLoading.value = true
  compareData.value = {
    version1: v1,
    version2: v2,
    hash1: null,
    hash2: null,
    diffLines: [],
    stats: null
  }
  
  try {
    const res = await api.get(`/knowledge/${route.params.id}/versions/compare`, {
      params: { version1: v1, version2: v2 }
    })
    
    if (res.code === 200 && res.data) {
      compareData.value = {
        ...compareData.value,
        diffLines: res.data.diffLines || [],
        stats: res.data.stats
      }
      
      // 获取版本hash信息
      const ver1 = versions.value.find(v => v.version === v1)
      const ver2 = versions.value.find(v => v.version === v2)
      compareData.value.hash1 = ver1?.commitHash
      compareData.value.hash2 = ver2?.commitHash
    }
  } catch (error) {
    console.error('版本对比失败', error)
    ElMessage.error('版本对比失败')
  } finally {
    compareLoading.value = false
  }
}

const getDiffLineClass = (type) => {
  const classMap = {
    'INSERT': 'diff-insert',
    'DELETE': 'diff-delete',
    'EQUAL': 'diff-equal'
  }
  return classMap[type] || ''
}

const getDiffLineSymbol = (type) => {
  const symbolMap = {
    'INSERT': '+',
    'DELETE': '-',
    'EQUAL': ' '
  }
  return symbolMap[type] || ' '
}

const replyToComment = (comment) => {
    newCommentContent.value = `回复 @${comment.userRealName || comment.userName}: `
}

// 处理页面可见性变化（从编辑器返回时刷新预览）
const handleVisibilityChange = async () => {
  if (!document.hidden) {
    console.log('页面重新获得焦点，开始轮询检查内容更新...')
    
    // 记录当前状态
    const currentFileIdStr = String(knowledge.value.fileId)
    const currentUpdateTime = knowledge.value.updateTime
    
    // 定义检查函数
    const checkUpdate = async () => {
      console.log('执行刷新检查...')
      try {
        await loadDetail()
        
        // 检查是否有变化
        const newFileIdStr = String(knowledge.value.fileId)
        const newUpdateTime = knowledge.value.updateTime
        
        if (newFileIdStr !== currentFileIdStr || newUpdateTime !== currentUpdateTime) {
          console.log('检测到内容更新！停止轮询')
          return true // 有变化
        }
      } catch (e) {
        console.warn('刷新检查失败', e)
      }
      return false // 无变化
    }
    
    // 立即执行一次
    if (await checkUpdate()) return
    
    // 设置轮询策略：1.5s, 3s, 5s 后再检查，应对只有Office回调延迟
    const delays = [1500, 3000, 5000]
    
    for (const delay of delays) {
      setTimeout(async () => {
        // 如果页面又不可见了，停止轮询
        if (document.hidden) return
        await checkUpdate()
      }, delay)
    }
  }
}

watch(() => route.params.id, loadDetail)

onMounted(() => {
    loadDetail()
    
    // Set watermark with current user name and date
    const userInfo = userStore.userInfo
    const userName = userInfo?.realName || userInfo?.username || 'User'
    const date = new Date().toLocaleDateString()
    watermark.set(`${userName} ${date}`)
    
    // 监听页面可见性变化，用于从编辑器返回后刷新预览
    document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
    // Remove watermark when leaving the page
    watermark.remove()
    // 移除可见性监听器
    document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.knowledge-detail-container {
  height: calc(100vh - 84px);
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

/* 草稿状态横幅 */
.draft-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  flex-shrink: 0;
}

.draft-banner.banner-info {
  background: linear-gradient(135deg, #fdf6ec 0%, #fef0e6 100%);
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.draft-banner.banner-draft {
  background: linear-gradient(135deg, #ecf5ff 0%, #e6f1fc 100%);
  color: #409eff;
  border: 1px solid #d9ecff;
}

.draft-banner.banner-history {
  background: linear-gradient(135deg, #f0f9eb 0%, #e8f5e1 100%);
  color: #67c23a;
  border: 1px solid #c2e7b0;
}

.draft-banner .el-icon {
  font-size: 18px;
}

.draft-banner span {
  flex: 1;
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
.back-btn {
  flex-shrink: 0;
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
  background: #525659;
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

/* AI Chat */
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 350px;
  max-height: calc(100vh - 280px);
}

.doc-context-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f7ff 100%);
  border-radius: 8px;
  margin-bottom: 12px;
  color: #409eff;
  font-size: 12px;
  flex-shrink: 0;
}

.doc-context-hint .el-icon {
  font-size: 14px;
}

.ai-welcome {
  text-align: center;
  padding: 20px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.welcome-icon {
  font-size: 40px;
  color: #409eff;
  margin-bottom: 12px;
}

.ai-welcome h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.ai-welcome p {
  font-size: 13px;
  color: #909399;
  margin: 0 0 16px 0;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 12px;
  min-height: 0;
}

.ai-messages::-webkit-scrollbar {
  width: 6px;
}

.ai-messages::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.ai-messages::-webkit-scrollbar-thumb:hover {
  background: #b0b5bd;
}

.ai-message {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
}

.ai-message.assistant .message-avatar {
  background: #ecf5ff;
  color: #409eff;
}

.ai-message.user .message-avatar {
  background: #f5f7fa;
  color: #606266;
}

.message-content {
  max-width: 80%;
}

.message-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
}

.ai-message.assistant .message-text {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.ai-message.user .message-text {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-text.streaming {
  border-left: 2px solid #409eff;
  animation: streamingPulse 1s ease-in-out infinite;
}

@keyframes streamingPulse {
  0%, 100% { border-left-color: #409eff; }
  50% { border-left-color: #66b1ff; }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 6px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #c0c4cc;
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.ai-input-area {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  flex-shrink: 0;
  background: white;
  margin-top: auto;
}

.ai-input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.ai-input-actions .hint {
  font-size: 12px;
  color: #c0c4cc;
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

/* Edit Mode Styles */
.edit-mode-header {
  border-bottom: 1px solid #ebeef5;
  box-shadow: none;
}

.immersive-title-editor {
  flex: 1;
  margin-right: 20px;
}

.title-input-immersive :deep(.el-input__wrapper) {
  padding: 0;
  box-shadow: none !important;
  background-color: transparent;
}

.title-input-immersive :deep(.el-input__inner) {
  height: 40px;
  line-height: 40px;
  color: #1f2329;
}

.unsaved-hint {
  font-size: 12px;
  color: #e6a23c;
  margin-right: 12px;
  display: flex;
  align-items: center;
}

.unsaved-hint::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #e6a23c;
  border-radius: 50%;
  margin-right: 6px;
}

/* Edit Canvas */
.left-panel.is-editing {
  background: transparent;
  box-shadow: none;
  overflow: visible;
}

.edit-canvas {
  height: 100%;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px; /* Space for shadow */
}

/* Edit Cards */
.edit-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  padding: 24px;
  transition: box-shadow 0.2s;
}

.edit-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edit-form-grid {
  /* Layout tweaks */
}

.edit-form-grid :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

/* Content Card Specifics */
.content-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 500px;
}

.content-editor-input {
  flex: 1;
}

.content-editor-input :deep(.el-textarea__inner) {
  height: 100%;
  padding: 16px;
  font-size: 15px;
  line-height: 1.6;
  border-color: #dcdfe6;
  border-radius: 4px;
  resize: none; /* Disable manual resize */
}

.content-editor-input :deep(.el-textarea__inner):focus {
  border-color: #409eff;
  background-color: #fcfcfc;
}

/* File Edit Placeholder */
.file-edit-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f9f9fa;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  margin-top: 10px;
  padding: 40px;
}

.file-icon-large {
  font-size: 64px;
  color: #409eff;
  margin-bottom: 16px;
}

.file-info-text {
  text-align: center;
  margin-bottom: 24px;
}

.file-info-text h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.file-info-text p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.file-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.file-info-tag {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Git Log Style History */
.history-panel {
  padding: 0;
}

.git-log {
  display: flex;
  flex-direction: column;
}

.commit-item {
  display: flex;
  padding: 12px 8px;
  border-radius: 8px;
  transition: background 0.2s;
  cursor: pointer;
}

.commit-item:hover {
  background: #f5f7fa;
}

.commit-item.is-current {
  background: #f0f9eb; /* Greenish for current/published */
}

.commit-item.is-viewing {
  background: #ecf5ff; /* Blue for viewing */
  border-left: 3px solid #409eff;
}

.commit-graph {
  position: relative;
  width: 24px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.commit-line {
  position: absolute;
  top: 20px;
  left: 50%;
  width: 2px;
  height: calc(100% + 12px);
  background: #dcdfe6;
  transform: translateX(-50%);
}

.commit-node {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #dcdfe6;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #dcdfe6;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.commit-node.is-current {
  width: 18px;
  height: 18px;
  background: #67c23a;
  box-shadow: 0 0 0 2px #67c23a;
}

.commit-node.is-current .el-icon {
  color: white;
  font-size: 14px;
}

.commit-content {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.commit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.commit-hash-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.commit-hash {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
  font-size: 12px;
  color: #606266;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.commit-time {
  font-size: 12px;
  color: #909399;
}

.commit-message {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 6px;
  line-height: 1.4;
}

.commit-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.commit-author {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 11px;
  font-weight: 600;
}

.commit-version {
  font-family: 'SF Mono', 'Monaco', monospace;
}

.commit-actions {
  display: flex;
  gap: 4px;
}

/* Version View Dialog */
.version-view-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

.version-status-banner {
  margin-bottom: 16px;
}

.version-info-header {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.version-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.version-info-item .label {
  font-size: 12px;
  color: #909399;
}

.version-info-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.version-info-item .value.commit-hash {
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 13px;
}

.version-message {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
}

.version-content-wrapper {
  max-height: 400px;
  overflow: auto;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
}

.version-content {
  margin: 0;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #d4d4d4;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Compare Dialog */
.compare-dialog .el-dialog__body {
  padding: 0 20px 20px;
}

.compare-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.compare-version {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compare-hash {
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 12px;
  color: #606266;
}

.compare-arrow {
  font-size: 24px;
  color: #909399;
}

.compare-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  justify-content: center;
}

.compare-stats .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.diff-container {
  max-height: 500px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #1e1e1e;
}

.diff-view {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.diff-line {
  display: flex;
  padding: 0;
  min-height: 24px;
}

.diff-line.diff-insert {
  background: rgba(46, 160, 67, 0.2);
}

.diff-line.diff-delete {
  background: rgba(248, 81, 73, 0.2);
}

.diff-line.diff-equal {
  background: transparent;
}

.line-number {
  width: 40px;
  padding: 0 8px;
  text-align: right;
  color: #6e7681;
  background: rgba(0, 0, 0, 0.2);
  user-select: none;
  flex-shrink: 0;
}

.line-number.old {
  border-right: 1px solid #30363d;
}

.line-type {
  width: 20px;
  text-align: center;
  flex-shrink: 0;
  font-weight: bold;
}

.diff-insert .line-type {
  color: #3fb950;
}

.diff-delete .line-type {
  color: #f85149;
}

.line-content {
  flex: 1;
  padding: 0 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #d4d4d4;
}

.diff-insert .line-content {
  color: #aff5b4;
}

.diff-delete .line-content {
  color: #ffa198;
}
</style>
