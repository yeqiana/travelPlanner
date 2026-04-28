package com.yeqian.travelagent.domain.model;

import java.util.List;

/**
 * 缺失信息检查结果。
 *
 * <p>描述当前旅行意图是否需要继续向用户澄清，以及需要追问的问题。</p>
 *
 * @param needClarification 是否需要澄清
 * @param clarificationQuestions 澄清问题列表
 */
public record MissingInfoCheckResult(
        boolean needClarification,
        List<String> clarificationQuestions
) {
    /**
     * 创建缺失信息检查结果并规整空集合字段。
     */
    public MissingInfoCheckResult {
        clarificationQuestions = clarificationQuestions == null ? List.of() : List.copyOf(clarificationQuestions);
    }

    /**
     * 获取澄清问题列表。
     *
     * @return 澄清问题列表
     */
    public List<String> questions() {
        return clarificationQuestions;
    }
}
