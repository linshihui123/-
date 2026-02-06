package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.LikeRel;
import org.example.model.MovieNode;
import org.example.model.UserNode;
import org.example.repository.MovieRepository;
import org.example.repository.UserRepository;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LikeService {

    @Autowired
    private Session neo4jSession;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    /**
     * 为所有用户随机补充点赞记录
     */
    public int generateRandomLikes() {
        try {
            // 获取所有用户
            Iterable<UserNode> users = userRepository.findAll();
            List<UserNode> userList = new ArrayList<>();
            users.forEach(userList::add);
            
            if (userList.isEmpty()) {
                log.warn("没有找到用户，无法生成点赞记录");
                return 0;
            }
            
            // 获取所有电影
            Iterable<MovieNode> movies = movieRepository.findAll();
            List<MovieNode> movieList = new ArrayList<>();
            movies.forEach(movieList::add);
            
            if (movieList.isEmpty()) {
                log.warn("没有找到电影，无法生成点赞记录");
                return 0;
            }
            
            // 为每个用户生成随机点赞记录
            int totalLikes = 0;
            Random random = new Random();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (UserNode user : userList) {
                // 为每个用户随机选择1-5部电影进行点赞
                int likeCount = random.nextInt(5) + 1;
                Set<Integer> selectedMovieIndices = new HashSet<>();
                
                while (selectedMovieIndices.size() < likeCount && selectedMovieIndices.size() < movieList.size()) {
                    selectedMovieIndices.add(random.nextInt(movieList.size()));
                }
                
                // 为选中的电影创建点赞关系
                for (Integer index : selectedMovieIndices) {
                    MovieNode movie = movieList.get(index);
                    
                    // 检查是否已经有点赞关系
                    if (!hasLiked(user, movie)) {
                        LikeRel likeRel = new LikeRel();
                        likeRel.setUser(user);
                        likeRel.setMovie(movie);
                        likeRel.setLikeTime(sdf.format(new Date()));
                        
                        // 保存点赞关系
                        neo4jSession.save(likeRel);
                        totalLikes++;
                        
                        log.info("为用户 {} 生成对电影 {} 的点赞记录", user.getUsername(), movie.getMovieName());
                    }
                }
            }
            
            log.info("成功为用户生成 {} 条点赞记录", totalLikes);
            return totalLikes;
        } catch (Exception e) {
            log.error("生成随机点赞记录失败", e);
            return 0;
        }
    }

    /**
     * 检查用户是否已经点赞了电影
     */
    private boolean hasLiked(UserNode user, MovieNode movie) {
        try {
            String cypher = "MATCH (u:User)-[:LIKED]->(m:Movie) WHERE ID(u) = $userId AND ID(m) = $movieId RETURN count(*) as count";
            Map<String, Object> params = new HashMap<>();
            params.put("userId", user.getId());
            params.put("movieId", movie.getId());
            
            Iterable<Map<String, Object>> result = neo4jSession.query(cypher, params);
            if (result.iterator().hasNext()) {
                Map<String, Object> row = result.iterator().next();
                long count = ((Number) row.get("count")).longValue();
                return count > 0;
            }
            return false;
        } catch (Exception e) {
            log.error("检查点赞关系失败", e);
            return false;
        }
    }

    /**
     * 获取用户的点赞记录
     */
    public List<MovieNode> getUserLikedMovies(String username) {
        try {
            String cypher = "MATCH (u:User)-[:LIKED]->(m:Movie) WHERE u.username = $username RETURN m";
            Map<String, Object> params = new HashMap<>();
            params.put("username", username);
            
            Iterable<MovieNode> result = neo4jSession.query(MovieNode.class, cypher, params);
            List<MovieNode> likedMovies = new ArrayList<>();
            result.forEach(likedMovies::add);
            
            return likedMovies;
        } catch (Exception e) {
            log.error("获取用户点赞记录失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取电影的点赞数
     */
    public long getMovieLikeCount(Long movieId) {
        try {
            String cypher = "MATCH (u:User)-[:LIKED]->(m:Movie) WHERE ID(m) = $movieId RETURN count(*) as count";
            Map<String, Object> params = new HashMap<>();
            params.put("movieId", movieId);
            
            Iterable<Map<String, Object>> result = neo4jSession.query(cypher, params);
            if (result.iterator().hasNext()) {
                Map<String, Object> row = result.iterator().next();
                return ((Number) row.get("count")).longValue();
            }
            return 0;
        } catch (Exception e) {
            log.error("获取电影点赞数失败", e);
            return 0;
        }
    }
}
