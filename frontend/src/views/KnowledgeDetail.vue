<template>
  <div class="knowledge-detail" v-loading="loading">
    <div v-if="knowledge">
      <div class="detail-header">
        <h1>{{ knowledge.title }}</h1>
        <el-button type="primary" @click="showVersionDialog = true">版本历史</el-button>
      </div>
      <div class="meta-info">
        <span>版本：v{{ knowledge.version }}</span>
        <span>作者：{{ knowledge.author }}</span>
        <span>分类：{{ knowledge.category }}</span>
        <span>点击量：{{ knowledge.clickCount }}</span>
        <span>收藏量：{{ knowledge.collectCount }}</span>
      </div>
      <div class="content">
        <p>{{ knowledge.content }}</p>
      </div>
      <div class="related-knowledge" v-if="relatedKnowledge.length > 0">
        <h3>相关知识</h3>
        <el-row :gutter="20">
          <el-col :span="8" v-for="item in relatedKnowledge" :key="item.id">
            <el-card @click="viewDetail(item.id)" style="cursor: pointer">
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 版本历史对话框 -->
    <el-dialog v-model="showVersionDialog" title="版本历史" width="80%" @close="handleVersionDialogClose">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="版本列表" name="list">
          <el-table :data="versions" style="width: 100%" v-loading="versionsLoading">
            <el-table-column prop="version" label="版本" width="80" />
            <el-table-column prop="changeDescription" label="变更说明" />
            <el-table-column prop="createdBy" label="创建人" width="120" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="250">
              <template #default="scope">
                <el-button size="small" @click="viewVersion(scope.row)">查看</el-button>
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="selectedVersionForCompare = scope.row; compareVersions(scope.row)"
                >
                  {{ selectedVersionForCompare?.version === scope.row.version ? '已选中' : '对比' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="版本对比" name="compare" v-if="compareResult">
          <div class="compare-header">
            <div class="version-select">
              <span>版本1：v{{ compareResult.version1 }}</span>
              <span style="margin: 0 20px">vs</span>
              <span>版本2：v{{ compareResult.version2 }}</span>
            </div>
            <div class="compare-stats" v-if="compareResult.stats">
              <span>新增：{{ compareResult.stats.insertCount }}行</span>
              <span>删除：{{ compareResult.stats.deleteCount }}行</span>
              <span>相同：{{ compareResult.stats.equalCount }}行</span>
            </div>
          </div>
          <div class="diff-content">
            <div 
              v-for="(line, index) in compareResult.diffLines" 
              :key="index"
              :class="['diff-line', `diff-${line.type.toLowerCase()}`]"
            >
              <span class="line-number">{{ line.lineNumber1 || '' }}</span>
              <span class="line-number">{{ line.lineNumber2 || '' }}</span>
              <span class="line-content">{{ line.content }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 版本查看对话框 -->
    <el-dialog v-model="showVersionViewDialog" :title="`版本 v${selectedVersion?.version}`" width="70%">
      <div v-if="selectedVersion">
        <div class="version-meta">
          <span>变更说明：{{ selectedVersion.changeDescription || '无' }}</span>
          <span>创建人：{{ selectedVersion.createdBy }}</span>
          <span>创建时间：{{ selectedVersion.createTime }}</span>
        </div>
        <div class="version-content">
          <pre>{{ selectedVersion.content }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const knowledge = ref(null)
const relatedKnowledge = ref([])
const loading = ref(false)

const showVersionDialog = ref(false)
const activeTab = ref('list')
const versions = ref([])
const versionsLoading = ref(false)
const selectedVersion = ref(null)
const showVersionViewDialog = ref(false)
const compareResult = ref(null)
const selectedVersionForCompare = ref(null)

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}`)
    knowledge.value = res.data
    
    // 加载相关知识
    const relatedRes = await api.get(`/knowledge/${route.params.id}/related`, {
      params: { limit: 6 }
    })
    relatedKnowledge.value = relatedRes.data || []
  } catch (error) {
    console.error('加载详情失败', error)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const loadVersions = async () => {
  versionsLoading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}/versions`)
    versions.value = res.data || []
  } catch (error) {
    console.error('加载版本列表失败', error)
    ElMessage.error('加载版本列表失败')
  } finally {
    versionsLoading.value = false
  }
}

const viewVersion = (version) => {
  selectedVersion.value = version
  showVersionViewDialog.value = true
}

const compareVersions = async (version) => {
  if (!selectedVersionForCompare.value) {
    selectedVersionForCompare.value = version
    ElMessage.info('请选择另一个版本进行对比')
    return
  }
  
  try {
    const res = await api.get(`/knowledge/${route.params.id}/versions/compare`, {
      params: {
        version1: selectedVersionForCompare.value.version,
        version2: version.version
      }
    })
    compareResult.value = res.data
    activeTab.value = 'compare'
  } catch (error) {
    console.error('对比版本失败', error)
    ElMessage.error('对比版本失败')
  }
}

const handleVersionDialogClose = () => {
  activeTab.value = 'list'
  compareResult.value = null
  selectedVersionForCompare.value = null
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const handleVersionDialogOpen = () => {
  loadVersions()
}

// 监听对话框打开
watch(showVersionDialog, (val) => {
  if (val) {
    handleVersionDialogOpen()
  }
})

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.knowledge-detail {
  padding: 20px;
}

.meta-info {
  margin: 20px 0;
  color: #666;
}

.meta-info span {
  margin-right: 20px;
}

.content {
  margin: 30px 0;
  line-height: 1.8;
}

.related-knowledge {
  margin-top: 40px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.version-meta {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.version-meta span {
  margin-right: 20px;
}

.version-content {
  margin-top: 20px;
}

.version-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 500px;
  overflow-y: auto;
}

.compare-header {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.version-select {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 500;
}

.compare-stats span {
  margin-right: 20px;
  color: #606266;
}

.diff-content {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 600px;
  overflow-y: auto;
}

.diff-line {
  display: flex;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.diff-line .line-number {
  display: inline-block;
  width: 50px;
  padding: 2px 8px;
  text-align: right;
  background-color: #f5f7fa;
  border-right: 1px solid #dcdfe6;
  color: #909399;
  user-select: none;
}

.diff-line .line-content {
  flex: 1;
  padding: 2px 8px;
  white-space: pre;
}

.diff-equal {
  background-color: #ffffff;
}

.diff-insert {
  background-color: #f0f9ff;
}

.diff-insert .line-content {
  background-color: #e1f3ff;
}

.diff-delete {
  background-color: #fef0f0;
}

.diff-delete .line-content {
  background-color: #fde2e2;
  text-decoration: line-through;
}
</style>

