// com/example/service/MovieRecommendationService.java
package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.*;
import org.example.repository.MovieRepository;
import org.example.repository.CommentRepository;
import org.example.response.Result;
import org.example.response.ResultCodeEnum;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class MovieRecommendationService {

    @Autowired
    private Session neo4jSession;
    
    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private CommentRepository commentRepository;

    // 配置注入（替代硬编码）
    @Value("${recommend.cf.topN}")
    private Integer cfTopN;
    @Value("${recommend.content.topN}")
    private Integer contentTopN;
    @Value("${recommend.kg.topN}")
    private Integer kgTopN;
    @Value("${recommend.liked.rating.threshold}")
    private Integer likedRatingThreshold;
    @Value("${recommend.weight.cf}")
    private Double cfWeight;
    @Value("${recommend.weight.content}")
    private Double contentWeight;
    @Value("${recommend.weight.kg}")
    private Double kgWeight;

    /**
     * 增强版推荐入口（支持意图驱动）
     */
    public Result<List<MovieNode>> recommendMovies(Integer userId, int topN, RecommendIntent intent) {
        // 参数校验
        if (userId == null || userId <= 0 || topN <= 0 || topN > 50) {
            log.error("推荐参数异常：userId={}, topN={}", userId, topN);
            return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "参数错误：用户ID需为正整数，推荐数量1-50");
        }

        try {
            // 1. 基础推荐结果
            List<MovieNode> cfRecommend = collaborativeFilteringRecommend(userId);
            List<MovieNode> contentRecommend = contentBasedRecommend(userId);
            List<MovieNode> kgRecommend = knowledgeGraphPathRecommend(userId);

            // 2. 意图驱动过滤（新增）
            if (intent != null && !"unknown".equals(intent.getIntentType())) {
                cfRecommend = filterByIntent(cfRecommend, intent);
                contentRecommend = filterByIntent(contentRecommend, intent);
                kgRecommend = filterByIntent(kgRecommend, intent);
            }

            // 3. 融合结果（加权投票）
            Map<Integer, Double> movieScoreMap = new HashMap<>();
            addToScoreMap(cfRecommend, movieScoreMap, cfWeight);
            addToScoreMap(contentRecommend, movieScoreMap, contentWeight);
            addToScoreMap(kgRecommend, movieScoreMap, kgWeight);

            // 4. 排序取TopN + 兜底处理
            List<MovieNode> finalRecommend = movieScoreMap.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(topN)
                    .map(entry -> {
                        Filter filter = new Filter("infoId", ComparisonOperator.EQUALS, entry.getKey());
                        Iterable<MovieNode> iterable = neo4jSession.loadAll(MovieNode.class, filter);
                        return iterable.iterator().hasNext() ? iterable.iterator().next() : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 兜底：无推荐结果时返回高评分热门电影
            if (CollectionUtils.isEmpty(finalRecommend)) {
                finalRecommend = getDefaultHighRatingMovies(topN);
                log.warn("用户{}无个性化推荐，返回默认热门电影", userId);
            }

            return Result.success(finalRecommend);
        } catch (Exception e) {
            log.error("推荐失败：userId={}", userId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "推荐服务异常，请重试");
        }
    }

    /**
     * 获取所有电影列表（分页支持）
     */
    public Result<List<MovieNode>> getAllMovies(int page, int size) {
        try {
            int skip = page * size;

            // 获取所有电影（不只是有评论的电影）
            List<MovieNode> allMovies = movieRepository.findOtherAllMovies(skip, size);
            
            // 获取所有电影总数
            long total = getTotalMovieCount();
            
            // 检查allMovies是否为null
            if (allMovies == null) {
                allMovies = new ArrayList<>();
            }
            
            // 使用专门的方法来设置分页数据
            return Result.successWithTotal(allMovies, total);
        } catch (Exception e) {
            log.error("获取所有电影列表失败：page={}, size={}", page, size, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取所有电影列表失败");
        }
    }
    
    /**
     * 获取电影总数
     */
    private long getTotalMovieCount() {
        String cypher = "MATCH (m:Movie) RETURN COUNT(m) as count";
        Iterable<Map<String, Object>> result = neo4jSession.query(cypher, Collections.emptyMap());
        if (result.iterator().hasNext()) {
            return ((Number) result.iterator().next().get("count")).longValue();
        }
        return 0;
    }

    /**
     * 根据ID获取电影详情
     */
    public Result<MovieNode> getMovieById(Long id) {
        try {
            Optional<MovieNode> movieOpt = movieRepository.findById(id);

            if (!movieOpt.isPresent()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "电影不存在");
            }

            return Result.success(movieOpt.get());
        } catch (Exception e) {
            log.error("获取电影详情失败：id={}", id, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影详情失败");
        }
    }

    /**
     * 根据电影ID获取电影详情（通过movie_id字段）
     */
    public Result<MovieNode> getMovieByMovieId(Integer movieId) {
        try {
            Optional<MovieNode> movieOpt = movieRepository.findByInfoId(movieId);

            if (!movieOpt.isPresent()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "电影不存在");
            }

            return Result.success(movieOpt.get());
        } catch (Exception e) {
            log.error("获取电影详情失败：movieId={}", movieId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影详情失败");
        }
    }

    /**
     * 搜索电影（按名称、类型、导演、演员等）
     */
    public Result<List<MovieNode>> searchMovies(String keyword, String type, String director, String actor, int page, int size) {
        try {
            int skip = page * size;
            
            // 如果只有关键词，使用名称搜索
            if (keyword != null && !keyword.trim().isEmpty() && 
                (type == null || type.trim().isEmpty()) && 
                (director == null || director.trim().isEmpty()) && 
                (actor == null || actor.trim().isEmpty())) {
                
                List<MovieNode> movies = movieRepository.findByMovieNameContaining(keyword);
                return Result.success(movies);
            }
            
            // 如果只有类型，按类型搜索
            if (type != null && !type.trim().isEmpty() && 
                (keyword == null || keyword.trim().isEmpty()) && 
                (director == null || director.trim().isEmpty()) && 
                (actor == null || actor.trim().isEmpty())) {
                
                List<MovieNode> movies = movieRepository.findByType(type);
                return Result.success(movies);
            }
            
            // 如果只有导演，按导演搜索
            if (director != null && !director.trim().isEmpty() && 
                (keyword == null || keyword.trim().isEmpty()) && 
                (type == null || type.trim().isEmpty()) && 
                (actor == null || actor.trim().isEmpty())) {
                
                List<MovieNode> movies = movieRepository.findByDirectorName(director);
                return Result.success(movies);
            }
            
            // 如果只有演员，按演员搜索
            if (actor != null && !actor.trim().isEmpty() && 
                (keyword == null || keyword.trim().isEmpty()) && 
                (type == null || type.trim().isEmpty()) && 
                (director == null || director.trim().isEmpty())) {
                
                List<MovieNode> movies = movieRepository.findByActorName(actor);
                return Result.success(movies);
            }
            
            // 使用更通用的搜索方法
            List<MovieNode> movies = movieRepository.searchMovies(
                keyword != null && !keyword.trim().isEmpty() ? keyword : null,
                type != null && !type.trim().isEmpty() ? type : null,
                skip, size);
                
            return Result.success(movies);
        } catch (Exception e) {
            log.error("搜索电影失败：keyword={}, type={}, director={}, actor={}", keyword, type, director, actor, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "搜索电影失败");
        }
    }

    /**
     * 获取热门电影（高评分电影）
     */
    public Result<List<MovieNode>> getTopRatedMovies(int count) {
        try {
            List<MovieNode> movies = movieRepository.findTopRatedMovies(count);

            return Result.success(movies);
        } catch (Exception e) {
            log.error("获取热门电影失败：count={}", count, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取热门电影失败");
        }
    }

    /**
     * 获取最新电影
     */
    public Result<List<MovieNode>> getLatestMovies(int count) {
        // 由于MovieNode中没有日期字段，我们使用评分和ID作为替代指标
        try {
            // 使用Cypher查询按ID降序排列（假设ID越大越新）
            String cypher = "MATCH (m:Movie) RETURN m ORDER BY m.id DESC LIMIT $count";
            Map<String, Object> params = new HashMap<>();
            params.put("count", count);

            Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
            List<MovieNode> movies = StreamSupport.stream(movieIterable.spliterator(), false)
                    .collect(Collectors.toList());

            return Result.success(movies);
        } catch (Exception e) {
            log.error("获取最新电影失败：count={}", count, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取最新电影失败");
        }
    }

    /**
     * 根据类型获取电影
     */
    public Result<List<MovieNode>> getMoviesByType(String type, int page, int size) {
        try {
            List<MovieNode> movies = movieRepository.findByTypeOrderByRating(type, size);
            
            // 获取该类型的总记录数
            long total = getTotalMovieCountByType(type);
            
            // 使用专门的方法来设置分页数据
            return Result.successWithTotal(movies, total);
        } catch (Exception e) {
            log.error("根据类型获取电影失败：type={}, page={}, size={}", type, page, size, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "根据类型获取电影失败");
        }
    }
    
    /**
     * 获取指定类型的电影总数
     */
    private long getTotalMovieCountByType(String type) {
        String cypher = "MATCH (m:Movie) WHERE m.type = $type RETURN COUNT(m) as count";
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        Iterable<Map<String, Object>> result = neo4jSession.query(cypher, params);
        if (result.iterator().hasNext()) {
            return ((Number) result.iterator().next().get("count")).longValue();
        }
        return 0;
    }

    /**
     * 根据导演获取电影
     */
    public Result<List<MovieNode>> getMoviesByDirector(String directorName, int page, int size) {
        try {
            // 使用Cypher查询来处理director字符串字段
            String cypher = "MATCH (m:Movie) WHERE m.director CONTAINS $directorName RETURN m SKIP $skip LIMIT $size";
            Map<String, Object> params = new HashMap<>();
            params.put("directorName", directorName);
            params.put("skip", page * size);
            params.put("size", size);

            Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
            List<MovieNode> movies = StreamSupport.stream(movieIterable.spliterator(), false)
                    .collect(Collectors.toList());

            return Result.success(movies);
        } catch (Exception e) {
            log.error("根据导演获取电影失败：directorName={}, page={}, size={}", directorName, page, size, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "根据导演获取电影失败");
        }
    }

    /**
     * 根据演员获取电影
     */
    public Result<List<MovieNode>> getMoviesByActor(String actorName, int page, int size) {
        try {
            // 使用Cypher查询来处理actor字符串字段
            String cypher = "MATCH (m:Movie) WHERE m.actor CONTAINS $actorName RETURN m SKIP $skip LIMIT $size";
            Map<String, Object> params = new HashMap<>();
            params.put("actorName", actorName);
            params.put("skip", page * size);
            params.put("size", size);

            Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
            List<MovieNode> movies = StreamSupport.stream(movieIterable.spliterator(), false)
                    .collect(Collectors.toList());

            return Result.success(movies);
        } catch (Exception e) {
            log.error("根据演员获取电影失败：actorName={}, page={}, size={}", actorName, page, size, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "根据演员获取电影失败");
        }
    }

    /**
     * 获取电影的评论列表（包含用户信息）
     */
    public Result<List<CommentNode>> getCommentsByMovieId(Integer movieId) {
        try {
            String cypher = "MATCH (m:Movie)-[:HAS_COMMENT]->(c:Comment)-[:CREATED_BY]->(u:User) WHERE m.info_id = $movieId RETURN c, u";
            Map<String, Object> params = new HashMap<>();
            params.put("movieId", movieId);
            
            Iterable<Map<String, Object>> resultIterable = neo4jSession.query(cypher, params);
            List<CommentNode> comments = new ArrayList<>();
            
            for (Map<String, Object> row : resultIterable) {
                CommentNode comment = (CommentNode) row.get("c");
                UserNode user = (UserNode) row.get("u");
                
                // 设置完整的用户信息
                comment.setUser(user);
                
                comments.add(comment);
            }
            
            return Result.success(comments);
        } catch (Exception e) {
            log.error("获取电影评论失败：movieId={}", movieId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影评论失败");
        }
    }

    /**
     * 获取所有电影和评论的详细信息
     */
    public Result<List<Map<String, Object>>> getAllMovieComments() {
        try {
            String cypher = "MATCH (m:Movie)-[:HAS_COMMENT]->(c:Comment) RETURN m.id AS 电影ID, m.name AS 电影名, c.id AS 评论ID, c.content AS 评论内容";
            Iterable<Map<String, Object>> queryResult = neo4jSession.query(cypher, Collections.emptyMap());
            
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> row : queryResult) {
                Map<String, Object> resultRow = new HashMap<>();
                resultRow.put("电影ID", row.get("电影ID"));
                resultRow.put("电影名", row.get("电影名"));
                resultRow.put("评论ID", row.get("评论ID"));
                resultRow.put("评论内容", row.get("评论内容"));
                results.add(resultRow);
            }
            
            return Result.success(results);
        } catch (Exception e) {
            log.error("获取所有电影评论失败", e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取所有电影评论失败");
        }
    }
    
    /**
     * 根据电影ID获取特定电影的评论信息
     */
    public Result<List<Map<String, Object>>> getMovieCommentsByMovieId(Integer movieId) {
        try {
            log.info("正在查询电影ID {} 的评论", movieId);
            
            // 1. 先查询通过关系连接的评论
            String cypher = "MATCH (m:Movie)-[:HAS_COMMENT]->(c:Comment) " +
                    "WHERE m.info_id = $movieId " +
                    "RETURN " +
                    "id(c) AS comment_id, " +
                    "m.info_id AS movie_id, " +
                    "c.creator AS creator, " +
                    "c.content AS content, " +
                    "c.comment_rating AS comment_rating, " +
                    "c.comment_time AS comment_time, " +
                    "c.comment_add_time AS comment_add_time";

            Map<String, Object> params = new HashMap<>();
            params.put("movieId", movieId);

            Iterable<Map<String, Object>> queryResult = neo4jSession.query(cypher, params);

            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> row : queryResult) {
                Map<String, Object> resultRow = new HashMap<>();
                resultRow.put("comment_id", row.get("comment_id"));
                resultRow.put("movie_id", row.get("movie_id"));
                resultRow.put("creator", row.get("creator"));
                resultRow.put("content", row.get("content"));
                resultRow.put("comment_rating", row.get("comment_rating"));
                resultRow.put("comment_time", row.get("comment_time"));
                resultRow.put("comment_add_time", row.get("comment_add_time"));
                results.add(resultRow);
            }
            
            log.info("通过关系查询到 {} 条评论", results.size());
            
            // 如果通过关系没查到，尝试通过属性查询
            if (results.isEmpty()) {
                log.info("通过关系未查询到评论，尝试通过属性查询电影ID {}", movieId);
                String attrCypher = "MATCH (c:Comment) " +
                        "WHERE c.movie_id = $movieId " +
                        "RETURN " +
                        "id(c) AS comment_id, " +
                        "c.movie_id AS movie_id, " +
                        "c.creator AS creator, " +
                        "c.content AS content, " +
                        "c.comment_rating AS comment_rating, " +
                        "c.comment_time AS comment_time, " +
                        "c.comment_add_time AS comment_add_time";
                
                Iterable<Map<String, Object>> attrQueryResult = neo4jSession.query(attrCypher, params);
                
                for (Map<String, Object> row : attrQueryResult) {
                    Map<String, Object> resultRow = new HashMap<>();
                    resultRow.put("comment_id", row.get("comment_id"));
                    resultRow.put("movie_id", row.get("movie_id"));
                    resultRow.put("creator", row.get("creator"));
                    resultRow.put("content", row.get("content"));
                    resultRow.put("comment_rating", row.get("comment_rating"));
                    resultRow.put("comment_time", row.get("comment_time"));
                    resultRow.put("comment_add_time", row.get("comment_add_time"));
                    results.add(resultRow);
                }
                
                log.info("通过属性查询到 {} 条评论", results.size());
            }

            return Result.success(results);
        } catch (Exception e) {
            log.error("获取电影评论失败：movieId={}", movieId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影评论失败");
        }
    }
    
    /**
     * 获取有评论的电影列表
     */
    public Result<List<MovieNode>> getMoviesWithComments(int page, int size) {
        try {
            int skip = page * size;
            List<MovieNode> movies = movieRepository.findMoviesWithCommentsPaginated(skip, size);
            
            // 获取有评论的电影总数
            long total = getMoviesWithCommentsCount();
            
            return Result.successWithTotal(movies, total);
        } catch (Exception e) {
            log.error("获取有评论的电影列表失败：page={}, size={}", page, size, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取有评论的电影列表失败");
        }
    }
    
    /**
     * 获取有评论的电影总数
     */
    private long getMoviesWithCommentsCount() {
        String cypher = "MATCH (m:Movie)-[:HAS_COMMENT]->(c:Comment) RETURN COUNT(DISTINCT m) as count";
        Iterable<Map<String, Object>> result = neo4jSession.query(cypher, Collections.emptyMap());
        if (result.iterator().hasNext()) {
            return ((Number) result.iterator().next().get("count")).longValue();
        }
        return 0;
    }
    
    /**
     * 增强版协同过滤（UserCF）
     */
    private List<MovieNode> collaborativeFilteringRecommend(Integer userId) {
        // 步骤1：获取目标用户已评分电影
        List<Integer> ratedMovieIds = getUserRatedMovieIds(userId);
        if (CollectionUtils.isEmpty(ratedMovieIds)) {
            log.warn("用户{}无评分记录，协同过滤返回默认热门电影", userId);
            return getDefaultHighRatingMovies(cfTopN);
        }

        // 步骤2：计算用户相似度（余弦相似度）
        String similarityCypher = "MATCH (u1:User {username: $userId})-[r1:RATED]->(m:Movie)<-[r2:RATED]-(u2:User) " +
                                  "WITH u2, collect(r1.rating) as ratings1, collect(r2.rating) as ratings2 " +
                                  "WHERE size(ratings1) > 1 " +
                                  "RETURN u2.username as similarUserId, cosineSimilarity(ratings1, ratings2) as similarity " +
                                  "ORDER BY similarity DESC LIMIT 10";

        Map<String, Object> similarityParams = new HashMap<>();
        similarityParams.put("userId", userId.toString());
        Iterable<Map<String, Object>> similarityResult = neo4jSession.query(similarityCypher, similarityParams);

        // 步骤3：获取相似用户高分未看电影
        List<String> similarUserIds = StreamSupport.stream(similarityResult.spliterator(), false)
                .map(row -> (String) row.get("similarUserId"))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(similarUserIds)) {
            return getDefaultHighRatingMovies(cfTopN);
        }

        String recommendCypher = "MATCH (u:User) WHERE u.username IN $similarUserIds " +
                                 "MATCH (u)-[r:RATED]->(m:Movie) WHERE r.rating >= $threshold AND NOT m.movieId IN $excludedIds " +
                                 "RETURN m ORDER BY r.rating DESC LIMIT $topN";

        Map<String, Object> recommendParams = new HashMap<>();
        recommendParams.put("similarUserIds", similarUserIds);
        recommendParams.put("threshold", likedRatingThreshold);
        recommendParams.put("excludedIds", ratedMovieIds);
        recommendParams.put("topN", cfTopN);

        Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, recommendCypher, recommendParams);
        return StreamSupport.stream(movieIterable.spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     * 增强版内容推荐（含意图过滤）
     */
    private List<MovieNode> contentBasedRecommend(Integer userId) {
        List<MovieNode> userLikedMovies = getUserLikedMovies(userId);
        if (CollectionUtils.isEmpty(userLikedMovies)) {
            return getDefaultHighRatingMovies(contentTopN);
        }

        // 提取多维度特征
        Set<String> likedDirectors = userLikedMovies.stream()
                .map(MovieNode::getDirectorString)
                .filter(Objects::nonNull)
                .flatMap(directorString -> Arrays.stream(directorString.split("\\|")))
                .collect(Collectors.toSet());

        Set<String> likedActors = userLikedMovies.stream()
                .map(MovieNode::getActorString)
                .filter(Objects::nonNull)
                .flatMap(actorString -> Arrays.stream(actorString.split("\\|")))
                .collect(Collectors.toSet());

        Set<String> likedTypes = userLikedMovies.stream()
                .map(MovieNode::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 多路径Cypher（导演+演员+类型）
        String cypher = "MATCH (m:Movie) " +
                        "WHERE (m.type IN $types OR " +
                        "EXISTS((m)-[:DIRECTED_BY]->(d:Director) WHERE d.name IN $directors) OR " +
                        "EXISTS((m)-[:STARRED_BY]->(a:Actor) WHERE a.name IN $actors)) " +
                        "AND NOT EXISTS((:User {username: $userIdStr})-[:RATED]->(m)) " +
                        "RETURN m ORDER BY m.rating DESC LIMIT $topN";

        Map<String, Object> params = new HashMap<>();
        params.put("types", likedTypes);
        params.put("directors", likedDirectors);
        params.put("actors", likedActors);
        params.put("userIdStr", userId.toString());
        params.put("topN", contentTopN);

        Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
        return StreamSupport.stream(movieIterable.spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     * 增强版知识图谱推荐（多路径融合）
     */
    private List<MovieNode> knowledgeGraphPathRecommend(Integer userId) {
        // 多路径Cypher（导演+演员+类型）
        String cypher = "MATCH (u:User {username: $userIdStr})-[:POSTED]->(c:Comment)-[:TARGETS]->(m:Movie) " +
                        "MATCH (m)-[:DIRECTED_BY]->(d:Director)-[:DIRECTED]->(m2:Movie) " +
                        "MATCH (m)-[:STARRED_BY]->(a:Actor)-[:STARRED_IN]->(m2:Movie) " +
                        "MATCH (m)-[:HAS_TYPE]->(t:Type)-[:BELONGS_TO]->(m2:Movie) " +
                        "WHERE NOT EXISTS((u)-[:RATED]->(m2)) " +
                        "WITH m2, COUNT(DISTINCT d) + COUNT(DISTINCT a) + COUNT(DISTINCT t) AS score " +
                        "RETURN m2 ORDER BY score DESC LIMIT $topN";

        Map<String, Object> params = new HashMap<>();
        params.put("userIdStr", userId.toString());
        params.put("topN", kgTopN);

        Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
        List<MovieNode> result = StreamSupport.stream(movieIterable.spliterator(), false)
                .collect(Collectors.toList());

        // 兜底：无结果时返回默认
        return CollectionUtils.isEmpty(result) ? getDefaultHighRatingMovies(kgTopN) : result;
    }

    /**
     * 意图驱动过滤（新增核心逻辑）
     */
    private List<MovieNode> filterByIntent(List<MovieNode> movies, RecommendIntent intent) {
        if (CollectionUtils.isEmpty(movies) || intent == null || CollectionUtils.isEmpty(intent.getParams())) {
            return movies;
        }

        Map<String, Object> params = intent.getParams();
        // 按导演过滤
        if (params.containsKey("director")) {
            String director = (String) params.get("director");
            movies = movies.stream()
                    .filter(m -> {
                        String directorString = m.getDirectorString();
                        if (directorString != null && !directorString.isEmpty()) {
                            String[] directors = directorString.split("\\|");
                            return Arrays.asList(directors).contains(director);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // 按演员过滤
        if (params.containsKey("actor")) {
            String actor = (String) params.get("actor");
            movies = movies.stream()
                    .filter(m -> {
                        String actorString = m.getActorString();
                        if (actorString != null && !actorString.isEmpty()) {
                            String[] actors = actorString.split("\\|");
                            return Arrays.asList(actors).contains(actor);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // 按类型过滤
        if (params.containsKey("type")) {
            String type = (String) params.get("type");
            movies = movies.stream()
                    .filter(m -> type.equals(m.getType()))
                    .collect(Collectors.toList());
        }

        // 按最低评分过滤
        if (params.containsKey("min_rating")) {
            Double minRating = new Double(params.get("min_rating").toString());
            movies = movies.stream()
                    .filter(m -> Optional.ofNullable(m.getMovieRating()).orElse((double) 0).compareTo(minRating) >= 0)
                    .collect(Collectors.toList());
        }

        return movies;
    }

    // 辅助方法：添加评分权重
    private void addToScoreMap(List<MovieNode> movies, Map<Integer, Double> scoreMap, double weight) {
        if (CollectionUtils.isEmpty(movies)) {
            return;
        }
        for (int i = 0; i < movies.size(); i++) {
            MovieNode movie = movies.get(i);
            if (movie == null || movie.getInfoId() == null) {
                continue;
            }
            double score = (1.0 - (double) i / movies.size()) * weight;
            scoreMap.put(movie.getInfoId(), scoreMap.getOrDefault(movie.getInfoId(), 0.0) + score);
        }
    }

    // 辅助方法：获取用户已评分电影ID
    private List<Integer> getUserRatedMovieIds(Integer userId) {
        String cypher = "MATCH (u:User {username: $userIdStr})-[:RATED]->(m:Movie) RETURN m.movieId";
        Map<String, Object> params = new HashMap<>();
        params.put("userIdStr", userId.toString());

        Iterable<Map<String, Object>> queryResult = neo4jSession.query(cypher, params);
        return StreamSupport.stream(queryResult.spliterator(), false)
                .map(row -> ((Number) row.get("m.movieId")).intValue())
                .collect(Collectors.toList());
    }

    // 辅助方法：获取用户高评分电影
    private List<MovieNode> getUserLikedMovies(Integer userId) {
        String cypher = "MATCH (u:User {username: $userIdStr})-[r:RATED]->(m:Movie) " +
                        "WHERE r.rating >= $threshold RETURN m";
        Map<String, Object> params = new HashMap<>();
        params.put("userIdStr", userId.toString());
        params.put("threshold", likedRatingThreshold);

        Iterable<Map<String, Object>> queryResult = neo4jSession.query(cypher, params);
        return StreamSupport.stream(queryResult.spliterator(), false)
                .map(row -> (MovieNode) row.get("m"))
                .collect(Collectors.toList());
    }

    // 兜底方法：默认高评分电影
    private List<MovieNode> getDefaultHighRatingMovies(Integer topN) {
        String cypher = "MATCH (m:Movie) WHERE m.rating >= 8.0 RETURN m ORDER BY m.rating DESC LIMIT $topN";
        Map<String, Object> params = new HashMap<>();
        params.put("topN", topN);

        Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
        return StreamSupport.stream(movieIterable.spliterator(), false)
                .collect(Collectors.toList());
    }

}