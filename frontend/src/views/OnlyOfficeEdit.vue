<template>
  <div class="onlyoffice-page">
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <span class="file-title">{{ fileName || '文档编辑' }}</span>
        <el-tag v-if="mode === 'edit'" type="success" size="small">编辑模式</el-tag>
        <el-tag v-else type="info" size="small">查看模式</el-tag>
      </div>
      <div class="header-right">
        <span v-if="loading" class="loading-hint">
          <el-icon class="is-loading"><Loading /></el-icon> 加载中...
        </span>
        <span v-else class="ready-hint">
          <el-icon><CircleCheckFilled /></el-icon> 已就绪
        </span>
        <el-tooltip content="OnlyOffice会自动保存，关闭编辑器时会创建新版本" placement="bottom">
          <span class="save-tip">
            <el-icon><InfoFilled /></el-icon>
          </span>
        </el-tooltip>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-container">
      <el-result icon="error" :title="error" sub-title="请检查 OnlyOffice 服务是否正常运行">
        <template #extra>
          <el-button type="primary" @click="initEditor">重试</el-button>
          <el-button @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>

    <!-- 编辑器容器 -->
    <div v-show="!error" v-loading="loading" class="editor-wrapper">
      <div id="onlyoffice-editor" class="editor-container"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading, CircleCheckFilled, InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import api from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 从路由参数获取
const fileId = ref(route.params.fileId || route.query.fileId)
const mode = ref(route.query.mode || 'edit')
const fileName = ref(route.query.fileName || '')

// 状态
const loading = ref(true)
const error = ref(null)
let docEditor = null

// 返回上一页
const goBack = () => {
  router.back()
}

// 初始化编辑器
const initEditor = async () => {
  if (!fileId.value) {
    error.value = '缺少文件ID参数'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    // 获取编辑器配置
    const res = await api.get(`/onlyoffice/config/${fileId.value}`, {
      params: {
        mode: mode.value,
        userName: userStore.userInfo?.realName || userStore.userInfo?.username || '匿名用户',
        userId: String(userStore.userInfo?.id || 'anonymous')
      }
    })

    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '获取编辑器配置失败')
    }

    const config = res.data
    console.log('OnlyOffice 配置:', config)

    // 更新文件名
    if (config.document?.title) {
      fileName.value = config.document.title
    }

    // 加载 OnlyOffice API
    await loadOnlyOfficeApi(config.apiUrl)

    // 销毁旧的编辑器实例
    if (docEditor) {
      try {
        docEditor.destroyEditor()
      } catch (e) {
        console.warn('销毁旧编辑器失败:', e)
      }
      docEditor = null
    }

    // 配置编辑器
    const editorConfig = {
      document: config.document,
      documentType: config.documentType,
      editorConfig: {
        ...config.editorConfig,
        events: {
          onReady: () => {
            console.log('编辑器已就绪')
            // 编辑器框架加载完成，关闭 loading 遮罩
            loading.value = false
          },
          onDocumentReady: () => {
            console.log('文档已加载')
            loading.value = false
          },
          onDocumentStateChange: (event) => {
            console.log('文档状态:', event.data ? '有未保存的更改' : '已保存')
          },
          onError: (event) => {
            console.error('编辑器错误:', event)
            const errorCode = event.data?.errorCode || event.data
            let errorMsg = '编辑器出错'
            
            // 常见错误码
            if (errorCode === -2) {
              errorMsg = '无法下载文档，请检查文件是否存在'
            } else if (errorCode === -3) {
              errorMsg = '文档格式不支持'
            } else if (errorCode === -4) {
              errorMsg = '无法连接到文档服务器'
            } else if (errorCode === -5 || errorCode === -6) {
              errorMsg = '文档服务器配置错误'
            }
            
            error.value = errorMsg
            loading.value = false
          },
          onWarning: (event) => {
            console.warn('编辑器警告:', event)
          },
          onInfo: (event) => {
            console.log('编辑器信息:', event)
          }
        }
      },
      type: 'desktop',
      width: '100%',
      height: '100%'
    }

    // 创建编辑器
    if (window.DocsAPI) {
      console.log('创建 DocEditor...')
      docEditor = new window.DocsAPI.DocEditor('onlyoffice-editor', editorConfig)
      
      // 备用超时机制：如果 10 秒后仍在 loading，自动关闭
      setTimeout(() => {
        if (loading.value) {
          console.log('编辑器加载超时，自动关闭 loading')
          loading.value = false
        }
      }, 10000)
    } else {
      throw new Error('OnlyOffice API 未加载')
    }

  } catch (err) {
    console.error('初始化编辑器失败:', err)
    error.value = err.message || '初始化编辑器失败'
    loading.value = false
  }
}

// 动态加载 OnlyOffice API
const loadOnlyOfficeApi = (apiUrl) => {
  return new Promise((resolve, reject) => {
    // 检查是否已加载
    if (window.DocsAPI) {
      console.log('DocsAPI 已存在')
      resolve()
      return
    }

    console.log('加载 OnlyOffice API:', apiUrl)

    // 创建 script 标签
    const script = document.createElement('script')
    script.src = apiUrl
    script.async = true

    script.onload = () => {
      console.log('OnlyOffice API 脚本已加载')
      // 等待 DocsAPI 可用
      let attempts = 0
      const checkApi = setInterval(() => {
        attempts++
        if (window.DocsAPI) {
          clearInterval(checkApi)
          console.log('DocsAPI 已可用')
          resolve()
        } else if (attempts > 50) {
          clearInterval(checkApi)
          reject(new Error('DocsAPI 加载超时'))
        }
      }, 100)
    }

    script.onerror = (e) => {
      console.error('加载脚本失败:', e)
      reject(new Error('加载 OnlyOffice API 失败，请确保 OnlyOffice Document Server 已启动'))
    }

    document.head.appendChild(script)
  })
}

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  if (docEditor) {
    try {
      docEditor.destroyEditor()
    } catch (e) {
      console.warn('销毁编辑器失败:', e)
    }
    docEditor = null
  }
})
</script>

<style scoped>
.onlyoffice-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  z-index: 1000;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 14px;
}

.ready-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #67c23a;
  font-size: 14px;
}

.save-tip {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 16px;
  cursor: help;
}

.error-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
}

.editor-wrapper {
  flex: 1;
  overflow: hidden;
}

.editor-container {
  width: 100%;
  height: 100%;
}
</style>

