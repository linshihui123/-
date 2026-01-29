<template>
  <div class="collaborative-filtering-recommend">
    <h2 class="section-title">推荐</h2>
    <p class="section-description">
      基于用户评分的协同过滤推荐 - 系统根据您与其他用户的评分相似性，为您推荐相似用户喜欢的电影
    </p>

    <!-- 推荐控制面板 -->
    <div class="recommend-controls">
      <el-form :inline="true" class="control-form">
        <el-form-item label="选择用户">
          <el-select v-model="selectedUserId" placeholder="请选择用户" @change="loadRecommendations">
            <el-option v-for="item in userList" :key="item.value" :label="item.label" :value="item.value"></el-option>
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
          :title="`基于用户 ${selectedUserId} 的评分，为您推荐了 ${movies.length} 部电影`"
          type="success"
          :closable="false"
          show-icon>
        </el-alert>
      </div>
    </div>

    <!-- 推荐电影列表 -->
    <MovieList :movies="movies" :loading="loading" :showTitle="true" @movie-click="handleMovieClick" />
  </div>
</template>

<script>
import MovieList from './MovieList.vue';
import { getCollaborativeFilteringRecommend, getAllCommentCreators } from '../api/recommend';

export default {
  name: 'CollaborativeFilteringRecommend',
  components: {
    MovieList
  },
  data() {
    return {
      movies: [],
      loading: false,
      selectedUserId: '', // 默认用户ID
      userList: [] // 从评论创建者动态加载
    }
  },
  async mounted() {
    await this.loadCommentCreators();
    if (this.userList.length > 0) {
      this.selectedUserId = this.userList[0].value;
    }
    this.loadRecommendations();
  },
  methods: {
    async loadCommentCreators() {
      try {
        console.log('开始请求评论创建者数据...');
        const response = await getAllCommentCreators();
        console.log('收到响应:', response);
        
        if (response && response.data) {
          console.log('响应数据结构正确');
          
          // 检查是否是标准Result格式
          if (response.data.hasOwnProperty('code')) {
            // 标准Result格式
            if (response.data.code === 200 && response.data.data) {
              // 将获取到的评论创建者转换为下拉选项格式
              this.userList = response.data.data.map((creator, index) => ({
                value: creator, // 使用实际的创建者名称作为值
                label: creator  // 使用实际的创建者名称作为标签
              }));
              
              // 如果列表不为空，设置默认选中项
              if (this.userList.length > 0) {
                this.selectedUserId = this.userList[0].value;
              }
              console.log('成功加载评论创建者，数量:', this.userList.length);
            } else {
              console.error('获取评论创建者失败:', response.data.message || '服务器错误');
              // 如果获取失败，回退到默认值
              this.userList = [
                { value: 'default', label: '默认用户' }
              ];
              this.selectedUserId = 'default';
            }
          } else {
            // 假设直接返回了数据数组
            if (Array.isArray(response.data)) {
              this.userList = response.data.map((creator, index) => ({
                value: creator,
                label: creator
              }));
              
              if (this.userList.length > 0) {
                this.selectedUserId = this.userList[0].value;
              }
              console.log('成功加载评论创建者，数量:', this.userList.length);
            } else {
              console.error('响应数据格式不符合预期:', typeof response.data);
              this.userList = [
                { value: 'default', label: '默认用户' }
              ];
              this.selectedUserId = 'default';
            }
          }
        } else {
          console.error('响应数据为空或格式错误');
          this.userList = [
            { value: 'default', label: '默认用户' }
          ];
          this.selectedUserId = 'default';
        }
      } catch (error) {
        console.error('获取评论创建者失败:', error);
        console.error('错误详情:', error.message, error.stack);
        // 如果获取失败，回退到默认值
        this.userList = [
          { value: 'default', label: '默认用户' }
        ];
        this.selectedUserId = 'default';
      }
    },
    async loadRecommendations() {
      this.loading = true;
      try {
        const response = await getCollaborativeFilteringRecommend(this.selectedUserId);
        if (response.data.code === 200) {
          this.movies = response.data.data || [];
        } else {
          this.$message.error(response.data.message || '获取推荐失败');
          this.movies = [];
        }
      } catch (error) {
        console.error('获取协同过滤推荐失败:', error);
        this.$message.error('获取推荐失败，请稍后重试');
        this.movies = [];
      } finally {
        this.loading = false;
      }
    },
    handleMovieClick(movie) {
      console.log('点击了电影:', movie);
      // 这里可以添加电影详情查看逻辑
      this.$emit('movie-click', movie);
    }
  }
}
</script>

<style scoped>
.collaborative-filtering-recommend {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

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

.recommend-controls {
  margin-bottom: 30px;
}

.control-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.recommend-stats {
  margin-top: 15px;
}

.el-form-item {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .section-title {
    font-size: 24px;
  }
  
  .section-description {
    font-size: 14px;
  }
  
  .control-form {
    padding: 15px;
  }
}
</style>