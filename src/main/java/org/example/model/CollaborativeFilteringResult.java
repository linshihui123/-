package org.example.model;

import lombok.Data;
import org.example.response.SimilarityResult;

import java.util.List;

/**
 * 协同过滤推荐结果DTO：包含推荐电影、用户评分电影、相似用户相似度
 */
@Data
public class CollaborativeFilteringResult {
    /** 为当前用户推荐的电影列表 */
    private List<MovieNode> recommendedMovies;
    /** 当前用户已评分的电影列表（含评分信息） */
    private List<RatedMovieDTO> userRatedMovies;
    /** 相似用户及对应余弦相似度 */
    private List<SimilarityResult> similarityResults;

    /**
     * 用户已评分电影DTO（扩展MovieNode，增加评分字段）
     */
    @Data
    public static class RatedMovieDTO {
        private MovieNode movie;
        private Integer rating; // 用户对该电影的评分
    }
}

