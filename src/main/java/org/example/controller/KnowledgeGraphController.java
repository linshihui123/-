package org.example.controller;

import org.example.model.MovieNode;
import org.example.response.Result;
import org.example.service.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/kg")
@CrossOrigin(origins = "*")
public class KnowledgeGraphController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    /**
     * 获取知识图谱数据。支持按节点搜索：传 keyword 时返回包含该关键词的子图（电影名/导演/演员/地区/类型匹配），否则按 movieCount 返回全量子集。
     */
    @GetMapping("/graph-data")
    public Result<Map<String, Object>> getFullGraphData(
            @RequestParam(defaultValue = "100") int movieCount,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String nodeTypes) {

        Map<String, Object> graphData;
        if (keyword != null && !keyword.trim().isEmpty()) {
            graphData = knowledgeGraphService.getGraphDataByNodeKeyword(keyword.trim(), Math.max(1, movieCount));
        } else {
            graphData = knowledgeGraphService.getFullKnowledgeGraphData(movieCount);
        }
        return Result.success(graphData);
    }
    
    /**
     * 为了兼容前端旧接口路径，新增此方法
     */
    @GetMapping("/data")
    public Result<Map<String, Object>> getGraphData(
            @RequestParam(defaultValue = "10") int movieCount) {
        
        Map<String, Object> graphData = knowledgeGraphService.getFullKnowledgeGraphData(movieCount);
        
        return Result.success(graphData);
    }
    
    /**
     * 获取知识图谱统计信息（电影数、关系数等，从 Neo4j 统计）
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getGraphStats() {
        Map<String, Integer> stats = knowledgeGraphService.getGraphStats();
        return Result.success(stats);
    }

    /**
     * 基于用户点赞的电影推荐：喜欢的电影 → 同导演 → 其他电影
     */
    @GetMapping("/recommend/director")
    public Result<List<MovieNode>> recommendMoviesByDirector(
            @RequestParam String username,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<MovieNode> recommendedMovies = knowledgeGraphService.recommendMoviesByDirector(username, limit);
            return Result.success(recommendedMovies);
        } catch (Exception e) {
            return Result.error(500, "获取推荐失败: " + e.getMessage());
        }
    }

    /**
     * 基于用户点赞的电影推荐：喜欢的电影 → 同类型 → 同地区 → 其他电影
     */
    @GetMapping("/recommend/type-region")
    public Result<List<MovieNode>> recommendMoviesByTypeAndRegion(
            @RequestParam String username,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<MovieNode> recommendedMovies = knowledgeGraphService.recommendMoviesByTypeAndRegion(username, limit);
            return Result.success(recommendedMovies);
        } catch (Exception e) {
            return Result.error(500, "获取推荐失败: " + e.getMessage());
        }
    }

    /**
     * 基于用户点赞的电影推荐：喜欢的电影 → 同演员 → 同导演 → 其他电影
     */
    @GetMapping("/recommend/actor-director")
    public Result<List<MovieNode>> recommendMoviesByActorAndDirector(
            @RequestParam String username,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<MovieNode> recommendedMovies = knowledgeGraphService.recommendMoviesByActorAndDirector(username, limit);
            return Result.success(recommendedMovies);
        } catch (Exception e) {
            return Result.error(500, "获取推荐失败: " + e.getMessage());
        }
    }
}