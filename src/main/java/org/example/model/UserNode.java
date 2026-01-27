package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity(label = "User")
@Data
public class UserNode{
    @Id
    @GeneratedValue
    private Long id;
    private Long userId;
    private String username;
    private String password;
}