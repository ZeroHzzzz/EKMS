<template>
  <div class="merge-review-page">
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div class="title-section">
          <h2>合并审核</h2>
          <p class="subtitle">解决版本冲突并发布</p>
        </div>
      </div>
      <div class="header-right">
        <el-tag v-if="hasConflict" type="warning" effect="dark">
          <el-icon><Warning /></el-icon> {{ conflictCount }} 处冲突
        </el-tag>
        <el-tag v-else type="success" effect="dark">
          <el-icon><Check /></el-icon> 可自动合并
        </el-tag>
      </div>
    </div>

    <!-- 版本信息 -->
    <div class="version-info-bar" v-loading="loading">
      <div class="version-card base">
        <div class="card-label">基础版本</div>
        <div class="card-version">v{{ mergePreview?.baseVersion || '?' }}</div>
      </div>
      <el-icon class="arrow"><ArrowRight /></el-icon>
      <div class="version-card current">
        <div class="card-label">当前发布</div>
        <div class="card-version">v{{ mergePreview?.currentVersion || '?' }}</div>
        <div class="card-author">{{ mergePreview?.currentVersionAuthor }}</div>
      </div>
      <el-icon class="arrow"><ArrowRight /></el-icon>
      <div class="version-card draft">
        <div class="card-label">待合并草稿</div>
        <div class="card-version">v{{ mergePreview?.draftVersion || '?' }}</div>
        <div class="card-author">{{ mergePreview?.draftAuthor }}</div>
      </div>
    </div>

    <!-- 合并块列表 -->
    <div class="merge-blocks" v-loading="loading">
      <div 
        v-for="block in mergePreview?.blocks" 
        :key="block.blockId"
        class="merge-block"
        :class="block.type.toLowerCase()"
      >
        <!-- 相同内容块 -->
        <template v-if="block.type === 'EQUAL'">
          <div class="block-content equal">
            <pre>{{ block.autoMergedContent }}</pre>
          </div>
        </template>

        <!-- 仅当前版本修改 -->
        <template v-else-if="block.type === 'CURRENT_ONLY'">
          <div class="block-header">
            <el-tag size="small" type="info">仅当前版本修改</el-tag>
          </div>
          <div class="block-content current-only">
            <div class="diff-section">
              <div class="diff-line delete" v-if="block.baseContent">
                <span class="prefix">-</span>
                <pre>{{ block.baseContent }}</pre>
              </div>
              <div class="diff-line insert">
                <span class="prefix">+</span>
                <pre>{{ block.currentContent }}</pre>
              </div>
            </div>
          </div>
          <div class="block-actions">
            <el-radio-group v-model="resolutions[block.blockId]" size="small">
              <el-radio-button label="CURRENT">接受修改</el-radio-button>
              <el-radio-button label="DRAFT">使用草稿版本</el-radio-button>
            </el-radio-group>
          </div>
        </template>

        <!-- 仅草稿修改 -->
        <template v-else-if="block.type === 'DRAFT_ONLY'">
          <div class="block-header">
            <el-tag size="small" type="success">草稿新增修改</el-tag>
          </div>
          <div class="block-content draft-only">
            <div class="diff-section">
              <div class="diff-line delete" v-if="block.baseContent">
                <span class="prefix">-</span>
                <pre>{{ block.baseContent }}</pre>
              </div>
              <div class="diff-line insert">
                <span class="prefix">+</span>
                <pre>{{ block.draftContent }}</pre>
              </div>
            </div>
          </div>
          <div class="block-actions">
            <el-radio-group v-model="resolutions[block.blockId]" size="small">
              <el-radio-button label="DRAFT">接受修改</el-radio-button>
              <el-radio-button label="CURRENT">使用当前版本</el-radio-button>
            </el-radio-group>
          </div>
        </template>

        <!-- 冲突块 -->
        <template v-else-if="block.type === 'CONFLICT'">
          <div class="block-header conflict">
            <el-icon><Warning /></el-icon>
            <span>冲突：两个版本都修改了此区域</span>
          </div>
          
          <div class="conflict-comparison">
            <div class="conflict-side current">
              <div class="side-header">
                <el-tag size="small" type="info">当前版本</el-tag>
                <span class="author">{{ mergePreview?.currentVersionAuthor }}</span>
              </div>
              <pre>{{ block.currentContent }}</pre>
            </div>
            <div class="conflict-divider"></div>
            <div class="conflict-side draft">
              <div class="side-header">
                <el-tag size="small" type="success">草稿版本</el-tag>
                <span class="author">{{ mergePreview?.draftAuthor }}</span>
              </div>
              <pre>{{ block.draftContent }}</pre>
            </div>
          </div>

          <div class="block-actions">
            <el-radio-group v-model="resolutions[block.blockId]" size="small">
              <el-radio-button label="CURRENT">使用当前版本</el-radio-button>
              <el-radio-button label="DRAFT">使用草稿版本</el-radio-button>
              <el-radio-button label="BOTH">两者都保留</el-radio-button>
              <el-radio-button label="CUSTOM">自定义</el-radio-button>
            </el-radio-group>
            
            <el-input
              v-if="resolutions[block.blockId] === 'CUSTOM'"
              type="textarea"
              v-model="customContents[block.blockId]"
              :rows="4"
              placeholder="输入自定义合并内容..."
              class="custom-input"
            />
          </div>
        </template>
      </div>

      <el-empty v-if="!loading && (!mergePreview?.blocks || mergePreview.blocks.length === 0)" 
                description="无内容差异" />
    </div>

    <!-- 底部操作栏 -->
    <div class="merge-footer">
      <el-input
        v-model="commitMessage"
        placeholder="合并说明（如：合并用户修改）"
        style="width: 400px"
      />
      <div class="footer-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="warning" @click="forcePublish" v-if="hasConflict && !allResolved">
          强制发布草稿版本
        </el-button>
        <el-button type="primary" @click="submitMerge" :disabled="hasConflict && !allResolved">
          {{ hasConflict ? '解决冲突并发布' : '确认合并并发布' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Warning, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()

const knowledgeId = ref(route.params.id)
const draftVersion = ref(route.query.draftVersion)
const loading = ref(false)
const mergePreview = ref(null)
const resolutions = reactive({})
const customContents = reactive({})
const commitMessage = ref('')

const hasConflict = computed(() => mergePreview.value?.hasConflict || false)
const conflictCount = computed(() => mergePreview.value?.conflictBlockCount || 0)

const allResolved = computed(() => {
  if (!mergePreview.value?.blocks) return true
  for (const block of mergePreview.value.blocks) {
    if (block.type === 'CONFLICT') {
      const resolution = resolutions[block.blockId]
      if (!resolution) return false
      if (resolution === 'CUSTOM' && !customContents[block.blockId]) return false
    }
  }
  return true
})

const goBack = () => router.back()

const loadMergePreview = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${knowledgeId.value}/merge/preview`, {
      params: { draftVersion: draftVersion.value }
    })
    if (res.code === 200) {
      mergePreview.value = res.data
      // 初始化默认选择
      for (const block of res.data.blocks || []) {
        if (block.type === 'CURRENT_ONLY') {
          resolutions[block.blockId] = 'CURRENT'
        } else if (block.type === 'DRAFT_ONLY') {
          resolutions[block.blockId] = 'DRAFT'
        }
      }
    } else {
      ElMessage.error(res.message || '加载合并预览失败')
    }
  } catch (e) {
    console.error('加载合并预览失败', e)
    ElMessage.error('加载合并预览失败')
  } finally {
    loading.value = false
  }
}

const submitMerge = async () => {
  if (hasConflict.value && !allResolved.value) {
    ElMessage.warning('请解决所有冲突后再提交')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要合并并发布此版本吗？',
      '确认合并',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )

    loading.value = true
    
    const resolutionList = []
    for (const blockId in resolutions) {
      resolutionList.push({
        blockId: parseInt(blockId),
        choice: resolutions[blockId],
        customContent: customContents[blockId] || null
      })
    }

    const res = await api.post(`/knowledge/${knowledgeId.value}/merge/resolve`, {
      baseVersion: mergePreview.value?.baseVersion,
      currentVersion: mergePreview.value?.currentVersion,
      draftVersion: draftVersion.value,
      resolutions: resolutionList,
      commitMessage: commitMessage.value || '合并版本',
      forceOverwrite: false
    })

    if (res.code === 200) {
      ElMessage.success('合并成功！')
      router.push(`/knowledge/${knowledgeId.value}`)
    } else {
      ElMessage.error(res.message || '合并失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('合并失败', e)
      ElMessage.error('合并失败: ' + (e.message || e))
    }
  } finally {
    loading.value = false
  }
}

const forcePublish = async () => {
  try {
    await ElMessageBox.confirm(
      '强制发布将覆盖当前版本的所有修改，只保留草稿内容。确定继续吗？',
      '警告',
      { confirmButtonText: '强制发布', cancelButtonText: '取消', type: 'warning' }
    )

    loading.value = true
    
    const res = await api.post(`/knowledge/${knowledgeId.value}/versions/${draftVersion.value}/publish`, null, {
      params: { force: true }
    })

    if (res.code === 200) {
      ElMessage.success('发布成功！')
      router.push(`/knowledge/${knowledgeId.value}`)
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('发布失败', e)
      ElMessage.error('发布失败: ' + (e.message || e))
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!draftVersion.value) {
    ElMessage.error('缺少草稿版本参数')
    router.back()
    return
  }
  loadMergePreview()
})
</script>

<style scoped>
.merge-review-page {
  padding: 24px 32px;
  background: #f8fafc;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 20px 28px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  font-size: 14px;
  color: #6b7280;
}

.back-btn:hover {
  color: #3b82f6;
}

.title-section h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.title-section .subtitle {
  margin: 4px 0 0;
  color: #9ca3af;
  font-size: 13px;
}

/* 版本信息栏 */
.version-info-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.version-card {
  padding: 16px 24px;
  border-radius: 10px;
  text-align: center;
  min-width: 140px;
}

.version-card.base {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border: 1px solid #d1d5db;
}

.version-card.current {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border: 1px solid #93c5fd;
}

.version-card.draft {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border: 1px solid #6ee7b7;
}

.card-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.card-version {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.card-author {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

.arrow {
  font-size: 20px;
  color: #9ca3af;
}

/* 合并块样式 */
.merge-blocks {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 80px;
}

.merge-block {
  border-bottom: 1px solid #e5e7eb;
  padding: 0;
}

.merge-block:last-child {
  border-bottom: none;
}

.block-header {
  padding: 12px 20px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 8px;
}

.block-header.conflict {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  font-weight: 500;
}

.block-content {
  padding: 16px 20px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.block-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.block-content.equal {
  background: #fff;
  color: #374151;
}

.diff-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.diff-line {
  display: flex;
  padding: 4px 12px;
  border-radius: 4px;
}

.diff-line .prefix {
  width: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.diff-line.delete {
  background: rgba(254, 202, 202, 0.4);
  color: #991b1b;
}

.diff-line.insert {
  background: rgba(187, 247, 208, 0.4);
  color: #065f46;
}

/* 冲突对比 */
.conflict-comparison {
  display: flex;
  gap: 0;
}

.conflict-side {
  flex: 1;
  padding: 16px 20px;
}

.conflict-side.current {
  background: rgba(219, 234, 254, 0.3);
  border-right: 2px solid #3b82f6;
}

.conflict-side.draft {
  background: rgba(209, 250, 229, 0.3);
  border-left: 2px solid #10b981;
}

.side-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.side-header .author {
  font-size: 12px;
  color: #6b7280;
}

.conflict-side pre {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.conflict-divider {
  width: 2px;
  background: #e5e7eb;
}

/* 块操作 */
.block-actions {
  padding: 12px 20px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.custom-input {
  margin-top: 8px;
}

/* 底部操作栏 */
.merge-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.footer-actions {
  display: flex;
  gap: 12px;
}
</style>
