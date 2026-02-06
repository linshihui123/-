package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Property;

// 用户-电影点赞关系
@RelationshipEntity(type = "LIKED")
@Data
public class LikeRel {
    @Id
    @GeneratedValue
    private Long id;
    
    @StartNode
    private UserNode user;
    
    @EndNode
    private MovieNode movie;
    
    @Property(name = "like_time")
    private String likeTime; // 点赞时间
}
