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
        List<MovieNode> allMovies = movieRepository.findAllMovies();
        List<MovieNode> movies = allMovies.size() > movieCount
                ? allMovies.subList(0, movieCount)
                : allMovies;
        return buildGraphFromMovies(movies);
    }

    /**
     * 按节点关键词搜索：电影名、导演、演员、地区、类型任一包含关键词即纳入，返回该子图的 nodes+edges
     */
    public Map<String, Object> getGraphDataByNodeKeyword(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getFullKnowledgeGraphData(limit);
        }
        List<MovieNode> movies = movieRepository.findMoviesByNodeKeyword(keyword.trim(), limit <= 0 ? 100 : limit);
        return buildGraphFromMovies(movies);
    }

    /** 根据电影列表构建图谱节点与边（供全量与按关键词搜索复用） */
    private Map<String, Object> buildGraphFromMovies(List<MovieNode> movies) {
        Map<String, Object> graphData = new HashMap<>();
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        Set<Map<String, Object>> edgeSet = new LinkedHashSet<>();
        Random rand = new Random();

        for (MovieNode movie : movies) {
            if (movie == null) continue;

            String movieNodeId = "movie_" + movie.getInfoId();
            Map<String, Object> movieNode = new HashMap<>();
            movieNode.put("id", movieNodeId);
            movieNode.put("name", movie.getMovieName());
            movieNode.put("type", "movie");
            movieNode.put("rating", movie.getMovieRating());
            movieNode.put("region", movie.getRegion());
            movieNode.put("x", 100 + rand.nextInt(800));
            movieNode.put("y", 200 + rand.nextInt(400));
            if (!nodeMap.containsKey(movieNodeId)) {
                nodeMap.put(movieNodeId, movieNode);
            }

            List<String> directors = movie.getDirectorList();
            if (directors != null) {
                for (String director : directors) {
                    if (director == null || director.trim().isEmpty()) continue;
                    String directorNodeId = "director_" + Math.abs(director.hashCode());
                    if (!nodeMap.containsKey(directorNodeId)) {
                        Map<String, Object> directorNode = new HashMap<>();
                        directorNode.put("id", directorNodeId);
                        directorNode.put("name", director);
                        directorNode.put("type", "director");
                        directorNode.put("x", 100 + rand.nextInt(800));
                        directorNode.put("y", 100 + rand.nextInt(400));
                        nodeMap.put(directorNodeId, directorNode);
                    }
                    Map<String, Object> directorEdge = new HashMap<>();
                    directorEdge.put("source", movieNodeId);
                    directorEdge.put("target", directorNodeId);
                    directorEdge.put("label", "导演");
                    edgeSet.add(directorEdge);
                }
            }

            List<String> actors = movie.getActorList();
            if (actors != null) {
                for (String actor : actors) {
                    if (actor == null || actor.trim().isEmpty()) continue;
                    String actorNodeId = "actor_" + Math.abs(actor.hashCode());
                    if (!nodeMap.containsKey(actorNodeId)) {
                        Map<String, Object> actorNode = new HashMap<>();
                        actorNode.put("id", actorNodeId);
                        actorNode.put("name", actor);
                        actorNode.put("type", "actor");
                        actorNode.put("x", 100 + rand.nextInt(800));
                        actorNode.put("y", 300 + rand.nextInt(400));
                        nodeMap.put(actorNodeId, actorNode);
                    }
                    Map<String, Object> actorEdge = new HashMap<>();
                    actorEdge.put("source", movieNodeId);
                    actorEdge.put("target", actorNodeId);
                    actorEdge.put("label", "主演");
                    edgeSet.add(actorEdge);
                }
            }

            String region = movie.getRegion();
            if (region != null && !region.isEmpty()) {
                String regionNodeId = "region_" + Math.abs(region.hashCode());
                if (!nodeMap.containsKey(regionNodeId)) {
                    Map<String, Object> regionNode = new HashMap<>();
                    regionNode.put("id", regionNodeId);
                    regionNode.put("name", region);
                    regionNode.put("type", "region");
                    regionNode.put("x", 100 + rand.nextInt(800));
                    regionNode.put("y", 400 + rand.nextInt(400));
                    nodeMap.put(regionNodeId, regionNode);
                }
                Map<String, Object> regionEdge = new HashMap<>();
                regionEdge.put("source", movieNodeId);
                regionEdge.put("target", regionNodeId);
                regionEdge.put("label", "地区");
                edgeSet.add(regionEdge);
            }

            String type = movie.getType();
            if (type != null && !type.isEmpty()) {
                String genreNodeId = "genre_" + Math.abs(type.hashCode());
                if (!nodeMap.containsKey(genreNodeId)) {
                    Map<String, Object> genreNode = new HashMap<>();
                    genreNode.put("id", genreNodeId);
                    genreNode.put("name", type);
                    genreNode.put("type", "genre");
                    genreNode.put("x", 100 + rand.nextInt(800));
                    genreNode.put("y", 500 + rand.nextInt(400));
                    nodeMap.put(genreNodeId, genreNode);
                }
                Map<String, Object> genreEdge = new HashMap<>();
                genreEdge.put("source", movieNodeId);
                genreEdge.put("target", genreNodeId);
                genreEdge.put("label", "类型");
                edgeSet.add(genreEdge);
            }
        }

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
                
                String directorMoviesCypher = "MATCH (m:Movie) WHERE m.director IS NOT NULL AND toLower(m.director) CONTAINS toLower($director) RETURN m";
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
                    
                    String actorDirectorMoviesCypher = "MATCH (m:Movie) WHERE m.actor IS NOT NULL AND toLower(m.actor) CONTAINS toLower($actor) AND m.director IS NOT NULL AND toLower(m.director) CONTAINS toLower($director) RETURN m";
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

    /**
     * 获取知识图谱统计信息：电影数、图中关系数（用于前端展示）
     */
    public Map<String, Integer> getGraphStats() {
        Map<String, Integer> stats = new HashMap<>();
        try {
            long movieCount = movieRepository.count();
            stats.put("movies", (int) Math.min(movieCount, Integer.MAX_VALUE));
            String countRelsCypher = "MATCH ()-[r]->() RETURN count(r) AS cnt";
            Iterable<Map<String, Object>> relResult = neo4jSession.query(countRelsCypher, Collections.emptyMap());
            int relations = 0;
            for (Map<String, Object> row : relResult) {
                Object cnt = row.get("cnt");
                if (cnt instanceof Number) relations = ((Number) cnt).intValue();
                break;
            }
            stats.put("relations", relations);
            stats.put("directors", 0);
            stats.put("actors", 0);
        } catch (Exception e) {
            stats.put("movies", 0);
            stats.put("directors", 0);
            stats.put("actors", 0);
            stats.put("relations", 0);
        }
        return stats;
    }
}