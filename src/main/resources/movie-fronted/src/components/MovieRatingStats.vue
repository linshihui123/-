<template>
  <div class="movie-rating-stats">
    <el-card class="stats-card">
      <div slot="header" class="card-title">
        <el-button-group>
          <el-button 
            :type="activeTab === 'movie' ? 'primary' : ''" 
            @click="switchTab('movie')"
          >
            评论查询
          </el-button>
          <el-button 
            :type="activeTab === 'type' ? 'primary' : ''" 
            @click="switchTab('type')"
          >
            统计分析
          </el-button>
        </el-button-group>
      </div>

      <!-- 基于评论文本的兴趣推荐 Tab -->
      <div v-if="activeTab === 'movie'" class="tab-content">
        <div class="search-section">
          <!-- 改为下拉选择框 -->
          <el-select
              v-model="searchMovieName"
              placeholder="请选择电影"
              style="width: 300px; margin-right: 10px;"
              filterable
              clearable
              :loading="loadingMovieList"
              @change="handleMovieSelect"
          >
            <el-option
                v-for="movie in movieList"
                :key="movie.id || movie.movieId"
                :label="movie.movieName || movie.name || '未知电影'"
                :value="movie.movieName || movie.name"
            >
            </el-option>
          </el-select>
          <el-button type="primary" @click="fetchMovieComments" :disabled="!searchMovieName">查询评论</el-button>
        </div>

        <!-- 有评论或有 AI 总结时展示（无评论时也会调用大模型根据电影名生成总结并显示） -->
        <div v-if="searchMovieName && !loadingMovieComments && ((movieCommentsData && movieCommentsData.length > 0) || movieCommentsAiSummary)" class="comments-container">
          <h3>{{ searchMovieName }}{{ (movieCommentsData && movieCommentsData.length > 0) ? ' 的用户评论' : ' 的电影总结' }}</h3>
          <el-alert v-if="movieCommentsAiSummary" :title="movieCommentsAiSummary" type="success" :closable="false" show-icon class="ai-summary-alert"></el-alert>
          <div v-if="movieCommentsData && movieCommentsData.length > 0" class="comments-list">
            <el-card v-for="(comment, index) in movieCommentsData" :key="index" class="comment-card">
              <div class="comment-header">
                <span class="comment-user"><i class="el-icon-user"></i> {{ comment.creator || '匿名用户' }}</span>
                <span class="comment-rating" v-if="comment.comment_rating">
                  <i class="el-icon-star-on" style="color: #F7BA2A;"></i>
                  {{ comment.comment_rating }}分
                </span>
              </div>
              <div class="comment-content">
                {{ comment.content || '暂无评论内容' }}
              </div>
              <div class="comment-footer">
                <span class="comment-time">
                  <i class="el-icon-time"></i>
                  {{ comment.comment_time || comment.comment_add_time || '未知时间' }}
                </span>
              </div>
            </el-card>
          </div>
          <div v-else class="no-comments-tip">
            <p>该电影暂无用户评论，以上为根据电影信息生成的 AI 总结。</p>
          </div>
        </div>

        <div v-else-if="!searchMovieName" class="placeholder-message">
          <el-empty description="请选择电影查询用户评论"></el-empty>
        </div>

        <div v-else-if="loadingMovieComments" class="loading-container">
          <i class="el-icon-loading" style="font-size: 32px;"></i>
          <span style="margin-left: 8px;">正在加载评论与AI总结，请稍候...</span>
        </div>

        <div v-else class="no-data-message">
          <el-empty description="暂无用户评论与总结"></el-empty>
        </div>
      </div>

      <!-- 类型/地区统计 Tab -->
      <div v-if="activeTab === 'type'" class="tab-content">
        <!-- 自动加载数据，无需手动点击 -->
        
        <el-button-group style="margin-bottom: 20px;">
          <el-button
              size="small"
              :type="statsSubTab === 'type' ? 'primary' : ''"
              @click="switchStatsSubTab('type')"
          >
            类型统计
          </el-button>
          <el-button
              size="small"
              :type="statsSubTab === 'region' ? 'primary' : ''"
              @click="switchStatsSubTab('region')"
          >
            地区统计
          </el-button>
        </el-button-group>

        <div v-if="(statsSubTab === 'type' && typeStatsData && typeStatsData.length > 0) || (statsSubTab === 'region' && regionStatsData && regionStatsData.length > 0)" class="chart-container">
          <h3 v-if="statsSubTab === 'type'">电影类型统计</h3>
          <h3 v-else-if="statsSubTab === 'region'">电影地区统计</h3>
          <div id="type-rating-chart" style="height: 500px;"></div>
        </div>

        <div v-else-if="loadingTypeStats" class="loading-container">
          <i class="el-icon-loading" style="font-size: 32px;"></i>
          <span style="margin-left: 8px;">加载中...</span>
        </div>

        <div v-else class="no-data-message">
          <el-empty description="暂无统计数据"></el-empty>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
// 不直接导入echarts，而是动态加载
import axios from 'axios';
import { getMovieCommentsByMovieName, getMovieRatingsByType, getMovieRatingsByRegion } from '../api/recommend';
import request from '@/utils/request';

export default {
  name: 'MovieRatingStats',
  data() {
    return {
      activeTab: 'movie',
      statsSubTab: 'type', // 在type和region之间切换
      searchMovieName: '',
      movieCommentsData: null,
      movieCommentsAiSummary: '',
      typeStatsData: null,
      regionStatsData: null,
      movieList: [], // 存储电影列表
      loadingMovieList: false,
      loadingMovieComments: false,
      commentRequestCancel: null,
      commentRequestId: 0, // 用于判断是否仍是当前请求，避免取消后误关 loading
      loadingTypeStats: false,
      chartMovie: null,
      chartType: null
    };
  },
  mounted() {
    // 初始化时自动获取类型和区域统计数据
    this.fetchMovieRatingsByType();
    // 获取有评论的电影列表
    this.fetchMoviesWithComments();
    // 动态加载ECharts
    this.loadECharts();
  },
  methods: {
    // 动态加载ECharts
    loadECharts() {
      if (window.echarts) {
        return Promise.resolve(window.echarts);
      }

      return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js';
        script.onload = () => {
          window.addEventListener('load', () => {
            resolve(window.echarts);
          });
          resolve(window.echarts);
        };
        script.onerror = reject;
        document.head.appendChild(script);
      });
    },

    cleanupCharts() {
      if (this.chartType) {
        try { this.chartType.dispose(); } catch (e) {}
        this.chartType = null;
      }
      if (this.chartMovie) {
        try { this.chartMovie.dispose(); } catch (e) {}
        this.chartMovie = null;
      }
    },

    switchTab(tab) {
      this.activeTab = tab;
      // 切换标签页时清理图表实例
      this.cleanupCharts();

      // 当切换到类型/地区统计标签页时，确保图表被渲染
      if (tab === 'type') {
        this.$nextTick(() => {
          this.renderTypeRatingChart();
        });
      } else if (tab === 'movie') {
        // 切换回评论查询时，确保下拉框数据已加载
        this.$nextTick(() => {
          if (this.movieList.length === 0) {
            this.fetchMoviesWithComments();
          }
        });
      }
    },
    
    switchStatsSubTab(subTab) {
      this.statsSubTab = subTab;
      // 切换子标签页后渲染对应的图表
      this.$nextTick(() => {
        this.renderTypeRatingChart();
      });
    },

    // 获取全部电影列表（供评论查询下拉框使用）
    async fetchMoviesWithComments() {
      this.loadingMovieList = true;
      this.movieList = [];
      try {
        const response = await request({
          url: '/movie/list',
          method: 'get',
          params: { page: 0, size: 5000 }
        });
        const list = (response && response.data != null && Array.isArray(response.data)) ? response.data : [];
        this.movieList = list;
        if (list.length > 0) {
          console.log('获取到全部电影列表:', list.length, '部');
        } else {
          console.warn('电影列表为空');
        }
      } catch (error) {
        console.error('获取电影列表失败:', error);
        this.$message.error(error && error.message ? error.message : '获取电影列表失败');
        this.movieList = [];
      } finally {
        this.loadingMovieList = false;
      }
    },

    // 处理电影选择变化
    handleMovieSelect(value) {
      if (value) {
        this.fetchMovieComments();
      }
    },

    async fetchMovieComments() {
      if (!this.searchMovieName.trim()) {
        this.$message.warning('请选择电影');
        return;
      }

      // 取消上一次未完成的请求，避免切换电影时显示旧数据或一直 loading
      if (this.commentRequestCancel) {
        this.commentRequestCancel();
        this.commentRequestCancel = null;
      }

      const currentMovie = this.searchMovieName.trim();
      this.commentRequestId += 1;
      const requestId = this.commentRequestId;
      const cancelToken = new axios.CancelToken(c => { this.commentRequestCancel = c; });

      this.loadingMovieComments = true;
      this.movieCommentsAiSummary = '';
      this.movieCommentsData = null;
      try {
        const response = await getMovieCommentsByMovieName(currentMovie, { cancelToken });
        if (requestId !== this.commentRequestId) return;
        const data = (response && response.data) ? response.data : null;
        if (data) {
          const list = Array.isArray(data) ? data : (data.comments || []);
          this.movieCommentsData = list;
          this.movieCommentsAiSummary = (data.aiSummary != null && data.aiSummary !== '') ? data.aiSummary : '';
          this.$message.success(`找到 ${this.movieCommentsData.length} 条用户评论` + (this.movieCommentsAiSummary ? '，已生成AI总结' : ''));
        } else {
          this.movieCommentsData = [];
          this.$message.info('未找到该电影的用户评论');
        }
      } catch (error) {
        if (axios.isCancel(error)) return;
        if (requestId !== this.commentRequestId) return;
        console.error('获取电影评论数据失败:', error);
        this.$message.error('获取电影评论数据失败');
        this.movieCommentsData = [];
      } finally {
        if (requestId === this.commentRequestId) {
          this.loadingMovieComments = false;
        }
        this.commentRequestCancel = null;
      }
    },

    async fetchMovieRatingsByType() {
      this.loadingTypeStats = true;
      try {
        // 分别获取类型和区域数据，避免一个失败影响另一个
        let typeResponse, regionResponse;
        
        try {
          typeResponse = await getMovieRatingsByType();
        } catch (typeError) {
          console.error('获取类型统计数据失败:', typeError);
          typeResponse = null;
        }
        
        try {
          regionResponse = await getMovieRatingsByRegion();
        } catch (regionError) {
          console.error('获取区域统计数据失败:', regionError);
          regionResponse = null;
        }

        if (typeResponse && typeResponse.data) {
          this.typeStatsData = typeResponse.data || [];
        } else {
          this.typeStatsData = [];
        }
        
        if (regionResponse && regionResponse.data) {
          this.regionStatsData = regionResponse.data || [];
        } else {
          this.regionStatsData = [];
        }
        
        this.$nextTick(() => {
          this.renderTypeRatingChart();
        });
      } catch (error) {
        console.error('获取类型和区域统计数据失败:', error);
        this.$message.error('获取统计数据失败');
        this.typeStatsData = [];
        this.regionStatsData = [];
      } finally {
        this.loadingTypeStats = false;
      }
    },

    async renderTypeRatingChart() {
      let currentData = this.statsSubTab === 'type' ? this.typeStatsData : this.regionStatsData;
      
      if (!currentData || currentData.length === 0) {
        console.log('没有' + (this.statsSubTab === 'type' ? '类型' : '地区') + '统计数据可供渲染');
        return;
      }

      const echarts = await this.loadECharts();
      
      // 确保DOM元素存在
      const chartDom = document.getElementById('type-rating-chart');
      if (!chartDom) {
        console.log('图表DOM元素不存在，稍后重试');
        // 延迟一点再尝试
        setTimeout(() => {
          if (document.getElementById('type-rating-chart')) {
            this.$nextTick(() => {
              this.renderTypeRatingChart();
            });
          }
        }, 100);
        return;
      }

      // 如果已有图表实例，先销毁
      if (this.chartType) {
        this.chartType.dispose();
      }

      this.chartType = echarts.init(chartDom);

      // 安全地提取数据，处理可能的字段变化
      const labels = [];
      const avgRatings = [];
      const movieCounts = [];

      currentData.forEach(item => {
        // 根据当前子标签页选择适当的字段
        let label;
        if (this.statsSubTab === 'type') {
          label = item.movieType !== undefined ? item.movieType :
                 item.type !== undefined ? item.type :
                 item.name !== undefined ? item.name : '未知类型';
        } else { // region
          label = item.movieRegion !== undefined ? item.movieRegion :
                 item.region !== undefined ? item.region :
                 item.name !== undefined ? item.name : '未知地区';
        }

        // 平均评分字段
        const avgRating = item.avgMovieRating !== undefined ? item.avgMovieRating :
                         item.avgRating !== undefined ? item.avgRating :
                         item.averageRating !== undefined ? item.averageRating :
                         item.avg_score !== undefined ? item.avg_score : 0;

        // 电影总数字段
        const movieCount = item.movieTotalCount !== undefined ? item.movieTotalCount :
                          item.totalCount !== undefined ? item.totalCount :
                          item.count !== undefined ? item.count : 0;

        labels.push(label);
        avgRatings.push(avgRating);
        movieCounts.push(movieCount);
      });

      const option = {
        title: {
          text: (this.statsSubTab === 'type' ? '电影类型' : '电影地区') + '统计',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross',
            crossStyle: {
              color: '#999'
            }
          }
        },
        legend: {
          data: ['平均评分', '电影数量'],
          top: '10%'
        },
        xAxis: [
          {
            type: 'category',
            data: labels,
            axisPointer: {
              type: 'shadow'
            },
            axisLabel: {
              interval: 0,
              rotate: 45
            }
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '平均评分',
            min: 0,
            max: 10,
            interval: 1,
            axisLabel: {
              formatter: '{value}'
            }
          },
          {
            type: 'value',
            name: '电影数量',
            axisLabel: {
              formatter: '{value}'
            }
          }
        ],
        series: [
          {
            name: '平均评分',
            type: 'bar',
            data: avgRatings,
            itemStyle: {
              color: '#5B8FF9'
            }
          },
          {
            name: '电影数量',
            type: 'line',
            yAxisIndex: 1,
            data: movieCounts,
            itemStyle: {
              color: '#F6BD16'
            }
          }
        ]
      };

      this.chartType.setOption(option);
    }
  },

  beforeDestroy() {
    // 组件销毁前清理图表实例
    if (this.chartType) {
      try {
        this.chartType.dispose();
      } catch (e) {}
      this.chartType = null;
    }
    if (this.chartMovie) {
      try {
        this.chartMovie.dispose();
      } catch (e) {}
      this.chartMovie = null;
    }
  }
};
</script>

<style scoped>
.movie-rating-stats {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.stats-card {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.tab-content {
  padding: 20px 0;
  position: relative;
  z-index: 1;
  /* 修复切换标签页时的重叠问题 */
  overflow: hidden;
}

.search-section {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  gap: 10px;
  position: relative;
  z-index: 2;
}

.chart-container {
  margin-top: 20px;
  position: relative;
  z-index: 1;
}

.placeholder-message,
.no-data-message {
  text-align: center;
  padding: 40px 0;
}

.no-comments-tip {
  margin-top: 16px;
  padding: 12px 16px;
  color: #909399;
  font-size: 14px;
}

.ai-summary-alert {
  margin-bottom: 16px;
}

.loading-container {
  text-align: center;
  padding: 40px 0;
}

/* 修复穿模问题的关键样式 */
.comment-title {
  margin-top: 20px;
  margin-bottom: 20px;
  padding: 10px 0;
  background-color: white;
  border-radius: 4px;
  position: relative;
  z-index: 3;
}

#type-rating-chart {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 500px;
  margin-top: 10px;
}

::v-deep .el-card__header {
  background-color: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
}
</style>