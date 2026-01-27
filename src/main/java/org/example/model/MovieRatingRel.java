package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

// 用户-电影评分关系（带属性）
@RelationshipEntity(type = "RATED")
@Data
public class MovieRatingRel {
    @Id
    @GeneratedValue
    private Long id;
    @StartNode
    private UserNode user;
    @EndNode
    private MovieNode movie;
    private Integer rating; // 评分属性
}