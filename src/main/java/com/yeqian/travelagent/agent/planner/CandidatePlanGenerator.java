package com.yeqian.travelagent.agent.planner;

import com.yeqian.travelagent.domain.model.TravelCandidatePlan;
import com.yeqian.travelagent.domain.model.TravelEvidence;
import com.yeqian.travelagent.domain.model.TravelIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 候选方案生成器。
 *
 * <p>根据旅行意图和证据信息生成少量候选路线方案。</p>
 */
@Component
public class CandidatePlanGenerator {

    /**
     * 生成候选旅行方案。
     *
     * @param intent 旅行意图
     * @param evidences 旅行证据列表
     * @return 候选旅行方案列表
     */
    public List<TravelCandidatePlan> generate(TravelIntent intent, List<TravelEvidence> evidences) {
        String departure = isBlank(intent.departureCity()) ? "出发地" : intent.departureCity();
        List<String> destinations = normalizedDestinations(intent);
        boolean hasHangzhou = destinations.contains("杭州");
        boolean hasShanghai = destinations.stream().anyMatch(item -> item.contains("上海"));
        if (hasHangzhou && hasShanghai) {
            return List.of(
                    new TravelCandidatePlan("杭州 + 上海低疲劳精选", List.of(departure, "杭州", "上海", departure), "跨城次数少，优先保留杭州和上海核心体验，适合不想太累的用户。"),
                    new TravelCandidatePlan("杭州 + 南浔 + 上海顺路游", List.of(departure, "杭州", "南浔", "上海", departure), "南浔位于江浙沪路线中间，能补充古镇体验，整体仍保持顺路。"),
                    new TravelCandidatePlan("杭州 + 苏州 + 上海经典游", List.of(departure, "杭州", "苏州", "上海", departure), "经典城市组合交通便利，景点价值高，但节假日热门城市人流风险更高。")
            );
        }

        if (destinations.size() >= 2) {
            return multiCityPlans(departure, destinations);
        }

        String destination = destinations.isEmpty() ? "目的地待推荐" : destinations.get(0);
        return List.of(
                new TravelCandidatePlan(destination + "轻量游", List.of(departure, destination, departure), "只保留一个核心目的地，交通和体力成本最低。"),
                new TravelCandidatePlan(destination + "深度游", List.of(departure, destination, destination + "周边", departure), "在同城或近郊增加体验层次，兼顾景点丰富度和可控疲劳。")
        );
    }

    /**
     * 生成多城市通用候选方案。
     *
     * @param departure 出发城市
     * @param destinations 目的地列表
     * @return 候选旅行方案列表
     */
    private List<TravelCandidatePlan> multiCityPlans(String departure, List<String> destinations) {
        List<String> compactRoute = new ArrayList<>();
        compactRoute.add(departure);
        compactRoute.addAll(destinations.subList(0, Math.min(2, destinations.size())));
        compactRoute.add(departure);

        List<String> fullRoute = new ArrayList<>();
        fullRoute.add(departure);
        fullRoute.addAll(destinations);
        fullRoute.add(departure);

        return List.of(
                new TravelCandidatePlan(String.join(" + ", compactRoute.subList(1, compactRoute.size() - 1)) + "轻量串联", compactRoute, "压缩跨城数量，优先匹配低疲劳和预算可控。"),
                new TravelCandidatePlan(String.join(" + ", destinations) + "完整串联", fullRoute, "覆盖用户提到的主要目的地，体验更完整，但跨城强度更高。")
        );
    }

    /**
     * 规整目的地偏好。
     *
     * @param intent 旅行意图
     * @return 去重后的目的地列表
     */
    private List<String> normalizedDestinations(TravelIntent intent) {
        return intent.destinationPreferences().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 为空时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
