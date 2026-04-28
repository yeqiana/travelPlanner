package com.yeqian.travelagent.agent.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeqian.travelagent.domain.model.TravelIntent;
import com.yeqian.travelagent.infrastructure.ai.JsonExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 旅行意图解析器测试。
 */
class TravelIntentParserTest {

    /**
     * 验证完整自然语言输入可以解析为结构化旅行意图。
     */
    @Test
    void shouldParseCompleteInputByFallbackRules() {
        TravelIntent intent = parser().parse("五一从西安出发去杭州和上海周边玩4天，两个人，预算3000，不想太累");

        assertThat(intent.departureCity()).isEqualTo("西安");
        assertThat(intent.dateText()).isEqualTo("五一");
        assertThat(intent.days()).isEqualTo(4);
        assertThat(intent.peopleCount()).isEqualTo(2);
        assertThat(intent.budget()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(intent.destinationPreferences()).contains("杭州", "上海周边");
        assertThat(intent.travelStyles()).contains("不想太累", "节假日");
    }

    /**
     * 验证缺少人数时解析结果默认按 1 人处理。
     */
    @Test
    void shouldDefaultPeopleCountToOneWhenMissing() {
        TravelIntent intent = parser().parse("五一从西安出发去杭州玩3天，预算2000");

        assertThat(intent.peopleCount()).isEqualTo(1);
    }

    /**
     * 验证无法识别日期时保留为空，交给缺失信息检查器追问。
     */
    @Test
    void shouldKeepDateNullWhenDateMissing() {
        TravelIntent intent = parser().parse("从西安出发去杭州玩3天");

        assertThat(intent.dateText()).isNull();
    }

    /**
     * 构造不接入模型的解析器，让测试稳定走规则兜底。
     *
     * @return 旅行意图解析器
     */
    private TravelIntentParser parser() {
        TravelIntentParser parser = new TravelIntentParser();
        ReflectionTestUtils.setField(parser, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(parser, "jsonExtractor", new JsonExtractor());
        ReflectionTestUtils.setField(parser, "chatClientBuilderProvider", emptyChatClientBuilderProvider());
        return parser;
    }

    /**
     * 构造空模型客户端提供器。
     *
     * @return 空模型客户端提供器
     */
    private ObjectProvider<ChatClient.Builder> emptyChatClientBuilderProvider() {
        return new ObjectProvider<>() {
            /**
             * 获取模型客户端构造器。
             *
             * @param args 参数列表
             * @return 始终返回 null
             */
            @Override
            public ChatClient.Builder getObject(Object... args) {
                return null;
            }

            /**
             * 获取模型客户端构造器。
             *
             * @return 始终返回 null
             */
            @Override
            public ChatClient.Builder getObject() {
                return null;
            }
        };
    }
}
