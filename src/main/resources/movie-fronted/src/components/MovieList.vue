<template>
  <div class="movie-list-container" v-loading="loading">
<el-card shadow="hover" v-if="movies.length > 0">
  <div slot="header" class="card-header" v-if="showTitle">
    <span>推荐结果（共{{ movies.length }}部）</span>
  </div>
<el-row :gutter="12">
  <el-col :xs="24" :sm="12" :md="6" :lg="6" v-for="movie in movies" :key="movie.id" class="movie-card">
    <el-card class="movie-item shadow-lift" @click.native="handleMovieClick(movie)">
        <div class="movie-content">
          <div class="movie-name">{{ movie.movieName || movie.name || '未知电影' }}</div>

          <div class="movie-metadata">
            <div class="metadata-item">
              <i class="el-icon-collection-tag"></i>
              <span>{{ movie.type || '未知' }}</span>
            </div>
            <div class="metadata-item">
              <i class="el-icon-location"></i>
              <span>{{ movie.direction || '未知' }}</span>
            </div>
            <div class="metadata-item" v-if="movie.rating && movie.rating !== '暂无评分'">
              <i class="el-icon-star-on"></i>
              <span>{{ parseFloat(movie.rating).toFixed(1) }}</span>
            </div>
          </div>

          <div class="movie-info">
            <div class="info-item">
              <strong>导演：</strong>
              <span class="director-names">{{ getDirectorNames(movie) }}</span>
            </div>
            <div class="info-item">
              <strong>演员：</strong>
              <span class="actor-names">{{ getActorNames(movie) }}</span>
            </div>
          </div>

          <div class="movie-desc" :title="movie.instruction || '暂无介绍'">
            {{ movie.instruction || '暂无介绍' }}
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</el-card>
    <el-empty v-else description="暂无推荐结果"></el-empty>
  </div>
</template>

<script>
export default {
  name: 'MovieList',
  props: {
    movies: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    showTitle: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    // 处理电影点击事件
    handleMovieClick(movie) {
      this.$emit('movie-click', movie);
    },
    // 格式化导演名称
    getDirectorNames(movie) {
      // 优先使用关系数据，如果没有则使用字符串解析
      if (movie.directors && movie.directors.length > 0) {
        return movie.directors.map(item => item.name).join('、')
      } else if (movie.directorString) {
        return movie.directorString.split('|').join('、')
      } else if (movie.directorList && movie.directorList.length > 0) {
        // 使用解析后的导演列表
        return movie.directorList.join('、')
      } else if (movie.director) {
        // 如果是数据库中的director字段
        if (typeof movie.director === 'string') {
          return movie.director.split('|').join('、')
        }
        return movie.director
      }
      return '未知'
    },
    // 格式化演员名称
    getActorNames(movie) {
      // 优先使用关系数据，如果没有则使用字符串解析
      if (movie.actors && movie.actors.length > 0) {
        return movie.actors.map(item => item.name).join('、')
      } else if (movie.actorString) {
        return movie.actorString.split('|').join('、')
      } else if (movie.actorList && movie.actorList.length > 0) {
        // 使用解析后的演员列表
        return movie.actorList.join('、')
      } else if (movie.actor) {
        // 如果是数据库中的actor字段
        if (typeof movie.actor === 'string') {
          return movie.actor.split('|').join('、')
        }
        return movie.actor
      }
      return '未知'
    }
  }
}
</script>

<style scoped>
.movie-list-container {
  margin: 20px auto;
  max-width: 1200px;
  padding: 0 15px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding: 20px;
}

.movie-card {
  margin-bottom: 25px;
}

.movie-item {
  height: auto;
  min-height: 280px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.movie-content {
  padding: 20px;
}

.movie-name {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin-bottom: 15px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.movie-metadata {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.metadata-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #666;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 12px;
}

.movie-info {
  margin-bottom: 15px;
}

.info-item {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.4;
}

.info-item strong {
  color: #333;
  font-weight: 600;
}

.director-names,
.actor-names {
  color: #666;
}

.movie-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 67px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .movie-card {
    margin-bottom: 20px;
  }

  .movie-content {
    padding: 15px;
  }

  .movie-name {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .movie-metadata {
    flex-direction: column;
    gap: 8px;
  }
}
</style>