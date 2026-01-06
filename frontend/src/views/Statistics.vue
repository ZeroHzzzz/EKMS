<template>
  <div class="statistics-page">
    <div class="page-header">
      <div class="header-left">
        <h2>统计分析</h2>
        <p class="subtitle">系统知识库数据概览</p>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleDateRangeChange"
          size="default"
        />
      </div>
    </div>
    
    <!-- 基础统计卡片 -->
    <el-row :gutter="24" class="stat-cards-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper icon-blue">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">知识总数</div>
              <div class="stat-value">{{ statistics.totalKnowledge }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper icon-purple">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总点击量</div>
              <div class="stat-value">{{ statistics.totalClicks }}</div>
              <div class="stat-sub">平均 {{ formatNumber(statistics.averageClickRate) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper icon-orange">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总收藏量</div>
              <div class="stat-value">{{ statistics.totalCollections }}</div>
              <div class="stat-sub">平均 {{ formatNumber(statistics.averageCollectRate) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper icon-red">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">待审核</div>
              <div class="stat-value">{{ statistics.pendingAudit }}</div>
              <div class="stat-sub">收藏率 {{ formatPercent(statistics.collectClickRatio) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图表 -->
    <el-row :gutter="24" class="charts-row">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>点击量趋势</span>
              <el-tag size="small" effect="plain" type="info">最近7天</el-tag>
            </div>
          </template>
          <div ref="clickTrendChart" style="width: 100%; height: 320px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>收藏量趋势</span>
              <el-tag size="small" effect="plain" type="info">最近7天</el-tag>
            </div>
          </template>
          <div ref="collectTrendChart" style="width: 100%; height: 320px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <span>热点知识排行</span>
          <el-tag size="small" type="danger" effect="light">TOP 10</el-tag>
        </div>
      </template>
      <el-table :data="hotKnowledge" v-loading="loading" :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column label="排名" width="80" align="center">
          <template #default="scope">
            <div class="rank-badge" :class="getRankClass(scope.$index + 1)">
              {{ scope.$index + 1 }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip>
          <template #default="scope">
            <span class="knowledge-title" @click="viewDetail(scope.row.id)">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Document, View, Star, Timer } from '@element-plus/icons-vue'
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
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return 'rank-other'
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
  const commonOption = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    }
  }

  // 点击量趋势图
  if (clickTrendChart.value && statistics.value.clickTrend) {
    if (!clickTrendChartInstance) {
      clickTrendChartInstance = echarts.init(clickTrendChart.value)
    }
    clickTrendChartInstance.setOption({
      ...commonOption,
      xAxis: {
        type: 'category',
        data: statistics.value.clickTrend.map(t => t.date),
        axisLine: { lineStyle: { color: '#ddd' } },
        axisLabel: { color: '#666' }
      },
      yAxis: { 
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }
      },
      series: [{
        data: statistics.value.clickTrend.map(t => t.value),
        type: 'line',
        smooth: true,
        symbolSize: 8,
        itemStyle: { color: '#409eff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0)' }
          ])
        }
      }]
    })
  }

  // 收藏量趋势图
  if (collectTrendChart.value && statistics.value.collectTrend) {
    if (!collectTrendChartInstance) {
      collectTrendChartInstance = echarts.init(collectTrendChart.value)
    }
    collectTrendChartInstance.setOption({
      ...commonOption,
      xAxis: {
        type: 'category',
        data: statistics.value.collectTrend.map(t => t.date),
        axisLine: { lineStyle: { color: '#ddd' } },
        axisLabel: { color: '#666' }
      },
      yAxis: { 
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }
      },
      series: [{
        data: statistics.value.collectTrend.map(t => t.value),
        type: 'line',
        smooth: true,
        symbolSize: 8,
        itemStyle: { color: '#67c23a', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0)' }
          ])
        }
      }]
    })
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
  loadStatistics()
}

// 窗口大小变化时重新调整图表
const handleResize = () => {
  clickTrendChartInstance?.resize()
  collectTrendChartInstance?.resize()
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  clickTrendChartInstance?.dispose()
  collectTrendChartInstance?.dispose()
})
</script>

<style scoped>
.statistics-page {
  padding: 24px;
  background-color: #f6f8fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2f3d;
}

.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.stat-cards-row {
  margin-bottom: 24px;
}

.stat-card {
  height: 100%;
  border: none;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 16px;
}

.icon-blue {
  background-color: #ecf5ff;
  color: #409eff;
}

.icon-purple {
  background-color: #f4f4f5;
  color: #909399; /* Adjust color if needed */
  background: linear-gradient(135deg, #e9e9eb, #ffffff);
  /* Changed purple to standard gray/neutral or keep consistent with Element Plus colors */
  background-color: #f0f2f5; 
  color: #606266;
}
/* Let's make it purple for distinction as designed */
.icon-purple {
  background-color: #f2f6fc;
  color: #79bbff;
}
.icon-purple {
  background-color: #ebeef5;
  color: #909399;
}
/* Re-deciding on colors */
.icon-blue { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.icon-purple { background: rgba(114, 46, 209, 0.1); color: #722ed1; } /* Actual Purple */
.icon-orange { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
.icon-red { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

/* Charts */
.charts-row {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-card {
  border: none;
}

/* Table */
.table-card {
  border: none;
}

.rank-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.rank-1 {
  background-color: #fff6e5;
  color: #ff9900;
}

.rank-2 {
  background-color: #f0f2f5;
  color: #909399; /* Silver-ish */
  background-color: #e6f6ff;
  color: #409eff;
}
/* Gold, Silver, Bronze */
.rank-1 { background-color: #feefda; color: #faad14; }
.rank-2 { background-color: #f0f2f5; color: #606266; } /* Standard grey for 2? Maybe clarify */
.rank-3 { background-color: #fde2e2; color: #f56c6c; } /* Bronze/Reddish */
.rank-other { color: #909399; }

/* Custom Overrides for Rank Colors */
.rank-1 { background: #ffe1b3; color: #d46b08; }
.rank-2 { background: #d9d9d9; color: #595959; }
.rank-3 { background: #ffccc7; color: #cf1322; }

.knowledge-title {
  color: #303133;
  cursor: pointer;
  transition: color 0.2s;
}

.knowledge-title:hover {
  color: #409eff;
}

.click-count { font-weight: 600; color: #409eff; }
.collect-count { font-weight: 600; color: #67c23a; }

</style>
