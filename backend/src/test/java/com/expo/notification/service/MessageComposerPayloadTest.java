package com.expo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.expo.expo.event.ExpoCanceledEvent;
import com.expo.refund.event.RefundCompletedEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * payload 가 <b>언제나 유효한 JSON</b> 인지 못박는다.
 *
 * <p>이 값은 {@code notifications.payload}(JSONB) 에 그대로 들어간다. 깨진 JSON 은 예외가 아니라 <b>INSERT
 * 실패</b>로 나타나고, 그러면 알림 행도 문자도 통째로 사라진다.
 *
 * <p>원래는 따옴표·역슬래시·개행만 손으로 치환했다. 그 목록에 <b>탭과 캐리지리턴이 빠져 있었다.</b> 사람이 목록을 채우는
 * 방식은 이렇게 조용히 구멍이 난다. 여기서 검사하는 것은 "특정 문자를 잘 바꿨나" 가 아니라 <b>"어떤 입력이 와도 다시
 * 읽히나"</b> 다.
 */
class MessageComposerPayloadTest {

    /** 사람이 입력하는 자유 문자열에 들어올 수 있는 것들을 한 번에 담았다. */
    private static final String NASTY = "따옴표\" 역슬래시\\ 탭\t 개행\n 캐리지리턴\r 끝";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExpoCanceledMessageComposer expoCanceled =
            new ExpoCanceledMessageComposer(new ObjectMapper());

    private final RefundCompletedMessageComposer refundCompleted =
            new RefundCompletedMessageComposer(new ObjectMapper());

    @Test
    void expoCanceledPayloadStaysValidJson() {
        String payload = expoCanceled.payloadJson(new ExpoCanceledEvent(7L, "박람회" + NASTY, NASTY));

        JsonNode parsed = parse(payload);
        assertThat(parsed.get("expoId").asInt()).isEqualTo(7);
        assertThat(parsed.get("expoTitle").asString()).isEqualTo("박람회" + NASTY);
        assertThat(parsed.get("reason").asString()).isEqualTo(NASTY);
    }

    @Test
    void refundCompletedPayloadStaysValidJson() {
        String payload =
                refundCompleted.payloadJson(
                        new RefundCompletedEvent(
                                1L, "ORD" + NASTY, new BigDecimal("12345.67"), NASTY));

        JsonNode parsed = parse(payload);
        assertThat(parsed.get("orderNumber").asString()).isEqualTo("ORD" + NASTY);
        assertThat(parsed.get("refundAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("12345.67"));
        assertThat(parsed.get("reason").asString()).isEqualTo(NASTY);
    }

    /** 사유가 없으면 빈 문자열이 아니라 JSON null 이다. "사유 없음" 과 "사유가 빈 값" 은 다르다. */
    @Test
    void blankReasonBecomesJsonNull() {
        JsonNode expo = parse(expoCanceled.payloadJson(new ExpoCanceledEvent(7L, "박람회", "  ")));
        JsonNode refund =
                parse(
                        refundCompleted.payloadJson(
                                new RefundCompletedEvent(1L, "ORD-1", BigDecimal.TEN, null)));

        assertThat(expo.get("reason").isNull()).isTrue();
        assertThat(refund.get("reason").isNull()).isTrue();
    }

    /** 금액이 없어도 깨지지 않는다. 숫자 자리에 JSON null 이 들어간다. */
    @Test
    void nullAmountBecomesJsonNull() {
        JsonNode parsed =
                parse(
                        refundCompleted.payloadJson(
                                new RefundCompletedEvent(1L, "ORD-1", null, null)));

        assertThat(parsed.get("refundAmount").isNull()).isTrue();
    }

    private JsonNode parse(String payload) {
        assertThatCode(() -> objectMapper.readTree(payload)).doesNotThrowAnyException();
        return objectMapper.readTree(payload);
    }
}
