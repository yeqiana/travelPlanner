package com.yeqian.travelagent.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 旅行计划请求 DTO。
 *
 * <p>承载用户的旅行需求文本和可选会话编号。</p>
 *
 * @param message 旅行需求文本
 * @param sessionId 会话编号
 */
public record TravelPlanRequest(
        @NotBlank(message = "旅行需求不能为空")
        String message,
        String sessionId
) {
}
