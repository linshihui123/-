package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.CommentNode;
import org.example.model.MovieNode;
import org.example.repository.CommentRepository;
import org.example.repository.MovieRepository;
import org.example.response.Result;
import org.example.response.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MovieRepository movieRepository;

    /**
     * 根据电影ID获取评论列表
     */
    public Result<List<CommentNode>> getCommentsByMovieId(Integer movieId) {
        try {
            List<CommentNode> comments = commentRepository.findByMovieId(movieId);
            return Result.success(comments);
        } catch (Exception e) {
            log.error("获取电影评论失败：movieId={}", movieId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影评论失败");
        }
    }

    /**
     * 根据用户ID获取评论列表
     */
    public Result<List<CommentNode>> getCommentsByUserId(String userId) {
        try {
            List<CommentNode> comments = commentRepository.findByUserId(userId);
            return Result.success(comments);
        } catch (Exception e) {
            log.error("获取用户评论失败：userId={}", userId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取用户评论失败");
        }
    }

    /**
     * 根据电影ID和最低评分获取评论
     */
    public Result<List<CommentNode>> getCommentsByMovieIdAndMinRating(Integer movieId, int minRating) {
        try {
            List<CommentNode> comments = commentRepository.findByMovieIdAndMinRating(movieId, minRating);
            return Result.success(comments);
        } catch (Exception e) {
            log.error("获取电影高分评论失败：movieId={}, minRating={}", movieId, minRating, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取电影高分评论失败");
        }
    }

    /**
     * 添加评论
     */
    public Result<CommentNode> addComment(CommentNode comment) {
        try {
            // 验证评论数据
            if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评论内容不能为空");
            }
            
            if (comment.getRating() == null || comment.getRating() < 0 || comment.getRating() > 10) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评分应在0-10之间");
            }
            
            if (comment.getMovieId() == null || comment.getMovieId() == 0) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "电影ID不能为空或0");
            }

            // 设置时间字段
            String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            comment.setCommentTime(currentTime);
            comment.setAddTime(currentTime);

            // 查找电影节点并建立关系
            Optional<MovieNode> movieOpt = movieRepository.findByInfoId(comment.getMovieId());
            if (movieOpt.isPresent()) {
                MovieNode movie = movieOpt.get();
                System.out.println("找到电影节点：" + movie);
                
                // 建立电影到评论的关系
                if (movie.getComments() == null) {
                    movie.setComments(new ArrayList<>());
                }
                movie.getComments().add(comment);
                movieRepository.save(movie);
                System.out.println("已建立电影到评论的关系");
            } else {
                System.out.println("未找到电影节点，电影ID：" + comment.getMovieId());
            }

            System.out.println("保存评论：" + comment);
            CommentNode savedComment = commentRepository.save(comment);
            System.out.println("保存成功：" + savedComment);
            return Result.success(savedComment);
        } catch (Exception e) {
            log.error("添加评论失败：{}", e.getMessage(), e);
            e.printStackTrace();
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "添加评论失败");
        }
    }

    /**
     * 更新评论
     */
    public Result<CommentNode> updateComment(Long commentId, CommentNode updatedComment) {
        try {
            Optional<CommentNode> existingCommentOpt = commentRepository.findById(commentId);
            
            if (!existingCommentOpt.isPresent()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评论不存在");
            }

            CommentNode existingComment = existingCommentOpt.get();
            
            // 更新评论内容
            if (updatedComment.getContent() != null) {
                existingComment.setContent(updatedComment.getContent());
            }
            
            if (updatedComment.getRating() != null) {
                if (updatedComment.getRating() < 0 || updatedComment.getRating() > 10) {
                    return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评分应在0-10之间");
                }
                existingComment.setRating(updatedComment.getRating());
            }
            
            if (updatedComment.getCreator() != null) {
                existingComment.setCreator(updatedComment.getCreator());
            }

            CommentNode savedComment = commentRepository.save(existingComment);
            return Result.success(savedComment);
        } catch (Exception e) {
            log.error("更新评论失败：commentId={}, {}", commentId, e.getMessage(), e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "更新评论失败");
        }
    }

    /**
     * 删除评论
     */
    public Result<String> deleteComment(Long commentId) {
        try {
            Optional<CommentNode> commentOpt = commentRepository.findById(commentId);
            
            if (!commentOpt.isPresent()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评论不存在");
            }

            commentRepository.deleteById(commentId);
            return Result.success("评论删除成功");
        } catch (Exception e) {
            log.error("删除评论失败：commentId={}, {}", commentId, e.getMessage(), e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "删除评论失败");
        }
    }

    /**
     * 根据评论ID获取评论详情
     */
    public Result<CommentNode> getCommentById(Long commentId) {
        try {
            Optional<CommentNode> commentOpt = commentRepository.findById(commentId);
            
            if (!commentOpt.isPresent()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评论不存在");
            }

            return Result.success(commentOpt.get());
        } catch (Exception e) {
            log.error("获取评论详情失败：commentId={}", commentId, e);
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "获取评论详情失败");
        }
    }

    /**
     * 提交电影评论
     */
    public Result<String> submitMovieComment(CommentNode comment) {
        try {
            // 验证评论数据
            if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评论内容不能为空");
            }
            
            if (comment.getRating() == null || (comment.getRating() < 0 || comment.getRating() > 10)) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "评分应在0-10之间");
            }
            
            if (comment.getMovieId() == null || comment.getMovieId() == 0) {
                return Result.error(ResultCodeEnum.PARAM_ERROR.getCode(), "电影ID不能为空或0");
            }

            // 设置时间字段
            String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            comment.setCommentTime(currentTime);
            comment.setAddTime(currentTime);

            // 查找电影节点并建立关系
            Optional<MovieNode> movieOpt = movieRepository.findByInfoId(comment.getMovieId());
            if (movieOpt.isPresent()) {
                MovieNode movie = movieOpt.get();
                System.out.println("找到电影节点：" + movie);
                
                // 建立电影到评论的关系
                if (movie.getComments() == null) {
                    movie.setComments(new ArrayList<>());
                }
                movie.getComments().add(comment);
                movieRepository.save(movie);
                System.out.println("已建立电影到评论的关系");
            } else {
                System.out.println("未找到电影节点，电影ID：" + comment.getMovieId());
            }

            System.out.println("保存评论：" + comment);
            commentRepository.save(comment);
            System.out.println("保存成功：" + comment);
            return Result.success("评论提交成功");
        } catch (Exception e) {
            log.error("提交电影评论失败：{}", e.getMessage(), e);
            e.printStackTrace();
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.getCode(), "提交电影评论失败");
        }
    }
}