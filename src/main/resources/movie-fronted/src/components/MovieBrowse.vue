<template>
  <div class="movie-browse-container">
    <el-container>
      <!-- 主内容区 -->
      <el-main class="main-content">
        <el-card shadow="hover">
          <div slot="header" class="card-header">
            <span>电影浏览</span>
            <el-select v-model="selectedType" placeholder="选择类型" style="margin-right: 10px; float: right;" @change="handleTypeChange">
              <el-option label="全部" value=""></el-option>
              <el-option label="剧情" value="剧情"></el-option>
              <el-option label="喜剧" value="喜剧"></el-option>
              <el-option label="动作" value="动作"></el-option>
              <el-option label="爱情" value="爱情"></el-option>
              <el-option label="科幻" value="科幻"></el-option>
              <el-option label="悬疑" value="悬疑"></el-option>
              <el-option label="惊悚" value="惊悚"></el-option>
              <el-option label="恐怖" value="恐怖"></el-option>
              <el-option label="动画" value="动画"></el-option>
              <el-option label="纪录片" value="纪录片"></el-option>
            </el-select>
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

                <div class="movie-actions">
                  <el-button
                    type="primary"
                    size="mini"
                    @click.stop="showDetailedMovieComments(movie)"
                    class="action-button"
                  >
                    <i class="el-icon-chat-dot-round"></i> 评论详情
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <div class="pagination-container" v-if="total > 0">
            <el-pagination
              @current-change="handlePageChange"
              :current-page="currentPage"
              :page-size="pageSize"
              layout="prev, pager, next, jumper"
              :total="total"
              :background="true"
            ></el-pagination>
          </div>
        </el-card>
      </el-main>

      <!-- AI助手推荐侧边栏 -->
      <el-aside width="350px" class="recommend-sidebar">
        <el-card class="ai-recommend-card" shadow="hover">
          <div slot="header" class="ai-card-header">
            <i class="el-icon-chat-dot-round"></i>
            <span>AI 助手推荐</span>
          </div>

          <div class="ai-chat-container">
            <div class="chat-messages" ref="chatMessages">
              <div v-for="(msg, index) in chatMessages" :key="index" class="message-item" :class="msg.role">
                <div class="message-avatar" :class="msg.role">
                  <i v-if="msg.role === 'user'" class="el-icon-user-solid"></i>
                  <i v-if="msg.role === 'assistant'" class="el-icon-chat-line-round"></i>
                </div>
                <div class="message-content">
                  <p v-html="formatMessage(msg.content)"></p>
                </div>
              </div>
            </div>

            <div class="input-area">
              <el-input
                v-model="userInput"
                :rows="3"
                type="textarea"
                placeholder="告诉我您喜欢什么类型的电影，例如：'推荐一些高分科幻电影' 或 '我想看张艺谋导演的电影'"
                maxlength="200"
                show-word-limit
                @keyup.enter.native="handleSendRequest($event)"
              ></el-input>
              <el-button
                type="primary"
                :loading="aiLoading"
                @click="sendRecommendRequest"
                :disabled="!userInput.trim()"
                class="send-button"
              >
                <i class="el-icon-position"></i> 获取推荐
              </el-button>
            </div>

            <div class="quick-actions">
              <el-button size="mini" @click="setQuickPrompt('推荐一些高分电影')">高分电影</el-button>
              <el-button size="mini" @click="setQuickPrompt('推荐科幻类型的电影')">科幻电影</el-button>
              <el-button size="mini" @click="setQuickPrompt('推荐喜剧类型的电影')">喜剧电影</el-button>
              <el-button size="mini" @click="setQuickPrompt('推荐张艺谋导演的电影')">张艺谋作品</el-button>
            </div>
          </div>
        </el-card>
      </el-aside>
    </el-container>

    <!-- 评论弹窗 -->
    <el-dialog
      title="电影评论"
      :visible.sync="commentDialogVisible"
      width="60%"
      :before-close="closeCommentDialog">
      <div v-if="currentMovie">
        <h3>{{ currentMovie.movieName }} 的评论</h3>
        <div v-if="comments.length > 0" class="comments-list">
          <el-card v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-content">
              <p><strong>用户：</strong>{{ comment.creator || '匿名用户' }}</p>
              <p><strong>评论内容：</strong>{{ comment.content || '暂无评论内容' }}</p>
              <p><strong>评分：</strong>{{ comment.comment_rating !== undefined ? comment.comment_rating : '暂无评分' }}</p>
              <p><strong>评论时间：</strong>{{ comment.comment_time || '未知' }}</p>
              <p><strong>添加时间：</strong>{{ comment.comment_add_time || '未知' }}</p>
            </div>
          </el-card>
        </div>
        <div v-else class="no-comments">
          <p>暂无评论</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { parseUserIntent, getRecommendMoviesPost } from '@/api/recommend'

export default {
  name: 'MovieBrowse',
  data() {
    return {
      movies: [],
      currentPage: 1,
      pageSize: 12,
      total: 0,
      loading: false,
      selectedType: '',
      commentDialogVisible: false,
      currentMovie: null,
      comments: [],
      // AI助手相关数据
      chatMessages: [
        {
          role: 'assistant',
          content: '您好！我是AI电影推荐助手，可以根据您的喜好推荐电影。请告诉我您想看什么类型的电影，或者对导演、演员有什么偏好吗？'
        }
      ],
      userInput: '',
      aiLoading: false,
      currentUser: null
    }
  },
  created() {
    this.loadAllMovies();
    this.checkUserSession();
  },
  mounted() {
    this.scrollToBottom();
  },
  watch: {
    chatMessages: {
      handler() {
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      },
      deep: true
    }
  },
  methods: {
    // 检查用户会话
    checkUserSession() {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          this.currentUser = JSON.parse(userStr);
        } catch (e) {
          console.error('解析用户信息失败', e);
        }
      }
    },

    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        if (this.$refs.chatMessages) {
          this.$refs.chatMessages.scrollTop = this.$refs.chatMessages.scrollHeight;
        }
      });
    },

    // 格式化消息内容
    formatMessage(content) {
      return content.replace(/\n/g, '<br>');
    },

    // 设置快捷提示
    setQuickPrompt(prompt) {
      this.userInput = prompt;
    },

    // 发送推荐请求
    async sendRecommendRequest() {
      if (!this.userInput.trim()) {
        this.$message.warning('请输入您的推荐需求');
        return;
      }

      // 添加用户消息到聊天记录
      this.chatMessages.push({
        role: 'user',
        content: this.userInput
      });

      this.aiLoading = true;

      try {
        // 获取当前用户的ID
        const userId = this.currentUser ? this.currentUser.userid || this.currentUser.id || this.currentUser.userId : 'anonymous';

        // 调用后端解析用户意图
        const intentResponse = await parseUserIntent({
          userInput: this.userInput,
          userId: userId
        });

        // 如果解析成功，再获取推荐电影
        if (intentResponse && intentResponse.intentType) {
          const recommendResponse = await getRecommendMoviesPost({
            userId: userId,
            topN: 6,
            intent: intentResponse
          });

          if (recommendResponse && recommendResponse.data) {
            // 更新主内容区的电影列表为推荐结果
            this.movies = recommendResponse.data.map(movie => {
              return {
                ...movie,
                id: movie.id || 0,
                movieId: movie.movieId || null,
                movieName: movie.movieName || movie.name || '未知电影',
                type: movie.type || '未知',
                direction: movie.direction || movie.region || '未知',
                rating: movie.rating || movie.movie_rating || '暂无评分',
                instruction: movie.instruction || '',
                directors: movie.directors || movie.director || [],
                actors: movie.actors || movie.actor || [],
                directorList: movie.directorString ? movie.directorString.split('|') :
                             movie.director ? (typeof movie.director === 'string' ? movie.director.split('|') : []) : [],
                actorList: movie.actorString ? movie.actorString.split('|') :
                          movie.actor ? (typeof movie.actor === 'string' ? movie.actor.split('|') : []) : []
              };
            }) || [];

            // 重置分页信息
            this.total = this.movies.length;
            this.currentPage = 1;

            // 添加AI回复
            const intentType = intentResponse.intentType;
            let responseText = '';

            if (intentType === 'director_based') {
              responseText = `根据您的要求，为您推荐了相关导演的作品，共${this.movies.length}部电影。`;
            } else if (intentType === 'type_based') {
              responseText = `根据您的类型偏好，为您推荐了${intentResponse.params && intentResponse.params.type ? intentResponse.params.type : ''}类型的电影，共${this.movies.length}部。`;
            } else if (intentType === 'rating_based') {
              responseText = `为您推荐了高分电影，共${this.movies.length}部。`;
            } else {
              responseText = `根据您的喜好，为您推荐了${this.movies.length}部电影。`;
            }

            this.chatMessages.push({
              role: 'assistant',
              content: responseText + '<br><br>以下是为您推荐的电影：<br>' +
                      this.movies.slice(0, 3).map(m => `• ${m.movieName || m.name}`).join('<br>') +
                      (this.movies.length > 3 ? `<br>... 还有${this.movies.length - 3}部电影` : '')
            });
          } else {
            this.chatMessages.push({
              role: 'assistant',
              content: '抱歉，没有找到符合您要求的电影，请尝试其他条件。'
            });
          }
        } else {
          this.chatMessages.push({
            role: 'assistant',
            content: '抱歉，未能理解您的需求，请尝试用更清晰的语言描述您的电影偏好。'
          });
        }
      } catch (error) {
        console.error('AI推荐请求失败:', error);
        this.chatMessages.push({
          role: 'assistant',
          content: '抱歉，推荐服务暂时不可用，请稍后重试。'
        });
      } finally {
        this.aiLoading = false;
        this.userInput = '';
      }
    },

    // 处理回车键发送
    handleSendRequest(event) {
      if (event.shiftKey) {
        // Shift + Enter: 换行
        return;
      }
      event.preventDefault();
      this.sendRecommendRequest();
    },

    // 加载所有电影
    async loadAllMovies() {
      this.loading = true;
      try {
        let url;

        // 根据是否选择了类型来决定使用哪个API端点
        if (this.selectedType) {
          // 使用按类型获取电影的API
          url = `/movie/by-type/${encodeURIComponent(this.selectedType)}?page=${this.currentPage - 1}&size=${this.pageSize}`;
        } else {
          // 使用获取所有电影的API
          url = `/movie/list?page=${this.currentPage - 1}&size=${this.pageSize}`;
        }

        // 使用配置好的request实例
        const response = await request.get(url);
        if (response && response.code === 200 && response.data) {
          // 处理电影数据，确保字段映射正确
          this.movies = response.data.map(movie => {
            return {
              ...movie,
              id: movie.id || 0,
              movieId: movie.movieId || null,
              movieName: movie.movieName || movie.name || '未知电影',
              type: movie.type || '未知',
              direction: movie.direction || movie.region || '未知',
              rating: movie.rating || movie.movie_rating || '暂无评分',
              instruction: movie.instruction || '',
              directors: movie.directors || movie.director || [],
              actors: movie.actors || movie.actor || [],
              // 添加解析后的列表
              directorList: movie.directorString ? movie.directorString.split('|') :
                           movie.director ? (typeof movie.director === 'string' ? movie.director.split('|') : []) : [],
              actorList: movie.actorString ? movie.actorString.split('|') :
                        movie.actor ? (typeof movie.actor === 'string' ? movie.actor.split('|') : []) : []
            };
          }) || [];
          // 尝试从响应中获取总数量
          this.total = response.total || response.data.length;
          // 显示成功消息
          this.$message.success(`已加载 ${this.movies.length} 部电影`);
        } else {
          this.movies = [];
          this.total = 0;
          this.$message.error('未找到电影数据');
        }
      } catch (error) {
        console.error('加载电影列表失败:', error);
        this.movies = [];
        this.total = 0;
        this.$message.error('加载电影列表失败，请重试');
      } finally {
        this.loading = false;
      }
    },
    // 处理分页变化
    handlePageChange(page) {
      this.currentPage = page;
      this.loadAllMovies();
    },
    // 处理类型选择变化
    handleTypeChange() {
      // 当类型改变时，重置到第一页
      this.currentPage = 1;
      this.loadAllMovies();
    },

    // 加载有评论的电影
    async loadMoviesWithComments() {
      this.loading = true;
      try {
        const response = await request.get(`/movie/movies-with-comments?page=${this.currentPage - 1}&size=${this.pageSize}`);
        if (response && response.code === 200 && response.data) {
          // 处理电影数据，确保字段映射正确
          this.movies = response.data.map(movie => {
            return {
              ...movie,
              id: movie.id || 0,
              movieId: movie.movieId || null,
              movieName: movie.movieName || movie.name || '未知电影',
              type: movie.type || '未知',
              direction: movie.direction || movie.region || '未知',
              rating: movie.rating || movie.movie_rating || '暂无评分',
              instruction: movie.instruction || '',
              directors: movie.directors || movie.director || [],
              actors: movie.actors || movie.actor || [],
              // 添加解析后的列表
              directorList: movie.directorString ? movie.directorString.split('|') :
                           movie.director ? (typeof movie.director === 'string' ? movie.director.split('|') : []) : [],
              actorList: movie.actorString ? movie.actorString.split('|') :
                        movie.actor ? (typeof movie.actor === 'string' ? movie.actor.split('|') : []) : []
            };
          }) || [];
          // 尝试从响应中获取总数量
          this.total = response.total || response.data.length;
          // 显示成功消息
          this.$message.success(`已加载 ${this.movies.length} 部有评论的电影`);
        } else {
          this.movies = [];
          this.total = 0;
          this.$message.error('未找到有评论的电影数据');
        }
      } catch (error) {
        console.error('加载有评论的电影列表失败:', error);
        this.movies = [];
        this.total = 0;
        this.$message.error('加载有评论的电影列表失败，请重试');
      } finally {
        this.loading = false;
      }
    },
    // 处理电影点击事件
    handleMovieClick(movie) {
      this.$emit('movie-click', movie);
    },
    // 格式化导演名称
    getDirectorNames(movie) {
      // 优先使用关系数据，如果没有则使用字符串解析
      if (movie.directors && movie.directors.length > 0) {
        return movie.directors.map(director => director.name || director).join('、');
      } else if (movie.directorString) {
        return movie.directorString.split('|').join('、');
      } else if (movie.directorList && movie.directorList.length > 0) {
        // 使用解析后的导演列表
        return movie.directorList.join('、');
      } else if (movie.director) {
        // 如果director是字符串格式
        if (typeof movie.director === 'string') {
          return movie.director.split('|').join('、');
        }
        // 如果director是对象数组
        if (Array.isArray(movie.director)) {
          return movie.director.map(director => director.name || director).join('、');
        }
        return movie.director;
      }
      return '未知';
    },
    // 格式化演员名称
    getActorNames(movie) {
      // 优先使用关系数据，如果没有则使用字符串解析
      if (movie.actors && movie.actors.length > 0) {
        return movie.actors.map(actor => actor.name || actor).join('、');
      } else if (movie.actorString) {
        return movie.actorString.split('|').join('、');
      } else if (movie.actorList && movie.actorList.length > 0) {
        // 使用解析后的演员列表
        return movie.actorList.join('、');
      } else if (movie.actor) {
        // 如果actor是字符串格式
        if (typeof movie.actor === 'string') {
          return movie.actor.split('|').join('、');
        }
        // 如果actor是对象数组
        if (Array.isArray(movie.actor)) {
          return movie.actor.map(actor => actor.name || actor).join('、');
        }
        return movie.actor;
      }
      return '未知';
    },

    // 显示电影评论
    async showMovieComments(movie) {
      this.currentMovie = movie;
      try {
        // 获取电影评论 - 根据movieId或id获取评论
        const movieId = movie.movieId || movie.info_id || movie.id;
        const response = await request.get(`/movie/by-movie-id/${movieId}/comments`);
        if (response && response.code === 200) {
          this.comments = response.data || [];
          this.commentDialogVisible = true;
        } else {
          this.comments = [];
          this.$message.error('获取评论失败');
        }
      } catch (error) {
        console.error('获取评论失败:', error);
        this.comments = [];
        this.$message.error('获取评论失败，请重试');
      }
    },

    // 显示详细电影评论
    async showDetailedMovieComments(movie) {
      this.currentMovie = movie;
      try {
        // 获取详细电影评论信息 - 包含电影ID、电影名、评论ID和评论内容
        const movieId = movie.movieId || movie.info_id || movie.id;
        const response = await request.get(`http://localhost:8081/movie/movie-comments/${movieId}`);
        if (response && response.code === 200 && response.data) {
          // 直接使用后端返回的数据结构
          this.comments = response.data.map(item => {
            return {
              comment_id: item['comment_id'],
              movie_id: item['movie_id'],
              creator: item['creator'],
              content: item['content'],
              comment_rating: item['comment_rating'],
              comment_time: item['comment_time'],
              comment_add_time: item['comment_add_time']
            };
          });
          this.commentDialogVisible = true;

          // 显示成功消息
          this.$message.success(`获取到 ${this.comments.length} 条评论详情`);
        } else {
          this.comments = [];
          this.$message.error('获取评论详情失败');
        }
      } catch (error) {
        console.error('获取评论详情失败:', error);
        this.comments = [];
        this.$message.error('获取评论详情失败，请重试');
      }
    },

    // 关闭评论弹窗
    closeCommentDialog() {
      this.commentDialogVisible = false;
      this.currentMovie = null;
      this.comments = [];
    }
  }
}
</script>

<style scoped>
.movie-browse-container {
  margin: 20px auto;
  max-width: 1600px;
  padding: 0 15px;
}

.movie-browse-container .el-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.main-content {
  padding: 0;
  flex: 1;
}

.recommend-sidebar {
  width: 350px !important;
  padding: 0;
  margin-left: 20px;
}

.recommend-sidebar .el-card {
  height: calc(100vh - 120px);
  margin: 0;
  border-radius: 0 8px 8px 0;
  display: flex;
  flex-direction: column;
}

.ai-recommend-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-recommend-card .el-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding: 20px;
}

.ai-card-header {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-card-header i {
  color: #409EFF;
}

.movie-card {
  margin-bottom: 25px;
}

.movie-item {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  border-radius: 12px;
  overflow: hidden;
}

/* 移除电影卡片头部样式 */

.movie-content {
  padding: 20px;
}

.movie-item {
  height: auto;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
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
  margin-bottom: 20px;
  min-height: 67px;
}

.movie-actions {
  display: flex;
  justify-content: space-between;
  padding: 0 20px 20px;
  gap: 10px;
}

.action-button {
  flex: 1;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-button:hover {
  transform: translateY(-1px);
}

.pagination-container {
  margin-top: 30px;
  text-align: center;
}

.comments-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
}

.comment-item {
  margin-bottom: 15px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.comment-content p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.5;
}

.no-comments {
  text-align: center;
  color: #999;
  padding: 30px;
  font-style: italic;
}

/* AI助手聊天区域样式 */
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #fafafa;
  min-height: 300px;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 3px;
}

.message-avatar.user {
  background: #409EFF;
  color: white;
}

.message-avatar.assistant {
  background: #67C23A;
  color: white;
}

.message-content {
  max-width: calc(100% - 50px);
  margin: 0 15px;
}

.message-content p {
  margin: 0;
  padding: 10px 15px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
}

.message-item.user .message-content p {
  background: #409EFF;
  color: white;
  border-bottom-right-radius: 5px;
}

.message-item.assistant .message-content p {
  background: white;
  color: #333;
  border: 1px solid #e0e0e0;
  border-bottom-left-radius: 5px;
}

.input-area {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  background: white;
}

.send-button {
  margin-top: 10px;
  width: 100%;
}

.quick-actions {
  padding: 0 20px 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 1px solid #eee;
  background: white;
}

.quick-actions .el-button {
  padding: 6px 10px;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .recommend-sidebar {
    width: 300px !important;
    margin-left: 15px;
  }
}

@media (max-width: 992px) {
  .recommend-sidebar {
    display: none;
  }

  .movie-browse-container {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .movie-card {
    margin-bottom: 20px;
  }

  .movie-card-header {
    height: 180px;
  }

  .movie-content {
    padding: 15px;
  }

  .movie-name {
    font-size: 16px;
  }

  .movie-actions {
    flex-direction: column;
    gap: 8px;
  }

  .action-button {
    width: 100%;
  }

  .card-header {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }

  .el-select {
    margin-right: 0 !important;
    margin-top: 10px;
    float: none !important;
    width: 100%;
  }
}

@media (max-width: 480px) {
  .movie-browse-container {
    margin: 10px;
    padding: 0 5px;
  }

  .movie-metadata {
    flex-direction: column;
    gap: 8px;
  }

  .card-header {
    padding: 15px;
  }
}
</style>