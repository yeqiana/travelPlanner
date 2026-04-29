package com.yeqian.travelagent.domain.model;

/**
 * 路线查询参数。
 *
 * <p>用于承载地图路线查询所需的出发地、目的地和已解析坐标。</p>
 *
 * @param origin 出发地文本
 * @param destination 目的地文本
 * @param originLocation 出发地坐标
 * @param destinationLocation 目的地坐标
 * @param departureDate 出发日期文本
 * @param transportMode 交通方式
 */
public record RouteQuery(
        String origin,
        String destination,
        GeoPoint originLocation,
        GeoPoint destinationLocation,
        String departureDate,
        String transportMode
) {
}
