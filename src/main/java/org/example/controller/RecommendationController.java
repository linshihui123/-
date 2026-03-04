package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.AiDomainFacadeService;
import org.example.model.CollaborativeFilteringResult;
import org.example.model.MovieRecommendItem;
import org.example.model.CommentBasedRecommendationItem;
import org.example.model.RatingPredictionItem;
import org.example.response.Result;
import org.example.response.ResultCodeEnum;
import org.example.service.MovieRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/recommendation")
@CrossOrigin(origins = "*") // 跨域支持
public class RecommendationController {

    @Autowired
    private MovieRecommendationService recommendationService;

    @Autowired
    private AiDomainFacadeService aiDomainFacadeService;

    /**
     * 个性化融合推荐：按配置权重融合协同过滤、内容推荐、知识图谱推荐
     * @param username 用户名
     * @param limit 返回数量，默认 20
     */
    @GetMapping("/fused")
    public Result<List<org.example.model.MovieNode>> fusedRecommend(
            @RequestParam String username,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            List<org.example.model.MovieNode> list = recommendationService.fusedRecommendationByUsername(username, limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("融合推荐失败：username={}, limit={}", username, limit, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "融合推荐失败");
        }
    }

    /**
     * 基于用户名的协同过滤推荐接口
     * 新增返回：用户已评分电影 + 相似用户及相似度 + 推荐电影
     * @param userId 用户名/用户ID
     * @return Result<CollaborativeFilteringResult> 封装所有推荐相关数据
     */
    @GetMapping("/collaborative-filtering/{userId}")
    public Result<CollaborativeFilteringResult> collaborativeFilteringRecommend(@PathVariable String userId) {
        try {
            // 调用修改后的Service方法，返回封装后的DTO对象
            CollaborativeFilteringResult recommendations = recommendationService.collaborativeFilteringRecommendByUsername(userId);
            // 成功返回DTO对象，前端可一次性获取所有数据
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("协同过滤推荐失败：userId={}", userId, e);
            // 保持原有异常返回逻辑，统一错误码和提示
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "协同过滤推荐失败");
        }
    }

    /**
     * 获取指定电影的评论列表及 AI 总结。若该电影已存在缓存的 ai_comments_summary 则直接返回（响应快）；
     * 否则调用大模型生成总结并写入电影节点，下次可直接使用。
     * 返回：{ "comments": 评论列表, "aiSummary": 大模型总结（可能为 null） }
     */
    @GetMapping("/movie-comments")
    public Result<Map<String, Object>> getMovieCommentsByMovieName(@RequestParam String movieName) {
        try {
            return recommendationService.getMovieCommentsWithAiSummary(movieName);
        } catch (Exception e) {
            log.error("获取电影评论失败：movieName={}", movieName, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影评论失败");
        }
    }

    /**
     * 智能推荐：返回统一格式列表，字段：index、movieName、intro、type、rating、director；无简介时由系统生成并落库
     */
    @GetMapping("/ai-recommend-with-comments")
    public Result<List<MovieRecommendItem>> getAiRecommendWithComments(
            @RequestParam(defaultValue = "8") Integer limit) {
        try {
            List<MovieRecommendItem> list = aiDomainFacadeService.getRecommendationWithComments(limit);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取智能推荐失败：limit={}", limit, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取智能推荐失败");
        }
    }

    /**
     * 电影数据分析
     */
     //TODO:这个接口传递给AI
    @GetMapping("/movie-analysis")
    public Result<List<Map<String, Object>>> getMovieAnalysis() {
        try {
            List<Map<String, Object>> movieAnalysis = (List<Map<String, Object>>) recommendationService.getMovieAnalysis();
            return Result.success(movieAnalysis);
        } catch (Exception e) {
            log.error("获取电影分析数据失败", e);
            return Result.error(org.example.response.ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影分析数据失败");
        }
    }
    /**
     * 多维度电影榜单接口
     * 包含：高分榜、热门评论榜、类型榜
     * @param year 年份筛选（可选）
     * @param type 类型筛选（可选）
     * @param region 地区筛选（可选）
     * @param limit 返回数量限制
     * @return 多维度榜单数据
     */
    @GetMapping("/multi-dimensional-ranking")
    public Result<Map<String, Object>> getMultiDimensionalRanking(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Map<String, Object> rankingData = recommendationService.getMultiDimensionalRanking(year, type, region, limit);
            return Result.success(rankingData);
        } catch (Exception e) {
            log.error("获取多维度榜单失败：year={}, type={}, region={}, limit={}", year, type, region, limit, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取多维度榜单失败");
        }
    }

    /**
     * 基于评论 + 大模型分析的推荐接口。
     * 入参示例：
     * {
     *   "username": "user1",
     *   "movieName": "肖申克的救赎",
     *   "comment": "剧情很感人，节奏舒缓但不拖沓，结局非常治愈。",
     *   "limit": 5
     * }
     * 说明：
     * - 当前后端要求前端传入评论内容 comment；
     * - 返回列表中每一项包含推荐电影节点信息，reason 字段暂留（当前版本未填充文案）。
     */
    @PostMapping("/comment-based")
    public Result<List<CommentBasedRecommendationItem>> recommendByComment(@RequestBody Map<String, Object> body) {
        try {
            if (body == null) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "请求体不能为空");
            }
            Object usernameObj = body.get("username");
            Object movieNameObj = body.get("movieName");
            Object commentObj = body.get("comment");
            Object limitObj = body.get("limit");

            String username = usernameObj != null ? String.valueOf(usernameObj).trim() : null;
            String movieName = movieNameObj != null ? String.valueOf(movieNameObj).trim() : null;
            String comment = commentObj != null ? String.valueOf(commentObj).trim() : null;
            int limit = 10;
            if (limitObj instanceof Number) {
                limit = ((Number) limitObj).intValue();
            } else if (limitObj instanceof String) {
                try {
                    limit = Integer.parseInt((String) limitObj);
                } catch (NumberFormatException ignored) {
                }
            }
            if (limit <= 0) {
                limit = 10;
            }

            if (movieName == null || movieName.isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "movieName 不能为空");
            }
            if (comment == null || comment.isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "comment 不能为空");
            }

            List<CommentBasedRecommendationItem> items =
                    recommendationService.recommendByComment(username, movieName, comment, limit);
            return Result.success(items != null ? items : new java.util.ArrayList<>());
        } catch (Exception e) {
            log.error("基于评论的大模型推荐失败，请求体={}", body, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "基于评论推荐失败");
        }
    }

    /**
     * 基于评分预测的推荐接口：
     * - 系统根据其他用户对电影的评分，预测当前用户可能的评分；
     * - 返回可能高分的电影列表，每条包含预测评分、推荐理由和知识图谱关系摘要。
     */
    @GetMapping("/rating-prediction")
    public Result<List<RatingPredictionItem>> ratingPredictionRecommend(
            @RequestParam String username,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "username 不能为空");
            }
            if (limit == null || limit <= 0) {
                limit = 20;
            }
            List<RatingPredictionItem> items = recommendationService.ratingPredictionByUsername(username.trim(), limit);
            return Result.success(items != null ? items : new ArrayList<>());
        } catch (Exception e) {
            log.error("评分预测推荐失败：username={}, limit={}", username, limit, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "评分预测推荐失败");
        }
    }
}