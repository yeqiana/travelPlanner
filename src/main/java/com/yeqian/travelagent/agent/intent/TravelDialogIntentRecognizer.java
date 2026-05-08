package com.yeqian.travelagent.agent.intent;

import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 旅行多轮对话意图识别器。
 *
 * <p>使用规则识别用户在后续对话中是要细化、重写、局部调整、补充约束，还是回答系统追问。</p>
 */
@Component
public class TravelDialogIntentRecognizer {

    private static final List<String> DETAIL_KEYWORDS = List.of("不详细", "太笼统", "没有详细", "详细计划", "细化", "具体一点", "看不懂", "没看懂");
    private static final List<String> REGENERATE_KEYWORDS = List.of("重新", "重做", "重写", "换一版", "重新安排", "重新规划");
    private static final List<String> ADJUST_KEYWORDS = List.of("调整", "改成", "换一些", "换个", "轻松一点", "少一点", "多一点", "减少", "增加", "第");
    private static final List<String> CONSTRAINT_KEYWORDS = List.of("预算", "带老人", "带孩子", "带娃", "亲子", "宠物", "自驾", "不想", "避免", "必须", "想住", "高铁", "飞机", "餐饮", "酒店");

    /**
     * 识别用户本轮旅行对话意图。
     *
     * @param message 用户输入文本
     * @param sessionContext 当前会话上下文
     * @return 旅行对话意图
     */
    public TravelDialogIntent recognize(String message, TravelSessionContext sessionContext) {
        String normalizedMessage = message == null ? "" : message.trim();
        if (hasClarifyingContext(sessionContext)) {
            return TravelDialogIntent.ANSWER_CLARIFICATION;
        }
        if (!hasText(normalizedMessage)) {
            return TravelDialogIntent.CREATE_PLAN;
        }
        if (containsAny(normalizedMessage, REGENERATE_KEYWORDS)) {
            return TravelDialogIntent.REGENERATE_PLAN;
        }
        if (containsAny(normalizedMessage, DETAIL_KEYWORDS)) {
            return TravelDialogIntent.DETAIL_PLAN;
        }
        if (containsAny(normalizedMessage, ADJUST_KEYWORDS)) {
            return TravelDialogIntent.ADJUST_PLAN;
        }
        if (containsAny(normalizedMessage, CONSTRAINT_KEYWORDS)) {
            return TravelDialogIntent.ADD_CONSTRAINT;
        }
        return sessionContext == null ? TravelDialogIntent.CREATE_PLAN : TravelDialogIntent.ADD_CONSTRAINT;
    }

    /**
     * 判断是否存在待回答的追问上下文。
     *
     * @param sessionContext 当前会话上下文
     * @return 存在追问上下文时返回 true
     */
    private boolean hasClarifyingContext(TravelSessionContext sessionContext) {
        return sessionContext != null
                && "CLARIFYING".equals(sessionContext.status())
                && !sessionContext.lastQuestions().isEmpty();
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param message 用户输入文本
     * @param keywords 关键词列表
     * @return 命中任一关键词时返回 true
     */
    private boolean containsAny(String message, List<String> keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否包含有效内容。
     *
     * @param value 待判断文本
     * @return 包含有效内容时返回 true
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
