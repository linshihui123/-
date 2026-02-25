<template>
  <div class="movie-browse-container">
    <el-container>
      <!-- 主内容区 -->
      <el-main class="main-content">
        <el-card shadow="hover">
          <div slot="header" class="card-header">
            <span>电影浏览</span>
            <div class="header-controls">
              <el-select v-model="selectedType" placeholder="选择类型" style="margin-right: 10px;" @change="handleTypeChange">
                <el-option label="全部" value=""></el-option>
                <el-option label="有评论的电影" value="with_comments"></el-option>
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
    </el-container>

    <!-- 电影详情弹窗 -->
    <el-dialog
        title="电影详情"
        :visible.sync="movieDetailDialogVisible"
        width="60%"
        :before-close="closeMovieDetailDialog"
    >
      <div v-if="currentMovie" class="movie-detail-content">
        <h3>{{ currentMovie.movieName || currentMovie.name || '未知电影' }}</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="电影ID">{{ currentMovie.id || currentMovie.movieId || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="电影名称">{{ currentMovie.movieName || currentMovie.name || '未知电影' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentMovie.type || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="地区">{{ currentMovie.direction || currentMovie.region || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ currentMovie.rating || currentMovie.movie_rating || currentMovie.movieRating || '暂无评分' }}</el-descriptions-item>
          <el-descriptions-item label="导演">{{ getDirectorNames(currentMovie) }}</el-descriptions-item>
          <el-descriptions-item label="演员">{{ getActorNames(currentMovie) }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">
            <div class="movie-intro">{{ currentMovie.instruction || currentMovie.description || '暂无简介' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 评论弹窗（新增评论/评分提交功能） -->
    <el-dialog
        title="电影评论"
        :visible.sync="commentDialogVisible"
        width="60%"
        :before-close="closeCommentDialog"
        class="comment-dialog"
    >
      <div v-if="currentMovie" class="comment-dialog-content">
        <h3 class="comment-movie-title">{{ currentMovie.movieName || currentMovie.name }} 的评论</h3>

        <!-- 新增：评论/评分提交表单 -->
        <div class="comment-submit-form" v-if="currentUser">
          <el-card shadow="never" class="submit-card form-card">
            <div slot="header" class="submit-card-header">
              <span class="submit-card-title"><i class="el-icon-edit-outline"></i> 提交我的评论</span>
            </div>
            <div class="submit-form-content">
              <div class="rating-item">
                <label class="form-label">我的评分：</label>
                <div class="rating-wrapper">
                  <el-rate
                      v-model="userRating"
                      :max="10"
                      :colors="['#F7BA2A', '#F7BA2A', '#FF9900']"
                      :void-color="'#C6D1DE'"
                      @change="handleRatingChange"
                      class="rate-component"
                  ></el-rate>
                  <span class="rating-text">{{ userRating !== undefined && userRating !== null ? `${userRating} 分` : '未评分' }}</span>
                </div>
              </div>
              <div class="comment-content-item">
                <label class="form-label">评论内容：</label>
                <el-input
                    v-model="userCommentContent"
                    type="textarea"
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                    placeholder="请输入您的观影感受（最多500字）"
                    class="comment-textarea"
                ></el-input>
              </div>
              <div class="submit-button-group">
                <el-button
                    type="primary"
                    @click="submitComment"
                    :loading="commentSubmitting"
                    :disabled="(userRating === undefined || userRating === null) || !userCommentContent.trim()"
                    icon="el-icon-check"
                    class="submit-btn"
                >
                  提交评论
                </el-button>
                <el-button @click="resetCommentForm" icon="el-icon-refresh-left" class="reset-btn">清空</el-button>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 未登录提示 -->
        <div class="login-tip" v-else>
          <el-card class="login-tip-card">
            <i class="el-icon-warning"></i>
            <span class="login-tip-text">请先登录后再提交评论</span>
            <el-button type="primary" @click="gotoLogin" class="login-btn">去登录</el-button>
          </el-card>
        </div>

        <!-- 现有评论列表 -->
        <div v-if="comments.length > 0" class="comments-list-section">
          <h4 class="section-title"><i class="el-icon-chat-line-square"></i> 已有评论</h4>
          <div class="comments-list">
            <el-card v-for="comment in comments" :key="comment.comment_id" class="comment-item user-comment-card">
              <div class="comment-header">
                <div class="comment-user">
                  <i class="el-icon-user"></i>
                  <span class="user-name">{{ comment.creator || '匿名用户' }}</span>
                </div>
                <div class="comment-rating">
                  <i class="el-icon-star-on" :style="{ color: comment.comment_rating >= 4 ? '#F7BA2A' : comment.comment_rating >= 2 ? '#8CC5FF' : '#E6A23C' }"></i>
                  <span class="rating-value">{{ comment.comment_rating !== undefined ? comment.comment_rating : '暂无评分' }}</span>
                </div>
              </div>
              <div class="comment-body">
                <p class="comment-content-text">{{ comment.content || '暂无评论内容' }}</p>
              </div>
              <div class="comment-footer">
                <span class="comment-time"><i class="el-icon-time"></i> {{ comment.comment_time || comment.comment_add_time || '未知' }}</span>
              </div>
            </el-card>
          </div>
        </div>
        <div v-else class="no-comments">
          <i class="el-icon-chat-square"></i>
          <p>暂无评论，快来抢沙发吧~</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

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
      movieDetailDialogVisible: false,
      currentMovie: null,
      comments: [],
      currentUser: null,
      isShowingCommentsOnly: false,
      userRating: undefined,
      userCommentContent: '',
      commentSubmitting: false
    }
  },
  created() {
    this.loadAllMovies();
    this.checkUserSession();
  },
  methods: {
    // 检查用户会话
    checkUserSession() {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          let parsedUser = JSON.parse(userStr);
          console.log('原始解析的用户信息:', parsedUser);
          
          // 尝试提取真实的用户信息
          let realUser = parsedUser;
          
          // 处理各种可能的用户信息结构
          if (parsedUser && typeof parsedUser === 'object') {
            // 检查是否有username字段
            if (parsedUser.username) {
              realUser = parsedUser;
            } 
            // 检查是否有value字段（可能是嵌套结构）
            else if (parsedUser.value && typeof parsedUser.value === 'object') {
              realUser = parsedUser.value;
            }
            // 检查是否有user字段（可能是登录响应的结构）
            else if (parsedUser.user && typeof parsedUser.user === 'object') {
              realUser = parsedUser.user;
            }
          }
          
          this.currentUser = realUser;
          console.log('最终使用的用户信息:', this.currentUser);
        } catch (e) {
          console.error('解析用户信息失败', e);
        }
      } else {
        console.log('未找到用户信息');
      }
    },

    // 加载所有电影
    async loadAllMovies() {
      this.loading = true;
      try {
        let url;
        if (this.selectedType && this.selectedType !== 'with_comments') {
          url = `/movie/by-type/${encodeURIComponent(this.selectedType)}?page=${this.currentPage - 1}&size=${this.pageSize}`;
        } else {
          url = `/movie/list?page=${this.currentPage - 1}&size=${this.pageSize}`;
        }
        const response = await request.get(url);
        if (response && response.code === 200 && response.data) {
          this.movies = response.data.map((movie, index) => {
            const movieId = movie.id || movie.info_id || movie.movieId || index + 1;
            return {
              ...movie,
              id: movieId,
              movieId: movieId,
              movieName: movie.movieName || movie.name || '未知电影',
              type: movie.type || '未知',
              direction: movie.direction || movie.region || '未知',
              rating: movie.rating || movie.movie_rating || movie.movieRating || '暂无评分',
              instruction: movie.instruction || '',
              directors: movie.directors || movie.director || [],
              actors: movie.actors || movie.actor || [],
              directorList: movie.directorString ? movie.directorString.split('|') :
                  movie.director ? (typeof movie.director === 'string' ? movie.director.split('|') : []) : [],
              actorList: movie.actorString ? movie.actorString.split('|') :
                  movie.actor ? (typeof movie.actor === 'string' ? movie.actor.split('|') : []) : []
            };
          }) || [];
          this.total = response.total || response.data.length;
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

    // 切换到只显示有评论的电影
    async switchToMoviesWithComments() {
      this.loading = true;
      try {
        this.isShowingCommentsOnly = true;
        const response = await request.get(`/movie/movies-with-comments?page=${this.currentPage - 1}&size=${this.pageSize}`);
        if (response && response.code === 200 && response.data) {
          this.movies = response.data.map((movie, index) => {
            const movieId = movie.id || movie.info_id || movie.movieId || index + 1;
            return {
              ...movie,
              id: movieId,
              movieId: movieId,
              movieName: movie.movieName || movie.name || '未知电影',
              type: movie.type || '未知',
              direction: movie.direction || movie.region || '未知',
              rating: movie.rating || movie.movie_rating || movie.movieRating || '暂无评分',
              instruction: movie.instruction || '',
              directors: movie.directors || movie.director || [],
              actors: movie.actors || movie.actor || [],
              directorList: movie.directorString ? movie.directorString.split('|') :
                  movie.director ? (typeof movie.director === 'string' ? movie.director.split('|') : []) : [],
              actorList: movie.actorString ? movie.actorString.split('|') :
                  movie.actor ? (typeof movie.actor === 'string' ? movie.actor.split('|') : []) : []
            };
          }) || [];
          this.total = response.total || response.data.length;
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

    // 处理分页变化
    handlePageChange(page) {
      this.currentPage = page;
      if (this.selectedType === 'with_comments') {
        this.switchToMoviesWithComments();
      } else {
        this.loadAllMovies();
      }
    },

    // 处理类型选择变化
    handleTypeChange() {
      this.currentPage = 1;
      if (this.selectedType === 'with_comments') {
        this.switchToMoviesWithComments();
      } else {
        this.isShowingCommentsOnly = false;
        this.loadAllMovies();
      }
    },

    // 加载有评论的电影（保留原有方法，兼容逻辑）
    async loadMoviesWithComments() {
      this.loading = true;
      try {
        const response = await request.get(`/movie/movies-with-comments?page=${this.currentPage - 1}&size=${this.pageSize}`);
        if (response && response.code === 200 && response.data) {
          this.movies = response.data.map((movie, index) => {
            const movieId = movie.id || movie.info_id || movie.movieId || index + 1;
            return {
              ...movie,
              id: movieId,
              movieId: movieId,
              movieName: movie.movieName || movie.name || '未知电影',
              type: movie.type || '未知',
              direction: movie.direction || movie.region || '未知',
              rating: movie.rating || movie.movie_rating || movie.movieRating || '暂无评分',
              instruction: movie.instruction || '',
              directors: movie.directors || movie.director || [],
              actors: movie.actors || movie.actor || [],
              directorList: movie.directorString ? movie.directorString.split('|') :
                  movie.director ? (typeof movie.director === 'string' ? movie.director.split('|') : []) : [],
              actorList: movie.actorString ? movie.actorString.split('|') :
                  movie.actor ? (typeof movie.actor === 'string' ? movie.actor.split('|') : []) : []
            };
          }) || [];
          this.total = response.total || response.data.length;
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

    // 处理电影点击事件 - 打开电影详情
    handleMovieClick(movie) {
      this.currentMovie = movie;
      this.movieDetailDialogVisible = true;
    },

    // 格式化导演名称
    getDirectorNames(movie) {
      if (movie.directors && movie.directors.length > 0) {
        return movie.directors.map(director => director.name || director).join('、');
      } else if (movie.directorString) {
        return movie.directorString.split('|').join('、');
      } else if (Array.isArray(movie.directorList) && movie.directorList.length > 0) {
        return movie.directorList.join('、');
      } else if (movie.director) {
        if (typeof movie.director === 'string') {
          const directorStr = movie.director.trim();
          if (directorStr && directorStr !== 'null' && directorStr !== 'undefined') {
            return directorStr.split('|').filter(item => item.trim()).join('、');
          } else {
            return '未知';
          }
        }
        if (Array.isArray(movie.director)) {
          return movie.director.map(director => director.name || director).join('、');
        } else if (movie.director && movie.director !== 'null' && movie.director !== 'undefined') {
          return String(movie.director);
        } else {
          return '未知';
        }
      }
      return '未知';
    },

    // 格式化演员名称
    getActorNames(movie) {
      if (movie.actors && movie.actors.length > 0) {
        return movie.actors.map(actor => actor.name || actor).join('、');
      } else if (movie.actorString) {
        return movie.actorString.split('|').join('、');
      } else if (Array.isArray(movie.actorList) && movie.actorList.length > 0) {
        return movie.actorList.join('、');
      } else if (movie.actor) {
        if (typeof movie.actor === 'string') {
          const actorStr = movie.actor.trim();
          if (actorStr && actorStr !== 'null' && actorStr !== 'undefined') {
            return actorStr.split('|').filter(item => item.trim()).join('、');
          } else {
            return '未知';
          }
        }
        if (Array.isArray(movie.actor)) {
          return movie.actor.map(actor => actor.name || actor).join('、');
        } else if (movie.actor && movie.actor !== 'null' && movie.actor !== 'undefined') {
          return String(movie.actor);
        } else {
          return '未知';
        }
      }
      return '未知';
    },

    // 显示电影评论
    async showMovieComments(movie) {
      this.currentMovie = movie;
      try {
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
      console.log('准备获取电影评论，电影信息:', movie);
      try {
        const movieId = movie.movieId || movie.info_id || movie.id;
        console.log('请求的电影ID:', movieId);
        
        // 处理 movieId 为无效值的情况
        if (!movieId || movieId === null || movieId === undefined || movieId === '' || movieId === 'null' || movieId === 'undefined') {
          this.comments = [];
          this.$nextTick(() => {
            this.commentDialogVisible = true;
            this.$forceUpdate();
            console.log('评论弹窗已打开，评论数量: 0');
          });
          this.$message.warning('电影ID不存在，无法获取评论');
          return;
        }
        
        const response = await request.get(`/movie/movie-comments/${movieId}`);
        console.log('API响应:', response);
        if (response && response.code === 200 && response.data) {
          const processedComments = response.data.map(item => {
            return {
              comment_id: item['id'] || item['comment_id'] || '未知',
              movie_id: item['movieId'] || item['movie_id'] || '未知',
              creator: item['creator'] || '匿名用户',
              content: item['content'] || '暂无评论内容',
              comment_rating: item['rating'] || item['comment_rating'] || '暂无评分',
              comment_time: item['commentTime'] || item['comment_time'] || '未知',
              comment_add_time: item['addTime'] || item['comment_add_time'] || '未知'
            };
          });
          console.log('处理后的评论数据:', processedComments);
          this.comments = processedComments;
          this.$nextTick(() => {
            this.commentDialogVisible = true;
            this.$forceUpdate();
            console.log('评论弹窗已打开，评论数量:', this.comments.length);
          });
          this.$message.success(`获取到 ${this.comments.length} 条评论详情`);
        } else {
          this.comments = [];
          console.error('获取评论失败，响应数据:', response);
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
      // 重置评论表单
      this.resetCommentForm();
    },

    // 关闭电影详情弹窗
    closeMovieDetailDialog() {
      this.movieDetailDialogVisible = false;
      this.currentMovie = null;
      this.comments = [];
    },

    // ========== 新增：评论/评分提交相关方法 ==========
    // 处理评分变化
    handleRatingChange(value) {
      this.userRating = value;
    },

    // 重置评论表单
    resetCommentForm() {
      this.userRating = undefined;
      this.userCommentContent = '';
    },

    // 跳转到登录页（需根据实际路由调整）
    gotoLogin() {
      this.$router.push('/login'); // 假设登录页路由为 /login
    },

    // 提交评论
    async submitComment() {
      // 跳过登录检查，使用模拟用户名
      // if (!this.currentUser) {
      //   this.$message.warning('请先登录');
      //   return;
      // }
      if ((this.userRating === undefined || this.userRating === null) || !this.userCommentContent.trim()) {
        this.$message.warning('请完成评分和评论内容填写');
        return;
      }
      if (this.userRating < 0 || this.userRating > 10) {
        this.$message.error('评分应在0-10分之间');
        return;
      }

      // 验证电影ID
      const movieId = this.currentMovie.movieId || this.currentMovie.info_id || this.currentMovie.id;
      if (!movieId || movieId === null || movieId === undefined || movieId === '' || movieId === 'null' || movieId === 'undefined') {
        this.$message.error('电影ID不能为空或无效');
        return;
      }

      this.commentSubmitting = true;
      try {
        // 格式化时间为 yyyy-MM-dd HH:mm:ss
        const formatDate = (date) => {
          const year = date.getFullYear();
          const month = String(date.getMonth() + 1).padStart(2, '0');
          const day = String(date.getDate()).padStart(2, '0');
          const hours = String(date.getHours()).padStart(2, '0');
          const minutes = String(date.getMinutes()).padStart(2, '0');
          const seconds = String(date.getSeconds()).padStart(2, '0');
          return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        };
        // 使用固定的模拟用户名，确保评论能显示用户名
        const mockUsername = '测试用户';
        const submitData = {
          movieId: movieId, // 电影ID
          creator: mockUsername, // 使用模拟用户名
          rating: this.userRating, // 评分
          content: this.userCommentContent.trim(), // 评论内容
          commentTime: formatDate(new Date()) // 评论时间（与后端格式一致）
        };
        
        console.log('用户信息:', this.currentUser);
        console.log('currentUser是否存在:', !!this.currentUser);
        console.log('currentUser.username:', this.currentUser ? this.currentUser.username : 'undefined');
        console.log('currentUser.name:', this.currentUser ? this.currentUser.name : 'undefined');
        console.log('currentUser.userid:', this.currentUser ? this.currentUser.userid : 'undefined');
        console.log('currentUser.id:', this.currentUser ? this.currentUser.id : 'undefined');
        console.log('currentUser.userId:', this.currentUser ? this.currentUser.userId : 'undefined');
        console.log('提交评论数据:', submitData);
        console.log('电影信息:', this.currentMovie);

        // 调用后端提交评论接口（需根据实际接口调整URL和请求方式）
        const response = await request.post('/movie/comment/submit', submitData);
        if (response && response.code === 200) {
          this.$message.success('评论提交成功！');
          // 重置表单
          this.resetCommentForm();
          // 重新加载评论列表
          await this.showDetailedMovieComments(this.currentMovie);
        } else {
          this.$message.error('评论提交失败：' + (response.msg || '服务器错误'));
        }
      } catch (error) {
        console.error('提交评论失败:', error);
        this.$message.error('评论提交失败，请稍后重试');
      } finally {
        this.commentSubmitting = false;
      }
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
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.ai-recommend-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-recommend-card .el-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-controls {
  display: flex;
  align-items: center;
}

.ai-card-header {
  padding: 20px;
  border-bottom: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 8px 8px 0 0;
}

.header-icon-wrapper {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.header-icon {
  font-size: 24px;
  color: white;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: white;
  letter-spacing: 0.5px;
}

.header-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 400;
}

.movie-card {
  margin-bottom: 25px;
}

.movie-item {
  height: 400px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.movie-content {
  padding: 20px;
  height: calc(100% - 60px);
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
  height: 48px;
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
  height: 48px;
  overflow: hidden;
  flex-grow: 1;
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
  height: 67px;
  flex-grow: 1;
}

.movie-actions {
  display: flex;
  justify-content: space-between;
  padding: 0 20px 20px;
  gap: 10px;
  margin-top: auto;
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
  margin-top: 20px;
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
  min-height: 0;
  padding: 0;
  background: #f8f9fa;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  background: linear-gradient(to bottom, #ffffff 0%, #f8f9fa 100%);
  scroll-behavior: smooth;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #b0b0b0;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.message-avatar:hover {
  transform: scale(1.1);
}

.message-avatar.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-avatar.assistant {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.message-content {
  max-width: calc(100% - 60px);
  margin: 0 12px;
  min-width: 0;
}

.message-bubble {
  margin: 0;
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.message-bubble.user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background: white;
  color: #333;
  border: 1px solid #e8e8e8;
  border-bottom-left-radius: 4px;
}

/* 打字指示器 */
.typing-indicator {
  opacity: 0.7;
}

.typing {
  background: white !important;
  padding: 16px 20px !important;
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite;
}

.typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.input-area {
  padding: 20px;
  border-top: 1px solid #e8e8e8;
  background: white;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.chat-input {
  margin-bottom: 12px;
}

.chat-input .el-textarea__inner {
  border-radius: 12px;
  border: 2px solid #e8e8e8;
  transition: all 0.3s;
  font-size: 14px;
  padding: 12px 16px;
}

.chat-input .el-textarea__inner:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.send-button {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.quick-actions {
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
  background: white;
}

.quick-actions-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.quick-actions-title i {
  color: #667eea;
  font-size: 14px;
}

.quick-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-btn {
  padding: 8px 14px !important;
  font-size: 13px !important;
  border-radius: 20px !important;
  border: 1px solid #e8e8e8 !important;
  background: white !important;
  color: #666 !important;
  transition: all 0.3s !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05) !important;
}

.quick-btn:hover {
  border-color: #667eea !important;
  color: #667eea !important;
  background: #f0f4ff !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.2) !important;
}

.quick-btn i {
  margin-right: 4px;
  font-size: 14px;
}

.movie-detail-content h3 {
  margin-top: 0;
  color: #303133;
  border-bottom: 1px solid #dcdfe6;
  padding-bottom: 15px;
}

.movie-intro {
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
}

/* ========== 新增：评论提交表单样式 ========== */
.comment-submit-form {
  margin-bottom: 20px;
}

.submit-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.submit-card-header {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.submit-form-content {
  padding: 20px;
}

.rating-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  gap: 10px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  width: 80px;
  flex-shrink: 0;
}

.rating-text {
  font-size: 14px;
  color: #666;
  margin-left: 10px;
}

.comment-content-item {
  margin-bottom: 15px;
}

.submit-button-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.login-tip {
  margin-bottom: 20px;
  padding: 15px;
  text-align: center;
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

  .header-controls {
    flex-direction: column;
    align-items: flex-end;
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

  .header-controls {
    width: 100%;
    margin-top: 10px;
  }

  .el-select {
    width: 100%;
  }

  /* 响应式调整评论表单 */
  .rating-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }

  .form-label {
    width: 100%;
    margin-bottom: 5px;
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
