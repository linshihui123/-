package org.example.dify;

import org.example.model.RecommendIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 适配JDK 1.8的Dify集成服务
 */
@Service
public class DifyIntegrationService {
    @Value("${dify.api.key:your-dify-api-key}")
    private String difyApiKey;

    @Value("${dify.api.url:https://api.dify.ai/v1/workflows/run}")
    private String difyApiUrl;

    // 成员变量声明（JDK 1.8不支持final变量在构造器外初始化的部分场景，显式声明）
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造器初始化（JDK 1.8规范写法）
     */
    public DifyIntegrationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用Dify工作流，解析用户自然语言意图
     * @param userInput 用户自然语言输入（如："推荐张艺谋导演的高分喜剧电影"）
     * @param userId 用户ID
     * @return 解析后的推荐意图
     */
    public RecommendIntent parseUserIntent(String userInput, String userId) {
        if (userInput == null || userInput.trim().isEmpty() || userId == null) {
            throw new IllegalArgumentException("用户输入和用户ID不能为空");
        }

        try {
            String requestBody = String.format(
                    "{\n" +
                    "    \"inputs\": {\n" +
                    "        \"user_input\": \"%s\",\n" +
                    "        \"user_id\": \"%s\"\n" +
                    "    },\n" +
                    "    \"response_mode\": \"blocking\",\n" +
                    "    \"user\": \"%s\"\n" +
                    "}",
                    escapeJsonString(userInput), // 转义JSON特殊字符
                    userId,
                    userId
            );


            // 2. 设置请求头
            java.util.HashMap<String, String> headers = new java.util.HashMap<>();
            headers.put("Authorization", "Bearer " + (difyApiKey != null ? difyApiKey : "your-dify-api-key"));
            headers.put("Content-Type", "application/json");

            // 3. 调用Dify API
            String response = restTemplate.postForObject(
                difyApiUrl != null ? difyApiUrl : "https://api.dify.ai/v1/workflows/run",
                requestBody,
                String.class
            );

            JsonNode jsonNode = this.objectMapper.readTree(response);
            JsonNode dataNode = jsonNode.get("data");
            if (dataNode == null) {
                throw new RuntimeException("Dify返回数据格式异常：缺少data节点");
            }

            JsonNode outputNode = dataNode.get("outputs");
            if (outputNode == null) {
                throw new RuntimeException("Dify返回数据格式异常：缺少outputs节点");
            }

            // 5. 封装推荐意图
            RecommendIntent intent = new RecommendIntent();
            // 处理intent_type空值
            String intentType = outputNode.has("intent_type") ? outputNode.get("intent_type").asText() : "unknown";
            intent.setIntentType(intentType);

            JsonNode paramsNode = outputNode.get("params");
            Map<String, Object> params = paramsNode != null
                    ? this.objectMapper.convertValue(paramsNode, HashMap.class)
                    : new HashMap<>();
            intent.setParams(params);

            return intent;
        } catch (Exception e) {
            e.printStackTrace(); // 生产环境建议替换为日志框架（如SLF4J）
            return null;
        }
    }

    /**
     * 辅助方法：转义JSON字符串中的特殊字符（JDK 1.8手动实现）
     * 避免用户输入包含双引号、换行等导致JSON语法错误
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


}