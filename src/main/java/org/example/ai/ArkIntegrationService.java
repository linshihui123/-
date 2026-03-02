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

