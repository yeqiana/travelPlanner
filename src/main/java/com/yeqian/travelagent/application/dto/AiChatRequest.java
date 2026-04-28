package com.yeqian.travelagent.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * AI 聊天请求 DTO。
 *
 * <p>承载用户发给 AI 聊天接口的文本消息。</p>
 *
 * @param message 用户消息
 */
@Schema(description = "AI 聊天请求")
public record AiChatRequest(
        @Schema(description = "用户发送给 AI 的文本消息", example = "帮我推荐一个适合周末放松的旅行目的地")
        @NotBlank(message = "message 不能为空")
        String message
) {
}
