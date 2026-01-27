package org.example.service;

import org.example.model.MovieNode;
import org.example.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class KnowledgeGraphService {
    @Autowired
    private MovieRepository movieRepository;

    // 返回前端图谱需要的nodes+edges数据
    public Map<String, Object> getFullKnowledgeGraphData(int movieCount) {
        Map<String, Object> graphData = new HashMap<>();
        List<MovieNode> allMovies = movieRepository.findAllMovies();
        // 限制返回的电影数量（避免节点过多）
        List<MovieNode> movies = allMovies.size() > movieCount
                ? allMovies.subList(0, movieCount)
                : allMovies;

        // 存储所有节点（去重，使用ID作为key）
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        // 存储所有边（关系）
        Set<Map<String, Object>> edgeSet = new LinkedHashSet<>();

        // 遍历每个电影，构建节点和边
        for (MovieNode movie : movies) {
            if (movie == null) continue;

            // 1. 构建电影节点（核心：用infoId+电影名做ID，更直观）
            String movieNodeId = "movie_" + movie.getInfoId();
            Map<String, Object> movieNode = new HashMap<>();
            movieNode.put("id", movieNodeId);
            movieNode.put("name", movie.getMovieName()); // 电影名：人在囧途
            movieNode.put("type", "movie");
            movieNode.put("rating", movie.getMovieRating()); // 评分：7.5
            movieNode.put("region", movie.getRegion()); // 地区：中国大陆
            movieNode.put("x", 100 + new Random().nextInt(800)); // 随机X坐标
            movieNode.put("y", 200 + new Random().nextInt(400)); // 随机Y坐标
            
            // 只有当ID不重复时才添加
            if (!nodeMap.containsKey(movieNodeId)) {
                nodeMap.put(movieNodeId, movieNode);
            }

            // 2. 构建导演节点 + 电影→导演的边
            List<String> directors = movie.getDirectorList();
            for (String director : directors) {
                if (director == null || director.trim().isEmpty()) continue;
                
                String directorNodeId = "director_" + Math.abs(director.hashCode()); // 使用绝对值避免负数
                Map<String, Object> directorNode = new HashMap<>();
                directorNode.put("id", directorNodeId);
                directorNode.put("name", director); // 导演名：叶伟民
                directorNode.put("type", "director");
                directorNode.put("x", 100 + new Random().nextInt(800));
                directorNode.put("y", 100 + new Random().nextInt(400));
                
                // 只有当ID不重复时才添加
                if (!nodeMap.containsKey(directorNodeId)) {
                    nodeMap.put(directorNodeId, directorNode);
                }

                // 电影→导演的边（关系：导演）
                Map<String, Object> directorEdge = new HashMap<>();
                directorEdge.put("source", movieNodeId); // 电影节点ID
                directorEdge.put("target", directorNodeId); // 导演节点ID
                directorEdge.put("label", "导演"); // 关系标签
                edgeSet.add(directorEdge);
            }

            // 3. 构建演员节点 + 电影→演员的边
            List<String> actors = movie.getActorList();
            for (String actor : actors) {
                if (actor == null || actor.trim().isEmpty()) continue;
                
                String actorNodeId = "actor_" + Math.abs(actor.hashCode()); // 使用绝对值避免负数
                Map<String, Object> actorNode = new HashMap<>();
                actorNode.put("id", actorNodeId);
                actorNode.put("name", actor); // 演员名：徐峥/王宝强
                actorNode.put("type", "actor");
                actorNode.put("x", 100 + new Random().nextInt(800));
                actorNode.put("y", 300 + new Random().nextInt(400));
                
                // 只有当ID不重复时才添加
                if (!nodeMap.containsKey(actorNodeId)) {
                    nodeMap.put(actorNodeId, actorNode);
                }

                // 电影→演员的边（关系：主演）
                Map<String, Object> actorEdge = new HashMap<>();
                actorEdge.put("source", movieNodeId); // 电影节点ID
                actorEdge.put("target", actorNodeId); // 演员节点ID
                actorEdge.put("label", "主演"); // 关系标签
                edgeSet.add(actorEdge);
            }

            // 4. 构建地区节点 + 电影→地区的边（可选，按需添加）
            String region = movie.getRegion();
            if (region != null && !region.isEmpty()) {
                String regionNodeId = "region_" + Math.abs(region.hashCode());
                Map<String, Object> regionNode = new HashMap<>();
                regionNode.put("id", regionNodeId);
                regionNode.put("name", region); // 地区：中国大陆
                regionNode.put("type", "region");
                regionNode.put("x", 100 + new Random().nextInt(800));
                regionNode.put("y", 400 + new Random().nextInt(400));
                
                // 只有当ID不重复时才添加
                if (!nodeMap.containsKey(regionNodeId)) {
                    nodeMap.put(regionNodeId, regionNode);
                }

                // 电影→地区的边
                Map<String, Object> regionEdge = new HashMap<>();
                regionEdge.put("source", movieNodeId);
                regionEdge.put("target", regionNodeId);
                regionEdge.put("label", "地区");
                edgeSet.add(regionEdge);
            }
        }

        // 组装返回数据（前端需要nodes和edges）
        graphData.put("nodes", new ArrayList<>(nodeMap.values()));
        graphData.put("edges", new ArrayList<>(edgeSet));
        return graphData;
    }
}