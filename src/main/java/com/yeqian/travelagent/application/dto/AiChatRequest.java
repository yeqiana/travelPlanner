package com.yeqian.travelagent.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 聊天请求 DTO。
 *
 * <p>承载用户发给 AI 聊天接口的文本消息。</p>
 *
 * @param message 用户消息
 */
public record AiChatRequest(
        @NotBlank(message = "message 不能为空")
        String message
) {
}
