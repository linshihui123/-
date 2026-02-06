package org.example.service;

import org.example.model.MovieNode;
import org.example.repository.MovieRepository;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private Session neo4jSession;

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

            // 5. 构建类型节点 + 电影→类型的边
            String type = movie.getType();
            if (type != null && !type.isEmpty()) {
                String genreNodeId = "genre_" + Math.abs(type.hashCode());
                Map<String, Object> genreNode = new HashMap<>();
                genreNode.put("id", genreNodeId);
                genreNode.put("name", type); // 类型：喜剧
                genreNode.put("type", "genre");
                genreNode.put("x", 100 + new Random().nextInt(800));
                genreNode.put("y", 500 + new Random().nextInt(400));
                
                // 只有当ID不重复时才添加
                if (!nodeMap.containsKey(genreNodeId)) {
                    nodeMap.put(genreNodeId, genreNode);
                }

                // 电影→类型的边
                Map<String, Object> genreEdge = new HashMap<>();
                genreEdge.put("source", movieNodeId);
                genreEdge.put("target", genreNodeId);
                genreEdge.put("label", "类型");
                edgeSet.add(genreEdge);
            }
        }

        // 组装返回数据（前端需要nodes和edges）
        graphData.put("nodes", new ArrayList<>(nodeMap.values()));
        graphData.put("edges", new ArrayList<>(edgeSet));
        return graphData;
    }

    /**
     * 基于用户点赞的电影推荐：喜欢的电影 → 同导演 → 其他电影
     */
    public List<MovieNode> recommendMoviesByDirector(String username, int limit) {
        try {
            // 1. 获取用户喜欢的电影
            String likedMoviesCypher = "MATCH (u:User)-[:LIKED]->(m:Movie) WHERE u.username = $username RETURN m";
            Map<String, Object> params = new HashMap<>();
            params.put("username", username);
            Iterable<MovieNode> likedMovies = neo4jSession.query(MovieNode.class, likedMoviesCypher, params);
            List<MovieNode> likedMovieList = new ArrayList<>();
            likedMovies.forEach(likedMovieList::add);

            if (likedMovieList.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 提取这些电影的导演
            Set<String> directors = new HashSet<>();
            for (MovieNode movie : likedMovieList) {
                if (movie.getDirectorList() != null) {
                    directors.addAll(movie.getDirectorList());
                }
            }

            if (directors.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. 查找这些导演的其他电影（排除用户已喜欢的）
            List<MovieNode> recommendedMovies = new ArrayList<>();
            Set<Long> likedMovieIds = likedMovieList.stream().map(MovieNode::getId).collect(Collectors.toSet());

            for (String director : directors) {
                if (director == null || director.trim().isEmpty()) continue;
                
                String directorMoviesCypher = "MATCH (m:Movie) WHERE ANY(d IN m.directorList WHERE d CONTAINS $director) RETURN m";
                Map<String, Object> directorParams = new HashMap<>();
                directorParams.put("director", director);
                Iterable<MovieNode> directorMovies = neo4jSession.query(MovieNode.class, directorMoviesCypher, directorParams);
                
                for (MovieNode movie : directorMovies) {
                    if (movie != null && !likedMovieIds.contains(movie.getId())) {
                        recommendedMovies.add(movie);
                    }
                }
            }

            // 去重并限制数量
            return recommendedMovies.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 基于用户点赞的电影推荐：喜欢的电影 → 同类型 → 同地区 → 其他电影
     */
    public List<MovieNode> recommendMoviesByTypeAndRegion(String username, int limit) {
        try {
            // 1. 获取用户喜欢的电影
            String likedMoviesCypher = "MATCH (u:User)-[:LIKED]->(m:Movie) WHERE u.username = $username RETURN m";
            Map<String, Object> params = new HashMap<>();
            params.put("username", username);
            Iterable<MovieNode> likedMovies = neo4jSession.query(MovieNode.class, likedMoviesCypher, params);
            List<MovieNode> likedMovieList = new ArrayList<>();
            likedMovies.forEach(likedMovieList::add);

            if (likedMovieList.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 提取这些电影的类型和地区
            Set<String> types = new HashSet<>();
            Set<String> regions = new HashSet<>();
            for (MovieNode movie : likedMovieList) {
                if (movie.getType() != null && !movie.getType().isEmpty()) {
                    types.add(movie.getType());
                }
                if (movie.getRegion() != null && !movie.getRegion().isEmpty()) {
                    regions.add(movie.getRegion());
                }
            }

            if (types.isEmpty() || regions.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. 查找同类型同地区的其他电影（排除用户已喜欢的）
            List<MovieNode> recommendedMovies = new ArrayList<>();
            Set<Long> likedMovieIds = likedMovieList.stream().map(MovieNode::getId).collect(Collectors.toSet());

            for (String type : types) {
                for (String region : regions) {
                    String typeRegionMoviesCypher = "MATCH (m:Movie) WHERE m.type = $type AND m.region = $region RETURN m";
                    Map<String, Object> typeRegionParams = new HashMap<>();
                    typeRegionParams.put("type", type);
                    typeRegionParams.put("region", region);
                    Iterable<MovieNode> typeRegionMovies = neo4jSession.query(MovieNode.class, typeRegionMoviesCypher, typeRegionParams);
                    
                    for (MovieNode movie : typeRegionMovies) {
                        if (movie != null && !likedMovieIds.contains(movie.getId())) {
                            recommendedMovies.add(movie);
                        }
                    }
                }
            }

            // 去重并限制数量
            return recommendedMovies.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 基于用户点赞的电影推荐：喜欢的电影 → 同演员 → 同导演 → 其他电影
     */
    public List<MovieNode> recommendMoviesByActorAndDirector(String username, int limit) {
        try {
            // 1. 获取用户喜欢的电影
            String likedMoviesCypher = "MATCH (u:User)-[:LIKED]->(m:Movie) WHERE u.username = $username RETURN m";
            Map<String, Object> params = new HashMap<>();
            params.put("username", username);
            Iterable<MovieNode> likedMovies = neo4jSession.query(MovieNode.class, likedMoviesCypher, params);
            List<MovieNode> likedMovieList = new ArrayList<>();
            likedMovies.forEach(likedMovieList::add);

            if (likedMovieList.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 提取这些电影的演员和导演
            Set<String> actors = new HashSet<>();
            Set<String> directors = new HashSet<>();
            for (MovieNode movie : likedMovieList) {
                if (movie.getActorList() != null) {
                    actors.addAll(movie.getActorList());
                }
                if (movie.getDirectorList() != null) {
                    directors.addAll(movie.getDirectorList());
                }
            }

            if (actors.isEmpty() || directors.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. 查找这些演员和导演合作的其他电影（排除用户已喜欢的）
            List<MovieNode> recommendedMovies = new ArrayList<>();
            Set<Long> likedMovieIds = likedMovieList.stream().map(MovieNode::getId).collect(Collectors.toSet());

            for (String actor : actors) {
                if (actor == null || actor.trim().isEmpty()) continue;
                
                for (String director : directors) {
                    if (director == null || director.trim().isEmpty()) continue;
                    
                    String actorDirectorMoviesCypher = "MATCH (m:Movie) WHERE ANY(a IN m.actorList WHERE a CONTAINS $actor) AND ANY(d IN m.directorList WHERE d CONTAINS $director) RETURN m";
                    Map<String, Object> adParams = new HashMap<>();
                    adParams.put("actor", actor);
                    adParams.put("director", director);
                    Iterable<MovieNode> adMovies = neo4jSession.query(MovieNode.class, actorDirectorMoviesCypher, adParams);
                    
                    for (MovieNode movie : adMovies) {
                        if (movie != null && !likedMovieIds.contains(movie.getId())) {
                            recommendedMovies.add(movie);
                        }
                    }
                }
            }

            // 去重并限制数量
            return recommendedMovies.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}