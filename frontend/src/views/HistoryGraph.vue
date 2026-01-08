<template>
  <div class="history-graph-wrapper">
    <div v-if="!versions || versions.length === 0" class="empty-graph">
      暂无历史数据
    </div>
    <div v-else class="graph-canvas" ref="graphContainer"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, onUnmounted, nextTick } from 'vue'
import * as d3 from 'd3'

const props = defineProps({
  versions: {
    type: Array,
    default: () => []
  },
  currentVersion: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['version-click'])

const graphContainer = ref(null)
let simulation = null

// 配置
const config = {
  nodeRadius: 6,
  nodeStrokeWidth: 2,
  rowHeight: 40,
  colWidth: 60,
  marginTop: 20,
  marginLeft: 40,
  colors: {
    main: '#409EFF',     // 主分支
    user: '#67C23A',     // 用户分支
    merged: '#909399',   // 归档/合并
    pending: '#E6A23C',  // 待审核
    rejected: '#F56C6C', // 已驳回
    current: '#F56C6C'   // 当前查看版本高亮
  }
}

// 监听数据变化重新渲染
watch(() => props.versions, () => {
  nextTick(() => {
    renderGraph()
  })
}, { deep: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
  renderGraph()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
    renderGraph()
}

const renderGraph = () => {
  if (!graphContainer.value || !props.versions || props.versions.length === 0) return

  // 1. 数据预处理：构建节点和连线
  // 我们需要根据 parentCommitId 和 mergeFromVersion 构建 DAG
  // 并且计算每个节点的 (x, y) 坐标
  // y 坐标通常对应时间或版本号 (倒序)
  // x 坐标对应分支 (Lane)
  
  const nodes = JSON.parse(JSON.stringify(props.versions))
  // Sort by version desc (newest first) for Y axis calculation
  nodes.sort((a, b) => b.version - a.version)
  
  // Assign simple Y based on index (or version number gap)
  const versionMap = new Map(nodes.map(n => [n.version, n]))
  
  // Lane assignment (Simple logic: Main=0, Others=Allocated)
  const branches = {} // branchName -> laneIndex
  let laneCount = 1
  branches['main'] = 0
  
  // Identify branches
  nodes.forEach(node => {
      const branch = node.branch || 'main'
      if (branches[branch] === undefined) {
          branches[branch] = laneCount++
      }
      node.lane = branches[branch]
  })
  
  // Create Links
  const links = []
  nodes.forEach(node => {
      // Parent Link
      if (node.parentCommitId) {
          // Find parent node by ID (Wait, we load versions by knowledgeId, but parentCommitId refers to ID PK, not version num)
          // The backend usually returns parentCommitId as the PK ID.
          // But here in `versions` list, we have items. Let's find by id.
          const parent = nodes.find(n => n.id === node.parentCommitId)
          if (parent) {
              links.push({ source: node, target: parent, type: 'parent' })
          }
      } else if (node.version > 1) {
          // Implicit parent: version - 1 (if logical parent missing, mostly for main branch sequential)
          // But strict graph requires explicit IDs. If missing, we might assume linear history for main
          if (node.branch === 'main') {
               const prevMain = nodes.find(n => n.branch === 'main' && n.version < node.version) // Find nearest smaller
               // Actually, let's rely on parentCommitId if possible.
          }
      }
      
      // Merge Link
      if (node.mergeFromVersion) {
            // This is a merge commit. Link from source version.
            // mergeFromVersion stores the *Version Number* of the draft that was merged?
            // Or the ID? Let's check backend.
            // Backend: mergeVer.setMergeFromVersion(draftVer.getVersion()); -> It is Version Number.
            const source = nodes.find(n => n.version === node.mergeFromVersion)
            if (source) {
                links.push({ source: node, target: source, type: 'merge' })
            }
      }
  })
  
  // Improve: If parentCommitId is missing (legacy data), try to link to version-1
  nodes.forEach(node => {
      const hasParentLink = links.some(l => l.source === node && l.type === 'parent')
      if (!hasParentLink && node.version > 1) {
          // Try to find version - 1
           const prev = nodes.find(n => n.version === node.version - 1)
           if (prev) {
               links.push({ source: node, target: prev, type: 'parent' })
           }
      }
  })
  
  // Drawing dimensions
  const width = graphContainer.value.clientWidth || 600
  const height = nodes.length * config.rowHeight + config.marginTop * 2
  
  // Clear previous
  d3.select(graphContainer.value).selectAll('*').remove()
  
  const svg = d3.select(graphContainer.value)
    .append('svg')
    .attr('width', width)
    .attr('height', height)
    .append('g')
    .attr('transform', `translate(${config.marginLeft}, ${config.marginTop})`)
    
  // Draw Links
  // Curve generator
  const lineGenerator = d3.line()
    .x(d => d.x)
    .y(d => d.y)
    .curve(d3.curveMonotoneY) // Smooth curve
    
  // Calculate Node Positions
  nodes.forEach((node, index) => {
      node.x = node.lane * config.colWidth
      node.y = index * config.rowHeight
  })
  
  // Link paths
  links.forEach(link => {
      // Direct line or curved?
      // Git graph style:
      // If same lane: straight vertical
      // If diff lane: curve
      
      const source = link.source
      const target = link.target
      
      const pathData = []
      if (source.lane === target.lane) {
          pathData.push({x: source.x, y: source.y})
          pathData.push({x: target.x, y: target.y})
      } else {
           // Merge or Branch logic
           pathData.push({x: source.x, y: source.y})
           pathData.push({x: source.x, y: source.y + config.rowHeight * 0.5}) // Down a bit
           pathData.push({x: target.x, y: target.y - config.rowHeight * 0.5}) // Over and up a bit
           pathData.push({x: target.x, y: target.y})
      }
      
      link.path = pathData
  })
  
  // Render Links
  svg.selectAll('.link')
    .data(links)
    .enter()
    .append('path')
    .attr('class', d => `link ${d.type}`)
    .attr('d', d => lineGenerator(d.path))
    .attr('fill', 'none')
    .attr('stroke', '#ccc')
    .attr('stroke-width', 2)
    .attr('stroke-dasharray', d => d.type === 'merge' ? '4,2' : 'none')
    
  // Render Nodes
  const nodeGroups = svg.selectAll('.node')
    .data(nodes)
    .enter()
    .append('g')
    .attr('class', 'node')
    .attr('transform', d => `translate(${d.x}, ${d.y})`)
    .style('cursor', 'pointer')
    .on('click', (event, d) => {
        emit('version-click', d)
    })
    
  // Node Circle
  nodeGroups.append('circle')
    .attr('r', config.nodeRadius)
    .attr('fill', d => {
        if (d.version === props.currentVersion) return config.colors.current
        if (d.status === 'PENDING') return config.colors.pending
        if (d.status === 'REJECTED') return config.colors.rejected
        return d.lane === 0 ? config.colors.main : config.colors.user
    })
    .attr('stroke', '#fff')
    .attr('stroke-width', config.nodeStrokeWidth)
    
  // Labels
  nodeGroups.append('text')
    .attr('x', 12)
    .attr('y', 4)
    .text(d => `v${d.version} ${d.commitMessage || d.changeDescription || ''}`)
    .attr('font-size', '12px')
    .attr('fill', '#606266')
    .attr('class', 'node-label')
    
  // Status badges (Simplified)
  nodeGroups.append('text')
    .attr('x', -10)
    .attr('y', 4)
    .attr('text-anchor', 'end')
    .text(d => d.branch === 'main' ? '' : d.branch)
    .attr('font-size', '10px')
    .attr('fill', '#909399')
    
}
</script>

<style scoped>
.history-graph-wrapper {
  width: 100%;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background: #fff;
  min-height: 200px;
  position: relative;
}

.graph-canvas {
  width: 100%;
  overflow-x: auto;
  min-height: 200px;
}

.empty-graph {
  padding: 20px;
  text-align: center;
  color: #909399;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
}
</style>
