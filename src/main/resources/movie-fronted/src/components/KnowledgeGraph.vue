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

      <el-button type="primary" @click="resetGraph" class="reset-btn">重置图谱</el-button>
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
      dragStartPos: { x: 0, y: 0 }
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
      const fontSize = isCore ? 14 : 12
      const baseSize = isCore ? 60 : 40
      const length = name ? name.length : 0

      // 根据文本长度调整大小
      if (length <= 4) return baseSize
      if (length <= 8) return baseSize * 1.2
      return baseSize * 1.5
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
              opacity: 0.8
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
        { name: 'movie', itemStyle: { color: '#8A2BE2', borderWidth: 2, borderColor: '#4B0082' } },
        { name: 'director', itemStyle: { color: '#FF4500', borderWidth: 1, borderColor: '#FF6347' } },
        { name: 'actor', itemStyle: { color: '#FF6347', borderWidth: 1, borderColor: '#FF4500' } },
        { name: 'region', itemStyle: { color: '#FFD700', borderWidth: 1, borderColor: '#FFA500' } },
        { name: 'genre', itemStyle: { color: '#32CD32', borderWidth: 1, borderColor: '#228B22' } }
      ]
    },

    renderGraph() {
      if (!this.graphInstance) return

      const graphData = this.generateGraphData()
      const _this = this

      // 计算画布大小
      const containerWidth = this.$refs.graphCanvas.offsetWidth
      const containerHeight = this.$refs.graphCanvas.offsetHeight

      // ECharts配置
      const option = {
        backgroundColor: '#FFFFFF',
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#E5E5E5',
          borderWidth: 1,
          borderRadius: 6,
          padding: [8, 12],
          textStyle: { color: '#333', fontSize: 12 },
          formatter: params => {
            if (params.dataType === 'node') {
              const types = ['电影','导演','演员','地区','类型']
              return `${params.name}<br/>类型：${types[params.data.category] || '未知'}`
            }
            return params.dataType === 'edge' ? `关系：${params.data.label.formatter}` : params.name
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
          textStyle: { fontSize: 12, color: '#000' },
          icon: 'circle'
        },
        series: [
          {
            type: 'graph',
            layout: 'none',
            roam: true,
            zoom: 1,
            center: ['50%', '50%'],
            draggable: true,
            symbol: 'circle',
            symbolSize: 40,
            edgeSymbol: ['circle', 'circle'],
            edgeSymbolSize: [4, 4],
            cursor: 'move',
            label: {
              show: true,
              position: 'inside',
              color: '#000',
              fontSize: 10,
              fontWeight: 'normal'
            },
            edgeLabel: {
              show: true,
              position: 'middle',
              fontSize: 10
            },
            data: graphData.nodes,
            links: graphData.edges,
            categories: graphData.categories,
            lineStyle: {
              color: 'source',
              width: 1.5,
              curveness: 0.1,
              opacity: 0.6
            },
            emphasis: {
              focus: 'adjacency',
              lineStyle: {
                width: 2,
                opacity: 1
              }
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

        const currentNode = params.data
        const dx = currentNode.x - _this.dragStartPos.x
        const dy = currentNode.y - _this.dragStartPos.y

        // 如果是电影节点被拖动，移动所有子节点
        if (currentNode.type === 'movie' && _this.coreMovieMap.has(currentNode.id)) {
          const childNodeIds = _this.coreMovieMap.get(currentNode.id)
          const graphOption = _this.graphInstance.getOption()
          const allNodes = graphOption.series[0].data

          // 更新所有子节点的位置
          childNodeIds.forEach(childId => {
            const childNode = allNodes.find(n => n.id === childId)
            if (childNode) {
              childNode.x += dx
              childNode.y += dy
            }
          })

          // 更新图表
          _this.graphInstance.setOption({
            series: [{
              data: allNodes
            }]
          }, true) // 使用true参数强制更新
        }

        // 如果是子节点被拖动，检查它是否属于多个父节点
        else if (currentNode.type !== 'movie' && _this.nodePositions.has(currentNode.id)) {
          const parentMovieIds = Array.from(_this.nodePositions.get(currentNode.id))
          const graphOption = _this.graphInstance.getOption()
          const allNodes = graphOption.series[0].data

          // 找到对应的父电影节点
          parentMovieIds.forEach(parentId => {
            const parentNode = allNodes.find(n => n.id === parentId && n.type === 'movie')
            if (parentNode) {
              // 计算父节点的新位置（保持相对位置不变）
              const relativeX = currentNode.x - _this.dragStartPos.x
              const relativeY = currentNode.y - _this.dragStartPos.y
              parentNode.x += relativeX
              parentNode.y += relativeY

              // 移动该父节点的其他子节点
              if (_this.coreMovieMap.has(parentId)) {
                const siblingIds = _this.coreMovieMap.get(parentId)
                siblingIds.forEach(siblingId => {
                  if (siblingId !== currentNode.id) {
                    const siblingNode = allNodes.find(n => n.id === siblingId)
                    if (siblingNode) {
                      siblingNode.x += relativeX
                      siblingNode.y += relativeY
                    }
                  }
                })
              }
            }
          })

          // 更新图表
          _this.graphInstance.setOption({
            series: [{
              data: allNodes
            }]
          }, true) // 使用true参数强制更新
        }

        // 更新起始位置
        _this.dragStartPos = {
          x: currentNode.x,
          y: currentNode.y
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
        this.$message.info(`${nodeModel.name} | ${['电影','导演','演员','地区','类型'][nodeModel.category] || '未知'}`)
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