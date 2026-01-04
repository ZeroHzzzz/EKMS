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
          <el-autocomplete
            v-model="searchKeyword"
            placeholder="搜索节点并定位..."
            style="width: 250px; margin-right: 10px"
            clearable
            :fetch-suggestions="fetchSearchSuggestions"
            :trigger-on-focus="false"
            @select="handleVisualSearchSelect"
            @keyup.enter="handleSearch"
            @input="handleSearchInput"
            class="search-autocomplete"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #default="{ item }">
              <div class="suggestion-item">
                <div class="suggestion-left">
                  <el-icon v-if="item.type === 'history'" class="history-icon"><Clock /></el-icon>
                  <el-icon v-else-if="item.type === 'title'" class="keyword-icon"><Search /></el-icon>
                  <el-icon v-else class="knowledge-icon"><Document /></el-icon>
                  <span class="suggestion-title">{{ item.value }}</span>
                </div>
              </div>
            </template>
          </el-autocomplete>
          <el-button @click="refreshTree">刷新</el-button>
        </div>
      </div>
      
      <div class="visual-tree-wrapper">
        <div class="visual-tree-container" ref="treeContainer">
          <svg ref="svgRef" class="tree-svg"></svg>
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
          <el-button 
            v-if="selectedVisualNode.id !== 'root' && !selectedVisualNode.isRoot && !selectedVisualNode.isDepartmentRoot && !String(selectedVisualNode.id).startsWith('dept-')"
            size="small" 
            @click="handleEditVisualNode"
          >
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button 
            v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE') && selectedVisualNode.id !== 'root' && !selectedVisualNode.isRoot && !selectedVisualNode.isDepartmentRoot && !String(selectedVisualNode.id).startsWith('dept-')"
            size="small" 
            type="danger" 
            @click="handleDeleteVisualNode"
          >
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


    <!-- 列表格式（简洁风格） -->
    <div v-else-if="viewMode === 'list'" class="list-view-wrapper">
      <!-- 顶部工具栏 -->
      <div class="list-header-bar">
        <div class="header-left">
          <el-input
            v-model="listSearchKeyword"
            placeholder="搜索知识..."
            style="width: 280px"
            clearable
            :prefix-icon="Search"
            @input="handleListTreeSearchInput"
            @keyup.enter="handleListTreeSearch"
          />
        </div>
        <div class="header-right">
          <el-button-group>
            <el-button @click="expandAllFileTree">
              <el-icon><Expand /></el-icon>
              展开
            </el-button>
            <el-button @click="collapseAllFileTree">
              <el-icon><Fold /></el-icon>
              折叠
            </el-button>
          </el-button-group>
          <el-button @click="refreshTree" :icon="Refresh">刷新</el-button>
          <el-button 
            v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE') && listSelectedItems && listSelectedItems.length > 0"
            type="danger"
            @click="batchDeleteListItems"
          >
            <el-icon><Delete /></el-icon>
            批量删除 ({{ listSelectedItems.length }})
          </el-button>
          <el-dropdown trigger="click" v-if="activeDeptId">
            <el-button type="primary">
              <el-icon><Plus /></el-icon>
              添加
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="addNodeToSection(activeDeptId)">
                  <el-icon><Folder /></el-icon>
                  新建文件夹
                </el-dropdown-item>
                <el-dropdown-item @click="uploadToSection(activeDeptId)">
                  <el-icon><Upload /></el-icon>
                  上传文件
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 批量操作工具栏 -->
      <div v-if="listSelectedItems && listSelectedItems.length > 0" class="list-batch-bar">
        <div class="batch-left">
          <span class="selected-text">已选择 {{ listSelectedItems.length }} 项</span>
          <span class="file-count" v-if="selectedFileCount > 0">({{ selectedFileCount }} 个文件)</span>
        </div>
        <div class="batch-right">
          <el-button 
            type="primary" 
            size="small"
            :loading="batchDownloading"
            :disabled="selectedFileCount === 0"
            @click="batchDownloadWithStructure"
          >
            <el-icon><Download /></el-icon>
            批量下载
          </el-button>
          <el-button 
            v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE')"
            type="danger" 
            size="small"
            @click="batchDeleteListItems"
          >
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>

      <!-- 主内容区：左侧部门列表 + 右侧文件树 -->
      <div class="list-main-content">
        <!-- 左侧部门列表 -->
        <div class="dept-list-panel">
          <div class="dept-list-title">部门分类</div>
          <div class="dept-list">
            <!-- 搜索匹配结果分类 -->
            <div 
              v-if="hasListSearch && listSearchResults.length > 0"
              class="dept-list-item search-result"
              :class="{ 'is-active': activeDeptId === 'searchResults' }"
              @click="setActiveDept('searchResults')"
            >
              <el-icon class="dept-item-icon"><Search /></el-icon>
              <span class="dept-item-name">匹配结果</span>
              <el-badge :value="listSearchResults.length" :max="99" class="dept-item-badge" type="info" />
            </div>
            <!-- 各部门 -->
            <div 
              v-for="dept in departmentSections" 
              :key="dept.id"
              class="dept-list-item"
              :class="{ 'is-active': activeDeptId === dept.id }"
              @click="setActiveDept(dept.id)"
            >
              <el-icon class="dept-item-icon"><OfficeBuilding /></el-icon>
              <span class="dept-item-name">{{ dept.title }}</span>
              <el-badge :value="dept.children?.length || 0" :max="99" class="dept-item-badge" />
            </div>
            
            <!-- 未分类 -->
            <div 
              v-if="unclassifiedKnowledge.length > 0"
              class="dept-list-item unclassified"
              :class="{ 'is-active': activeDeptId === 'unclassified' }"
              @click="setActiveDept('unclassified')"
            >
              <el-icon class="dept-item-icon"><QuestionFilled /></el-icon>
              <span class="dept-item-name">未分类</span>
              <el-badge :value="unclassifiedKnowledge.length" :max="99" class="dept-item-badge" type="warning" />
            </div>
          </div>
        </div>

        <!-- 右侧文件树 -->
        <div class="file-tree-panel">
          <!-- 当前部门标题 -->
          <div class="current-dept-header" v-if="activeDeptId">
            <el-icon class="back-btn" @click="activeDeptId = null"><ArrowLeft /></el-icon>
            <span class="current-dept-title">{{ getActiveDeptTitle }}</span>
            <el-tag size="small">{{ activeTreeData.length }} 项</el-tag>
          </div>
          <div class="current-dept-header" v-else>
            <span class="current-dept-title">请选择部门查看知识</span>
          </div>

          <!-- 文件树内容 -->
          <div class="file-tree-body" v-if="activeDeptId">
            <div v-if="activeTreeData.length === 0" class="empty-state">
              <el-empty description="暂无内容">
                <div class="empty-btns">
                  <el-button type="primary" @click="addNodeToSection(activeDeptId)">
                    <el-icon><Folder /></el-icon>
                    新建文件夹
                  </el-button>
                  <el-button @click="uploadToSection(activeDeptId)">
                    <el-icon><Upload /></el-icon>
                    上传文件
                  </el-button>
                </div>
              </el-empty>
            </div>
            
            <!-- 可拖拽文件树 -->
            <div v-else class="draggable-file-tree">
              <div class="tree-table-header">
                <el-checkbox 
                  :model-value="isAllSelected"
                  :indeterminate="isIndeterminate"
                  @change="toggleSelectAll"
                />
                <span class="col-name">名称</span>
                <span class="col-author">作者</span>
                <span class="col-time">修改时间</span>
                <span class="col-actions">操作</span>
              </div>
              <div class="tree-table-body">
                <template v-for="(item, index) in activeTreeData" :key="item.id">
                  <FileTreeNode
                    :node="item"
                    :depth="0"
                    :index="index"
                    :selectedIds="selectedNodeIds"
                    :expandedIds="expandedNodeIds"
                    :dragOverId="dragOverNodeId"
                    :dragOverPosition="dragOverPosition"
                    @toggle-expand="toggleNodeExpand"
                    @toggle-select="toggleNodeSelect"
                    @view="viewDetail"
                    @edit="editNode"
                    @delete="deleteNode"
                    @add-child="addChildNode"
                    @drag-start="handleTreeDragStart"
                    @drag-over="handleTreeDragOver"
                    @drag-leave="handleTreeDragLeave"
                    @drop="handleTreeDrop"
                    @drag-end="handleTreeDragEnd"
                  />
                </template>
              </div>
            </div>
          </div>
          
          <!-- 未选择部门时的提示 -->
          <div class="no-dept-selected" v-else>
            <el-icon class="tip-icon"><Folder /></el-icon>
            <p>请从左侧选择一个部门</p>
            <p class="sub-tip">查看该部门下的知识文档</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 保留旧列表模式代码用于兼容 -->
    <el-card v-else-if="viewMode === 'list-old'" class="list-tree-card" style="display:none">
      <div class="tree-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="listSearchKeyword"
            placeholder="搜索节点..."
            style="width: 300px; margin-right: 10px"
            clearable
            size="large"
            @input="handleListTreeSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="handleNewFolder" size="large">
            <el-icon><Plus /></el-icon>
            新建文件夹
          </el-button>
          <el-button @click="expandAll" size="large">展开全部</el-button>
          <el-button @click="collapseAll" size="large">折叠全部</el-button>
          <el-button @click="refreshTree" size="large" :icon="Refresh">刷新</el-button>
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
          class="modern-tree"
        >
          <template #default="{ node, data }">
            <div 
              class="tree-node-card"
              :class="{ 'node-selected': selectedListNode?.id === data.id }"
              @contextmenu.prevent="showContextMenu($event, node, data)"
              @mouseenter="selectedListNode = data"
              @mouseleave="selectedListNode = null"
              @click="handleCardClick(data, node, $event)"
            >
              <div class="node-main-content">
                <div class="node-icon-wrapper">
              <el-icon class="node-icon" :class="{ 'folder-icon': isFolder(data), 'file-icon': !isFolder(data) }">
                <Folder v-if="isFolder(data)" />
                <Document v-else />
              </el-icon>
                </div>
                <div class="node-info">
                  <div class="node-title-row">
                    <span class="node-title">{{ data.title }}</span>
                    <el-tag v-if="isFolder(data)" size="small" :type="data.isDepartmentRoot ? 'success' : 'warning'" class="folder-tag">
                      {{ data.isDepartmentRoot ? '部门' : '文件夹' }}
                    </el-tag>
                    <el-tag v-else size="small" type="info" class="file-tag">
                      文件
                    </el-tag>
                  </div>
                  <div v-if="data.summary" class="node-summary">
                    {{ data.summary }}
                  </div>
                  <div class="node-meta">
                    <span v-if="data.author" class="meta-item">
                      <el-icon><User /></el-icon>
                      {{ data.author }}
                    </span>
                    <span v-if="data.createTime" class="meta-item">
                      <el-icon><Clock /></el-icon>
                      {{ formatTime(data.createTime) }}
                    </span>
                    <span v-if="data.children && data.children.length > 0" class="meta-item">
                      <el-icon><Files /></el-icon>
                      {{ data.children.length }} 项
                    </span>
                  </div>
                </div>
              </div>
              <div class="node-actions">
                <el-button 
                  v-if="!isFolder(data)"
                  size="small" 
                  text 
                  type="primary" 
                  @click.stop="viewDetail(data.id)"
                  class="action-btn"
                >
                  <el-icon><View /></el-icon>
                  查看
                </el-button>
                <el-button 
                  v-if="data.id !== 'root' && !data.isRoot && !data.isDepartmentRoot && !String(data.id).startsWith('dept-')"
                  size="small" 
                  text 
                  type="warning" 
                  @click.stop="editNode(data)"
                  class="action-btn"
                >
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button 
                  v-if="isFolder(data)" 
                  size="small" 
                  text 
                  type="success" 
                  @click.stop="addChildNode(data)"
                  class="action-btn"
                >
                  <el-icon><Plus /></el-icon>
                  添加
                </el-button>
                <el-button 
                  v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE') && data.id !== 'root' && !data.isRoot && !data.isDepartmentRoot && !String(data.id).startsWith('dept-')"
                  size="small" 
                  text 
                  type="danger" 
                  @click.stop="deleteNode(data)"
                  class="action-btn"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
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
              <el-button 
                v-if="data.id !== 'root' && !data.isRoot"
                size="small" 
                text 
                type="warning" 
                @click.stop="editNode(data)"
              >
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
              <el-button 
                v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE')"
                size="small" 
                text 
                type="danger" 
                @click.stop="deleteNode(data)"
              >
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
        <template v-if="hasPermission(userInfo, 'MANAGE_STRUCTURE')">
          <div class="context-menu-divider"></div>
          <div class="context-menu-item" @click="handleContextMenuClick('delete')">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </div>
        </template>
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
        <el-form :model="uploadForm" label-width="120px">
          <el-form-item label="存放的知识结构">
            <el-input 
              v-model="uploadForm.parentPath" 
              placeholder="将从选中的节点自动填充" 
              readonly
              style="cursor: default;"
            >
              <template #prefix>
                <el-icon><FolderOpened /></el-icon>
              </template>
            </el-input>
          </el-form-item>
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
import { ZoomIn, ZoomOut, Refresh, Search, Close, Document, Folder, FolderOpened, Plus, Delete, Edit, CopyDocument, Files, UploadFilled, Upload, User, Clock, View, ArrowRight, OfficeBuilding, QuestionFilled, Download, ArrowLeft, ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import * as d3 from 'd3'
import CryptoJS from 'crypto-js'
import Sortable from 'sortablejs'
import api from '../api'
import { useUserStore } from '../stores/user'
import { hasPermission, hasRole, ROLE_ADMIN, ROLE_EDITOR } from '../utils/permission'
import FileTreeNode from '../components/FileTreeNode.vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
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

// 部门分区相关
const expandedSections = ref({ shared: true, unclassified: true })
const deptTreeRefs = ref({})
const listSelectedItems = ref([]) // 列表模式所有表格的选中项
const sortableInstances = ref({}) // 存储 Sortable 实例
const batchDownloading = ref(false) // 批量下载中

// 新列表模式相关
const activeDeptId = ref(null) // 当前选中的部门ID
const fileTreeContent = ref(null) // 文件树内容容器引用
const draggableTreeRef = ref(null) // 可拖拽树引用
const selectedNodeIds = ref([]) // 选中的节点ID列表
const expandedNodeIds = ref([]) // 展开的节点ID列表
const dragOverNodeId = ref(null) // 拖拽悬停的节点ID
const dragOverPosition = ref(null) // 拖拽悬停的位置 (before/after/inside)
const draggingNode = ref(null) // 正在拖拽的节点

// 计算选中的文件数量（排除文件夹）
const selectedFileCount = computed(() => {
  return listSelectedItems.value.filter(item => item.fileId).length
})

// 获取当前选中部门的标题
const getActiveDeptTitle = computed(() => {
  if (!activeDeptId.value) return ''
  if (activeDeptId.value === 'searchResults') return '匹配结果'
  if (activeDeptId.value === 'unclassified') return '未分类'
  const dept = departmentSections.value.find(d => d.id === activeDeptId.value)
  return dept?.title || ''
})

// 获取当前活动的树数据
const activeTreeData = computed(() => {
  if (!activeDeptId.value) return []
  if (activeDeptId.value === 'searchResults') {
    // 将搜索结果视为一个扁平列表
    return (listSearchResults.value || []).map(item => ({
      ...item,
      children: []
    }))
  }
  if (activeDeptId.value === 'unclassified') return unclassifiedKnowledge.value
  const dept = departmentSections.value.find(d => d.id === activeDeptId.value)
  return dept?.children || []
})

// 判断是否全选
const isAllSelected = computed(() => {
  if (activeTreeData.value.length === 0) return false
  const allIds = getAllNodeIds(activeTreeData.value)
  return allIds.every(id => selectedNodeIds.value.includes(id))
})

// 判断是否部分选中
const isIndeterminate = computed(() => {
  if (activeTreeData.value.length === 0) return false
  const allIds = getAllNodeIds(activeTreeData.value)
  const selectedCount = allIds.filter(id => selectedNodeIds.value.includes(id)).length
  return selectedCount > 0 && selectedCount < allIds.length
})

// 获取所有节点ID（递归）
const getAllNodeIds = (nodes) => {
  const ids = []
  const traverse = (list) => {
    list.forEach(node => {
      ids.push(node.id)
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  traverse(nodes)
  return ids
}

// 设置活动部门
const setActiveDept = (deptId) => {
  activeDeptId.value = deptId
  selectedNodeIds.value = []
  // 默认展开第一层
  if (deptId) {
    expandedNodeIds.value = activeTreeData.value.map(n => n.id)
  }
}

// 切换节点展开
const toggleNodeExpand = (nodeId) => {
  const idx = expandedNodeIds.value.indexOf(nodeId)
  if (idx === -1) {
    expandedNodeIds.value.push(nodeId)
  } else {
    expandedNodeIds.value.splice(idx, 1)
  }
}

// 切换节点选中
const toggleNodeSelect = (node) => {
  const idx = selectedNodeIds.value.indexOf(node.id)
  if (idx === -1) {
    selectedNodeIds.value.push(node.id)
    // 同步到 listSelectedItems
    listSelectedItems.value.push(node)
  } else {
    selectedNodeIds.value.splice(idx, 1)
    // 同步到 listSelectedItems
    const itemIdx = listSelectedItems.value.findIndex(item => item.id === node.id)
    if (itemIdx !== -1) {
      listSelectedItems.value.splice(itemIdx, 1)
    }
  }
}

// 切换全选
const toggleSelectAll = (val) => {
  const allIds = getAllNodeIds(activeTreeData.value)
  if (val) {
    selectedNodeIds.value = [...allIds]
    // 获取所有节点对象
    const getAllNodes = (nodes) => {
      const result = []
      const traverse = (list) => {
        list.forEach(node => {
          result.push(node)
          if (node.children && node.children.length > 0) {
            traverse(node.children)
          }
        })
      }
      traverse(nodes)
      return result
    }
    listSelectedItems.value = getAllNodes(activeTreeData.value)
  } else {
    selectedNodeIds.value = []
    listSelectedItems.value = []
  }
}

// 展开所有节点
const expandAllFileTree = () => {
  expandedNodeIds.value = getAllNodeIds(activeTreeData.value)
}

// 折叠所有节点
const collapseAllFileTree = () => {
  expandedNodeIds.value = []
}

// 拖拽处理函数
const handleTreeDragStart = (e, node) => {
  draggingNode.value = node
}

const handleTreeDragOver = (e, node, position) => {
  if (draggingNode.value && draggingNode.value.id !== node.id) {
    // 不能拖拽到自己的子节点内
    if (position === 'inside' && isDescendantOf(node, draggingNode.value)) {
      return
    }
    dragOverNodeId.value = node.id
    dragOverPosition.value = position
  }
}

const handleTreeDragLeave = (e, node) => {
  if (dragOverNodeId.value === node.id) {
    dragOverNodeId.value = null
    dragOverPosition.value = null
  }
}

const handleTreeDrop = async (e, targetNode, position) => {
  if (!draggingNode.value || draggingNode.value.id === targetNode.id) {
    resetDragState()
    return
  }
  
  try {
    let newParentId = null
    let newSortOrder = 0
    
    if (position === 'inside') {
      // 移动到文件夹内部
      newParentId = normalizeParentId(targetNode.id)
      newSortOrder = targetNode.children?.length || 0
    } else if (position === 'before') {
      // 移动到目标前面
      newParentId = normalizeParentId(targetNode.parentId)
      newSortOrder = (targetNode.sortOrder || 0)
    } else {
      // 移动到目标后面
      newParentId = normalizeParentId(targetNode.parentId)
      newSortOrder = (targetNode.sortOrder || 0) + 1
    }
    
    const res = await api.put(`/knowledge/${draggingNode.value.id}/move`, {
      parentId: newParentId,
      sortOrder: newSortOrder
    })
    
    if (res.code === 200) {
      ElMessage.success('移动成功')
      await loadTree()
    } else {
      ElMessage.error(res.message || '移动失败')
    }
  } catch (error) {
    ElMessage.error('移动失败: ' + (error.message || '未知错误'))
  } finally {
    resetDragState()
  }
}

const handleTreeDragEnd = (e) => {
  resetDragState()
}

const resetDragState = () => {
  draggingNode.value = null
  dragOverNodeId.value = null
  dragOverPosition.value = null
}

// 检查 node 是否是 parent 的后代
const isDescendantOf = (node, parent) => {
  const checkChildren = (children) => {
    if (!children) return false
    for (const child of children) {
      if (child.id === node.id) return true
      if (checkChildren(child.children)) return true
    }
    return false
  }
  return checkChildren(parent.children)
}

// 设置树引用
const setTreeRef = (id, el) => {
  if (el) {
    deptTreeRefs.value[id] = el
    // 初始化拖拽功能
    nextTick(() => {
      initTableSortable(id, el)
    })
  }
}

// 部门分区数据（排除未分类）
const departmentSections = computed(() => {
  return treeData.value.filter(item => item.isDepartmentRoot && item.title !== '未分类')
})

// 未分类知识
const unclassifiedKnowledge = computed(() => {
  // 找到"未分类"部门节点，获取其children
  const unclassifiedDept = treeData.value.find(item => 
    item.isDepartmentRoot && item.title === '未分类'
  )
  return unclassifiedDept?.children || []
})

// 切换分区展开/折叠
const toggleSection = (sectionId) => {
  expandedSections.value[sectionId] = !expandedSections.value[sectionId]
}

// 转换 parentId 为后端需要的格式（Long 类型）
// 注意：部门根节点的 ID 是负数（如 -1），这些是虚拟节点，不存在于数据库中
// 当用户在部门根节点下创建知识时，parentId 应该为 null（表示该部门下的顶级知识）
const normalizeParentId = (parentId) => {
  // 1. 处理空值
  if (parentId === 'root' || parentId === null || parentId === undefined) {
    return null
  }
  
  // 2. 处理字符串类型的 ID
  if (typeof parentId === 'string') {
    // 如果是 "dept-X" 格式，说明是部门根节点，返回 null
    if (parentId.startsWith('dept-')) {
      return null
    }
    
    // 尝试转换为数字
    const num = parseInt(parentId)
    if (isNaN(num)) {
      // 无法转换为数字，可能是其他非数字 ID，返回 null
      return null
    }
    
    // 如果是负数，说明是部门根节点，返回 null
    if (num < 0) {
      return null
    }
    
    // 正常的正数 ID
    return num
  }
  
  // 3. 处理数字类型的 ID
  if (typeof parentId === 'number') {
    // 负数 ID 是部门根节点，不存在于数据库
    if (parentId < 0) {
      return null
    }
    return parentId
  }
  
  // 其他情况返回 null
  return null
}

// 添加节点（文件夹）到分区
const addNodeToSection = (sectionId) => {
  isNewFolder.value = true
  // 设计原则：部门根节点是虚拟节点，不存在于数据库中
  // 在部门根节点下创建的知识应该是顶级知识（parentId = null）
  // 但需要设置 department 字段来标识知识所属的部门
  if (sectionId === 'unclassified') {
    parentNodeId.value = null
    nodeForm.value = { title: '', summary: '', keywords: '', department: null }
  } else {
    // 部门分区：找到对应的部门名称
    const dept = departmentSections.value.find(d => d.id === sectionId)
    const departmentName = dept ? dept.title : null
    // 部门根节点是虚拟节点，parentId 应该为 null，但需要设置 department
    parentNodeId.value = null
    nodeForm.value = { title: '', summary: '', keywords: '', department: departmentName }
  }
  showAddDialog.value = true
}

// 添加知识到分区（保留用于兼容，现在主要用于上传）
const addToSection = (sectionId) => {
  isNewFolder.value = false
  // 设计原则：部门根节点是虚拟节点，不存在于数据库中
  // 在部门根节点下创建的知识应该是顶级知识（parentId = null）
  // 但需要设置 department 字段来标识知识所属的部门
  if (sectionId === 'unclassified') {
    parentNodeId.value = null
    nodeForm.value = { title: '', summary: '', keywords: '', department: null }
  } else {
    // 部门分区：找到对应的部门名称
    const dept = departmentSections.value.find(d => d.id === sectionId)
    const departmentName = dept ? dept.title : null
    // 部门根节点是虚拟节点，parentId 应该为 null，但需要设置 department
    parentNodeId.value = null
    nodeForm.value = { title: '', summary: '', keywords: '', department: departmentName }
  }
  showAddDialog.value = true
}

// 上传到分区
const uploadToSection = (sectionId) => {
  // 设计原则：部门根节点是虚拟节点，不存在于数据库中
  // 在部门根节点下上传的知识应该是顶级知识（parentId = null）
  // 但需要设置 department 字段来标识知识所属的部门
  if (sectionId === 'unclassified') {
    uploadForm.value.parentId = null
    uploadForm.value.department = null
    uploadForm.value.parentPath = '未分类'
  } else {
    // 部门分区：找到对应的部门名称
    const dept = departmentSections.value.find(d => d.id === sectionId)
    const departmentName = dept ? dept.title : null
    // 部门根节点是虚拟节点，parentId 应该为 null，但需要设置 department
    uploadForm.value.parentId = null
    uploadForm.value.department = departmentName
    uploadForm.value.parentPath = departmentName || '根目录'
  }
  showUploadDialog.value = true
}

// 初始化分区展开状态
const initExpandedSections = () => {
  departmentSections.value.forEach(dept => {
    if (expandedSections.value[dept.id] === undefined) {
      expandedSections.value[dept.id] = true
    }
  })
}

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
  department: null, // 部门名称（当在部门根节点下上传时使用）
  summary: '',
  keywords: '',
  parentPath: '' // 存放的知识结构路径（显示用）
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
  keywords: '',
  department: null // 部门名称（当在部门根节点下创建时使用）
})

// 加载知识树
const loadTree = async () => {
  try {
    const res = await api.get('/knowledge/tree')
    if (res.code === 200) {
      const flatData = res.data || []
      const builtTree = buildTree(flatData)
      
      // 如果没有数据，显示提示
      if (builtTree.length === 0) {
        ElMessage.info('暂无知识数据，请先添加部门或知识')
        treeData.value = []
      } else {
        // 强制更新 treeData，确保响应式系统检测到变化
        treeData.value = []
        await nextTick()
        treeData.value = builtTree
      }
      
      // 初始化分区展开状态
      initExpandedSections()
      
      // 根据当前视图模式更新显示
      await nextTick()
      if (viewMode.value === 'visual') {
        initVisualTree(flatData)
      } else if (viewMode.value === 'list') {
        // 列表模式：计算属性会自动更新，但需要确保 DOM 刷新
        // 强制触发计算属性的重新计算
        initListTree()
        // 重新初始化所有表格的拖拽功能
        await nextTick()
        Object.keys(deptTreeRefs.value).forEach(sectionId => {
          const table = deptTreeRefs.value[sectionId]
          if (table) {
            initTableSortable(sectionId, table)
          }
        })
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
  
  // 分离部门根节点和普通知识节点
  const departmentRoots = []
  const knowledgeNodes = []
  
  list.forEach(item => {
    if (item.isDepartmentRoot) {
      // 部门根节点
      departmentRoots.push({ ...item, children: [], isDepartmentRoot: true })
    } else {
      // 普通知识节点
      knowledgeNodes.push(item)
    }
  })
  
  // 为部门根节点创建映射（使用负数ID或特殊标识）
  departmentRoots.forEach(dept => {
    const deptId = dept.id < 0 ? `dept-${Math.abs(dept.id)}` : `dept-${dept.id}`
    map[deptId] = { ...dept, id: deptId, children: [] }
  })
  
  // 为知识节点创建映射
  knowledgeNodes.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })
  
  // 构建树：将知识节点挂载到部门根节点下
  knowledgeNodes.forEach(item => {
    if (item.parentId) {
      // 有父节点，查找父节点
      const parent = map[item.parentId]
      if (parent) {
        parent.children.push(map[item.id])
      } else {
        // 父节点不存在，可能是部门根节点
        // 根据知识的部门找到对应的部门根节点
        const deptRoot = departmentRoots.find(dept => dept.department === item.department)
        if (deptRoot) {
          const deptId = deptRoot.id < 0 ? `dept-${Math.abs(deptRoot.id)}` : `dept-${deptRoot.id}`
          map[deptId].children.push(map[item.id])
        } else {
          // 找不到部门，作为根节点
          roots.push(map[item.id])
        }
      }
    } else {
      // 没有父节点，挂载到对应部门下
      if (item.department) {
        // 匹配部门：优先使用 department 字段，如果匹配不到，尝试使用 title 字段
        let deptRoot = departmentRoots.find(dept => 
          dept.department === item.department || dept.title === item.department
        )
        if (deptRoot) {
          const deptId = deptRoot.id < 0 ? `dept-${Math.abs(deptRoot.id)}` : `dept-${deptRoot.id}`
          map[deptId].children.push(map[item.id])
        } else {
          // 找不到匹配的部门，作为根节点（这种情况不应该发生，但容错处理）
          console.warn('找不到匹配的部门:', item.department, '知识ID:', item.id, '知识标题:', item.title)
          roots.push(map[item.id])
        }
      } else {
        // 没有部门的知识，挂载到"未分类"部门
        const unclassifiedDept = departmentRoots.find(dept => dept.title === '未分类')
        if (unclassifiedDept) {
          const deptId = unclassifiedDept.id < 0 ? `dept-${Math.abs(unclassifiedDept.id)}` : `dept-${unclassifiedDept.id}`
          map[deptId].children.push(map[item.id])
        } else {
          roots.push(map[item.id])
        }
      }
    }
  })
  
  // 按sortOrder排序并设置 hasChildren 属性
  const sortChildren = (nodes) => {
    nodes.sort((a, b) => {
      // 部门根节点优先
      if (a.isDepartmentRoot && !b.isDepartmentRoot) return -1
      if (!a.isDepartmentRoot && b.isDepartmentRoot) return 1
      // 同类型按sortOrder排序
      return (a.sortOrder || 0) - (b.sortOrder || 0)
    })
    nodes.forEach(node => {
      // 设置 hasChildren 属性，用于 el-table 的树形展开
      node.hasChildren = node.children && node.children.length > 0
      if (node.children && node.children.length > 0) {
        sortChildren(node.children)
      }
    })
  }
  
  // 添加部门根节点到结果
  departmentRoots.forEach(dept => {
    const deptId = dept.id < 0 ? `dept-${Math.abs(dept.id)}` : `dept-${dept.id}`
    roots.push(map[deptId])
  })
  
  sortChildren(roots)
  return roots
}

// 展开全部
const expandAll = () => {
  if (viewMode.value === 'list') {
    // 新文件管理器模式：如果在部门视图中，展开所有节点
    if (activeDeptId.value) {
      expandAllFileTree()
    } else {
      // 展开所有部门分区
      Object.keys(expandedSections.value).forEach(key => {
        expandedSections.value[key] = true
      })
      departmentSections.value.forEach(dept => {
        expandedSections.value[dept.id] = true
      })
    }
  } else {
    // 其他模式：使用原来的逻辑
    const tree = treeRef.value
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
}

// 折叠全部
const collapseAll = () => {
  if (viewMode.value === 'list') {
    // 新文件管理器模式：如果在部门视图中，折叠所有节点
    if (activeDeptId.value) {
      collapseAllFileTree()
    } else {
      // 折叠所有部门分区
      Object.keys(expandedSections.value).forEach(key => {
        expandedSections.value[key] = false
      })
      departmentSections.value.forEach(dept => {
        expandedSections.value[dept.id] = false
      })
    }
  } else {
    // 其他模式：使用原来的逻辑
    const tree = treeRef.value
    if (tree) {
      const nodes = tree.store?.nodesMap
      if (nodes) {
        Object.values(nodes).forEach(node => {
          node.expanded = false
        })
      }
    }
  }
}

// 刷新树
const refreshTree = async () => {
  selectedVisualNode.value = null
  selectedListNode.value = null
  // 清空当前树数据，强制触发响应式更新
  treeData.value = []
  await nextTick()
  // 重新加载树
  await loadTree()
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
    const rawParentId = dropType === 'inner' ? dropNode.data.id : (dropNode.parent ? dropNode.parent.data.id : null)
    let newParentId = rawParentId;
    if (newParentId !== null) {
      newParentId = normalizeParentId(newParentId);
    }
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

// 处理表格行移动
const handleTableRowMove = async (draggedRow, targetRow, sectionId, isMovingDown) => {
  // 不能拖到自己或自己的子节点
  if (String(draggedRow.id) === String(targetRow.id)) return
  
  const isDescendant = (parent, child) => {
    if (String(parent.id) === String(child.id)) return true
    if (parent.children && parent.children.length > 0) {
      return parent.children.some(c => isDescendant(c, child))
    }
    return false
  }
  if (isDescendant(draggedRow, targetRow)) {
    ElMessage.warning('不能移动到自己的子节点')
    return
  }
  
  // 获取表格数据用于查找父节点
  let tableData = []
  if (sectionId === 'unclassified') {
    tableData = unclassifiedKnowledge.value
  } else {
    const dept = departmentSections.value.find(d => d.id === sectionId)
    tableData = dept?.children || []
  }
  
  // 查找父节点
  const findParent = (nodes, targetId, parent = null) => {
    for (const node of nodes) {
      if (String(node.id) === String(targetId)) {
        return parent
      }
      if (node.children && node.children.length > 0) {
        const found = findParent(node.children, targetId, node)
        if (found !== null) return found
      }
    }
    return null
  }
  
  // 判断是移动到文件夹内部还是同级移动
  let newParentId = null
  let newSortOrder = 0
  
  // 检查目标行是否是文件夹
  if (isFolder(targetRow)) {
    // 移动到文件夹内部
    newParentId = targetRow.id
    newSortOrder = targetRow.children?.length || 0
  } else {
    // 同级移动，使用目标行的父节点
    const parent = findParent(tableData, targetRow.id)
    newParentId = parent ? parent.id : null
    
    // 计算新的排序顺序（使用目标行的 sortOrder）
    newSortOrder = (targetRow.sortOrder || 0) + (isMovingDown ? 1 : 0)
  }
  
  // 调用移动 API
  try {
    const normalizedParentId = newParentId !== null ? normalizeParentId(newParentId) : null
    const res = await api.put(`/knowledge/${draggedRow.id}/move`, {
      parentId: normalizedParentId,
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

// 当前拖拽悬停的文件夹行
const dragOverFolderRow = ref(null)
const draggedRowData = ref(null) // 保存拖拽行的数据

// 递归查找节点的辅助函数（全局）
const findNodeInTree = (nodes, id) => {
  if (!nodes || !Array.isArray(nodes)) return null
  for (const node of nodes) {
    if (String(node.id) === String(id)) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findNodeInTree(node.children, id)
      if (found) return found
    }
  }
  return null
}

// 在所有数据中查找节点
const findNodeGlobally = (id) => {
  // 在各部门中查找
  let found = null
  for (const dept of departmentSections.value) {
    found = findNodeInTree(dept.children, id)
    if (found) return found
  }
  
  // 在未分类中查找
  found = findNodeInTree(unclassifiedKnowledge.value, id)
  if (found) return found
  
  return null
}

// 初始化表格拖拽功能
const initTableSortable = (sectionId, tableEl) => {
  // 销毁已存在的实例
  if (sortableInstances.value[sectionId]) {
    sortableInstances.value[sectionId].destroy()
    delete sortableInstances.value[sectionId]
  }
  
  // 等待表格渲染完成
  nextTick(() => {
    const tbody = tableEl?.$el?.querySelector('.el-table__body-wrapper tbody')
    if (!tbody) return
    
    // 获取表格数据的辅助函数
    const getTableData = () => {
      if (sectionId === 'unclassified') {
        return unclassifiedKnowledge.value
      } else {
        const dept = departmentSections.value.find(d => d.id === sectionId)
        return dept?.children || []
      }
    }
    
    sortableInstances.value[sectionId] = Sortable.create(tbody, {
      handle: '.drag-handle', // 只有点击拖拽手柄才能拖拽
      animation: 200,
      ghostClass: 'sortable-ghost',
      chosenClass: 'sortable-chosen',
      dragClass: 'sortable-drag',
      filter: '.node-actions, .node-title', // 过滤掉操作按钮和标题区域
      
      // 拖拽开始时保存拖拽行数据
      onStart: (evt) => {
        dragOverFolderRow.value = null
        const draggedRowKey = evt.item?.getAttribute('data-key')
        if (draggedRowKey) {
          draggedRowData.value = findNodeGlobally(draggedRowKey)
        }
      },
      
      // 拖拽移动时的回调
      onMove: (evt) => {
        const relatedEl = evt.related
        if (!relatedEl) return true
        
        const relatedRowKey = relatedEl.getAttribute('data-key')
        if (!relatedRowKey) return true
        
        const targetRow = findNodeGlobally(relatedRowKey)
        const draggedRow = draggedRowData.value
        
        // 如果目标是文件夹，且拖拽的是文件（不是文件夹）
        if (targetRow && isFolder(targetRow) && draggedRow && !isFolder(draggedRow)) {
          // 不能拖入自己的父文件夹（没有意义）
          if (targetRow.id === draggedRow.parentId) {
            return true
          }
          
          // 移除之前的高亮
          tbody.querySelectorAll('.drag-over-folder').forEach(el => {
            el.classList.remove('drag-over-folder')
          })
          // 添加高亮
          relatedEl.classList.add('drag-over-folder')
          dragOverFolderRow.value = targetRow
          // 返回 false 阻止位置交换
          return false
        } else {
          // 移除高亮
          tbody.querySelectorAll('.drag-over-folder').forEach(el => {
            el.classList.remove('drag-over-folder')
          })
          dragOverFolderRow.value = null
        }
        
        return true
      },
      
      onEnd: async (evt) => {
        const { oldIndex, newIndex, item } = evt
        
        // 清除所有文件夹高亮
        tbody.querySelectorAll('.drag-over-folder').forEach(el => {
          el.classList.remove('drag-over-folder')
        })
        
        const draggedRow = draggedRowData.value
        draggedRowData.value = null
        
        if (!draggedRow) {
          ElMessage.warning('无法获取行数据，请重试')
          dragOverFolderRow.value = null
          return
        }
        
        // 检查是否拖拽到文件夹上（文件拖到文件夹）
        if (dragOverFolderRow.value && !isFolder(draggedRow)) {
          const targetFolder = dragOverFolderRow.value
          dragOverFolderRow.value = null
          // 文件拖拽到文件夹，执行移动到文件夹操作
          await moveToFolder(draggedRow, targetFolder, sectionId)
          return
        }
        
        dragOverFolderRow.value = null
        
        // 普通排序操作（位置没变就不处理）
        if (oldIndex === newIndex) return
        
        const targetItem = tbody.children[newIndex]
        const targetRowKey = targetItem?.getAttribute('data-key')
        
        if (!targetRowKey) {
          return
        }
        
        const targetRow = findNodeGlobally(targetRowKey)
        
        if (!targetRow) {
          ElMessage.warning('无法找到目标行数据')
          return
        }
        
        await handleTableRowMove(draggedRow, targetRow, sectionId, newIndex > oldIndex)
      }
    })
  })
}

// 移动文件到文件夹
const moveToFolder = async (file, folder, sectionId) => {
  try {
    // 确认操作
    await ElMessageBox.confirm(
      `确定将「${file.title}」移动到「${folder.title}」文件夹中吗？`,
      '移动文件',
      { type: 'info' }
    )
    
    // 调用 API 更新文件的 parentId
    const res = await api.put(`/knowledge/${file.id}`, {
      parentId: folder.id,
      // 保持其他属性不变
      title: file.title,
      department: file.department
    })
    
    if (res.code === 200) {
      ElMessage.success(`已将「${file.title}」移动到「${folder.title}」`)
      // 刷新数据
      await loadTree()
    } else {
      ElMessage.error(res.message || '移动失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '移动失败')
    }
  }
}

// 格式化时间
// 点击卡片（自定义模板中的卡片）
const handleCardClick = (data, node, event, sectionId) => {
  // 如果点击的是按钮，不处理（按钮已经使用了 @click.stop）
  if (event && event.target.closest('.node-actions')) {
    return
  }
  
  // 在列表模式下，区分文件夹和文件
  if (viewMode.value === 'list') {
    // 如果是文件夹，展开/折叠节点
    if (isFolder(data)) {
      if (sectionId) {
        // 使用 el-table 的 toggleRowExpansion
        const table = deptTreeRefs.value[sectionId]
        if (table) {
          // 确保有子节点才展开
          if (data.hasChildren || (data.children && data.children.length > 0)) {
            table.toggleRowExpansion(data, undefined)
          }
        }
      } else if (listTreeRef.value && node) {
        // node 是 el-tree 的节点对象，直接切换
        node.expanded = !node.expanded
      } else if (listTreeRef.value) {
        // 如果没有 node 对象，通过 store 获取
        const treeNode = listTreeRef.value.store.nodesMap[data.id]
        if (treeNode) {
          treeNode.expanded = !treeNode.expanded
        }
      }
    } else {
      // 如果是文件，打开详情页
      viewDetail(data.id)
    }
  }
}

// 点击节点（el-tree 的原生事件）
const handleNodeClick = (data, node) => {
  // 在列表模式下，区分文件夹和文件
  if (viewMode.value === 'list') {
    // 如果是文件夹，展开/折叠节点
    if (isFolder(data)) {
      // 使用 el-tree 的 store 来切换展开状态
      if (listTreeRef.value && node) {
        const treeNode = listTreeRef.value.store.nodesMap[data.id]
        if (treeNode) {
          // 切换展开状态
          treeNode.expanded = !treeNode.expanded
        }
      }
    } else {
      // 如果是文件，打开详情页
      viewDetail(data.id)
    }
  } else {
    // 其他模式下，直接查看详情
    viewDetail(data.id)
  }
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/knowledge/${id}`)
}

// 编辑节点
const editNode = (data) => {
  // 虚拟根节点和部门根节点不能编辑
  if (data.id === 'root' || data.isRoot || data.isDepartmentRoot || (data.id && String(data.id).startsWith('dept-'))) {
    ElMessage.warning('根节点不能编辑')
    return
  }
  
  editingNode.value = data
  nodeForm.value = {
    title: data.title || '',
    category: data.category || '',
    summary: data.summary || '',
    keywords: data.keywords || '',
    department: data.department || null
  }
  // 如果 parentId 是 'root'，转换为 null
  parentNodeId.value = data.parentId === 'root' ? null : data.parentId
  showAddDialog.value = true
}

// 添加子节点
const addChildNode = (data) => {
  editingNode.value = null
  nodeForm.value = {
    title: '',
    category: data.category || '',
    summary: '',
    keywords: '',
    department: data.department || null
  }
  // 如果是虚拟根节点，parentId 应该为 null
  parentNodeId.value = data.id === 'root' ? null : data.id
  showAddDialog.value = true
}

// 删除节点
const deleteNode = async (data) => {
  // 虚拟根节点不能删除
  if (data.id === 'root' || data.isRoot) {
    ElMessage.warning('根节点不能删除')
    return
  }
  
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

// 列表模式批量选择变化（处理多个表格的选中项）
const tableSelections = ref({}) // 存储每个表格的选中项
const handleListTableSelectionChange = (tableId, selection) => {
  tableSelections.value[tableId] = selection || []
  // 合并所有表格的选中项
  const allSelections = []
  Object.values(tableSelections.value).forEach(selections => {
    allSelections.push(...selections)
  })
  // 去重（根据id）
  const uniqueSelections = []
  const idSet = new Set()
  allSelections.forEach(item => {
    if (!idSet.has(item.id)) {
      idSet.add(item.id)
      uniqueSelections.push(item)
    }
  })
  listSelectedItems.value = uniqueSelections
}

// 列表模式批量删除
const batchDeleteListItems = async () => {
  if (!listSelectedItems.value || listSelectedItems.value.length === 0) {
    ElMessage.warning('请先选择要删除的项')
    return
  }
  
  // 过滤掉根节点
  const validItems = listSelectedItems.value.filter(item => 
    item.id !== 'root' && !item.isRoot && !item.isDepartmentRoot && !String(item.id).startsWith('dept-')
  )
  
  if (validItems.length === 0) {
    ElMessage.warning('选中的项中包含不能删除的根节点')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${validItems.length} 个项吗？`, '批量删除', {
      type: 'warning'
    })
    
    // 逐个删除
    let successCount = 0
    let failCount = 0
    for (const item of validItems) {
      try {
        const res = await api.delete(`/knowledge/${item.id}`)
        if (res.code === 200) {
          successCount++
        } else {
          failCount++
        }
      } catch (error) {
        failCount++
      }
    }
    
    if (failCount === 0) {
      ElMessage.success(`成功删除 ${successCount} 个项`)
    } else {
      ElMessage.warning(`删除完成：成功 ${successCount} 个，失败 ${failCount} 个`)
    }
    
    // 清空所有表格的选择
    listSelectedItems.value = []
    tableSelections.value = {}
    Object.values(deptTreeRefs.value).forEach(tableRef => {
      if (tableRef && tableRef.clearSelection) {
        tableRef.clearSelection()
      }
    })
    await loadTree()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  }
}

// 递归获取节点的完整路径（用于批量下载）
const getNodeFullPath = (nodeId, nodes, path = []) => {
  for (const node of nodes) {
    if (String(node.id) === String(nodeId)) {
      return [...path, node.title].join('/')
    }
    if (node.children && node.children.length > 0) {
      const found = getNodeFullPath(nodeId, node.children, [...path, node.title])
      if (found) return found
    }
  }
  return null
}

// 批量下载（保留文件夹结构）
const batchDownloadWithStructure = async () => {
  const fileItems = listSelectedItems.value.filter(item => item.fileId)
  if (fileItems.length === 0) {
    ElMessage.warning('请选择要下载的文件')
    return
  }
  
  batchDownloading.value = true
  ElMessage.info(`正在准备下载 ${fileItems.length} 个文件...`)
  
  try {
    // 构建下载项，包含文件路径信息
    const downloadItems = fileItems.map(item => {
      // 尝试获取文件在知识结构中的路径
      let filePath = item.title
      
      // 遍历部门数据查找完整路径
      for (const dept of departments.value) {
        const deptData = deptKnowledgeMap.value[dept.name]
        if (deptData) {
          const path = getNodeFullPath(item.id, deptData)
          if (path) {
            filePath = `${dept.name}/${path}`
            break
          }
        }
      }
      
      // 添加文件扩展名（如果没有的话）
      if (item.fileName && !filePath.includes('.')) {
        const ext = item.fileName.split('.').pop()
        if (ext) {
          filePath = `${filePath}.${ext}`
        }
      }
      
      return {
        fileId: item.fileId,
        path: filePath
      }
    })
    
    // 调用批量下载接口
    const res = await api.post('/file/batch-download-with-structure', { items: downloadItems }, { 
      responseType: 'blob',
      timeout: 300000 // 5分钟超时
    })
    
    // 创建下载链接
    const blob = new Blob([res], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `知识库文档_${new Date().toLocaleDateString()}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success(`成功下载 ${fileItems.length} 个文件`)
  } catch (error) {
    console.error('批量下载失败', error)
    // 如果批量下载失败，尝试逐个下载
    ElMessage.warning('正在逐个下载文件...')
    for (const item of fileItems) {
      try {
        const link = document.createElement('a')
        link.href = `/api/file/download/${item.fileId}`
        link.download = item.title || item.fileName || 'file'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        await new Promise(resolve => setTimeout(resolve, 500))
      } catch (e) {
        console.error(`下载 ${item.title} 失败`)
      }
    }
    ElMessage.success('下载完成')
  } finally {
    batchDownloading.value = false
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
      let actualParentId = parentNodeId.value;
      if (actualParentId !== null) {
        actualParentId = normalizeParentId(actualParentId);
      }
      const res = await api.put(`/knowledge/${editingNode.value.id}`, {
        ...nodeForm.value,
        parentId: actualParentId
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
      let actualParentId = parentNodeId.value;
      if (actualParentId !== null) {
        actualParentId = normalizeParentId(actualParentId);
      }
      
      // 确定 department：优先使用 nodeForm.value.department（从部门根节点创建时设置）
      // 如果为 null 且 parentId 也为 null，则使用用户信息中的部门
      let department = nodeForm.value.department
      if (department === null && actualParentId === null) {
        const userInfo = userStore.userInfo
        department = userInfo?.department || null
      }
      
      // 所有创建的知识初始状态都是待审核
      const userInfo = userStore.userInfo
      const res = await api.post('/knowledge', {
        ...nodeForm.value,
        parentId: actualParentId,
        department: department,
        status: 'PENDING' // 所有创建的知识都是待审核状态
      })
      
      // 创建知识后，自动提交审核
      if (res.code === 200 && userInfo) {
        try {
          await api.post(`/knowledge/${res.data.id}/submit-audit?userId=${userInfo.id}`)
        } catch (error) {
          console.warn('提交审核失败，但知识已创建', error)
        }
      }
      if (res.code === 200) {
        ElMessage.success('创建成功')
        showAddDialog.value = false
        nodeForm.value = { title: '', category: '', summary: '', keywords: '', department: null }
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
  const builtTree = buildTree(data)
  
  // 如果没有数据，显示空状态
  if (builtTree.length === 0) {
    root = { nodes: [], links: [], simulation: null }
    return
  }
  
  // 使用力导向图布局 - 每个部门作为独立集群，不需要虚拟根节点
  const nodes = []
  const links = []
  
  // 部门颜色映射
  const deptColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6', '#1ABC9C', '#E74C3C']
  let colorIndex = 0
  
  // 遍历每个部门树，提取节点和边
  const traverse = (node, parentId = null, deptColor = '#409EFF') => {
    const nodeId = node.id || `node-${nodes.length}`
    nodes.push({
      id: nodeId,
      title: node.title || '未命名',
      deptColor: deptColor,
      ...node
    })
    
    if (parentId) {
      links.push({
        source: parentId,
        target: nodeId
      })
    }
    
    if (node.children && node.children.length > 0) {
      node.children.forEach(child => traverse(child, nodeId, deptColor))
    }
  }
  
  // 遍历所有部门（每个部门是独立的根）
  builtTree.forEach((deptRoot, index) => {
    const color = deptColors[index % deptColors.length]
    traverse(deptRoot, null, color)
  })
  
  // 创建力导向图模拟 - 部门之间有斥力，同部门内有引力
  const simulation = d3.forceSimulation(nodes)
    .force('link', d3.forceLink(links).id(d => d.id).distance(100).strength(0.8))
    .force('charge', d3.forceManyBody().strength(-400))
    .force('center', d3.forceCenter(containerWidth / 2, containerHeight / 2))
    .force('collision', d3.forceCollide().radius(50))
    // 部门集群：同色节点相互吸引
    .force('cluster', d3.forceX().x(d => {
      const deptIndex = deptColors.indexOf(d.deptColor)
      const cols = 3
      const col = deptIndex % cols
      return (containerWidth / (cols + 1)) * (col + 1)
    }).strength(0.1))
    .force('clusterY', d3.forceY().y(d => {
      const deptIndex = deptColors.indexOf(d.deptColor)
      const cols = 3
      const row = Math.floor(deptIndex / cols)
      return (containerHeight / 3) * (row + 1)
    }).strength(0.1))
  
  // 保存simulation引用以便后续使用
  root = { nodes, links, simulation }
  
  // 收集所有节点用于列表显示（使用第一个部门作为虚拟根）
  const treeRoot = d3.hierarchy(builtTree[0] || { id: 'empty', title: '空', children: [] })
  
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
  // 绘制连线 - 使用源节点的部门颜色
  const link = g.selectAll('.link')
    .data(links)
    .enter()
    .append('line')
    .attr('class', 'link')
    .attr('stroke', d => d.source.deptColor || '#999')
    .attr('stroke-opacity', 0.4)
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
      focusForceNode(d)
      // 选中节点用于操作
      selectedVisualNode.value = d
    })
    .on('dblclick', (event, d) => {
      event.stopPropagation()
      // 部门根节点双击不跳转详情
      if (!d.isDepartmentRoot && !String(d.id).startsWith('dept-')) {
        viewDetail(d.id)
      }
    })
    .on('contextmenu', (event, d) => {
      event.stopPropagation()
      event.preventDefault()
      showContextMenuOnVisualNode(event, d)
    })
  
  // 绘制节点圆圈 - 使用部门颜色
  node.append('circle')
    .attr('r', d => d.isDepartmentRoot ? 28 : 18)
    .attr('fill', d => {
      if (d.isDepartmentRoot) return d.deptColor || '#409EFF'
      if (highlightedNode === d.id) return '#f56c6c'
      return '#fff'
    })
    .attr('stroke', d => {
      if (highlightedNode === d.id) return '#f56c6c'
      return d.deptColor || '#409EFF'
    })
    .attr('stroke-width', d => highlightedNode === d.id ? 3 : (d.isDepartmentRoot ? 3 : 2))
    .on('mouseenter', function(event, d) {
      d3.select(this).attr('fill', d.isDepartmentRoot ? d.deptColor : '#e6f7ff')
      d3.select(this).attr('opacity', 0.8)
    })
    .on('mouseleave', function(event, d) {
      d3.select(this).attr('fill', d.isDepartmentRoot ? (d.deptColor || '#409EFF') : (highlightedNode === d.id ? '#f56c6c' : '#fff'))
      d3.select(this).attr('opacity', 1)
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
  
  // 更新节点样式（保持其他节点原始颜色不变）
  if (g) {
    g.selectAll('.node circle')
      .attr('fill', node => {
        const idStr = String(node.id)
        // 当前聚焦的节点用橙黄色高亮
        if (idStr === String(highlightedNode)) return '#FF9800'
        // 搜索匹配的节点也保持高亮
        if (visualSearchResultIds.value.has(idStr)) return '#FF9800'
        // 其他节点保持原始颜色
        if (node.isDepartmentRoot) return node.deptColor || '#409EFF'
        return '#fff'
      })
      .attr('stroke', node => {
        const idStr = String(node.id)
        // 当前聚焦或搜索匹配的节点用深橙色边框
        if (idStr === String(highlightedNode) || visualSearchResultIds.value.has(idStr)) return '#E65100'
        // 其他节点保持原始边框颜色
        return node.deptColor || '#409EFF'
      })
      .attr('stroke-width', node => {
        const idStr = String(node.id)
        if (idStr === String(highlightedNode) || visualSearchResultIds.value.has(idStr)) return 4
        return node.isDepartmentRoot ? 3 : 2
      })
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
  if (!d || !svg || !zoom) return
  
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
  if (!svg || !zoom) return
  currentTransform = currentTransform.scale(1.2)
  svg.transition().duration(300).call(zoom.transform, currentTransform)
}

const zoomOut = () => {
  if (!svg || !zoom) return
  currentTransform = currentTransform.scale(1 / 1.2)
  svg.transition().duration(300).call(zoom.transform, currentTransform)
}

const resetZoom = () => {
  if (!svg || !zoom || !treeContainer.value) return
  
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
    if (!g || !g.node()) {
      currentTransform = d3.zoomIdentity
      svg.transition().duration(500).call(zoom.transform, currentTransform)
      return
    }
    
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
    if (zoom) {
      svg.transition().duration(500).call(zoom.transform, currentTransform)
    }
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

// 图画模式搜索选择处理
const handleVisualSearchSelect = (item) => {
  searchKeyword.value = item.value
  if (item.type !== 'history') {
    saveSearchHistory(item.value)
  }
  handleSearch()
}

// 搜索输入处理（带防抖）
let visualSearchDebounceTimer = null
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
  
  // 防抖处理
  if (visualSearchDebounceTimer) {
    clearTimeout(visualSearchDebounceTimer)
  }
  visualSearchDebounceTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}

// 搜索结果ID集合（用于高亮多个结果）
const visualSearchResultIds = ref(new Set())

// ---- 搜索辅助方法：本地模糊匹配 & 高亮生成 ----
const escapeRegExp = (str) => str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const buildHighlightHtml = (text, keyword) => {
  if (!text || !keyword) return null
  
  // 支持多词搜索（空格分隔）
  const words = keyword.trim().split(/\s+/).filter(w => w.length > 0)
  
  let result = text
  let hasHighlight = false
  
  for (const word of words) {
    const escapedWord = escapeRegExp(word)
    const regex = new RegExp(`(${escapedWord})`, 'ig')
    
    // 先尝试精确匹配
    if (regex.test(result)) {
      result = result.replace(regex, '<mark>$1</mark>')
      hasHighlight = true
    } else {
      // 尝试模糊匹配：高亮每个匹配的字符
      const wordLower = word.toLowerCase()
      const resultLower = result.toLowerCase()
      let matchPositions = []
      let patternIndex = 0
      
      for (let i = 0; i < resultLower.length && patternIndex < wordLower.length; i++) {
        if (resultLower[i] === wordLower[patternIndex]) {
          matchPositions.push(i)
          patternIndex++
        }
      }
      
      // 如果所有字符都匹配了
      if (patternIndex === wordLower.length && matchPositions.length > 0) {
        // 从后往前替换，避免位置偏移
        let chars = result.split('')
        for (let i = matchPositions.length - 1; i >= 0; i--) {
          const pos = matchPositions[i]
          chars[pos] = `<mark>${chars[pos]}</mark>`
        }
        result = chars.join('')
        hasHighlight = true
      }
    }
  }
  
  return hasHighlight ? result : null
}

const buildHighlightPayload = (node, keyword) => {
  const payload = {}
  const titleHighlight = buildHighlightHtml(node.title || '', keyword)
  const summaryHighlight = buildHighlightHtml(node.summary || node.content || '', keyword)
  const keywordsHighlight = buildHighlightHtml(node.keywords || '', keyword)

  if (titleHighlight) payload.title = [titleHighlight]
  if (summaryHighlight) payload.content = [summaryHighlight]
  if (keywordsHighlight) payload.keywords = [keywordsHighlight]

  if (Object.keys(payload).length > 0) {
    payload.matchLocation = titleHighlight ? '匹配位置：标题' : '匹配位置：内容'
    return payload
  }
  return null
}

const collectSearchableNodes = () => {
  const result = []
  const traverse = (nodes = []) => {
    nodes.forEach(node => {
      result.push(node)
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  traverse(treeData.value || [])
  return result.filter(n => !n.isDepartmentRoot && !String(n.id).startsWith('dept-'))
}

// 模糊匹配：检查搜索字符是否按顺序出现在文本中
const fuzzyMatch = (text, pattern) => {
  if (!text || !pattern) return { match: false, score: 0 }
  
  const textLower = text.toLowerCase()
  const patternLower = pattern.toLowerCase()
  
  // 1. 精确子串匹配（最高优先级）
  if (textLower.includes(patternLower)) {
    // 匹配位置越靠前，分数越高
    const index = textLower.indexOf(patternLower)
    return { match: true, score: 100 - index, type: 'exact' }
  }
  
  // 2. 字符序列匹配（模糊匹配）
  // 例如：搜索 "知管" 可以匹配 "知识管理"
  let patternIndex = 0
  let matchPositions = []
  
  for (let i = 0; i < textLower.length && patternIndex < patternLower.length; i++) {
    if (textLower[i] === patternLower[patternIndex]) {
      matchPositions.push(i)
      patternIndex++
    }
  }
  
  if (patternIndex === patternLower.length) {
    // 计算匹配分数：字符越连续分数越高
    let consecutiveBonus = 0
    for (let i = 1; i < matchPositions.length; i++) {
      if (matchPositions[i] === matchPositions[i-1] + 1) {
        consecutiveBonus += 10
      }
    }
    // 匹配位置越靠前，分数越高
    const positionScore = 50 - matchPositions[0]
    return { match: true, score: positionScore + consecutiveBonus, type: 'fuzzy', positions: matchPositions }
  }
  
  return { match: false, score: 0 }
}

// 多词匹配：用空格分隔的多个关键词，全部匹配才返回 true
const multiWordMatch = (text, keywords) => {
  if (!text || !keywords) return { match: false, score: 0 }
  
  // 按空格分割关键词
  const words = keywords.trim().split(/\s+/).filter(w => w.length > 0)
  if (words.length === 0) return { match: false, score: 0 }
  
  // 单个关键词直接模糊匹配
  if (words.length === 1) {
    return fuzzyMatch(text, words[0])
  }
  
  // 多个关键词：每个都要匹配
  let totalScore = 0
  for (const word of words) {
    const result = fuzzyMatch(text, word)
    if (!result.match) {
      return { match: false, score: 0 }
    }
    totalScore += result.score
  }
  
  return { match: true, score: totalScore / words.length, type: 'multi' }
}

// 对节点进行模糊匹配，返回匹配结果和分数
const fuzzyMatchNode = (node, keyword) => {
  const fields = [
    { text: node.title || '', weight: 3 },      // 标题权重最高
    { text: node.keywords || '', weight: 2 },   // 关键词权重次之
    { text: node.summary || '', weight: 1 },    // 摘要
    { text: node.fileName || '', weight: 1 }    // 文件名
  ]
  
  let bestMatch = { match: false, score: 0, field: '' }
  
  for (const field of fields) {
    const result = multiWordMatch(field.text, keyword)
    if (result.match) {
      const weightedScore = result.score * field.weight
      if (weightedScore > bestMatch.score) {
        bestMatch = { ...result, score: weightedScore, field: field.text }
      }
    }
  }
  
  return bestMatch
}

const mergeWithLocalFuzzyResults = (remoteResults = [], keyword = '') => {
  if (!keyword) return remoteResults

  const existingIds = new Set(remoteResults.map(r => String(r.id)))
  const merged = [...remoteResults]
  const fuzzyMatched = []

  collectSearchableNodes().forEach(node => {
    if (existingIds.has(String(node.id))) return
    
    const matchResult = fuzzyMatchNode(node, keyword)
    if (matchResult.match) {
      fuzzyMatched.push({
        ...node,
        highlight: buildHighlightPayload(node, keyword),
        department: node.department,
        _fuzzyScore: matchResult.score
      })
      existingIds.add(String(node.id))
    }
  })
  
  // 按模糊匹配分数排序
  fuzzyMatched.sort((a, b) => (b._fuzzyScore || 0) - (a._fuzzyScore || 0))
  
  // 合并结果
  merged.push(...fuzzyMatched)

  // 统一生成高亮信息，避免后端未返回时无标签可用
  return merged.map(item => {
    const highlight = item.highlight || buildHighlightPayload(item, keyword)
    return { ...item, highlight }
  })
}

// 搜索节点
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    highlightedNode = null
    visualSearchResultIds.value = new Set()
    if (g && root && root.nodes) {
      // 重置所有节点样式（恢复原始颜色）
      g.selectAll('.node circle')
        .attr('fill', d => d.isDepartmentRoot ? (d.deptColor || '#409EFF') : '#fff')
        .attr('stroke', d => d.deptColor || '#409EFF')
        .attr('stroke-width', d => d.isDepartmentRoot ? 3 : 2)
    }
    return
  }
  
  if (!root || !root.nodes || !svg) return
  
  try {
    // 保存搜索历史
    saveSearchHistory(searchKeyword.value.trim())
    
    // 使用搜索API
    const keyword = searchKeyword.value.trim()
    const res = await api.post('/knowledge/search', {
      keyword,
      searchType: 'AUTO'
    })
    
    let searchResults = res.data.results || []
    // 合并本地模糊匹配，支持前缀/子串匹配
    searchResults = mergeWithLocalFuzzyResults(searchResults, keyword)
    
    if (searchResults.length === 0) {
      visualSearchResultIds.value = new Set()
      ElMessage.warning('未找到匹配的节点')
      // 重置样式（恢复原始颜色）
      if (g) {
        g.selectAll('.node circle')
          .attr('fill', d => d.isDepartmentRoot ? (d.deptColor || '#409EFF') : '#fff')
          .attr('stroke', d => d.deptColor || '#409EFF')
          .attr('stroke-width', d => d.isDepartmentRoot ? 3 : 2)
      }
      return
    }
    
    // 保存搜索结果ID
    visualSearchResultIds.value = new Set(searchResults.map(r => String(r.id)))
    
    // 高亮所有匹配的节点（保持其他节点原始颜色不变）
    if (g) {
      g.selectAll('.node circle')
        .attr('fill', d => {
          const idStr = String(d.id)
          // 匹配节点用醒目的橙黄色高亮
          if (visualSearchResultIds.value.has(idStr)) return '#FF9800'
          // 其他节点保持原始颜色
          if (d.isDepartmentRoot) return d.deptColor || '#409EFF'
          return '#fff'
        })
        .attr('stroke', d => {
          const idStr = String(d.id)
          // 匹配节点用深橙色边框
          if (visualSearchResultIds.value.has(idStr)) return '#E65100'
          // 其他节点保持原始边框颜色
          return d.deptColor || '#409EFF'
        })
        .attr('stroke-width', d => {
          const idStr = String(d.id)
          // 匹配节点加粗边框
          if (visualSearchResultIds.value.has(idStr)) return 4
          return d.isDepartmentRoot ? 3 : 2
        })
    }
    
    // 聚焦到第一个匹配节点
    const foundNode = root.nodes.find(n => {
      const idStr = String(n.id)
      if (idStr === 'root' || idStr.startsWith('dept-')) return false
      return searchResults.some(result => String(result.id) === idStr)
    })
    
    if (foundNode) {
      // 更新节点的高亮信息（保证至少有标题/内容高亮）
      const searchResult = searchResults.find(r => r.id === foundNode.id)
      if (searchResult) {
        foundNode.highlight = searchResult.highlight || buildHighlightPayload(foundNode, keyword)
      }
      focusForceNode(foundNode)
      ElMessage.success(`找到 ${searchResults.length} 个匹配节点`)
    } else {
      ElMessage.warning('未找到匹配的节点')
    }
  } catch (error) {
    console.error('搜索失败', error)
    // 降级到本地搜索
    const keyword = searchKeyword.value.toLowerCase()
    const matchedNodes = root.nodes.filter(n => 
      String(n.id) !== 'root' && 
      !String(n.id).startsWith('dept-') && 
      n.title && 
      n.title.toLowerCase().includes(keyword)
    )
    
    if (matchedNodes.length > 0) {
      visualSearchResultIds.value = new Set(matchedNodes.map(n => String(n.id)))
      // 高亮匹配节点（保持其他节点原始颜色不变）
      if (g) {
        g.selectAll('.node circle')
          .attr('fill', d => {
            const idStr = String(d.id)
            // 匹配节点用醒目的橙黄色高亮
            if (visualSearchResultIds.value.has(idStr)) return '#FF9800'
            // 其他节点保持原始颜色
            if (d.isDepartmentRoot) return d.deptColor || '#409EFF'
            return '#fff'
          })
          .attr('stroke', d => {
            const idStr = String(d.id)
            // 匹配节点用深橙色边框
            if (visualSearchResultIds.value.has(idStr)) return '#E65100'
            // 其他节点保持原始边框颜色
            return d.deptColor || '#409EFF'
          })
          .attr('stroke-width', d => {
            const idStr = String(d.id)
            // 匹配节点加粗边框
            if (visualSearchResultIds.value.has(idStr)) return 4
            return d.isDepartmentRoot ? 3 : 2
          })
      }
      focusForceNode(matchedNodes[0])
      ElMessage.success(`找到 ${matchedNodes.length} 个匹配节点`)
    } else {
      ElMessage.warning('未找到匹配的节点')
    }
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

// 搜索历史
const searchHistory = ref([])
const MAX_SEARCH_HISTORY = 10

// 保存搜索历史
const saveSearchHistory = (keyword) => {
  if (!keyword || !keyword.trim()) return
  const trimmed = keyword.trim()
  // 移除重复项
  const index = searchHistory.value.findIndex(item => item.value === trimmed)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }
  // 添加到开头
  searchHistory.value.unshift({ value: trimmed, type: 'history' })
  // 限制数量
  if (searchHistory.value.length > MAX_SEARCH_HISTORY) {
    searchHistory.value = searchHistory.value.slice(0, MAX_SEARCH_HISTORY)
  }
  // 保存到本地存储
  localStorage.setItem('knowledgeTreeSearchHistory', JSON.stringify(searchHistory.value))
}

// 加载搜索历史
const loadSearchHistory = () => {
  try {
    const saved = localStorage.getItem('knowledgeTreeSearchHistory')
    if (saved) {
      searchHistory.value = JSON.parse(saved)
    }
  } catch (e) {
    console.error('加载搜索历史失败', e)
  }
}

// 获取搜索建议
const fetchSearchSuggestions = async (queryString, cb) => {
  if (!queryString || queryString.trim().length < 1) {
    // 显示搜索历史
    cb(searchHistory.value.slice(0, 5))
    return
  }

  try {
    const res = await api.get('/knowledge/search/suggestions', {
      params: {
        keyword: queryString,
        limit: 10
      }
    })

    const suggestions = []
    
    // 添加标题建议
    if (res.data.suggestions && res.data.suggestions.length > 0) {
      res.data.suggestions.forEach(title => {
        suggestions.push({
          value: title,
          type: 'title'
        })
      })
    }

    // 添加预览结果作为建议
    if (res.data.previewResults && res.data.previewResults.length > 0) {
      res.data.previewResults.forEach(item => {
        // 避免重复
        if (!suggestions.find(s => s.value === item.title)) {
          suggestions.push({
            value: item.title,
            type: 'knowledge',
            id: item.id
          })
        }
      })
    }

    cb(suggestions)
  } catch (error) {
    console.error('获取搜索建议失败', error)
    cb([])
  }
}

// 选择搜索建议
const handleSearchSelect = (item) => {
  listSearchKeyword.value = item.value
  if (item.type !== 'history') {
    saveSearchHistory(item.value)
  }
  handleListTreeSearch()
}

// 列表格式搜索输入处理（防抖）
let searchDebounceTimer = null
const handleListTreeSearchInput = () => {
  // 防抖处理
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = setTimeout(() => {
    handleListTreeSearch()
  }, 300)
}

// 列表格式搜索
const handleListTreeSearch = async () => {
  const keyword = listSearchKeyword.value?.trim()
  
  if (!keyword) {
    // 清空搜索，恢复原始数据并清除高亮
    listSearchResults.value = []
    await loadTree()
    return
  }
  
  try {
    // 保存搜索历史
    saveSearchHistory(keyword)
    
    // 使用搜索API
    const res = await api.post('/knowledge/search', {
      keyword,
      searchType: 'AUTO'
    })
    
    let searchResults = res.data.results || []
    // 合并本地模糊匹配，支持前缀/子串匹配
    searchResults = mergeWithLocalFuzzyResults(searchResults, keyword)
    
    if (searchResults.length === 0) {
      ElMessage.info('未找到匹配的知识')
      return
    }
    
    // 将搜索结果合并到树结构中，保留高亮信息
    const searchResultMap = new Map()
    const searchResultIds = new Set()
    searchResults.forEach(item => {
      const normalizedItem = {
        ...item,
        highlight: item.highlight || buildHighlightPayload(item, keyword)
      }
      searchResultMap.set(item.id, normalizedItem)
      searchResultIds.add(item.id)
    })
    // 同步带高亮的列表结果，便于调试或额外展示
    listSearchResults.value = Array.from(searchResultMap.values())
    // 搜索后默认切换到“匹配结果”分类
    activeDeptId.value = 'searchResults'
    selectedNodeIds.value = []
    
    // 递归更新树数据，添加高亮信息
    const updateTreeWithHighlight = (nodes) => {
      return nodes.map(node => {
        const highlighted = searchResultMap.get(node.id)
        const newNode = { ...node }
        if (highlighted) {
          newNode.isSearchResult = true
          newNode.highlight = highlighted.highlight || buildHighlightPayload(newNode, keyword)
        } else {
          newNode.isSearchResult = false
          newNode.highlight = null
        }
        if (newNode.children && newNode.children.length > 0) {
          newNode.children = updateTreeWithHighlight(newNode.children)
        }
        return newNode
      })
    }
    
    // 更新树数据
    const updatedTree = updateTreeWithHighlight(treeData.value)
    treeData.value = updatedTree
    
    // 搜索后默认保持在"匹配结果"分类，不再自动切换到具体部门
    // activeDeptId 已在上方设置为 'searchResults'
    
    ElMessage.success(`找到 ${searchResults.length} 个匹配项`)
  } catch (error) {
    console.error('搜索失败', error)
    ElMessage.error('搜索失败: ' + (error.message || '未知错误'))
  }
}

// 搜索结果列表
const listSearchResults = ref([])
const hasListSearch = computed(() => !!(listSearchKeyword.value && listSearchKeyword.value.trim()))

const handleListSearchResultClick = async (item) => {
  if (!item) return
  // 切换到对应部门
  let targetDeptId = activeDeptId.value
  if (item.department) {
    const dept = departmentSections.value.find(d => d.title === item.department)
    if (dept) targetDeptId = dept.id
  } else if (unclassifiedKnowledge.value.some(n => String(n.id) === String(item.id))) {
    targetDeptId = 'unclassified'
  }
  if (targetDeptId) {
    activeDeptId.value = targetDeptId
    await nextTick()
    expandSearchResultPaths(new Set([item.id]))
  }
}

// 展开包含搜索结果的节点路径
const expandSearchResultPaths = (resultIds) => {
  // 递归查找并展开包含搜索结果的路径
  const expandPaths = (nodes, path = []) => {
    for (const node of nodes) {
      const currentPath = [...path, node.id]
      if (resultIds.has(node.id)) {
        // 展开路径上的所有节点
        currentPath.forEach(id => {
          if (!expandedNodeIds.value.includes(id)) {
            expandedNodeIds.value.push(id)
          }
        })
      }
      if (node.children && node.children.length > 0) {
        expandPaths(node.children, currentPath)
      }
    }
  }
  expandPaths(activeTreeData.value)
}

// 树节点过滤方法（保留用于兼容）
const filterTreeNode = (value, data) => {
  if (!value) return true
  return data.title.toLowerCase().includes(value.toLowerCase())
}

// 判断是否为文件夹（有子节点或者没有文件ID的节点）
const isFolder = (data) => {
  // 部门根节点始终是文件夹
  if (data.isDepartmentRoot || (data.id && String(data.id).startsWith('dept-'))) {
    return true
  }
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
  // 如果是虚拟根节点或部门根节点，parentId 应该为 null
  parentNodeId.value = normalizeParentId(selectedVisualNode.value.id === 'root' ? null : selectedVisualNode.value.id)
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
  // 如果是虚拟根节点或部门根节点，parentId 应该为 null
  const nodeId = selectedVisualNode.value.id === 'root' ? null : selectedVisualNode.value.id
  uploadForm.value.parentId = normalizeParentId(nodeId)
  // 获取节点路径
  if (nodeId) {
    uploadForm.value.parentPath = getNodePath(nodeId) || selectedVisualNode.value.title || '根目录'
  } else {
    uploadForm.value.parentPath = '根目录'
  }
  showUploadDialog.value = true
}

// 上传文件到文件夹（通用方法）
const uploadToFolder = (folderData) => {
  if (!folderData || !isFolder(folderData)) return
  // 如果是虚拟根节点或部门根节点，parentId 应该为 null
  const nodeId = folderData.id === 'root' ? null : folderData.id
  uploadForm.value.parentId = normalizeParentId(nodeId)
  // 获取节点路径
  if (nodeId) {
    uploadForm.value.parentPath = getNodePath(nodeId) || folderData.title || '根目录'
  } else {
    uploadForm.value.parentPath = '根目录'
  }
  showUploadDialog.value = true
}

// 编辑可视化节点
const handleEditVisualNode = () => {
  if (!selectedVisualNode.value) return
  // 虚拟根节点不能编辑
  if (selectedVisualNode.value.id === 'root' || selectedVisualNode.value.isRoot) {
    ElMessage.warning('根节点不能编辑')
    return
  }
  editNode(selectedVisualNode.value)
}

// 删除可视化节点
const handleDeleteVisualNode = () => {
  if (!selectedVisualNode.value) return
  // 虚拟根节点和部门根节点不能删除
  if (selectedVisualNode.value.id === 'root' || selectedVisualNode.value.isRoot || 
      selectedVisualNode.value.isDepartmentRoot || String(selectedVisualNode.value.id).startsWith('dept-')) {
    ElMessage.warning('根节点不能删除')
    selectedVisualNode.value = null
    return
  }
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
  const parentId = normalizeParentId(nodeData.parentId || null)
  
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
      keywords: '',
      department: null
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
  // 如果是虚拟根节点或部门根节点，parentId 应该为 null
  parentNodeId.value = normalizeParentId(targetNode.id === 'root' ? null : targetNode.id)
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
  // 如果是虚拟根节点或部门根节点，parentId 应该为 null
  uploadForm.value.parentId = normalizeParentId(selectedListNode.value.id === 'root' ? null : selectedListNode.value.id)
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
      parentNodeId.value = normalizeParentId(data.id)
      nodeForm.value = {
        title: '',
        summary: '',
        keywords: ''
      }
      showAddDialog.value = true
      break
    case 'newFile':
      isNewFolder.value = false
      parentNodeId.value = normalizeParentId(data.id)
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
      if (data && isFolder(data)) {
        const nodeId = data.id === 'root' ? null : data.id
        uploadForm.value.parentId = normalizeParentId(nodeId)
        // 获取节点路径
        if (nodeId) {
          uploadForm.value.parentPath = getNodePath(nodeId) || data.title || '根目录'
        } else {
          uploadForm.value.parentPath = '根目录'
        }
        showUploadDialog.value = true
      }
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
    let actualParentId = moveTargetId.value;
    if (actualParentId !== null) {
      actualParentId = normalizeParentId(actualParentId);
    }
    const res = await api.put(`/knowledge/${moveSourceId.value}/move`, {
      parentId: actualParentId,
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
      
      // 设计原则：部门根节点是虚拟节点，parentId 应该为 null
      // 如果 uploadForm.value.parentId 不是 null，说明是在某个文件夹下上传，需要标准化
      let actualParentId = uploadForm.value.parentId
      if (actualParentId !== null) {
        actualParentId = normalizeParentId(actualParentId)
      }
      
      // 确定 department：优先使用 uploadForm.value.department（从部门根节点上传时设置）
      // 否则使用用户信息中的部门
      const department = uploadForm.value.department || userInfo.department || '未知'
      
      // 所有上传的知识初始状态都是待审核
      const knowledgeRes = await api.post('/knowledge', {
        title: fileNameWithoutExt,
        content: fileContent,
        summary: uploadForm.value.summary || `上传的文件：${file.name}`,
        keywords: uploadForm.value.keywords || '',
        fileId: fileDTO.id,
        parentId: actualParentId,
        author: userInfo.realName || userInfo.username || '未知',
        department: department,
        createBy: userInfo.username,
        status: 'PENDING' // 所有上传的知识都是待审核状态
      })
      
      if (knowledgeRes.code !== 200) {
        throw new Error(knowledgeRes.message || '创建知识条目失败')
      }
      
      // 创建知识后，自动提交审核
      try {
        await api.post(`/knowledge/${knowledgeRes.data.id}/submit-audit?userId=${userInfo.id}`)
      } catch (error) {
        console.warn('提交审核失败，但知识已创建', error)
      }
      
      uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
    }
    
    ElMessage.success(`成功上传 ${fileList.value.length} 个文件`)
    // 先关闭对话框
    handleUploadDialogClose()
    // 等待一小段时间确保后端数据同步完成
    await new Promise(resolve => setTimeout(resolve, 500))
    // 强制刷新树
    await refreshTree()
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
    department: null,
    summary: '',
    keywords: '',
    parentPath: ''
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

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 监听窗口大小变化
const handleResize = () => {
  if (viewMode.value === 'visual' && svg && zoom && treeContainer.value) {
    const containerWidth = treeContainer.value.clientWidth
    const containerHeight = treeContainer.value.clientHeight || 800
    svg.attr('width', containerWidth).attr('height', containerHeight)
    resetZoom()
  }
}

onMounted(() => {
  loadTree()
  loadSearchHistory()
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
  padding: 24px;
}

.tree-list-container {
  flex: 1;
  padding: 16px;
  background: linear-gradient(to bottom, #f8f9fa, #ffffff);
  border-radius: 8px;
  min-height: 500px;
  height: calc(100vh - 300px);
  overflow: auto;
}

/* 现代化树组件样式 */
.modern-tree :deep(.el-tree-node) {
  margin-bottom: 12px;
}

.modern-tree :deep(.el-tree-node__content) {
  height: auto;
  min-height: 0;
  padding: 0;
  margin-bottom: 8px;
}

.modern-tree :deep(.el-tree-node__expand-icon) {
  display: none !important;
}

.modern-tree :deep(.el-tree-node__children) {
  padding-left: 32px;
  margin-top: 8px;
  margin-left: 24px;
  position: relative;
}

.modern-tree :deep(.el-tree-node__children)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, #e4e7ed 0%, transparent 100%);
  border-radius: 2px;
}

/* 节点卡片样式 */
.tree-node-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 16px 20px;
  margin-bottom: 8px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.tree-node-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  transition: background 0.3s;
}

.tree-node-card:hover {
  background: linear-gradient(to right, #f0f9ff, #ffffff);
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.tree-node-card:hover::before {
  background: linear-gradient(to bottom, #409EFF, #66b1ff);
}

.tree-node-card.node-selected {
  background: linear-gradient(to right, #ecf5ff, #ffffff);
  border-color: #409EFF;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2);
}

.tree-node-card.node-selected::before {
  background: linear-gradient(to bottom, #409EFF, #66b1ff);
}

.node-main-content {
  display: flex;
  align-items: flex-start;
  flex: 1;
  gap: 16px;
  min-width: 0;
}

.node-icon-wrapper {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
  border-radius: 10px;
  transition: all 0.3s;
}

.tree-node-card:hover .node-icon-wrapper {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  transform: scale(1.1);
}

.node-icon {
  font-size: 24px;
  transition: all 0.3s;
}

.folder-icon {
  color: #FF9800;
}

.file-icon {
  color: #409EFF;
}

.tree-node-card:hover .folder-icon {
  color: #FF6B00;
  transform: scale(1.1);
}

.tree-node-card:hover .file-icon {
  color: #66b1ff;
  transform: scale(1.1);
}

.node-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.node-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.node-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.folder-tag,
.file-tag {
  flex-shrink: 0;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.node-summary {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-top: 4px;
}

.node-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.meta-item .el-icon {
  font-size: 14px;
}

.node-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.3s;
  margin-left: 16px;
}

.tree-node-card:hover .node-actions {
  opacity: 1;
}

.action-btn {
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f0f9ff;
  transform: translateY(-1px);
}

/* ==================== 列表视图样式（简洁风格） ==================== */
.list-view-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  min-height: 0;
}

/* 顶部工具栏 */
.list-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 批量操作栏 */
.list-batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #ecf5ff;
  border-bottom: 1px solid #b3d8ff;
  flex-shrink: 0;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-left .selected-text {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.batch-left .file-count {
  font-size: 13px;
  color: #67c23a;
}

.batch-right {
  display: flex;
  gap: 8px;
}

/* 主内容区 */
.list-main-content {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

/* 左侧部门列表 */
.dept-list-panel {
  width: 220px;
  border-right: 1px solid #e4e7ed;
  background: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.dept-list-title {
  padding: 16px 16px 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.search-match-panel {
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.match-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.match-panel-body {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  /* 隐藏滚动条但保持滚动功能 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.match-panel-body::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.match-panel-item {
  display: flex;
  gap: 8px;
  padding: 8px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.match-panel-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.12);
}

.match-icon {
  color: #409eff;
  font-size: 16px;
  margin-top: 2px;
}

.match-info {
  flex: 1;
  min-width: 0;
}

.match-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  word-break: break-all;
}

.match-extra {
  margin-top: 4px;
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.match-location {
  color: #e6a23c;
}

.match-dept {
  color: #606266;
}

.dept-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  /* 隐藏滚动条但保持滚动功能 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.dept-list::-webkit-scrollbar {
  display: none;
}

.dept-list-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.dept-list-item:hover {
  background: #f5f7fa;
}

.dept-list-item.is-active {
  background: #409eff;
  color: #fff;
}

.dept-list-item.is-active .dept-item-icon,
.dept-list-item.is-active .dept-item-name {
  color: #fff;
}

.dept-list-item.is-active .dept-item-badge :deep(.el-badge__content) {
  background: #fff;
  color: #409eff;
}

.dept-item-icon {
  font-size: 18px;
  color: #409eff;
  margin-right: 10px;
  flex-shrink: 0;
}

.dept-list-item.unclassified .dept-item-icon {
  color: #e6a23c;
}

.dept-item-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dept-item-badge {
  flex-shrink: 0;
}

.dept-item-badge :deep(.el-badge__content) {
  font-size: 11px;
}

/* 右侧文件树面板 */
.file-tree-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
}

.current-dept-header {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
}

.back-btn {
  font-size: 18px;
  color: #909399;
  cursor: pointer;
  margin-right: 12px;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.back-btn:hover {
  color: #409eff;
  background: #ecf5ff;
}

.current-dept-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-right: 12px;
}

/* 文件树内容 */
.file-tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-btns {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

/* 未选择部门时的提示 */
.no-dept-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.no-dept-selected .tip-icon {
  font-size: 64px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

.no-dept-selected p {
  font-size: 16px;
  margin: 0;
  color: #606266;
}

.no-dept-selected .sub-tip {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

/* 可拖拽文件树 */
.draggable-file-tree {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.tree-table-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  font-weight: 500;
  color: #909399;
  gap: 12px;
}

.tree-table-header .el-checkbox {
  margin-right: 4px;
}

.col-name {
  flex: 1;
  min-width: 200px;
  padding-left: 40px;
}

.col-author {
  width: 100px;
  text-align: center;
}

.col-time {
  width: 120px;
  text-align: center;
}

.col-actions {
  width: 180px;
  text-align: center;
}

.tree-table-body {
  max-height: calc(100vh - 380px);
  overflow-y: auto;
}

/* 滚动条美化 */
.dept-list::-webkit-scrollbar,
.file-tree-body::-webkit-scrollbar,
.tree-table-body::-webkit-scrollbar {
  width: 6px;
}

.dept-list::-webkit-scrollbar-track,
.file-tree-body::-webkit-scrollbar-track,
.tree-table-body::-webkit-scrollbar-track {
  background: #f5f7fa;
}

.dept-list::-webkit-scrollbar-thumb,
.file-tree-body::-webkit-scrollbar-thumb,
.tree-table-body::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.dept-list::-webkit-scrollbar-thumb:hover,
.file-tree-body::-webkit-scrollbar-thumb:hover,
.tree-table-body::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* 旧列表视图样式（保留兼容） */
.list-view-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.list-toolbar {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #fafbfc, #fff);
  flex-shrink: 0;
}

.list-toolbar .toolbar-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.list-toolbar .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.list-batch-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-bottom: 2px solid #409EFF;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.1);
}

.list-batch-toolbar .toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.list-batch-toolbar .toolbar-right {
  display: flex;
  gap: 10px;
}

.list-batch-toolbar .selected-count {
  font-size: 14px;
  font-weight: 600;
  color: #409EFF;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
}

.list-batch-toolbar .selected-files-count {
  font-size: 13px;
  color: #67c23a;
}

.dept-sections-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0;
}

.dept-section {
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: all 0.3s;
}

.dept-section:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.dept-section.is-expanded {
  border-color: #409EFF;
}

.dept-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(to right, #f8f9fa, #fff);
  cursor: pointer;
  transition: background 0.2s;
}

.dept-section-header:hover {
  background: linear-gradient(to right, #ecf5ff, #f5f7fa);
}

.dept-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 部门分区展开图标 */
.expand-icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #f5f5f5;
  transition: all 0.2s ease;
  cursor: pointer;
  flex-shrink: 0;
}

.expand-icon:hover {
  background: #e8e8e8;
}

/* 隐藏默认箭头 */
.expand-icon svg {
  display: none;
}

/* 使用 CSS 绘制 chevron */
.expand-icon::before {
  content: '';
  display: block;
  width: 5px;
  height: 5px;
  border-right: 1.5px solid #606266;
  border-bottom: 1.5px solid #606266;
  transform: rotate(-45deg);
  transition: transform 0.2s ease;
  margin-left: -1px;
}

.expand-icon:hover::before {
  border-color: #409EFF;
}

/* 展开状态 */
.expand-icon.is-expanded {
  background: #e0f0ff;
}

.expand-icon.is-expanded::before {
  transform: rotate(45deg);
  margin-left: 0;
  margin-top: -1px;
  border-color: #409EFF;
}

.dept-icon {
  font-size: 22px;
  color: #409EFF;
}


.dept-icon.unclassified-icon {
  color: #E6A23C;
}

.dept-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.dept-header-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.dept-section-header:hover .dept-header-actions {
  opacity: 1;
}

.dept-section-content {
  padding: 0;
  border-top: 1px solid #ebeef5;
  background: #fafbfc;
}

.table-wrapper {
  padding: 16px 20px;
  max-height: 600px;
  overflow-y: auto;
  overflow-x: hidden;
}

.table-wrapper::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.table-wrapper::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 4px;
}

.table-wrapper::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}

/* 拖拽样式 - 简洁风格 */
.sortable-ghost {
  opacity: 0.4;
  background: #e8f4ff !important;
}

.sortable-chosen {
  background: #fff !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 1000;
}

.sortable-drag {
  opacity: 0.95;
}

/* 拖拽时的行样式 */
.knowledge-tree-table :deep(.el-table__row.sortable-ghost) {
  background: #e8f4ff !important;
}

.knowledge-tree-table :deep(.el-table__row.sortable-chosen) {
  background: #fff !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

/* 拖拽到文件夹时的高亮样式 */
.knowledge-tree-table :deep(.el-table__row.drag-over-folder) {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%) !important;
  box-shadow: inset 0 0 0 2px #1890ff;
  position: relative;
}

.knowledge-tree-table :deep(.el-table__row.drag-over-folder)::after {
  content: '放开移入此文件夹';
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: #1890ff;
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  z-index: 10;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a4a9ae;
}

.table-wrapper :deep(.el-table) {
  background: #fff;
}

.table-wrapper :deep(.el-table__header th) {
  background: #fafbfc;
  font-weight: 500;
  color: #606266;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.table-wrapper :deep(.el-table__header-wrapper) {
  background: #fff;
}

.empty-section {
  padding: 40px 20px;
  text-align: center;
  background: #fff;
}


.unclassified-section {
  border-color: #E6A23C;
  background: linear-gradient(to right, #fdf6ec, #fff);
}

.unclassified-section .dept-section-header {
  background: linear-gradient(to right, #fdf6ec, #fff);
}

/* 部门内树节点样式 */
.dept-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 8px 0;
}

/* 列表表格树形结构样式 - 简洁现代风格 */
.knowledge-tree-table {
  --el-table-indent-size: 24px;
}

/* 树形表格的缩进 */
.knowledge-tree-table :deep(.el-table__indent) {
  padding-left: 24px !important;
  display: inline-block;
  flex-shrink: 0;
}

/* 表格行样式 */
.knowledge-tree-table :deep(.el-table__body-wrapper) .el-table__row {
  background-color: #fff;
  transition: background-color 0.2s ease;
}

.knowledge-tree-table :deep(.el-table__body-wrapper) .el-table__row:hover {
  background-color: #f5f7fa !important;
}

/* 单元格样式 */
.knowledge-tree-table :deep(.el-table__body-wrapper) td {
  padding: 8px 12px !important;
  border-bottom: 1px solid #f0f0f0;
}

.knowledge-tree-table :deep(.el-table__body-wrapper) .el-table__row:hover td {
  border-bottom-color: #e0e0e0;
}

/* 修复表格单元格布局 - 确保展开图标和内容在同一行 */
.knowledge-tree-table :deep(td .cell) {
  display: flex !important;
  align-items: center !important;
  flex-wrap: nowrap !important;
}

/* 树节点内容容器 */
.knowledge-tree-table :deep(td .cell .tree-node-item) {
  display: inline-flex !important;
  align-items: center;
  flex: 1;
  min-width: 0;
}

/* 展开图标样式 - 现代风格 */
.knowledge-tree-table :deep(.el-table__expand-icon) {
  width: 22px;
  height: 22px;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #f5f5f5;
  transition: all 0.2s ease;
  margin-right: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.knowledge-tree-table :deep(.el-table__expand-icon:hover) {
  background: #e8e8e8;
}

/* 隐藏默认箭头图标 */
.knowledge-tree-table :deep(.el-table__expand-icon > .el-icon),
.knowledge-tree-table :deep(.el-table__expand-icon svg) {
  display: none !important;
}

/* 自定义展开图标 - 使用 CSS 绘制 chevron */
.knowledge-tree-table :deep(.el-table__expand-icon)::before {
  content: '';
  display: block;
  width: 5px;
  height: 5px;
  border-right: 1.5px solid #606266;
  border-bottom: 1.5px solid #606266;
  transform: rotate(-45deg);
  transition: transform 0.2s ease;
  margin-left: -1px;
}

.knowledge-tree-table :deep(.el-table__expand-icon:hover)::before {
  border-color: #409EFF;
}

/* 展开状态 - 箭头向下 */
.knowledge-tree-table :deep(.el-table__expand-icon--expanded) {
  background: #e0f0ff;
}

.knowledge-tree-table :deep(.el-table__expand-icon--expanded)::before {
  transform: rotate(45deg);
  margin-left: 0;
  margin-top: -1px;
  border-color: #409EFF;
}

/* 没有子节点时的占位符 */
.knowledge-tree-table :deep(.el-table__placeholder) {
  width: 30px;
  display: inline-block;
}

/* 文件节点样式 - 简洁风格 */
.tree-node-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
  cursor: pointer;
  user-select: none;
  flex: 1;
  min-width: 0;
}

.tree-node-item:hover {
  background-color: #f0f7ff;
}

/* 拖拽手柄 - 六点图标 */
.tree-node-item .drag-handle {
  width: 16px;
  height: 20px;
  cursor: grab;
  opacity: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  transition: opacity 0.15s ease, background-color 0.15s ease;
  position: relative;
}

/* 六点拖拽手柄 */
.tree-node-item .drag-handle::before {
  content: '';
  width: 4px;
  height: 4px;
  background: #bbb;
  border-radius: 50%;
  box-shadow: 
    6px 0 0 #bbb,
    0 6px 0 #bbb,
    6px 6px 0 #bbb,
    0 12px 0 #bbb,
    6px 12px 0 #bbb;
}

.tree-node-item:hover .drag-handle {
  opacity: 1;
}

.tree-node-item .drag-handle:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.tree-node-item .drag-handle:hover::before {
  background: #666;
  box-shadow: 
    6px 0 0 #666,
    0 6px 0 #666,
    6px 6px 0 #666,
    0 12px 0 #666,
    6px 12px 0 #666;
}

.tree-node-item .drag-handle:active {
  cursor: grabbing;
  background-color: rgba(0, 0, 0, 0.08);
}

/* 文件/文件夹图标 */
.tree-node-item .node-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.tree-node-item .folder-icon {
  color: #f5a623;
}

.tree-node-item .file-icon {
  color: #409EFF;
}

/* 文件名称 */
.tree-node-item .node-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-item:hover .node-title {
  color: #409EFF;
}

.tree-node-item .shared-tag {
  margin-left: 8px;
}

.tree-node-item .node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-item:hover .node-actions {
  opacity: 1;
}

/* 搜索高亮样式 */
.tree-node-item .node-title :deep(mark) {
  background-color: #fff566;
  color: #303133;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: 700;
}

.tree-node-item .match-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
}

/* 搜索建议样式 */
.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.suggestion-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.suggestion-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.history-icon {
  color: #909399;
  font-size: 14px;
}

.keyword-icon {
  color: #409eff;
  font-size: 14px;
}

.knowledge-icon {
  color: #67c23a;
  font-size: 14px;
}

:deep(.search-autocomplete .el-autocomplete-suggestion__list) {
  padding: 0;
}

:deep(.search-autocomplete .el-autocomplete-suggestion__list li) {
  padding: 0 15px;
  line-height: normal;
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

