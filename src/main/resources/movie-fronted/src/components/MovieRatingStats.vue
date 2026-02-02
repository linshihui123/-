<template>
  <div class="movie-rating-stats">
    <el-card class="stats-card">
      <div slot="header" class="clearfix">
        <span class="card-title">电影评分统计</span>
        <el-button-group style="float: right;">
          <el-button
              size="small"
              :type="activeTab === 'movie' ? 'primary' : ''"
              @click="switchTab('movie')"
          >
            电影评分分布
          </el-button>
          <el-button
              size="small"
              :type="activeTab === 'type' ? 'primary' : ''"
              @click="switchTab('type')"
          >
            类型/地区统计
          </el-button>
        </el-button-group>
      </div>

      <!-- 电影评分分布 Tab -->
      <div v-if="activeTab === 'movie'" class="tab-content">
        <div class="search-section">
          <el-input
              v-model="searchMovieName"
              placeholder="请输入电影名称"
              style="width: 300px; margin-right: 10px;"
              @keyup.enter.native="fetchMovieRatings"
          />
          <el-button type="primary" @click="fetchMovieRatings">查询</el-button>
        </div>

        <div v-if="movieRatingsData && movieRatingsData.length > 0" class="chart-container">
          <h3>{{ searchMovieName }} 的评分分布</h3>
          <div id="movie-rating-chart" style="height: 400px;"></div>
        </div>

        <div v-else-if="!searchMovieName" class="placeholder-message">
          <el-empty description="请输入电影名称查询评分分布"></el-empty>
        </div>

        <div v-else-if="loadingMovieRatings" class="loading-container">
          <el-spin></el-spin>
        </div>

        <div v-else class="no-data-message">
          <el-empty description="暂无评分数据"></el-empty>
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
          <el-spin></el-spin>
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
import { getMovieRatings, getMovieRatingsByType, getMovieRatingsByRegion } from '../api/recommend';

export default {
  name: 'MovieRatingStats',
  data() {
    return {
      activeTab: 'movie',
      statsSubTab: 'type', // 在type和region之间切换
      searchMovieName: '',
      movieRatingsData: null,
      typeStatsData: null,
      regionStatsData: null,
      loadingMovieRatings: false,
      loadingTypeStats: false,
      chartMovie: null,
      chartType: null
    };
  },
  mounted() {
    // 初始化时自动获取类型和区域统计数据
    this.fetchMovieRatingsByType();
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

    switchTab(tab) {
      this.activeTab = tab;
      // 当切换到类型/地区统计标签页时，确保图表被渲染
      if (tab === 'type') {
        this.$nextTick(() => {
          this.renderTypeRatingChart();
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

    async fetchMovieRatings() {
      if (!this.searchMovieName.trim()) {
        this.$message.warning('请输入电影名称');
        return;
      }

      this.loadingMovieRatings = true;
      try {
        const response = await getMovieRatings(this.searchMovieName.trim());
        if (response && response.data) {
          this.movieRatingsData = response.data;
          this.$nextTick(() => {
            this.renderMovieRatingChart();
          });
        } else {
          this.movieRatingsData = null;
          this.$message.info('未找到该电影的评分数据');
        }
      } catch (error) {
        console.error('获取电影评分数据失败:', error);
        this.$message.error('获取电影评分数据失败');
        this.movieRatingsData = null;
      } finally {
        this.loadingMovieRatings = false;
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


    async renderMovieRatingChart() {
      if (!this.movieRatingsData || this.movieRatingsData.length === 0) {
        console.log('没有电影评分数据可供渲染');
        return;
      }

      const echarts = await this.loadECharts();
      
      // 确保DOM元素存在
      const chartDom = document.getElementById('movie-rating-chart');
      if (!chartDom) {
        console.log('电影评分图表DOM元素不存在');
        return;
      }

      // 如果已有图表实例，先销毁
      if (this.chartMovie) {
        this.chartMovie.dispose();
      }

      this.chartMovie = echarts.init(chartDom);

      // 安全地提取数据，处理可能的字段变化
      const seriesData = [];
      this.movieRatingsData.forEach(item => {
        // 尝试多种可能的字段名
        const rating = item.rating !== undefined ? item.rating :
                    item.score !== undefined ? item.score :
                    item.star !== undefined ? item.star : null;
        
        const count = item.count !== undefined ? item.count :
                     item.proportion !== undefined ? item.proportion :
                     item.num !== undefined ? item.num : null;
        
        if (rating !== null && count !== null) {
          seriesData.push({
            value: count,
            name: rating + '星',
            label: {
              formatter: '{b}: {d}%'
            }
          });
        }
      });

      // 如果没有有效的数据点，则不渲染图表
      if (seriesData.length === 0) {
        console.log('没有有效的评分数据用于渲染');
        return;
      }

      const option = {
        title: {
          text: this.searchMovieName + ' 评分分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: '评分分布',
            type: 'pie',
            radius: '50%',
            data: seriesData,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };

      this.chartMovie.setOption(option);
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
    if (this.chartMovie) {
      this.chartMovie.dispose();
    }
    if (this.chartType) {
      this.chartType.dispose();
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
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.tab-content {
  padding: 20px 0;
}

.search-section {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  gap: 10px;
}

.chart-container {
  margin-top: 20px;
}

.placeholder-message,
.no-data-message {
  text-align: center;
  padding: 40px 0;
}

.loading-container {
  text-align: center;
  padding: 40px 0;
}

::v-deep .el-card__header {
  background-color: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
}
</style>