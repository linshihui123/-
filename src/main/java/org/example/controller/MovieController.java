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
     * 获取所有电影列表（分页支持）
     */
    @GetMapping("/list")
    public Result<List<MovieNode>> getAllMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return recommendationService.getAllMovies(page, size);
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