package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基于评论 + 大模型分析的推荐结果项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentBasedRecommendationItem {

    /**
     * 推荐的电影节点（包含电影基本信息）
     */
    private MovieNode movie;

    /**
     * 大模型给出的简要推荐理由（可选，用于前端展示）
     */
    private String reason;

    /**
     * 该电影在知识图谱中的简要关系描述列表（类型、地区、导演、演员等）
     */
    private java.util.List<String> kgRelations;
}


