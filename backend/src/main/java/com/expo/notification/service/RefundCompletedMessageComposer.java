package com.expo.notification.service;

import com.expo.refund.event.RefundCompletedEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import org.springframework.stereotype.Component;

/**
 * 환불 완료 SMS 의 본문과 저장용 payload 를 만든다.
 *
 * <p>발권 문자와 달리 <b>링크가 없다.</b> 환불은 받는 사람이 더 할 일이 없다 — 확인만 하면 된다. 덕분에 본문이 짧아
 * 90바이트 안에 들어가고 SMS 단가로 나간다 (발권 문자는 링크 때문에 LMS 로 넘어간다).
 */
@Component
public class RefundCompletedMessageComposer {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,###");

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
     * <p>사유는 사용자가 적었을 수도 있는 자유 입력이라 <b>따옴표·역슬래시를 이스케이프</b>해야 한다. 안 하면 JSONB 컬럼에
     * 넣는 순간 파싱이 깨진다.
     */
    public String payloadJson(RefundCompletedEvent event) {
        return "{\"orderNumber\":\"%s\",\"refundAmount\":%s,\"reason\":%s}"
                .formatted(
                        escape(event.orderNumber()),
                        event.refundAmount() == null
                                ? "null"
                                : event.refundAmount().toPlainString(),
                        isBlank(event.reason()) ? "null" : "\"" + escape(event.reason()) + "\"");
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0" : AMOUNT_FORMAT.format(amount);
    }

    /** JSON 문자열 안에서 의미를 갖는 문자만 최소로 처리한다. */
    private String escape(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
