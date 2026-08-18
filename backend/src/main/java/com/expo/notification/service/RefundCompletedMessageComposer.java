package com.expo.notification.service;

import com.expo.refund.event.RefundCompletedEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 환불 완료 SMS 의 본문과 저장용 payload 를 만든다.
 *
 * <p>발권 문자와 달리 <b>링크가 없다.</b> 환불은 받는 사람이 더 할 일이 없다 — 확인만 하면 된다. 덕분에 본문이 짧아
 * 90바이트 안에 들어가고 SMS 단가로 나간다 (발권 문자는 링크 때문에 LMS 로 넘어간다).
 */
@Component
public class RefundCompletedMessageComposer {

    private static final String AMOUNT_PATTERN = "#,###";

    private final ObjectMapper objectMapper;

    public RefundCompletedMessageComposer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * SMS 본문.
     *
     * <p>사유는 있을 때만 넣는다. 없는데 "사유: null" 이 나가면 안 된다.
     */
    public String smsText(RefundCompletedEvent event) {
        String base =
                """
                [expo] 환불이 완료되었습니다.
                주문번호 %s
                환불금액 %s원"""
                        .formatted(event.orderNumber(), formatAmount(event.refundAmount()));

        return isBlank(event.reason()) ? base : base + "\n사유 " + event.reason();
    }

    /**
     * {@code notifications.payload} 에 저장할 템플릿 변수.
     *
     * <p>주문번호와 사유는 사람이 입력한 값이 섞일 수 있어 <b>직렬화를 손으로 하지 않는다.</b> 컬럼이 JSONB 라
     * 깨진 JSON 은 INSERT 자체를 실패시키고, 알림도 문자도 통째로 사라진다.
     *
     * <p>예전에는 따옴표·역슬래시·개행만 직접 치환했는데 <b>탭·캐리지리턴 같은 제어문자가 남았다.</b> 그것들도
     * JSON 문자열 안에서는 이스케이프 대상이라 그대로 두면 같은 사고가 난다. 손으로 목록을 채우는 대신 Jackson 에
     * 맡긴다.
     */
    public String payloadJson(RefundCompletedEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("orderNumber", nullToEmpty(event.orderNumber()));
        node.put("refundAmount", event.refundAmount());
        if (isBlank(event.reason())) {
            node.putNull("reason");
        } else {
            node.put("reason", event.reason());
        }
        return node.toString();
    }

    /**
     * 금액을 천 단위로 끊는다.
     *
     * <p>{@link DecimalFormat} 을 <b>매번 새로 만든다.</b> 스레드 안전하지 않아서다. 알림은
     * {@code AFTER_COMMIT} 리스너라 여러 환불이 동시에 커밋되면 서로 다른 스레드가 같이 들어온다. 상수로 공유하면
     * 그때 금액이 뒤섞인 문자가 나갈 수 있다 — 터지지 않고 <b>틀린 값이 나가는</b> 쪽이라 더 나쁘다.
     */
    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0" : new DecimalFormat(AMOUNT_PATTERN).format(amount);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
