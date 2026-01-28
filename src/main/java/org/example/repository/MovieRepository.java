package org.example.repository;

import org.example.model.MovieNode;
import org.example.model.CommentNode;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends Neo4jRepository<MovieNode, Long> {
    
    // 根据movieId查找电影
    Optional<MovieNode> findByInfoId(Integer infoId);
    
    // 根据电影名称搜索（模糊匹配）
    @Query("MATCH (m:Movie) WHERE toLower(m.name) CONTAINS toLower($keyword) RETURN m")
    List<MovieNode> findByMovieNameContaining(@Param("keyword") String keyword);
    
    // 根据类型查找电影
    List<MovieNode> findByType(String type);
    
    // 根据评分排序获取电影列表
    @Query("MATCH (m:Movie) WHERE m.movie_rating IS NOT NULL RETURN m ORDER BY m.movie_rating DESC LIMIT $limit")
    List<MovieNode> findTopRatedMovies(@Param("limit") int limit);
    
    // 根据类型和评分排序获取电影
    @Query("MATCH (m:Movie) WHERE m.type = $type RETURN m ORDER BY m.movie_rating DESC LIMIT $limit")
    List<MovieNode> findByTypeOrderByRating(@Param("type") String type, @Param("limit") int limit);
    
    // 根据导演名称查找电影（处理字符串字段）
    @Query("MATCH (m:Movie) WHERE m.director CONTAINS $directorName RETURN m")
    List<MovieNode> findByDirectorName(@Param("directorName") String directorName);
    
    // 根据演员名称查找电影（处理字符串字段）
    @Query("MATCH (m:Movie) WHERE m.actor CONTAINS $actorName RETURN m")
    List<MovieNode> findByActorName(@Param("actorName") String actorName);
    
    // 使用新的查询方法获取完整的图数据
    @Query("MATCH (m:Movie) RETURN m")
    List<MovieNode> findAllMovies();

    // 根据多个条件搜索电影
    @Query("MATCH (m:Movie) " +
           "WHERE ($keyword IS NULL OR toLower(m.name) CONTAINS toLower($keyword)) " +
           "AND ($type IS NULL OR m.type = $type) " +
           "RETURN m ORDER BY m.movie_rating DESC SKIP $skip LIMIT $size"
            )
    List<MovieNode> searchMovies(@Param("keyword") String keyword, 
                                @Param("type") String type, 
                                @Param("skip") int skip, 
                                @Param("size") int size);

    // 获取有评论的电影列表（分页）
    @Query("MATCH (m:Movie)-[:HAS_COMMENT]->(c:Comment) RETURN DISTINCT m SKIP $skip LIMIT $size")
    List<MovieNode> findMoviesWithCommentsPaginated(@Param("skip") int skip, @Param("size") int size);

    @Query("MATCH (m:Movie) RETURN m SKIP $skip LIMIT $size")
    List<MovieNode> findOtherAllMovies(@Param("skip") int skip, @Param("size") int size);
}