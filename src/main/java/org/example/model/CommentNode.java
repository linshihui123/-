package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

// 评论节点（关联用户）
@NodeEntity(label = "Comment")
@Data
public class CommentNode {
    @Id
    @GeneratedValue
    private Long id;
    
    @Property(name = "comment_rating")
    private Integer rating;
    
    @Property(name = "content")
    private String content;
    
    @Property(name = "creator")
    private String creator; // 评论者（用户）
    
    @Property(name = "comment_time")
    private String TIME; // 评论时间
    
    @Property(name = "comment_add_time")
    private String ADD_TIME; // 添加时间
    
    @Property(name = "movie_id")
    private Integer movieId; // 电影ID

    // 关系：评论-属于->用户
    @Relationship(type = "CREATED_BY", direction = Relationship.OUTGOING)
    private UserNode user;

    // 关系：评论-针对->电影
    @Relationship(type = "HAS_COMMENT", direction = Relationship.OUTGOING)
    private MovieNode movie;
}