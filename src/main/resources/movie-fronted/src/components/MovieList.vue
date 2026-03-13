<template>
  <div class="movie-list-container" v-loading="loading">

    <el-card shadow="hover" v-if="movies.length > 0">
      <div slot="header" class="card-header" v-if="showTitle">
        <span>推荐结果（共{{ movies.length }}部）</span>
      </div>

      <!-- 纵向列表：左列电影名与点赞按钮，上下排列；右列对应推荐理由 -->
      <div class="movie-rows-wrapper">
        <div
          class="movie-row"
          v-for="item in movies"
          :key="getInnerMovie(item).id"
          @click="handleMovieClick(getInnerMovie(item))"
        >
          <div class="movie-name-col">
            <div class="movie-name">
              {{ getInnerMovie(item).movieName || getInnerMovie(item).name || getInnerMovie(item).movie_name || '未知电影' }}
            </div>
            <!-- 当前登录用户的点赞/取消点赞按钮，点击不触发整行点击事件 -->
            <el-button
              v-if="currentUsername && getInnerMovie(item).id"
              type="text"
              size="mini"
              class="like-btn"
              @click.stop="toggleLike(getInnerMovie(item))"
            >
              {{ isMovieLiked(getInnerMovie(item)) ? '取消点赞' : '点赞' }}
            </el-button>
          </div>
          <div class="movie-reason-col" v-if="item.reason">
            <div class="recommend-reason">
              推荐理由：{{ item.reason }}
            </div>
          </div>
        </div>
      </div>
    </el-card>
    <el-empty v-else description="暂无推荐结果"></el-empty>
  </div>
</template>

<script>
import { addLike, removeLike, isLiked } from '@/api/recommend'

export default {
  name: 'MovieList',
  
  // 调试：当movies数据变化时输出到控制台
  watch: {
    movies: {
      handler(newVal) {
        console.log('🎬 MovieList 组件接收到数据:', newVal);
        if (newVal && newVal.length > 0) {
          const firstMovie = this.getInnerMovie(newVal[0]);
          console.log('📌 第一部电影详情:', firstMovie);
          console.log('📌 电影名称:', firstMovie.movieName || firstMovie.name || '未知');
          console.log('📌 电影ID:', firstMovie.id);
          console.log('📌 电影类型:', firstMovie.type);
          console.log('📌 评分:', firstMovie.movieRating);
        }
        // 推荐结果变化时刷新点赞状态
        this.refreshLikeStates()
      },
      immediate: true
    }
  },
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
  data() {
    return {
      currentUsername: '',
      likeStates: {}
    }
  },
  created() {
    // 从本地存储中读取当前登录用户
    try {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        this.currentUsername = user && user.username ? user.username : ''
      }
    } catch (e) {
      this.currentUsername = ''
    }
  },
  methods: {
    // 处理电影点击事件
    handleMovieClick(movie) {
      this.$emit('movie-click', movie);
    },
    // 兼容旧数据结构（直接是 MovieNode）和新结构（{ movie, reason, kgRelations }）
    getInnerMovie(item) {
      if (!item) {
        return {};
      }
      return item.movie || item;
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
    },
    // 判断某电影当前是否已被点赞
    isMovieLiked(movie) {
      if (!movie || !movie.id) {
        return false
      }
      return !!this.likeStates[movie.id]
    },
    // 刷新当前列表中所有电影的点赞状态
    async refreshLikeStates() {
      if (!this.currentUsername || !this.movies || this.movies.length === 0) {
        this.likeStates = {}
        return
      }
      const username = this.currentUsername
      const tasks = this.movies
        .map(item => this.getInnerMovie(item))
        .filter(m => m && m.id)
        .map(async m => {
          try {
            const res = await isLiked(username, m.id)
            // 兼容 Result<T> 或直接 boolean 的两种响应结构
            const liked = res && res.data && typeof res.data === 'object' && 'code' in res.data
              ? (res.data.code === 200 && res.data.data === true)
              : (res.data === true || res === true)
            this.$set(this.likeStates, m.id, liked)
          } catch (e) {
            console.error('查询点赞状态失败', e)
          }
        })
      await Promise.all(tasks)
    },
    // 点赞/取消点赞
    async toggleLike(movie) {
      if (!this.currentUsername) {
        this.$message.warning('请先登录再进行点赞操作')
        return
      }
      if (!movie || !movie.id) {
        return
      }
      const username = this.currentUsername
      const movieId = movie.id
      const liked = this.isMovieLiked(movie)
      try {
        if (liked) {
          const res = await removeLike(username, movieId)
          const ok = res && res.data && typeof res.data === 'object' && 'code' in res.data
            ? res.data.code === 200
            : true
          if (ok) {
            this.$set(this.likeStates, movieId, false)
            this.$message.success('已取消点赞')
            this.$emit('like-changed')
          } else {
            this.$message.error('取消点赞失败')
          }
        } else {
          const res = await addLike(username, movieId)
          const ok = res && res.data && typeof res.data === 'object' && 'code' in res.data
            ? res.data.code === 200
            : true
          if (ok) {
            this.$set(this.likeStates, movieId, true)
            this.$message.success('点赞成功')
            this.$emit('like-changed')
          } else {
            this.$message.error('点赞失败')
          }
        }
      } catch (e) {
        console.error('点赞操作失败', e)
        this.$message.error('操作失败，请稍后重试')
      }
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

.debug-info {
  background: #e7f3ff;
  border-left: 4px solid #2196f3;
  padding: 15px;
  margin-bottom: 20px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #333;
  overflow-x: auto;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding: 20px;
}

.movie-rows-wrapper {
  display: flex;
  flex-direction: column;
}

.movie-row {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 12px 18px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.movie-row + .movie-row {
  border-top: 1px solid #f0f0f0;
}

.movie-row:hover {
  background-color: #f9fafc;
}

.movie-name-col {
  flex: 0 0 110px;
  padding-right: 12px;
  border-right: 1px solid #f0f0f0;
}

.like-btn {
  margin-top: 6px;
  padding: 0;
  font-size: 12px;
  color: #409EFF;
}

.movie-reason-col {
  flex: 1;
  padding-left: 12px;
}

.recommend-reason {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

.movie-name {
  font-size: 16px;
  font-weight: 700;
  color: #333;
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
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 60px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .movie-row {
    flex-direction: column;
  }

  .movie-name-col {
    flex: 1;
    padding-right: 0;
    border-right: none;
    margin-bottom: 6px;
  }

  .movie-reason-col {
    padding-left: 0;
  }
}

@media (max-width: 480px) {
  .movie-metadata {
    flex-direction: column;
    gap: 8px;
  }
}
</style>