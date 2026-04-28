package com.yeqian.travelagent.agent.checker;

import com.yeqian.travelagent.domain.model.MissingInfoCheckResult;
import com.yeqian.travelagent.domain.model.TravelIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缺失信息检查器测试。
 */
class MissingInfoCheckerTest {

    private final MissingInfoChecker checker = new MissingInfoChecker();

    /**
     * 验证完整输入不需要澄清。
     */
    @Test
    void shouldNotAskClarificationWhenInputIsComplete() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                4,
                2,
                BigDecimal.valueOf(3000),
                List.of("杭州", "上海周边"),
                List.of("不想太累"),
                null,
                null,
                List.of()
        );

        MissingInfoCheckResult result = checker.check(intent);

        assertThat(result.needClarification()).isFalse();
        assertThat(result.clarificationQuestions()).isEmpty();
    }

    /**
     * 验证缺少出发城市时需要追问。
     */
    @Test
    void shouldAskClarificationWhenDepartureCityMissing() {
        TravelIntent intent = new TravelIntent(
                null,
                "五一",
                4,
                2,
                null,
                List.of("杭州"),
                List.of(),
                null,
                null,
                List.of()
        );

        MissingInfoCheckResult result = checker.check(intent);

        assertThat(result.needClarification()).isTrue();
        assertThat(result.clarificationQuestions()).containsExactly("你是从哪个城市出发？");
    }

    /**
     * 验证缺少出行天数时需要追问。
     */
    @Test
    void shouldAskClarificationWhenDaysMissing() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                null,
                2,
                null,
                List.of("杭州"),
                List.of(),
                null,
                null,
                List.of()
        );

        MissingInfoCheckResult result = checker.check(intent);

        assertThat(result.needClarification()).isTrue();
        assertThat(result.clarificationQuestions()).containsExactly("计划出行几天？");
    }

    /**
     * 验证缺少人数时默认按 1 人处理。
     */
    @Test
    void shouldDefaultPeopleCountToOneWhenMissing() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                4,
                null,
                null,
                List.of("杭州"),
                List.of(),
                null,
                null,
                List.of()
        );

        MissingInfoCheckResult result = checker.check(intent);

        assertThat(intent.peopleCount()).isEqualTo(1);
        assertThat(result.needClarification()).isFalse();
    }
}
