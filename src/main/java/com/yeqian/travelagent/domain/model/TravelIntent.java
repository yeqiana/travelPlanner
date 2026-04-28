package com.yeqian.travelagent.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 旅行意图。
 *
 * <p>承载从用户自然语言中解析出的出发地、时间、目的地、预算和偏好等信息。</p>
 *
 * @param departureCity 出发城市
 * @param dateText 出发日期文本
 * @param days 出行天数
 * @param peopleCount 出行人数
 * @param budget 总预算
 * @param destinationPreferences 目的地偏好
 * @param travelStyles 旅行风格
 * @param transportPreference 交通偏好
 * @param hotelBudgetPerNight 每晚酒店预算
 * @param avoidPlaces 避开地点
 */
public record TravelIntent(
        String departureCity,
        String dateText,
        Integer days,
        Integer peopleCount,
        BigDecimal budget,
        List<String> destinationPreferences,
        List<String> travelStyles,
        String transportPreference,
        BigDecimal hotelBudgetPerNight,
        List<String> avoidPlaces
) {
    /**
     * 创建旅行意图并规整默认值和空集合字段。
     */
    public TravelIntent {
        peopleCount = peopleCount == null ? 1 : peopleCount;
        destinationPreferences = destinationPreferences == null ? List.of() : List.copyOf(destinationPreferences);
        travelStyles = travelStyles == null ? List.of() : List.copyOf(travelStyles);
        avoidPlaces = avoidPlaces == null ? List.of() : List.copyOf(avoidPlaces);
    }
}
