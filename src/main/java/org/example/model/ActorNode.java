package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

import java.math.BigDecimal;
import java.util.List;

// 演员节点
@NodeEntity(label = "Actor")
@Data
public class ActorNode {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    // 反向关系：演员-参演->电影
    @Relationship(type = "STARRED_IN", direction = Relationship.OUTGOING)
    private List<MovieNode> movies;

}