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
        <el-checkbox v-model="enableZoom" @change="updateInteraction">启用缩放</el-checkbox>
        <el-checkbox v-model="enableDrag" @change="updateInteraction">启用拖拽</el-checkbox>
        <el-checkbox v-model="enableNodeFollow" @change="updateInteraction">节点跟随</el-checkbox>
      </div>

      <el-button type="primary" @click="resetGraph" class="reset-btn">重置图谱</el-button>
      <el-button type="info" @click="expandAllNodes" class="expand-btn">展开全部</el-button>
      <el-button type="info" @click="collapseAllNodes" class="collapse-btn">收起全部</el-button>
      <el-button type="success" @click="exportGraph" class="export-btn">导出图谱</el-button>
    </div>

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
import axios from 'axios'
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
      zoomLevel: 1
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
      const apiUrl = `http://localhost:8081/api/kg/graph-data?movieCount=${this.localMovieCount}`
      axios.get(apiUrl)
          .then(res => {
            if (res.status === 200) {
              this.graphData = res.data.data || res.data
              this.renderGraph()
            }
          })
          .catch(err => {
            this.$message.error('数据请求失败：' + err.message)
            console.error('图谱数据请求失败：', err)
          })
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

      // 关键词搜索
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.trim().toLowerCase()
        filteredNodes = filteredNodes.filter(node => node.name && node.name.toLowerCase().includes(keyword))
        const filteredNodeIds = filteredNodes.map(node => node.id)
        filteredEdges = filteredEdges.filter(edge =>
            filteredNodeIds.includes(edge.source) && filteredNodeIds.includes(edge.target)
        )
      }

      const nodeCategories = this.getNodeCategories()

      // 收集所有电影节点
      const movieNodes = filteredNodes.filter(node => node.type === 'movie')

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
          // 排除电影节点自身
          const childNode = filteredNodes.find(n => n.id === childId && n.type !== 'movie')
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
      this.renderGraph()
    },

    resetGraph() {
      this.selectedNodeType = 'all'
      this.searchKeyword = ''
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
  gap: 16px;
  padding: 12px 20px;
  background-color: #F8F9FA;
  border-bottom: 1px solid #E5E5E5;
  flex-shrink: 0;
  z-index: 10;
  flex-wrap: wrap;
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

.reset-btn {
  background-color: #8A2BE2;
  border-color: #8A2BE2;
  color: white;
  padding: 0 20px;
}

.reset-btn:hover {
  background-color: #7B2BBE;
  border-color: #7B2BBE;
}

.layout-selector {
  width: 180px;
  min-width: 180px;
}

.interaction-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.interaction-controls .el-checkbox {
  margin-right: 8px;
}

.expand-btn,
.collapse-btn {
  padding: 0 16px;
}

.expand-btn:hover,
.collapse-btn:hover {
  background-color: #E6F7FF;
  border-color: #91D5FF;
  color: #1890FF;
}

.export-btn {
  background-color: #52C41A;
  border-color: #52C41A;
  color: white;
  padding: 0 16px;
}

.export-btn:hover {
  background-color: #389E0D;
  border-color: #389E0D;
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