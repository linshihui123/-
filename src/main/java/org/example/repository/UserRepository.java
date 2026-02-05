package org.example.repository;

import org.example.model.UserNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓库接口
 * 使用Neo4j图形数据库存储用户信息
 */
@Repository
public interface UserRepository extends Neo4jRepository<UserNode, Long> {
    
    // 根据用户名查找用户
    Optional<UserNode> findByUsername(String username);
    
    // 检查用户名是否存在
    @Query("MATCH (u:User) WHERE u.username = $username RETURN u")
    UserNode findUserByUsername(@Param("username") String username);
    
    // 删除用户
    @Query("MATCH (u:User) WHERE u.id = $userId DETACH DELETE u")
    void deleteUserById(@Param("userId") Long userId);
}