package com.pdd.common.api;

/**
 * 统一错误码定义。
 * <p>
 * 这里既包含常见 HTTP 语义错误，也包含项目里的业务错误，
 * 方便前后端和服务间统一处理。
 */
public enum ErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "重复请求"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    BUSINESS_ERROR(1000, "业务处理失败"),
    STOCK_NOT_ENOUGH(2001, "库存不足"),
    ACTIVITY_NOT_STARTED(2002, "活动未开始"),
    ACTIVITY_ENDED(2003, "活动已结束"),
    DUPLICATE_PARTICIPATION(2004, "请勿重复参与"),
    SYSTEM_ERROR(500, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
