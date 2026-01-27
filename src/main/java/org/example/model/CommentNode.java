package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

// 评论节点（关联用户）
@NodeEntity(label = "Comment")
@Data
public class CommentNode {
    @Id
    @GeneratedValue
    private Long id;
    private Integer rating;
    private String content;
    private String creator; // 评论者（用户）
    private String TIME; // 评论时间
    private String ADD_TIME; // 添加时间
    private Integer MOVIEID; // 电影ID

    // 关系：评论-属于->用户
    @Relationship(type = "CREATED_BY", direction = Relationship.OUTGOING)
    private UserNode user;

    // 关系：评论-针对->电影
    @Relationship(type = "TARGETS", direction = Relationship.OUTGOING)
    private MovieNode movie;
}