package org.example.controller;

import org.example.model.ActorNode;
import org.example.model.DirectorNode;
import org.example.model.MovieNode;
import org.example.repository.MovieRepository;
import org.example.response.Result;
import org.example.service.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/kg")
@CrossOrigin(origins = "*")
public class KnowledgeGraphController {

    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    /**
     * 获取完整的知识图谱数据
     */
    @GetMapping("/graph-data")
    public Result<Map<String, Object>> getFullGraphData(
            @RequestParam(defaultValue = "100") int movieCount,
            @RequestParam(defaultValue = "all") String nodeTypes) {
        
        Map<String, Object> graphData = knowledgeGraphService.getFullKnowledgeGraphData(movieCount);
        
        return Result.success(graphData);
    }
    
    /**
     * 为了兼容前端旧接口路径，新增此方法
     */
    @GetMapping("/data")
    public Result<Map<String, Object>> getGraphData(
            @RequestParam(defaultValue = "10") int movieCount) {
        
        Map<String, Object> graphData = knowledgeGraphService.getFullKnowledgeGraphData(movieCount);
        
        return Result.success(graphData);
    }
    
    /**
     * 获取知识图谱统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getGraphStats() {
        // 这里简单返回一个示例统计数据，实际应用中需要从数据库获取
        Map<String, Integer> stats = new HashMap<>();
        stats.put("movies", 0);
        stats.put("directors", 0);
        stats.put("actors", 0);
        stats.put("relations", 0);
        
        return Result.success(stats);
    }
}