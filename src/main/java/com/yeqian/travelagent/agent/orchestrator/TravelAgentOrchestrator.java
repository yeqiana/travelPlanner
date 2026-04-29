package com.yeqian.travelagent.agent.orchestrator;

import com.yeqian.travelagent.agent.checker.MissingInfoChecker;
import com.yeqian.travelagent.agent.generator.ImageBriefGenerator;
import com.yeqian.travelagent.agent.generator.ReminderGenerator;
import com.yeqian.travelagent.agent.normalizer.EvidenceNormalizer;
import com.yeqian.travelagent.agent.parser.TravelIntentParser;
import com.yeqian.travelagent.agent.planner.CandidatePlanGenerator;
import com.yeqian.travelagent.agent.planner.ItineraryPlanner;
import com.yeqian.travelagent.agent.planner.TravelTaskPlanner;
import com.yeqian.travelagent.agent.scorer.TravelScorer;
import com.yeqian.travelagent.agent.session.TravelSessionStore;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.domain.model.ImageBrief;
import com.yeqian.travelagent.domain.model.MissingInfoCheckResult;
import com.yeqian.travelagent.domain.model.ScoredTravelPlan;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelPlan;
import com.yeqian.travelagent.domain.model.TravelReminder;
import com.yeqian.travelagent.domain.model.TravelSessionContext;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.tool.ToolExecutor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 旅行 Agent 编排器。
 *
 * <p>负责串联需求解析、缺失信息判断、查询任务拆解和完整旅行计划生成流程。</p>
 */
@Component
public class TravelAgentOrchestrator {

    @Resource
    private TravelIntentParser travelIntentParser;

    @Resource
    private MissingInfoChecker missingInfoChecker;

    @Resource
    private TravelTaskPlanner travelTaskPlanner;

    @Resource
    private ToolExecutor toolExecutor;

    @Resource
    private EvidenceNormalizer evidenceNormalizer;

    @Resource
    private CandidatePlanGenerator candidatePlanGenerator;

    @Resource
    private TravelScorer travelScorer;

    @Resource
    private ItineraryPlanner itineraryPlanner;

    @Resource
    private ReminderGenerator reminderGenerator;

    @Resource
    private ImageBriefGenerator imageBriefGenerator;

    @Resource
    private TravelSessionStore travelSessionStore;

    /**
     * 执行旅行规划编排流程。
     *
     * @param request 旅行计划请求
     * @return 旅行计划响应
     */
    public TravelPlanResponse plan(TravelPlanRequest request) {
        TravelSessionContext sessionContext = travelSessionStore.findBySessionId(request.sessionId());
        TravelIntent currentIntent = travelIntentParser.parse(request.message());
        TravelIntent intent = mergeIntent(sessionContext == null ? null : sessionContext.partialIntent(), currentIntent);
        String activeSessionId = sessionContext == null ? request.sessionId() : sessionContext.sessionId();

        MissingInfoCheckResult checkResult = missingInfoChecker.check(intent);
        if (checkResult.needClarification()) {
            TravelSessionContext savedContext = travelSessionStore.saveClarifying(
                    activeSessionId,
                    intent,
                    checkResult.structuredClarificationQuestions()
            );
            return TravelPlanResponse.needClarification(
                    savedContext.sessionId(),
                    checkResult.clarificationQuestions(),
                    checkResult.structuredClarificationQuestions(),
                    intent
            );
        }

        if (hasText(activeSessionId)) {
            travelSessionStore.markCompleted(activeSessionId, intent);
        }

        List<TravelTask> tasks = travelTaskPlanner.plan(intent);
        List<ToolResult> toolResults = toolExecutor.execute(tasks);
        List<TravelEvidence> evidences = evidenceNormalizer.normalize(toolResults, tasks);
        List<TravelCandidatePlan> candidatePlans = candidatePlanGenerator.generate(intent, evidences);
        List<ScoredTravelPlan> scoredPlans = travelScorer.score(candidatePlans, intent, evidences);
        TravelPlan recommendedPlan = itineraryPlanner.generate(intent, scoredPlans, evidences);
        List<TravelReminder> reminders = reminderGenerator.generate(intent, recommendedPlan, evidences);
        ImageBrief imageBrief = imageBriefGenerator.generate(recommendedPlan);
        return TravelPlanResponse.completed(activeSessionId, intent, tasks, toolResults, evidences, candidatePlans, scoredPlans, recommendedPlan, reminders, imageBrief);
    }

    /**
     * 合并历史旅行意图和本轮旅行意图。
     *
     * @param previousIntent 历史部分旅行意图
     * @param currentIntent 本轮解析出的旅行意图
     * @return 合并后的旅行意图
     */
    private TravelIntent mergeIntent(TravelIntent previousIntent, TravelIntent currentIntent) {
        if (previousIntent == null) {
            return currentIntent;
        }
        if (currentIntent == null) {
            return previousIntent;
        }
        return new TravelIntent(
                firstText(currentIntent.departureCity(), previousIntent.departureCity()),
                firstText(currentIntent.dateText(), previousIntent.dateText()),
                firstNumber(currentIntent.days(), previousIntent.days()),
                firstPeopleCount(currentIntent.peopleCount(), previousIntent.peopleCount()),
                firstAmount(currentIntent.budget(), previousIntent.budget()),
                mergeList(previousIntent.destinationPreferences(), currentIntent.destinationPreferences()),
                mergeList(previousIntent.travelStyles(), currentIntent.travelStyles()),
                firstText(currentIntent.transportPreference(), previousIntent.transportPreference()),
                firstAmount(currentIntent.hotelBudgetPerNight(), previousIntent.hotelBudgetPerNight()),
                mergeList(previousIntent.avoidPlaces(), currentIntent.avoidPlaces())
        );
    }

    /**
     * 返回优先级更高的非空文本。
     *
     * @param preferred 优先文本
     * @param fallback 兜底文本
     * @return 非空文本，不存在时返回 null
     */
    private String firstText(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    /**
     * 返回优先级更高的正数。
     *
     * @param preferred 优先数值
     * @param fallback 兜底数值
     * @return 正数，不存在时返回 null
     */
    private Integer firstNumber(Integer preferred, Integer fallback) {
        return preferred != null && preferred > 0 ? preferred : fallback;
    }

    /**
     * 返回合并后的人数。
     *
     * @param preferred 优先人数
     * @param fallback 兜底人数
     * @return 合并后的人数
     */
    private Integer firstPeopleCount(Integer preferred, Integer fallback) {
        if (preferred != null && preferred > 1) {
            return preferred;
        }
        return fallback == null ? preferred : fallback;
    }

    /**
     * 返回优先级更高的金额。
     *
     * @param preferred 优先金额
     * @param fallback 兜底金额
     * @return 金额，不存在时返回 null
     */
    private BigDecimal firstAmount(BigDecimal preferred, BigDecimal fallback) {
        return preferred != null ? preferred : fallback;
    }

    /**
     * 合并列表并按出现顺序去重。
     *
     * @param previousValues 历史值列表
     * @param currentValues 本轮值列表
     * @return 合并去重后的列表
     */
    private List<String> mergeList(List<String> previousValues, List<String> currentValues) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        addValues(values, previousValues);
        addValues(values, currentValues);
        return new ArrayList<>(values);
    }

    /**
     * 将有效文本加入集合。
     *
     * @param values 目标集合
     * @param sourceValues 来源文本列表
     */
    private void addValues(LinkedHashSet<String> values, List<String> sourceValues) {
        if (sourceValues == null) {
            return;
        }
        for (String value : sourceValues) {
            if (hasText(value)) {
                values.add(value);
            }
        }
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
