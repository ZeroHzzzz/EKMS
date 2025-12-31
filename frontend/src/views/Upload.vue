<template>
  <div class="upload-page">
    <el-upload
      class="upload-demo"
      drag
      :auto-upload="false"
      :on-change="handleFileChange"
      :file-list="fileList"
      multiple
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 Word、Excel、PDF、图片、视频等格式，支持批量上传
        </div>
      </template>
    </el-upload>

    <!-- 上传信息表单 -->
    <div v-if="fileList.length > 0" class="upload-form">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="分类" required>
          <el-select v-model="uploadForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="技术文档" value="技术文档" />
            <el-option label="业务文档" value="业务文档" />
            <el-option label="培训资料" value="培训资料" />
            <el-option label="未分类" value="未分类" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="uploadForm.summary" type="textarea" :rows="3" placeholder="请输入文件摘要" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="uploadForm.keywords" placeholder="请输入关键词，多个用逗号分隔" />
        </el-form-item>
      </el-form>
    </div>

    <div v-if="uploading" class="upload-progress">
      <el-progress :percentage="uploadProgress" />
      <p>上传中：{{ currentFileName }}</p>
    </div>

    <el-button type="primary" @click="startUpload" :disabled="fileList.length === 0 || uploading || !uploadForm.category">
      开始上传
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import api from '../api'
import CryptoJS from 'crypto-js'

const userStore = useUserStore()

const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const currentFileName = ref('')

// 上传表单
const uploadForm = ref({
  category: '',
  summary: '',
  keywords: ''
})

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

const handleFileChange = (file, files) => {
  fileList.value = files
}

const calculateFileHash = (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const wordArray = CryptoJS.lib.WordArray.create(e.target.result)
      const hash = CryptoJS.SHA256(wordArray).toString()
      resolve(hash)
    }
    reader.readAsArrayBuffer(file)
  })
}

const startUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  
  if (!uploadForm.value.category) {
    ElMessage.warning('请选择分类')
    return
  }

  for (const file of fileList.value) {
    await uploadFile(file.raw)
  }
  
  // 上传完成后清空表单
  fileList.value = []
  uploadForm.value = {
    category: '',
    summary: '',
    keywords: ''
  }
}

const uploadFile = async (file) => {
  uploading.value = true
  currentFileName.value = file.name
  uploadProgress.value = 0

  try {
    // 计算文件哈希
    const fileHash = await calculateFileHash(file)
    
    // 初始化上传
    const initRes = await api.post('/file/init', {
      fileName: file.name,
      fileSize: file.size,
      fileHash: fileHash
    }, {
      transformRequest: [(data) => {
        const params = new URLSearchParams()
        params.append('fileName', data.fileName)
        params.append('fileSize', data.fileSize)
        params.append('fileHash', data.fileHash)
        return params
      }]
    })

    let fileDTO = null
    
    if (initRes.data.completed) {
      // 秒传，直接使用返回的文件信息
      fileDTO = initRes.data.file
      ElMessage.success(`${file.name} 上传成功（秒传）`)
    } else {
      // 正常上传流程
      const uploadId = initRes.data.uploadId
      const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

      // 分片上传
      for (let i = 0; i < totalChunks; i++) {
        const start = i * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, file.size)
        const chunk = file.slice(start, end)
        
        const chunkArrayBuffer = await chunk.arrayBuffer()
        const chunkHash = await calculateFileHash(new Blob([chunkArrayBuffer]))
        
        const formData = new FormData()
        formData.append('uploadId', uploadId)
        formData.append('chunkIndex', i)
        formData.append('chunkHash', chunkHash)
        formData.append('chunk', chunk)

        await api.post('/file/chunk', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

        uploadProgress.value = Math.round(((i + 1) / totalChunks) * 100)
      }

      // 完成上传
      const completeRes = await api.post(`/file/complete/${uploadId}`)
      if (completeRes.code !== 200 || !completeRes.data) {
        throw new Error('文件上传完成失败')
      }
      fileDTO = completeRes.data
    }
    
    // 创建知识条目
    const userInfo = userStore.userInfo
    if (!userInfo) {
      throw new Error('用户信息不存在，请重新登录')
    }
    
    // 提取文件名（不含扩展名）作为标题
    const fileNameWithoutExt = file.name.substring(0, file.name.lastIndexOf('.')) || file.name
    
    // 读取文件内容（仅文本文件）
    let fileContent = `文件：${file.name}`
    if (file.type.startsWith('text/') || file.name.endsWith('.txt')) {
      try {
        const text = await file.text()
        fileContent = text
      } catch (e) {
        console.warn('读取文件内容失败，使用默认内容', e)
      }
    }
    
    const knowledgeRes = await api.post('/knowledge', {
      title: fileNameWithoutExt,
      content: fileContent,
      summary: uploadForm.value.summary || `上传的文件：${file.name}`,
      category: uploadForm.value.category,
      keywords: uploadForm.value.keywords || '',
      fileId: fileDTO.id,
      author: userInfo.realName || userInfo.username || '未知',
      department: userInfo.department || '未知',
      createBy: userInfo.username
    })
    
    if (knowledgeRes.code !== 200) {
      throw new Error(knowledgeRes.message || '创建知识条目失败')
    }
    
    ElMessage.success(`${file.name} 上传成功并已创建知识条目`)
    
  } catch (error) {
    ElMessage.error(`${file.name} 上传失败：${error.message}`)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}
</script>

<style scoped>
.upload-page {
  padding: 20px;
}

.upload-progress {
  margin: 20px 0;
}

.upload-form {
  margin: 20px 0;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>

