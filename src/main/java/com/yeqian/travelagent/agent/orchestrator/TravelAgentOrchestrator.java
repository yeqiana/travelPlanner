package com.yeqian.travelagent.agent.orchestrator;

import com.yeqian.travelagent.agent.checker.MissingInfoChecker;
import com.yeqian.travelagent.agent.adjuster.TravelPlanLocalAdjuster;
import com.yeqian.travelagent.agent.generator.ImageBriefGenerator;
import com.yeqian.travelagent.agent.generator.ReminderGenerator;
import com.yeqian.travelagent.agent.intent.TravelDialogIntentRecognizer;
import com.yeqian.travelagent.agent.normalizer.EvidenceNormalizer;
import com.yeqian.travelagent.agent.parser.TravelIntentParser;
import com.yeqian.travelagent.agent.planner.CandidatePlanGenerator;
import com.yeqian.travelagent.agent.planner.ItineraryPlanner;
import com.yeqian.travelagent.agent.planner.TravelTaskPlanner;
import com.yeqian.travelagent.agent.scorer.TravelScorer;
import com.yeqian.travelagent.agent.session.TravelSessionStore;
import com.yeqian.travelagent.agent.suggestion.ContextualSuggestionGenerator;
import com.yeqian.travelagent.application.dto.TravelPlanRequest;
import com.yeqian.travelagent.application.dto.TravelPlanResponse;
import com.yeqian.travelagent.domain.enums.TravelDialogIntent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 旅行 Agent 编排器。
 *
 * <p>负责串联需求解析、缺失信息判断、查询任务拆解和完整旅行计划生成流程。</p>
 */
@Component
public class TravelAgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TravelAgentOrchestrator.class);

    @Resource
    private TravelIntentParser travelIntentParser;

    @Resource
    private TravelDialogIntentRecognizer travelDialogIntentRecognizer;

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
    private TravelPlanLocalAdjuster travelPlanLocalAdjuster;

    @Resource
    private ReminderGenerator reminderGenerator;

    @Resource
    private ImageBriefGenerator imageBriefGenerator;

    @Resource
    private ContextualSuggestionGenerator contextualSuggestionGenerator;

    @Resource
    private TravelSessionStore travelSessionStore;

    /**
     * 执行旅行规划编排流程。
     *
     * @param request 旅行计划请求
     * @return 旅行计划响应
     */
    public TravelPlanResponse plan(TravelPlanRequest request) {
        long startMillis = System.currentTimeMillis();
        TravelSessionContext sessionContext = travelSessionStore.findBySessionId(request.sessionId());
        log.info("Agent 编排开始：sessionId={}, hasSessionContext={}, message={}",
                request.sessionId(),
                sessionContext != null,
                abbreviate(request.message()));
        TravelDialogIntent dialogIntent = travelDialogIntentRecognizer.recognize(request.message(), sessionContext);
        log.info("识别多轮意图完成：dialogIntent={}", dialogIntent);
        // 解析旅行需求文本
        TravelIntent currentIntent = travelIntentParser.parse(request.message());
        log.info("解析本轮旅行意图完成：{}", intentSummary(currentIntent));
        currentIntent = normalizeCurrentIntentForDialog(request.message(), dialogIntent, currentIntent);
        TravelIntent intent = mergeIntent(previousIntent(dialogIntent, sessionContext), currentIntent);
        String activeSessionId = sessionContext == null ? request.sessionId() : sessionContext.sessionId();
        log.info("合并旅行意图完成：activeSessionId={}, {}", activeSessionId, intentSummary(intent));

        MissingInfoCheckResult checkResult = missingInfoChecker.check(intent);
        log.info("缺失信息检查完成：needClarification={}, questions={}",
                checkResult.needClarification(),
                checkResult.clarificationQuestions());
        if (checkResult.needClarification()) {
            TravelSessionContext savedContext = travelSessionStore.saveClarifying(
                    activeSessionId,
                    intent,
                    checkResult.structuredClarificationQuestions()
            );
            log.info("保存追问会话完成：sessionId={}, structuredQuestions={}",
                    savedContext.sessionId(),
                    checkResult.structuredClarificationQuestions().size());
            log.info("Agent 编排结束：result=NEED_CLARIFICATION, costMillis={}", System.currentTimeMillis() - startMillis);
            return TravelPlanResponse.needClarification(
                    savedContext.sessionId(),
                    checkResult.clarificationQuestions(),
                    checkResult.structuredClarificationQuestions(),
                    intent
            ).withInteraction(dialogIntent, contextualSuggestionGenerator.forClarification(checkResult.structuredClarificationQuestions()));
        }

        if (hasText(activeSessionId)) {
            travelSessionStore.markCompleted(activeSessionId, intent);
            log.info("标记会话完成：sessionId={}", activeSessionId);
        }

        List<TravelTask> tasks = travelTaskPlanner.plan(intent);
        log.info("查询任务拆解完成：count={}, tasks={}", tasks.size(), taskSummary(tasks));
        List<ToolResult> toolResults = toolExecutor.execute(tasks);
        log.info("工具执行完成：count={}, success={}, failed={}",
                toolResults.size(),
                toolResults.stream().filter(ToolResult::success).count(),
                toolResults.stream().filter(result -> !result.success()).count());
        List<TravelEvidence> evidences = evidenceNormalizer.normalize(toolResults, tasks);
        log.info("证据归一完成：count={}, evidences={}", evidences.size(), evidenceSummary(evidences));
        List<TravelCandidatePlan> candidatePlans = candidatePlanGenerator.generate(intent, evidences);
        log.info("候选方案生成完成：count={}", candidatePlans.size());
        List<ScoredTravelPlan> scoredPlans = travelScorer.score(candidatePlans, intent, evidences);
        log.info("候选方案评分完成：count={}, topScore={}", scoredPlans.size(), topScore(scoredPlans));
        TravelPlan recommendedPlan = travelPlanLocalAdjuster.adjust(
                dialogIntent,
                request.message(),
                sessionContext,
                itineraryPlanner.generate(intent, scoredPlans, evidences)
        );
        log.info("推荐行程生成完成：title={}, days={}, risks={}, todos={}",
                recommendedPlan == null ? null : recommendedPlan.title(),
                recommendedPlan == null ? 0 : recommendedPlan.dailyPlans().size(),
                recommendedPlan == null ? 0 : recommendedPlan.risks().size(),
                recommendedPlan == null ? 0 : recommendedPlan.todoList().size());
        List<TravelReminder> reminders = reminderGenerator.generate(intent, recommendedPlan, evidences);
        log.info("旅行提醒生成完成：count={}", reminders.size());
        ImageBrief imageBrief = imageBriefGenerator.generate(recommendedPlan);
        log.info("一图流文案生成完成：title={}", imageBrief == null ? null : imageBrief.title());
        log.info("Agent 编排结束：result=COMPLETED, costMillis={}", System.currentTimeMillis() - startMillis);
        return TravelPlanResponse.completed(activeSessionId, intent, tasks, toolResults, evidences, candidatePlans, scoredPlans, recommendedPlan, reminders, imageBrief)
                .withInteraction(dialogIntent, contextualSuggestionGenerator.forPlan(intent, recommendedPlan, dialogIntent));
    }

    /**
     * 根据多轮意图决定是否沿用上一轮旅行意图。
     *
     * @param dialogIntent 本轮多轮对话意图
     * @param sessionContext 会话上下文
     * @return 可参与合并的上一轮旅行意图
     */
    private TravelIntent previousIntent(TravelDialogIntent dialogIntent, TravelSessionContext sessionContext) {
        if (sessionContext == null || dialogIntent == TravelDialogIntent.REGENERATE_PLAN) {
            return null;
        }
        return sessionContext.partialIntent();
    }

    /**
     * 按多轮语义规整本轮解析结果，避免“第二天”被误当成总天数 2。
     *
     * @param message 用户输入文本
     * @param dialogIntent 本轮多轮对话意图
     * @param currentIntent 本轮解析出的旅行意图
     * @return 规整后的旅行意图
     */
    private TravelIntent normalizeCurrentIntentForDialog(String message, TravelDialogIntent dialogIntent, TravelIntent currentIntent) {
        if (currentIntent == null || dialogIntent != TravelDialogIntent.ADJUST_PLAN || message == null || !message.contains("第")) {
            return currentIntent;
        }
        return new TravelIntent(
                currentIntent.departureCity(),
                currentIntent.dateText(),
                null,
                currentIntent.peopleCount(),
                currentIntent.budget(),
                currentIntent.destinationPreferences(),
                currentIntent.travelStyles(),
                currentIntent.transportPreference(),
                currentIntent.hotelBudgetPerNight(),
                currentIntent.avoidPlaces()
        );
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

    /**
     * 构建旅行意图日志摘要。
     *
     * @param intent 旅行意图
     * @return 旅行意图摘要
     */
    private String intentSummary(TravelIntent intent) {
        if (intent == null) {
            return "intent=null";
        }
        return "departureCity=%s, dateText=%s, days=%s, peopleCount=%s, budget=%s, destinations=%s, styles=%s"
                .formatted(
                        intent.departureCity(),
                        intent.dateText(),
                        intent.days(),
                        intent.peopleCount(),
                        intent.budget(),
                        intent.destinationPreferences(),
                        intent.travelStyles()
                );
    }

    /**
     * 构建任务日志摘要。
     *
     * @param tasks 查询任务列表
     * @return 任务摘要
     */
    private String taskSummary(List<TravelTask> tasks) {
        return tasks.stream()
                .map(task -> task.taskType() + ":" + task.city())
                .collect(Collectors.joining(", "));
    }

    /**
     * 构建证据日志摘要。
     *
     * @param evidences 旅行证据列表
     * @return 证据摘要
     */
    private String evidenceSummary(List<TravelEvidence> evidences) {
        return evidences.stream()
                .map(evidence -> evidence.evidenceType() + ":" + evidence.city() + ":" + evidence.sourceName())
                .limit(12)
                .collect(Collectors.joining(", "));
    }

    /**
     * 获取最高评分摘要。
     *
     * @param scoredPlans 已评分候选方案列表
     * @return 最高评分摘要
     */
    private Object topScore(List<ScoredTravelPlan> scoredPlans) {
        if (scoredPlans == null || scoredPlans.isEmpty() || scoredPlans.get(0).score() == null) {
            return null;
        }
        return scoredPlans.get(0).score().totalScore();
    }

    /**
     * 缩短日志中的用户输入文本。
     *
     * @param value 原始文本
     * @return 缩短后的文本
     */
    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }
}
