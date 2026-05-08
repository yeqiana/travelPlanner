package com.yeqian.travelagent.agent.intent;

import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
import com.yeqian.travelagent.domain.model.ClarificationQuestion;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 旅行多轮对话意图识别器测试。
 */
class TravelDialogIntentRecognizerTest {

    private final TravelDialogIntentRecognizer recognizer = new TravelDialogIntentRecognizer();

    /**
     * 验证细化计划反馈可以被识别。
     */
    @Test
    void shouldRecognizeDetailPlan() {
        assertThat(recognizer.recognize("没有详细的旅游计划啊", null)).isEqualTo(TravelDialogIntent.DETAIL_PLAN);
    }

    /**
     * 验证重新生成反馈可以被识别。
     */
    @Test
    void shouldRecognizeRegeneratePlan() {
        assertThat(recognizer.recognize("重新安排一版", null)).isEqualTo(TravelDialogIntent.REGENERATE_PLAN);
    }

    /**
     * 验证局部调整反馈可以被识别。
     */
    @Test
    void shouldRecognizeAdjustPlan() {
        assertThat(recognizer.recognize("第二天轻松一点", null)).isEqualTo(TravelDialogIntent.ADJUST_PLAN);
    }

    /**
     * 验证补充约束反馈可以被识别。
     */
    @Test
    void shouldRecognizeAddConstraint() {
        assertThat(recognizer.recognize("预算控制在3000以内", null)).isEqualTo(TravelDialogIntent.ADD_CONSTRAINT);
    }

    /**
     * 验证追问上下文优先识别为回答追问。
     */
    @Test
    void shouldRecognizeAnswerClarificationWhenSessionIsClarifying() {
        TravelSessionContext context = new TravelSessionContext(
                "session-test",
                null,
                List.of(new ClarificationQuestion("departureCity", "你是从哪个城市出发？", "例如：西安", true)),
                null,
                null,
                null,
                "CLARIFYING"
        );

        assertThat(recognizer.recognize("我从西安出发", context)).isEqualTo(TravelDialogIntent.ANSWER_CLARIFICATION);
    }
}
