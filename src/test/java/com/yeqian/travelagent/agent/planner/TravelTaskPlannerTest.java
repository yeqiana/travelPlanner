package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 旅行查询任务拆解器测试。
 */
class TravelTaskPlannerTest {

    private final TravelTaskPlanner planner = new TravelTaskPlanner();

    /**
     * 验证多城市目的地可以生成核心查询任务。
     */
    @Test
    void shouldGenerateCoreTasksForXiAnToHangzhouAndShanghaiFourDays() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                4,
                2,
                BigDecimal.valueOf(3000),
                List.of("杭州", "上海"),
                List.of("不想太累"),
                null,
                null,
                List.of()
        );

        List<TravelTask> tasks = planner.plan(intent);

        assertThat(taskTypes(tasks))
                .contains(
                        TravelTaskType.TRANSPORT,
                        TravelTaskType.WEATHER,
                        TravelTaskType.HOTEL,
                        TravelTaskType.ATTRACTION,
                        TravelTaskType.ROUTE
                );
        assertThat(tasks)
                .filteredOn(task -> task.taskType() == TravelTaskType.WEATHER)
                .extracting(TravelTask::city)
                .contains("杭州", "上海");
        assertThat(tasks)
                .filteredOn(task -> task.taskType() == TravelTaskType.ATTRACTION)
                .extracting(TravelTask::city)
                .contains("杭州", "上海");
    }

    /**
     * 验证目的地不明确时生成通用推荐和路线可行性任务。
     */
    @Test
    void shouldGenerateGeneralWebAndRouteTasksWhenDestinationMissing() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                4,
                2,
                null,
                List.of(),
                List.of("不想太累"),
                null,
                null,
                List.of()
        );

        List<TravelTask> tasks = planner.plan(intent);

        assertThat(taskTypes(tasks)).contains(TravelTaskType.GENERAL_WEB, TravelTaskType.ROUTE);
    }

    /**
     * 验证任务数量不会超过上限。
     */
    @Test
    void shouldLimitTasksToEight() {
        TravelIntent intent = new TravelIntent(
                "西安",
                "五一",
                6,
                2,
                null,
                List.of("杭州", "上海", "苏州", "南京", "无锡"),
                List.of(),
                null,
                null,
                List.of()
        );

        List<TravelTask> tasks = planner.plan(intent);

        assertThat(tasks).hasSizeLessThanOrEqualTo(8);
    }

    /**
     * 提取任务类型集合。
     *
     * @param tasks 查询任务列表
     * @return 任务类型集合
     */
    private Set<TravelTaskType> taskTypes(List<TravelTask> tasks) {
        return tasks.stream().map(TravelTask::taskType).collect(Collectors.toSet());
    }
}
