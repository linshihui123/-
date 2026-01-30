<template>
  <div id="app">
    <el-container style="min-height: 100vh;">
      <el-header class="header-container">
        <div class="header-content">
          <div class="logo-section">
            <i class="el-icon-film" style="font-size: 24px; margin-right: 10px;"></i>
            <span class="logo-text gradient-text">电影推荐系统</span>
          </div>
          <div class="nav-section">
            <template v-if="!currentUser">
              <el-button
                  type="primary"
                  plain
                  @click="showLoginDialog"
                  class="auth-button"
              >
                <i class="el-icon-user"></i> 登录/注册
              </el-button>
            </template>
            <template v-else>
              <el-button-group class="nav-buttons">
                <!-- 移除推荐按钮 -->
                <el-button
                    type="primary"
                    plain
                    :class="{ 'nav-active': activeTab === 'browse' }"
                    @click="switchTab('browse')"
                >
                  <i class="el-icon-search"></i> 浏览
                </el-button>
                <el-button
                    type="primary"
                    plain
                    :class="{ 'nav-active': activeTab === 'collaborative' }"
                    @click="switchTab('collaborative')"
                >
                  <i class="el-icon-user"></i> 推荐
                </el-button>
                <el-button
                    type="primary"
                    plain
                    :class="{ 'nav-active': activeTab === 'graph' }"
                    @click="switchTab('graph')"
                >
                  <i class="el-icon-s-data"></i> 知识图谱
                </el-button>
              </el-button-group>
              <span class="user-welcome">欢迎，{{ currentUser.username }}</span>

              <el-button
                  type="info"
                  plain
                  @click="logout"
                  class="logout-button"
              >
                <i class="el-icon-switch-button"></i> 退出
              </el-button>
            </template>
          </div>
        </div>
      </el-header>
      <el-main>
        <!-- 用户未登录时直接显示浏览界面 -->
        <div v-if="!currentUser && !activeTab" class="browse-container">
          <div class="layout-wrapper">
            <div class="main-content">
              <MovieBrowse
                  :movies="currentMovies"
                  :loading="loading"
                  :show-title="true"
                  @movie-click="handleMovieClick"
              />
            </div>
          </div>
        </div>

        <!-- 登录成功后的首页，只显示欢迎信息和功能介绍 -->
        <div v-if="currentUser && !activeTab" class="dashboard-container">
          <div class="dashboard-header slide-in">
            <h2>欢迎回来，{{ currentUser.username }}！</h2>
            <p>探索电影世界的无限精彩</p>
          </div>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-card class="dashboard-card" shadow="hover">
                <div slot="header" class="dashboard-card-header">
                  <span><i class="el-icon-menu"></i> 功能导航</span>
                </div>
                <div class="features-grid">
                  <!-- 移除个性化推荐卡片 -->
                  <el-card class="feature-card" @click.native="switchTab('browse')">
                    <div class="feature-content">
                      <i class="el-icon-search feature-icon" style="color: #409eff;"></i>
                      <h3>电影浏览</h3>
                      <p>按类型、评分等条件浏览电影</p>
                    </div>
                  </el-card>
                  <el-card class="feature-card" @click.native="switchTab('collaborative')">
                    <div class="feature-content">
                      <i class="el-icon-user feature-icon" style="color: #e6a23c;"></i>
                      <h3>协同过滤推荐</h3>
                      <p>基于用户评分相似性的推荐</p>
                    </div>
                  </el-card>
                  <el-card class="feature-card" @click.native="switchTab('graph')">
                    <div class="feature-content">
                      <i class="el-icon-s-data feature-icon" style="color: #67c23a;"></i>
                      <h3>知识图谱</h3>
                      <p>探索电影、导演、演员之间的关系</p>
                    </div>
                  </el-card>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 浏览页面 -->
        <div v-if="activeTab === 'browse'" class="browse-container">
          <div class="layout-wrapper">
            <div class="main-content">
              <MovieBrowse
                  :movies="currentMovies"
                  :loading="loading"
                  :show-title="true"
                  @movie-click="handleMovieClick"
              />
            </div>
          </div>
        </div>

        <!-- 协同过滤推荐页面 -->
        <div v-if="activeTab === 'collaborative'" class="browse-container">
          <div class="layout-wrapper">
            <div class="main-content">
              <CollaborativeFilteringRecommend
                  @movie-click="handleMovieClick"
              />
            </div>
          </div>
        </div>

        <!-- 知识图谱页面 -->
        <div v-if="activeTab === 'graph'">
          <KnowledgeGraph :current-user="currentUser" />
        </div>

        <!-- 电影详情弹窗 -->
        <el-dialog title="电影详情" :visible.sync="detailDialogVisible" width="60%">
          <div v-if="selectedMovie">
            <h3>{{ selectedMovie.movieName }}</h3>
            <p><strong>类型：</strong>{{ selectedMovie.type || '未知' }}</p>
            <p><strong>地区：</strong>{{ selectedMovie.direction || '未知' }}</p>
            <p><strong>评分：</strong>{{ selectedMovie.rating || '暂无评分' }}</p>
            <p><strong>导演：</strong>{{ getDirectorNames(selectedMovie.directors) }}</p>
            <p><strong>演员：</strong>{{ getActorNames(selectedMovie.actors) }}</p>
            <p><strong>简介：</strong>{{ selectedMovie.instruction || '暂无介绍' }}</p>
          </div>
        </el-dialog>

        <!-- 用户认证弹窗 -->
        <el-dialog
            :title="authDialogTitle"
            :visible.sync="authDialogVisible"
            width="40%"
            :close-on-click-modal="false"
            :close-on-press-escape="false"
        >
          <UserAuth @login-success="handleLoginSuccess" />
        </el-dialog>
      </el-main>
      <el-footer class="footer-container">
        <div class="footer-content">
          <div class="footer-text">
            <i class="el-icon-film" style="margin-right: 8px;"></i>
            电影推荐系统 ©2025
          </div>
          <div class="footer-links">
            <span class="footer-link">关于我们</span>
            <span class="footer-link">隐私政策</span>
            <span class="footer-link">帮助中心</span>
          </div>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script>
import MovieList from './components/MovieList.vue'
import MovieBrowse from './components/MovieBrowse.vue'
import UserAuth from './components/UserAuth.vue'
import KnowledgeGraph from './components/KnowledgeGraph.vue'
import CollaborativeFilteringRecommend from './components/CollaborativeFilteringRecommend.vue'
// 移除推荐相关接口导入
import { getMovieDetail } from './api/recommend'
import axios from 'axios'

export default {
  name: 'App',
  components: {
    MovieList,
    MovieBrowse,
    UserAuth,
    KnowledgeGraph,
    CollaborativeFilteringRecommend
  },
  data() {
    return {
      // 移除推荐电影列表
      allMovies: [],
      currentMovies: [], // 当前显示的电影列表
      loading: false,
      detailDialogVisible: false,
      selectedMovie: null,
      showTitle: true,
      authDialogVisible: false,
      currentUser: null,
      currentUserId: null,
      activeTab: null, // 默认为null，显示首页
      // 知识图谱预览相关数据
      graphPreviewNodes: [],
      graphStats: {
        movies: 0,
        directors: 0,
        actors: 0
      },
      // 移除features中的智能推荐项
      features: [
        {
          id: 2,
          icon: 'el-icon-s-data',
          title: '知识图谱',
          description: '电影关系可视化展示',
          color: '#409eff'
        },
        {
          id: 3,
          icon: 'el-icon-search',
          title: '智能搜索',
          description: '快速找到您想看的电影',
          color: '#67c23a'
        }
      ]
    }
  },
  computed: {
    authDialogTitle() {
      return this.currentUser ? '用户中心' : '登录/注册';
    }
  },
  created() {
    // 尝试从本地存储恢复用户会话
    this.restoreUserSession();
    // 如果用户未登录，默认显示浏览页面；如果已登录，默认显示首页
    if (!this.currentUser) {
      this.activeTab = 'browse';
      this.handleGetAllMovies({ page: 0, size: 20 });
    }
  },
  methods: {
    // 恢复用户会话
    restoreUserSession() {
      const userStr = localStorage.getItem('user');
      const token = localStorage.getItem('token');
      if (userStr && userStr !== 'undefined' && userStr !== 'null' && token) {
        try {
          this.currentUser = JSON.parse(userStr);
          this.currentUserId = this.currentUser.userid || this.currentUser.id || this.currentUser.userId; // 尝试多个可能的字段名
        } catch (e) {
          console.error('恢复用户会话失败', e);
          this.clearUserSession();
        }
      }
    },

    // 清除用户会话
    clearUserSession() {
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      this.currentUser = null;
      this.currentUserId = null;
      this.activeTab = null; // 返回首页
    },

    // 显示登录对话框
    showLoginDialog() {
      this.authDialogVisible = true;
    },

    // 处理登录成功事件
    async handleLoginSuccess(responseData) {
      console.log('登录回调数据:', responseData); // 调试日志

      // 后端返回的是Result<User>对象，直接从responseData.data中提取用户信息
      let user = null;
      let token = null;

      // 检查是否是标准的Result对象格式
      if (responseData && typeof responseData === 'object' && responseData.data) {
        user = responseData.data;
        token = responseData.token;
      } else if (responseData && typeof responseData === 'object') {
        // 如果不是标准Result对象，直接使用整个对象作为用户数据
        user = responseData;
      }

      if (!user) {
        console.error('登录回调数据格式错误，无法提取用户信息:', responseData);
        return;
      }

      this.currentUser = user;
      // 根据后端User实体类，使用userid字段
      this.currentUserId = user.userid || user.id || user.userId;

      // 确保ID存在
      if (!this.currentUserId) {
        console.warn('用户ID未找到，尝试从其他字段提取');
        this.currentUserId = user.userid || user.id || user.userId;
      }

      this.authDialogVisible = false; // 登录成功后自动关闭登录对话框
      this.activeTab = null; // 登录成功后显示首页

      // 保存用户信息到本地存储
      localStorage.setItem('user', JSON.stringify(user));
      if (token) {
        localStorage.setItem('token', token);
      } else if (responseData.token) {
        localStorage.setItem('token', responseData.token);
      }

      // 使用$nextTick确保DOM更新后再显示消息，避免UI闪烁
      this.$nextTick(() => {
        // 显示欢迎消息，优先使用username字段
        const username = user.username || user.usrename || '用户';
        this.$message.success(`欢迎回来，${username}！`);
      });

      // 移除获取推荐电影的调用
      // 获取知识图谱预览数据
      this.loadGraphPreviewData();

      // 强制更新视图
      this.$forceUpdate();
    },

    // 加载知识图谱预览数据
    async loadGraphPreviewData() {
      try {
        // 从后端API获取知识图谱数据
        const response = await axios.get(`/api/kg/graph-data?movieCount=${this.graphMovieCount}`);
        if (response.data && response.data.code === 200 && response.data.data) {
          const graphData = response.data.data;
          const nodes = graphData.nodes || [];

          // 选取前10个节点作为预览
          this.graphPreviewNodes = nodes.slice(0, 10).map(item => {
            let color;
            switch(item.type) {
              case 'movie':
                color = '#5B8FF9';
                break;
              case 'director':
                color = '#5AD8A6';
                break;
              case 'actor':
                color = '#5D7092';
                break;
              default:
                color = '#CDDDFD';
            }

            return {
              id: item.id,
              name: item.name,
              type: item.type,
              color: color
            };
          });

          // 统计各类节点数量
          this.graphStats = {
            movies: nodes.filter(node => node.type === 'movie').length,
            directors: nodes.filter(node => node.type === 'director').length,
            actors: nodes.filter(node => node.type === 'actor').length
          };
        } else {
          // 如果API调用失败，设置默认值
          this.graphPreviewNodes = [];
          this.graphStats = { movies: 0, directors: 0, actors: 0 };
        }
      } catch (error) {
        console.error('获取知识图谱预览数据失败:', error);
        // 设置默认值
        this.graphPreviewNodes = [];
        this.graphStats = { movies: 0, directors: 0, actors: 0 };
      }
    },

    // 获取节点类型名称
    getNodeTypeName(type) {
      const typeMap = {
        'movie': '电影',
        'director': '导演',
        'actor': '演员',
        'genre': '类型',
        'region': '地区'
      };
      return typeMap[type] || type;
    },

    // 退出登录
    logout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.clearUserSession();
        this.$message.success('已退出登录');
      }).catch(() => {
        // 取消退出
      });
    },

    // 切换标签页
    switchTab(tab) {
      this.activeTab = tab;
    },

    // 移除handleGetRecommend方法

    // 获取全部电影
    async handleGetAllMovies(params) {
      this.loading = true;
      this.showTitle = false; // 不显示"推荐结果"标题
      try {
        const response = await axios.get(`/api/movie/list?page=${params.page || 0}&size=${params.size || 20}`);
        if (response.data && response.data.data) {
          this.allMovies = response.data.data || [];
          this.currentMovies = this.allMovies;
        } else {
          this.allMovies = [];
          this.currentMovies = [];
        }
      } catch (error) {
        this.$message.error('获取电影列表失败！')
        console.error(error)
        this.allMovies = [];
        this.currentMovies = [];
      } finally {
        this.loading = false
      }
    },

    // 显示全部电影
    showAllMovies() {
      this.currentMovies = this.allMovies;
      this.showTitle = false; // 不显示"推荐结果"标题
    },

    // 处理电影点击事件
    handleMovieClick(movie) {
      this.selectedMovie = movie;
      this.detailDialogVisible = true;
    },

    // 格式化导演名称
    getDirectorNames(directors) {
      if (!directors || directors.length === 0) return '未知';
      return directors.map(item => item.name).join('、');
    },

    // 格式化演员名称
    getActorNames(actors) {
      if (!actors || actors.length === 0) return '未知';
      return actors.map(item => item.name).join('、');
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}
.el-header, .el-footer {
  background-color: #1989fa;
  color: #fff;
}
.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

/* 浏览页面布局 */
.browse-container {
  max-width: 1400px;
  margin: 0 auto;
}

.recommend-sidebar {
  position: sticky;
  top: 20px;
  height: fit-content;
}

/* 首页英雄区域样式 */
.home-hero {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="2" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
}

.hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 1000px;
  width: 100%;
}

.hero-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.hero-header {
  margin-bottom: 40px;
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 15px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.2rem;
  color: #666;
  line-height: 1.6;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
  margin-bottom: 40px;
}

.feature-card {
  padding: 30px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  text-align: center;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.15);
}

.feature-icon {
  font-size: 3rem;
  margin-bottom: 20px;
  transition: transform 0.3s ease;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1);
}

.feature-card h3 {
  font-size: 1.3rem;
  font-weight: 600;
  margin-bottom: 15px;
  color: #333;
}

.feature-card p {
  color: #666;
  line-height: 1.6;
}

.feature-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-button {
  padding: 15px 40px;
  font-size: 1.1rem;
  border-radius: 30px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  font-weight: 600;
}

/* 用户仪表盘样式 */
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.dashboard-header {
  text-align: center;
  margin-bottom: 40px;
}

.dashboard-header h2 {
  font-size: 2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}

.dashboard-header p {
  font-size: 1.1rem;
  color: #666;
}

.dashboard-card {
  border-radius: 15px;
  overflow: hidden;
}

.dashboard-card-header {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa, #e4e8f0);
}

.view-full-graph-btn {
  width: 100%;
  border-radius: 8px;
  margin-top: 15px;
}

/* 导航按钮样式 */
.nav-active {
  background: linear-gradient(135deg, #409EFF, #337ecc) !important;
  border-color: #409EFF !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* 头部样式 */
.header-container {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 0 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  max-width: 1200px;
  margin: 0 auto;
}

.logo-section {
  display: flex;
  align-items: center;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
  background: linear-gradient(135deg, #ff6b6b, #feca57);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-section {
  display: flex;
  align-items: center;
  gap: 15px;
}

.nav-buttons .el-button {
  border-radius: 20px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.nav-buttons .el-button:hover {
  transform: translateY(-1px);
}

.user-welcome {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  font-weight: 500;
}

.auth-button,
.logout-button {
  border-radius: 20px;
  padding: 10px 20px;
  font-weight: 500;
}

/* 底部样式 */
.footer-container {
  background: linear-gradient(135deg, #2c3e50, #34495e);
  color: #fff;
  padding: 20px;
  margin-top: auto;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.footer-text {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.footer-links {
  display: flex;
  gap: 20px;
}

.footer-link {
  cursor: pointer;
  font-size: 12px;
  opacity: 0.7;
  transition: opacity 0.3s ease;
}

.footer-link:hover {
  opacity: 1;
  text-decoration: underline;
}

/* 知识图谱预览样式 */
.graph-preview-container {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 10px;
}

.graph-node-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-bottom: 1px solid #eee;
}

.node-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: bold;
  margin-right: 10px;
  flex-shrink: 0;
}

.node-info {
  flex: 1;
  overflow: hidden;
}

.node-name {
  font-size: 14px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.node-type {
  font-size: 12px;
  color: #666;
}

.no-data {
  text-align: center;
  color: #999;
  padding: 20px;
}

.graph-stats-preview {
  display: flex;
  justify-content: space-around;
  margin-bottom: 10px;
}

/* 浏览页面布局 */
.browse-container,
.recommend-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0;
}

.layout-wrapper {
  display: flex;
  gap: 0;
  width: 100%;
}

.main-content {
  flex: 1;
  min-width: 0; /* 防止内容溢出 */
  padding-right: 0;
}

.sidebar-right {
  width: 320px;
  flex-shrink: 0;
  margin-left: 0;
  border-left: 1px solid #e0e0e0;
}

.recommend-sidebar {
  position: sticky;
  top: 10px;
  height: fit-content;
  margin: 0;
  padding: 0;
  border-radius: 0;
}

/* 确保AI对话组件紧贴右侧 */
.sidebar-right .ai-chat-recommend-container {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}

/* 移除AI组件的内部边距 */
.sidebar-right .el-card {
  margin: 0;
  border-radius: 0;
  border: none;
}

.sidebar-right .el-card__header {
  padding: 15px 20px;
  border-bottom: 1px solid #e0e0e0;
}

.sidebar-right .el-card__body {
  padding: 15px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .sidebar-right {
    width: 280px;
  }
}

@media (max-width: 768px) {
  .layout-wrapper {
    flex-direction: column;
  }

  .sidebar-right {
    width: 100%;
    border-left: none;
    border-top: 1px solid #e0e0e0;
  }

  .recommend-sidebar {
    position: static;
  }
}
</style>