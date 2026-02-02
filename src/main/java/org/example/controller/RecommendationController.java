package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.CollaborativeFilteringResult;
import org.example.model.MovieNode;
import org.example.response.Result;
import org.example.response.ResultCodeEnum;
import org.example.service.MovieRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/recommendation")
@CrossOrigin(origins = "*") // 跨域支持
public class RecommendationController {

    @Autowired
    private MovieRecommendationService recommendationService;

    /**
     * 基于用户评分的协同过滤推荐
     */
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
     * 基于评论文本的兴趣推荐
     */
    //TODO:这个接口传递给AI，AI处理后返回结果
    @GetMapping("/movie-comments")
    public Result<List<Map<String, Object>>> getAllMovieComments() {
        try {
            List<Map<String, Object>> movieComments = (List<Map<String, Object>>) recommendationService.getAllMovieComments();
            return Result.success(movieComments);
        } catch (Exception e) {
            log.error("获取所有电影评论失败", e);
            return Result.error(org.example.response.ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取所有电影评论失败");
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
     * 展示单部电影的评分分布（如 1 分占比 10%、4 分占比 50%）；
     */
    @GetMapping("/movie-ratings/{movieName}")
    public Result<List<Map<String, Object>>> getMovieRatings(@PathVariable String movieName) {
        List<Map<String, Object>> movieRatings = (List<Map<String, Object>>) recommendationService.getMovieRatings(movieName);
        return Result.success(movieRatings);
    }
    /**
     * 按类型 / 地区统计电影的平均评分、平均评论数
     */

    @GetMapping("/movie-ratings-by-type")
    public Result<List<Map<String, Object>>> getMovieRatingsByType() {
        try {
            List<Map<String, Object>> typeStats = recommendationService.getMovieRatingsByType();
            
            return Result.success(typeStats != null ? typeStats : new ArrayList<>());
        } catch (Exception e) {
            log.error("获取类型统计信息失败", e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取类型统计信息失败");
        }
    }
    
    @GetMapping("/movie-ratings-by-region")
    public Result<List<Map<String, Object>>> getMovieRatingsByRegion() {
        try {
            List<Map<String, Object>> regionStats = recommendationService.getMovieRatingsByRegion();
            
            return Result.success(regionStats != null ? regionStats : new ArrayList<>());
        } catch (Exception e) {
            log.error("获取区域统计信息失败", e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取区域统计信息失败");
        }
    }
    /**
     *
     * 计算电影间的评分相似度（如电影 X 和 Y 的评分分布高度相似），结合知识图谱（同导演 / 演员）强化推荐
     * //TODO:问AI说某部电影的评分是多少，同类型的相同评分，或者同导演，演员等
     */
}