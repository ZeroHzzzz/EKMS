<template>
  <!-- 登录和注册页面不显示布局，直接显示router-view -->
  <template v-if="isAuthPage">
    <router-view />
  </template>
  
  <!-- 主应用布局（包含header和sidebar） -->
  <el-container v-else>
    <el-header>
      <div class="header-content">
        <h1>企业知识库管理系统</h1>
        <div class="user-info">
          <el-dropdown>
            <span class="el-dropdown-link">
              {{ userInfo?.realName || '未登录' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToUserCenter">个人中心</el-dropdown-item>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu
          :default-active="activeMenu"
          router
          class="el-menu-vertical"
        >
          <el-menu-item index="/">
            <el-icon><Document /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'UPLOAD')" index="/upload">
            <el-icon><Upload /></el-icon>
            <span>上传文档</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'AUDIT')" index="/audit">
            <el-icon><Edit /></el-icon>
            <span>审核管理</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'VIEW_STATISTICS')" index="/statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>统计分析</span>
          </el-menu-item>
          <el-menu-item v-if="hasPermission(userInfo, 'MANAGE_USER')" index="/user-management">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
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

const goToUserCenter = () => {
  router.push('/user-center')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
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
}

.user-info {
  color: #333;
}

.el-menu-vertical {
  height: 100%;
}
</style>

