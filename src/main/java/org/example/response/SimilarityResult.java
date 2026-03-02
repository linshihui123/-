package org.example.response;

public class SimilarityResult {
    private final String userId;
    private final double similarity;
    /** 该相似用户在共同评分电影上的平均分，便于在「相似用户匹配度」旁展示 */
    private final Double avgRating;

    public SimilarityResult(String userId, double similarity) {
        this(userId, similarity, null);
    }

    public SimilarityResult(String userId, double similarity, Double avgRating) {
        this.userId = userId;
        this.similarity = similarity;
        this.avgRating = avgRating;
    }

    public String getUserId() {
        return userId;
    }

    public double getSimilarity() {
        return similarity;
    }

    public Double getAvgRating() {
        return avgRating;
    }
}
