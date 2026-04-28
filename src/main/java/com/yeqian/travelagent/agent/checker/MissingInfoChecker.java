package com.yeqian.travelagent.agent.checker;

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
        List<String> questions = new ArrayList<>();
        if (isBlank(intent.departureCity())) {
            questions.add("你是从哪个城市出发？");
        }
        if (isBlank(intent.dateText())) {
            questions.add("你计划什么时候出发？");
        }
        if (intent.days() == null || intent.days() <= 0) {
            questions.add("计划出行几天？");
        }
        return new MissingInfoCheckResult(!questions.isEmpty(), questions);
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
