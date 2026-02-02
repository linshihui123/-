// com/example/service/MovieRecommendationService.java
package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.*;
import org.example.repository.MovieRepository;
import org.example.repository.CommentRepository;
import org.example.repository.UserMapper;
import org.example.response.Result;
import org.example.response.ResultCodeEnum;
import org.example.response.SimilarityResult;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private UserMapper userMapper;

    // 配置注入（替代硬编码）

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
     *
     */


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
     * 基于用户名的协同过滤推荐（接收字符串用户名）
     */
    public List<MovieNode> collaborativeFilteringRecommendByUsername(String username) {
        // 步骤1：获取目标用户已评分电影ID（基于Comment节点movie_id）
        List<Integer> ratedMovieIds = getUserRatedMovieIdsByUsername(username);
        if (CollectionUtils.isEmpty(ratedMovieIds)) {
            log.warn("用户{}无评分记录，协同过滤返回默认热门电影", username);
            return getDefaultHighRatingMovies(20);
        }

        String similarityCypher = String.format(
                "MATCH (c1:Comment), (c2:Comment) " +
                        "WHERE c1.creator = '%s' AND c2.creator <> '%s' " +
                        "  AND c1.movie_id = c2.movie_id " +
                        "WITH c2.creator as similarUserId, " +
                        "     collect(c1.comment_rating) as ratings1, " +
                        "     collect(c2.comment_rating) as ratings2 " +
                        "WHERE size(ratings1) > 0 " +
                        "RETURN similarUserId, ratings1, ratings2 " +
                        "ORDER BY size(ratings1) DESC LIMIT 10",
                username, username);

        Map<String, Object> similarityParams = new HashMap<>();
        similarityParams.put("username", username);
        Iterable<Map<String, Object>> similarityResult = neo4jSession.query(similarityCypher, similarityParams);

        // 步骤3：Java中计算余弦相似度，封装相似结果
        List<SimilarityResult> similarityResults = new ArrayList<>();


        for (Map<String, Object> row : similarityResult) {
            // 尝试多种可能的字段名
            String similarUserId = null;
            if (row.containsKey("similarUserId")) {
                similarUserId = (String) row.get("similarUserId");
            } else if (row.containsKey("c2.creator")) {
                similarUserId = (String) row.get("c2.creator");
            } else {
                // 查找包含 creator 或 user 相关的字段
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getKey().toLowerCase().contains("creator") ||
                            entry.getKey().toLowerCase().contains("user") ||
                            entry.getKey().toLowerCase().contains("similar")) {
                        similarUserId = entry.getValue() != null ? entry.getValue().toString() : null;
                        break;
                    }
                }
            }

            // 尝试多种可能的字段名
            Object ratings1Obj = null;
            Object ratings2Obj = null;
            if (row.containsKey("ratings1")) {
                ratings1Obj = row.get("ratings1");
            } else if (row.containsKey("col1") || row.containsKey("collect(c1.comment_rating)")) {
                ratings1Obj = row.get("col1") != null ? row.get("col1") : row.get("collect(c1.comment_rating)");
            }

            if (row.containsKey("ratings2")) {
                ratings2Obj = row.get("ratings2");
            } else if (row.containsKey("col2") || row.containsKey("collect(c2.comment_rating)")) {
                ratings2Obj = row.get("col2") != null ? row.get("col2") : row.get("collect(c2.comment_rating)");
            }

            List<Integer> ratings1 = convertToObjectList(ratings1Obj);
            List<Integer> ratings2 = convertToObjectList(ratings2Obj);

            // 计算余弦相似度 - 松化条件检查
            if (similarUserId != null && ratings1 != null && ratings2 != null &&
                    !ratings1.isEmpty() && !ratings2.isEmpty()) {
                double similarity = calculateCosineSimilarity(ratings1, ratings2);
                similarityResults.add(new SimilarityResult(similarUserId, similarity));
            }
        }

        // 按相似度降序排序，取前10个最相似用户
        similarityResults.sort(Comparator.comparingDouble(SimilarityResult::getSimilarity).reversed());
        List<String> similarUserIds = similarityResults.stream()
                .limit(10)
                .map(SimilarityResult::getUserId)
                .collect(Collectors.toList());

        // 无相似用户时返回默认热门电影
        if (CollectionUtils.isEmpty(similarUserIds)) {
            log.info("用户{}未找到相似用户，协同过滤返回默认热门电影", username);
            return getDefaultHighRatingMovies(20);
        }

        String similarUserIdsStr = similarUserIds.stream()
                .map(s -> "'" + s.replace("'", "\\'") + "'")  // 关键：转义单引号，防止用户名含单引号导致语法错误
                .collect(Collectors.joining(", ", "[", "]"));

        String ratedMovieIdsStr = ratedMovieIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]"));

        String recommendCypher = String.format(
                "MATCH (c:Comment) WHERE c.creator IN %s AND c.comment_rating >= %d AND NOT c.movie_id IN %s MATCH (m:Movie) WHERE m.id = c.movie_id WITH m, MAX(c.comment_rating) as maxRating ORDER BY maxRating DESC LIMIT %d RETURN m as movie",
                similarUserIdsStr,  // 第1个%s：拼接好的用户集合
                likedRatingThreshold, // 第1个%d：评分阈值
                ratedMovieIdsStr,   // 第2个%s：拼接好的电影ID集合
                20);            // 第2个%d：推荐数量

        Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, recommendCypher, new HashMap<>());

        List<MovieNode> result = StreamSupport.stream(movieIterable.spliterator(), false)
                .collect(Collectors.toList());
        return result;
    }



    // 兜底方法：默认高评分电影
    private List<MovieNode> getDefaultHighRatingMovies(Integer topN) {
        String cypher = "MATCH (m:Movie) WHERE m.movie_rating >= 8.0 RETURN m ORDER BY m.rating DESC LIMIT $topN";
        Map<String, Object> params = new HashMap<>();
        params.put("topN", topN);

        Iterable<MovieNode> movieIterable = neo4jSession.query(MovieNode.class, cypher, params);
        return StreamSupport.stream(movieIterable.spliterator(), false)
                .collect(Collectors.toList());
    }
    private List<Integer> getUserRatedMovieIdsByUsername(String username) {
        String cypher = "MATCH (c:Comment) WHERE c.creator = $username RETURN c.movie_id";
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);

        Iterable<Map<String, Object>> queryResult = neo4jSession.query(cypher, params);
        return StreamSupport.stream(queryResult.spliterator(), false)
                .filter(row -> row.get("c.movie_id") != null)
                .map(row -> ((Number) row.get("c.movie_id")).intValue())
                .distinct() // 去重：同一电影多条评论仅保留一个ID
                .collect(Collectors.toList());
    }
    private double calculateCosineSimilarity(List<Integer> vec1, List<Integer> vec2) {
        if (vec1 == null || vec2 == null || vec1.isEmpty() || vec2.isEmpty()) {
            return 0.0;
        }

        // 确保向量长度相同
        int maxLength = Math.max(vec1.size(), vec2.size());
        double[] v1 = new double[maxLength];
        double[] v2 = new double[maxLength];

        for (int i = 0; i < maxLength; i++) {
            v1[i] = i < vec1.size() ? vec1.get(i) : 0.0;
            v2[i] = i < vec2.size() ? vec2.get(i) : 0.0;
        }

        // 计算点积和范数
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < maxLength; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        // 避免除零错误
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    private List<Integer> convertToObjectList(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            List<Integer> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                } else if (item instanceof String) {
                    try {
                        result.add(Integer.parseInt((String) item));
                    } catch (NumberFormatException e) {
                        // 忽略无法转换的字符串
                    }
                }
            }
            return result;
        } else if (obj.getClass().isArray() && obj instanceof Long[]) {
            // 处理Long[]数组
            Long[] longArray = (Long[]) obj;
            List<Integer> result = new ArrayList<>();
            for (Long value : longArray) {
                if (value != null) {
                    result.add(value.intValue());
                }
            }
            return result;
        } else if (obj.getClass().isArray() && obj instanceof Integer[]) {
            // 处理Integer[]数组
            Integer[] intArray = (Integer[]) obj;
            return Arrays.asList(intArray);
        }

        return null;
    }

    public Object getMovieAnalysis() {
        // 这里可以调用Neo4j的Cypher查询语句来获取电影分析数据
        /**
         *
         * 展示电影评论的情感倾向（正面 / 负面 / 中性，基于 CONTENT 文本分析）；
         */
        // TODO：使用coze来接收处理数据
        String cypher = "MATCH (m:Movie) RETURN m.title, m.director";
        return null;
    }

    /**
     * 展示单部电影的评分分布（如 1 分占比 10%、4 分占比 50%）
     * @param movieName 电影名称（精准匹配）
     * @return 结构化评分分布结果（含电影基本信息、各分数段统计、总评分数）
     */
    public Object getMovieRatings(String movieName) {
        // 1. 入参非空校验
        if (movieName == null || movieName.trim().isEmpty()) {
            log.error("电影评分查询失败：电影名称不能为空");
            return buildErrorResult(400, "参数错误，电影名称不能为空");
        }
        String trimedMovieName = movieName.trim();

        try {
            // 2. 第一步：根据电影名称查询Movie节点，获取电影ID和基本信息（确保电影存在）
            Map<String, Object> movieParams = new HashMap<>();
            movieParams.put("movieName", trimedMovieName);
            // Cypher：精准匹配电影名称，返回Movie节点基本属性
            String movieCypher = "MATCH (m:Movie) WHERE m.name = $movieName RETURN m.id, m.name, m.movie_rating, m.type, m.region LIMIT 1";
            Iterable<Map<String, Object>> movieResult = neo4jSession.query(movieCypher, movieParams);
            List<Map<String, Object>> movieList = StreamSupport.stream(movieResult.spliterator(), false)
                    .collect(Collectors.toList());

            // 电影不存在时返回友好提示
            if (CollectionUtils.isEmpty(movieList)) {
                log.warn("电影评分查询失败：未找到名称为[{}]的电影", trimedMovieName);
                return buildErrorResult(404, "未找到名称为【" + trimedMovieName + "】的电影");
            }

            // 提取电影基本信息
            Map<String, Object> movieInfo = movieList.get(0);
            Integer movieId = (Integer) movieInfo.get("m.id");

            // 3. 第二步：根据电影ID统计Comment节点的评分分布（1-5分各分数段数量）
            Map<String, Object> ratingParams = new HashMap<>();
            ratingParams.put("movieId", movieId);
            // Cypher：按评分分组统计数量，自动过滤无评分的评论，确保评分是1-5的整数
            String ratingCypher = "MATCH (c:Comment) " +
                    "WHERE c.movie_id = $movieId AND c.comment_rating IS NOT NULL " +
                    "  AND c.comment_rating IN [1,2,3,4,5] " +
                    "WITH c.comment_rating as score, COUNT(c) as ratingCount " +
                    "ORDER BY score ASC " +
                    "RETURN score, ratingCount";
            Iterable<Map<String, Object>> ratingResult = neo4jSession.query(ratingCypher, ratingParams);
            List<Map<String, Object>> ratingList = StreamSupport.stream(ratingResult.spliterator(), false)
                    .collect(Collectors.toList());

            // 4. 第三步：计算各分数段占比，补全1-5分所有分数段（无评分的分数段数量/占比为0）
            int totalRating = ratingList.stream()
                    .mapToInt(item -> ((Long) item.get("ratingCount")).intValue())
                    .sum(); // 总评分数量

            // 初始化1-5分评分分布，默认数量0、占比0%
            Map<Integer, Map<String, Object>> ratingDistribution = new TreeMap<>(); // TreeMap保证按分数升序
            for (int score = 1; score <= 5; score++) {
                Map<String, Object> scoreInfo = new HashMap<>();
                scoreInfo.put("ratingCount", 0);
                scoreInfo.put("proportion", "0.00%");
                ratingDistribution.put(score, scoreInfo);
            }

            // 填充统计结果，计算占比（保留2位小数，四舍五入）
            for (Map<String, Object> item : ratingList) {
                Integer score = ((Long) item.get("score")).intValue(); // Neo4j返回Long，转Integer
                int count = ((Long) item.get("ratingCount")).intValue();
                // 计算占比，总评分为0时占比保持0%
                String proportion = totalRating == 0 ? "0.00%" :
                        new BigDecimal(count * 100.0 / totalRating)
                                .setScale(2, RoundingMode.HALF_UP)
                                .toString() + "%";
                // 更新数量和占比
                ratingDistribution.get(score).put("ratingCount", count);
                ratingDistribution.get(score).put("proportion", proportion);
            }

            // 5. 构建最终结构化返回结果
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("code", 200);
            finalResult.put("msg", "查询成功");
            finalResult.put("movieInfo", movieInfo); // 电影基本信息
            finalResult.put("totalRating", totalRating); // 总评分数量
            finalResult.put("ratingDistribution", ratingDistribution); // 1-5分评分分布（升序）

            log.info("电影[{}({})]评分查询成功，总评分数：{}", trimedMovieName, movieId, totalRating);
            return finalResult;

        } catch (Exception e) {
            // 全局异常捕获，避免接口崩溃
            log.error("电影[{}]评分查询发生异常", movieName, e);
            return buildErrorResult(500, "服务器内部异常，电影评分查询失败");
        }
    }

    /**
     * 构建统一的异常返回结果
     * @param code 错误码（400参数错/404未找到/500服务器错）
     * @param msg 错误信息
     * @return 结构化异常结果
     */
    private Map<String, Object> buildErrorResult(int code, String msg) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("code", code);
        errorResult.put("msg", msg);
        errorResult.put("movieInfo", null);
        errorResult.put("totalRating", 0);
        errorResult.put("ratingDistribution", null);
        return errorResult;
    }

    /**
     * 按类型统计电影的平均评分、电影数量
     * 统计项：各类别下的「平均电影评分」「电影数量」
     * @return 类型统计结果列表
     */
    public List<Map<String, Object>> getMovieRatingsByType() {
        try {
            // 简化查询：按类型分组统计
            String typeCypher = "MATCH (m:Movie) " +
                    "WHERE m.type IS NOT NULL AND m.movie_rating IS NOT NULL " +
                    "RETURN m.type as movieType, " +
                    "       avg(m.movie_rating) as avgRating, " +
                    "       count(m) as movieCount";
            
            Iterable<Map<String, Object>> typeQueryResult = neo4jSession.query(typeCypher, new HashMap<>());
            List<Map<String, Object>> typeResults = StreamSupport.stream(typeQueryResult.spliterator(), false)
                    .collect(Collectors.toList());

            // 处理类型维度数据
            List<Map<String, Object>> typeStatistics = new ArrayList<>();
            for (Map<String, Object> item : typeResults) {
                Map<String, Object> typeStat = new HashMap<>();
                typeStat.put("movieType", item.get("movieType"));
                typeStat.put("avgMovieRating", new BigDecimal(item.get("avgRating").toString()).setScale(2, RoundingMode.HALF_UP));
                typeStat.put("movieTotalCount", ((Long) item.get("movieCount")).intValue());
                typeStatistics.add(typeStat);
            }
            
            // 按电影数量降序排序
            typeStatistics.sort((a, b) -> Integer.compare((Integer) b.get("movieTotalCount"), (Integer) a.get("movieTotalCount")));

            log.info("电影类型维度统计完成，类型数：{}", typeStatistics.size());
            return typeStatistics;

        } catch (Exception e) {
            // 全局异常捕获，保证接口不崩溃，返回友好错误信息
            log.error("按类型统计电影评分发生异常", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 按地区统计电影的平均评分、电影数量
     * 统计项：各地区下的「平均电影评分」「电影数量」
     * @return 地区统计结果列表
     */
    public List<Map<String, Object>> getMovieRatingsByRegion() {
        try {
            // 简化查询：按地区分组统计
            String regionCypher = "MATCH (m:Movie) " +
                    "WHERE m.region IS NOT NULL AND m.movie_rating IS NOT NULL " +
                    "RETURN m.region as movieRegion, " +
                    "       avg(m.movie_rating) as avgRating, " +
                    "       count(m) as movieCount";
            
            Iterable<Map<String, Object>> regionQueryResult = neo4jSession.query(regionCypher, new HashMap<>());
            List<Map<String, Object>> regionResults = StreamSupport.stream(regionQueryResult.spliterator(), false)
                    .collect(Collectors.toList());

            // 处理地区维度数据
            List<Map<String, Object>> regionStatistics = new ArrayList<>();
            for (Map<String, Object> item : regionResults) {
                Map<String, Object> regionStat = new HashMap<>();
                regionStat.put("movieRegion", item.get("movieRegion"));
                regionStat.put("avgMovieRating", new BigDecimal(item.get("avgRating").toString()).setScale(2, RoundingMode.HALF_UP));
                regionStat.put("movieTotalCount", ((Long) item.get("movieCount")).intValue());
                regionStatistics.add(regionStat);
            }
            
            // 按电影数量降序排序
            regionStatistics.sort((a, b) -> Integer.compare((Integer) b.get("movieTotalCount"), (Integer) a.get("movieTotalCount")));

            log.info("电影地区维度统计完成，地区数：{}", regionStatistics.size());
            return regionStatistics;

        } catch (Exception e) {
            log.error("按地区统计电影评分发生异常", e);
            return new ArrayList<>();
        }
    }
}
