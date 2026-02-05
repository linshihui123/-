package org.example.ai;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import okhttp3.*;
import java.io.IOException;
import java.util.List;

/**
 * Java调用Coze工作流通用模板
 * 核心：根据自己的工作流修改【入参实体类FlowRequest】和【出参实体类FlowResponse】
 */
public class CozeFlowClient {
    // -------------------------- 配置常量（需替换为你的信息）--------------------------
    /** Coze API令牌（和对话API通用） */
    private static final String COZE_API_TOKEN = "your_coze_api_token_here";
    /** 工作流调用地址（Coze控制台部署后生成的专属地址） */
    private static final String COZE_FLOW_URL = "https://www.coze.cn/work_flow?space_id=7527206408275099698&workflow_id=7529354387682836526&force_stay=1cc";
    /** 超时时间（秒），根据工作流执行耗时调整（复杂工作流可设60/120） */
    private static final int TIMEOUT_SECONDS = 60;
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final Gson GSON = new Gson();

    /**
     * 调用Coze工作流
     * @param flowRequest 工作流入参对象（自定义）
     * @return FlowResponse 工作流出参对象（自定义）
     * @throws IOException 网络异常、API请求异常
     */
    public static FlowResponse callCozeFlow(FlowRequest flowRequest) throws IOException {
        // 1. 将入参对象序列化为JSON字符串
        String requestBodyJson = GSON.toJson(flowRequest);
        // 2. 构造请求体（JSON格式）
        RequestBody requestBody = RequestBody.create(
                requestBodyJson,
                MediaType.get("application/json; charset=utf-8")
        );
        // 3. 构造HTTP请求（核心：Bearer Token鉴权，和对话API一致）
        Request request = new Request.Builder()
                .url(COZE_FLOW_URL) // 工作流专属地址
                .post(requestBody)  // 固定POST请求
                .addHeader("Authorization", "Bearer " + COZE_API_TOKEN) // 核心鉴权，不可改
                .addHeader("Content-Type", "application/json; charset=utf-8") // 固定JSON格式
                .build();
        // 4. 执行请求并解析响应
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            // 检查请求是否成功（非200状态码直接抛异常）
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无错误信息";
                throw new IOException("Coze工作流调用失败 | 状态码：" + response.code() + " | 错误信息：" + errorBody);
            }
            // 解析响应体为自定义出参对象
            String responseBody = response.body().string();
            return GSON.fromJson(responseBody, FlowResponse.class);
        }
    }


}