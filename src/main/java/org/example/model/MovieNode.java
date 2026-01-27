package org.example.model;

import lombok.Data;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import java.util.Arrays;
import java.util.List;

@NodeEntity(label = "Movie")
@Data
public class MovieNode {
    @Id
    @GeneratedValue
    private Long id;

    // 适配数据中的 "info_id" 字段
    @Property(name = "info_id")
    private Integer infoId;

    @Property(name = "name")
    private String movieName; // 电影名

    @Property(name = "type")
    private String type; // 电影类型

    // 修复：注解name="region" 对应变量名改为region（原direction）
    @Property(name = "region")
    private String region; // 地区（原字段名direction导致映射失败）

    @Property(name = "movie_rating")
    private Double movieRating; // 评分

    @Property(name = "instruction")
    private String instruction; // 简介

    @Property(name = "actor")
    private String actorString; // 演员（|分隔字符串）

    @Property(name = "director")
    private String directorString; // 导演（|分隔字符串）

    @Property(name = "characters")
    private String characters;

    // 关系定义（保留原逻辑）
    @Relationship(type = "DIRECTED_BY", direction = Relationship.OUTGOING)
    private List<DirectorNode> directors;

    @Relationship(type = "STARRED_BY", direction = Relationship.OUTGOING)
    private List<ActorNode> actors;

    @Relationship(type = "HAS_COMMENT", direction = Relationship.OUTGOING)
    private List<CommentNode> comments;

    // 解析导演列表（正确转义|）
    public List<String> getDirectorList() {
        return actorString != null && !actorString.isEmpty()
                ? Arrays.asList(directorString.split("\\|"))
                : null;
    }

    // 解析演员列表（正确转义|）
    public List<String> getActorList() {
        return actorString != null && !actorString.isEmpty()
                ? Arrays.asList(actorString.split("\\|"))
                : null;
    }
}