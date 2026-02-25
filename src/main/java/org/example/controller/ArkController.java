package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.ArkIntegrationService;
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
    
    /**
     * 单次对话接口（集成推荐API）
     * @param request 包含message和userId字段的请求体
     * @return AI回复
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String userId = request.get("userId");
            
            if (message == null || message.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            String response = arkService.singleChat(message, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("对话请求失败", e);
            return Result.error("对话服务异常：" + e.getMessage());
        }
    }
    
    /**
     * 多轮对话接口（支持历史消息，集成推荐API）
     * @param request 包含messages数组和userId的请求体
     * @return AI回复
     */
    @PostMapping("/chat/multi")
    public Result<String> multiChat(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messagesList = (List<Map<String, String>>) request.get("messages");
            String userId = (String) request.get("userId");
            
            if (messagesList == null || messagesList.isEmpty()) {
                return Result.error("消息列表不能为空");
            }
            
            // 转换为服务层需要的格式
            List<ArkIntegrationService.ChatMessage> messages = new java.util.ArrayList<>();
            for (Map<String, String> msgMap : messagesList) {
                String role = msgMap.get("role");
                String content = msgMap.get("content");
                if (content != null && !content.trim().isEmpty()) {
                    messages.add(new ArkIntegrationService.ChatMessage(role, content));
                }
            }
            
            if (messages.isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            // 调用增强版的chat方法，传入userId以支持推荐API调用
            String response = arkService.chat(messages, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("多轮对话请求失败", e);
            return Result.error("对话服务异常：" + e.getMessage());
        }
    }
}
