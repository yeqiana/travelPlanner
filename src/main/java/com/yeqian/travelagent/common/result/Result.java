package com.yeqian.travelagent.common.result;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一接口响应。
 *
 * <p>为 Controller 返回值提供稳定结构，便于前后端和测试统一处理。</p>
 */
@Schema(description = "统一接口响应")
public record Result<T>(
        @Schema(description = "业务状态码。0 表示成功，-1 表示失败", example = "0")
        int code,
        @Schema(description = "响应消息", example = "success")
        String message,
        @Schema(description = "响应数据。具体结构由接口返回类型决定")
        T data
) {

    /**
     * 构造成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 统一成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    /**
     * 构造失败响应。
     *
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 统一失败响应
     */
    public static <T> Result<T> failure(String message) {
        return new Result<>(-1, message, null);
    }
}
