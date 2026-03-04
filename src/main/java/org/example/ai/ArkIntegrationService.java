package org.example.ai;

import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * 火山方舟集成服务
 */
@Slf4j
@Service
public class ArkIntegrationService {
    
    @Value("${ark.api.key:9c4f4931-78f0-464c-b3f8-a9677535f15c}")
    private String arkApiKey;
    
    @Value("${ark.api.url:https://ark.cn-beijing.volces.com/api/v3}")
    private String arkApiUrl;
    
    @Value("${ark.model:doubao-seed-1-6-250615}")
    private String arkModel;
    
    private ArkService arkService;
    
    @PostConstruct
    public void init() {
        try {
            arkService = ArkService.builder()
                    .apiKey(arkApiKey)
                    .baseUrl(arkApiUrl)
                    .build();
            log.info("火山方舟服务初始化成功");
        } catch (Exception e) {
            log.error("火山方舟服务初始化失败", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (arkService != null) {
            try {
                arkService.shutdownExecutor();
                log.info("火山方舟服务已关闭");
            } catch (Exception e) {
                log.error("关闭火山方舟服务失败", e);
            }
        }
    }
    
    /**
     * 发送对话消息
     * @param messages 对话历史消息列表，每个消息包含role和content
     * @return AI回复内容
     */
    public String chat(List<ChatMessage> messages) {
        if (arkService == null) {
            throw new RuntimeException("火山方舟服务未初始化");
        }
        
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        
        try {
            // 构建请求消息列表
            List<ItemEasyMessage> requestMessages = new ArrayList<>();
            for (ChatMessage msg : messages) {
                String role = msg.getRole();
                // 转换角色为SDK常量
                String sdkRole;
                if ("user".equalsIgnoreCase(role)) {
                    sdkRole = ResponsesConstants.MESSAGE_ROLE_USER;
                } else if ("assistant".equalsIgnoreCase(role)) {
                    sdkRole = ResponsesConstants.MESSAGE_ROLE_ASSISTANT;
                } else if ("system".equalsIgnoreCase(role)) {
                    sdkRole = ResponsesConstants.MESSAGE_ROLE_SYSTEM;
                } else {
                    sdkRole = ResponsesConstants.MESSAGE_ROLE_USER; // 默认角色
                }
                
                MessageContent content = MessageContent.builder()
                        .addListItem(InputContentItemText.builder().text(msg.getContent()).build())
                        .build();
                
                ItemEasyMessage message = ItemEasyMessage.builder()
                        .role(sdkRole)
                        .content(content)
                        .build();
                
                requestMessages.add(message);
            }
            
            // 构建请求 - 根据SDK示例构建
            CreateResponsesRequest.Builder requestBuilder = CreateResponsesRequest.builder()
                    .model(arkModel);
            
            ResponsesInput.Builder inputBuilder = ResponsesInput.builder();
            for (ItemEasyMessage msg : requestMessages) {
                inputBuilder.addListItem(msg);
            }
            
            CreateResponsesRequest request = requestBuilder
                    .input(inputBuilder.build())
                    .build();

            
            // 调用API
            ResponseObject response = arkService.createResponse(request);
            
            // 解析响应 - 火山方舟 output 为 List<ItemReasoning|ItemOutputMessage>，需从 ItemOutputMessage 中取 output_text
            if (response != null) {
                try {
                    Object output = response.getOutput();
                    if (output instanceof List) {
                        List<?> outputList = (List<?>) output;
                        for (Object item : outputList) {
                            if (item == null) continue;
                            String type = ArkIntegrationService.invokeGetter(item, "getType");
                            if (!"message".equals(type)) continue;
                            String role = ArkIntegrationService.invokeGetter(item, "getRole");
                            if (!"assistant".equals(role)) continue;
                            Object contentObj = ArkIntegrationService.invokeGetterMethod(item, "getContent");
                            if (contentObj instanceof List) {
                                List<?> contentList = (List<?>) contentObj;
                                for (Object part : contentList) {
                                    if (part == null) continue;
                                    String partType = ArkIntegrationService.invokeGetter(part, "getType");
                                    if ("output_text".equals(partType)) {
                                        String text = ArkIntegrationService.invokeGetter(part, "getText");
                                        if (text != null && !text.isEmpty()) {
                                            return text;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析火山方舟 output 列表失败", e);
                }
            }
            
            log.warn("火山方舟返回数据格式异常，无法解析响应内容");
            return "抱歉，AI服务暂时无法响应，请稍后重试。";
        } catch (Exception e) {
            log.error("调用火山方舟API失败", e);
            throw new RuntimeException("调用AI服务失败：" + e.getMessage(), e);
        }
    }

    /** 通过反射调用无参 getter，返回 String；失败返回 null */
    private static String invokeGetter(Object obj, String getterName) {
        Object v = invokeGetterMethod(obj, getterName);
        return v != null ? v.toString() : null;
    }

    /** 通过反射调用无参 getter，返回 Object；失败返回 null */
    private static Object invokeGetterMethod(Object obj, String getterName) {
        if (obj == null || getterName == null) return null;
        try {
            java.lang.reflect.Method m = obj.getClass().getMethod(getterName);
            return m.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 单次对话（不保留历史）
     * @param userMessage 用户消息
     * @return AI回复
     */
    public String singleChat(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", userMessage));
        return chat(messages);
    }

    /**
     * 基于用户对某部电影的评论，结合候选电影列表，请求大模型返回推荐的电影名称列表。
     * 说明：
     * - 仅在候选列表中选择推荐结果，避免大模型“胡编电影”。
     * - 约定大模型严格返回 JSON 数组字符串，例如：
     *   ["电影A","电影B","电影C"]
     *
     * @param comment       用户对目标电影的评论内容（已做基本清洗）
     * @param targetMovie   目标电影节点（用于提供名称、类型、评分等上下文）
     * @param candidateMovies 候选电影列表（供大模型筛选）
     * @param limit         期望返回的推荐数量上限
     * @return 大模型返回的候选电影名称列表（按相关度从高到低排序），名称需与候选列表中的名称匹配
     */
    public List<String> recommendMoviesByComment(
            String comment,
            org.example.model.MovieNode targetMovie,
            List<org.example.model.MovieNode> candidateMovies,
            int limit
    ) {
        if (arkService == null) {
            throw new RuntimeException("火山方舟服务未初始化");
        }
        if (targetMovie == null || candidateMovies == null || candidateMovies.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的电影推荐系统，需要根据用户对某部电影的评论，")
                .append("在给定的候选电影列表中挑选出最有可能让该用户感兴趣的电影，并只返回候选列表中的电影名称。\n\n");

        // 目标电影信息
        prompt.append("【目标电影信息】\n");
        prompt.append("名称：").append(safe(targetMovie.getMovieName())).append("\n");
        if (targetMovie.getType() != null) {
            prompt.append("类型：").append(targetMovie.getType()).append("\n");
        }
        if (targetMovie.getRegion() != null) {
            prompt.append("地区：").append(targetMovie.getRegion()).append("\n");
        }
        if (targetMovie.getMovieRating() != null) {
            prompt.append("评分：").append(targetMovie.getMovieRating()).append("\n");
        }
        if (targetMovie.getDirectorString() != null) {
            prompt.append("导演：").append(targetMovie.getDirectorString().replace("|", "、")).append("\n");
        }
        if (targetMovie.getActorString() != null) {
            prompt.append("主演：").append(targetMovie.getActorString().replace("|", "、")).append("\n");
        }

        // 用户评论
        prompt.append("\n【用户对该电影的评论】\n");
        prompt.append(comment).append("\n\n");

        // 候选电影列表
        prompt.append("【候选电影列表】（你只能从下面这些电影中选择推荐目标）\n");
        int index = 1;
        for (org.example.model.MovieNode m : candidateMovies) {
            if (m == null) continue;
            prompt.append(index++).append("、")
                    .append(safe(m.getMovieName()));
            if (m.getType() != null) {
                prompt.append("（类型：").append(m.getType()).append("）");
            }
            if (m.getMovieRating() != null) {
                prompt.append(" 评分：").append(m.getMovieRating());
            }
            prompt.append("\n");
        }

        // 输出要求
        prompt.append("\n【任务要求】\n")
                .append("1. 请综合分析用户评论中的喜好倾向（例如喜欢的题材、节奏、情感氛围等），")
                .append("并从候选电影中选择最合适的电影进行推荐。\n")
                .append("2. 最多推荐 ").append(limit).append(" 部电影，至少 1 部。\n")
                .append("3. 严格以 JSON 数组形式输出结果，不要包含多余说明文字，例如：\n")
                .append("[\"电影A\",\"电影B\"]\n")
                .append("4. 数组元素必须是上面候选列表中出现过的电影名称，不要编造新电影。\n");

        String raw = singleChat(prompt.toString());
        if (raw == null) {
            return new ArrayList<>();
        }

        // 简单解析 JSON 数组格式：["电影A","电影B"]
        List<String> names = new ArrayList<>();
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("[")) {
                // 去掉前后方括号
                String inner = trimmed.substring(1, trimmed.lastIndexOf("]"));
                String[] parts = inner.split(",");
                for (String part : parts) {
                    String name = part.trim();
                    if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                        name = name.substring(1, name.length() - 1);
                    }
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
            } else {
                // 非数组时，尽量拆分行或逗号
                String[] parts = trimmed.split("[,\\n]");
                for (String part : parts) {
                    String name = part.trim();
                    if (name.startsWith("-")) {
                        name = name.substring(1).trim();
                    }
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析评论推荐返回结果失败，原始内容：{}", raw, e);
        }

        return names;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
    
    /**
     * 对话消息实体类
     */
    public static class ChatMessage {
        private String role;
        private String content;
        
        public ChatMessage() {
        }
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}

