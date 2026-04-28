package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具执行器测试。
 */
class ToolExecutorTest {

    /**
     * 验证每种任务类型都能路由到对应 mock 工具。
     */
    @Test
    void shouldRouteEachTaskTypeToMatchedMockTool() {
        ToolExecutor executor = toolExecutorWith(List.of(
                new MockWeatherTool(),
                new MockTransportSearchTool(),
                new MockHotelSearchTool(),
                new MockAttractionInfoTool(),
                new MockRouteTool(),
                new MockWebSearchTool()
        ));
        List<TravelTask> tasks = List.of(
                task(TravelTaskType.WEATHER),
                task(TravelTaskType.TRANSPORT),
                task(TravelTaskType.HOTEL),
                task(TravelTaskType.ATTRACTION),
                task(TravelTaskType.ROUTE),
                task(TravelTaskType.GENERAL_WEB)
        );

        List<ToolResult> results = executor.execute(tasks);

        assertThat(results).hasSameSizeAs(tasks);
        assertThat(results).allMatch(ToolResult::success);
        Map<TravelTaskType, String> sourceMap = results.stream()
                .collect(Collectors.toMap(ToolResult::taskType, ToolResult::source));
        assertThat(sourceMap)
                .containsEntry(TravelTaskType.WEATHER, "MockWeatherTool")
                .containsEntry(TravelTaskType.TRANSPORT, "MockTransportSearchTool")
                .containsEntry(TravelTaskType.HOTEL, "MockHotelSearchTool")
                .containsEntry(TravelTaskType.ATTRACTION, "MockAttractionInfoTool")
                .containsEntry(TravelTaskType.ROUTE, "MockRouteTool")
                .containsEntry(TravelTaskType.GENERAL_WEB, "MockWebSearchTool");
    }

    /**
     * 验证单个工具异常不会中断整体执行。
     */
    @Test
    void shouldContinueWhenOneToolThrowsException() {
        ToolExecutor executor = toolExecutorWith(List.of(
                new BrokenWeatherTool(),
                new MockHotelSearchTool()
        ));
        List<TravelTask> tasks = List.of(
                task(TravelTaskType.WEATHER),
                task(TravelTaskType.HOTEL)
        );

        List<ToolResult> results = executor.execute(tasks);

        assertThat(results).hasSameSizeAs(tasks);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(0).source()).isEqualTo("BrokenWeatherTool");
        assertThat(results.get(0).errorMessage()).contains("mock 工具异常");
        assertThat(results.get(1).success()).isTrue();
        assertThat(results.get(1).source()).isEqualTo("MockHotelSearchTool");
    }

    /**
     * 创建带指定工具列表的执行器。
     *
     * @param tools 工具列表
     * @return 工具执行器
     */
    private ToolExecutor toolExecutorWith(List<TravelTool> tools) {
        ToolExecutor executor = new ToolExecutor();
        ReflectionTestUtils.setField(executor, "travelTools", tools);
        return executor;
    }

    /**
     * 创建测试任务。
     *
     * @param taskType 任务类型
     * @return 测试任务
     */
    private TravelTask task(TravelTaskType taskType) {
        return new TravelTask(taskType, taskType.name() + " query", "杭州", 1);
    }

    /**
     * 故障天气工具。
     */
    private static class BrokenWeatherTool implements TravelTool {

        /**
         * 判断是否支持天气任务。
         *
         * @param taskType 任务类型
         * @return 支持天气任务返回 true，否则返回 false
         */
        @Override
        public boolean supports(TravelTaskType taskType) {
            return TravelTaskType.WEATHER == taskType;
        }

        /**
         * 执行任务并模拟异常。
         *
         * @param task 旅行查询任务
         * @return 不会正常返回
         */
        @Override
        public ToolResult execute(TravelTask task) {
            throw new IllegalStateException("mock 工具异常");
        }

        /**
         * 获取工具来源名称。
         *
         * @return 工具来源名称
         */
        @Override
        public String source() {
            return "BrokenWeatherTool";
        }
    }
}
