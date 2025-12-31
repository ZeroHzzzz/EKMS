import { createRouter, createWebHistory } from 'vue-router'
import KnowledgeList from '../views/KnowledgeList.vue'
import KnowledgeDetail from '../views/KnowledgeDetail.vue'
import Upload from '../views/Upload.vue'
import Audit from '../views/Audit.vue'
import Statistics from '../views/Statistics.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import UserManagement from '../views/UserManagement.vue'
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
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
  
  // 检查页面权限
  if (to.meta && to.meta.permission) {
    const userStore = useUserStore()
    const userInfo = userStore.userInfo
    
    // 如果用户信息不存在，说明可能还未加载，先放行，由具体页面组件处理权限
    if (!userInfo) {
      next()
      return
    }
    
    if (!hasPermission(userInfo, to.meta.permission)) {
      // 没有权限，重定向到首页
      next('/')
      return
    }
  }
  
  next()
})

export default router

