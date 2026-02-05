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
    private static final String COZE_FLOW_URL = "https://api.coze.cn/v3/flow/run/xxx/xxx";
    /** 超时时间（秒），根据工作流执行耗时调整（复杂工作流可设60/120） */
    private static final int TIMEOUT_SECONDS = 60;

    // -------------------------- 客户端实例（单例，避免重复创建）--------------------------
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final Gson GSON = new Gson();

    // -------------------------- 1. 工作流入参实体类（★★★需根据你的工作流修改★★★）--------------------------
    /**
     * 示例：电影推荐工作流入参
     * 你的工作流有哪些入参，就定义哪些字段（字段名、类型必须和Coze工作流一致）
     * @SerializedName：若Java字段名和JSON入参名不一致时使用，一致则可省略
     */
    static class FlowRequest {
        // 入参1：要推荐的电影名（字符串类型）
        @SerializedName("movieName")
        private String movieName;

        // 构造器（入参字段全参）
        public FlowRequest(String movieName) {
            this.movieName = movieName;
        }

        // Getter/Setter（按需添加，Gson序列化无需，但业务中可能需要）
        public String getMovieName() {
            return movieName;
        }

        public void setMovieName(String movieName) {
            this.movieName = movieName;
        }
    }

    // -------------------------- 2. 工作流出参实体类（★★★需根据你的工作流修改★★★）--------------------------
    /**
     * 示例：电影推荐工作流出参
     * 你的工作流输出什么字段，就定义什么字段（字段名、类型必须和Coze工作流一致）
     */
    static class FlowResponse {
        // 出参1：推荐的电影列表（字符串数组）
        @SerializedName("recommendMovies")
        private List<String> recommendMovies;
        // 出参2：推荐理由（字符串）
        @SerializedName("reason")
        private String reason;
        // 工作流通用出参：执行状态（success/fail），Coze工作流默认返回
        @SerializedName("status")
        private String status;
        // 工作流通用出参：错误信息（失败时非空，成功时为null/空字符串）
        @SerializedName("error_msg")
        private String errorMsg;

        // 所有出参的Getter（核心，用于获取工作流返回结果）
        public List<String> getRecommendMovies() {
            return recommendMovies;
        }

        public String getReason() {
            return reason;
        }

        public String getStatus() {
            return status;
        }

        public String getErrorMsg() {
            return errorMsg;
        }
    }

    // -------------------------- 核心：调用Coze工作流方法（通用，无需修改）--------------------------
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