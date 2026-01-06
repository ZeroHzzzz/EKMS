<template>
  <div class="maintenance-container">
    <div class="dashboard-header">
      <div class="header-content">
        <h2 class="page-title">系统运维中心</h2>
        <div class="page-subtitle">集中监控与管理系统服务状态</div>
      </div>
      <div class="header-status">
        <div class="status-indicator" :class="statusClass">
          <div class="status-orb"></div>
          <span class="status-text">{{ statusText }}</span>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <el-row :gutter="24">
        <!-- Grafana -->
        <el-col :span="8" :xs="24" :sm="12" :md="8">
          <div class="service-card grafana-card" @click="openLink('http://localhost:3001/d/28e3358f-3fac-4321-804f-420e116de8a1/spring-boot-statistics?orgId=1&from=now-1h&to=now&timezone=browser&var-application=audit-service&var-instance=host.docker.internal:8084&var-hikaricp=&var-memory_pool_heap=$__all&var-memory_pool_nonheap=$__all')">
            <div class="card-content">
              <div class="icon-container">
                <el-icon><DataLine /></el-icon>
              </div>
              <div class="info-container">
                <h3>监控仪表盘</h3>
                <p>Grafana Dashboard</p>
                <span class="feature-tag">核心指标可视化</span>
              </div>
              <div class="action-icon">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </el-col>

        <!-- Nacos -->
        <el-col :span="8" :xs="24" :sm="12" :md="8">
          <div class="service-card nacos-card" @click="openLink('http://localhost:8848/nacos/#/serviceManagement?dataId=&group=&appName=&namespace=&namespaceShowName=public&serviceNameParam=&groupNameParam=')">
             <div class="card-content">
              <div class="icon-container">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="info-container">
                <h3>服务注册中心</h3>
                <p>Nacos Registry</p>
                <span class="feature-tag">微服务与配置管理</span>
              </div>
              <div class="action-icon">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </el-col>

        <!-- Prometheus -->
        <el-col :span="8" :xs="24" :sm="12" :md="8">
          <div class="service-card prometheus-card" @click="openLink('http://localhost:9090/targets')">
            <div class="card-content">
              <div class="icon-container">
                <el-icon><Odometer /></el-icon>
              </div>
               <div class="info-container">
                <h3>指标采集</h3>
                <p>Prometheus Metrics</p>
                <span class="feature-tag">原始数据查询与调试</span>
              </div>
              <div class="action-icon">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { DataLine, Connection, Odometer, ArrowRight } from '@element-plus/icons-vue'
import axios from 'axios'

const loading = ref(true)
const systemStatus = ref('UNKNOWN')

const statusText = computed(() => {
  if (loading.value) return '正在检测系统状态...'
  if (systemStatus.value === 'UP') return '所有系统运行正常'
  return '系统主要服务异常'
})

const statusClass = computed(() => {
  if (loading.value) return 'status-loading'
  if (systemStatus.value === 'UP') return 'status-healthy'
  return 'status-error'
})

const checkHealth = async () => {
  loading.value = true
  try {
    const response = await axios.get('http://localhost:8080/actuator/health')
    if (response.data && response.data.status === 'UP') {
      systemStatus.value = 'UP'
    } else {
      systemStatus.value = 'DOWN'
    }
  } catch (error) {
    console.error('Health check failed:', error)
    systemStatus.value = 'DOWN'
  } finally {
    loading.value = false
  }
}

const openLink = (url) => {
  window.open(url, '_blank')
}

onMounted(() => {
  checkHealth()
  setInterval(checkHealth, 30000)
})
</script>

<style scoped>
.maintenance-container {
  padding: 32px;
  max-width: 1600px;
  margin: 0 auto;
}

/* Header Styles */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  background: white;
  padding: 30px 40px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid #f0f2f5;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 15px;
  color: #6b7280;
}

/* Status Indicator */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  background: #f8fafc;
  border-radius: 30px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.status-orb {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  position: relative;
}

.status-orb::after {
  content: '';
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  opacity: 0.4;
  animation: pulse 2s infinite;
}

.status-text {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

/* Status Variants */
.status-healthy .status-orb {
  background-color: #10b981;
}
.status-healthy .status-orb::after {
  background-color: #10b981;
}
.status-healthy .status-text {
  color: #059669;
}
.status-healthy {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.status-error .status-orb {
  background-color: #ef4444;
}
.status-error .status-orb::after {
  background-color: #ef4444;
}
.status-error .status-text {
  color: #b91c1c;
}
.status-error {
  background: #fef2f2;
  border-color: #fecaca;
}

.status-loading .status-orb {
  background-color: #cbd5e1;
}
.status-loading .status-orb::after {
  background-color: #94a3b8;
}

@keyframes pulse {
  0% { transform: scale(0.8); opacity: 0.5; }
  100% { transform: scale(1.5); opacity: 0; }
}

/* Card Styles */
.service-card {
  background: white;
  border-radius: 16px;
  padding: 30px;
  cursor: pointer;
  border: 1px solid #f0f2f5;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  height: 200px;
  display: flex;
  align-items: center;
}

.service-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.08);
  border-color: transparent;
}

.card-content {
  display: flex;
  align-items: flex-start;
  width: 100%;
  gap: 24px;
  position: relative;
  z-index: 2;
}

.icon-container {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.service-card:hover .icon-container {
  transform: scale(1.1) rotate(5deg);
}

.info-container h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.info-container p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0 0 16px 0;
  font-weight: 500;
}

.feature-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
}

.action-icon {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: all 0.3s ease;
}

.service-card:hover .action-icon {
  opacity: 1;
  right: -10px;
}

/* Theme Variants */
.grafana-card .icon-container {
  background: #fff7ed;
  color: #f97316;
}
.grafana-card:hover {
  border-top: 4px solid #f97316;
}

.nacos-card .icon-container {
  background: #eff6ff;
  color: #3b82f6;
}
.nacos-card:hover {
  border-top: 4px solid #3b82f6;
}

.prometheus-card .icon-container {
  background: #fef2f2;
  color: #ef4444;
}
.prometheus-card:hover {
  border-top: 4px solid #ef4444;
}
</style>
