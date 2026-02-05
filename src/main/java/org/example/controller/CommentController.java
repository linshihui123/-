package org.example.controller;

import org.example.model.CommentNode;
import org.example.repository.CommentRepository;
import org.example.response.Result;
import org.example.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentService commentService;

    /**
     * 获取电影的评论列表
     */
    @GetMapping("/comment/movie/{movieId}")
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
    @GetMapping("/comment/user/{userId}")
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
    @PostMapping("/comment")
    public Result<String> addComment(@RequestBody CommentNode comment) {
        try {
            commentRepository.save(comment);
            return Result.success("评论添加成功");
        } catch (Exception e) {
            return Result.error("评论添加失败");
        }
    }

    /**
     * 提交评论（匹配前端路径）
     */
    @PostMapping("/movie/comment/submit")
    public Result<String> submitComment(@RequestBody CommentNode comment) {
        try {
            Result<CommentNode> result = commentService.addComment(comment);
            if (result.getCode() == 200) {
                return Result.success("评论提交成功");
            } else {
                return Result.error(result.getMsg());
            }
        } catch (Exception e) {
            return Result.error("评论提交失败");
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{commentId}")
    public Result<String> deleteComment(@PathVariable Long commentId) {
        try {
            commentRepository.deleteById(commentId);
            return Result.success("评论删除成功");
        } catch (Exception e) {
            return Result.error("评论删除失败");
        }
    }

    /**
     * 获取所有评论创建者
     */
    @GetMapping("/comment/creators")
    public Result<List<String>> getAllCreators() {
        try {
            List<String> creators = commentRepository.findAllCreators();
            return Result.success(creators);
        } catch (Exception e) {
            return Result.error("获取评论创建者失败");
        }
    }
}