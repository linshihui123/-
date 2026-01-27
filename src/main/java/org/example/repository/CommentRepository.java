package org.example.repository;

import org.example.model.CommentNode;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends Neo4jRepository<CommentNode, Long> {
    
    // 根据电影ID获取评论列表
    @Query("MATCH (c:Comment)-[:TARGETS]->(m:Movie) WHERE m.info_id = $movieId RETURN c")
    List<CommentNode> findByMovieId(@Param("movieId") Integer movieId);
    
    // 根据用户ID获取评论列表
    @Query("MATCH (c:Comment)-[:CREATED_BY]->(u:User) WHERE u.username = $userId RETURN c")
    List<CommentNode> findByUserId(@Param("userId") String userId);
    
    // 根据电影ID和评分获取评论
    @Query("MATCH (c:Comment)-[:TARGETS]->(m:Movie) WHERE m.info_id = $movieId AND c.rating >= $minRating RETURN c")
    List<CommentNode> findByMovieIdAndMinRating(@Param("movieId") Integer movieId, @Param("minRating") int minRating);
}