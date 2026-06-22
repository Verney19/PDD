package com.pdd.common.exception;

import com.pdd.common.api.ErrorCode;
import com.pdd.common.api.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>
 * 作用是把 Spring 校验异常、业务异常、未知异常统一包装成 Result 返回前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理显式抛出的业务异常。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验、反序列化失败等请求错误。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public Result<Void> handleBadRequest(Exception ex) {
        // 参数校验、请求体反序列化失败等，都统一视为请求参数错误。
        return Result.fail(ErrorCode.BAD_REQUEST.code(), ex.getMessage());
    }

    /**
     * 兜底处理未显式分类的异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        // 兜底异常，避免把原始堆栈直接暴露给前端。
        return Result.fail(ErrorCode.SYSTEM_ERROR.code(), ex.getMessage());
    }
}
