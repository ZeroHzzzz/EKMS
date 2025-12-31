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

    <div v-if="uploading" class="upload-progress">
      <el-progress :percentage="uploadProgress" />
      <p>上传中：{{ currentFileName }}</p>
    </div>

    <el-button type="primary" @click="startUpload" :disabled="fileList.length === 0 || uploading">
      开始上传
    </el-button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import CryptoJS from 'crypto-js'

const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const currentFileName = ref('')

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

  for (const file of fileList.value) {
    await uploadFile(file.raw)
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

    if (initRes.data.completed) {
      ElMessage.success(`${file.name} 上传成功（秒传）`)
      return
    }

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
    await api.post(`/file/complete/${uploadId}`)
    ElMessage.success(`${file.name} 上传成功`)
    
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
</style>

