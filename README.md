# 基于Neo4j的知识图谱电影推荐系统

这是一个基于Spring Boot + Neo4j的电影知识图谱推荐系统，结合了知识图谱、协同过滤和内容推荐等多种算法，为用户提供个性化的电影推荐服务。

## 🎬 项目特性

- **知识图谱驱动**: 使用Neo4j构建电影、导演、演员、类型之间的复杂关系网络
- **多算法融合**: 结合协同过滤、内容推荐和知识图谱路径推荐算法
- **AI智能推荐**: 集成自然语言处理，支持通过自然语言描述获取推荐
- **Vue.js现代化前端**: 响应式设计，优雅的用户体验
- **RESTful API**: 完整的API接口设计，前后端分离架构

## 技术栈

- **后端**: Spring Boot 2.7+, Java 8+
- **图数据库**: Neo4j 4.x
- **前端**: Vue.js 3, D3.js (知识图谱可视化)
- **缓存**: Redis
- **文档数据库**: MongoDB (可选)

## 系统架构

```
+-------------------+    +------------------+    +------------------+
|   前端展示层       |    |   后台服务层      |    |   图数据库层      |
|  (Vue.js)        | -> |  (Spring Boot)   | -> |  (Neo4j)         |
+-------------------+    +------------------+    +------------------+
         |                        |                        |
         v                        v                        v
+-------------------+    +------------------+    +------------------+
|   用户交互         |    |   业务逻辑        |    |   知识图谱        |
|  - 电影浏览       |    |  - 推荐算法       |    |  - 电影节点       |
|  - 评分评论       |    |  - 数据管理       |    |  - 人员节点       |
|  - 智能搜索       |    |  - API接口        |    |  - 关系建模       |
+-------------------+    +------------------+    +------------------+
```

## 核心实体模型

### Neo4j图模型设计

#### 节点类型:
- **Movie**: 电影节点 (movieId, movieName, type, direction, rating, instruction)
- **User**: 用户节点 (username)
- **Director**: 导演节点 (name)
- **Actor**: 演员节点 (name)
- **Comment**: 评论节点 (rating, content, creator)

#### 关系类型:
- **USER-POSTED->COMMENT**: 用户发布评论
- **COMMENT-TARGETS->MOVIE**: 评论针对电影
- **USER-RATED->MOVIE**: 用户评分电影
- **MOVIE-DIRECTED_BY->DIRECTOR**: 电影由导演执导
- **MOVIE-STARRED_BY->ACTOR**: 电影由演员主演
- **MOVIE-HAS_COMMENT->COMMENT**: 电影包含评论

## 功能模块

1. **电影信息管理**: 电影信息的增删改查
2. **用户评价系统**: 评论、评分功能
3. **知识图谱可视化**: 基于D3.js的交互式关系图谱展示
4. **智能推荐**: 基于用户行为的个性化推荐
5. **多维度推荐**: 协同过滤、内容推荐、知识图谱推荐
6. **搜索功能**: 电影名称、类型、导演、演员等多维度搜索
7. **AI增强搜索**: 结合自然语言处理的智能搜索
8. **自然语言推荐**: 通过自然语言描述获取个性化推荐

## 推荐算法

### 1. 协同过滤推荐
基于用户行为相似性，找到相似用户喜欢的电影进行推荐

### 2. 内容推荐
基于用户喜欢的电影特征（导演、演员、类型等）推荐相似内容

### 3. 知识图谱路径推荐
通过图遍历算法挖掘用户-评论-电影-导演-演员等多跳关系进行推荐

### 4. 融合推荐
将多种推荐算法结果按权重融合，提供更精准的推荐

## 快速开始

### 环境要求

- Java 8+
- Maven 3.6+
- Neo4j 4.0+ (bolt://localhost:7687)
- Redis (localhost:6379)

### 配置说明

在 `application.properties` 中配置Neo4j连接:

```properties
# Neo4j Configuration
spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=your-neo4j-password

# Dify AI Configuration (可选)
dify.api.url=${DIFY_API_URL:https://api.dify.ai/v1}
dify.api.key=${DIFY_API_KEY:your-dify-api-key-here}
```

### 启动步骤

1. 启动Neo4j数据库
2. 配置数据库连接信息
3. 编译并启动应用
   ```bash
   mvn spring-boot:run
   ```
4. 访问系统: `http://localhost:8080`

## API接口

- `GET /api/movies` - 获取所有电影
- `GET /api/movies/{id}` - 根据ID获取电影
- `GET /api/recommendations/user/{userId}` - 获取用户推荐
- `GET /api/recommendations/knowledge-graph/{movieId}` - 知识图谱推荐
- `GET /api/ai/recommendations?query={query}` - AI推荐

## 知识图谱构建

系统从数据源导入电影、导演、演员等信息，构建完整的知识图谱：

1. 创建电影节点
2. 创建人员节点（导演、演员）
3. 建立电影-人员关系
4. 建立用户-电影交互关系
5. 构建多跳关联路径

## 推荐策略

### 个性化推荐流程

1. 收集用户行为数据（评分、评论）
2. 分析用户偏好特征
3. 应用多算法推荐策略
4. 融合推荐结果
5. 返回个性化推荐列表

### 算法权重配置

- 协同过滤: 40%
- 内容推荐: 30%
- 知识图谱: 30%

## 部署说明

1. 确保Neo4j服务已启动
2. 配置环境变量
3. 打包应用: `mvn package`
4. 运行应用: `java -jar target/demo-1.0-SNAPSHOT.jar`

## 扩展功能

- 支持更多实体类型（制片公司、奖项等）
- 实现更复杂的图神经网络算法
- 集成实时学习机制
- 支持多语言自然语言处理

## 🚀 前后端联调指南

### 已实现的联调功能
1. **电影数据获取** - Vue前端通过REST API获取后端电影数据
2. **知识图谱可视化** - 后端提供图谱数据，前端D3.js动态渲染
3. **AI推荐接口** - 完整的推荐流程联调
4. **用户交互** - 评分、搜索、详情查看等交互功能

### 访问入口
- **系统入口页**: http://localhost:8080/index-vue.html
- **Vue版本**: http://localhost:8080/vue-app.html
- **AngularJS版本**: http://localhost:8080/index.html

### API接口测试
系统启动后，可以测试以下核心接口：

| 接口 | 路径 | 描述 |
|------|------|------|
| 电影列表 | `GET /api/movie-infos` | 获取所有电影数据 |
| 知识图谱统计 | `GET /api/knowledge-graph/stats` | 获取图谱统计信息 |
| AI推荐 | `GET /api/ai/recommendations?userId=test` | 测试AI推荐功能 |
| 图谱可视化 | `GET /api/knowledge-graph/full` | 获取完整图谱数据 |

### 联调验证步骤
1. 启动后端服务：`mvn spring-boot:run`
2. 访问系统入口页查看版本选择
3. 进入Vue版本测试各项功能
4. 验证数据加载和交互功能

## 📁 新增文件结构

```
src/main/resources/static/
├── vue-app.html              # Vue 3版本前端
├── index-vue.html           # 系统入口页面
└── css/
    └── style.css            # 样式文件

src/main/java/org/example/
├── controller/
│   ├── KnowledgeGraphController.java    # 知识图谱API
│   └── AIRecommendationController.java  # AI推荐API
└── config/
    └── DataInitializer.java             # 数据初始化
```

## 🧪 测试数据

系统启动时会自动插入20部经典电影测试数据，包括：
- 肖申克的救赎 (9.7分)
- 阿甘正传 (9.5分)
- 泰坦尼克号 (9.4分)
- 盗梦空间 (9.3分)
- 星际穿越 (9.2分)
- 等15部其他经典电影

## 🔧 技术栈扩展

### 前端技术栈
- **Vue 3** - 现代化前端框架
- **D3.js** - 数据可视化引擎  
- **Bootstrap 5** - UI组件库
- **Axios** - HTTP客户端

### 后端新增功能
- **知识图谱构建算法**
- **实体关系计算**
- **可视化数据接口**
- **AI推荐优化**

## 贡献

欢迎提交Issue和Pull Request来改进项目。

## 许可证

MIT