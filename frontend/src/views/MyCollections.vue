<template>
  <div class="my-collections">
    <div class="page-header">
      <h2>我的收藏</h2>
    </div>
    
    <el-card>
      <el-table 
        :data="collections" 
        v-loading="collectionsLoading" 
        stripe
        @row-click="handleRowClick"
        class="collections-table"
      >
        <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="clickCount" label="点击量" width="100" align="right" />
        <el-table-column prop="collectCount" label="收藏量" width="100" align="right" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button 
              size="small" 
              type="danger" 
              @click.stop="cancelCollect(scope.row)"
            >
              取消收藏
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="collections.length === 0 && !collectionsLoading" style="text-align: center; padding: 40px; color: #909399">
        暂无收藏的知识
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()
const collections = ref([])
const collectionsLoading = ref(false)

// 加载收藏列表
const loadCollections = async () => {
  if (!userStore.userInfo || !userStore.userInfo.id) {
    return
  }
  
  collectionsLoading.value = true
  try {
    const res = await api.get('/knowledge/my/collections', {
      params: { userId: userStore.userInfo.id }
    })
    collections.value = res.data || []
  } catch (error) {
    console.error('加载收藏列表失败', error)
    ElMessage.error('加载收藏列表失败')
  } finally {
    collectionsLoading.value = false
  }
}

// 点击行查看详情
const handleRowClick = (row) => {
  router.push(`/knowledge/${row.id}`)
}

// 取消收藏
const cancelCollect = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏吗？', '提示', {
      type: 'warning'
    })
    
    const res = await api.delete(`/knowledge/${row.id}/collect`, {
      params: { userId: userStore.userInfo.id }
    })
    
    if (res.code === 200) {
      ElMessage.success('取消收藏成功')
      loadCollections() // 重新加载收藏列表
    } else {
      ElMessage.error(res.message || '取消收藏失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消收藏失败')
    }
  }
}

onMounted(() => {
  loadCollections()
})
</script>

<style scoped>
.my-collections {
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

.collections-table :deep(.el-table__row) {
  cursor: pointer;
}

.collections-table :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

:deep(.el-card) {
  border-radius: 4px;
}
</style>

