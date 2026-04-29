package com.yeqian.travelagent.domain.model;

/**
 * 地理坐标点。
 *
 * <p>用于承载地图 API 地理编码后的经纬度和格式化地址。</p>
 *
 * @param longitude 经度
 * @param latitude 纬度
 * @param formattedAddress 格式化地址
 */
public record GeoPoint(
        String longitude,
        String latitude,
        String formattedAddress
) {

    /**
     * 转换为地图 API 常用的经纬度文本。
     *
     * @return 经度和纬度拼接文本
     */
    public String location() {
        return longitude + "," + latitude;
    }
}
