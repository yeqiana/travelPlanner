package com.yeqian.travelagent.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 聊天响应 DTO。
 *
 * <p>承载 AI 调用是否成功、返回内容和错误信息。</p>
 *
 * @param success 是否成功
 * @param data AI 返回内容
 * @param message 错误信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "AI 聊天响应")
public record AiChatResponse(
        @Schema(description = "AI 调用是否成功", example = "true")
        boolean success,
        @Schema(description = "AI 返回内容，调用成功时返回", example = "杭州适合周末放松，可以安排西湖、灵隐寺和龙井村。")
        String data,
        @Schema(description = "错误信息，调用失败时返回", example = "AI 调用失败：上游服务超时")
        String message
) {

    /**
     * 构造成功响应。
     *
     * @param data AI 返回内容
     * @return AI 聊天响应
     */
    public static AiChatResponse success(String data) {
        return new AiChatResponse(true, data, null);
    }

    /**
     * 构造失败响应。
     *
     * @param message 错误信息
     * @return AI 聊天响应
     */
    public static AiChatResponse failure(String message) {
        return new AiChatResponse(false, null, message);
    }
}
