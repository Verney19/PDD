package com.pdd.common.exception;

import com.pdd.common.api.ErrorCode;

/**
 * 业务异常。
 * <p>
 * 与系统异常区分开来，抛出后会被全局异常处理器转换为统一返回体。
 */
public class BizException extends RuntimeException {
    private final int code;

    /**
     * 使用预定义错误码创建业务异常。
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    /**
     * 使用预定义错误码和自定义提示创建业务异常。
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    /**
     * 返回业务错误码。
     */
    public int getCode() {
        return code;
    }
}
