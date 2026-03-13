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

    /**
     * 当前用户对电影点赞
     * @param username 当前登录用户名（前端在登录后从本地存储中传入）
     * @param movieId  电影在图数据库中的ID
     */
    @PostMapping("/add")
    public Result<Boolean> addLike(@RequestParam String username,
                                   @RequestParam Long movieId) {
        try {
            boolean ok = likeService.addLike(username, movieId);
            if (!ok) {
                return Result.error(500, "创建点赞关系失败");
            }
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(500, "创建点赞关系失败: " + e.getMessage());
        }
    }

    /**
     * 取消当前用户对电影的点赞
     */
    @PostMapping("/remove")
    public Result<Boolean> removeLike(@RequestParam String username,
                                      @RequestParam Long movieId) {
        try {
            boolean ok = likeService.removeLike(username, movieId);
            return ok ? Result.success(true) : Result.error(500, "取消点赞失败");
        } catch (Exception e) {
            return Result.error(500, "取消点赞失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户是否已对指定电影点赞
     */
    @GetMapping("/is-liked")
    public Result<Boolean> isLiked(@RequestParam String username,
                                   @RequestParam Long movieId) {
        try {
            boolean liked = likeService.isLiked(username, movieId);
            return Result.success(liked);
        } catch (Exception e) {
            return Result.error(500, "查询点赞状态失败: " + e.getMessage());
        }
    }
}
