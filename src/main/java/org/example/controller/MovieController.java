package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.model.MovieNode;
import org.example.model.CommentNode;
import org.example.model.RecommendIntent;
import org.example.response.Result;
import org.example.service.MovieRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/movie")
@CrossOrigin(origins = "*") // 跨域支持
public class MovieController {

    @Autowired
    private MovieRecommendationService recommendationService;

    /**
     * 获取电影推荐
     */
    @GetMapping("/recommend")
    public Result<List<MovieNode>> recommend(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "10") Integer topN,
            @RequestParam(required = false) String intentType,
            @RequestParam(required = false) String intentParams) {
        // 为了兼容前端请求，创建一个RecommendIntent对象
        RecommendIntent intent = null;
        if (intentType != null || intentParams != null) {
            intent = new RecommendIntent();
            intent.setIntentType(intentType != null ? intentType : "unknown");
            // 这里可以根据intentParams字符串解析参数，但目前简化处理
        }
        return recommendationService.recommendMovies(userId, topN, intent);
    }

    /**
     * POST方式获取电影推荐（支持请求体）
     */
    @PostMapping("/recommend")
    public Result<List<MovieNode>> recommendPost(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "10") Integer topN,
            @RequestBody(required = false) RecommendIntent intent) {
        return recommendationService.recommendMovies(userId, topN, intent);
    }

    /**
     * 获取所有电影列表（分页支持）
     */
    @GetMapping("/list")
    public Result<List<MovieNode>> getAllMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recommendationService.getAllMovies(page, size);
    }

    /**
     * 根据ID获取电影详情
     */
    @GetMapping("/{id}")
    public Result<MovieNode> getMovieById(@PathVariable Long id) {
        return recommendationService.getMovieById(id);
    }

    /**
     * 根据电影ID获取电影详情（通过movie_id字段）
     */
    @GetMapping("/by-movie-id/{movieId}")
    public Result<MovieNode> getMovieByMovieId(@PathVariable Integer movieId) {
        return recommendationService.getMovieByMovieId(movieId);
    }

    /**
     * 搜索电影（按名称、类型、导演、演员等）
     */
    @GetMapping("/search")
    public Result<List<MovieNode>> searchMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) String actor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recommendationService.searchMovies(keyword, type, director, actor, page, size);
    }

    /**
     * 获取热门电影（高评分电影）
     */
    @GetMapping("/top-rated")
    public Result<List<MovieNode>> getTopRatedMovies(@RequestParam(defaultValue = "10") int count) {
        return recommendationService.getTopRatedMovies(count);
    }

    /**
     * 获取最新电影
     */
    @GetMapping("/latest")
    public Result<List<MovieNode>> getLatestMovies(@RequestParam(defaultValue = "10") int count) {
        return recommendationService.getLatestMovies(count);
    }

    /**
     * 根据类型获取电影
     */
    @GetMapping("/by-type/{type}")
    public Result<List<MovieNode>> getMoviesByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recommendationService.getMoviesByType(type, page, size);
    }

    
    /**
     * 根据电影ID获取评论列表
     */
    @GetMapping("/by-movie-id/{movieId}/comments")
    public Result<List<CommentNode>> getCommentsByMovieId(@PathVariable Integer movieId) {
        return recommendationService.getCommentsByMovieId(movieId);
    }
    
    /**
     * 获取所有电影和评论的详细信息
     */
    @GetMapping("/all-movie-comments")
    public Result<List<Map<String, Object>>> getAllMovieComments() {
        return recommendationService.getAllMovieComments();
    }
    
    /**
     * 根据电影ID获取特定电影的评论信息
     */
    @GetMapping("/movie-comments/{movieId}")
    public Result<List<Map<String, Object>>> getMovieCommentsByMovieId(@PathVariable Integer movieId) {
        return recommendationService.getMovieCommentsByMovieId(movieId);
    }
    
    /**
     * 获取有评论的电影列表
     */
    @GetMapping("/movies-with-comments")
    public Result<List<MovieNode>> getMoviesWithComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recommendationService.getMoviesWithComments(page, size);
    }
}