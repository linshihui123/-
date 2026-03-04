package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 基于评分预测的推荐结果项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingPredictionItem {

    /**
     * 推荐的电影节点（包含电影基本信息）
     */
    private MovieNode movie;

    /**
     * 预测该用户对电影的评分（例如 4.5 分）
     */
    private Double predictedRating;

    /**
     * 简要推荐理由（例如：预测评分 4.5 分，基于其他用户评分）
     */
    private String reason;

    /**
     * 该电影在知识图谱中的简要关系描述列表（类型、地区、导演、演员等）
     */
    private List<String> kgRelations;
}

