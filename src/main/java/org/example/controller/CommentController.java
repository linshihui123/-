package org.example.controller;

import org.example.model.CommentNode;
import org.example.repository.CommentRepository;
import org.example.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@CrossOrigin(origins = "*")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    /**
     * 获取电影的评论列表
     */
    @GetMapping("/movie/{movieId}")
    public Result<List<CommentNode>> getCommentsByMovieId(@PathVariable Integer movieId) {
        try {
            List<CommentNode> comments = commentRepository.findByMovieId(movieId);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error("获取评论失败");
        }
    }

    /**
     * 获取用户的评论列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<CommentNode>> getCommentsByUserId(@PathVariable String userId) {
        try {
            List<CommentNode> comments = commentRepository.findByUserId(userId);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error("获取评论失败");
        }
    }

    /**
     * 添加评论
     */
    @PostMapping
    public Result<String> addComment(@RequestBody CommentNode comment) {
        try {
            commentRepository.save(comment);
            return Result.success("评论添加成功");
        } catch (Exception e) {
            return Result.error("评论添加失败");
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<String> deleteComment(@PathVariable Long commentId) {
        try {
            commentRepository.deleteById(commentId);
            return Result.success("评论删除成功");
        } catch (Exception e) {
            return Result.error("评论删除失败");
        }
    }
}