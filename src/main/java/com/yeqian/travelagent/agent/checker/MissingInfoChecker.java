package com.yeqian.travelagent.agent.checker;

import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.MissingInfoCheckResult;
import com.yeqian.travelagent.domain.model.TravelIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 缺失信息检查器。
 *
 * <p>检查旅行意图中是否缺少继续规划所需的关键字段。</p>
 */
@Component
public class MissingInfoChecker {

    /**
     * 检查旅行意图是否需要补充信息。
     *
     * @param intent 旅行意图
     * @return 缺失信息检查结果
     */
    public MissingInfoCheckResult check(TravelIntent intent) {
        List<ClarificationQuestion> structuredQuestions = new ArrayList<>();
        if (intent == null || isBlank(intent.departureCity())) {
            structuredQuestions.add(new ClarificationQuestion(
                    "departureCity",
                    "你是从哪个城市出发？",
                    "例如：西安",
                    true
            ));
        }
        if (intent == null || isBlank(intent.dateText())) {
            structuredQuestions.add(new ClarificationQuestion(
                    "dateText",
                    "你计划什么时候出发？",
                    "例如：五一、周末、2026-05-01",
                    true
            ));
        }
        if (intent == null || intent.days() == null || intent.days() <= 0) {
            structuredQuestions.add(new ClarificationQuestion(
                    "days",
                    "计划出行几天？",
                    "例如：4天",
                    true
            ));
        }

        List<String> questions = structuredQuestions.stream()
                .map(ClarificationQuestion::question)
                .toList();
        return new MissingInfoCheckResult(!structuredQuestions.isEmpty(), questions, structuredQuestions);
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 为空时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
