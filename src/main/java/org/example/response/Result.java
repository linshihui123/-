package org.example.response;

import lombok.Data;

/**
 * 统一返回结果
 */
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    private Long total; // 添加total字段

    // 成功响应
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 成功响应（带分页数据）
    public static <T> Result<T> successWithTotal(T data, Long total) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        result.setTotal(total);
        return result;
    }

    // 失败响应
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 通用失败
    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }
}// 在MovieRecommendationService类内部添加：



