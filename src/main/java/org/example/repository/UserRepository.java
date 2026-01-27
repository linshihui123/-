package org.example.repository;

import org.example.model.User;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 这个可以考虑使用mysql数据库，也或者是直接使用Neo4j数据库
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {
    
    // 根据用户名查找用户
    Optional<User> findByUsername(String username);
    
    // 检查用户名是否存在
    @Query("MATCH (u:User) WHERE u.username = $username RETURN u")
    User findUserByUsername(@Param("username") String username);
}