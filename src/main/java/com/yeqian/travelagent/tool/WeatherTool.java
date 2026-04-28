package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import com.yeqian.travelagent.infrastructure.client.WeatherApiClient;
import jakarta.annotation.Resource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 真实天气查询工具。
 *
 * <p>优先调用真实天气 API；调用失败时降级到 mock 天气工具，避免中断完整规划流程。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WeatherTool implements TravelTool {

    @Resource
    private WeatherApiClient weatherApiClient;

    @Resource
    private MockWeatherTool mockWeatherTool;

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
     * 执行真实天气查询，失败时返回 mock 降级结果。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        try {
            String city = task.city() == null || task.city().isBlank() ? "目的地" : task.city();
            String rawContent = weatherApiClient.queryCurrentWeather(city);
            return new ToolResult(task.taskType(), source(), true, rawContent, null, OffsetDateTime.now());
        } catch (Exception exception) {
            ToolResult fallback = mockWeatherTool.execute(task);
            return new ToolResult(
                    task.taskType(),
                    source() + " -> " + fallback.source(),
                    true,
                    fallback.rawContent(),
                    "真实天气 API 调用失败，已降级到 mock：" + exception.getMessage(),
                    OffsetDateTime.now()
            );
        }
    }

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    @Override
    public String source() {
        return "WeatherTool";
    }
}
