package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("rating")
    @Property(name = "comment_rating")
    private Integer rating;
    
    @JsonProperty("content")
    @Property(name = "content")
    private String content;
    
    @JsonProperty("creator")
    @Property(name = "creator")
    private String creator; // 评论者（用户）
    
    @JsonProperty("commentTime")
    @Property(name = "comment_time")
    private String commentTime; // 评论时间
    
    @Property(name = "comment_add_time")
    private String addTime; // 添加时间
    
    @JsonProperty("movieId")
    @Property(name = "movie_id")
    private Integer movieId; // 电影ID

    // 关系：评论-属于->用户
    @Relationship(type = "CREATED_BY", direction = Relationship.OUTGOING)
    private UserNode user;

    // 关系：电影-有评论->评论
    @Relationship(type = "HAS_COMMENT", direction = Relationship.INCOMING)
    private MovieNode movie;

    // 自定义toString方法，避免递归调用
    @Override
    public String toString() {
        return "CommentNode{" +
                "id=" + id +
                ", movieId=" + movieId +
                ", rating=" + rating +
                ", content='" + content + '\'' +
                ", creator='" + creator + '\'' +
                ", commentTime='" + commentTime + '\'' +
                '}';
    }

    // 自定义hashCode方法，避免递归调用
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // 自定义equals方法，避免递归调用
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CommentNode that = (CommentNode) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }
}