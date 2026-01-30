package org.example.response;

public  class SimilarityResult {
    private final String userId;
    private final double similarity;

    public SimilarityResult(String userId, double similarity) {
        this.userId = userId;
        this.similarity = similarity;
    }

    public String getUserId() {
        return userId;
    }

    public double getSimilarity() {
        return similarity;
    }
}
