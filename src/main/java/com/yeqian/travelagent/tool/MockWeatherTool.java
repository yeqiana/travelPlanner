package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * mock 天气查询工具。
 *
 * <p>用于在不接入真实天气 API 的情况下返回城市天气和穿衣建议。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MockWeatherTool implements TravelTool {

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
     * 执行 mock 天气查询。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        String city = task.city() == null ? "目的地" : task.city();
        return new ToolResult(task.taskType(), source(), true,
                city + " mock 天气：白天 18-26 度，早晚偏凉，建议携带薄外套；节假日期间天气需出发前二次确认。",
                null,
                OffsetDateTime.now());
    }

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    @Override
    public String source() {
        return "MockWeatherTool";
    }
}
