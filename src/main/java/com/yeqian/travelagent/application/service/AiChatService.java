package com.yeqian.travelagent.application.service;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 聊天应用服务。
 *
 * <p>封装基础 AI 对话调用，并在配置缺失时给出明确错误。</p>
 */
@Service
public class AiChatService {

    @Resource
    private ObjectProvider<ChatModel> chatModelProvider;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * 调用 AI 模型进行对话。
     *
     * @param message 用户消息
     * @return 模型回复文本
     */
    public String chat(String message) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("未配置 Ark API Key，请设置环境变量 ARK_API_KEY");
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new IllegalStateException("Spring AI ChatModel 未初始化，请检查 Spring AI OpenAI 兼容配置");
        }

        return chatModel.call(message);
    }
}
