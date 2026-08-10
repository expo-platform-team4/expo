package com.expo.notification.service;

import com.expo.checkin.dto.TicketIssueResult;
import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.entity.MessageChannel;
import com.expo.notification.entity.MessageHistory;
import com.expo.notification.entity.Notification;
import com.expo.notification.entity.NotificationChannel;
import com.expo.notification.repository.MessageHistoryRepository;
import com.expo.notification.repository.NotificationRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발권 완료 SMS 를 보내고 결과를 남긴다.
 *
 * <p>기록은 두 테이블로 나뉜다.
 *
 * <ul>
 *   <li>{@code notifications} — "이 사람에게 이걸 보내야 한다" 는 <b>작업</b>. 알림당 1행
 *   <li>{@code message_histories} — "몇 번째 시도가 어떻게 됐다" 는 <b>결과</b>. 시도당 1행
 * </ul>
 *
 * <p>지금은 동기 발송이라 시도가 항상 1회지만, 나중에 재시도 워커가 붙으면 한 알림에 여러 행이 쌓인다. 그때 구조를 바꾸지 않아도 되도록 처음부터 나눠 둔다.
 */
@Slf4j
@Service
public class TicketIssuedNotificationService {

    private static final String TEMPLATE_CODE = "TICKET_ISSUED";
    private static final String REFERENCE_TYPE = "ORDER";
    private static final int FIRST_ATTEMPT = 1;

    private final NotificationRepository notificationRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final SmsSender smsSender;
    private final TicketIssuedMessageComposer messageComposer;

    public TicketIssuedNotificationService(
            NotificationRepository notificationRepository,
            MessageHistoryRepository messageHistoryRepository,
            SmsSender smsSender,
            TicketIssuedMessageComposer messageComposer) {
        this.notificationRepository = notificationRepository;
        this.messageHistoryRepository = messageHistoryRepository;
        this.smsSender = smsSender;
        this.messageComposer = messageComposer;
    }

    /**
     * 발권 결과를 받아 SMS 를 보낸다.
     *
     * <p>{@code REQUIRES_NEW} 인 이유 — 이 메서드는 발권 트랜잭션이 <b>커밋된 뒤</b>에 불린다. 그 시점에는 이미 끝난 트랜잭션이
     * 스레드에 묶여 있어서, 새 트랜잭션을 열지 않으면 여기서 저장한 알림이 커밋되지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyTicketIssued(TicketIssueResult result) {
        String payload = messageComposer.payloadJson(result);

        if (!result.hasRecipient()) {
            // 발권은 이미 끝났다. 문자를 못 보낸다고 되돌리지 않는다 — 마이페이지에서 QR 을 볼 수 있다.
            notificationRepository.save(
                    Notification.canceled(
                            result.memberUserId(),
                            NotificationChannel.SMS,
                            TEMPLATE_CODE,
                            REFERENCE_TYPE,
                            result.orderId(),
                            payload,
                            "수신 가능한 휴대폰 번호가 없습니다."));
            log.info("SMS 발송 생략 (수신번호 없음) orderId={}", result.orderId());
            return;
        }

        Notification notification =
                notificationRepository.save(
                        Notification.pending(
                                result.memberUserId(),
                                result.recipientPhoneNumber(),
                                NotificationChannel.SMS,
                                TEMPLATE_CODE,
                                REFERENCE_TYPE,
                                result.orderId(),
                                payload));

        Instant requestedAt = Instant.now();
        MessageSendResult sendResult =
                smsSender.send(result.recipientPhoneNumber(), messageComposer.smsText(result));
        Instant completedAt = Instant.now();

        messageHistoryRepository.save(
                MessageHistory.record(
                        notification.getId(),
                        MessageChannel.SMS,
                        FIRST_ATTEMPT,
                        sendResult,
                        requestedAt,
                        completedAt));

        if (sendResult.success()) {
            notification.markSent(completedAt);
        } else {
            notification.markFailed(sendResult.errorCode());
            log.warn(
                    "SMS 발송 실패 orderId={} notificationId={} errorCode={}",
                    result.orderId(),
                    notification.getId(),
                    sendResult.errorCode());
        }
    }
}
