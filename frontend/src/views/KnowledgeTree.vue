<template>
  <div class="knowledge-tree-page">
    <div class="page-header">
      <h2>知识结构管理</h2>
      <div class="header-actions">
        <el-radio-group v-model="viewMode" @change="handleViewModeChange">
          <el-radio-button value="visual">图画模式</el-radio-button>
          <el-radio-button value="list">列表格式</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="showAddDialog = true">
          添加知识节点
        </el-button>
      </div>
    </div>

    <!-- 可视化视图 -->
    <el-card v-if="viewMode === 'visual'" class="visual-tree-card">
      <div class="tree-toolbar">
        <div class="toolbar-left">
          <el-button-group>
            <el-button @click="zoomIn" :icon="ZoomIn">放大</el-button>
            <el-button @click="zoomOut" :icon="ZoomOut">缩小</el-button>
            <el-button @click="resetZoom" :icon="Refresh">重置</el-button>
          </el-button-group>
          <el-button @click="fitToScreen" style="margin-left: 10px">适应屏幕</el-button>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索节点并定位..."
            style="width: 250px; margin-right: 10px"
            clearable
            @keyup.enter="handleSearch"
            @input="handleSearchInput"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="showNodeList = !showNodeList" style="margin-right: 10px">
            {{ showNodeList ? '隐藏节点列表' : '显示节点列表' }}
          </el-button>
          <el-button @click="refreshTree">刷新</el-button>
        </div>
      </div>
      
      <div class="visual-tree-wrapper">
        <div class="visual-tree-container" ref="treeContainer">
          <svg ref="svgRef" class="tree-svg"></svg>
        </div>
        <!-- 节点列表侧边栏 -->
        <div v-if="showNodeList" class="node-list-sidebar">
          <div class="node-list-header">
            <h4>节点列表</h4>
            <el-icon class="close-icon" @click="showNodeList = false"><Close /></el-icon>
          </div>
          <div class="node-list-content">
            <el-input
              v-model="nodeListSearch"
              placeholder="搜索节点..."
              size="small"
              style="margin-bottom: 10px"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <div class="node-list-items">
              <div
                v-for="node in filteredNodeList"
                :key="node.id"
                class="node-list-item"
                :class="{ 'highlighted': highlightedNode === node.id }"
                @click="locateNode(node.id)"
              >
                <div class="node-item-title">{{ node.title }}</div>
                <div class="node-item-path">{{ getNodePath(node.id) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 可视化模式节点操作工具栏 -->
    <div v-if="viewMode === 'visual' && selectedVisualNode" class="visual-node-toolbar">
      <div class="node-toolbar-content">
        <span class="selected-node-title">{{ selectedVisualNode.title }}</span>
        <div class="node-toolbar-actions">
          <el-button 
            v-if="isFolder(selectedVisualNode)" 
            size="small" 
            type="primary" 
            @click="handleAddChildToVisualNode"
          >
            <el-icon><Plus /></el-icon>
            添加子节点
          </el-button>
          <el-button 
            v-if="isFolder(selectedVisualNode)" 
            size="small" 
            type="success" 
            @click="handleUploadToFolder"
          >
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <el-button 
            size="small" 
            type="primary" 
            @click="handleAddSiblingToVisualNode"
          >
            <el-icon><Plus /></el-icon>
            添加同级节点
          </el-button>
          <el-button size="small" @click="handleEditVisualNode">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDeleteVisualNode">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
          <el-button size="small" @click="viewDetail(selectedVisualNode.id)">
            <el-icon><Document /></el-icon>
            查看详情
          </el-button>
          <el-icon class="close-toolbar" @click="selectedVisualNode = null"><Close /></el-icon>
        </div>
      </div>
    </div>


    <!-- 列表格式（文件系统树状结构） -->
    <el-card v-else-if="viewMode === 'list'" class="list-tree-card">
      <div class="tree-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="listSearchKeyword"
            placeholder="搜索节点..."
            style="width: 250px; margin-right: 10px"
            clearable
            @input="handleListTreeSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="handleNewFolder">
            <el-icon><Plus /></el-icon>
            新建文件夹
          </el-button>
          <el-button @click="expandAll">展开全部</el-button>
          <el-button @click="collapseAll">折叠全部</el-button>
          <el-button @click="refreshTree">刷新</el-button>
        </div>
      </div>
      
      <div 
        class="tree-list-container"
        @contextmenu.prevent="showContextMenuOnBlank($event)"
      >
        <el-tree
          ref="listTreeRef"
          :data="filteredTreeData"
          :props="treeProps"
          node-key="id"
          :default-expand-all="false"
          :filter-node-method="filterTreeNode"
          draggable
          :allow-drop="allowDrop"
          :allow-drag="allowDrag"
          @node-drop="handleNodeDrop"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div 
              class="tree-node-item"
              @contextmenu.prevent="showContextMenu($event, node, data)"
            >
              <el-icon class="node-icon" :class="{ 'folder-icon': isFolder(data), 'file-icon': !isFolder(data) }">
                <Folder v-if="isFolder(data)" />
                <Document v-else />
              </el-icon>
              <span class="node-label">{{ data.title }}</span>
            </div>
          </template>
        </el-tree>
      </div>
    </el-card>

    <!-- 管理视图（用于拖拽管理） -->
    <el-card v-else-if="viewMode === 'manage'">
      <div class="tree-toolbar">
        <el-button @click="expandAll">展开全部</el-button>
        <el-button @click="collapseAll">折叠全部</el-button>
        <el-button @click="refreshTree">刷新</el-button>
      </div>

      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        default-expand-all
        draggable
        :allow-drop="allowDrop"
        :allow-drag="allowDrag"
        @node-drop="handleNodeDrop"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span class="node-label">{{ data.title }}</span>
            <div class="node-actions">
              <el-button size="small" text type="primary" @click.stop="viewDetail(data.id)">
                查看
              </el-button>
              <el-button size="small" text type="warning" @click.stop="editNode(data)">
                编辑
              </el-button>
              <el-button 
                v-if="isFolder(data)" 
                size="small" 
                text 
                type="success" 
                @click.stop="addChildNode(data)"
              >
                添加子节点
              </el-button>
              <el-button 
                v-if="isFolder(data)" 
                size="small" 
                text 
                type="primary" 
                @click.stop="uploadToFolder(data)"
              >
                <el-icon><Upload /></el-icon>
                上传文件
              </el-button>
              <el-button size="small" text type="danger" @click.stop="deleteNode(data)">
                删除
              </el-button>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <!-- 右键菜单 -->
    <div 
      v-if="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <!-- 只有文件夹或根目录才能添加子节点 -->
      <template v-if="!contextMenuData || !contextMenuData.id || isFolder(contextMenuData)">
        <div class="context-menu-item" @click="handleContextMenuClick('newFolder')">
          <el-icon><Plus /></el-icon>
          <span>新建文件夹</span>
        </div>
        <div class="context-menu-item" @click="handleContextMenuClick('newFile')">
          <el-icon><Document /></el-icon>
          <span>新建文件</span>
        </div>
        <div class="context-menu-divider" v-if="contextMenuData && contextMenuData.id && isFolder(contextMenuData)"></div>
        <div 
          v-if="contextMenuData && contextMenuData.id && isFolder(contextMenuData)" 
          class="context-menu-item" 
          @click="handleContextMenuClick('uploadFile')"
        >
          <el-icon><Upload /></el-icon>
          <span>上传文件</span>
        </div>
      </template>
      <!-- 添加同级节点（所有节点都可以） -->
      <template v-if="contextMenuData && contextMenuData.id">
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextMenuClick('newSiblingFolder')">
          <el-icon><Plus /></el-icon>
          <span>添加同级文件夹</span>
        </div>
        <div class="context-menu-item" @click="handleContextMenuClick('newSiblingFile')">
          <el-icon><Document /></el-icon>
          <span>添加同级文件</span>
        </div>
      </template>
      <template v-if="contextMenuData && contextMenuData.id">
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextMenuClick('viewDetail')">
          <el-icon><Document /></el-icon>
          <span>查看详情</span>
        </div>
        <div class="context-menu-item" @click="handleContextMenuClick('edit')">
          <el-icon><Edit /></el-icon>
          <span>编辑</span>
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextMenuClick('rename')">
          <el-icon><Edit /></el-icon>
          <span>重命名</span>
        </div>
        <div class="context-menu-item" @click="handleContextMenuClick('move')">
          <el-icon><CopyDocument /></el-icon>
          <span>移动到</span>
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="handleContextMenuClick('delete')">
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </div>
      </template>
    </div>

    <!-- 移动节点对话框 -->
    <el-dialog 
      v-model="showMoveDialog" 
      title="移动到"
      width="500px"
    >
      <el-tree
        ref="moveTreeRef"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        :default-expand-all="false"
        :highlight-current="true"
        @node-click="handleMoveTargetSelect"
      >
        <template #default="{ node, data }">
          <div class="move-tree-node">
            <el-icon><FolderOpened /></el-icon>
            <span>{{ data.title }}</span>
          </div>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="showMoveDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmMove" :disabled="!moveTargetId">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog 
      v-model="showRenameDialog" 
      title="重命名"
      width="400px"
    >
      <el-input v-model="renameValue" placeholder="请输入新名称" />
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRename" :disabled="!renameValue.trim()">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑节点对话框 -->
    <el-dialog 
      v-model="showAddDialog" 
      :title="editingNode ? '编辑知识节点' : (isNewFolder ? '新建文件夹' : '添加知识节点')"
      width="600px"
    >
      <el-form :model="nodeForm" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="nodeForm.title" placeholder="请输入知识标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="nodeForm.summary" type="textarea" :rows="3" placeholder="请输入摘要" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="nodeForm.keywords" placeholder="请输入关键词，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveNode">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="700px" @close="handleUploadDialogClose">
      <el-upload
        class="upload-demo"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :file-list="fileList"
        multiple
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
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
      <div v-if="fileList.length > 0" class="upload-form" style="margin-top: 20px">
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="摘要">
            <el-input v-model="uploadForm.summary" type="textarea" :rows="3" placeholder="请输入文件摘要" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="uploadForm.keywords" placeholder="请输入关键词，多个用逗号分隔" />
          </el-form-item>
        </el-form>
      </div>

      <div v-if="uploading" class="upload-progress" style="margin: 20px 0">
        <el-progress :percentage="uploadProgress" />
        <p style="margin-top: 10px">上传中：{{ currentFileName }}</p>
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="startUpload" 
          :disabled="fileList.length === 0 || uploading"
          :loading="uploading"
        >
          开始上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ZoomIn, ZoomOut, Refresh, Search, Close, Document, Folder, FolderOpened, Plus, Delete, Edit, CopyDocument, Files, UploadFilled, Upload } from '@element-plus/icons-vue'
import * as d3 from 'd3'
import CryptoJS from 'crypto-js'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const treeRef = ref(null)
const treeData = ref([])
const showAddDialog = ref(false)
const editingNode = ref(null)
const parentNodeId = ref(null)

// 视图模式：visual（可视化）或 list（列表）
const viewMode = ref('visual')

// 可视化相关
const svgRef = ref(null)
const treeContainer = ref(null)
let svg = null
let g = null
let zoom = null
let tree = null
let root = null
let currentTransform = d3.zoomIdentity

// 搜索相关
const searchKeyword = ref('')
let highlightedNode = null

// 节点列表相关
const showNodeList = ref(false)
const nodeListSearch = ref('')
const allNodes = ref([]) // 存储所有节点用于列表显示
const nodePathMap = ref({}) // 存储节点路径映射

// 列表格式相关
const listTreeRef = ref(null)
const listSearchKeyword = ref('')
const filteredTreeData = ref([])

// 右键菜单相关
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuNode = ref(null)
const contextMenuData = ref(null)

// 移动节点相关
const showMoveDialog = ref(false)
const moveTreeRef = ref(null)
const moveTargetId = ref(null)
const moveSourceId = ref(null)

// 上传相关
const showUploadDialog = ref(false)
const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const currentFileName = ref('')
const uploadForm = ref({
  parentId: null,
  summary: '',
  keywords: ''
})
const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB

// 重命名相关
const showRenameDialog = ref(false)
const renameValue = ref('')
const renameNodeId = ref(null)

// 新建文件夹标识
const isNewFolder = ref(false)

// 可视化模式选中的节点
const selectedVisualNode = ref(null)
const selectedListNode = ref(null)

const treeProps = {
  children: 'children',
  label: 'title'
}

const nodeForm = ref({
  title: '',
  summary: '',
  keywords: ''
})

// 加载知识树
const loadTree = async () => {
  try {
    const res = await api.get('/knowledge/tree')
    if (res.code === 200) {
      const flatData = res.data || []
      treeData.value = buildTree(flatData)
      
      // 根据当前视图模式更新显示
      if (viewMode.value === 'visual') {
        await nextTick()
        initVisualTree(flatData)
      } else if (viewMode.value === 'list') {
        initListTree()
      }
    } else {
      ElMessage.error(res.message || '加载知识树失败')
    }
  } catch (error) {
    console.error('加载知识树失败', error)
    ElMessage.error('加载知识树失败')
  }
}

// 构建树形结构
const buildTree = (list) => {
  const map = {}
  const roots = []
  
  // 创建映射
  list.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })
  
  // 构建树
  list.forEach(item => {
    if (item.parentId) {
      const parent = map[item.parentId]
      if (parent) {
        parent.children.push(map[item.id])
      } else {
        roots.push(map[item.id])
      }
    } else {
      roots.push(map[item.id])
    }
  })
  
  // 按sortOrder排序
  const sortChildren = (nodes) => {
    nodes.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    nodes.forEach(node => {
      if (node.children && node.children.length > 0) {
        sortChildren(node.children)
      }
    })
  }
  
  sortChildren(roots)
  return roots
}

// 展开全部
const expandAll = () => {
  // 根据当前视图模式选择对应的 tree ref
  const tree = viewMode.value === 'list' ? listTreeRef.value : treeRef.value
  if (tree) {
    const nodes = tree.store?.nodesMap
    if (nodes) {
      Object.values(nodes).forEach(node => {
        if (node.childNodes && node.childNodes.length > 0) {
          node.expanded = true
        }
      })
    }
  }
}

// 折叠全部
const collapseAll = () => {
  // 根据当前视图模式选择对应的 tree ref
  const tree = viewMode.value === 'list' ? listTreeRef.value : treeRef.value
  if (tree) {
    const nodes = tree.store?.nodesMap
    if (nodes) {
      Object.values(nodes).forEach(node => {
        node.expanded = false
      })
    }
  }
}

// 刷新树
const refreshTree = () => {
  selectedVisualNode.value = null
  selectedListNode.value = null
  loadTree()
}

// 允许拖拽
const allowDrag = (node) => {
  // 所有节点都可以拖拽
  return true
}

// 允许放置
const allowDrop = (draggingNode, dropNode, type) => {
  // 不能拖到自己
  if (draggingNode.data.id === dropNode.data.id) {
    return false
  }
  
  // 不能拖到自己的子节点
  const isDescendant = (parent, child) => {
    if (!child.parent) return false
    if (child.parent.data.id === parent.data.id) return true
    return isDescendant(parent, child.parent)
  }
  
  // 如果拖到文件夹内部，检查是否是自己的子节点
  if (type === 'inner') {
    if (isDescendant(draggingNode, dropNode)) {
      return false
    }
    // 只有文件夹才能作为放置目标（内部放置）
    // 如果目标节点是文件，不允许内部放置
    if (!isFolder(dropNode.data)) {
      return false
    }
  }
  
  return true
}

// 处理节点拖拽
const handleNodeDrop = async (draggingNode, dropNode, dropType, ev) => {
  try {
    const newParentId = dropType === 'inner' ? dropNode.data.id : (dropNode.parent ? dropNode.parent.data.id : null)
    const newSortOrder = dropType === 'inner' 
      ? (dropNode.data.children?.length || 0) 
      : (dropNode.nextSibling ? dropNode.nextSibling.data.sortOrder : (dropNode.data.sortOrder + 1))
    
    const res = await api.put(`/knowledge/${draggingNode.data.id}/move`, {
      parentId: newParentId,
      sortOrder: newSortOrder
    })
    
    if (res.code === 200) {
      ElMessage.success('移动成功')
      await loadTree()
    } else {
      ElMessage.error(res.message || '移动失败')
      await loadTree() // 恢复原状态
    }
  } catch (error) {
    ElMessage.error('移动失败: ' + (error.response?.data?.message || error.message))
    await loadTree() // 恢复原状态
  }
}

// 点击节点
const handleNodeClick = (data) => {
  // 所有模式下，点击节点直接查看详情
  viewDetail(data.id)
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

// 编辑节点
const editNode = (data) => {
  editingNode.value = data
  nodeForm.value = {
    title: data.title || '',
    category: data.category || '',
    summary: data.summary || '',
    keywords: data.keywords || ''
  }
  parentNodeId.value = data.parentId
  showAddDialog.value = true
}

// 添加子节点
const addChildNode = (data) => {
  editingNode.value = null
  nodeForm.value = {
    title: '',
    category: data.category || '',
    summary: '',
    keywords: ''
  }
  parentNodeId.value = data.id
  showAddDialog.value = true
}

// 删除节点
const deleteNode = async (data) => {
  try {
    await ElMessageBox.confirm(`确定要删除知识《${data.title}》吗？`, '提示', {
      type: 'warning'
    })
    
    const res = await api.delete(`/knowledge/${data.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadTree()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 保存节点
const saveNode = async () => {
  if (!nodeForm.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  
  try {
    if (editingNode.value) {
      // 更新
      const res = await api.put(`/knowledge/${editingNode.value.id}`, {
        ...nodeForm.value,
        parentId: parentNodeId.value
      })
      if (res.code === 200) {
        ElMessage.success('更新成功')
        showAddDialog.value = false
        await loadTree()
      } else {
        ElMessage.error(res.message || '更新失败')
      }
    } else {
      // 创建
      const res = await api.post('/knowledge', {
        ...nodeForm.value,
        parentId: parentNodeId.value,
        status: 'DRAFT'
      })
      if (res.code === 200) {
        ElMessage.success('创建成功')
        showAddDialog.value = false
        nodeForm.value = { title: '', category: '', summary: '', keywords: '' }
        parentNodeId.value = null
        await loadTree()
      } else {
        ElMessage.error(res.message || '创建失败')
      }
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

// 初始化可视化树
const initVisualTree = (data) => {
  if (!svgRef.value || !treeContainer.value) return
  
  // 清空之前的绘制
  d3.select(svgRef.value).selectAll('*').remove()
  
  const containerWidth = treeContainer.value.clientWidth
  const containerHeight = treeContainer.value.clientHeight || 800
  
  // 设置SVG尺寸
  svg = d3.select(svgRef.value)
    .attr('width', containerWidth)
    .attr('height', containerHeight)
  
  // 创建主组
  g = svg.append('g')
  
  // 构建树形数据
  const treeData = buildTree(data)
  if (treeData.length === 0) {
    ElMessage.info('暂无知识数据')
    return
  }
  
  // 创建根节点（如果有多个根节点，创建一个虚拟根节点）
  if (treeData.length > 1) {
    root = { id: 'root', title: '知识库', children: treeData }
  } else {
    root = treeData[0]
  }
  
  // 使用力导向图布局（类似GraphVis效果）
  // 准备节点和边的数据
  const nodes = []
  const links = []
  
  // 遍历树结构，提取节点和边
  const traverse = (node, parentId = null) => {
    const nodeId = node.id || `node-${nodes.length}`
    nodes.push({
      id: nodeId,
      title: node.title || '未命名',
      ...node
    })
    
    if (parentId) {
      links.push({
        source: parentId,
        target: nodeId
      })
    }
    
    if (node.children && node.children.length > 0) {
      node.children.forEach(child => traverse(child, nodeId))
    }
  }
  
  if (treeData.length > 1) {
    // 多个根节点，创建虚拟根节点
    const virtualRoot = { id: 'root', title: '知识库', children: treeData }
    traverse(virtualRoot)
  } else {
    treeData.forEach(node => traverse(node))
  }
  
  // 创建力导向图模拟
  const simulation = d3.forceSimulation(nodes)
    .force('link', d3.forceLink(links).id(d => d.id).distance(150))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(containerWidth / 2, containerHeight / 2))
    .force('collision', d3.forceCollide().radius(60))
  
  // 保存simulation引用以便后续使用
  root = { nodes, links, simulation }
  
  // 收集所有节点用于列表显示
  const treeRoot = d3.hierarchy(root.nodes.find(n => n.id === 'root') || root.nodes[0] || { id: 'root', title: '知识库', children: [] })
  
  // 设置缩放和平移 - 只允许画布拖动，节点本身不能拖动
  zoom = d3.zoom()
    .scaleExtent([0.1, 3])
    .on('zoom', (event) => {
      currentTransform = event.transform
      g.attr('transform', event.transform)
    })
    .filter((event) => {
      // 只允许鼠标中键、右键或按住Ctrl/Cmd键时拖动画布
      // 或者点击空白区域拖动
      return event.type === 'wheel' || 
             (event.type === 'mousedown' && (event.button === 1 || event.button === 2 || event.ctrlKey || event.metaKey)) ||
             (event.type === 'mousedown' && event.target === svg.node())
    })
  
  svg.call(zoom)
  
  // 添加鼠标中键和右键拖动支持
  svg.on('mousedown', function(event) {
    if (event.button === 1 || event.button === 2) {
      event.preventDefault()
      const startX = event.clientX
      const startY = event.clientY
      const startTransform = currentTransform
      
      const mousemove = (e) => {
        const dx = e.clientX - startX
        const dy = e.clientY - startY
        if (startTransform && startTransform.k && !isNaN(startTransform.k)) {
          currentTransform = startTransform.translate(dx / startTransform.k, dy / startTransform.k)
          if (g) {
            g.attr('transform', currentTransform)
          }
        }
      }
      
      const mouseup = () => {
        document.removeEventListener('mousemove', mousemove)
        document.removeEventListener('mouseup', mouseup)
      }
      
      document.addEventListener('mousemove', mousemove)
      document.addEventListener('mouseup', mouseup)
    }
  })
  
  // 允许右键菜单（用于节点操作）
  svg.on('contextmenu', (event) => {
    // 如果点击的是节点，不阻止默认行为，让节点自己处理
    if (event.target.closest('.node')) {
      return
    }
    // 如果点击的是空白处，显示根目录菜单
    event.preventDefault()
    showContextMenuOnVisualBlank(event)
  })
  
  // 初始化 currentTransform
  currentTransform = d3.zoomIdentity
  
  // 收集所有节点用于列表显示
  collectAllNodes(treeRoot)
  
  // 绘制力导向图
  drawForceGraph(root.nodes, root.links, root.simulation)
}

// 绘制力导向图
const drawForceGraph = (nodes, links, simulation) => {
  // 绘制连线
  const link = g.selectAll('.link')
    .data(links)
    .enter()
    .append('line')
    .attr('class', 'link')
    .attr('stroke', '#999')
    .attr('stroke-opacity', 0.6)
    .attr('stroke-width', 2)
  
  // 绘制节点
  const node = g.selectAll('.node')
    .data(nodes)
    .enter()
    .append('g')
    .attr('class', 'node')
    .style('cursor', 'pointer')
    .call(d3.drag()
      .on('start', dragstarted)
      .on('drag', dragged)
      .on('end', dragended)
    )
    .on('click', (event, d) => {
      event.stopPropagation()
      if (d.id !== 'root') {
        focusForceNode(d)
        // 选中节点用于操作
        selectedVisualNode.value = d
      }
    })
    .on('dblclick', (event, d) => {
      event.stopPropagation()
      if (d.id !== 'root') {
        viewDetail(d.id)
      }
    })
    .on('contextmenu', (event, d) => {
      event.stopPropagation()
      event.preventDefault()
      if (d.id !== 'root') {
        showContextMenuOnVisualNode(event, d)
      }
    })
  
  // 绘制节点圆圈
  node.append('circle')
    .attr('r', d => d.id === 'root' ? 25 : 20)
    .attr('fill', d => {
      if (d.id === 'root') return '#409EFF'
      if (highlightedNode === d.id) return '#f56c6c'
      return '#fff'
    })
    .attr('stroke', d => {
      if (highlightedNode === d.id) return '#f56c6c'
      return '#409EFF'
    })
    .attr('stroke-width', d => highlightedNode === d.id ? 3 : 2)
    .on('mouseenter', function(event, d) {
      d3.select(this).attr('fill', '#e6f7ff')
    })
    .on('mouseleave', function(event, d) {
      d3.select(this).attr('fill', d.id === 'root' ? '#409EFF' : (highlightedNode === d.id ? '#f56c6c' : '#fff'))
    })
  
  // 绘制节点文本
  node.append('text')
    .attr('class', 'node-text')
    .attr('dy', 35)
    .attr('text-anchor', 'middle')
    .attr('fill', '#333')
    .attr('font-size', '12px')
    .text(d => {
      const title = d.title || '未命名'
      return title.length > 12 ? title.substring(0, 12) + '...' : title
    })
  
  // 更新位置
  simulation.on('tick', () => {
    link
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y)
    
    node.attr('transform', d => `translate(${d.x},${d.y})`)
  })
  
  // 拖拽函数
  function dragstarted(event, d) {
    if (!event.active) simulation.alphaTarget(0.3).restart()
    d.fx = d.x
    d.fy = d.y
  }
  
  function dragged(event, d) {
    d.fx = event.x
    d.fy = event.y
  }
  
  function dragended(event, d) {
    if (!event.active) simulation.alphaTarget(0)
    d.fx = null
    d.fy = null
  }
}

// 聚焦力导向图节点
const focusForceNode = (d) => {
  if (!root || !root.simulation) return
  
  // 高亮节点
  highlightedNode = d.id
  
  // 更新节点样式
  if (g) {
    g.selectAll('.node circle')
      .attr('fill', node => {
        if (node.id === 'root') return '#409EFF'
        if (node.id === highlightedNode) return '#f56c6c'
        return '#fff'
      })
      .attr('stroke', node => {
        if (node.id === highlightedNode) return '#f56c6c'
        return '#409EFF'
      })
      .attr('stroke-width', node => node.id === highlightedNode ? 3 : 2)
  }
  
  // 将节点移动到中心
  const centerX = treeContainer.value.clientWidth / 2
  const centerY = treeContainer.value.clientHeight / 2
  
  d.fx = centerX
  d.fy = centerY
  
  // 重新启动模拟
  root.simulation.alpha(1).restart()
  
  // 延迟释放固定位置，让节点自然分布
  setTimeout(() => {
    d.fx = null
    d.fy = null
  }, 1000)
}

// 绘制树
const drawTree = (root) => {
  // 绘制连线 - 使用直线连接（树形结构）
  const links = g.selectAll('.link')
    .data(root.links())
    .enter()
    .append('path')
    .attr('class', 'link')
    .attr('d', d => {
      // 垂直树形：从父节点底部到子节点顶部
      return `M${d.source.y},${d.source.x + 20}L${d.target.y},${d.target.x - 20}`
    })
    .attr('fill', 'none')
    .attr('stroke', '#ccc')
    .attr('stroke-width', 1.5)
  
  // 绘制节点组
  const nodeGroups = g.selectAll('.node')
    .data(root.descendants())
    .enter()
    .append('g')
    .attr('class', 'node')
    .attr('transform', d => `translate(${d.y},${d.x})`)
    .style('cursor', 'pointer')
    .on('click', (event, d) => {
      event.stopPropagation()
      if (d.data.id !== 'root') {
        focusNode(d)
      }
    })
    .on('dblclick', (event, d) => {
      event.stopPropagation()
      if (d.data.id !== 'root') {
        viewDetail(d.data.id)
      }
    })
    // 禁用节点本身的拖拽，只允许画布拖拽
    .call(d3.drag()
      .on('start', () => {}) // 空函数，禁用节点拖拽
      .on('drag', () => {})
      .on('end', () => {})
    )
  
  // 绘制节点背景
  nodeGroups.append('rect')
    .attr('class', 'node-bg')
    .attr('x', -60)
    .attr('y', -20)
    .attr('width', 120)
    .attr('height', 40)
    .attr('rx', 5)
    .attr('fill', d => d.data.id === 'root' ? '#409EFF' : '#fff')
    .attr('stroke', d => highlightedNode === d.data.id ? '#f56c6c' : '#409EFF')
    .attr('stroke-width', d => highlightedNode === d.data.id ? 3 : 2)
    .on('mouseenter', function(event, d) {
      d3.select(this).attr('fill', '#e6f7ff')
    })
    .on('mouseleave', function(event, d) {
      d3.select(this).attr('fill', d.data.id === 'root' ? '#409EFF' : '#fff')
    })
  
  // 绘制节点文本
  nodeGroups.append('text')
    .attr('class', 'node-text')
    .attr('text-anchor', 'middle')
    .attr('dy', 5)
    .attr('fill', d => d.data.id === 'root' ? '#fff' : '#333')
    .attr('font-size', '12px')
    .text(d => {
      const title = d.data.title || '未命名'
      return title.length > 10 ? title.substring(0, 10) + '...' : title
    })
}

// 聚焦节点
const focusNode = (d) => {
  if (!d || !svg) return
  
  const scale = 1.5
  const x = -d.y * scale + (treeContainer.value.clientWidth / 2)
  const y = -d.x * scale + (treeContainer.value.clientHeight / 2)
  
  currentTransform = d3.zoomIdentity
    .translate(x, y)
    .scale(scale)
  
  svg.transition()
    .duration(750)
    .call(zoom.transform, currentTransform)
  
  // 高亮节点
  highlightedNode = d.data.id
  g.selectAll('.node-bg')
    .attr('stroke', function() {
      const nodeData = d3.select(this.parentElement).datum()
      return nodeData.data.id === highlightedNode ? '#f56c6c' : '#409EFF'
    })
    .attr('stroke-width', function() {
      const nodeData = d3.select(this.parentElement).datum()
      return nodeData.data.id === highlightedNode ? 3 : 2
    })
}

// 缩放控制
const zoomIn = () => {
  if (!svg) return
  currentTransform = currentTransform.scale(1.2)
  svg.transition().duration(300).call(zoom.transform, currentTransform)
}

const zoomOut = () => {
  if (!svg) return
  currentTransform = currentTransform.scale(1 / 1.2)
  svg.transition().duration(300).call(zoom.transform, currentTransform)
}

const resetZoom = () => {
  if (!svg || !treeContainer.value) return
  
  // 如果是力导向图，直接重置到中心
  if (root && root.simulation) {
    currentTransform = d3.zoomIdentity
    svg.transition().duration(500).call(zoom.transform, currentTransform)
    return
  }
  
  // 树形布局的重置逻辑
  const containerWidth = treeContainer.value.clientWidth
  const containerHeight = treeContainer.value.clientHeight || 800
  
  try {
    const bounds = g.node().getBBox()
    const width = bounds.width
    const height = bounds.height
    
    if (width > 0 && height > 0) {
      const initialScale = Math.min(containerWidth / width, containerHeight / height) * 0.8
      const initialX = (containerWidth - width * initialScale) / 2 - bounds.x * initialScale
      const initialY = (containerHeight - height * initialScale) / 2 - bounds.y * initialScale
      
      currentTransform = d3.zoomIdentity
        .translate(initialX, initialY)
        .scale(initialScale)
      
      svg.transition().duration(500).call(zoom.transform, currentTransform)
    } else {
      // 如果无法获取边界，直接重置
      currentTransform = d3.zoomIdentity
      svg.transition().duration(500).call(zoom.transform, currentTransform)
    }
  } catch (e) {
    // 如果出错，直接重置
    currentTransform = d3.zoomIdentity
    svg.transition().duration(500).call(zoom.transform, currentTransform)
  }
}

const fitToScreen = () => {
  resetZoom()
}

// 收集所有节点
const collectAllNodes = (treeRoot) => {
  allNodes.value = []
  nodePathMap.value = {}
  
  const traverse = (node, path = []) => {
    if (node.data.id !== 'root') {
      const currentPath = [...path, node.data.title]
      allNodes.value.push({
        id: node.data.id,
        title: node.data.title,
        path: currentPath
      })
      nodePathMap.value[node.data.id] = currentPath.join(' / ')
    }
    
    if (node.children) {
      node.children.forEach(child => {
        traverse(child, node.data.id === 'root' ? [] : [...path, node.data.title])
      })
    }
  }
  
  traverse(treeRoot)
}

// 获取节点路径
const getNodePath = (nodeId) => {
  return nodePathMap.value[nodeId] || ''
}

// 过滤节点列表
const filteredNodeList = computed(() => {
  if (!nodeListSearch.value.trim()) {
    return allNodes.value
  }
  const keyword = nodeListSearch.value.toLowerCase()
  return allNodes.value.filter(node => 
    node.title.toLowerCase().includes(keyword) ||
    node.path.some(p => p.toLowerCase().includes(keyword))
  )
})

// 定位节点
const locateNode = (nodeId) => {
  if (!root || !svg) return
  
  const findNodeInTree = (node) => {
    if (node.data.id === nodeId) {
      return node
    }
    if (node.children) {
      for (const child of node.children) {
        const found = findNodeInTree(child)
        if (found) return found
      }
    }
    return null
  }
  
  const treeRoot = d3.hierarchy(root)
  const targetNode = findNodeInTree(treeRoot)
  
  if (targetNode) {
    focusNode(targetNode)
    highlightedNode = nodeId
  } else {
    ElMessage.warning('未找到该节点')
  }
}

// 搜索输入处理
const handleSearchInput = () => {
  if (!searchKeyword.value.trim()) {
    highlightedNode = null
    if (g && root && root.nodes) {
      // 重置所有节点样式
      g.selectAll('.node circle')
        .attr('fill', d => d.id === 'root' ? '#409EFF' : '#fff')
        .attr('stroke', '#409EFF')
        .attr('stroke-width', 2)
    }
    return
  }
  
  // 实时搜索并高亮
  handleSearch()
}

// 搜索节点
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    highlightedNode = null
    if (g && root && root.nodes) {
      // 重置所有节点样式
      g.selectAll('.node circle')
        .attr('fill', d => d.id === 'root' ? '#409EFF' : '#fff')
        .attr('stroke', '#409EFF')
        .attr('stroke-width', 2)
    }
    return
  }
  
  if (!root || !root.nodes || !svg) return
  
  // 在力导向图中搜索匹配的节点
  const foundNode = root.nodes.find(n => 
    n.id !== 'root' && n.title && n.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
  
  if (foundNode) {
    focusForceNode(foundNode)
  } else {
    ElMessage.warning('未找到匹配的节点')
  }
}

// 切换视图
const handleViewModeChange = (mode) => {
  if (mode === 'visual') {
    nextTick(() => {
      const flatData = flattenTree(treeData.value)
      initVisualTree(flatData)
    })
  } else if (mode === 'list') {
    initListTree()
  }
}

// 初始化列表格式（文件系统树状结构）
const initListTree = () => {
  filteredTreeData.value = JSON.parse(JSON.stringify(treeData.value))
}

// 列表格式搜索
const handleListTreeSearch = (value) => {
  if (listTreeRef.value) {
    listTreeRef.value.filter(value)
  }
}

// 树节点过滤方法
const filterTreeNode = (value, data) => {
  if (!value) return true
  return data.title.toLowerCase().includes(value.toLowerCase())
}

// 判断是否为文件夹（有子节点或者没有文件ID的节点）
const isFolder = (data) => {
  // 如果有子节点，肯定是文件夹
  if (data.children && data.children.length > 0) {
    return true
  }
  // 如果没有文件ID，也认为是文件夹
  if (!data.fileId) {
    return true
  }
  return false
}

// 显示右键菜单
const showContextMenu = (event, node, data) => {
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuNode.value = node
  contextMenuData.value = data
  contextMenuVisible.value = true
  
  // 点击其他地方关闭菜单
  const closeMenu = (e) => {
    if (!e.target.closest('.context-menu')) {
      contextMenuVisible.value = false
      document.removeEventListener('click', closeMenu)
    }
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 在空白处显示右键菜单（根目录菜单）
const showContextMenuOnBlank = (event) => {
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuNode.value = null
  // 创建一个虚拟的根节点数据
  contextMenuData.value = { id: null, title: '根目录', children: [] }
  contextMenuVisible.value = true
  
  // 点击其他地方关闭菜单
  const closeMenu = (e) => {
    if (!e.target.closest('.context-menu')) {
      contextMenuVisible.value = false
      document.removeEventListener('click', closeMenu)
    }
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 在可视化模式空白处显示右键菜单
const showContextMenuOnVisualBlank = (event) => {
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuNode.value = null
  contextMenuData.value = { id: null, title: '根目录', children: [] }
  contextMenuVisible.value = true
  
  const closeMenu = (e) => {
    if (!e.target.closest('.context-menu')) {
      contextMenuVisible.value = false
      document.removeEventListener('click', closeMenu)
    }
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 在可视化模式节点上显示右键菜单
const showContextMenuOnVisualNode = (event, nodeData) => {
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuNode.value = null
  contextMenuData.value = nodeData
  contextMenuVisible.value = true
  
  const closeMenu = (e) => {
    if (!e.target.closest('.context-menu')) {
      contextMenuVisible.value = false
      document.removeEventListener('click', closeMenu)
    }
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 在可视化节点上添加子节点
const handleAddChildToVisualNode = () => {
  if (!selectedVisualNode.value) return
  isNewFolder.value = false
  parentNodeId.value = selectedVisualNode.value.id
  nodeForm.value = {
    title: '',
    summary: '',
    keywords: ''
  }
  showAddDialog.value = true
}

// 在可视化节点上上传文件到文件夹
const handleUploadToFolder = () => {
  if (!selectedVisualNode.value || !isFolder(selectedVisualNode.value)) return
  uploadForm.value.parentId = selectedVisualNode.value.id
  showUploadDialog.value = true
}

// 上传文件到文件夹（通用方法）
const uploadToFolder = (folderData) => {
  if (!folderData || !isFolder(folderData)) return
  uploadForm.value.parentId = folderData.id
  showUploadDialog.value = true
}

// 编辑可视化节点
const handleEditVisualNode = () => {
  if (!selectedVisualNode.value) return
  editNode(selectedVisualNode.value)
}

// 删除可视化节点
const handleDeleteVisualNode = () => {
  if (!selectedVisualNode.value) return
  deleteNode(selectedVisualNode.value)
  selectedVisualNode.value = null
}

// 在可视化节点上添加同级节点
const handleAddSiblingToVisualNode = () => {
  if (!selectedVisualNode.value) return
  handleAddSiblingNode(selectedVisualNode.value, false)
}

// 添加同级节点（通用方法）
const handleAddSiblingNode = (nodeData, isFolder = false) => {
  // 直接使用节点的parentId，不需要调用API
  const parentId = nodeData.parentId || null
  
  // 判断是否是叶子节点（有fileId的节点）
  const isLeafNode = nodeData.fileId != null && nodeData.fileId !== undefined
  
  if (isLeafNode) {
    // 叶子节点添加同级节点 = 上传新文件
    // 设置父节点ID并打开上传对话框
    uploadForm.value.parentId = parentId
    showUploadDialog.value = true
  } else {
    // 文件夹节点添加同级节点 = 创建新节点（文件夹或文件）
    isNewFolder.value = isFolder
    parentNodeId.value = parentId
    nodeForm.value = {
      title: '',
      summary: '',
      keywords: ''
    }
    showAddDialog.value = true
  }
}

// ========== 列表模式操作函数 ==========
// 在列表节点上添加子节点
const handleAddChildToListNode = (nodeData = null) => {
  const targetNode = nodeData || selectedListNode.value
  if (!targetNode || !isFolder(targetNode)) return
  isNewFolder.value = false
  parentNodeId.value = targetNode.id
  nodeForm.value = {
    title: '',
    summary: '',
    keywords: ''
  }
  showAddDialog.value = true
}

// 在列表节点上上传文件到文件夹
const handleUploadToListFolder = () => {
  if (!selectedListNode.value || !isFolder(selectedListNode.value)) return
  uploadForm.value.parentId = selectedListNode.value.id
  showUploadDialog.value = true
}

// 在列表节点上添加同级节点
const handleAddSiblingToListNode = (nodeData = null) => {
  const targetNode = nodeData || selectedListNode.value
  if (!targetNode) return
  handleAddSiblingNode(targetNode, false)
}

// 编辑列表节点
const handleEditListNode = () => {
  if (!selectedListNode.value) return
  editNode(selectedListNode.value)
}

// 删除列表节点
const handleDeleteListNode = () => {
  if (!selectedListNode.value) return
  deleteNode(selectedListNode.value).then(() => {
    selectedListNode.value = null
  })
}

// 处理右键菜单点击
const handleContextMenuClick = (action) => {
  contextMenuVisible.value = false
  const data = contextMenuData.value
  
  // 如果是根目录（id为null），只允许新建操作
  if (!data.id) {
    if (action === 'newFolder') {
      isNewFolder.value = true
      parentNodeId.value = null // 根目录
      nodeForm.value = {
        title: '',
        summary: '',
        keywords: ''
      }
      showAddDialog.value = true
    } else if (action === 'newFile') {
      isNewFolder.value = false
      parentNodeId.value = null // 根目录
      nodeForm.value = {
        title: '',
        summary: '',
        keywords: ''
      }
      showAddDialog.value = true
    }
    return
  }
  
  switch (action) {
    case 'newFolder':
      isNewFolder.value = true
      parentNodeId.value = data.id
      nodeForm.value = {
        title: '',
        summary: '',
        keywords: ''
      }
      showAddDialog.value = true
      break
    case 'newFile':
      isNewFolder.value = false
      parentNodeId.value = data.id
      nodeForm.value = {
        title: '',
        summary: '',
        keywords: ''
      }
      showAddDialog.value = true
      break
    case 'rename':
      renameValue.value = data.title
      renameNodeId.value = data.id
      showRenameDialog.value = true
      break
    case 'move':
      moveSourceId.value = data.id
      moveTargetId.value = null
      showMoveDialog.value = true
      break
    case 'delete':
      deleteNode(data)
      break
    case 'newSiblingFolder':
      // 添加同级文件夹
      handleAddSiblingNode(data, true)
      break
    case 'newSiblingFile':
      // 添加同级文件
      handleAddSiblingNode(data, false)
      break
    case 'uploadFile':
      // 上传文件到文件夹
      uploadToFolder(data)
      break
    case 'viewDetail':
      // 查看详情
      viewDetail(data.id)
      break
    case 'edit':
      // 编辑节点
      editNode(data)
      break
  }
}

// 确认移动
const confirmMove = async () => {
  if (!moveSourceId.value || !moveTargetId.value) {
    ElMessage.warning('请选择目标位置')
    return
  }
  
  if (moveSourceId.value === moveTargetId.value) {
    ElMessage.warning('不能移动到自身')
    return
  }
  
  try {
    const res = await api.put(`/knowledge/${moveSourceId.value}/move`, {
      parentId: moveTargetId.value,
      sortOrder: 0
    })
    
    if (res.code === 200) {
      ElMessage.success('移动成功')
      showMoveDialog.value = false
      moveSourceId.value = null
      moveTargetId.value = null
      await loadTree()
    } else {
      ElMessage.error(res.message || '移动失败')
    }
  } catch (error) {
    ElMessage.error('移动失败: ' + (error.response?.data?.message || error.message))
  }
}

// 选择移动目标
const handleMoveTargetSelect = (data) => {
  moveTargetId.value = data.id
}

// 确认重命名
const confirmRename = async () => {
  if (!renameValue.value.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }
  
  try {
    const res = await api.put(`/knowledge/${renameNodeId.value}`, {
      title: renameValue.value.trim()
    })
    
    if (res.code === 200) {
      ElMessage.success('重命名成功')
      showRenameDialog.value = false
      renameValue.value = ''
      renameNodeId.value = null
      await loadTree()
    } else {
      ElMessage.error(res.message || '重命名失败')
    }
  } catch (error) {
    ElMessage.error('重命名失败: ' + (error.response?.data?.message || error.message))
  }
}

// 新建文件夹（工具栏按钮）
const handleNewFolder = () => {
  isNewFolder.value = true
  parentNodeId.value = null // 根目录
  nodeForm.value = {
    title: '',
    summary: '',
    keywords: ''
  }
  showAddDialog.value = true
}

// 上传相关方法
const handleFileChange = (file, files) => {
  fileList.value = files
}

// 计算文件哈希（对整个文件计算SHA256，与后端保持一致）
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
  
  uploading.value = true
  uploadProgress.value = 0
  
  try {
    for (let i = 0; i < fileList.value.length; i++) {
      const file = fileList.value[i].raw
      currentFileName.value = file.name
      
      // 计算文件哈希
      const fileHash = await calculateFileHash(file)
      
      // 检查文件是否已存在
      let fileDTO = null
      try {
        const checkRes = await api.get(`/file/check/${fileHash}`, {
          validateStatus: (status) => {
            // 404 是正常情况（文件不存在），不应该触发错误提示
            return status === 200 || status === 404
          }
        })
        if (checkRes.code === 200 && checkRes.data) {
          fileDTO = checkRes.data
          ElMessage.info(`${file.name} 已存在，跳过上传`)
          uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
          continue
        }
      } catch (error) {
        // 文件不存在或其他错误，继续上传（不显示错误提示）
        // 404 错误已经被 validateStatus 处理，不会进入这里
      }
      
      // 分片上传
      if (!fileDTO) {
        // 先初始化上传
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
        
        if (initRes.code !== 200) {
          throw new Error(initRes.message || '初始化上传失败')
        }
        
        // 如果秒传，直接使用返回的文件信息
        if (initRes.data.completed && initRes.data.file) {
          fileDTO = initRes.data.file
          uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
        } else {
          // 正常上传流程
          const uploadId = initRes.data.uploadId
          const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
          
          for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            const start = chunkIndex * CHUNK_SIZE
            const end = Math.min(start + CHUNK_SIZE, file.size)
            const chunk = file.slice(start, end)
            
            // 计算分片哈希
            const chunkBuffer = await new Promise((resolve) => {
              const reader = new FileReader()
              reader.onload = (e) => resolve(e.target.result)
              reader.readAsArrayBuffer(chunk)
            })
            const chunkWordArray = CryptoJS.lib.WordArray.create(chunkBuffer)
            const chunkHash = CryptoJS.SHA256(chunkWordArray).toString()
            
            const formData = new FormData()
            formData.append('chunk', chunk)
            formData.append('uploadId', uploadId)
            formData.append('chunkIndex', chunkIndex)
            formData.append('chunkHash', chunkHash)
            
            await api.post('/file/chunk', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })
            
            uploadProgress.value = Math.round(((chunkIndex + 1) / totalChunks) * 100 * (i + 1) / fileList.value.length)
          }
          
          // 完成上传
          const completeRes = await api.post(`/file/complete/${uploadId}`)
          if (completeRes.code !== 200 || !completeRes.data) {
            throw new Error('文件上传完成失败')
          }
          fileDTO = completeRes.data
        }
      }
      
      // 如果文件已存在或上传完成，继续创建知识条目
      if (!fileDTO) {
        throw new Error('文件上传失败')
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
        keywords: uploadForm.value.keywords || '',
        fileId: fileDTO.id,
        parentId: uploadForm.value.parentId,
        author: userInfo.realName || userInfo.username || '未知',
        department: userInfo.department || '未知',
        createBy: userInfo.username
      })
      
      if (knowledgeRes.code !== 200) {
        throw new Error(knowledgeRes.message || '创建知识条目失败')
      }
      
      uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
    }
    
    ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)
    handleUploadDialogClose()
    await loadTree() // 刷新树
  } catch (error) {
    ElMessage.error(`上传失败：${error.message}`)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    currentFileName.value = ''
  }
}

const handleUploadDialogClose = () => {
  showUploadDialog.value = false
  fileList.value = []
  uploadForm.value = {
    parentId: null,
    summary: '',
    keywords: ''
  }
  uploading.value = false
  uploadProgress.value = 0
  currentFileName.value = ''
}

// 扁平化树数据（用于可视化）
const flattenTree = (nodes) => {
  const result = []
  const traverse = (node, parentId = null) => {
    result.push({ ...node, parentId })
    if (node.children) {
      node.children.forEach(child => traverse(child, node.id))
    }
  }
  nodes.forEach(node => traverse(node))
  return result
}

// 监听窗口大小变化
const handleResize = () => {
  if (viewMode.value === 'visual' && svg && treeContainer.value) {
    const containerWidth = treeContainer.value.clientWidth
    const containerHeight = treeContainer.value.clientHeight || 800
    svg.attr('width', containerWidth).attr('height', containerHeight)
    resetZoom()
  }
}

onMounted(() => {
  loadTree()
  window.addEventListener('resize', handleResize)
})

// 监听视图模式变化
watch(viewMode, (newMode) => {
  if (newMode === 'visual') {
    nextTick(() => {
      const flatData = flattenTree(treeData.value)
      initVisualTree(flatData)
    })
  } else if (newMode === 'list') {
    initListTree()
  }
})
</script>

<style scoped>
.knowledge-tree-page {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.tree-toolbar {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

.visual-tree-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.visual-tree-wrapper {
  flex: 1;
  display: flex;
  position: relative;
  min-height: 600px;
}

.visual-tree-container {
  flex: 1;
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  background: #fafafa;
}

.node-list-sidebar {
  width: 300px;
  border-left: 1px solid #ebeef5;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.node-list-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.node-list-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.close-icon {
  cursor: pointer;
  font-size: 18px;
  color: #909399;
}

.close-icon:hover {
  color: #303133;
}

.node-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.node-list-items {
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.node-list-item {
  padding: 10px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.node-list-item:hover {
  background: #f5f7fa;
  border-color: #409EFF;
}

.node-list-item.highlighted {
  background: #ecf5ff;
  border-color: #409EFF;
  border-width: 2px;
}

.node-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.node-item-path {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
  padding-right: 8px;
}

.node-label {
  flex: 1;
  font-size: 14px;
}

.node-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-item:hover .node-actions {
  opacity: 1;
}

/* 列表格式卡片样式 */
.list-tree-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.list-tree-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 20px;
}

.tree-list-container {
  flex: 1;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  min-height: 500px;
  height: calc(100vh - 280px);
  overflow: auto;
}

.tree-node-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 8px;
  min-height: 40px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 4px;
}

.tree-node-item:hover {
  background-color: #f5f7fa;
}

.node-icon {
  margin-right: 12px;
  font-size: 20px;
  flex-shrink: 0;
}

.node-label {
  flex: 1;
  font-size: 14px;
  line-height: 1.5;
}

.folder-icon {
  color: #FFA500;
}

.file-icon {
  color: #409EFF;
}
</style>

<style>
/* 全局样式，用于SVG元素 */
.tree-svg .link {
  fill: none;
  stroke: #ccc;
  stroke-width: 2;
}

.tree-svg .node-bg {
  transition: all 0.3s;
}

.tree-svg .node-text {
  pointer-events: none;
  user-select: none;
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 3000;
  min-width: 160px;
  padding: 4px 0;
}

.context-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background-color: #f5f7fa;
}

.context-menu-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
}

.context-menu-divider {
  height: 1px;
  background-color: #e4e7ed;
  margin: 4px 0;
}

.move-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.move-tree-node .el-icon {
  color: #409EFF;
}

/* 可视化模式节点操作工具栏 */
.visual-node-toolbar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.15);
  z-index: 2000;
  padding: 12px 20px;
  min-width: 400px;
}

.node-toolbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.selected-node-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-toolbar {
  margin-left: 8px;
  cursor: pointer;
  color: #909399;
  font-size: 18px;
  transition: color 0.2s;
}

.close-toolbar:hover {
  color: #303133;
}
</style>

