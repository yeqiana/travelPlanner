package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 缺失信息检查结果。
 *
 * <p>描述当前旅行意图是否需要继续向用户澄清，以及需要追问的问题。</p>
 *
 * @param needClarification 是否需要澄清
 * @param clarificationQuestions 兼容旧接口的澄清问题文本列表
 * @param structuredClarificationQuestions 结构化澄清问题列表
 */
public record MissingInfoCheckResult(
        boolean needClarification,
        List<String> clarificationQuestions,
        List<ClarificationQuestion> structuredClarificationQuestions
) {
    /**
     * 基于文本问题创建缺失信息检查结果。
     *
     * @param needClarification 是否需要澄清
     * @param clarificationQuestions 澄清问题文本列表
     */
    public MissingInfoCheckResult(boolean needClarification, List<String> clarificationQuestions) {
        this(needClarification, clarificationQuestions, List.of());
    }

    /**
     * 创建缺失信息检查结果并规整空集合字段。
     */
    public MissingInfoCheckResult {
        clarificationQuestions = clarificationQuestions == null ? List.of() : List.copyOf(clarificationQuestions);
        structuredClarificationQuestions = structuredClarificationQuestions == null ? List.of() : List.copyOf(structuredClarificationQuestions);
    }

    /**
     * 获取澄清问题文本列表。
     *
     * @return 澄清问题文本列表
     */
    public List<String> questions() {
        return clarificationQuestions;
    }
}
