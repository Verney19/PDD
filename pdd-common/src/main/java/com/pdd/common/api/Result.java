package com.pdd.common.api;

import java.io.Serializable;

/**
 * 项目统一返回体。
 * <p>
 * 所有 Controller 默认都返回该结构，前端只需要约定：
 * code = 0 表示成功，非 0 表示失败。
 */
public record Result<T>(int code, String message, T data) implements Serializable {
    public static final int SUCCESS_CODE = 0;

    /**
     * 返回带数据的成功结果。
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    /**
     * 返回不带数据的成功结果。
     */
    public static Result<Void> ok() {
        return new Result<>(SUCCESS_CODE, "success", null);
    }

    /**
     * 返回失败结果。
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 便于服务间调用后快速判断是否成功。
     */
    public boolean success() {
        return code == SUCCESS_CODE;
    }
}
