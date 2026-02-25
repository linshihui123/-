<template>
  <div class="collaborative-filtering-recommend">
    <h2 class="section-title">推荐</h2>
    <p class="section-description">
      基于用户评分的协同过滤推荐 - 系统根据您与其他用户的评分相似性，为您推荐相似用户喜欢的电影
    </p>

    <!-- 推荐控制+红框展示区（左右布局，匹配截图红框位置） -->
    <div class="recommend-controls">
      <!-- 左侧：选择用户+按钮+统计提示 -->
      <div class="control-left">
        <el-form :inline="true" class="control-form">
          <el-form-item label="选择用户">
            <el-select v-model="selectedUserId" placeholder="请选择用户" @change="loadRecommendations">
              <el-option v-for="item in userList" :key="item.value" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="推荐类型">
            <el-select v-model="recommendationType" placeholder="选择推荐类型" @change="onRecommendationTypeChange">
              <el-option value="collaborative" label="协同过滤推荐"></el-option>
              <el-option value="director" label="基于导演推荐"></el-option>
              <el-option value="type-region" label="基于类型和地区推荐"></el-option>
              <el-option value="actor-director" label="基于演员和导演推荐"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadRecommendations" :loading="loading">
              获取推荐
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 推荐结果统计 -->
        <div class="recommend-stats" v-if="movies.length > 0">
          <el-alert
              :title="getRecommendStatsTitle()"
              type="success"
              :closable="false"
              show-icon>
          </el-alert>
        </div>
      </div>

      <!-- 右侧：红框核心展示区（我的评分+相似用户，选项卡切换） -->
      <div class="control-right">
        <el-card shadow="hover" class="display-card" v-loading="loading" element-loading-text="加载中...">
          <!-- 选项卡标题 -->
          <div class="display-tabs">
            <div class="tab-title" :class="{active: activeTab === 'rated'}" @click="activeTab='rated'">
              我的评分记录
            </div>
            <div class="tab-title" :class="{active: activeTab === 'similar'}" @click="activeTab='similar'">
              相似用户匹配度
            </div>
            <div class="tab-title" :class="{active: activeTab === 'liked'}" @click="activeTab='liked'">
              我的点赞电影
            </div>
          </div>

          <!-- 选项卡内容 -->
          <div class="tab-content">
            <!-- 我的评分记录模块 -->
            <div v-if="activeTab === 'rated'">
              <div class="rated-movies-list" v-if="userRatedMovies.length > 0">
                <div class="rated-movie-item" v-for="(item, idx) in userRatedMovies" :key="idx">
                  <span class="movie-name">{{ item.movie.movieName || '未知电影' }}</span>
                  <el-rate v-model="item.rating" disabled max="5" class="movie-rating"></el-rate>
                </div>
              </div>
              <p class="empty-tip" v-else>暂无评分记录</p>
            </div>

            <!-- 相似用户匹配度模块 -->
            <div v-if="activeTab === 'similar'">
              <div class="similar-users-list" v-if="similarityResults.length > 0">
                <div class="similar-user-item" v-for="(item, idx) in similarityResults" :key="idx">
                  <span class="user-id">{{ item.userId }}</span>
                  <div class="similarity-wrap">
                    <span class="percent">{{ (item.similarity * 100).toFixed(1) }}%</span>
                    <el-progress :percentage="item.similarity * 100" :show-text="false" width="100px"></el-progress>
                  </div>
                </div>
              </div>
              <p class="empty-tip" v-else>暂无相似用户</p>
            </div>

            <!-- 我的点赞电影模块 -->
            <div v-if="activeTab === 'liked'">
              <div class="liked-movies-list" v-if="userLikedMovies.length > 0">
                <div class="liked-movie-item" v-for="(movie, idx) in userLikedMovies" :key="idx">
                  <span class="movie-name">{{ movie.movieName || '未知电影' }}</span>
                  <el-button type="text" size="small" @click="handleMovieClick(movie)">查看详情</el-button>
                </div>
              </div>
              <p class="empty-tip" v-else>暂无点赞记录</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 推荐电影列表组件 -->
    <MovieList :movies="movies" :loading="loading" :showTitle="true" :recommendationType="recommendationType" @movie-click="handleMovieClick" />
  </div>
</template>

<script>
// 导入子组件和接口方法（确保路径与你的项目一致）
import MovieList from './MovieList.vue';
import { 
  getCollaborativeFilteringRecommend, 
  getAllCommentCreators,
  getRecommendMoviesByDirector,
  getRecommendMoviesByTypeAndRegion,
  getRecommendMoviesByActorAndDirector,
  getUsersWithLikes,
  getLikedMovies
} from '../api/recommend';

export default {
  name: 'CollaborativeFilteringRecommend',
  components: {
    MovieList // 注册电影列表组件
  },
  data() {
    return {
      movies: [], // 推荐电影列表
      loading: false, // 全局加载状态
      selectedUserId: '', // 当前选中的用户ID
      userList: [], // 下拉选择的用户列表
      userRatedMovies: [], // 接口返回：用户已评分电影
      similarityResults: [], // 接口返回：相似用户及匹配度
      userLikedMovies: [], // 用户点赞的电影
      activeTab: 'rated', // 红框区域选项卡默认选中「我的评分记录」
      recommendationType: 'collaborative' // 推荐类型：collaborative, director, type-region, actor-director
    }
  },
  // 页面挂载时加载用户列表并默认获取推荐
  async mounted() {
    await this.loadUserList();
    if (this.userList.length > 0) {
      this.selectedUserId = this.userList[0].value;
      this.loadRecommendations();
    }
  },
  methods: {
    // 根据推荐类型加载用户列表：协同过滤用评论用户，知识图谱推荐用有点赞记录的用户
    async loadUserList() {
      const isKgType = ['director', 'type-region', 'actor-director'].indexOf(this.recommendationType) !== -1;
      try {
        if (isKgType) {
          const res = await getUsersWithLikes();
          const list = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
          this.userList = (list || []).map(name => ({ value: name, label: name }));
          if (this.userList.length === 0) {
            this.$message.warning('暂无有点赞记录的用户，请先使用「生成点赞记录」或选择协同过滤推荐');
          }
        } else {
          const response = await getAllCommentCreators();
          if (response && response.data) {
            if (response.data.code === 200 && response.data.data) {
              this.userList = response.data.data.map(creator => ({ value: creator, label: creator }));
            } else if (Array.isArray(response.data)) {
              this.userList = response.data.map(creator => ({ value: creator, label: creator }));
            } else {
              this.userList = [{ value: 'default', label: '默认用户' }];
              this.selectedUserId = 'default';
            }
          } else {
            this.userList = [{ value: 'default', label: '默认用户' }];
            this.selectedUserId = 'default';
          }
        }
      } catch (error) {
        console.error('获取用户列表失败:', error);
        this.userList = [{ value: 'default', label: '默认用户' }];
        this.selectedUserId = 'default';
      }
    },

    // 推荐类型切换：先按类型加载对应用户列表（点赞用户 / 评论用户），再拉推荐
    async onRecommendationTypeChange() {
      await this.loadUserList();
      if (this.userList.length > 0) {
        this.selectedUserId = this.userList[0].value;
        await this.loadRecommendations();
      } else {
        this.movies = [];
        this.userLikedMovies = [];
      }
    },

    // 获取推荐统计标题
    getRecommendStatsTitle() {
      let title = `基于用户 ${this.selectedUserId} 的`;
      switch (this.recommendationType) {
        case 'collaborative':
          title += `评分，为您推荐了 ${this.movies.length} 部电影`;
          break;
        case 'director':
          title += `点赞，为您推荐了 ${this.movies.length} 部同导演电影`;
          break;
        case 'type-region':
          title += `点赞，为您推荐了 ${this.movies.length} 部同类型同地区电影`;
          break;
        case 'actor-director':
          title += `点赞，为您推荐了 ${this.movies.length} 部同演员同导演电影`;
          break;
        default:
          title += `推荐，为您推荐了 ${this.movies.length} 部电影`;
      }
      return title;
    },

    // 核心方法：获取推荐数据
    async loadRecommendations() {
      // 重置所有数据，避免切换用户时旧数据残留
      this.movies = [];
      this.userRatedMovies = [];
      this.similarityResults = [];
      this.userLikedMovies = [];
      this.loading = true;
      try {
        if (this.recommendationType === 'collaborative') {
          // 调用协同过滤推荐接口
          const response = await getCollaborativeFilteringRecommend(this.selectedUserId);
          let resData = null;

          // 兼容两种常见接口返回格式（解决axios拦截器导致的解析问题）
          // 格式1：标准Result格式 {code:200, msg:"", data:{...}}（未配置拦截器）
          if (response.data && response.data.code !== undefined) {
            if (response.data.code === 200) {
              resData = response.data.data;
            } else {
              this.$message.warning(response.data.msg || '获取推荐失败');
              return;
            }
          }
          // 格式2：拦截器直接返回data对象（项目中大概率是这种情况）
          else {
            resData = response.data || response;
          }

          // ########### 关键修正：移除?.，替换为&&兼容老环境 ###########
          this.movies = (resData && resData.recommendedMovies) || [];
          this.userRatedMovies = (resData && resData.userRatedMovies) || [];
          this.similarityResults = (resData && resData.similarityResults) || [];
          // ###########################################################
        } else {
          // 调用基于用户点赞的推荐接口
          let response;
          switch (this.recommendationType) {
            case 'director':
              response = await getRecommendMoviesByDirector(this.selectedUserId, 20);
              break;
            case 'type-region':
              response = await getRecommendMoviesByTypeAndRegion(this.selectedUserId, 20);
              break;
            case 'actor-director':
              response = await getRecommendMoviesByActorAndDirector(this.selectedUserId, 20);
              break;
            default:
              response = null;
          }

          if (response) {
            // request 拦截器返回 Result：{ code, msg, data }
            if (response.code === 200) {
              this.movies = response.data || [];
            } else {
              this.$message.warning(response.msg || '获取推荐失败');
              return;
            }
          }

          // 加载用户点赞的电影（用于显示在选项卡中）
          this.loadUserLikedMovies();
        }

        // 仅当推荐电影为空时，才提示无有效数据（避免误提示）
        if (this.movies.length === 0) {
          this.$message.warning('未获取到有效推荐数据');
        }
      } catch (error) {
        // 异常捕获，友好提示
        console.error('获取推荐失败:', error);
        console.error('错误详情:', error.response && error.response.data || error.message);
        this.$message.error('获取推荐失败，请稍后重试');
      } finally {
        // 无论成功/失败，结束加载状态
        this.loading = false;
      }
    },

    // 加载用户点赞的电影（基于用户点赞记录接口，用于「我的点赞电影」选项卡）
    async loadUserLikedMovies() {
      const isKgType = ['director', 'type-region', 'actor-director'].indexOf(this.recommendationType) !== -1;
      if (!isKgType) return;
      try {
        const res = await getLikedMovies(this.selectedUserId);
        const list = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
        this.userLikedMovies = list || [];
      } catch (error) {
        console.error('获取用户点赞电影失败:', error);
        this.userLikedMovies = [];
      }
    },

    // 电影点击事件（向父组件传递）
    handleMovieClick(movie) {
      console.log('点击电影:', movie);
      this.$emit('movie-click', movie);
    }
  }
}
</script>

<style scoped>
/* 全局容器样式 */
.collaborative-filtering-recommend {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 标题和描述 */
.section-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
  text-align: center;
}
.section-description {
  font-size: 16px;
  color: #666;
  text-align: center;
  margin-bottom: 30px;
  line-height: 1.6;
}

/* 推荐控制区 - 左右布局核心样式 */
.recommend-controls {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 30px;
}
/* 左侧：选择用户+按钮 */
.control-left {
  flex: 1;
}
.control-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}
/* 右侧：红框展示区（固定最小宽度，保证布局） */
.control-right {
  flex: 1;
  min-width: 350px;
}

/* 红框区域卡片样式 */
.display-card {
  height: 100%;
  min-height: 280px;
  border-radius: 8px;
}

/* 选项卡标题样式 */
.display-tabs {
  display: flex;
  border-bottom: 1px solid #e6e6e6;
  margin-bottom: 15px;
}
.tab-title {
  padding: 10px 15px;
  cursor: pointer;
  color: #666;
  font-weight: 500;
  transition: all 0.2s ease;
}
.tab-title.active {
  color: #409EFF;
  border-bottom: 2px solid #409EFF;
}
.tab-title:hover {
  color: #409EFF;
}

/* 选项卡内容区域 */
.tab-content {
  padding: 10px 0;
  height: calc(100% - 50px);
}

/* 我的评分记录样式 */
.rated-movies-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 5px;
}
.rated-movie-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

/* 相似用户匹配度样式 */
.similar-users-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 5px;
}
.similar-user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

/* 我的点赞电影样式 */
.liked-movies-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 5px;
}
.liked-movie-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

/* 通用样式 */
.movie-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
}
.movie-rating {
  color: #F7BA2A;
}
.user-id {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
}
.similarity-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.percent {
  color: #F56C6C;
  font-weight: 600;
  width: 50px;
  text-align: right;
}

/* 空数据提示样式 */
.empty-tip {
  text-align: center;
  color: #909399;
  padding: 20px 0;
  font-size: 14px;
  margin: 0;
}

/* 推荐结果统计提示 */
.recommend-stats {
  margin-top: 15px;
}

/* 全局小样式修正 */
.el-form-item {
  margin-bottom: 0;
}

/* 响应式适配：小屏幕（768px以下）自动切换为上下布局 */
@media (max-width: 768px) {
  .recommend-controls {
    flex-direction: column;
  }
  .control-right {
    min-width: 100%;
  }
  .section-title {
    font-size: 24px;
  }
  .section-description {
    font-size: 14px;
  }
  .control-form {
    padding: 15px;
  }
  .display-card {
    min-height: 240px;
  }
  .display-tabs {
    flex-wrap: wrap;
  }
  .tab-title {
    padding: 8px 12px;
    font-size: 14px;
  }
}
</style>