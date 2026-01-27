package org.example.model;

import lombok.Data;

@Data
public class MovieCommentDTO {
    private Long 电影ID;
    private String 电影名;
    private Long 评论ID;
    private String 评论内容;
}