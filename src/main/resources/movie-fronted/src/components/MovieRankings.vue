<template>
  <div class="movie-rankings">
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <div slot="header" class="clearfix">
        <span>榜单筛选</span>
      </div>
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="年份">
          <el-input v-model="filterForm.year" placeholder="如：2024" clearable></el-input>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.type" placeholder="选择类型" clearable>
            <el-option label="剧情" value="剧情"></el-option>
            <el-option label="喜剧" value="喜剧"></el-option>
            <el-option label="动作" value="动作"></el-option>
            <el-option label="爱情" value="爱情"></el-option>
            <el-option label="科幻" value="科幻"></el-option>
            <el-option label="动画" value="动画"></el-option>
            <el-option label="悬疑" value="悬疑"></el-option>
            <el-option label="惊悚" value="惊悚"></el-option>
            <el-option label="恐怖" value="恐怖"></el-option>
            <el-option label="犯罪" value="犯罪"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="地区">
          <el-select v-model="filterForm.region" placeholder="选择地区" clearable>
            <el-option label="中国大陆" value="中国大陆"></el-option>
            <el-option label="美国" value="美国"></el-option>
            <el-option label="日本" value="日本"></el-option>
            <el-option label="韩国" value="韩国"></el-option>
            <el-option label="英国" value="英国"></el-option>
            <el-option label="法国" value="法国"></el-option>
            <el-option label="德国" value="德国"></el-option>
            <el-option label="印度" value="印度"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="显示数量">
          <el-input-number v-model="filterForm.limit" :min="5" :max="50" :step="5"></el-input-number>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRankings">查询榜单</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 榜单内容 -->
    <div v-else class="rankings-content">
      <!-- 高分榜 -->
      <el-card class="ranking-card">
        <div slot="header" class="card-header">
          <span class="ranking-title">🏆 高分榜</span>
          <span class="ranking-desc">官方评分 + 用户评分加权排序</span>
        </div>
        <div class="ranking-list">
          <div v-for="(item, index) in rankings.highScoreRanking" :key="index" class="ranking-item">
            <div class="rank-number" :class="getRankClass(index)">
              {{ index + 1 }}
            </div>
            <div class="movie-info">
              <div class="movie-name">{{ item.movie?.name }}</div>
              <div class="movie-meta">
                <el-tag size="mini" type="primary">{{ item.movie?.type }}</el-tag>
                <el-tag size="mini">{{ item.movie?.region }}</el-tag>
              </div>
              <div class="rating-info">
                <span class="official-rating">
                  官方: {{ formatRating(item.officialRating) }}
                </span>
                <span class="user-rating" v-if="item.avgUserRating">
                  用户: {{ formatRating(item.avgUserRating) }}
                </span>
                <span class="comment-count">
                  评论: {{ item.commentCount }}条
                </span>
              </div>
            </div>
            <div class="weighted-score">
              <div class="score-value">{{ formatRating(item.weightedScore) }}</div>
              <div class="score-label">综合分</div>
            </div>
          </div>
          <div v-if="!rankings.highScoreRanking || rankings.highScoreRanking.length === 0" class="empty-state">
            暂无高分电影数据
          </div>
        </div>
      </el-card>

      <!-- 热门评论榜 -->
      <el-card class="ranking-card">
        <div slot="header" class="card-header">
          <span class="ranking-title">🔥 热门评论榜</span>
          <span class="ranking-desc">评论数量 + 近期增长速度</span>
        </div>
        <div class="ranking-list">
          <div v-for="(item, index) in rankings.popularCommentRanking" :key="index" class="ranking-item">
            <div class="rank-number" :class="getRankClass(index)">
              {{ index + 1 }}
            </div>
            <div class="movie-info">
              <div class="movie-name">{{ item.movie?.name }}</div>
              <div class="movie-meta">
                <el-tag size="mini" type="success">{{ item.movie?.type }}</el-tag>
                <el-tag size="mini">{{ item.movie?.region }}</el-tag>
              </div>
              <div class="comment-stats">
                <span class="total-comments">
                  总评论: {{ item.totalComments }}条
                </span>
                <span class="recent-comments" v-if="item.recentComments">
                  近期: {{ item.recentComments }}条
                </span>
                <span class="growth-rate" v-if="item.growthRate">
                  增长率: {{ formatPercentage(item.growthRate) }}
                </span>
              </div>
            </div>
            <div class="avg-rating">
              <div class="rating-value">{{ formatRating(item.avgRating) }}</div>
              <div class="rating-label">平均分</div>
            </div>
          </div>
          <div v-if="!rankings.popularCommentRanking || rankings.popularCommentRanking.length === 0" class="empty-state">
            暂无热门评论电影数据
          </div>
        </div>
      </el-card>

      <!-- 类型榜 -->
      <el-card class="ranking-card">
        <div slot="header" class="card-header">
          <span class="ranking-title">🎬 类型榜</span>
          <span class="ranking-desc">各类型高分电影排行</span>
        </div>
        <div class="type-rankings">
          <div v-for="(typeGroup, typeIndex) in rankings.typeRanking" :key="typeIndex" class="type-group">
            <div class="type-header">
              <h3>{{ typeGroup.type }} ({{ typeGroup.count }}部)</h3>
            </div>
            <div class="type-movies">
              <div v-for="(movie, movieIndex) in typeGroup.movies" :key="movieIndex" class="type-movie-item">
                <span class="type-rank">{{ movieIndex + 1 }}</span>
                <span class="type-movie-name">{{ movie.name }}</span>
                <span class="type-movie-rating">{{ formatRating(movie.rating) }}</span>
                <span class="type-movie-region">{{ movie.region }}</span>
              </div>
            </div>
          </div>
          <div v-if="!rankings.typeRanking || rankings.typeRanking.length === 0" class="empty-state">
            暂无类型榜单数据
          </div>
        </div>
      </el-card>
    </div>

    <!-- 统计信息 -->
    <el-card class="stats-card" v-if="!loading">
      <div slot="header">
        <span>📊 榜单统计</span>
      </div>
      <div class="stats-content">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ rankings.totalHighScoreMovies || 0 }}</div>
              <div class="stat-label">高分电影</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ rankings.totalPopularCommentMovies || 0 }}</div>
              <div class="stat-label">热门评论电影</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ rankings.totalTypeCategories || 0 }}</div>
              <div class="stat-label">电影类型</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getMultiDimensionalRanking } from '@/api/recommend'

export default {
  name: 'MovieRankings',
  data() {
    return {
      loading: false,
      filterForm: {
        year: '',
        type: '',
        region: '',
        limit: 20
      },
      rankings: {
        highScoreRanking: [],
        popularCommentRanking: [],
        typeRanking: [],
        totalHighScoreMovies: 0,
        totalPopularCommentMovies: 0,
        totalTypeCategories: 0
      }
    }
  },
  mounted() {
    this.loadRankings()
  },
  methods: {
    async loadRankings() {
      this.loading = true
      try {
        const params = {}
        if (this.filterForm.year) params.year = this.filterForm.year
        if (this.filterForm.type) params.type = this.filterForm.type
        if (this.filterForm.region) params.region = this.filterForm.region
        params.limit = this.filterForm.limit

        const response = await getMultiDimensionalRanking(params)
        if (response.code === 200) {
          this.rankings = response.data
        } else {
          this.$message.error(response.msg || '获取榜单数据失败')
        }
      } catch (error) {
        console.error('获取榜单数据失败:', error)
        this.$message.error('获取榜单数据失败')
      } finally {
        this.loading = false
      }
    },
    resetFilters() {
      this.filterForm = {
        year: '',
        type: '',
        region: '',
        limit: 20
      }
      this.loadRankings()
    },
    getRankClass(index) {
      if (index === 0) return 'first-place'
      if (index === 1) return 'second-place'
      if (index === 2) return 'third-place'
      return 'normal-place'
    },
    formatRating(rating) {
      if (rating === null || rating === undefined) return '暂无'
      return parseFloat(rating).toFixed(1)
    },
    formatPercentage(value) {
      if (value === null || value === undefined) return '0%'
      return (parseFloat(value) * 100).toFixed(1) + '%'
    }
  }
}
</script>

<style scoped>
.movie-rankings {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ranking-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.ranking-desc {
  font-size: 14px;
  color: #909399;
}

.ranking-list {
  min-height: 300px;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s;
}

.ranking-item:hover {
  background-color: #f5f7fa;
}

.rank-number {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 15px;
  flex-shrink: 0;
}

.first-place {
  background-color: #ffd700;
  color: #fff;
}

.second-place {
  background-color: #c0c0c0;
  color: #fff;
}

.third-place {
  background-color: #cd7f32;
  color: #fff;
}

.normal-place {
  background-color: #e9ecef;
  color: #495057;
}

.movie-info {
  flex: 1;
  min-width: 0;
}

.movie-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.movie-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.rating-info, .comment-stats {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #606266;
}

.official-rating, .user-rating, .comment-count,
.total-comments, .recent-comments, .growth-rate {
  display: flex;
  align-items: center;
}

.weighted-score, .avg-rating {
  text-align: center;
  padding: 0 20px;
  flex-shrink: 0;
}

.score-value, .rating-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.score-label, .rating-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.type-rankings {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.type-group {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.type-header {
  background-color: #f5f7fa;
  padding: 12px 15px;
  border-bottom: 1px solid #eee;
}

.type-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.type-movies {
  max-height: 300px;
  overflow-y: auto;
}

.type-movie-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #f5f5f5;
}

.type-movie-item:last-child {
  border-bottom: none;
}

.type-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #e9ecef;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 12px;
  flex-shrink: 0;
}

.type-movie-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-movie-rating {
  font-weight: bold;
  color: #409eff;
  margin: 0 10px;
  flex-shrink: 0;
}

.type-movie-region {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

.stats-card {
  margin-top: 20px;
}

.stats-content {
  text-align: center;
}

.stat-item {
  padding: 20px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.loading-container {
  padding: 20px;
}

@media (max-width: 768px) {
  .filter-form {
    flex-direction: column;
  }
  
  .ranking-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .weighted-score, .avg-rating {
    align-self: flex-end;
    padding: 0;
  }
  
  .type-rankings {
    grid-template-columns: 1fr;
  }
  
  .rating-info, .comment-stats {
    flex-direction: column;
    gap: 5px;
  }
}
</style>