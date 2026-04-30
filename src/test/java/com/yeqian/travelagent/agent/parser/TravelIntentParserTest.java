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
        assertThat(intent.destinationPreferences()).contains("杭州", "上海");
        assertThat(intent.destinationPreferences()).doesNotContain("上海周边", "杭州和上海周边");
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
     * 验证多段路线表达可以解析出出发地和多个目的地。
     */
    @Test
    void shouldParseMultiCityRouteExpression() {
        TravelIntent intent = parser().parse("西安到杭州再去上海玩4天，两个人，预算5000");

        assertThat(intent.departureCity()).isEqualTo("西安");
        assertThat(intent.destinationPreferences()).contains("杭州", "上海");
    }

    /**
     * 验证模糊目的地表达可以保留区域偏好并补充代表城市。
     */
    @Test
    void shouldParseFuzzyDestinationExpression() {
        TravelIntent intent = parser().parse("五一从西安出发去江浙沪周边玩4天，两个人");

        assertThat(intent.destinationPreferences()).contains("杭州", "苏州", "上海");
        assertThat(intent.destinationPreferences()).doesNotContain("江浙沪周边", "上海周边");
    }

    /**
     * 验证主题路线表达可以解析为目的地偏好。
     */
    @Test
    void shouldParseLoopRouteExpression() {
        TravelIntent intent = parser().parse("暑假从成都出发走川西小环线5天");

        assertThat(intent.departureCity()).isEqualTo("成都");
        assertThat(intent.destinationPreferences()).contains("川西小环线", "都江堰");
    }

    /**
     * 验证目的地后缀动作词会被清理。
     */
    @Test
    void shouldCleanActionSuffixFromDestination() {
        TravelIntent intent = parser().parse("五一从西安出发，想去杭州玩");

        assertThat(intent.destinationPreferences()).contains("杭州");
        assertThat(intent.destinationPreferences()).doesNotContain("杭州玩");
    }

    /**
     * 验证粘连的城市周边表达会拆分为城市。
     */
    @Test
    void shouldSplitCombinedCityAreaExpression() {
        TravelIntent intent = parser().parse("五一从西安出发，想去杭州上海周边");

        assertThat(intent.destinationPreferences()).contains("杭州", "上海");
        assertThat(intent.destinationPreferences()).doesNotContain("杭州上海周边", "上海周边");
    }

    /**
     * 验证带连接词的城市周边表达会拆分为城市。
     */
    @Test
    void shouldSplitCombinedCityAreaExpressionWithConnector() {
        TravelIntent intent = parser().parse("五一从西安出发，想去杭州和上海周边");

        assertThat(intent.destinationPreferences()).contains("杭州", "上海");
        assertThat(intent.destinationPreferences()).doesNotContain("杭州和上海周边", "上海周边");
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
