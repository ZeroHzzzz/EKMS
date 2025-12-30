<template>
  <div class="knowledge-detail" v-loading="loading">
    <div v-if="knowledge">
      <h1>{{ knowledge.title }}</h1>
      <div class="meta-info">
        <span>作者：{{ knowledge.author }}</span>
        <span>分类：{{ knowledge.category }}</span>
        <span>点击量：{{ knowledge.clickCount }}</span>
        <span>收藏量：{{ knowledge.collectCount }}</span>
      </div>
      <div class="content">
        <p>{{ knowledge.content }}</p>
      </div>
      <div class="related-knowledge" v-if="relatedKnowledge.length > 0">
        <h3>相关知识</h3>
        <el-row :gutter="20">
          <el-col :span="8" v-for="item in relatedKnowledge" :key="item.id">
            <el-card @click="viewDetail(item.id)" style="cursor: pointer">
              <h4>{{ item.title }}</h4>
              <p>{{ item.summary }}</p>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()

const knowledge = ref(null)
const relatedKnowledge = ref([])
const loading = ref(false)

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/knowledge/${route.params.id}`)
    knowledge.value = res.data
    
    // 加载相关知识
    const relatedRes = await api.get(`/knowledge/${route.params.id}/related`, {
      params: { limit: 6 }
    })
    relatedKnowledge.value = relatedRes.data || []
  } catch (error) {
    console.error('加载详情失败', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.knowledge-detail {
  padding: 20px;
}

.meta-info {
  margin: 20px 0;
  color: #666;
}

.meta-info span {
  margin-right: 20px;
}

.content {
  margin: 30px 0;
  line-height: 1.8;
}

.related-knowledge {
  margin-top: 40px;
}
</style>

