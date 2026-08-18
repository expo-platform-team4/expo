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

    /** 발송기가 결과를 덜 돌려줬을 때 남기는 코드. 실제 발송 여부를 알 수 없다는 뜻이다. */
    private static final String RESULT_MISSING = "RESULT_MISSING";

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
     * 발송한 뒤에 만들면 저장할 대상이 없어 <b>③에서 짝지을 행 자체가 없다.</b> 먼저 만들어 두면 결과를
     * 그 행에 붙이기만 하면 된다.
     *
     * <p><b>죽어도 남는다는 뜻은 아니다.</b> ①②③이 한 트랜잭션이라 커밋 전에 죽으면 ①의
     * {@code PENDING} 행도 함께 사라진다. 순서를 앞으로 옮긴 것이 durability 를 만들어 주지는 않는다.
     * 실제로 durable 하려면 ①을 별도 트랜잭션으로 커밋하고 나가야 하는데, 그러면 호출부의 잠금이 바깥에
     * 남아 중복 방어가 깨진다 — <b>둘 다는 안 된다.</b>
     *
     * <p>그래서 "보냈는데 기록이 없다" 는 창은 커밋 직전까지 열려 있다. 이 창을 닫는 것은 발송 자체를
     * 워커로 분리하는 설계이고, 재시도 워커(D-API-010)에서 함께 다룬다.
     *
     * <h2>외부 호출이 잠금 안에 있다</h2>
     *
     * 호출부가 {@code expos} 행을 잠근 채로 여기 들어오므로, 대행사 왕복(연결 3초 + 응답 10초) 동안
     * 그 행과 DB 커넥션이 잡혀 있다. 다른 리스너들이 {@code AFTER_COMMIT} 을 쓰는 이유가 바로 이걸
     * 피하려는 것인데, 대량 경로는 중복 방어 때문에 감수한다. 규모를 올릴 때 가장 먼저 손볼 곳이다.
     *
     * <h2>낱건 반복이 아닌 이유</h2>
     *
     * 왕복이 한 건에 약 1초라 낱건으로 돌리면 50명만 넘어도 호출부가 1분 가까이 묶인다. 대행사도 낱건
     * 반복을 하지 말라고 명시했다.
     *
     * <h2>{@code dispatch()} 와 전파 방식이 다르다</h2>
     *
     * 이쪽은 {@code REQUIRED} 다. 호출부가 <b>이미 박람회 행을 잠근 트랜잭션</b>을 열어 두었고, 잠금과
     * 알림 저장이 같은 트랜잭션 안에 있어야 중복 방어가 성립하기 때문이다. 여기서 새 트랜잭션을 열면
     * 잠금은 바깥에, 저장은 안쪽에 있게 되어 경계가 갈라진다.
     *
     * <p>트랜잭션이 없는 곳에서 불리면 {@code REQUIRED} 가 알아서 하나 연다.
     *
     * @param requests 보낼 알림들. <b>수신번호가 중복되면 안 된다</b> — 대행사가 걸러 실패로 기록된다
     */
    @Transactional
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
     * <h2>길이가 어긋나면 예외를 던지지 않는다</h2>
     *
     * 원래는 {@link IllegalStateException} 으로 멈췄다. 그런데 여기 도달한 시점에는 <b>문자가 이미
     * 나갔다.</b> 예외를 던지면 트랜잭션이 롤백되면서 ①에서 만든 알림 행까지 전부 사라진다 — 나간 문자에
     * 대한 기록이 하나도 남지 않는다. 막으려던 것과 정반대 결과다.
     *
     * <p>그래서 짝이 맞는 데까지만 반영하고, 결과를 못 받은 나머지는 {@code FAILED} 로 남긴다.
     * 발송기 구현이 계약을 어긴 상황이므로 {@code ERROR} 로 크게 남긴다.
     */
    private void recordResults(
            List<Notification> notifications,
            List<MessageSendResult> results,
            Instant requestedAt,
            Instant completedAt) {
        int paired = Math.min(notifications.size(), results.size());
        if (notifications.size() != results.size()) {
            // 짝이 맞는 만큼만 반영하고 나머지는 RESULT_MISSING 으로 남는다.
            log.error(
                    "발송 결과 개수 불일치 요청={}건 결과={}건 반영={}건",
                    notifications.size(),
                    results.size(),
                    paired);
        }

        List<MessageHistory> histories = new ArrayList<>(paired);
        int failed = 0;

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);

            if (i >= paired) {
                // 결과를 못 받은 몫. 이력에는 남길 것이 없고 상태만 실패로 둔다.
                notification.markFailed(RESULT_MISSING);
                failed++;
                continue;
            }

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
            Notification first = notifications.get(0);
            log.warn(
                    "대량 알림 일부 실패 template={} refType={} refId={} 전체={}건 실패={}건",
                    first.getTemplateCode(),
                    first.getReferenceType(),
                    first.getReferenceId(),
                    notifications.size(),
                    failed);
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
