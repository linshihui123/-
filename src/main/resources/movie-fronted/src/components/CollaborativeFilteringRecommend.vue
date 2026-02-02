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

    <!-- 推荐理由展示区域 -->
    <div class="recommendation-explanation" v-if="movies.length > 0">
      <el-card shadow="never" class="explanation-card">
        <div class="explanation-header">
          <i class="el-icon-lightning"></i>
          <span class="explanation-title">协同过滤推荐原理</span>
        </div>
        <div class="explanation-content">
          <p>系统通过分析您与其他用户的评分相似性，计算余弦相似度，找出与您偏好相似的用户群体。</p>
          <p>然后推荐这些相似用户评分较高的电影给您，确保推荐结果符合您的个人口味。</p>
          <p class="similarity-note">💡 相似度越高，推荐的相关性越强</p>
        </div>
      </el-card>
    </div>

    <MovieList :movies="movies" :loading="loading" :showTitle="true" recommendationType="collaborative" @movie-click="handleMovieClick" />
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
        console.log('🔍 调试：完整响应对象:', response);
        console.log('🔍 调试：response.data结构:', response.data);

        // 添加更灵活的数据提取逻辑
        let recommendations = [];
        if (response.data && response.data.code === 200) {
          // 尝试多种可能的数据路径
          if (response.data.data) {
            recommendations = response.data.data;
          } else if (Array.isArray(response.data)) {
            recommendations = response.data;
          } else if (response.data.recommendations) {
            recommendations = response.data.recommendations;
          }
        } else if (Array.isArray(response.data)) {
          // 如果直接返回数组
          recommendations = response.data;
        }

        console.log('🔍 调试：提取的recommendations数据:', recommendations);
        console.log('🔍 调试：recommendations长度:', recommendations.length);

        if (recommendations && recommendations.length > 0) {
          this.movies = recommendations;
          console.log('✅ 成功设置movies数据，数量:', this.movies.length);
          console.log('📌 第一部电影名称:', recommendations[0].movieName || recommendations[0].name || '未知');
        } else {
          this.movies = [];
          console.warn('⚠️ 数据为空或格式不匹配');
        }
      } catch (error) {
        console.error('❌ 获取协同过滤推荐失败:', error);
        // 兼容旧版JS语法，避免使用?.操作符
        console.error('❌ 错误详情:', error.response ? error.response.data : undefined, error.message);
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

.recommendation-explanation {
  margin: 20px 0;
}

.explanation-card {
  border-left: 4px solid #409EFF;
  background-color: #f8f9ff;
  border-radius: 8px;
}

.explanation-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #eef5ff;
  border-bottom: 1px solid #dcdfe6;
  font-weight: 600;
  color: #409EFF;
}

.explanation-header i {
  margin-right: 8px;
  font-size: 18px;
}

.explanation-content {
  padding: 16px;
  line-height: 1.6;
  color: #606266;
}

.explanation-content p {
  margin: 8px 0;
}

.similarity-note {
  margin-top: 12px;
  font-style: italic;
  color: #909399;
  font-size: 14px;
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