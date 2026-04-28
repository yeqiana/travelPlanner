package com.yeqian.travelagent.domain.model;

/**
 * 澄清问题。
 *
 * <p>用于描述缺失字段对应的追问内容，方便接口同时返回兼容的字符串问题和结构化问题。</p>
 *
 * @param field 缺失字段名称
 * @param question 面向用户的追问文案
 * @param example 示例回答
 * @param required 是否为继续规划所需的必要字段
 */
public record ClarificationQuestion(
        String field,
        String question,
        String example,
        Boolean required
) {
    /**
     * 创建结构化澄清问题并规整必填标记。
     */
    public ClarificationQuestion {
        required = required == null ? Boolean.TRUE : required;
    }
}
