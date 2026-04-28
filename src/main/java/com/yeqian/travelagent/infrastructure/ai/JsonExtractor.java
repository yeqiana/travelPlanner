package com.yeqian.travelagent.infrastructure.ai;

import org.springframework.stereotype.Component;

/**
 * JSON 提取器。
 *
 * <p>从模型输出中提取 JSON 对象或数组，降低模型额外输出解释文本导致的解析失败。</p>
 */
@Component
public class JsonExtractor {

    /**
     * 提取 JSON 对象文本。
     *
     * @param content 模型原始输出
     * @return JSON 对象文本
     */
    public String extractObject(String content) {
        return extract(content, '{', '}');
    }

    /**
     * 提取 JSON 数组文本。
     *
     * @param content 模型原始输出
     * @return JSON 数组文本
     */
    public String extractArray(String content) {
        return extract(content, '[', ']');
    }

    /**
     * 根据起止字符提取 JSON 片段。
     *
     * @param content 模型原始输出
     * @param startChar 起始字符
     * @param endChar 结束字符
     * @return JSON 片段
     */
    private String extract(String content, char startChar, char endChar) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("模型输出为空");
        }
        int start = content.indexOf(startChar);
        int end = content.lastIndexOf(endChar);
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型输出不包含合法 JSON");
        }
        return content.substring(start, end + 1);
    }
}
