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
    public Result<List<MovieNode>> collaborativeFilteringRecommend(@PathVariable String userId) {
        try {
            List<MovieNode> recommendations = recommendationService.collaborativeFilteringRecommendByUsername(userId);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("协同过滤推荐失败：userId={}", userId, e);
            return Result.error(org.example.response.ResultCodeEnum.SYSTEM_ERROR.getCode(), "协同过滤推荐失败");
        }
    }

    /**
     * 基于用户评分的协同过滤推荐（可选：通过查询参数传入用户ID）
     */
    @GetMapping("/collaborative-filtering")
    public Result<List<MovieNode>> collaborativeFilteringRecommendByQuery(@RequestParam(required = false) String userId) {
        try {
            String username = userId != null && !userId.trim().isEmpty() ? userId : "default_user";
            List<MovieNode> recommendations = recommendationService.collaborativeFilteringRecommendByUsername(username);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("协同过滤推荐失败：userId={}", userId, e);
            return Result.error(org.example.response.ResultCodeEnum.SYSTEM_ERROR.getCode(), "协同过滤推荐失败");
        }
    }
}