package org.example.ai;

import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * 火山方舟 SDK 集成服务
 * 封装 Ark 对话调用，供 Controller 使用。
 */
@Slf4j
@Service
public class ArkIntegrationService {

    @Value("${ark.api.key:9c4f4931-78f0-464c-b3f8-a9677535f15c}")
    private String arkApiKey;

    @Value("${ark.api.url:https://ark.cn-beijing.volces.com/api/v3}")
    private String arkBaseUrl;

    @Value("${ark.model:doubao-seed-1-6-250615}")
    private String arkModel;

    private ArkService arkService;

    @Autowired
    private AiDomainFacadeService aiDomainFacadeService;

    @PostConstruct
    public void init() {
        try {
            arkService = ArkService.builder()
                    .apiKey(arkApiKey)
                    .baseUrl(arkBaseUrl)
                    .build();
            log.info("ArkService 初始化成功");
        } catch (Exception e) {
            log.error("ArkService 初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (arkService != null) {
            try {
                arkService.shutdownExecutor();
                log.info("ArkService 已关闭");
            } catch (Exception e) {
                log.error("关闭 ArkService 失败", e);
            }
        }
    }

    /**
     * 多轮对话入口（当前实现：把所有历史消息发给 Ark）
     */
    public String chat(List<ChatMessage> messages, String userId) {
        if (arkService == null) {
            throw new IllegalStateException("ArkService 未初始化");
        }
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }

        try {
            // 0. 用户询问高分/推荐/类型/导演电影时，优先用系统推荐API，再交给 Ark 整理成名字、简介
            List<ChatMessage> messagesToSend = tryResolveRecommendationWithSystemApi(messages);
            if (messagesToSend == null) {
                messagesToSend = messages;
            }

            List<ItemEasyMessage> itemMessages = buildItemMessages(messagesToSend);

            // 2. 按官方 SDK 示例构造 ResponsesInput
            ResponsesInput.Builder inputBuilder = ResponsesInput.builder();
            for (ItemEasyMessage item : itemMessages) {
                // 注意：addListItem 是在 Builder 上调用，而不是在 ResponsesInput 上
                inputBuilder.addListItem(item);
            }

            // 3. 构造请求体
            CreateResponsesRequest request = CreateResponsesRequest.builder()
                    .model(arkModel)
                    .input(inputBuilder.build())
                    .build();

            // 4. 调用 Ark 接口
            ResponseObject resp = arkService.createResponse(request);

            // 5. 从响应中提取纯文本，避免返回 ResponseObject 的 toString()
            return extractTextFromResponse(resp);
        } catch (Exception e) {
            log.error("调用 Ark 对话接口失败", e);
            throw new RuntimeException("调用 Ark 接口失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 ChatMessage 列表转成 SDK ItemEasyMessage 列表
     */
    private List<ItemEasyMessage> buildItemMessages(List<ChatMessage> messages) {
        List<ItemEasyMessage> itemMessages = new ArrayList<>();
        for (ChatMessage m : messages) {
            String role = normalizeRole(m.getRole());
            MessageContent content = MessageContent.builder()
                    .addListItem(InputContentItemText.builder().text(m.getContent()).build())
                    .build();
            itemMessages.add(ItemEasyMessage.builder().role(role).content(content).build());
        }
        return itemMessages;
    }

    /**
     * 仅对话、不经过推荐替换，用于生成简介等内部调用
     */
    public String chatRaw(List<ChatMessage> messages, String userId) {
        if (arkService == null || messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("ArkService 未初始化或 messages 为空");
        }
        List<ItemEasyMessage> itemMessages = buildItemMessages(messages);
        ResponsesInput.Builder inputBuilder = ResponsesInput.builder();
        for (ItemEasyMessage item : itemMessages) {
            inputBuilder.addListItem(item);
        }
        CreateResponsesRequest request = CreateResponsesRequest.builder()
                .model(arkModel)
                .input(inputBuilder.build())
                .build();
        ResponseObject resp = arkService.createResponse(request);
        return extractTextFromResponse(resp);
    }

    /**
     * 若最后一条是推荐/高分/类型/导演类询问，则先调系统推荐API，再让 Ark 整理成名字、简介；否则返回 null 表示不替换
     */
    private List<ChatMessage> tryResolveRecommendationWithSystemApi(List<ChatMessage> messages) {
        if (messages.isEmpty() || aiDomainFacadeService == null) {
            return null;
        }
        String lastContent = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(messages.get(i).getRole())) {
                lastContent = messages.get(i).getContent();
                break;
            }
        }
        if (lastContent == null || lastContent.trim().isEmpty()) {
            return null;
        }
        String lower = lastContent.trim().toLowerCase();
        // 识别：推荐、高分电影、类型、导演 等
        boolean wantRecommend = lower.contains("推荐") || lower.contains("高分") || lower.contains("高分电影");
        boolean wantType = lower.contains("类型") || lower.contains("哪种") || lower.matches(".*(喜剧|科幻|剧情|爱情|动作).*电影.*");
        boolean wantDirector = lower.contains("导演") && (lower.contains("的") || lower.contains("作品"));
        String dataForAi;
        if (wantDirector && lower.length() < 30) {
            String director = extractDirectorFromQuery(lastContent);
            if (director != null) {
                dataForAi = aiDomainFacadeService.buildRecommendationWithCommentsByDirectorForAi(director, 6);
            } else {
                dataForAi = aiDomainFacadeService.buildRecommendationWithCommentsForAi(6);
            }
        } else if (wantType && lower.length() < 30) {
            String type = extractTypeFromQuery(lastContent);
            if (type != null) {
                dataForAi = aiDomainFacadeService.buildRecommendationWithCommentsByTypeForAi(type, 6);
            } else {
                dataForAi = aiDomainFacadeService.buildRecommendationWithCommentsForAi(6);
            }
        } else if (wantRecommend || lower.contains("电影")) {
            dataForAi = aiDomainFacadeService.buildRecommendationWithCommentsForAi(8);
        } else {
            return null;
        }
        if (dataForAi == null || dataForAi.isEmpty() || dataForAi.startsWith("抱歉") || dataForAi.startsWith("暂无")) {
            return null;
        }
        String instruction = "请将以下电影数据整理成列表，每部电影严格按两行输出：\n第一行：片名：[电影名]\n第二行：简介：[简介内容]\n不要编造，只整理以下内容：\n\n";
        List<ChatMessage> out = new ArrayList<>(messages);
        for (int i = out.size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(out.get(i).getRole())) {
                out.set(i, new ChatMessage("user", instruction + dataForAi));
                break;
            }
        }
        return out;
    }

    private String extractDirectorFromQuery(String query) {
        // 简单抽取：如 "张艺谋导演的电影" -> 张艺谋
        String s = query.replace("推荐", "").replace("电影", "").replace("的", " ").replace("导演", " ").trim();
        if (s.length() > 0 && s.length() <= 10) return s;
        return null;
    }

    private String extractTypeFromQuery(String query) {
        String[] types = {"喜剧", "科幻", "剧情", "爱情", "动作", "悬疑", "惊悚", "动画", "纪录片"};
        for (String t : types) {
            if (query.contains(t)) return t;
        }
        return null;
    }

    /**
     * 从 Ark ResponseObject 中提取回复正文
     */
    private String extractTextFromResponse(ResponseObject resp) {
        if (resp == null) {
            return "Ark 返回为空，请稍后重试。";
        }

        try {
            // 尝试通过 getOutput() -> getChoices() -> get(0) -> getMessage() -> getContent() -> getText()
            if (resp.getOutput() != null) {
                Object output = resp.getOutput();
                java.lang.reflect.Method getChoices = output.getClass().getMethod("getChoices");
                Object choices = getChoices.invoke(output);
                if (choices instanceof List && !((List<?>) choices).isEmpty()) {
                    Object first = ((List<?>) choices).get(0);
                    java.lang.reflect.Method getMessage = first.getClass().getMethod("getMessage");
                    Object message = getMessage.invoke(first);
                    if (message != null) {
                        java.lang.reflect.Method getContent = message.getClass().getMethod("getContent");
                        Object content = getContent.invoke(message);
                        if (content != null) {
                            java.lang.reflect.Method getText = content.getClass().getMethod("getText");
                            String text = (String) getText.invoke(content);
                            if (text != null && !text.trim().isEmpty()) {
                                return text.trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("通过 getChoices/getMessage 解析失败，尝试从 output 中解析: {}", e.getMessage());
        }

        // 备选：从 toString() 中提取 summary_text 或 text= 内容
        String raw = resp.toString();
        if (raw.contains("text='")) {
            int start = raw.lastIndexOf("text='");
            if (start != -1) {
                start += 6;
                int end = raw.indexOf("'", start);
                if (end != -1) {
                    String extracted = raw.substring(start, end).replace("\\'", "'");
                    if (extracted.length() > 0 && extracted.length() < 10000) {
                        return extracted;
                    }
                }
            }
        }

        log.warn("无法从 Ark 响应中解析文本，返回友好提示");
        return "抱歉，本次回复解析异常，请重新发送一次。";
    }

    /**
     * 单轮对话封装，Controller 里会用到。
     */
    public String singleChat(String userMessage, String userId) {
        List<ChatMessage> list = new ArrayList<>();
        list.add(new ChatMessage("user", userMessage));
        return chat(list, userId);
    }

    /**
     * 兼容以前只传一条消息的调用。
     */
    public String singleChat(String userMessage) {
        return singleChat(userMessage, null);
    }

    /**
     * 兼容 ArkController 中老的 chat(List<ChatMessage>) 调用。
     */
    public String chat(List<ChatMessage> messages) {
        return chat(messages, null);
    }

    /**
     * 规范化角色到 SDK 常量。
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return ResponsesConstants.MESSAGE_ROLE_USER;
        }
        String r = role.toLowerCase();
        if ("assistant".equals(r)) {
            return ResponsesConstants.MESSAGE_ROLE_ASSISTANT;
        }
        if ("system".equals(r)) {
            return ResponsesConstants.MESSAGE_ROLE_SYSTEM;
        }
        return ResponsesConstants.MESSAGE_ROLE_USER;
    }

    /**
     * 对话消息 DTO，供 Controller 使用。
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
