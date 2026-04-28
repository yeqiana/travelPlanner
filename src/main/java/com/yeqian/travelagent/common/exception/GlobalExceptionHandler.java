package com.yeqian.travelagent.common.exception;

import com.yeqian.travelagent.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>统一处理参数校验异常和系统异常，保证接口返回结构稳定。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数不合法" : error.getDefaultMessage())
                .orElse("请求参数不合法");
        return Result.failure(message);
    }

    /**
     * 处理资源不存在异常。
     *
     * @param exception 参数异常
     * @return 统一失败响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.failure(exception.getMessage());
    }

    /**
     * 处理未捕获系统异常。
     *
     * @param exception 系统异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception exception) {
        return Result.failure("系统暂时无法生成旅行计划，请稍后再试");
    }
}
