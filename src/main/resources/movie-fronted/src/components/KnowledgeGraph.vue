<template>
  <div class="knowledge-graph-container">
    <!-- 筛选和搜索区域 -->
    <div class="graph-controls">
      <el-select
          v-model="selectedNodeType"
          placeholder="筛选节点类型"
          class="type-selector"
          @change="filterNodes"
      >
        <el-option label="全部节点" value="all"></el-option>
        <el-option label="电影" value="movie"></el-option>
        <el-option label="导演" value="director"></el-option>
        <el-option label="演员" value="actor"></el-option>
        <el-option label="地区" value="region"></el-option>
        <el-option label="类型" value="genre"></el-option>
      </el-select>

      <el-input
          v-model="searchKeyword"
          placeholder="搜索节点名称"
          class="search-input"
          @keyup.enter="searchNodes"
      >
        <el-button slot="append" icon="el-icon-search" @click="searchNodes"></el-button>
      </el-input>

      <el-input-number
          v-model="localMovieCount"
          placeholder="电影数量"
          class="movie-count-input"
          :min="1"
          :max="100"
          @change="onMovieCountChange"
      ></el-input-number>

      <!-- 布局算法选择 -->
      <el-select
          v-model="selectedLayout"
          placeholder="选择布局算法"
          class="layout-selector"
          @change="changeLayout"
      >
        <el-option label="力导向布局" value="force"></el-option>
        <el-option label="层次布局" value="circular"></el-option>
        <el-option label="放射状布局" value="radial"></el-option>
        <el-option label="网格布局" value="grid"></el-option>
      </el-select>

      <!-- 交互控制 -->
      <div class="interaction-controls">
        <el-checkbox v-model="enableZoom" @change="updateInteraction" class="tool-check">启用缩放</el-checkbox>
        <el-checkbox v-model="enableDrag" @change="updateInteraction" class="tool-check">启用拖拽</el-checkbox>
        <el-checkbox v-model="enableNodeFollow" @change="updateInteraction" class="tool-check">节点跟随</el-checkbox>
      </div>

      <div class="toolbar-buttons">
        <el-button type="primary" @click="resetGraph" class="tool-btn reset-btn">
          <i class="el-icon-refresh-left"></i> 重置图谱
        </el-button>
        <el-button @click="expandAllNodes" class="tool-btn expand-btn">
          <i class="el-icon-arrow-down"></i> 展开全部
        </el-button>
        <el-button @click="collapseAllNodes" class="tool-btn collapse-btn">
          <i class="el-icon-arrow-up"></i> 收起全部
        </el-button>
        <el-button @click="exportGraph" class="tool-btn export-btn">
          <i class="el-icon-picture-outline"></i> 导出截图
        </el-button>
        <el-button
          type="success"
          @click="handleAiExplainGraph"
          class="tool-btn ai-explain-btn"
          :loading="aiExplaining"
          :disabled="aiExplaining"
        >
          <i class="el-icon-magic-stick"></i>
          {{ aiExplaining ? 'AI 正在解读图谱...' : 'AI 解读当前图谱' }}
        </el-button>
      </div>
    </div>

    <!-- AI 解读结果 -->
    <el-alert
        v-if="aiExplainText"
        class="ai-graph-explain"
        type="info"
        :closable="false"
        :title="aiExplainText"
        show-icon>
    </el-alert>

    <!-- 图谱渲染画布：使用绝对定位占满剩余空间 -->
    <div ref="graphCanvas" class="graph-canvas"></div>

    <!-- 电影详情弹窗 -->
    <el-dialog
        title="电影详情"
        :visible.sync="showDetailDialog"
        width="550px"
        append-to-body
        destroy-on-close
        custom-class="detail-dialog"
    >
      <div class="detail-card">
        <div class="detail-header">
          <h3 class="movie-title">{{ nodeDetail && nodeDetail.name || '' }}</h3>
          <span class="movie-rating">
            <i class="el-icon-star-on" style="color: #ffd700;"></i>
            {{ nodeDetail && nodeDetail.rating || '暂无' }}分
          </span>
        </div>
        <div class="detail-content">
          <div class="detail-item">
            <span class="detail-label">导演：</span>
            <span class="detail-value">{{ nodeDetail && nodeDetail.directors || '无' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">演员：</span>
            <span class="detail-value">{{ nodeDetail && nodeDetail.actors || '无' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">地区：</span>
            <span class="detail-value">{{ nodeDetail && nodeDetail.region || '无' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">类型：</span>
            <span class="detail-value">{{ nodeDetail && nodeDetail.genre || '无' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { arkMultiChat } from '@/api/recommend'
const echarts = require('echarts')

export default {
  name: 'KnowledgeGraph',
  props: {
    movieCount: {
      type: Number,
      default: 10
    },
    currentUser: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      graphInstance: null,
      graphData: {
        nodes: [],
        edges: []
      },
      selectedNodeType: 'all',
      searchKeyword: '',
      showDetailDialog: false,
      nodeDetail: null,
      localMovieCount: 10,
      coreMovieMap: new Map(), // 存储电影节点ID -> 子节点ID数组
      nodePositions: new Map(), // 存储节点位置关系，用于跟随拖动
      isDragging: false,
      draggedNode: null,
      dragStartPos: { x: 0, y: 0 },
      // 新增：布局算法选择
      selectedLayout: 'force', // 默认力导向布局
      // 新增：交互控制
      enableZoom: true,
      enableDrag: true,
      enableNodeFollow: true,
      // 新增：节点展开/收起状态
      expandedNodes: new Set(),
      // 新增：图谱缩放级别
      zoomLevel: 1,
      // 搜索时以该节点为中心并突出显示（存节点 id）
      searchCenterNodeId: null,
      // AI 对当前图谱的解读
      aiExplainText: '',
      aiExplaining: false
    }
  },
  mounted() {
    this.$nextTick(() => {
      // 确保画布DOM完全加载后初始化
      if (this.$refs.graphCanvas) {
        this.initGraph()
        this.localMovieCount = this.movieCount || 10
        this.fetchGraphData()
      } else {
        this.$message.error('图谱画布DOM元素未找到！')
      }
    })
  },
  beforeDestroy() {
    // 彻底销毁实例和事件，避免内存泄漏
    if (this.graphInstance) {
      this.graphInstance.off('dragstart')
      this.graphInstance.off('drag')
      this.graphInstance.off('dragend')
      this.graphInstance.dispose()
      this.graphInstance = null
    }
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    // 处理窗口大小变化
    handleResize() {
      if (this.graphInstance) {
        this.graphInstance.resize()
      }
    },

    initGraph() {
      if (!echarts) {
        this.$message.error('ECharts库加载失败！')
        return
      }
      // 初始化ECharts实例
      this.graphInstance = echarts.init(this.$refs.graphCanvas)

      // 监听窗口大小变化
      window.addEventListener('resize', this.handleResize)

      // 绑定点击事件
      this.graphInstance.on('click', (params) => {
        if (params.dataType === 'node') {
          this.showNodeDetail(params.data)
        }
      })
    },

    fetchGraphData() {
      const count = Math.min(Math.max(1, this.localMovieCount), 100)
      const params = { movieCount: count }
      if (this.searchKeyword && this.searchKeyword.trim()) {
        params.keyword = this.searchKeyword.trim()
      }
      request.get('/api/kg/graph-data', { params })
          .then(res => {
            const raw = (res && res.data) ? res.data : res
            if (raw) {
              this.graphData = { nodes: raw.nodes || [], edges: raw.edges || [] }
              this.graphData._fromKeywordSearch = !!(this.searchKeyword && this.searchKeyword.trim())
              const nodes = this.graphData.nodes || []
              if (nodes.length === 0 && this.searchKeyword && this.searchKeyword.trim()) {
                this.$message.info('未找到与「' + this.searchKeyword.trim() + '」相关的节点')
                this.searchCenterNodeId = null
              } else if (this.searchKeyword && this.searchKeyword.trim()) {
                const keyword = this.searchKeyword.trim().toLowerCase()
                const exact = nodes.find(n => n.name && n.name.toLowerCase() === keyword)
                const contains = nodes.find(n => n.name && n.name.toLowerCase().includes(keyword))
                const match = exact || contains
                this.searchCenterNodeId = match ? match.id : null
              } else {
                this.searchCenterNodeId = null
              }
              this.renderGraph()
            }
          })
          .catch(err => {
            this.$message.error('数据请求失败：' + err.message)
            console.error('图谱数据请求失败：', err)
          })
    },

    /**
     * 调用大模型，对当前知识图谱做一次自然语言导览说明。
     */
    async handleAiExplainGraph() {
      if (this.aiExplaining) return
      if (!this.graphData || !this.graphData.nodes || this.graphData.nodes.length === 0) {
        this.$message.warning('当前没有可供 AI 解读的图谱数据')
        return
      }
      this.aiExplaining = true
      try {
        const nodes = this.graphData.nodes || []
        const edges = this.graphData.edges || []
        const totalNodes = nodes.length
        const totalEdges = edges.length

        // 简单统计：按类型分类节点数量
        const typeCountMap = {}
        nodes.forEach(n => {
          const t = n.type || 'unknown'
          typeCountMap[t] = (typeCountMap[t] || 0) + 1
        })

        // 取若干度数最高的节点作为“核心节点”样本
        const degreeMap = {}
        edges.forEach(e => {
          if (!e || !e.source || !e.target) return
          degreeMap[e.source] = (degreeMap[e.source] || 0) + 1
          degreeMap[e.target] = (degreeMap[e.target] || 0) + 1
        })
        const nodesWithDegree = nodes.map(n => ({
          id: n.id,
          name: n.name,
          type: n.type,
          degree: degreeMap[n.id] || 0
        }))
        nodesWithDegree.sort((a, b) => b.degree - a.degree)
        const coreSamples = nodesWithDegree.slice(0, 8)

        const summary = {
          totalNodes,
          totalEdges,
          typeCountMap,
          coreSamples
        }

        const userId = this.currentUser && (this.currentUser.username || this.currentUser.name || this.currentUser.id) || 'anonymous'
        const question =
          `下面是当前知识图谱的结构摘要，来自用户「${userId}」浏览时的图谱视图。` +
          `请用通俗中文说明：1）这个图谱大致包含哪些类型的节点（电影/导演/演员/地区/类型等），各自数量大概多少；` +
          `2）度数较高的核心节点有哪些，他们分别代表什么含义；` +
          `3）从这个图谱可以看出这个电影数据的大致特点，比如地区分布或导演/演员合作关系。` +
          `不要输出 JSON，只输出自然语言解释。`

        const messages = [
          { role: 'user', content: question },
          { role: 'user', content: '图谱结构摘要：' + JSON.stringify(summary, null, 2) }
        ]

        const res = await arkMultiChat({ messages, userId })
        if (res && res.code === 200 && res.data) {
          this.aiExplainText = res.data
        } else {
          this.$message.error(res && res.msg ? res.msg : 'AI 解读图谱失败，请稍后重试')
        }
      } catch (e) {
        console.error('AI 解读知识图谱失败:', e)
        this.$message.error('AI 解读知识图谱失败，请稍后重试')
      } finally {
        this.aiExplaining = false
      }
    },

    // 优化后的节点大小计算
    getNodeSize(name, isCore) {
      if (!name) {
        return 40
      }
      const nameLength = name.length
      if (nameLength <= 2) return 40
      if (nameLength <= 4) return 50
      if (nameLength <= 6) return 60
      return 70
    },

    // 生成图谱数据（核心改进：建立节点跟随关系）
    generateGraphData() {
      let allNodes = [...this.graphData.nodes]
      let allEdges = [...this.graphData.edges]

      // 清空之前的映射
      this.coreMovieMap.clear()
      this.nodePositions.clear()

      // 筛选逻辑
      let filteredNodes = []
      let filteredEdges = []

      if (this.selectedNodeType === 'all') {
        filteredNodes = allNodes
        filteredEdges = allEdges
      } else {
        // 处理类型节点筛选
        if (this.selectedNodeType === 'genre') {
          // 查找所有类型边
          const genreEdges = allEdges.filter(edge => edge.label === '类型')
          if (genreEdges.length === 0) {
            return { nodes: [], edges: [], categories: this.getNodeCategories() }
          }
          
          // 提取类型节点ID
          const genreNodeIds = [...new Set(genreEdges.map(edge => edge.target))]
          
          // 查找类型节点
          const genreNodes = allNodes.filter(node => genreNodeIds.includes(node.id))
          if (genreNodes.length === 0) {
            return { nodes: [], edges: [], categories: this.getNodeCategories() }
          }
          
          // 查找与类型节点相关的所有边
          const relatedEdges = allEdges.filter(edge => 
              genreNodeIds.includes(edge.source) || genreNodeIds.includes(edge.target)
          )
          
          // 查找与这些边相关的所有节点
          const relatedNodeIds = new Set()
          relatedEdges.forEach(edge => {
            relatedNodeIds.add(edge.source)
            relatedNodeIds.add(edge.target)
          })
          
          filteredNodes = allNodes.filter(node => relatedNodeIds.has(node.id))
          filteredEdges = relatedEdges
        } else {
          // 其他类型节点的筛选逻辑
          const targetNodes = allNodes.filter(node => node.type === this.selectedNodeType)
          if (targetNodes.length === 0) {
            return { nodes: [], edges: [], categories: this.getNodeCategories() }
          }
          const targetNodeIds = targetNodes.map(node => node.id)
          const relatedEdges = allEdges.filter(edge =>
              targetNodeIds.includes(edge.source) || targetNodeIds.includes(edge.target)
          )
          const relatedNodeIds = new Set()
          relatedEdges.forEach(edge => {
            relatedNodeIds.add(edge.source)
            relatedNodeIds.add(edge.target)
          })
          filteredNodes = allNodes.filter(node => relatedNodeIds.has(node.id))
          filteredEdges = relatedEdges
        }
      }

      // 仅在全量数据下做前端关键词过滤；若本次数据是后端按 keyword 搜索返回的子图，不再过滤，直接展示
      if (this.searchKeyword && !this.graphData._fromKeywordSearch) {
        const keyword = this.searchKeyword.trim().toLowerCase()
        filteredNodes = filteredNodes.filter(node => node.name && node.name.toLowerCase().includes(keyword))
        const filteredNodeIds = filteredNodes.map(node => node.id)
        filteredEdges = filteredEdges.filter(edge =>
            filteredNodeIds.includes(edge.source) && filteredNodeIds.includes(edge.target)
        )
      }

      const nodeCategories = this.getNodeCategories()

      // 搜索节点时：以匹配节点为中心、突出显示
      if (this.searchCenterNodeId) {
        const centerNode = filteredNodes.find(n => n.id === this.searchCenterNodeId)
        if (centerNode) {
          const canvasWidth = this.$refs.graphCanvas ? this.$refs.graphCanvas.offsetWidth : 800
          const canvasHeight = this.$refs.graphCanvas ? this.$refs.graphCanvas.offsetHeight : 600
          const centerX = canvasWidth / 2
          const centerY = canvasHeight / 2
          const neighborIds = new Set()
          filteredEdges.forEach(edge => {
            if (edge.source === centerNode.id) neighborIds.add(edge.target)
            if (edge.target === centerNode.id) neighborIds.add(edge.source)
          })
          const neighborNodes = filteredNodes.filter(n => neighborIds.has(n.id))
          const otherNodes = filteredNodes.filter(n => n.id !== centerNode.id && !neighborIds.has(n.id))
          const catIdx = nodeCategories.findIndex(c => c.name === centerNode.type)
          const centerCat = catIdx >= 0 ? catIdx : 4
          const nodes = []
          const edges = []
          // 中心节点：置于画布中心，图二效果——仅清晰黄色描边、无光晕，避免被遮挡
          const centerSize = Math.max(this.getNodeSize(centerNode.name, true) * 1.4, 50)
          nodes.push({
            id: centerNode.id,
            name: centerNode.name,
            category: centerCat,
            x: centerX,
            y: centerY,
            symbolSize: centerSize,
            itemStyle: {
              ...(nodeCategories[centerCat].itemStyle || {}),
              borderWidth: 4,
              borderColor: '#FFD700',
              shadowBlur: 0,
              shadowColor: 'transparent'
            },
            label: { show: true, fontSize: 13, fontWeight: 'bold', color: '#000', overflow: 'break', lineHeight: 16 },
            draggable: true,
            type: centerNode.type,
            fixed: true
          })
          // 相邻节点：外圈半径足够大，不遮挡中心节点（中心半径约 centerSize/2，留出间距）
          const r1 = Math.max(180, centerSize + 80)
          neighborNodes.forEach((n, i) => {
            const angle = (i / Math.max(neighborNodes.length, 1)) * 2 * Math.PI
            const idx = nodeCategories.findIndex(c => c.name === n.type)
            const cat = idx >= 0 ? idx : 4
            nodes.push({
              id: n.id,
              name: n.name,
              category: cat,
              x: centerX + r1 * Math.cos(angle),
              y: centerY + r1 * Math.sin(angle),
              symbolSize: this.getNodeSize(n.name, false),
              itemStyle: nodeCategories[cat].itemStyle,
              label: { show: true, fontSize: 10, color: '#000', overflow: 'break', lineHeight: 14 },
              draggable: true,
              type: n.type,
              fixed: false
            })
          })
          // 其余节点：更外一圈，与第一圈留出间距，不互相遮挡
          const r2 = Math.max(320, r1 + 100)
          otherNodes.forEach((n, i) => {
            const angle = (i / Math.max(otherNodes.length, 1)) * 2 * Math.PI
            const idx = nodeCategories.findIndex(c => c.name === n.type)
            const cat = idx >= 0 ? idx : 4
            nodes.push({
              id: n.id,
              name: n.name,
              category: cat,
              x: centerX + r2 * Math.cos(angle),
              y: centerY + r2 * Math.sin(angle),
              symbolSize: this.getNodeSize(n.name, false),
              itemStyle: nodeCategories[cat].itemStyle,
              label: { show: true, fontSize: 10, color: '#000', overflow: 'break', lineHeight: 14 },
              draggable: true,
              type: n.type,
              fixed: false
            })
          })
          filteredEdges.forEach(edge => {
            if (nodes.find(n => n.id === edge.source) && nodes.find(n => n.id === edge.target)) {
              edges.push({
                source: edge.source,
                target: edge.target,
                label: { show: true, formatter: edge.label, fontSize: 10, color: '#666', position: 'middle' },
                lineStyle: { width: 1.5, color: '#B0C4DE', type: 'solid', opacity: 1 }
              })
            }
          })
          return { nodes, edges, categories: nodeCategories }
        }
        this.searchCenterNodeId = null
      }

      // 收集所有电影节点（作为布局中心）；若无电影节点则用任意节点保证有内容可展示
      let movieNodes = filteredNodes.filter(node => node.type === 'movie')
      if (movieNodes.length === 0 && filteredNodes.length > 0) {
        movieNodes = filteredNodes.slice(0, 1)
      }

      // 第一步：建立电影节点与子节点的映射关系
      movieNodes.forEach(movieNode => {
        if (!movieNode.id) return

        // 找到与当前电影节点相关的边
        const movieEdges = filteredEdges.filter(edge =>
            edge.source === movieNode.id || edge.target === movieNode.id
        )

        // 收集所有子节点ID
        const childNodeIds = []
        movieEdges.forEach(edge => {
          const childId = edge.source === movieNode.id ? edge.target : edge.source
          if (childId === movieNode.id) return
          const childNode = filteredNodes.find(n => n.id === childId)
          if (childNode) {
            childNodeIds.push(childId)
          }
        })

        // 存储映射关系
        this.coreMovieMap.set(movieNode.id, childNodeIds)

        // 为每个子节点存储其父电影节点的位置关系
        childNodeIds.forEach(childId => {
          if (!this.nodePositions.has(childId)) {
            this.nodePositions.set(childId, new Set())
          }
          this.nodePositions.get(childId).add(movieNode.id)
        })
      })

      // 第二步：计算布局
      const nodes = []
      const edges = []

      // 计算画布中心点
      const canvasWidth = this.$refs.graphCanvas.offsetWidth
      const canvasHeight = this.$refs.graphCanvas.offsetHeight
      const centerX = canvasWidth / 2
      const centerY = canvasHeight / 2

      // 使用放射状布局
      const radiusPerMovie = Math.min(canvasWidth, canvasHeight) * 0.35 / Math.max(1, movieNodes.length)

      // 处理电影节点
      movieNodes.forEach((movieNode, index) => {
        if (!movieNode.id) return

        // 计算电影节点位置（放射状布局）
        const angle = (index / movieNodes.length) * 2 * Math.PI
        const distance = radiusPerMovie * (index + 1)
        const movieX = centerX + distance * Math.cos(angle)
        const movieY = centerY + distance * Math.sin(angle)

        // 添加电影节点
        nodes.push({
          id: movieNode.id,
          name: movieNode.name,
          category: 0,
          x: movieX,
          y: movieY,
          symbolSize: this.getNodeSize(movieNode.name, true),
          itemStyle: nodeCategories[0].itemStyle,
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold',
            color: '#000',
            overflow: 'break',
            lineHeight: 16
          },
          draggable: true,
          type: 'movie'
        })

        // 添加电影节点的子节点
        const childNodeIds = this.coreMovieMap.get(movieNode.id) || []
        childNodeIds.forEach((childId, childIndex) => {
          const childNode = filteredNodes.find(n => n.id === childId)
          if (!childNode) return

          // 检查是否已经添加过该子节点（避免重复）
          if (nodes.find(n => n.id === childId)) return

          // 计算子节点位置（围绕电影节点，放射状分布）
          const angle = (childIndex / childNodeIds.length) * Math.PI * 2
          const radius = 80 // 固定半径，确保子节点围绕父节点合适距离
          const childX = movieX + radius * Math.cos(angle)
          const childY = movieY + radius * Math.sin(angle)

          // 确定节点类型索引
          const catIdx = nodeCategories.findIndex(c => c.name === childNode.type)

          nodes.push({
            id: childNode.id,
            name: childNode.name,
            category: catIdx >= 0 ? catIdx : 4,
            x: childX,
            y: childY,
            symbolSize: this.getNodeSize(childNode.name, false),
            itemStyle: nodeCategories[catIdx >= 0 ? catIdx : 4].itemStyle,
            label: {
              show: true,
              fontSize: 10,
              color: '#000',
              overflow: 'break',
              lineHeight: 14
            },
            draggable: true,
            type: childNode.type
          })
        })
      })

      // 第三步：添加边
      filteredEdges.forEach(edge => {
        // 确保边的两个节点都存在
        if (nodes.find(n => n.id === edge.source) && nodes.find(n => n.id === edge.target)) {
          edges.push({
            source: edge.source,
            target: edge.target,
            label: {
              show: true,
              formatter: edge.label,
              fontSize: 10,
              color: '#666',
              position: 'middle'
            },
            lineStyle: {
              width: 1.5,
              color: '#B0C4DE',
              type: 'solid',
              opacity: 1  // 修改为1，去除模糊效果
            }
          })
        }
      })

      return {
        nodes: nodes,
        edges: edges,
        categories: nodeCategories
      }
    },

    getNodeCategories() {
      return [
        { name: 'movie', itemStyle: { color: '#FF6B6B', borderWidth: 2, borderColor: '#FF4757', opacity: 1, shadowBlur: 0 } },
        { name: 'director', itemStyle: { color: '#87CEEB', borderWidth: 1, borderColor: '#4682B4', opacity: 1, shadowBlur: 0 } },
        { name: 'actor', itemStyle: { color: '#87CEEB', borderWidth: 1, borderColor: '#4682B4', opacity: 1, shadowBlur: 0 } },
        { name: 'region', itemStyle: { color: '#87CEEB', borderWidth: 1, borderColor: '#4682B4', opacity: 1, shadowBlur: 0 } },
        { name: 'genre', itemStyle: { color: '#87CEEB', borderWidth: 1, borderColor: '#4682B4', opacity: 1, shadowBlur: 0 } }
      ]
    },

    renderGraph() {
      if (!this.graphInstance) return

      const graphData = this.generateGraphData()
      const _this = this

      // 计算画布大小
      const containerWidth = this.$refs.graphCanvas.offsetWidth
      const containerHeight = this.$refs.graphCanvas.offsetHeight

      // 布局配置
      let layoutConfig = {}
      let layoutType = 'none'
      
      switch (this.selectedLayout) {
        case 'force':
          layoutType = 'force'
          layoutConfig = {
            repulsion: 100,
            edgeLength: [80, 150],
            gravity: 0.1,
            layoutAnimation: true
          }
          break
        case 'circular':
          layoutType = 'circular'
          layoutConfig = {
            rotateLabel: true
          }
          break
        case 'radial':
          layoutType = 'force'
          layoutConfig = {
            repulsion: 80,
            edgeLength: [100, 200],
            gravity: 0.15,
            layoutAnimation: true
          }
          break
        case 'grid':
          layoutType = 'force'
          layoutConfig = {
            repulsion: 50,
            edgeLength: [60, 120],
            gravity: 0.1,
            layoutAnimation: true
          }
          break
        default:
          layoutType = 'none'
          layoutConfig = {}
      }

      // ECharts配置
      const option = {
        backgroundColor: '#FFFFFF',
        tooltip: {
          show: true,
          formatter: function(params) {
            if (params.dataType === 'node') {
              const node = params.data
              let typeText = ''
              switch (node.type) {
                case 'movie': typeText = '电影'; break
                case 'director': typeText = '导演'; break
                case 'actor': typeText = '演员'; break
                case 'region': typeText = '地区'; break
                case 'genre': typeText = '类型'; break
              }
              return `${node.name}<br/>类型: ${typeText}`
            }
            return ''
          },
          backgroundColor: 'rgba(50, 50, 50, 0.8)',
          borderColor: '#333',
          borderWidth: 1,
          textStyle: {
            color: '#fff',
            fontSize: 12
          }
        },
        legend: {
          show: true,
          data: [
            {name:'电影', icon: 'circle', textStyle: {color: '#8A2BE2'}},
            {name:'导演', icon: 'circle', textStyle: {color: '#FF4500'}},
            {name:'演员', icon: 'circle', textStyle: {color: '#FF6347'}},
            {name:'地区', icon: 'circle', textStyle: {color: '#FFD700'}},
            {name:'类型', icon: 'circle', textStyle: {color: '#32CD32'}}
          ],
          top: 10,
          left: 'center',
          itemGap: 15,
          textStyle: { 
            fontSize: 13, 
            color: '#333',
            fontWeight: '500'
          },
          icon: 'circle',
          itemWidth: 12,
          itemHeight: 12
        },
        series: [
          {
            type: 'graph',
            layout: layoutType,
            force: layoutType === 'force' ? layoutConfig : null,
            circular: layoutType === 'circular' ? layoutConfig : null,
            roam: this.enableZoom,
            zoom: this.zoomLevel,
            center: ['50%', '50%'],
            draggable: this.enableDrag,
            symbol: 'circle',
            symbolSize: function(data) {
              if (!data || !data.name) {
                return 40;
              }
              const nameLength = data.name.length;
              if (nameLength <= 2) return 40;
              if (nameLength <= 4) return 50;
              if (nameLength <= 6) return 60;
              return 70;
            },
            edgeSymbol: ['none', 'none'],
            edgeSymbolSize: [0, 0],
            cursor: 'move',
            label: {
              show: true,
              position: 'inside',
              color: '#fff',
              fontSize: 12,
              fontWeight: 'normal',
              overflow: 'break',
              lineHeight: 14
            },
            edgeLabel: {
              show: false
            },
            data: graphData.nodes,
            links: graphData.edges,
            categories: graphData.categories,
            lineStyle: {
              color: '#999',
              width: 1,
              curveness: 0,
              opacity: 1,
              type: 'solid',
              shadowColor: 'none'
            },
            emphasis: {
              focus: 'none',
              blurScope: 'none',
              lineStyle: {
                width: 3,
                opacity: 1,
                color: '#ff6b6b'
              },
              itemStyle: {
                shadowBlur: 0,
                shadowColor: 'transparent',
                borderWidth: 2,
                borderColor: '#fff'
              },
              label: {
                fontSize: 13,
                fontWeight: 'bold',
                color: '#000'
              }
            },
            animationDuration: 1500,
            animationEasingUpdate: 'quinticInOut',
            scaleLimit: {
              min: 0.3,
              max: 3
            }
          }
        ]
      }

      // 清空并设置新配置
      this.graphInstance.clear()
      this.graphInstance.setOption(option, true)

      // 移除旧的事件监听器
      this.graphInstance.off('dragstart')
      this.graphInstance.off('drag')
      this.graphInstance.off('dragend')

      // 拖动开始事件
      this.graphInstance.on('dragstart', function(params) {
        if (params.dataType !== 'node') return

        _this.isDragging = true
        _this.draggedNode = params.data
        _this.dragStartPos = {
          x: params.data.x,
          y: params.data.y
        }
      })

      // 拖动事件 - 核心：子节点跟随父节点
      this.graphInstance.on('drag', function(params) {
        if (!_this.isDragging || params.dataType !== 'node') return

        // 直接从params中获取当前节点的位置
        const currentNode = params.data
        const currentX = params.event.offsetX
        const currentY = params.event.offsetY
        
        // 计算偏移量
        const dx = currentX - _this.dragStartPos.x
        const dy = currentY - _this.dragStartPos.y

        // 获取当前图表的所有节点
        const graphOption = _this.graphInstance.getOption()
        const allNodes = graphOption.series[0].data

        let nodesUpdated = false

        // 如果是电影节点被拖动，移动所有子节点
        if (currentNode.type === 'movie' && _this.coreMovieMap.has(currentNode.id)) {
          const childNodeIds = _this.coreMovieMap.get(currentNode.id)

          // 更新所有子节点的位置
          childNodeIds.forEach(childId => {
            const childNode = allNodes.find(n => n.id === childId)
            if (childNode) {
              childNode.x += dx
              childNode.y += dy
              nodesUpdated = true
            }
          })
        }

        // 如果是子节点被拖动，检查它是否属于多个父节点
        else if (currentNode.type !== 'movie' && _this.nodePositions.has(currentNode.id)) {
          const parentMovieIds = Array.from(_this.nodePositions.get(currentNode.id))

          // 去重集合，避免重复移动同一个节点
          const nodesToMove = new Set()

          // 收集所有需要移动的节点
          parentMovieIds.forEach(parentId => {
            const parentNode = allNodes.find(n => n.id === parentId && n.type === 'movie')
            if (parentNode) {
              // 添加父节点
              nodesToMove.add(parentNode)

              // 添加父节点的所有子节点（包括当前节点）
              if (_this.coreMovieMap.has(parentId)) {
                const siblingIds = _this.coreMovieMap.get(parentId)
                siblingIds.forEach(siblingId => {
                  if (siblingId !== currentNode.id) {
                    const siblingNode = allNodes.find(n => n.id === siblingId)
                    if (siblingNode) {
                      nodesToMove.add(siblingNode)
                    }
                  }
                })
              }
            }
          })

          // 统一移动所有相关节点
          nodesToMove.forEach(node => {
            node.x += dx
            node.y += dy
            nodesUpdated = true
          })
        }

        // 如果有节点位置更新，则更新图表
        if (nodesUpdated) {
          // 使用更直接的更新方式
          _this.graphInstance.setOption({
            series: [{
              data: allNodes
            }]
          }, true) // 强制重新渲染
        }

        // 更新起始位置
        _this.dragStartPos = {
          x: currentX,
          y: currentY
        }
      })

      // 拖动结束事件
      this.graphInstance.on('dragend', function() {
        _this.isDragging = false
        _this.draggedNode = null
        _this.dragStartPos = { x: 0, y: 0 }
      })

      // 点击节点事件
      this.graphInstance.on('click', params => {
        if (params.dataType === 'node') {
          _this.showNodeDetail(params.data)
        }
      })

      // 确保图表适应容器大小
      this.graphInstance.resize()
    },

    showNodeDetail(nodeModel) {
      if (nodeModel.category !== 0) {
        // 处理类型节点点击
        if (nodeModel.category === 4) {
          // 查找属于该类型的所有电影
          const movies = this.graphData.edges
              .filter(edge => edge.target === nodeModel.id && edge.label === '类型')
              .map(edge => {
                const movieNode = this.graphData.nodes.find(n => n.id === edge.source)
                return movieNode ? movieNode.name : null
              })
              .filter(Boolean)
          
          this.nodeDetail = {
            ...nodeModel,
            type: '类型',
            movies: movies.join('、')
          }
          this.showDetailDialog = true
        } else {
          this.$message.info(`${nodeModel.name} | ${['电影','导演','演员','地区','类型'][nodeModel.category] || '未知'}`)
        }
        return
      }

      // 获取电影详情信息
      const directors = this.graphData.edges
          .filter(edge => edge.source === nodeModel.id && edge.label === '导演')
          .map(edge => {
            const tNode = this.graphData.nodes.find(n => n.id === edge.target)
            return tNode ? tNode.name : null
          })
          .filter(Boolean)

      const actors = this.graphData.edges
          .filter(edge => edge.source === nodeModel.id && edge.label === '主演')
          .map(edge => {
            const tNode = this.graphData.nodes.find(n => n.id === edge.target)
            return tNode ? tNode.name : null
          })
          .filter(Boolean)

      const regionEdge = this.graphData.edges.find(edge => edge.source === nodeModel.id && edge.label === '地区')
      const genreEdge = this.graphData.edges.find(edge => edge.source === nodeModel.id && edge.label === '类型')

      let region = ''
      let genre = ''

      if (regionEdge) {
        const tNode = this.graphData.nodes.find(n => n.id === regionEdge.target)
        region = tNode ? tNode.name : ''
      }

      if (genreEdge) {
        const tNode = this.graphData.nodes.find(n => n.id === genreEdge.target)
        genre = tNode ? tNode.name : ''
      }

      this.nodeDetail = {
        ...nodeModel,
        directors: directors.join('、'),
        actors: actors.join('、'),
        region,
        genre
      }
      this.showDetailDialog = true
    },

    filterNodes() {
      this.renderGraph()
    },

    searchNodes() {
      if (this.searchKeyword && this.searchKeyword.trim()) {
        this.fetchGraphData()
      } else {
        this.searchCenterNodeId = null
        this.renderGraph()
      }
    },

    resetGraph() {
      this.selectedNodeType = 'all'
      this.searchKeyword = ''
      this.searchCenterNodeId = null
      this.localMovieCount = 10
      this.fetchGraphData()
    },

    onMovieCountChange() {
      this.fetchGraphData()
    },

    // 新增：切换布局算法
    changeLayout() {
      this.renderGraph()
    },

    // 新增：更新交互控制
    updateInteraction() {
      this.renderGraph()
    },

    // 新增：展开全部节点
    expandAllNodes() {
      const movieNodes = this.graphData.nodes.filter(node => node.type === 'movie')
      movieNodes.forEach(node => {
        if (node.id) {
          this.expandedNodes.add(node.id)
        }
      })
      this.renderGraph()
      this.$message.success('已展开全部节点')
    },

    // 新增：收起全部节点
    collapseAllNodes() {
      this.expandedNodes.clear()
      this.renderGraph()
      this.$message.success('已收起全部节点')
    },

    // 新增：中心对齐
    centerGraph() {
      this.zoomLevel = 1
      this.renderGraph()
    },

    // 新增：导出图谱
    exportGraph() {
      if (this.graphInstance) {
        const url = this.graphInstance.getDataURL({
          pixelRatio: 2,
          backgroundColor: '#FFFFFF'
        })
        const link = document.createElement('a')
        link.download = 'knowledge-graph.png'
        link.href = url
        link.click()
        this.$message.success('图谱导出成功！')
      }
    }
  }
}
</script>

<style scoped>
.knowledge-graph-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #FFFFFF;
  overflow: hidden;
  position: relative;
  margin: 0;
  padding: 0;
}

.graph-controls {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 22px;
  background: linear-gradient(180deg, #fafbfc 0%, #f2f4f6 100%);
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
  z-index: 10;
  flex-wrap: wrap;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.type-selector {
  width: 160px;
  min-width: 160px;
}

.search-input {
  flex: 1;
  min-width: 200px;
  max-width: 300px;
}

.movie-count-input {
  width: 140px;
  min-width: 140px;
}

.layout-selector {
  width: 180px;
  min-width: 180px;
}

.interaction-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  flex-wrap: wrap;
}

.interaction-controls .tool-check {
  margin-right: 4px;
}

.interaction-controls .tool-check ::v-deep .el-checkbox__label {
  font-size: 13px;
  color: #5f6368;
}

.interaction-controls .tool-check.is-checked ::v-deep .el-checkbox__label {
  color: #1a73e8;
}

.interaction-controls ::v-deep .el-checkbox__inner {
  border-radius: 4px;
}

.interaction-controls ::v-deep .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #1a73e8;
  border-color: #1a73e8;
}

.toolbar-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-graph-explain {
  margin: 10px 0;
}

.tool-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.tool-btn i {
  margin-right: 4px;
  font-size: 14px;
}

.reset-btn {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  border-color: #6d28d9;
  color: #fff;
}

.reset-btn:hover {
  background: linear-gradient(135deg, #6d28d9 0%, #5b21b6 100%);
  border-color: #5b21b6;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.35);
}

.expand-btn {
  background: #fff;
  border-color: #dadce0;
  color: #5f6368;
}

.expand-btn:hover {
  background: #e8f0fe;
  border-color: #1a73e8;
  color: #1a73e8;
}

.collapse-btn {
  background: #fff;
  border-color: #dadce0;
  color: #5f6368;
}

.collapse-btn:hover {
  background: #f1f3f4;
  border-color: #9aa0a6;
  color: #3c4043;
}

.export-btn {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  border-color: #047857;
  color: #fff;
}

.export-btn:hover {
  background: linear-gradient(135deg, #047857 0%, #065f46 100%);
  border-color: #065f46;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.35);
}

/* 关键改进：画布占满剩余空间 */
.graph-canvas {
  flex: 1;
  width: 100%;
  min-height: 0; /* 防止flex项目溢出 */
  overflow: hidden;
  background-color: #FFFFFF;
}

/* 详情弹窗样式 */
.detail-dialog {
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.detail-card {
  padding: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #F0F0F0;
}

.movie-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0;
  line-height: 1.4;
  flex: 1;
}

.movie-rating {
  font-size: 14px;
  color: #FFA500;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  white-space: nowrap;
  margin-left: 12px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
}

.detail-label {
  width: 60px;
  font-size: 14px;
  color: #666;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  word-break: break-word;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .knowledge-graph-container {
    height: 100vh;
  }

  .graph-controls {
    padding: 10px 15px;
    gap: 12px;
  }

  .type-selector,
  .search-input,
  .movie-count-input {
    width: 100%;
    min-width: unset;
    max-width: unset;
  }

  .reset-btn {
    width: 100%;
    padding: 12px;
  }

  .graph-canvas {
    height: calc(100vh - 160px);
  }
}

@media (max-width: 480px) {
  .graph-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .movie-title {
    font-size: 18px;
  }

  .detail-label,
  .detail-value {
    font-size: 13px;
  }
}
</style>