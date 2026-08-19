package com.expo.notification.service;

import com.expo.expo.event.ExpoCanceledEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 박람회 취소 안내 SMS 의 본문과 저장용 payload 를 만든다.
 *
 * <p>링크가 없다. 받는 사람이 할 일이 없기 때문이다 — 환불은 자동으로 진행되고, 개별 환불 완료는
 * {@code REFUND_COMPLETED} 로 따로 나간다.
 *
 * <p><b>주문번호도 넣지 않는다.</b> 한 사람이 같은 박람회 주문을 여러 건 가지고 있을 수 있어, 그중
 * 하나만 적으면 오히려 헷갈린다. 이 알림은 주문이 아니라 <b>박람회</b>에 딸린 사건이다.
 */
@Component
public class ExpoCanceledMessageComposer {

    private final ObjectMapper objectMapper;

    public ExpoCanceledMessageComposer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * SMS 본문.
     *
     * <p>박람회명 길이에 따라 SMS(90바이트) 를 넘어 LMS 로 전환될 수 있다. 이름이 긴 박람회는 그렇게 되고,
     * 단가가 올라간다. 이름을 줄이면 어느 박람회인지 알 수 없게 되므로 그대로 둔다.
     */
    public String smsText(ExpoCanceledEvent event) {
        String base =
                """
                [expo] 박람회가 취소되었습니다.
                %s
                결제 금액은 전액 환불됩니다."""
                        .formatted(nullToEmpty(event.expoTitle()));

        return isBlank(event.reason()) ? base : base + "\n사유 " + event.reason();
    }

    /**
     * {@code notifications.payload} 에 저장할 템플릿 변수.
     *
     * <p>박람회명과 사유는 사람이 입력한 값이라 <b>직렬화를 손으로 하지 않는다.</b> JSONB 컬럼이라 깨진
     * JSON 은 INSERT 를 실패시키고 알림이 통째로 사라진다. 치환 목록을 손으로 채우면 탭·캐리지리턴 같은
     * 제어문자가 빠진다 — 실제로 빠져 있었다. Jackson 에 맡긴다.
     */
    public String payloadJson(ExpoCanceledEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("expoId", event.expoId());
        node.put("expoTitle", nullToEmpty(event.expoTitle()));
        if (isBlank(event.reason())) {
            node.putNull("reason");
        } else {
            node.put("reason", event.reason());
        }
        return node.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
