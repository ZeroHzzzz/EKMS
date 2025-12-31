import { createRouter, createWebHistory } from 'vue-router'
import KnowledgeList from '../views/KnowledgeList.vue'
import KnowledgeDetail from '../views/KnowledgeDetail.vue'
import Upload from '../views/Upload.vue'
import Audit from '../views/Audit.vue'
import Statistics from '../views/Statistics.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import UserManagement from '../views/UserManagement.vue'
import UserCenter from '../views/UserCenter.vue'
import { useUserStore } from '../stores/user'
import { hasPermission } from '../utils/permission'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/',
    name: 'KnowledgeList',
    component: KnowledgeList
  },
  {
    path: '/knowledge/:id',
    name: 'KnowledgeDetail',
    component: KnowledgeDetail
  },
  {
    path: '/upload',
    name: 'Upload',
    component: Upload,
    meta: { permission: 'UPLOAD' }
  },
  {
    path: '/audit',
    name: 'Audit',
    component: Audit,
    meta: { permission: 'AUDIT' }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: Statistics,
    meta: { permission: 'VIEW_STATISTICS' }
  },
  {
    path: '/user-management',
    name: 'UserManagement',
    component: UserManagement,
    meta: { permission: 'MANAGE_USER' }
  },
  {
    path: '/user-center',
    name: 'UserCenter',
    component: UserCenter
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 登录和注册页面不需要token
  if (to.path === '/login' || to.path === '/register') {
    if (token) {
      // 已登录，重定向到首页
      next('/')
    } else {
      next()
    }
    return
  }
  
  // 其他页面需要token
  if (!token) {
    next('/login')
    return
  }
  
  // 确保用户信息已加载
  const userStore = useUserStore()
  if (!userStore.userInfo) {
    // 尝试恢复用户信息
    await userStore.initUserInfo()
    // 如果恢复失败，清除token并跳转到登录页
    if (!userStore.userInfo) {
      next('/login')
      return
    }
  }
  
  // 检查页面权限
  if (to.meta && to.meta.permission) {
    const userInfo = userStore.userInfo
    if (!hasPermission(userInfo, to.meta.permission)) {
      // 没有权限，重定向到首页
      next('/')
      return
    }
  }
  
  next()
})

export default router

