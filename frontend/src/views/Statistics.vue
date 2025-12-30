<template>
  <div class="statistics-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalKnowledge }}</div>
            <div class="stat-label">知识总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalClicks }}</div>
            <div class="stat-label">总点击量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalCollections }}</div>
            <div class="stat-label">总收藏量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.pendingAudit }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>热点知识排行</span>
      </template>
      <el-table :data="hotKnowledge" v-loading="loading">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="clickCount" label="点击量" width="120" />
        <el-table-column prop="collectCount" label="收藏量" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const statistics = ref({
  totalKnowledge: 0,
  totalClicks: 0,
  totalCollections: 0,
  pendingAudit: 0
})

const hotKnowledge = ref([])
const loading = ref(false)

const loadStatistics = async () => {
  loading.value = true
  try {
    const res = await api.get('/knowledge/hot', { params: { limit: 10 } })
    hotKnowledge.value = res.data || []
    
    // 计算统计数据
    statistics.value.totalKnowledge = hotKnowledge.value.length
    statistics.value.totalClicks = hotKnowledge.value.reduce((sum, item) => sum + (item.clickCount || 0), 0)
    statistics.value.totalCollections = hotKnowledge.value.reduce((sum, item) => sum + (item.collectCount || 0), 0)
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-page {
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}
</style>

