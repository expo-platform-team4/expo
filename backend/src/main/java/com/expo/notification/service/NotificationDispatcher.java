package com.expo.notification.service;

import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.NotificationRequest;
import com.expo.notification.dto.SmsMessage;
import com.expo.notification.entity.MessageChannel;
import com.expo.notification.entity.MessageHistory;
import com.expo.notification.entity.Notification;
import com.expo.notification.repository.MessageHistoryRepository;
import com.expo.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림을 보내고 두 테이블에 기록한다. <b>모든 알림 종류가 이 하나를 거친다.</b>
 *
 * <h2>왜 뽑았나</h2>
 *
 * 발권·환불·박람회 취소가 보내는 문구는 다르지만, <b>보내고 기록하는 절차는 완전히 같다.</b> 종류마다 이 30여 줄을 복사하면
 * "수신번호 없으면 CANCELED", "실패도 이력으로" 같은 규칙이 조금씩 어긋나기 시작한다. 종류별 서비스는 {@link
 * NotificationRequest} 를 만들기만 하고, 규칙은 여기 한 곳에 둔다.
 *
 * <h2>기록은 두 테이블로 나뉜다</h2>
 *
 * <ul>
 *   <li>{@code notifications} — "이 사람에게 이걸 보내야 한다" 는 <b>작업</b>. 알림당 1행
 *   <li>{@code message_histories} — "몇 번째 시도가 어떻게 됐다" 는 <b>결과</b>. 시도당 1행
 * </ul>
 */
@Slf4j
@Service
public class NotificationDispatcher {

    /** 재시도 워커가 없어 항상 1회다. 붙일 때는 이 알림의 시도 수를 세어 넘겨야 한다. */
    private static final int FIRST_ATTEMPT = 1;

    private final NotificationRepository notificationRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final SmsSender smsSender;

    public NotificationDispatcher(
            NotificationRepository notificationRepository,
            MessageHistoryRepository messageHistoryRepository,
            SmsSender smsSender) {
        this.notificationRepository = notificationRepository;
        this.messageHistoryRepository = messageHistoryRepository;
        this.smsSender = smsSender;
    }

    /**
     * 보내고 기록한다.
     *
     * <p>{@code REQUIRES_NEW} 인 이유 — 이 메서드는 원래 트랜잭션이 <b>커밋된 뒤</b>({@code AFTER_COMMIT})에
     * 불린다. 그 시점에는 이미 끝난 트랜잭션이 스레드에 묶여 있어서, 새 트랜잭션을 열지 않으면 여기서 저장한 알림이
     * <b>커밋되지 않는다.</b> 예외도 안 나서 조용히 사라진다.
     *
     * <p>예외를 던지지 않는다. 발송 실패로 이미 커밋된 원래 작업(발권·환불)을 되돌릴 수 없고, 되돌릴 이유도 없다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationRequest request) {
        if (!request.recipient().reachable()) {
            recordUnreachable(request);
            return;
        }

        Notification notification =
                notificationRepository.save(
                        Notification.pending(
                                request.recipient().userId(),
                                request.recipient().phoneNumber(),
                                request.channel(),
                                request.templateCode(),
                                request.referenceType(),
                                request.referenceId(),
                                request.payload()));

        Instant requestedAt = Instant.now();
        MessageSendResult sendResult =
                smsSender.send(request.recipient().phoneNumber(), request.smsText());
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
                    "알림 발송 실패 template={} refType={} refId={} notificationId={} errorCode={}",
                    request.templateCode(),
                    request.referenceType(),
                    request.referenceId(),
                    notification.getId(),
                    sendResult.errorCode());
        }
    }

    /**
     * <b>여러 건을 한 번에</b> 보내고 기록한다. 박람회 취소처럼 대상이 N명인 알림이 쓴다.
     *
     * <pre>
     * ① notifications N행을 PENDING 으로 저장   (배치)
     * ② sendMany 로 한 요청에 발송
     * ③ 응답을 순서대로 짝지어 SENT / FAILED 로 갱신
     * </pre>
     *
     * <h2>왜 ①이 먼저인가</h2>
     *
     * 발송 뒤에 저장하면 <b>중간에 죽는 순간 기록이 통째로 사라진다.</b> 문자는 이미 나갔는데 보낸 적이
     * 없는 것처럼 보이고, 다시 돌리면 두 번 간다. 먼저 저장해 두면 {@code PENDING} 이 남아 나중에
     * 재시도 워커가 주울 수 있다.
     *
     * <h2>낱건 반복이 아닌 이유</h2>
     *
     * 왕복이 한 건에 약 1초라 낱건으로 돌리면 50명만 넘어도 호출부가 1분 가까이 묶인다. 대행사도 낱건
     * 반복을 하지 말라고 명시했다.
     *
     * @param requests 보낼 알림들. <b>수신번호가 중복되면 안 된다</b> — 대행사가 걸러 실패로 기록된다
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchMany(List<NotificationRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }

        // 번호가 없는 대상은 발송 대상에서 빼되, CANCELED 로 기록은 남긴다.
        List<NotificationRequest> reachable =
                requests.stream().filter(r -> r.recipient().reachable()).toList();
        requests.stream().filter(r -> !r.recipient().reachable()).forEach(this::recordUnreachable);

        if (reachable.isEmpty()) {
            return;
        }

        // ① 먼저 저장한다. 죽어도 PENDING 이 남는다.
        List<Notification> notifications =
                notificationRepository.saveAll(reachable.stream().map(this::toPending).toList());

        // ② 한 요청으로 보낸다.
        List<SmsMessage> messages =
                reachable.stream()
                        .map(r -> new SmsMessage(r.recipient().phoneNumber(), r.smsText()))
                        .toList();

        Instant requestedAt = Instant.now();
        List<MessageSendResult> results = smsSender.sendMany(messages);
        Instant completedAt = Instant.now();

        // ③ 순서로 짝짓는다. sendMany 가 입력과 같은 순서·길이를 보장한다.
        recordResults(notifications, results, requestedAt, completedAt);
    }

    private Notification toPending(NotificationRequest request) {
        return Notification.pending(
                request.recipient().userId(),
                request.recipient().phoneNumber(),
                request.channel(),
                request.templateCode(),
                request.referenceType(),
                request.referenceId(),
                request.payload());
    }

    /**
     * 결과를 알림에 반영하고 이력을 남긴다.
     *
     * <p>길이가 어긋나면 <b>짝이 밀려 엉뚱한 사람의 결과가 기록된다.</b> 발송기 구현이 계약을 어긴
     * 경우인데, 조용히 넘어가면 원인을 찾기 어려우므로 여기서 멈춘다.
     */
    private void recordResults(
            List<Notification> notifications,
            List<MessageSendResult> results,
            Instant requestedAt,
            Instant completedAt) {
        if (notifications.size() != results.size()) {
            throw new IllegalStateException(
                    "발송 결과 개수가 요청과 다릅니다. 요청=%d 결과=%d"
                            .formatted(notifications.size(), results.size()));
        }

        List<MessageHistory> histories = new ArrayList<>(notifications.size());
        int failed = 0;

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            MessageSendResult result = results.get(i);

            histories.add(
                    MessageHistory.record(
                            notification.getId(),
                            MessageChannel.SMS,
                            FIRST_ATTEMPT,
                            result,
                            requestedAt,
                            completedAt));

            if (result.success()) {
                notification.markSent(completedAt);
            } else {
                notification.markFailed(result.errorCode());
                failed++;
            }
        }

        messageHistoryRepository.saveAll(histories);

        if (failed > 0) {
            log.warn("대량 알림 일부 실패 전체={}건 실패={}건", notifications.size(), failed);
        }
    }

    /**
     * 보낼 수 없는 경우. <b>시도하지 않지만 기록은 남긴다.</b>
     *
     * <p>{@code FAILED} 가 아니라 {@code CANCELED} 인 이유는 재시도로 해결되는 문제가 아니어서다. 뭉뚱그리면 나중에
     * 재시도 워커가 보낼 수 없는 알림을 영원히 다시 시도한다.
     *
     * <p>{@code message_histories} 에는 아무것도 넣지 않는다. 시도 자체가 없었다.
     */
    private void recordUnreachable(NotificationRequest request) {
        notificationRepository.save(
                Notification.canceled(
                        request.recipient().userId(),
                        request.channel(),
                        request.templateCode(),
                        request.referenceType(),
                        request.referenceId(),
                        request.payload(),
                        "수신 가능한 휴대폰 번호가 없습니다."));

        log.info(
                "알림 발송 생략 (수신번호 없음) template={} refType={} refId={}",
                request.templateCode(),
                request.referenceType(),
                request.referenceId());
    }
}
