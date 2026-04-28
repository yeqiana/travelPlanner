package com.yeqian.travelagent.tool;

import com.yeqian.travelagent.domain.enums.TravelTaskType;
import com.yeqian.travelagent.domain.model.ToolResult;
import com.yeqian.travelagent.domain.model.TravelTask;

/**
 * 旅行工具接口。
 *
 * <p>定义内部工具服务的统一匹配和执行入口，当前阶段用于承载 mock 工具闭环。</p>
 */
public interface TravelTool {

    /**
     * 判断工具是否支持指定任务类型。
     *
     * @param taskType 任务类型
     * @return 支持返回 true，否则返回 false
     */
    boolean supports(TravelTaskType taskType);

    /**
     * 执行旅行查询任务。
     *
     * @param task 旅行查询任务
     * @return 工具调用结果
     */
    ToolResult execute(TravelTask task);

    /**
     * 获取工具来源名称。
     *
     * @return 工具来源名称
     */
    String source();
}
