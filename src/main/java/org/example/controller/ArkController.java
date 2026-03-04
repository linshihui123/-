package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.ArkIntegrationService;
import org.example.ai.AiLocalContextService;
import org.example.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 火山方舟对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/ark")
@CrossOrigin(origins = "*")
public class ArkController {
    
    @Autowired
    private ArkIntegrationService arkService;

    @Autowired
    private AiLocalContextService aiLocalContextService;
    
    /**
     * 单次对话接口（集成推荐API）
     * @param request 包含message和userId字段的请求体
     * @return AI回复
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            String response = arkService.singleChat(message);
            return Result.success(response);
        } catch (Exception e) {
            log.error("对话请求失败", e);
            return Result.error("对话服务异常：" + e.getMessage());
        }
    }
    
    /**
     * 多轮对话接口（支持历史消息，集成推荐API）
     * @param request 包含messages数组和userId的请求体（userId 可能为字符串或数字）
     * @return AI回复
     */
    @PostMapping("/chat/multi")
    public Result<String> multiChat(@RequestBody Map<String, Object> request) {
        try {
            Object messagesObj = request != null ? request.get("messages") : null;
            if (!(messagesObj instanceof List)) {
                return Result.error("消息列表不能为空");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) messagesObj;
            if (rawList.isEmpty()) {
                return Result.error("消息列表不能为空");
            }

            List<ArkIntegrationService.ChatMessage> messages = new java.util.ArrayList<>();
            String lastUserQuestion = null;
            for (Map<String, Object> msgMap : rawList) {
                String role = msgMap.get("role") != null ? String.valueOf(msgMap.get("role")).trim() : "user";
                String content = msgMap.get("content") != null ? String.valueOf(msgMap.get("content")).trim() : "";
                if (!content.isEmpty()) {
                    messages.add(new ArkIntegrationService.ChatMessage(role, content));
                    if ("user".equalsIgnoreCase(role)) {
                        lastUserQuestion = content;
                    }
                }
            }
            if (messages.isEmpty()) {
                return Result.error("消息内容不能为空");
            }

            // 基于最后一条用户问题构建本地数据链上下文，并作为 system 消息注入
            String context = aiLocalContextService.buildContext(lastUserQuestion, null);
            List<ArkIntegrationService.ChatMessage> finalMessages = new java.util.ArrayList<>();
            if (context != null && !context.trim().isEmpty()) {
                finalMessages.add(new ArkIntegrationService.ChatMessage("system", context));
            }
            finalMessages.addAll(messages);

            String response = arkService.chat(finalMessages);
            return Result.success(response);
        } catch (Throwable e) {
            log.error("多轮对话请求失败", e);
            return Result.error("对话服务异常：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
