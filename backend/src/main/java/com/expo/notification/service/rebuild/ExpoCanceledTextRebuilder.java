package com.expo.notification.service.rebuild;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.expo.event.ExpoCanceledEvent;
import com.expo.notification.entity.Notification;
import com.expo.notification.service.ExpoCanceledMessageComposer;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 박람회 취소 안내를 다시 만든다. <b>payload 만으로 완전히 복원된다.</b>
 *
 * <p>이 알림은 대량 발송이라 한 박람회에 수백~수천 행이 생긴다. 재발송은 <b>그중 실패한 한 건만</b>
 * 다시 보내는 것이다. 전체를 다시 보내는 길은 만들지 않았다 — 중복 방어({@code OneShotNotificationGuard})가
 * 두 번째 전체 발송을 막고 있고, 막아야 하는 것이 맞다.
 */
@Component
public class ExpoCanceledTextRebuilder implements NotificationTextRebuilder {

    private final ObjectMapper objectMapper;
    private final ExpoCanceledMessageComposer messageComposer;

    public ExpoCanceledTextRebuilder(
            ObjectMapper objectMapper, ExpoCanceledMessageComposer messageComposer) {
        this.objectMapper = objectMapper;
        this.messageComposer = messageComposer;
    }

    @Override
    public String templateCode() {
        return "EXPO_CANCELED";
    }

    @Override
    public String rebuild(Notification notification) {
        JsonNode payload = parse(notification);
        return messageComposer.smsText(
                new ExpoCanceledEvent(
                        notification.getReferenceId(),
                        text(payload, "expoTitle"),
                        text(payload, "reason")));
    }

    private JsonNode parse(Notification notification) {
        try {
            return objectMapper.readTree(notification.getPayload());
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_REBUILDABLE);
        }
    }

    private String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
