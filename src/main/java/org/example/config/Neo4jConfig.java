package org.example.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;

@Configuration
public class Neo4jConfig {

    @Value("${spring.neo4j.uri:bolt://localhost:7687}")
    private String neo4jUri;

    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String neo4jUsername;

    @Value("${spring.neo4j.authentication.password:password}")
    private String neo4jPassword;

    // Neo4j驱动配置
    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
    }

    // Neo4j OGM配置
    @Bean
    public org.neo4j.ogm.config.Configuration ogmConfiguration() {
        return new org.neo4j.ogm.config.Configuration.Builder()
                .uri(neo4jUri)
                .credentials(neo4jUsername, neo4jPassword)
                // 连接池优化
                .connectionPoolSize(10)
                .build();
    }

    @Bean
    public SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration config) {
        // 指定实体扫描包
        return new SessionFactory(config, "org.example.model");
    }

    @Bean
    public Session neo4jSession(SessionFactory sessionFactory) {
        return sessionFactory.openSession();
    }

    // 事务管理器
    @Bean
    public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new Neo4jTransactionManager(sessionFactory);
    }
}