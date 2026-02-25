package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能推荐单条结果，统一返回格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRecommendItem {
    /** 序号，从 1 开始 */
    private Integer index;
    /** 电影名 */
    private String movieName;
    /** 简介（无则空字符串） */
    private String intro;
    /** 类型 */
    private String type;
    /** 评分 */
    private Double rating;
    /** 导演（多个用 | 分隔） */
    private String director;
}
