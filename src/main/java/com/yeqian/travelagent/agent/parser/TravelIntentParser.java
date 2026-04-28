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

        List<String> destinations = new ArrayList<>();
        addIfMentioned(message, destinations, "杭州");
        addIfMentioned(message, destinations, "上海周边");
        if (!destinations.contains("上海周边")) {
            addIfMentioned(message, destinations, "上海");
        }
        addIfMentioned(message, destinations, "苏州");
        addIfMentioned(message, destinations, "南京");
        addIfMentioned(message, destinations, "重庆");
        addIfMentioned(message, destinations, "成都");
        addIfMentioned(message, destinations, "桂林");
        addIfMentioned(message, destinations, "阳朔");

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
        return null;
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

    /**
     * 如果消息中提到指定值则加入列表。
     *
     * @param message 用户输入的旅行需求
     * @param values 目标列表
     * @param value 待匹配文本
     */
    private void addIfMentioned(String message, List<String> values, String value) {
        if (message.contains(value)) {
            values.add(value);
        }
    }
}
