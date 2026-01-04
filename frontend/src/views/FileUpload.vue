<template>
  <div class="file-upload-page">
    <div class="upload-wrapper">
      <!-- 页面标题 -->
      <div class="page-title">
        <h2>上传文档</h2>
        <p>支持 Word、Excel、PDF 等格式，自动提取内容用于全文搜索</p>
      </div>

      <!-- 主内容区 -->
      <div class="upload-content">
        <!-- 上传区域 -->
        <div class="upload-area">
          <el-upload
            class="upload-dragger"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="fileList"
            multiple
            :accept="acceptedTypes"
            :show-file-list="false"
          >
            <div class="upload-inner">
              <div class="upload-icon-wrapper">
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
              </div>
              <div class="upload-text">
                <p class="main-text">拖拽文件到这里，或 <em>点击上传</em></p>
                <p class="sub-text">支持 doc, docx, xls, xlsx, ppt, pptx, pdf, txt, 图片, 视频</p>
              </div>
            </div>
          </el-upload>
        </div>

        <!-- 已选文件列表 -->
        <div v-if="fileList.length > 0" class="file-list-section">
          <div class="section-header">
            <span class="section-title">
              <el-icon><Folder /></el-icon>
              已选择 {{ fileList.length }} 个文件
            </span>
            <el-button type="danger" text size="small" @click="clearAllFiles">
              清空全部
            </el-button>
          </div>
          <div class="file-list">
            <div 
              v-for="(file, index) in fileList" 
              :key="file.uid" 
              class="file-item"
              :class="{ 
                'is-uploading': uploadingIndex === index, 
                'is-uploaded': uploadedFiles.includes(file.uid) 
              }"
            >
              <div class="file-icon" :style="{ background: getFileTypeColor(file.name) }">
                {{ getFileExt(file.name) }}
              </div>
              <div class="file-info">
                <span class="file-name">{{ file.name }}</span>
                <span class="file-meta">{{ formatFileSize(file.size) }}</span>
              </div>
              <div class="file-action">
                <el-tag v-if="uploadedFiles.includes(file.uid)" type="success" size="small" effect="light">
                  <el-icon><Check /></el-icon> 已完成
                </el-tag>
                <el-progress 
                  v-else-if="uploadingIndex === index" 
                  :percentage="uploadProgress" 
                  :stroke-width="6"
                  style="width: 80px"
                />
                <el-button v-else type="danger" text circle size="small" @click="removeFile(index)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 表单区域 -->
        <div class="form-section">
          <div class="section-header">
            <span class="section-title">
              <el-icon><EditPen /></el-icon>
              文档信息
            </span>
          </div>
          <el-form :model="uploadForm" :rules="formRules" ref="formRef" label-position="top" class="upload-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="部门">
                  <el-select v-model="uploadForm.department" placeholder="选择部门" style="width: 100%">
                    <el-option 
                      v-for="dept in departments" 
                      :key="dept.id" 
                      :label="dept.name" 
                      :value="dept.name" 
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="分类" prop="category">
                  <el-select v-model="uploadForm.category" placeholder="选择分类" style="width: 100%">
                    <el-option label="技术文档" value="技术文档" />
                    <el-option label="业务文档" value="业务文档" />
                    <el-option label="培训资料" value="培训资料" />
                    <el-option label="规章制度" value="规章制度" />
                    <el-option label="项目文档" value="项目文档" />
                    <el-option label="会议纪要" value="会议纪要" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="关键词">
                  <el-input v-model="uploadForm.keywords" placeholder="多个关键词用逗号分隔" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="摘要">
                  <el-input v-model="uploadForm.summary" placeholder="简要描述文档内容（可选）" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <!-- 底部操作栏 -->
        <div class="action-bar">
          <div class="tips">
            <el-icon><InfoFilled /></el-icon>
            <span>上传后系统将自动提取文档内容，支持全文搜索</span>
          </div>
          <el-button 
            type="primary" 
            size="large"
            @click="startUpload" 
            :disabled="fileList.length === 0 || uploading"
            :loading="uploading"
          >
            <el-icon v-if="!uploading"><Upload /></el-icon>
            {{ uploading ? `上传中 ${uploadProgress}%` : '开始上传' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 上传成功弹窗 -->
    <el-dialog 
      v-model="showSuccessDialog" 
      width="400px"
      :show-close="false"
      center
    >
      <div class="success-dialog">
        <div class="success-icon-wrapper">
          <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
        </div>
        <h3>上传成功</h3>
        <p>已成功上传 <strong>{{ successCount }}</strong> 个文件</p>
        <p class="sub-tip">系统正在后台提取文档内容</p>
        <div class="dialog-actions">
          <el-button @click="continueUpload">继续上传</el-button>
          <el-button type="primary" @click="goToManagement">查看文档</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import api from '../api'
import CryptoJS from 'crypto-js'
import { 
  Upload, UploadFilled, Folder, Close, EditPen, Check,
  InfoFilled, CircleCheckFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)

// 文件列表
const fileList = ref([])
const uploadedFiles = ref([])
const uploadingIndex = ref(-1)
const uploadProgress = ref(0)
const uploading = ref(false)
const successCount = ref(0)
const showSuccessDialog = ref(false)

// 部门列表
const departments = ref([])

// 上传表单
const uploadForm = ref({
  department: '',
  category: '',
  summary: '',
  keywords: ''
})

// 表单验证规则
const formRules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

// 接受的文件类型
const acceptedTypes = '.doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.txt,.jpg,.jpeg,.png,.gif,.bmp,.mp4,.avi,.mov,.wmv,.mp3,.wav'

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

onMounted(async () => {
  await loadDepartments()
  const userInfo = userStore.userInfo
  if (userInfo?.department) {
    uploadForm.value.department = userInfo.department
  }
})

const loadDepartments = async () => {
  try {
    const res = await api.get('/department/list')
    if (res.code === 200) {
      departments.value = res.data || []
    }
  } catch (error) {
    console.error('加载部门列表失败', error)
  }
}

const handleFileChange = (file, files) => {
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
}

const removeFile = (index) => {
  fileList.value.splice(index, 1)
}

const clearAllFiles = () => {
  fileList.value = []
  uploadedFiles.value = []
}

const resetForm = () => {
  formRef.value?.resetFields()
  clearAllFiles()
  uploadForm.value = {
    department: userStore.userInfo?.department || '',
    category: '',
    summary: '',
    keywords: ''
  }
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
    ElMessage.warning('请选择要上传的文件')
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  uploading.value = true
  successCount.value = 0
  uploadedFiles.value = []

  for (let i = 0; i < fileList.value.length; i++) {
    if (uploadedFiles.value.includes(fileList.value[i].uid)) continue
    uploadingIndex.value = i
    uploadProgress.value = 0
    try {
      await uploadFile(fileList.value[i].raw)
      uploadedFiles.value.push(fileList.value[i].uid)
      successCount.value++
    } catch (error) {
      ElMessage.error(`${fileList.value[i].name} 上传失败：${error.message}`)
    }
  }

  uploading.value = false
  uploadingIndex.value = -1

  if (successCount.value > 0) {
    showSuccessDialog.value = true
  }
}

const uploadFile = async (file) => {
  const fileHash = await calculateFileHash(file)
  
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
    fileDTO = initRes.data.file
    uploadProgress.value = 100
  } else {
    const uploadId = initRes.data.uploadId
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

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

    const completeRes = await api.post(`/file/complete/${uploadId}`)
    if (completeRes.code !== 200 || !completeRes.data) {
      throw new Error('文件上传完成失败')
    }
    fileDTO = completeRes.data
  }
  
  const userInfo = userStore.userInfo
  if (!userInfo) {
    throw new Error('用户信息不存在，请重新登录')
  }
  
  const fileNameWithoutExt = file.name.substring(0, file.name.lastIndexOf('.')) || file.name
  
  let fileContent = `文件：${file.name}`
  if (file.type.startsWith('text/') || file.name.endsWith('.txt')) {
    try {
      const text = await file.text()
      fileContent = text
    } catch (e) {
      console.warn('读取文件内容失败', e)
    }
  }
  
  const knowledgeRes = await api.post('/knowledge', {
    title: fileNameWithoutExt,
    content: fileContent,
    summary: uploadForm.value.summary || `上传的文件：${file.name}`,
    category: uploadForm.value.category,
    keywords: uploadForm.value.keywords || '',
    department: uploadForm.value.department || userInfo.department || '未知',
    fileId: fileDTO.id,
    author: userInfo.realName || userInfo.username || '未知',
    createBy: userInfo.username
  })
  
  if (knowledgeRes.code !== 200) {
    throw new Error(knowledgeRes.message || '创建知识条目失败')
  }
}

const continueUpload = () => {
  showSuccessDialog.value = false
  resetForm()
}

const goToManagement = () => {
  showSuccessDialog.value = false
  router.push('/knowledge-management')
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileExt = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  return ext.length > 4 ? ext.substring(0, 4) : ext
}

const getFileTypeColor = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  const colorMap = {
    doc: '#2b579a', docx: '#2b579a',
    xls: '#217346', xlsx: '#217346',
    ppt: '#d24726', pptx: '#d24726',
    pdf: '#f40f02',
    txt: '#5c6bc0',
    jpg: '#ff9800', jpeg: '#ff9800', png: '#4caf50', gif: '#9c27b0',
    mp4: '#e91e63', avi: '#e91e63',
    mp3: '#00bcd4', wav: '#00bcd4'
  }
  return colorMap[ext] || '#909399'
}
</script>

<style scoped>
.file-upload-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7fa;
}

.upload-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  text-align: center;
  margin-bottom: 32px;
}

.page-title h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-title p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.upload-content {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* 上传区域 */
.upload-area {
  padding: 32px;
  border-bottom: 1px solid #f0f0f0;
}

.upload-dragger {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: auto;
  padding: 48px 20px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-icon-wrapper {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.upload-icon {
  font-size: 28px;
  color: #fff;
}

.upload-text {
  text-align: center;
}

.main-text {
  font-size: 16px;
  color: #606266;
  margin: 0 0 8px 0;
}

.main-text em {
  color: #409eff;
  font-style: normal;
  cursor: pointer;
}

.sub-text {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 文件列表 */
.file-list-section {
  padding: 20px 32px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.section-title .el-icon {
  color: #409eff;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 8px;
  transition: all 0.2s;
}

.file-item:hover {
  background: #f0f0f0;
}

.file-item.is-uploading {
  background: #ecf5ff;
}

.file-item.is-uploaded {
  background: #f0f9eb;
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  text-transform: uppercase;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  font-size: 12px;
  color: #909399;
}

.file-action {
  flex-shrink: 0;
}

/* 表单区域 */
.form-section {
  padding: 20px 32px;
  border-bottom: 1px solid #f0f0f0;
}

.upload-form {
  margin-top: 0;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form-item__label) {
  font-size: 13px;
  color: #606266;
  padding-bottom: 6px;
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
  background: #fafafa;
}

.tips {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.tips .el-icon {
  color: #409eff;
}

/* 成功弹窗 */
.success-dialog {
  text-align: center;
  padding: 20px 0;
}

.success-icon-wrapper {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.success-icon {
  font-size: 36px;
  color: #fff;
}

.success-dialog h3 {
  font-size: 20px;
  color: #303133;
  margin: 0 0 8px 0;
}

.success-dialog p {
  font-size: 14px;
  color: #606266;
  margin: 0 0 4px 0;
}

.success-dialog .sub-tip {
  font-size: 13px;
  color: #909399;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

/* 滚动条 */
.file-list::-webkit-scrollbar {
  width: 6px;
}

.file-list::-webkit-scrollbar-track {
  background: transparent;
}

.file-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.file-list::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>
