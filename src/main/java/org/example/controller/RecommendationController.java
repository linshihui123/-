package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.MovieNode;
import org.example.response.Result;
import org.example.service.MovieRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping("/collaborative-filtering/{userId}")
    public Result<List<MovieNode>> collaborativeFilteringRecommend(@PathVariable Integer userId) {
        try {
            List<MovieNode> recommendations = recommendationService.collaborativeFilteringRecommend(userId);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("协同过滤推荐失败：userId={}", userId, e);
            return Result.error(org.example.response.ResultCodeEnum.SYSTEM_ERROR.getCode(), "协同过滤推荐失败");
        }
    }

    /**
     * 基于用户评分的协同过滤推荐（无需指定用户，使用默认用户或从会话中获取）
     */
    @GetMapping("/collaborative-filtering")
    public Result<List<MovieNode>> collaborativeFilteringRecommendDefault() {
        try {
            Integer defaultUserId = 1;
            List<MovieNode> recommendations = recommendationService.collaborativeFilteringRecommend(defaultUserId);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("协同过滤推荐失败", e);
            return Result.error(org.example.response.ResultCodeEnum.SYSTEM_ERROR.getCode(), "协同过滤推荐失败");
        }
    }
}