package org.example.ai;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FlowResponse {
    // 出参1：推荐的电影列表（字符串数组）
    @SerializedName("recommendMovies")
    private List<String> recommendMovies;
    // 出参2：推荐理由（字符串）
    @SerializedName("reason")
    private String reason;
    // 工作流通用出参：执行状态（success/fail），Coze工作流默认返回
    @SerializedName("status")
    private String status;
    // 工作流通用出参：错误信息（失败时非空，成功时为null/空字符串）
    @SerializedName("error_msg")
    private String errorMsg;

    // 所有出参的Getter（核心，用于获取工作流返回结果）
    public List<String> getRecommendMovies() {
        return recommendMovies;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
