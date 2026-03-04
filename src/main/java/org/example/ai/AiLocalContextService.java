package org.example.ai;

import org.example.model.MovieNode;
import org.example.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 为大模型对话构建「本地数据链」上下文的服务。
 * 根据用户问题和可选 userId，从本地电影/点赞等数据中抽取一小段文本，
 * 作为系统提示注入到大模型对话中。
 */
@Service
public class AiLocalContextService {

    @Autowired
    private MovieRepository movieRepository;

    /**
     * 根据用户问题和可选用户ID构建本地上下文。
     *
     * @param question 用户当前问题（最后一条 user 消息）
     * @param userId   可选用户标识（目前未直接用于 Neo4j 查询，预留扩展）
     * @return 可注入到大模型对话中的文本上下文，可能为空字符串
     */
    public String buildContext(String question, String userId) {
        String q = question != null ? question.trim() : "";

        // 简单关键词识别
        boolean wantHighScore = containsAny(q, "高分", "评分高", "口碑好", "top", "排行榜");
        boolean wantSciFi = containsAny(q, "科幻", "sci-fi", "science fiction");
        boolean wantComedy = containsAny(q, "喜剧", "搞笑");
        boolean wantHorror = containsAny(q, "恐怖", "惊悚");
        boolean wantLove = containsAny(q, "爱情", "恋爱", "romance");

        // 如果没有明确意图，也给一部分高分电影，避免上下文为空
        if (!wantHighScore && !wantSciFi && !wantComedy && !wantHorror && !wantLove) {
            wantHighScore = true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("下面是系统当前已有的部分电影数据（来自本地数据库），你回答用户问题时应优先基于这些数据进行推荐：\n\n");

        int maxItemsPerSection = 8;

        // 1) 高分电影
        if (wantHighScore) {
            List<MovieNode> highList = safeTopRatedMovies(20);
            appendMoviesSection(sb, "高分电影", highList, maxItemsPerSection);
        }

        // 2) 按类型
        appendTypeSectionIfNeeded(sb, "科幻", wantSciFi, maxItemsPerSection);
        appendTypeSectionIfNeeded(sb, "喜剧", wantComedy, maxItemsPerSection);
        appendTypeSectionIfNeeded(sb, "恐怖", wantHorror, maxItemsPerSection);
        appendTypeSectionIfNeeded(sb, "爱情", wantLove, maxItemsPerSection);

        // 3) 预留：用户点赞电影（目前仅在 likeService 注入成功时使用用户名）
        // 由于 AI 助手当前只拿到 userId（数字），而 Neo4j User 使用 name 字段，
        // 这里暂不根据 userId 查询点赞数据，避免误匹配。后续如有用户名映射可在此扩展。

        String context = sb.toString();
        // 简单长度控制，避免 prompt 过长
        int maxLen = 1600;
        if (context.length() > maxLen) {
            context = context.substring(0, maxLen) + "\n……（其余电影略）\n";
        }
        return context;
    }

    private List<MovieNode> safeTopRatedMovies(int limit) {
        try {
            List<MovieNode> list = movieRepository.findTopRatedMovies(limit);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void appendTypeSectionIfNeeded(StringBuilder sb, String type, boolean needed, int maxItems) {
        if (!needed) return;
        try {
            List<MovieNode> list = movieRepository.findByTypeOrderByRating(type, maxItems * 2);
            appendMoviesSection(sb, type + "类型电影", list, maxItems);
        } catch (Exception ignored) {
        }
    }

    private void appendMoviesSection(StringBuilder sb, String title, List<MovieNode> movies, int maxItems) {
        if (CollectionUtils.isEmpty(movies)) {
            return;
        }
        sb.append("【").append(title).append("】（节选）：\n");
        int count = 0;
        for (MovieNode m : movies) {
            if (m == null) continue;
            String name = firstNonEmpty(m.getMovieName(), "未知电影");
            String type = firstNonEmpty(m.getType(), "未知类型");
            String region = firstNonEmpty(m.getRegion(), "未知地区");
            String directors = m.getDirectorString() != null ? m.getDirectorString().replace("|", "、") : "";
            Double rating = m.getMovieRating();
            sb.append("- ").append(name)
                    .append("（类型：").append(type)
                    .append("，地区：").append(region);
            if (rating != null) {
                sb.append("，评分：").append(String.format(Locale.CHINA, "%.1f", rating));
            }
            if (!directors.isEmpty()) {
                sb.append("，导演：").append(directors);
            }
            sb.append("）\n");
            count++;
            if (count >= maxItems) break;
        }
        sb.append("\n");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty() || keywords == null) return false;
        for (String k : keywords) {
            if (k != null && !k.isEmpty() && text.toLowerCase().contains(k.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "";
    }
}

