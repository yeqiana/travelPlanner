package com.yeqian.travelagent.agent.generator;

import com.yeqian.travelagent.domain.enums.EvidenceType;
import com.yeqian.travelagent.domain.enums.FatigueLevel;
import com.yeqian.travelagent.domain.enums.ReminderType;
import com.yeqian.travelagent.domain.model.DailyPlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 旅行提醒生成器测试。
 */
class ReminderGeneratorTest {

    private final ReminderGenerator generator = new ReminderGenerator();

    /**
     * 验证最终计划可以生成购票、景点预约和出发准备提醒。
     */
    @Test
    void shouldGenerateTicketAttractionAndPrepareReminders() {
        List<TravelReminder> reminders = generator.generate(intent(), plan());

        assertThat(reminders).hasSize(3);
        assertThat(reminders).extracting(TravelReminder::type)
                .containsExactly(ReminderType.TICKET, ReminderType.ATTRACTION, ReminderType.PREPARE);
        assertThat(reminders).allMatch(reminder -> reminder.description().contains("确认") || reminder.description().contains("检查"));
    }

    /**
     * 验证证据中的预约、路线和票务风险会进入提醒。
     */
    @Test
    void shouldGenerateEvidenceDrivenReminderTexts() {
        List<TravelEvidence> evidences = List.of(
                evidence(EvidenceType.ATTRACTION, Map.of(
                        "reservationRequired", true,
                        "ticketInfo", "需二次确认"
                )),
                evidence(EvidenceType.ROUTE, Map.of(
                        "routeRisk", "HIGH",
                        "needSecondConfirm", true
                ))
        );

        List<TravelReminder> reminders = generator.generate(intent(), plan(), evidences);
        String text = reminders.stream().map(TravelReminder::description).reduce("", (left, right) -> left + right);

        assertThat(text).contains("预约");
        assertThat(text).contains("二次确认路线/交通");
        assertThat(text).contains("票务/门票确认");
    }

    /**
     * 构造测试旅行意图。
     *
     * @return 旅行意图
     */
    private TravelIntent intent() {
        return new TravelIntent("西安", "五一", 3, 1, null, List.of("杭州"), List.of(), null, null, List.of());
    }

    /**
     * 构造测试最终计划。
     *
     * @return 最终计划
     */
    private TravelPlan plan() {
        DailyPlan dailyPlan = new DailyPlan(1, "杭州", "抵达杭州", "西湖轻量游", "早点休息", FatigueLevel.LOW, List.of("二次确认开放时间"));
        return new TravelPlan("杭州3天旅行计划", "摘要", List.of("西安", "杭州", "西安"), List.of(dailyPlan), List.of(), List.of(), Map.of(), List.of("节假日人流风险"), List.of("确认车票"));
    }

    /**
     * 构造测试证据。
     *
     * @param evidenceType 证据类型
     * @param facts 关键事实
     * @return 旅行证据
     */
    private TravelEvidence evidence(EvidenceType evidenceType, Map<String, Object> facts) {
        return new TravelEvidence(evidenceType, "杭州", "测试证据", "摘要", facts, 0.8, "TEST", null, OffsetDateTime.now());
    }
}
