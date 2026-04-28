package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * mock 路线查询工具。
 *
 * <p>用于在不接入真实地图 API 的情况下返回路线顺序和跨城强度建议。</p>
 */
@Component
public class MockRouteTool implements TravelTool {

    /**
     * 判断是否支持路线任务。
     *
     * @param taskType 任务类型
     * @return 支持路线任务返回 true，否则返回 false
     */
    @Override
    public boolean supports(TravelTaskType taskType) {
        return TravelTaskType.ROUTE == taskType;
    }

    /**
     * 执行 mock 路线查询。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    @Override
    public ToolResult execute(TravelTask task) {
        return new ToolResult(task.taskType(), source(), true,
                "mock 路线建议：" + task.query() + "。多城市路线建议每天跨城不超过 1 次，精确路程和耗时需二次确认。",
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
        return "MockRouteTool";
    }
}
