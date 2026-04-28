package com.yeqian.travelagent.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 旅行计划请求 DTO。
 *
 * <p>承载用户的旅行需求文本和可选会话编号。</p>
 *
 * @param message 旅行需求文本
 * @param sessionId 会话编号
 */
@Schema(description = "旅行计划请求")
public record TravelPlanRequest(
        @Schema(description = "用户的自然语言旅行需求", example = "五一从西安出发去杭州和上海周边玩4天，两个人，预算5000，不想太累")
        @NotBlank(message = "旅行需求不能为空")
        String message,
        @Schema(description = "会话编号。多轮补充信息时传入上一轮返回的 sessionId", example = "session-20260428-001", nullable = true)
        String sessionId
) {
}
