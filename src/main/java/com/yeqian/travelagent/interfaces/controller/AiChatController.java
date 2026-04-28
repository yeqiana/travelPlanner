package com.yeqian.travelagent.interfaces.controller;

import com.yeqian.travelagent.application.dto.AiChatRequest;
import com.yeqian.travelagent.application.dto.AiChatResponse;
import com.yeqian.travelagent.application.service.AiChatService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 聊天控制器。
 *
 * <p>提供基础 AI 对话接口，主要用于验证模型配置和调试模型调用链路。</p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    /**
     * 发送聊天消息。
     *
     * @param request AI 聊天请求
     * @return AI 聊天响应
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            return ResponseEntity.ok(AiChatResponse.success(aiChatService.chat(request.message())));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AiChatResponse.failure(exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(AiChatResponse.failure("AI 调用失败：" + exception.getMessage()));
        }
    }
}
