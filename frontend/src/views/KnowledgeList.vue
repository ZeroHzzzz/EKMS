<template>
  <div class="knowledge-list">
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入关键词搜索（支持全文、拼音、首字母）"
        class="search-input"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      <el-select v-model="searchType" style="width: 150px; margin-left: 10px">
        <el-option label="全文搜索" value="FULL_TEXT" />
        <el-option label="拼音搜索" value="PINYIN" />
        <el-option label="首字母" value="INITIAL" />
      </el-select>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.category" placeholder="分类" clearable style="width: 150px">
        <el-option label="技术文档" value="技术文档" />
        <el-option label="业务文档" value="业务文档" />
        <el-option label="培训资料" value="培训资料" />
      </el-select>
      <el-select 
        v-if="hasPermission(userInfo, 'VIEW_AUDIT')" 
        v-model="filters.status" 
        placeholder="状态" 
        clearable 
        style="width: 150px; margin-left: 10px"
      >
        <el-option label="已发布" value="APPROVED" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
      <!-- 普通用户默认只显示已发布的知识 -->
      <el-select 
        v-else
        v-model="filters.status" 
        placeholder="状态" 
        style="width: 150px; margin-left: 10px"
        disabled
      >
        <el-option label="已发布" value="APPROVED" />
      </el-select>
    </div>

    <el-table :data="knowledgeList" style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="标题" width="300" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="clickCount" label="点击量" width="100" />
      <el-table-column prop="collectCount" label="收藏量" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" :width="getActionColumnWidth()">
        <template #default="scope">
          <el-button size="small" @click="viewDetail(scope.row.id)">查看</el-button>
          <el-button size="small" type="primary" @click="collect(scope.row)">收藏</el-button>
          <el-button 
            v-if="canEdit(scope.row)" 
            size="small" 
            type="warning" 
            @click="editKnowledge(scope.row)"
          >
            编辑
          </el-button>
          <el-button 
            v-if="canDelete(scope.row)" 
            size="small" 
            type="danger" 
            @click="deleteKnowledge(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { hasPermission, hasRole, ROLE_ADMIN, ROLE_EDITOR } from '../utils/permission'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const searchKeyword = ref('')
const searchType = ref('FULL_TEXT')
const filters = ref({
  category: '',
  status: '' // 将在onMounted中根据权限设置
})
const knowledgeList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    if (searchKeyword.value) {
      // 搜索
      const res = await api.post('/knowledge/search', {
        keyword: searchKeyword.value,
        searchType: searchType.value,
        category: filters.value.category,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      knowledgeList.value = res.data.results || []
      total.value = res.data.total || 0
    } else {
      // 列表
      const res = await api.get('/knowledge/list', {
        params: {
          ...filters.value,
          pageNum: pageNum.value,
          pageSize: pageSize.value
        }
      })
      knowledgeList.value = res.data || []
      total.value = res.data?.length || 0
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleSizeChange = () => {
  loadData()
}

const handlePageChange = () => {
  loadData()
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

const collect = async (row) => {
  try {
    // await api.post(`/knowledge/${row.id}/collect`)
    ElMessage.success('收藏成功')
  } catch (error) {
    ElMessage.error('收藏失败')
  }
}

const canEdit = (row) => {
  if (!userInfo.value) return false
  // ADMIN可以编辑所有知识
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // EDITOR只能编辑自己创建的知识
  if (hasRole(userInfo.value, ROLE_EDITOR) && row.author === userInfo.value.realName) {
    return true
  }
  return false
}

const canDelete = (row) => {
  if (!userInfo.value) return false
  // ADMIN可以删除所有知识
  if (hasRole(userInfo.value, ROLE_ADMIN)) return true
  // EDITOR只能删除自己创建的知识
  if (hasRole(userInfo.value, ROLE_EDITOR) && row.author === userInfo.value.realName) {
    return true
  }
  return false
}

const editKnowledge = (row) => {
  // 跳转到编辑页面
  router.push(`/knowledge/${row.id}/edit`)
}

const deleteKnowledge = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该知识吗？', '提示', {
      type: 'warning'
    })
    // await api.delete(`/knowledge/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getActionColumnWidth = () => {
  if (hasPermission(userInfo.value, 'EDIT') || hasPermission(userInfo.value, 'DELETE')) {
    return 300
  }
  return 200
}

onMounted(() => {
  // 普通用户默认只显示已发布的知识
  if (!hasPermission(userInfo.value, 'VIEW_AUDIT')) {
    filters.value.status = 'APPROVED'
  }
  loadData()
})
</script>

<style scoped>
.knowledge-list {
  padding: 20px;
}

.search-bar {
  display: flex;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
}

.filter-bar {
  margin-bottom: 20px;
}
</style>

