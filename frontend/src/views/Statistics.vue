<template>
  <div class="statistics-page">
    <div class="page-header">
      <h2>统计分析</h2>
    </div>
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

    <el-card style="margin-top: 20px" class="table-card">
      <template #header>
        <span>热点知识排行（按点击量）</span>
      </template>
      <el-table :data="hotKnowledge" v-loading="loading" stripe>
        <el-table-column label="排名" width="80" align="center">
          <template #default="scope">
            <span :class="getRankClass(scope.$index + 1)">
              {{ scope.$index + 1 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="clickCount" label="点击量" width="120" align="right" sortable>
          <template #default="scope">
            <span class="click-count">{{ scope.row.clickCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="collectCount" label="收藏量" width="120" align="right" sortable>
          <template #default="scope">
            <span class="collect-count">{{ scope.row.collectCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="viewDetail(scope.row.id)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const statistics = ref({
  totalKnowledge: 0,
  totalClicks: 0,
  totalCollections: 0,
  pendingAudit: 0
})

const hotKnowledge = ref([])
const loading = ref(false)
const router = useRouter()

// 获取排名样式类
const getRankClass = (rank) => {
  if (rank === 1) return 'rank-first'
  if (rank === 2) return 'rank-second'
  if (rank === 3) return 'rank-third'
  return 'rank-normal'
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const loadStatistics = async () => {
  loading.value = true
  try {
    // 获取统计数据
    const statsRes = await api.get('/knowledge/statistics')
    if (statsRes.data) {
      statistics.value.totalKnowledge = statsRes.data.totalKnowledge || 0
      statistics.value.totalClicks = statsRes.data.totalClicks || 0
      statistics.value.totalCollections = statsRes.data.totalCollections || 0
      statistics.value.pendingAudit = statsRes.data.pendingAudit || 0
    }
    
    // 获取热门知识
    const hotRes = await api.get('/knowledge/hot', { params: { limit: 10 } })
    hotKnowledge.value = hotRes.data || []
    console.log('热门知识数据:', hotKnowledge.value)
  } catch (error) {
    console.error('加载统计数据失败', error)
    ElMessage.error('加载统计数据失败: ' + (error.message || '未知错误'))
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
  padding: 24px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.table-card {
  border-radius: 4px;
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

/* 排名样式 */
.rank-first {
  display: inline-block;
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  background: linear-gradient(135deg, #ffd700, #ffed4e);
  color: #fff;
  font-weight: bold;
  border-radius: 50%;
  font-size: 16px;
}

.rank-second {
  display: inline-block;
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
  color: #fff;
  font-weight: bold;
  border-radius: 50%;
  font-size: 16px;
}

.rank-third {
  display: inline-block;
  width: 32px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  background: linear-gradient(135deg, #cd7f32, #e6a857);
  color: #fff;
  font-weight: bold;
  border-radius: 50%;
  font-size: 16px;
}

.rank-normal {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  background: #f5f7fa;
  color: #606266;
  font-weight: 500;
  border-radius: 50%;
  font-size: 14px;
}

.click-count {
  color: #409eff;
  font-weight: 600;
}

.collect-count {
  color: #67c23a;
  font-weight: 600;
}
</style>

