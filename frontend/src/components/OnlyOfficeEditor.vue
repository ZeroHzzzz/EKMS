<template>
  <div class="onlyoffice-editor-container" v-loading="loading">
    <!-- 错误提示 -->
    <div v-if="error" class="editor-error">
      <el-result icon="error" :title="error">
        <template #extra>
          <el-button type="primary" @click="initEditor">重试</el-button>
          <el-button @click="$emit('close')">关闭</el-button>
        </template>
      </el-result>
    </div>
    
    <!-- 编辑器容器 -->
    <div v-show="!error" id="onlyoffice-editor-container" ref="editorContainer" class="editor-frame"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const props = defineProps({
  // 文件ID
  fileId: {
    type: [Number, String],
    required: true
  },
  // 模式：edit（编辑）或 view（查看）
  mode: {
    type: String,
    default: 'edit'
  },
  // 用户名
  userName: {
    type: String,
    default: ''
  },
  // 用户ID
  userId: {
    type: [String, Number],
    default: ''
  }
})

const emit = defineEmits(['close', 'save', 'error', 'ready'])

const editorContainer = ref(null)
const loading = ref(true)
const error = ref(null)
let docEditor = null

// 初始化编辑器
const initEditor = async () => {
  loading.value = true
  error.value = null
  
  try {
    // 获取编辑器配置
    const res = await api.get(`/onlyoffice/config/${props.fileId}`, {
      params: {
        mode: props.mode,
        userName: props.userName,
        userId: props.userId
      }
    })
    
    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '获取编辑器配置失败')
    }
    
    const config = res.data
    console.log('OnlyOffice 配置:', JSON.stringify(config, null, 2))
    
    // 加载 OnlyOffice API
    await loadOnlyOfficeApi(config.apiUrl)
    
    // 配置编辑器
    const editorConfig = {
      document: config.document,
      documentType: config.documentType,
      editorConfig: {
        ...config.editorConfig,
        // 编辑器事件回调
        events: {
          onReady: () => {
            console.log('OnlyOffice 编辑器已就绪')
            // 编辑器框架加载完成，关闭 loading
            loading.value = false
            emit('ready')
          },
          onDocumentStateChange: (event) => {
            console.log('文档状态变化:', event.data)
          },
          onError: (event) => {
            console.error('OnlyOffice 错误:', event.data)
            emit('error', event.data)
          },
          onWarning: (event) => {
            console.warn('OnlyOffice 警告:', event.data)
          },
          onInfo: (event) => {
            console.log('OnlyOffice 信息:', event.data)
          },
          onRequestSaveAs: (event) => {
            // 另存为事件
            console.log('请求另存为:', event.data)
          },
          onRequestClose: () => {
            // 关闭编辑器
            emit('close')
          },
          onDocumentReady: () => {
            console.log('文档已加载')
            loading.value = false
          }
        }
      },
      type: 'desktop',
      width: '100%',
      height: '100%'
    }
    
    // 销毁旧的编辑器实例
    if (docEditor) {
      docEditor.destroyEditor()
      docEditor = null
    }
    
    // 创建编辑器（需要传入元素ID字符串，而不是DOM元素）
    if (window.DocsAPI) {
      docEditor = new window.DocsAPI.DocEditor('onlyoffice-editor-container', editorConfig)
      
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
    emit('error', err.message)
  }
}

// 动态加载 OnlyOffice API
const loadOnlyOfficeApi = (apiUrl) => {
  return new Promise((resolve, reject) => {
    // 检查是否已加载
    if (window.DocsAPI) {
      resolve()
      return
    }
    
    // 创建 script 标签
    const script = document.createElement('script')
    script.src = apiUrl
    script.async = true
    
    script.onload = () => {
      console.log('OnlyOffice API 已加载')
      resolve()
    }
    
    script.onerror = () => {
      reject(new Error('加载 OnlyOffice API 失败，请确保 OnlyOffice Document Server 已启动'))
    }
    
    document.head.appendChild(script)
  })
}

// 强制保存文档
const forceSave = async () => {
  try {
    await api.post(`/onlyoffice/forcesave/${props.fileId}`)
    ElMessage.success('保存成功')
    emit('save')
  } catch (err) {
    console.error('保存失败:', err)
    ElMessage.error('保存失败')
  }
}

// 暴露方法给父组件
defineExpose({
  forceSave,
  initEditor
})

// 监听 fileId 变化
watch(() => props.fileId, () => {
  if (props.fileId) {
    initEditor()
  }
})

onMounted(() => {
  if (props.fileId) {
    initEditor()
  }
})

onBeforeUnmount(() => {
  // 销毁编辑器
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
.onlyoffice-editor-container {
  width: 100%;
  height: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.editor-frame {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
}

.editor-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: white;
}
</style>

