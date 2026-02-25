package org.example.controller;

import org.example.model.MovieNode;
import org.example.response.Result;
import org.example.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/like")
public class LikeController {

    @Autowired
    private LikeService likeService;

    /**
     * 为所有用户随机生成点赞记录
     */
    @PostMapping("/generate-random")
    public Result<Integer> generateRandomLikes() {
        try {
            int count = likeService.generateRandomLikes();
            Result<Integer> result = Result.success(count);
            result.setMsg("成功生成" + count + "条点赞记录");
            return result;
        } catch (Exception e) {
            return Result.error(500, "生成点赞记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取有点赞记录的用户名列表（用于知识图谱推荐时选择用户）
     */
    @GetMapping("/users-with-likes")
    public Result<List<String>> getUsernamesWithLikes() {
        try {
            List<String> usernames = likeService.getUsernamesWithLikes();
            return Result.success(usernames);
        } catch (Exception e) {
            return Result.error(500, "获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定用户的点赞电影列表（基于用户点赞记录）
     */
    @GetMapping("/liked-movies")
    public Result<List<MovieNode>> getUserLikedMovies(@RequestParam String username) {
        try {
            List<MovieNode> list = likeService.getUserLikedMovies(username);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "获取点赞记录失败: " + e.getMessage());
        }
    }
}
