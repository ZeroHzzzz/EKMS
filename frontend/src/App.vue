<template>
  <!-- 登录和注册页面不显示布局，直接显示router-view -->
  <template v-if="isAuthPage">
    <router-view />
  </template>
  
  <!-- 主应用布局（包含header和sidebar） -->
  <el-container v-else class="app-container">
    <el-header class="app-header">
      <div class="header-content">
        <h1>企业知识库管理系统</h1>
        <div class="user-info">
          <el-dropdown trigger="click" placement="bottom-end">
            <div class="user-dropdown-trigger">
              <el-icon class="user-icon"><UserFilled /></el-icon>
              <span class="user-name">{{ userInfo?.realName || '未登录' }}</span>
              <el-icon class="dropdown-icon"><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">
                  <el-icon><Close /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <el-container class="app-body">
      <el-aside width="220px" class="app-sidebar">
        <el-menu
          :default-active="activeMenu"
          router
          class="el-menu-vertical"
        >
          <el-menu-item index="/">
            <el-icon><Document /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'AUDIT')" index="/audit">
            <el-icon><Edit /></el-icon>
            <span>审核管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'VIEW_STATISTICS')" index="/statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>统计分析</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'MANAGE_USER')" index="/organization">
            <el-icon><OfficeBuilding /></el-icon>
            <span>组织管理</span>
          </el-menu-item>
                <el-menu-item v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE')" index="/knowledge-tree">
                  <el-icon><FolderOpened /></el-icon>
                  <span>知识结构</span>
                </el-menu-item>
                <el-divider />
          <el-menu-item index="/user-center">
            <el-icon><UserFilled /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <el-menu-item index="/my-collections">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './stores/user'
import { hasPermission } from './utils/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const userInfo = computed(() => userStore.userInfo)

// 判断是否是认证页面（登录或注册）
const isAuthPage = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  height: 100vh;
  overflow: hidden;
}

.app-header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  height: 60px !important;
  line-height: 60px;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

h1 {
  margin: 0;
  font-size: 20px;
  color: #409eff;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  color: #303133;
  font-size: 14px;
}

.user-dropdown-trigger:hover {
  background-color: #f5f7fa;
}

.user-icon {
  font-size: 18px;
  color: #409eff;
}

.user-name {
  font-weight: 500;
  color: #303133;
}

.dropdown-icon {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.user-dropdown-trigger:hover .dropdown-icon {
  color: #606266;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-body {
  flex: 1;
  overflow: hidden;
  display: flex;
}

.app-sidebar {
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  overflow-x: hidden;
  flex-shrink: 0;
}

:deep(.el-menu-vertical) {
  height: 100%;
  border-right: none;
}

:deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: 4px;
}

:deep(.el-menu-item:hover) {
  background-color: #ecf5ff;
}

:deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
  color: #409eff;
}

:deep(.el-divider) {
  margin: 8px 0;
}

.app-main {
  background-color: #f5f7fa;
  padding: 0;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
}

/* 自定义滚动条样式 */
.app-sidebar::-webkit-scrollbar,
.app-main::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.app-sidebar::-webkit-scrollbar-track,
.app-main::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.app-sidebar::-webkit-scrollbar-thumb,
.app-main::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.app-sidebar::-webkit-scrollbar-thumb:hover,
.app-main::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

