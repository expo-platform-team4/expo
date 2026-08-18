package com.expo.notification.service.rebuild;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.entity.Notification;
import com.expo.notification.service.RefundCompletedMessageComposer;
import com.expo.refund.event.RefundCompletedEvent;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 환불 완료 문자를 다시 만든다. <b>payload 만으로 완전히 복원된다.</b>
 *
 * <p>본문에 들어가는 값(주문번호·금액·사유)이 모두 {@code notifications.payload} 에 있다. 새로 만드는 것이
 * 없으므로 몇 번을 재발송해도 같은 문자가 나간다.
 *
 * <p><b>문구를 만드는 일은 {@link RefundCompletedMessageComposer} 에 그대로 맡긴다.</b> 여기서 문자열을
 * 다시 조립하면 최초 발송과 재발송의 문구가 갈린다 — 한쪽만 고쳐지는 사고가 나는 자리다.
 */
@Component
public class RefundCompletedTextRebuilder implements NotificationTextRebuilder {

    private final ObjectMapper objectMapper;
    private final RefundCompletedMessageComposer messageComposer;

    public RefundCompletedTextRebuilder(
            ObjectMapper objectMapper, RefundCompletedMessageComposer messageComposer) {
        this.objectMapper = objectMapper;
        this.messageComposer = messageComposer;
    }

    @Override
    public String templateCode() {
        return "REFUND_COMPLETED";
    }

    @Override
    public String rebuild(Notification notification) {
        JsonNode payload = parse(notification);
        return messageComposer.smsText(
                new RefundCompletedEvent(
                        notification.getReferenceId(),
                        text(payload, "orderNumber"),
                        amount(payload),
                        text(payload, "reason")));
    }

    private JsonNode parse(Notification notification) {
        try {
            return objectMapper.readTree(notification.getPayload());
        } catch (JacksonException e) {
            // payload 가 깨졌다면 본문을 만들 근거가 없다. 추측해서 보내는 것보다 막는 편이 낫다.
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }
    }

    private String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private BigDecimal amount(JsonNode payload) {
        JsonNode value = payload.get("refundAmount");
        return value == null || value.isNull() ? null : value.decimalValue();
    }
}
