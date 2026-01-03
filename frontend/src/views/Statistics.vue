<template>
  <div class="statistics-page">
    <div class="page-header">
      <h2>统计分析</h2>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="handleDateRangeChange"
      />
    </div>
    
    <!-- 基础统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
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
            <div class="stat-detail">平均: {{ formatNumber(statistics.averageClickRate) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.totalCollections }}</div>
            <div class="stat-label">总收藏量</div>
            <div class="stat-detail">平均: {{ formatNumber(statistics.averageCollectRate) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ statistics.pendingAudit }}</div>
            <div class="stat-label">待审核</div>
            <div class="stat-detail">收藏率: {{ formatPercent(statistics.collectClickRatio) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图表 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>点击量趋势（最近7天）</span>
          </template>
          <div ref="clickTrendChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>收藏量趋势（最近7天）</span>
          </template>
          <div ref="collectTrendChart" style="width: 100%; height: 300px;"></div>
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
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '../api'

const statistics = ref({
  totalKnowledge: 0,
  totalClicks: 0,
  totalCollections: 0,
  pendingAudit: 0,
  averageClickRate: 0,
  averageCollectRate: 0,
  collectClickRatio: 0,
  clickTrend: [],
  collectTrend: [],
  hotKnowledge: []
})

const hotKnowledge = ref([])
const loading = ref(false)
const router = useRouter()
const dateRange = ref(null)

// 图表引用
const clickTrendChart = ref(null)
const collectTrendChart = ref(null)

let clickTrendChartInstance = null
let collectTrendChartInstance = null

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
      statistics.value = {
        totalKnowledge: statsRes.data.totalKnowledge || 0,
        totalClicks: statsRes.data.totalClicks || 0,
        totalCollections: statsRes.data.totalCollections || 0,
        pendingAudit: statsRes.data.pendingAudit || 0,
        averageClickRate: statsRes.data.averageClickRate || 0,
        averageCollectRate: statsRes.data.averageCollectRate || 0,
        collectClickRatio: statsRes.data.collectClickRatio || 0,
        clickTrend: statsRes.data.clickTrend || [],
        collectTrend: statsRes.data.collectTrend || [],
        hotKnowledge: statsRes.data.hotKnowledge || []
      }
      hotKnowledge.value = statistics.value.hotKnowledge
    }
    
    // 渲染图表
    await nextTick()
    renderCharts()
  } catch (error) {
    console.error('加载统计数据失败', error)
    ElMessage.error('加载统计数据失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderCharts = () => {
  // 点击量趋势图
  if (clickTrendChart.value && statistics.value.clickTrend) {
    if (!clickTrendChartInstance) {
      clickTrendChartInstance = echarts.init(clickTrendChart.value)
    }
    const option = {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: statistics.value.clickTrend.map(t => t.date)
      },
      yAxis: { type: 'value' },
      series: [{
        data: statistics.value.clickTrend.map(t => t.value),
        type: 'line',
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#409eff' }
      }]
    }
    clickTrendChartInstance.setOption(option)
  }

  // 收藏量趋势图
  if (collectTrendChart.value && statistics.value.collectTrend) {
    if (!collectTrendChartInstance) {
      collectTrendChartInstance = echarts.init(collectTrendChart.value)
    }
    const option = {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: statistics.value.collectTrend.map(t => t.date)
      },
      yAxis: { type: 'value' },
      series: [{
        data: statistics.value.collectTrend.map(t => t.value),
        type: 'line',
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#67c23a' }
      }]
    }
    collectTrendChartInstance.setOption(option)
  }
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0.00'
  return num.toFixed(2)
}

// 格式化百分比
const formatPercent = (num) => {
  if (!num) return '0.00%'
  return (num * 100).toFixed(2) + '%'
}

// 日期范围变化
const handleDateRangeChange = () => {
  // 可以在这里实现按日期范围筛选统计
  loadStatistics()
}

// 窗口大小变化时重新调整图表
const handleResize = () => {
  if (clickTrendChartInstance) clickTrendChartInstance.resize()
  if (collectTrendChartInstance) collectTrendChartInstance.resize()
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (clickTrendChartInstance) clickTrendChartInstance.dispose()
  if (collectTrendChartInstance) collectTrendChartInstance.dispose()
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

.stat-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>

