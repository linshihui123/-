package org.example.ai;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public  class FlowRequest {
    // 入参1：要推荐的电影名（字符串类型）
    @SerializedName("movieName")
    private String movieName;

    // 构造器（入参字段全参）
    public FlowRequest(String movieName) {
        this.movieName = movieName;
    }

    // Getter/Setter（按需添加，Gson序列化无需，但业务中可能需要）
    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }
}
