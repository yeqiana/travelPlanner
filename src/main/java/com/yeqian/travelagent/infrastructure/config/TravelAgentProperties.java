package com.yeqian.travelagent.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 旅行 Agent 配置属性。
 *
 * <p>集中管理模型供应商、候选方案数量和每日行程限制等配置。</p>
 */
@ConfigurationProperties(prefix = "travel-agent")
public class TravelAgentProperties {

    private Ai ai = new Ai();
    private Planning planning = new Planning();
    private ExternalApi externalApi = new ExternalApi();

    /**
     * 获取 AI 配置。
     *
     * @return AI 配置
     */
    public Ai getAi() {
        return ai;
    }

    /**
     * 设置 AI 配置。
     *
     * @param ai AI 配置
     */
    public void setAi(Ai ai) {
        this.ai = ai;
    }

    /**
     * 获取规划配置。
     *
     * @return 规划配置
     */
    public Planning getPlanning() {
        return planning;
    }

    /**
     * 设置规划配置。
     *
     * @param planning 规划配置
     */
    public void setPlanning(Planning planning) {
        this.planning = planning;
    }

    /**
     * 获取外部 API 配置。
     *
     * @return 外部 API 配置
     */
    public ExternalApi getExternalApi() {
        return externalApi;
    }

    /**
     * 设置外部 API 配置。
     *
     * @param externalApi 外部 API 配置
     */
    public void setExternalApi(ExternalApi externalApi) {
        this.externalApi = externalApi;
    }

    /**
     * AI 配置。
     *
     * <p>描述当前启用的模型供应商、模型名和温度参数。</p>
     */
    public static class Ai {
        private String provider = "dashscope";
        private String model = "qwen-plus";
        private Double temperature = 0.4;

        /**
         * 获取模型供应商。
         *
         * @return 模型供应商
         */
        public String getProvider() {
            return provider;
        }

        /**
         * 设置模型供应商。
         *
         * @param provider 模型供应商
         */
        public void setProvider(String provider) {
            this.provider = provider;
        }

        /**
         * 获取模型名称。
         *
         * @return 模型名称
         */
        public String getModel() {
            return model;
        }

        /**
         * 设置模型名称。
         *
         * @param model 模型名称
         */
        public void setModel(String model) {
            this.model = model;
        }

        /**
         * 获取模型温度。
         *
         * @return 模型温度
         */
        public Double getTemperature() {
            return temperature;
        }

        /**
         * 设置模型温度。
         *
         * @param temperature 模型温度
         */
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
    }

    /**
     * 规划配置。
     *
     * <p>描述 MVP 阶段的候选方案和每日行程约束。</p>
     */
    public static class Planning {
        private int maxCandidatePlanCount = 3;
        private int maxDailyAttractionCount = 3;
        private int maxDailyCrossCityCount = 1;
        private int sessionTtlHours = 24;

        /**
         * 获取最大候选方案数量。
         *
         * @return 最大候选方案数量
         */
        public int getMaxCandidatePlanCount() {
            return maxCandidatePlanCount;
        }

        /**
         * 设置最大候选方案数量。
         *
         * @param maxCandidatePlanCount 最大候选方案数量
         */
        public void setMaxCandidatePlanCount(int maxCandidatePlanCount) {
            this.maxCandidatePlanCount = maxCandidatePlanCount;
        }

        /**
         * 获取每日最大景点数量。
         *
         * @return 每日最大景点数量
         */
        public int getMaxDailyAttractionCount() {
            return maxDailyAttractionCount;
        }

        /**
         * 设置每日最大景点数量。
         *
         * @param maxDailyAttractionCount 每日最大景点数量
         */
        public void setMaxDailyAttractionCount(int maxDailyAttractionCount) {
            this.maxDailyAttractionCount = maxDailyAttractionCount;
        }

        /**
         * 获取每日最大跨城次数。
         *
         * @return 每日最大跨城次数
         */
        public int getMaxDailyCrossCityCount() {
            return maxDailyCrossCityCount;
        }

        /**
         * 设置每日最大跨城次数。
         *
         * @param maxDailyCrossCityCount 每日最大跨城次数
         */
        public void setMaxDailyCrossCityCount(int maxDailyCrossCityCount) {
            this.maxDailyCrossCityCount = maxDailyCrossCityCount;
        }

        /**
         * 获取多轮追问会话过期小时数。
         *
         * @return 会话过期小时数
         */
        public int getSessionTtlHours() {
            return sessionTtlHours;
        }

        /**
         * 设置多轮追问会话过期小时数。
         *
         * @param sessionTtlHours 会话过期小时数
         */
        public void setSessionTtlHours(int sessionTtlHours) {
            this.sessionTtlHours = sessionTtlHours;
        }
    }

    /**
     * 外部 API 配置。
     *
     * <p>集中管理真实天气和搜索接口配置，密钥必须通过环境变量注入。</p>
     */
    public static class ExternalApi {
        private Weather weather = new Weather();
        private Search search = new Search();
        private Amap amap = new Amap();

        /**
         * 获取天气 API 配置。
         *
         * @return 天气 API 配置
         */
        public Weather getWeather() {
            return weather;
        }

        /**
         * 设置天气 API 配置。
         *
         * @param weather 天气 API 配置
         */
        public void setWeather(Weather weather) {
            this.weather = weather;
        }

        /**
         * 获取搜索 API 配置。
         *
         * @return 搜索 API 配置
         */
        public Search getSearch() {
            return search;
        }

        /**
         * 设置搜索 API 配置。
         *
         * @param search 搜索 API 配置
         */
        public void setSearch(Search search) {
            this.search = search;
        }

        /**
         * 获取高德地图 API 配置。
         *
         * @return 高德地图 API 配置
         */
        public Amap getAmap() {
            return amap;
        }

        /**
         * 设置高德地图 API 配置。
         *
         * @param amap 高德地图 API 配置
         */
        public void setAmap(Amap amap) {
            this.amap = amap;
        }
    }

    /**
     * 天气 API 配置。
     *
     * <p>默认使用 OpenWeatherMap 当前天气接口。</p>
     */
    public static class Weather {
        private String apiKey = "";
        private String baseUrl = "https://api.openweathermap.org";
        private int timeoutMillis = 3000;

        /**
         * 获取天气 API Key。
         *
         * @return 天气 API Key
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * 设置天气 API Key。
         *
         * @param apiKey 天气 API Key
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 获取天气 API 基础地址。
         *
         * @return 天气 API 基础地址
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置天气 API 基础地址。
         *
         * @param baseUrl 天气 API 基础地址
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取天气 API 超时时间。
         *
         * @return 超时时间，单位毫秒
         */
        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        /**
         * 设置天气 API 超时时间。
         *
         * @param timeoutMillis 超时时间，单位毫秒
         */
        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }

    /**
     * 搜索 API 配置。
     *
     * <p>默认使用 Tavily 搜索接口。</p>
     */
    public static class Search {
        private String apiKey = "";
        private String baseUrl = "https://api.tavily.com";
        private int timeoutMillis = 5000;
        private int maxResults = 5;

        /**
         * 获取搜索 API Key。
         *
         * @return 搜索 API Key
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * 设置搜索 API Key。
         *
         * @param apiKey 搜索 API Key
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 获取搜索 API 基础地址。
         *
         * @return 搜索 API 基础地址
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置搜索 API 基础地址。
         *
         * @param baseUrl 搜索 API 基础地址
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取搜索 API 超时时间。
         *
         * @return 超时时间，单位毫秒
         */
        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        /**
         * 设置搜索 API 超时时间。
         *
         * @param timeoutMillis 超时时间，单位毫秒
         */
        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        /**
         * 获取搜索结果数量上限。
         *
         * @return 搜索结果数量上限
         */
        public int getMaxResults() {
            return maxResults;
        }

        /**
         * 设置搜索结果数量上限。
         *
         * @param maxResults 搜索结果数量上限
         */
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
    }

    /**
     * 高德地图 API 配置。
     *
     * <p>用于路线查询和地理编码，密钥必须通过环境变量注入。</p>
     */
    public static class Amap {
        private String apiKey = "";
        private String baseUrl = "https://restapi.amap.com";
        private int timeoutMillis = 3000;

        /**
         * 获取高德地图 API Key。
         *
         * @return 高德地图 API Key
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * 设置高德地图 API Key。
         *
         * @param apiKey 高德地图 API Key
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * 获取高德地图 API 基础地址。
         *
         * @return 高德地图 API 基础地址
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * 设置高德地图 API 基础地址。
         *
         * @param baseUrl 高德地图 API 基础地址
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * 获取高德地图 API 超时时间。
         *
         * @return 超时时间，单位毫秒
         */
        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        /**
         * 设置高德地图 API 超时时间。
         *
         * @param timeoutMillis 超时时间，单位毫秒
         */
        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }
}
