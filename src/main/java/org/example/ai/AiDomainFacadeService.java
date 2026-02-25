package org.example.ai;

import lombok.extern.slf4j.Slf4j;
import org.example.model.CollaborativeFilteringResult;
import org.example.model.MovieNode;
import org.example.model.MovieRecommendItem;
import org.example.model.RecommendIntent;
import org.example.repository.MovieRepository;
import org.example.response.Result;
import org.example.service.MovieRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI领域封装服务
 * 封装电影、评论、推荐等API调用，返回格式化的文本答案
 * 供 ArkIntegrationService 或 Controller 调用
 */
@Slf4j
@Service
public class AiDomainFacadeService {

    @Autowired
    private MovieRecommendationService movieRecommendationService;

    @Autowired
    private MovieRepository movieRepository;

    @Lazy
    @Autowired
    private ArkIntegrationService arkIntegrationService;

    /**
     * 构建协同过滤推荐答案
     * @param userId 用户ID/用户名
     * @return 格式化的推荐文本
     */
    public String buildRecommendAnswer(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "抱歉，需要提供用户ID才能为您推荐电影。";
        }

        try {
            CollaborativeFilteringResult result = movieRecommendationService.collaborativeFilteringRecommendByUsername(userId);
            
            if (result == null || CollectionUtils.isEmpty(result.getRecommendedMovies())) {
                return "抱歉，暂时没有找到适合您的推荐电影。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("根据您的观影偏好，为您推荐以下电影：\n\n");

            List<MovieNode> movies = result.getRecommendedMovies();
            int count = Math.min(movies.size(), 10);
            
            for (int i = 0; i < count; i++) {
                MovieNode movie = movies.get(i);
                sb.append((i + 1)).append(". ").append(formatMovieInfo(movie)).append("\n");
            }

            if (movies.size() > 10) {
                sb.append("\n... 还有 ").append(movies.size() - 10).append(" 部推荐电影");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("构建推荐答案失败：userId={}", userId, e);
            return "抱歉，获取推荐信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 根据推荐意图构建答案
     * @param intent 推荐意图
     * @return 格式化的推荐文本
     */
    public String buildRecommendAnswerByIntent(RecommendIntent intent) {
        if (intent == null || intent.getIntentType() == null) {
            return "抱歉，无法理解您的推荐需求。";
        }

        try {
            String intentType = intent.getIntentType();
            Map<String, Object> params = intent.getParams();
            List<MovieNode> movies;

            if ("director_based".equals(intentType) && params != null) {
                String director = (String) params.get("director");
                if (director != null && !director.isEmpty()) {
                    movies = movieRepository.findByDirectorName(director);
                    if (movies.size() > 10) {
                        movies = movies.subList(0, 10);
                    }
                    return formatMoviesAnswer(movies, "根据导演「" + director + "」为您推荐以下电影：");
                }
            } else if ("type_based".equals(intentType) && params != null) {
                String type = (String) params.get("type");
                if (type != null && !type.isEmpty()) {
                    movies = movieRepository.findByTypeOrderByRating(type, 10);
                    return formatMoviesAnswer(movies, "根据类型「" + type + "」为您推荐以下电影：");
                }
            } else if ("rating_based".equals(intentType)) {
                movies = movieRepository.findTopRatedMovies(10);
                return formatMoviesAnswer(movies, "为您推荐以下高分电影：");
            }

            // 默认返回高分电影
            movies = movieRepository.findTopRatedMovies(10);
            return formatMoviesAnswer(movies, "为您推荐以下高分电影：");
        } catch (Exception e) {
            log.error("根据意图构建推荐答案失败：intent={}", intent, e);
            return "抱歉，获取推荐信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 构建电影评论答案
     * @param movieName 电影名称
     * @return 格式化的评论文本
     */
    public String buildMovieCommentsAnswer(String movieName) {
        if (movieName == null || movieName.trim().isEmpty()) {
            return "抱歉，请提供电影名称。";
        }

        try {
            Result<List<Map<String, Object>>> commentResult = 
                movieRecommendationService.getMovieCommentsByMovieName(movieName);

            if (commentResult == null || commentResult.getCode() != 200 || 
                commentResult.getData() == null || commentResult.getData().isEmpty()) {
                return "抱歉，没有找到《" + movieName + "》的评论信息。";
            }

            List<Map<String, Object>> comments = commentResult.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("《").append(movieName).append("》的评论：\n\n");

            int count = Math.min(comments.size(), 5);
            for (int i = 0; i < count; i++) {
                Map<String, Object> comment = comments.get(i);
                sb.append((i + 1)).append(". ");

                if (comment.get("creator") != null) {
                    sb.append(comment.get("creator")).append("：");
                }

                if (comment.get("comment_rating") != null) {
                    sb.append("评分 ").append(comment.get("comment_rating")).append("分 - ");
                }

                if (comment.get("content") != null) {
                    String content = comment.get("content").toString();
                    if (content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }
                    sb.append(content);
                }

                sb.append("\n");
            }

            if (comments.size() > 5) {
                sb.append("\n... 还有 ").append(comments.size() - 5).append(" 条评论");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("构建电影评论答案失败：movieName={}", movieName, e);
            return "抱歉，获取评论信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 构建电影信息答案
     * @param movieName 电影名称
     * @return 格式化的电影信息文本
     */
    public String buildMovieInfoAnswer(String movieName) {
        if (movieName == null || movieName.trim().isEmpty()) {
            return "抱歉，请提供电影名称。";
        }

        try {
            List<MovieNode> movies = movieRepository.findByMovieNameContaining(movieName);
            
            if (CollectionUtils.isEmpty(movies)) {
                return "抱歉，没有找到《" + movieName + "》的相关信息。";
            }

            MovieNode movie = movies.get(0); // 取第一个匹配的电影
            StringBuilder sb = new StringBuilder();
            sb.append("《").append(movie.getMovieName() != null ? movie.getMovieName() : movieName).append("》\n\n");

            if (movie.getType() != null && !movie.getType().isEmpty()) {
                sb.append("类型：").append(movie.getType()).append("\n");
            }

            if (movie.getRegion() != null && !movie.getRegion().isEmpty()) {
                sb.append("地区：").append(movie.getRegion()).append("\n");
            }

            if (movie.getMovieRating() != null) {
                sb.append("评分：").append(String.format("%.1f", movie.getMovieRating())).append("分\n");
            }

            if (movie.getDirectorString() != null && !movie.getDirectorString().isEmpty()) {
                sb.append("导演：").append(movie.getDirectorString().replace("|", "、")).append("\n");
            }

            if (movie.getActorString() != null && !movie.getActorString().isEmpty()) {
                sb.append("演员：").append(movie.getActorString().replace("|", "、")).append("\n");
            }

            if (movie.getInstruction() != null && !movie.getInstruction().isEmpty()) {
                String instruction = movie.getInstruction();
                if (instruction.length() > 200) {
                    instruction = instruction.substring(0, 200) + "...";
                }
                sb.append("\n简介：").append(instruction);
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("构建电影信息答案失败：movieName={}", movieName, e);
            return "抱歉，获取电影信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 构建高分电影答案
     * @param limit 返回数量限制
     * @return 格式化的高分电影文本
     */
    public String buildHighRatingMoviesAnswer(int limit) {
        try {
            List<MovieNode> movies = movieRepository.findTopRatedMovies(limit);
            return formatMoviesAnswer(movies, "为您推荐以下高分电影：");
        } catch (Exception e) {
            log.error("构建高分电影答案失败：limit={}", limit, e);
            return "抱歉，获取高分电影信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 构建按类型推荐的答案
     * @param type 电影类型
     * @param limit 返回数量限制
     * @return 格式化的推荐文本
     */
    public String buildMoviesByTypeAnswer(String type, int limit) {
        if (type == null || type.trim().isEmpty()) {
            return "抱歉，请提供电影类型。";
        }

        try {
            List<MovieNode> movies = movieRepository.findByTypeOrderByRating(type, limit);
            return formatMoviesAnswer(movies, "「" + type + "」类型的电影推荐：");
        } catch (Exception e) {
            log.error("构建按类型推荐答案失败：type={}, limit={}", type, limit, e);
            return "抱歉，获取电影信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 构建按导演推荐的答案
     * @param director 导演名称
     * @param limit 返回数量限制
     * @return 格式化的推荐文本
     */
    public String buildMoviesByDirectorAnswer(String director, int limit) {
        if (director == null || director.trim().isEmpty()) {
            return "抱歉，请提供导演名称。";
        }

        try {
            List<MovieNode> movies = movieRepository.findByDirectorName(director);
            if (movies.size() > limit) {
                movies = movies.subList(0, limit);
            }
            return formatMoviesAnswer(movies, "导演「" + director + "」的作品推荐：");
        } catch (Exception e) {
            log.error("构建按导演推荐答案失败：director={}, limit={}", director, limit, e);
            return "抱歉，获取电影信息时出现错误，请稍后重试。";
        }
    }

    /**
     * 基于系统推荐API 的智能推荐，供 AI 整理用：返回「名字、简介」的纯文本。无简介时由 AI 生成并写入数据库。
     */
    public String buildRecommendationWithCommentsForAi(int limit) {
        try {
            List<MovieNode> movies = movieRepository.findTopRatedMovies(limit);
            if (CollectionUtils.isEmpty(movies)) {
                return "暂无高分电影数据。";
            }
            return buildMoviesWithIntroText(movies);
        } catch (Exception e) {
            log.error("构建推荐失败：limit={}", limit, e);
            return "获取推荐数据失败，请稍后重试。";
        }
    }

    public String buildRecommendationWithCommentsByTypeForAi(String type, int limit) {
        if (type == null || type.trim().isEmpty()) {
            return "请提供电影类型。";
        }
        try {
            List<MovieNode> movies = movieRepository.findByTypeOrderByRating(type, limit);
            if (CollectionUtils.isEmpty(movies)) {
                return "暂无该类型电影数据。";
            }
            return buildMoviesWithIntroText(movies);
        } catch (Exception e) {
            log.error("构建按类型推荐失败：type={}, limit={}", type, limit, e);
            return "获取推荐数据失败，请稍后重试。";
        }
    }

    public String buildRecommendationWithCommentsByDirectorForAi(String director, int limit) {
        if (director == null || director.trim().isEmpty()) {
            return "请提供导演名称。";
        }
        try {
            List<MovieNode> movies = movieRepository.findByDirectorName(director);
            if (movies.size() > limit) {
                movies = movies.subList(0, limit);
            }
            if (CollectionUtils.isEmpty(movies)) {
                return "暂无该导演的电影数据。";
            }
            return buildMoviesWithIntroText(movies);
        } catch (Exception e) {
            log.error("构建按导演推荐失败：director={}, limit={}", director, limit, e);
            return "获取推荐数据失败，请稍后重试。";
        }
    }

    /** 单次推荐请求中最多为几部电影调用 Ark 生成简介，避免 8 次串联导致超时 */
    private static final int MAX_INTRO_GENERATION_PER_REQUEST = 3;

    /**
     * 内部：为电影列表生成给 AI 的文本（仅名字、简介）。无简介时按配额生成并落库，避免单次请求多次 Ark 调用超时。
     */
    private String buildMoviesWithIntroText(List<MovieNode> movies) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(movies.size(), 10);
        int introQuota = MAX_INTRO_GENERATION_PER_REQUEST;
        for (int i = 0; i < count; i++) {
            MovieNode m = movies.get(i);
            if (introQuota > 0 && (m.getInstruction() == null || m.getInstruction().trim().isEmpty())) {
                ensureIntro(m);
                introQuota--;
            } else if (introQuota <= 0 && (m.getInstruction() == null || m.getInstruction().trim().isEmpty())) {
                // 本请求内不再调用 Ark，沿用空简介
            } else {
                // 已有简介，无需生成
            }
            String name = m.getMovieName() != null ? m.getMovieName() : "未知电影";
            String intro = m.getInstruction() != null ? m.getInstruction() : "";
            if (intro.length() > 200) {
                intro = intro.substring(0, 200) + "...";
            }
            sb.append("---\n片名：").append(name).append("\n简介：").append(intro.isEmpty() ? "暂无简介" : intro).append("\n");
        }
        if (movies.size() > 10) {
            sb.append("---\n（共 ").append(movies.size()).append(" 部，仅展示前10部）");
        }
        return sb.toString();
    }

    /**
     * 无简介时根据电影名等信息用 AI 生成简介并写入数据库
     */
    private void ensureIntro(MovieNode m) {
        if (m == null) return;
        String name = m.getMovieName() != null ? m.getMovieName() : "";
        if (name.isEmpty()) return;
        String intro = m.getInstruction();
        if (intro != null && !intro.trim().isEmpty()) return;
        try {
            String prompt = "请为电影《" + name + "》写一段50-150字的简介，风格随机。只输出简介正文，不要标题和引号。";
            String generated = arkIntegrationService.chatRaw(
                    Collections.singletonList(new ArkIntegrationService.ChatMessage("user", prompt)), null);
            if (generated != null && !generated.trim().isEmpty() && !generated.startsWith("抱歉")) {
                String trimmed = generated.trim();
                if (trimmed.length() > 500) trimmed = trimmed.substring(0, 500);
                m.setInstruction(trimmed);
                movieRepository.save(m);
                log.info("已为电影《{}》生成并保存简介", name);
            } else {
                log.debug("电影《{}》未写入简介：Ark 返回为空或异常提示", name);
            }
        } catch (Exception e) {
            log.warn("为电影《{}》生成简介失败，跳过：{}", name, e.getMessage());
        }
    }

    /**
     * 返回智能推荐列表，统一格式：index、movieName、intro、type、rating、director。无简介时会按配额生成并落库。
     */
    public List<MovieRecommendItem> getRecommendationWithComments(int limit) {
        List<MovieRecommendItem> list = new ArrayList<>();
        try {
            List<MovieNode> movies = movieRepository.findTopRatedMovies(limit);
            if (CollectionUtils.isEmpty(movies)) {
                return list;
            }
            int count = Math.min(movies.size(), 10);
            int introQuota = MAX_INTRO_GENERATION_PER_REQUEST;
            for (int i = 0; i < count; i++) {
                MovieNode m = movies.get(i);
                if (introQuota > 0 && (m.getInstruction() == null || m.getInstruction().trim().isEmpty())) {
                    ensureIntro(m);
                    introQuota--;
                }
                String name = m.getMovieName() != null ? m.getMovieName() : "未知电影";
                String intro = m.getInstruction() != null ? m.getInstruction() : "";
                if (intro.length() > 300) intro = intro.substring(0, 300) + "...";
                list.add(MovieRecommendItem.builder()
                        .index(i + 1)
                        .movieName(name)
                        .intro(intro)
                        .type(m.getType() != null ? m.getType() : "")
                        .rating(m.getMovieRating())
                        .director(m.getDirectorString() != null ? m.getDirectorString() : "")
                        .build());
            }
        } catch (Exception e) {
            log.error("getRecommendationWithComments 失败：limit={}", limit, e);
        }
        return list;
    }

    /**
     * 格式化电影列表答案（内部辅助方法）
     */
    private String formatMoviesAnswer(List<MovieNode> movies, String title) {
        if (CollectionUtils.isEmpty(movies)) {
            return "抱歉，没有找到相关电影。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n\n");

        int count = Math.min(movies.size(), 10);
        for (int i = 0; i < count; i++) {
            MovieNode movie = movies.get(i);
            sb.append((i + 1)).append(". ").append(formatMovieInfo(movie)).append("\n");
        }

        if (movies.size() > 10) {
            sb.append("\n... 还有 ").append(movies.size() - 10).append(" 部电影");
        }

        return sb.toString();
    }

    /**
     * 格式化单个电影信息（内部辅助方法）
     */
    private String formatMovieInfo(MovieNode movie) {
        if (movie == null) {
            return "未知电影";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(movie.getMovieName() != null ? movie.getMovieName() : "未知电影");

        if (movie.getType() != null && !movie.getType().isEmpty()) {
            sb.append(" (").append(movie.getType()).append(")");
        }

        if (movie.getMovieRating() != null) {
            sb.append(" - 评分: ").append(String.format("%.1f", movie.getMovieRating()));
        }

        if (movie.getDirectorString() != null && !movie.getDirectorString().isEmpty()) {
            sb.append(" - 导演: ").append(movie.getDirectorString().replace("|", "、"));
        }

        return sb.toString();
    }
}
