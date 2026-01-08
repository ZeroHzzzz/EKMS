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
                  <el-icon v-if="instantUploadFiles.includes(file.uid)"><Lightning /></el-icon>
                  <el-icon v-else><Check /></el-icon> 
                  {{ instantUploadFiles.includes(file.uid) ? '秒传成功' : '已完成' }}
                </el-tag>
                <div v-else-if="uploadingIndex === index" class="uploading-status">
                  <el-progress 
                    :percentage="uploadProgress" 
                    :stroke-width="6"
                    style="width: 80px"
                  />
                  <span class="chunk-info" v-if="uploadChunkInfo[file.uid]">
                    {{ uploadChunkInfo[file.uid].current }}/{{ uploadChunkInfo[file.uid].total }}
                  </span>
                </div>
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
                <el-form-item label="部门" prop="department">
                  <el-select 
                    v-model="uploadForm.department" 
                    placeholder="选择部门" 
                    style="width: 100%"
                    @change="handleDepartmentChange"
                  >
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
                <el-form-item label="存放位置">
                  <el-input 
                    v-model="uploadForm.parentPath" 
                    placeholder="选择存放的知识结构（可选）" 
                    readonly
                    @click="openFolderDialog"
                    style="cursor: pointer;"
                    :disabled="!uploadForm.department"
                  >
                    <template #suffix>
                      <el-icon><FolderOpened /></el-icon>
                    </template>
                  </el-input>
                  <div v-if="!uploadForm.department" class="form-tip">请先选择部门</div>
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

    <!-- 文件夹选择对话框 -->
    <el-dialog v-model="showFolderDialog" :title="`选择存放位置 - ${uploadForm.department}`" width="500px">
      <div class="folder-dialog-header">
        <el-button type="primary" size="small" @click="showNewFolderInput">
          <el-icon><Plus /></el-icon>
          新建文件夹
        </el-button>
      </div>
      
      <!-- 新建文件夹输入 -->
      <div v-if="newFolderVisible" class="new-folder-input">
        <div class="new-folder-form">
          <div class="new-folder-hint">
            <el-icon><FolderAdd /></el-icon>
            <span v-if="contextMenuTargetFolder">
              在「{{ contextMenuTargetFolder.title }}」下创建子文件夹
            </span>
            <span v-else-if="selectedFolderId">
              在「{{ selectedFolderPath }}」下创建子文件夹
            </span>
            <span v-else>在根目录创建文件夹</span>
          </div>
          <div class="new-folder-row">
            <el-input
              v-model="newFolderName"
              placeholder="输入文件夹名称"
              size="small"
              @keyup.enter="createNewFolder"
            >
              <template #prefix>
                <el-icon><Folder /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" size="small" @click="createNewFolder" :loading="creatingFolder">
              创建
            </el-button>
            <el-button size="small" @click="cancelNewFolder">取消</el-button>
          </div>
        </div>
      </div>
      
      <div class="folder-tree-container">
        <el-tree
          ref="folderTreeRef"
          :data="folderTree"
          :props="{ label: 'title', children: 'children' }"
          node-key="id"
          highlight-current
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handleFolderSelect"
          @node-contextmenu="handleFolderContextMenu"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <el-icon><Folder /></el-icon>
              <span>{{ data.title }}</span>
            </div>
          </template>
          <template #empty>
            <div class="empty-tip">
              <el-icon><FolderOpened /></el-icon>
              <p>{{ uploadForm.department }} 部门下暂无文件夹</p>
              <p class="sub">点击上方"新建文件夹"创建</p>
            </div>
          </template>
        </el-tree>
      </div>
      <div v-if="selectedFolderPath" class="selected-path">
        <el-icon><FolderOpened /></el-icon>
        <span>已选择：{{ selectedFolderPath }}</span>
      </div>
      <template #footer>
        <el-button @click="clearFolderSelection">清除选择</el-button>
        <el-button type="primary" @click="confirmFolderSelection">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 右键菜单 -->
    <div 
      v-if="contextMenuVisible" 
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
    >
      <div class="context-menu-item" @click="createSubFolder">
        <el-icon><FolderAdd /></el-icon>
        <span>新建子文件夹</span>
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
  Upload, UploadFilled, Folder, FolderOpened, Close, EditPen, Check,
  InfoFilled, CircleCheckFilled, Plus, FolderAdd, Lightning
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const folderTreeRef = ref(null)

// 文件列表
const fileList = ref([])
const uploadedFiles = ref([])
const instantUploadFiles = ref([])
const uploadChunkInfo = ref({})
const uploadingIndex = ref(-1)
const uploadProgress = ref(0)
const uploading = ref(false)
const successCount = ref(0)
const showSuccessDialog = ref(false)

// 部门列表
const departments = ref([])

// 文件夹选择
const showFolderDialog = ref(false)
const allFolders = ref([])  // 所有文件夹原始数据
const folderTree = ref([])  // 过滤后的树形数据
const selectedFolderId = ref(null)
const selectedFolderPath = ref('')

// 新建文件夹相关
const newFolderVisible = ref(false)
const newFolderName = ref('')
const creatingFolder = ref(false)
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuTargetFolder = ref(null)

// 上传表单
const uploadForm = ref({
  department: '',
  parentId: null,
  parentPath: '',
  summary: '',
  keywords: ''
})

// 表单验证规则
const formRules = {
  department: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

// 接受的文件类型
const acceptedTypes = '.doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.txt,.jpg,.jpeg,.png,.gif,.bmp,.mp4,.avi,.mov,.wmv,.mp3,.wav'

const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

onMounted(async () => {
  await Promise.all([loadDepartments(), loadFolderTree()])
  const userInfo = userStore.userInfo
  if (userInfo?.department) {
    uploadForm.value.department = userInfo.department
  }
})

// 加载部门列表 - 修正 API 路径
const loadDepartments = async () => {
  try {
    const res = await api.get('/department')  // 修正：不是 /department/list
    if (res.code === 200) {
      departments.value = res.data || []
    }
  } catch (error) {
    console.error('加载部门列表失败', error)
  }
}

// 加载知识结构树（只加载文件夹）
const loadFolderTree = async () => {
  try {
    const res = await api.get('/knowledge/list', { 
      params: { 
        includeFolders: 'true',
        pageSize: 10000 
      } 
    })
    if (res.code === 200) {
      const allItems = res.data || []
      // 只保留文件夹（没有 fileId 的项目，且 id > 0）
      allFolders.value = allItems.filter(item => !item.fileId && item.id > 0)
    }
  } catch (error) {
    console.error('加载知识结构失败', error)
  }
}

// 打开文件夹选择对话框（根据部门过滤）
const openFolderDialog = () => {
  if (!uploadForm.value.department) {
    ElMessage.warning('请先选择部门')
    return
  }
  
  // 根据所选部门过滤文件夹
  const filteredFolders = allFolders.value.filter(
    folder => folder.department === uploadForm.value.department
  )
  folderTree.value = buildTree(filteredFolders)
  showFolderDialog.value = true
}

// 部门变化时清空存放位置
const handleDepartmentChange = () => {
  uploadForm.value.parentId = null
  uploadForm.value.parentPath = ''
  selectedFolderId.value = null
  selectedFolderPath.value = ''
}

// 构建树形结构
const buildTree = (list) => {
  const map = {}
  const roots = []
  
  list.forEach(item => {
    map[item.id] = { 
      id: item.id,
      title: item.title,
      parentId: item.parentId,
      children: [] 
    }
  })
  
  list.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    } else {
      roots.push(map[item.id])
    }
  })
  
  return roots
}

// 选择文件夹
const handleFolderSelect = (data) => {
  selectedFolderId.value = data.id
  selectedFolderPath.value = getNodePath(data.id, folderTree.value) || data.title
}

// 获取节点路径
const getNodePath = (nodeId, nodes, path = []) => {
  for (const node of nodes) {
    if (node.id === nodeId) {
      return [...path, node.title].join(' / ')
    }
    if (node.children && node.children.length > 0) {
      const found = getNodePath(nodeId, node.children, [...path, node.title])
      if (found) return found
    }
  }
  return null
}

// 确认文件夹选择
const confirmFolderSelection = () => {
  uploadForm.value.parentId = selectedFolderId.value
  uploadForm.value.parentPath = selectedFolderPath.value
  showFolderDialog.value = false
}

// 清除文件夹选择
const clearFolderSelection = () => {
  selectedFolderId.value = null
  selectedFolderPath.value = ''
  uploadForm.value.parentId = null
  uploadForm.value.parentPath = ''
  if (folderTreeRef.value) {
    folderTreeRef.value.setCurrentKey(null)
  }
  showFolderDialog.value = false
}

// 显示新建文件夹输入框
const showNewFolderInput = () => {
  newFolderVisible.value = true
  newFolderName.value = ''
  contextMenuTargetFolder.value = null  // 在根目录创建
}

// 取消新建文件夹
const cancelNewFolder = () => {
  newFolderVisible.value = false
  newFolderName.value = ''
  contextMenuTargetFolder.value = null
}

// 创建新文件夹
const createNewFolder = async () => {
  if (!newFolderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  
  creatingFolder.value = true
  try {
    const userInfo = userStore.userInfo
    const res = await api.post('/knowledge', {
      title: newFolderName.value.trim(),
      content: '',
      summary: '',
      keywords: '',
      department: uploadForm.value.department,
      parentId: contextMenuTargetFolder.value?.id || selectedFolderId.value || null,
      fileId: null,  // 没有 fileId 表示是文件夹
      author: userInfo?.realName || userInfo?.username || '未知',
      createBy: userInfo?.username
    })
    
    if (res.code === 200) {
      ElMessage.success('文件夹创建成功')
      cancelNewFolder()
      hideContextMenu()
      // 重新加载文件夹列表并刷新树
      await loadFolderTree()
      // 重新构建当前部门的树
      const filteredFolders = allFolders.value.filter(
        folder => folder.department === uploadForm.value.department
      )
      folderTree.value = buildTree(filteredFolders)
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    ElMessage.error('创建文件夹失败：' + (error.message || '未知错误'))
  } finally {
    creatingFolder.value = false
  }
}

// 右键菜单 - 在选中的文件夹下创建子文件夹
const handleFolderContextMenu = (event, data, node) => {
  event.preventDefault()
  contextMenuVisible.value = true
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuTargetFolder.value = data
}

// 创建子文件夹
const createSubFolder = () => {
  if (!contextMenuTargetFolder.value) return
  newFolderVisible.value = true
  newFolderName.value = ''
  hideContextMenu()
}

// 隐藏右键菜单
const hideContextMenu = () => {
  contextMenuVisible.value = false
  contextMenuTargetFolder.value = null
}

// 点击页面其他地方隐藏右键菜单
document.addEventListener('click', () => {
  hideContextMenu()
})

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
  instantUploadFiles.value = []
  uploadChunkInfo.value = {}
}

const resetForm = () => {
  formRef.value?.resetFields()
  clearAllFiles()
  clearFolderSelection()
  uploadForm.value = {
    department: userStore.userInfo?.department || '',
    parentId: null,
    parentPath: '',
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
  instantUploadFiles.value = []
  uploadChunkInfo.value = {}

  for (let i = 0; i < fileList.value.length; i++) {
    if (uploadedFiles.value.includes(fileList.value[i].uid)) continue
    uploadingIndex.value = i
    uploadProgress.value = 0
    try {
      await uploadFile(fileList.value[i].raw, fileList.value[i].uid)
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

const uploadFile = async (file, uid) => {
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
    ElMessage.success({
      message: '检测到文件已存在，秒传成功！',
      grouping: true
    })
    if (uid) {
      instantUploadFiles.value.push(uid)
    }
  } else {
    const uploadId = initRes.data.uploadId
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
    
    if (uid) {
      uploadChunkInfo.value[uid] = {
        current: 0,
        total: totalChunks
      }
    }

    for (let i = 0; i < totalChunks; i++) {
      if (uid && uploadChunkInfo.value[uid]) {
        uploadChunkInfo.value[uid].current = i + 1
      }
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
    keywords: uploadForm.value.keywords || '',
    department: uploadForm.value.department,
    parentId: uploadForm.value.parentId,  // 存放位置
    fileId: fileDTO.id,
    author: userInfo.realName || userInfo.username || '未知',
    createBy: userInfo.username,
    status: 'PENDING'  // 设置状态为待审核
  })
  
  if (knowledgeRes.code !== 200) {
    throw new Error(knowledgeRes.message || '创建知识条目失败')
  }
  
  // 创建知识后，自动提交审核（创建审核记录）
  try {
    await api.post(`/knowledge/${knowledgeRes.data.id}/submit-audit?userId=${userInfo.id}`)
  } catch (error) {
    console.warn('提交审核失败，但知识已创建', error)
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
  background: #f6f8fa;
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
  margin-left: 16px;
  min-width: 100px;
  text-align: right;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-shrink: 0;
}

.uploading-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.chunk-info {
  font-size: 12px;
  color: #909399;
  transform: scale(0.9);
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
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

/* 文件夹选择对话框 */
.folder-dialog-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.new-folder-input {
  margin-bottom: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.new-folder-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.new-folder-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #409eff;
}

.new-folder-hint .el-icon {
  font-size: 16px;
}

.new-folder-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-folder-row .el-input {
  flex: 1;
}

.folder-tree-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 9999;
  min-width: 140px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.2s;
}

.context-menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.context-menu-item .el-icon {
  font-size: 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-node .el-icon {
  color: #409eff;
}

.selected-path {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #f0f7ff;
  border-radius: 4px;
  font-size: 13px;
  color: #409eff;
}

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.empty-tip .el-icon {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 12px;
}

.empty-tip p {
  margin: 0;
  font-size: 14px;
}

.empty-tip .sub {
  font-size: 12px;
  margin-top: 8px;
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
.file-list::-webkit-scrollbar,
.folder-tree-container::-webkit-scrollbar {
  width: 6px;
}

.file-list::-webkit-scrollbar-track,
.folder-tree-container::-webkit-scrollbar-track {
  background: transparent;
}

.file-list::-webkit-scrollbar-thumb,
.folder-tree-container::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.file-list::-webkit-scrollbar-thumb:hover,
.folder-tree-container::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>
