package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * mock 酒店查询工具。
 *
 * <p>用于在不接入真实酒店 API 的情况下返回住宿区域和价格建议。</p>
 */
@Component
public class MockHotelSearchTool implements TravelTool {

    /**
     * 判断是否支持酒店任务。
     *
     * @param taskType 任务类型
     * @return 支持酒店任务返回 true，否则返回 false
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.HOTEL == taskType;
    }

    /**
     * 执行 mock 酒店查询。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        return new ToolResult(task.taskType(), source(), true,
                "mock 酒店建议：" + task.query() + "。建议选择靠近地铁或核心游玩区的住宿，具体酒店、房型和价格需二次确认。",
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
        return "MockHotelSearchTool";
    }
}
