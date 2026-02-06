package org.example.controller;

import org.example.model.ActorNode;
import org.example.model.DirectorNode;
import org.example.model.MovieNode;
import org.example.repository.MovieRepository;
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
    private MovieRepository movieRepository;
    
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    /**
     * 获取完整的知识图谱数据
     */
    @GetMapping("/graph-data")
    public Result<Map<String, Object>> getFullGraphData(
            @RequestParam(defaultValue = "100") int movieCount,
            @RequestParam(defaultValue = "all") String nodeTypes) {
        
        Map<String, Object> graphData = knowledgeGraphService.getFullKnowledgeGraphData(movieCount);
        
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
     * 获取知识图谱统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getGraphStats() {
        // 这里简单返回一个示例统计数据，实际应用中需要从数据库获取
        Map<String, Integer> stats = new HashMap<>();
        stats.put("movies", 0);
        stats.put("directors", 0);
        stats.put("actors", 0);
        stats.put("relations", 0);
        
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