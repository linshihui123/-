package org.example.response;

import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
public enum ResultCodeEnum {
    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    SYSTEM_ERROR(500, "系统异常"),
    DATA_NOT_FOUND(404, "数据不存在");

    private final Integer code;
    private final String msg;

    ResultCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
