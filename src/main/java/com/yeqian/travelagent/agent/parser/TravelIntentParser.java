package com.yeqian.travelagent.agent.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.infrastructure.ai.JsonExtractor;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 旅行意图解析器。
 *
 * <p>优先调用模型解析用户自然语言需求，模型不可用或解析失败时降级为规则解析。</p>
 */
@Component
public class TravelIntentParser {

    private static final List<String> KNOWN_DESTINATIONS = List.of(
            "江浙沪周边", "上海周边", "川西小环线", "杭州", "上海", "苏州", "南京", "无锡", "湖州", "宁波",
            "重庆", "成都", "都江堰", "桂林", "阳朔", "北京", "西安", "广州", "深圳", "厦门", "青岛"
    );
    private static final List<String> KNOWN_CITY_DESTINATIONS = List.of(
            "杭州", "上海", "苏州", "南京", "无锡", "湖州", "宁波", "重庆", "成都", "都江堰",
            "桂林", "阳朔", "北京", "西安", "广州", "深圳", "厦门", "青岛"
    );
    private static final List<String> DESTINATION_NOISE_SUFFIXES = List.of("游玩", "旅游", "附近", "一带", "周边", "想去", "玩", "去");

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private JsonExtractor jsonExtractor;

    @Resource
    private ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    /**
     * 解析旅行需求文本。
     *
     * @param message 用户输入的旅行需求
     * @return 旅行意图
     */
    public TravelIntent parse(String message) {
        ChatClient chatClient = chatClient();
        if (chatClient != null) {
            try {
                String content = chatClient.prompt()
                        .user(buildPrompt(message))
                        .call()
                        .content();
                return objectMapper.readValue(jsonExtractor.extractObject(content), TravelIntent.class);
            } catch (Exception ignored) {
                return fallbackParse(message);
            }
        }
        return fallbackParse(message);
    }

    /**
     * 创建聊天客户端。
     *
     * @return 聊天客户端，不可用时返回 null
     */
    private ChatClient chatClient() {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        return chatClientBuilder == null ? null : chatClientBuilder.build();
    }

    /**
     * 构建模型解析提示词。
     *
     * @param message 用户输入的旅行需求
     * @return 模型提示词
     */
    private String buildPrompt(String message) {
        return """
                你是一个旅行需求解析器。请把用户输入解析成严格 JSON，不要输出任何解释。
                用户输入：%s

                输出 JSON 字段：
                {
                  "departureCity": null,
                  "dateText": null,
                  "days": null,
                  "peopleCount": null,
                  "budget": null,
                  "destinationPreferences": [],
                  "travelStyles": [],
                  "transportPreference": null,
                  "hotelBudgetPerNight": null,
                  "avoidPlaces": []
                }

                规则：
                1. 没提到的字段填 null 或空数组。
                2. 不要编造用户没有说的信息。
                3. 预算如果用户说的是总预算，填 budget。
                4. 如果用户表达“不想太累”，加入 travelStyles。
                """.formatted(message);
    }

    /**
     * 使用规则解析旅行需求。
     *
     * @param message 用户输入的旅行需求
     * @return 旅行意图
     */
    private TravelIntent fallbackParse(String message) {
        String departureCity = extractDepartureCity(message);
        String dateText = extractDateText(message);
        Integer days = extractNumberBefore(message, "天");
        Integer peopleCount = extractPeopleCount(message);
        BigDecimal budget = extractBudget(message);

        List<String> destinations = extractDestinations(message, departureCity);

        List<String> styles = new ArrayList<>();
        if (message.contains("不想太累") || message.contains("轻松")) {
            styles.add("不想太累");
        }
        if (message.contains("五一") || message.contains("国庆") || message.contains("春节")) {
            styles.add("节假日");
        }

        return new TravelIntent(departureCity, dateText, days, peopleCount, budget, destinations, styles, null, null, List.of());
    }

    /**
     * 提取出发城市。
     *
     * @param message 用户输入的旅行需求
     * @return 出发城市，未识别时返回 null
     */
    private String extractDepartureCity(String message) {
        Matcher matcher = Pattern.compile("从([\\u4e00-\\u9fa5]{2,8})出发").matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher routeMatcher = Pattern.compile("^([\\u4e00-\\u9fa5]{2,8})到[\\u4e00-\\u9fa5]{2,8}").matcher(message);
        if (routeMatcher.find()) {
            return routeMatcher.group(1);
        }
        return null;
    }

    /**
     * 提取目的地偏好。
     *
     * @param message 用户输入的旅行需求
     * @param departureCity 已识别出的出发城市
     * @return 去重后的目的地偏好
     */
    private List<String> extractDestinations(String message, String departureCity) {
        LinkedHashSet<String> destinations = new LinkedHashSet<>();
        addRouteDestinations(message, destinations, departureCity);
        for (String destination : KNOWN_DESTINATIONS) {
            if (message.contains(destination) && !destination.equals(departureCity)) {
                addNormalizedDestinations(destinations, destination, departureCity);
            }
        }
        if (destinations.contains("江浙沪周边")) {
            destinations.remove("江浙沪周边");
            destinations.add("杭州");
            destinations.add("苏州");
            destinations.add("上海");
        }
        if (destinations.contains("川西小环线")) {
            addIfNotDeparture(destinations, "成都", departureCity);
            addIfNotDeparture(destinations, "都江堰", departureCity);
        }
        return new ArrayList<>(destinations);
    }

    /**
     * 从多段路线表达中提取目的地。
     *
     * @param message 用户输入的旅行需求
     * @param destinations 目的地集合
     * @param departureCity 已识别出的出发城市
     */
    private void addRouteDestinations(String message, LinkedHashSet<String> destinations, String departureCity) {
        Matcher matcher = Pattern.compile("(?:到|去|再去|然后去|顺路去)([\\u4e00-\\u9fa5]{2,8})(?=再去|然后去|顺路去|玩|旅游|周边|\\d|，|,|。|$)").matcher(message);
        while (matcher.find()) {
            for (String candidate : matcher.group(1).split("[和与及、]")) {
                addNormalizedDestinations(destinations, candidate, departureCity);
            }
        }
    }

    /**
     * 归一化并加入目的地文本。
     *
     * @param destinations 目的地集合
     * @param candidate 原始目的地文本
     * @param departureCity 已识别出的出发城市
     */
    private void addNormalizedDestinations(LinkedHashSet<String> destinations, String candidate, String departureCity) {
        String normalized = normalizeDestination(candidate);
        if (normalized.isBlank() || normalized.equals(departureCity)) {
            return;
        }
        if ("江浙沪".equals(normalized) || "江浙沪周边".equals(candidate)) {
            addIfNotDeparture(destinations, "杭州", departureCity);
            addIfNotDeparture(destinations, "苏州", departureCity);
            addIfNotDeparture(destinations, "上海", departureCity);
            return;
        }
        List<String> splitCities = splitCombinedDestination(normalized);
        if (!splitCities.isEmpty()) {
            for (String city : splitCities) {
                addIfNotDeparture(destinations, city, departureCity);
            }
            return;
        }
        addIfNotDeparture(destinations, normalized, departureCity);
    }

    /**
     * 清洗目的地文本中的动作词和区域后缀。
     *
     * @param value 原始目的地文本
     * @return 清洗后的目的地文本
     */
    private String normalizeDestination(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim()
                .replaceAll("^[想要打算计划]*去", "")
                .replaceAll("[，,。.!！?？\\s]", "");
        boolean changed;
        do {
            changed = false;
            for (String suffix : DESTINATION_NOISE_SUFFIXES) {
                if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                    normalized = normalized.substring(0, normalized.length() - suffix.length());
                    changed = true;
                }
            }
        } while (changed);
        return normalized.trim();
    }

    /**
     * 拆分粘连的多城市目的地。
     *
     * @param value 清洗后的目的地文本
     * @return 拆分出的城市列表
     */
    private List<String> splitCombinedDestination(String value) {
        List<String> cities = new ArrayList<>();
        String remaining = value;
        for (String city : KNOWN_CITY_DESTINATIONS) {
            if (remaining.contains(city)) {
                cities.add(city);
                remaining = remaining.replace(city, "");
            }
        }
        if (cities.size() <= 1 || !remaining.isBlank()) {
            return List.of();
        }
        return cities;
    }

    /**
     * 如果不是出发地则加入目的地集合。
     *
     * @param destinations 目的地集合
     * @param destination 目的地
     * @param departureCity 出发城市
     */
    private void addIfNotDeparture(LinkedHashSet<String> destinations, String destination, String departureCity) {
        if (destination != null && !destination.isBlank() && !destination.equals(departureCity)) {
            destinations.add(destination);
        }
    }

    /**
     * 提取出发日期文本。
     *
     * @param message 用户输入的旅行需求
     * @return 日期文本，未识别时返回 null
     */
    private String extractDateText(String message) {
        List<String> knownDateTexts = List.of("五一", "国庆", "春节", "元旦", "端午", "中秋", "暑假", "寒假", "周末");
        for (String knownDateText : knownDateTexts) {
            if (message.contains(knownDateText)) {
                return knownDateText;
            }
        }
        Matcher matcher = Pattern.compile("(\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{1,2}-\\d{1,2})").matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 提取出行人数。
     *
     * @param message 用户输入的旅行需求
     * @return 出行人数，未识别时返回 null
     */
    private Integer extractPeopleCount(String message) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(个)?人").matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        if (message.contains("两个人") || message.contains("俩人")) {
            return 2;
        }
        return null;
    }

    /**
     * 提取指定后缀前的数字。
     *
     * @param message 用户输入的旅行需求
     * @param suffix 数字后的单位或后缀
     * @return 数字结果，未识别时返回 null
     */
    private Integer extractNumberBefore(String message, String suffix) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*" + Pattern.quote(suffix)).matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        Matcher chineseMatcher = Pattern.compile("([一二两三四五六七八九十]+)\\s*" + Pattern.quote(suffix)).matcher(message);
        if (chineseMatcher.find()) {
            return parseChineseNumber(chineseMatcher.group(1));
        }
        return null;
    }

    /**
     * 提取总预算。
     *
     * @param message 用户输入的旅行需求
     * @return 总预算，未识别时返回 null
     */
    private BigDecimal extractBudget(String message) {
        Matcher matcher = Pattern.compile("预算\\D*(\\d+)").matcher(message);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
    }

    /**
     * 解析中文数字。
     *
     * @param value 中文数字文本
     * @return 数字值，无法解析时返回 null
     */
    private Integer parseChineseNumber(String value) {
        return switch (value) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> null;
        };
    }

}
