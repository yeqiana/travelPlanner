package com.yeqian.travelagent.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public record AiChatResponse(
        boolean success,
        String data,
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
