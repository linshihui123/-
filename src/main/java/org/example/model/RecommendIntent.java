package org.example.model;

import lombok.Data;

import java.util.Map;
@Data
public  class RecommendIntent {
    private String intentType; // 意图类型：director_based/type_based/rating_based
    private Map<String, Object> params; // 意图参数：如{director: "张艺谋", type: "喜剧", min_rating: 8}

    // JDK 1.8必须显式提供getter/setter（Lombok需确认版本兼容）
    public String getIntentType() {
        return intentType;
    }

    public void setIntentType(String intentType) {
        this.intentType = intentType;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}