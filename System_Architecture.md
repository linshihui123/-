# 电影推荐系统架构设计

## 系统概述
本系统是一个综合性的电影推荐平台，结合了离线推荐和实时推荐技术，为用户提供个性化的电影推荐服务。系统采用微服务架构设计，主要包含前端展示、后台服务、算法设计与平台部署等多模块。

## 整体架构图

```
+-------------------+    +------------------+    +------------------+
|   前端展示层       |    |   后台服务层      |    |   数据存储层      |
|  (AngularJS2)    | -> |  (Spring Boot)   | -> |  (MySQL/MongoDB) |
+-------------------+    +------------------+    |      (Redis)     |
                                                 +------------------+
                                                        |
                                                        v
                                            +-----------------------+
                                            |   推荐算法层           |
                                            | (Spark/ALS算法)       |
                                            +-----------------------+
```

## 技术栈

### 前端技术
- **AngularJS2**: 用于构建用户界面，提供交互式电影浏览和推荐展示

### 后端技术
- **JavaEE**: 企业级应用开发标准
- **Spring Framework**: 
  - Spring Boot: 快速开发框架
  - Spring MVC: Web层框架
  - Spring Data JPA: 数据访问层
  - Spring Data MongoDB: MongoDB数据访问
  - Spring Data Redis: Redis缓存访问
- **Tomcat**: 应用服务器

### 数据存储
- **MySQL**: 主关系型数据库，存储电影基础信息、评论等结构化数据
- **MongoDB**: NoSQL数据库，存储业务数据和非结构化信息
- **Redis**: 缓存数据库，支持实时推荐系统的高效数据读取

### 推荐算法
- **离线推荐**:
  - SparkCore: 大数据处理引擎
  - SparkMLlib: 机器学习库
  - ALS算法: 矩阵分解算法，生成用户推荐矩阵和影片相似度矩阵

- **实时推荐**:
  - Flume: 日志收集系统
  - Kafka: 分布式消息队列
  - Spark Streaming: 实时流处理框架
  - 对用户评分数据进行实时采集、传输和处理

## 数据库设计

### MySQL 数据库表结构

#### 评论表 (comment)
```sql
CREATE TABLE `comment` (
  `ID` int(11) DEFAULT NULL,
  `TIME` time DEFAULT NULL,
  `MOVIEID` int(11) DEFAULT NULL,
  `RATING` int(11) DEFAULT NULL,
  `CONTENT` varchar(255) DEFAULT NULL,
  `CREATOR` varchar(255) DEFAULT NULL,
  `ADD_TIME` time DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 电影基础信息表 (movie)
```sql
CREATE TABLE `movie` (
  `ID` int(11) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `ADD_TIME` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 电影详细信息表 (movie_info)
```sql
CREATE TABLE `movie_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键，唯一标识电影记录',
  `type` varchar(22) DEFAULT NULL COMMENT '类型',
  `actor` varchar(242) DEFAULT NULL COMMENT '主演',
  `direction` varchar(24) DEFAULT NULL COMMENT '地区',
  `director` varchar(287) DEFAULT NULL COMMENT '导演',
  `characters` varchar(22) DEFAULT NULL COMMENT '特色',
  `rating` float DEFAULT NULL COMMENT '评分',
  `movie_name` varchar(77) DEFAULT NULL COMMENT '电影名',
  `instruction` varchar(1000) NOT NULL DEFAULT '暂无简介' COMMENT '豆瓣电影真实简介',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据插入时间',
  PRIMARY KEY (`id`),
  KEY `idx_douban_movie_name` (`movie_name`) COMMENT '电影名索引，加速名称查询',
  KEY `idx_douban_movie_rating` (`rating`) COMMENT '评分索引，加速评分筛选',
  KEY `idx_movie_name` (`movie_name`)
) ENGINE=InnoDB AUTO_INCREMENT=32768 DEFAULT CHARSET=utf8mb4 COMMENT='豆瓣电影数据表（来源：movie_10.csv）';
```

## 系统功能模块

### 1. 电影信息管理模块
- 电影信息的增删改查
- 电影分类、搜索功能
- 电影详情展示

### 2. 用户评价模块
- 用户评论功能
- 用户评分系统
- 评价内容管理

### 3. 推荐算法模块
- **离线推荐**:
  - 定期执行ALS算法，生成用户-电影评分矩阵
  - 计算电影间相似度矩阵
  - 预计算推荐结果存储到缓存

- **实时推荐**:
  - 监听用户行为数据流
  - 实时更新用户偏好
  - 动态调整推荐结果

### 4. 推荐结果展示模块
- 融合离线、实时及内容推荐结果
- 个性化推荐列表展示
- 推荐理由说明

## 推荐算法实现

### 离线推荐算法
1. **数据预处理**: 清洗和转换历史评分数据
2. **ALS模型训练**: 使用Spark MLlib的ALS算法训练模型
3. **推荐生成**: 为每个用户生成推荐电影列表
4. **结果存储**: 将推荐结果存储到Redis缓存

### 实时推荐算法
1. **数据采集**: 通过Flume收集用户行为日志
2. **消息传输**: 使用Kafka作为消息中间件
3. **流处理**: Spark Streaming实时处理用户评分数据
4. **模型更新**: 动态更新用户偏好向量
5. **推荐调整**: 根据最新行为调整推荐结果

## 系统特点

### 1. 高性能
- Redis缓存支持快速数据读取
- 索引优化提升查询效率
- 分布式处理支持大数据量

### 2. 高可用性
- 多数据源保障数据可靠性
- 推荐算法容错设计
- 微服务架构提高系统稳定性

### 3. 个性化
- 混合推荐算法提升推荐精度
- 实时反馈机制优化用户体验
- 多维度特征融合

### 4. 可扩展性
- 模块化设计便于功能扩展
- 支持新增推荐算法
- 水平扩展支持更大用户量

## 部署架构

### 开发环境
- IDE: IntelliJ IDEA
- 构建工具: Maven
- 版本控制: Git

### 生产环境
- 应用服务器: Tomcat
- 数据库集群: MySQL主从复制
- 缓存集群: Redis Cluster
- 大数据处理: Spark集群
- 消息队列: Kafka集群

## 系统优势

1. **算法融合**: 结合离线和实时推荐，确保推荐数据的及时更新与准确性
2. **数据融合**: 结合MongoDB与ElasticSearch中的数据，实现多源数据推荐
3. **功能完整**: 提供电影信息查询、评分、标签等多种业务服务
4. **体验优化**: 增强用户互动体验，提升用户满意度