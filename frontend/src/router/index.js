import { createRouter, createWebHistory } from 'vue-router'
import KnowledgeList from '../views/KnowledgeList.vue'
import KnowledgeDetail from '../views/KnowledgeDetail.vue'
import Upload from '../views/Upload.vue'
import Audit from '../views/Audit.vue'
import Statistics from '../views/Statistics.vue'
import Login from '../views/Login.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
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
    component: Upload
  },
  {
    path: '/audit',
    name: 'Audit',
    component: Audit
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: Statistics
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router

