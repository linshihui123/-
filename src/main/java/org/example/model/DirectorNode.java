package org.example.model;
import lombok.Data;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

import java.util.List;

// 导演节点
@NodeEntity(label = "Director")
@Data
public class DirectorNode {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    // 反向关系：导演-执导->电影
    @Relationship(type = "DIRECTED", direction = Relationship.OUTGOING)
    private List<MovieNode> movies;
}