package com.expo.notification.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.notification.dto.MessageSendResult;
import com.expo.notification.dto.NotificationRetryResult;
import com.expo.notification.entity.MessageChannel;
import com.expo.notification.entity.MessageHistory;
import com.expo.notification.entity.Notification;
import com.expo.notification.entity.NotificationStatus;
import com.expo.notification.repository.MessageHistoryRepository;
import com.expo.notification.repository.NotificationHistoryMapper;
import com.expo.notification.repository.NotificationRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 실패한 알림을 관리자가 다시 보낸다 (D-API-010).
 *
 * <h2>재시도 워커와 다르다</h2>
 *
 * 이것은 <b>사람이 눌러 한 건을 다시 보내는</b> 것이다. 실패한 것을 주기적으로 알아서 집어 가는 워커는
 * 별개이고, 발송을 트랜잭션 밖으로 빼는 설계가 선행돼야 해서 아직 없다.
 *
 * <h2>순서가 방어다</h2>
 *
 * <pre>
 * ① 알림 행 잠금 (FOR UPDATE)   ← 두 번째 요청이 여기서 기다린다
 * ② 상태 재확인                 ← 앞선 요청이 이미 보냈으면 여기서 걸린다
 * ③ 본문 재구성
 * ④ 발송
 * ⑤ 이력 기록 + 상태 갱신
 * </pre>
 *
 * <p>①을 빼면 관리자 둘이 동시에 눌렀을 때 양쪽 다 {@code FAILED} 를 보고 문자가 두 번 나간다. 발권·체크인·
 * 박람회 취소에서 이미 세 번 겪은 구조다.
 *
 * <h2>여기서는 DB 제약도 함께 받쳐 준다</h2>
 *
 * 박람회 취소 때는 {@code (template, type, expoId)} 가 N행이라 UNIQUE 를 걸 수 없었다. 여기는 다르다 —
 * {@code uq_message_histories_attempt (notification_id, attempt_no)} 가 같은 시도 번호를 두 번 넣지
 * 못하게 막는다. <b>잠금이 뚫려도 두 번째 INSERT 가 실패한다.</b> 잠금은 성능(대기), 제약은 정확성을
 * 맡는 이중 방어다.
 *
 * <h2>외부 호출이 잠금 안에 있다</h2>
 *
 * 대행사 왕복(연결 3초 + 응답 10초) 동안 알림 행과 DB 커넥션이 잡혀 있다. 한 건짜리 관리자 조작이라
 * 감수했다. 이 구조를 걷어내는 것은 대량 발송과 같은 문제이고
 * <a href="https://github.com/expo-platform-team4/expo/issues/86">이슈 #86</a> 에 있다.
 *
 * <h2>보낸 뒤 커밋이 실패하면 기록이 사라진다</h2>
 *
 * 잠금 보유 시간과는 <b>다른 문제</b>다. 문자는 이미 나갔는데 커밋이 깨지면 이력도, {@code SENT}
 * 상태도, 발권 알림이라면 <b>새 토큰 행까지</b> 함께 롤백된다. 알림은 {@code FAILED} 로 남아 다시
 * 재발송 대상이 되고, 받는 사람은 두 번째 문자를 받는다. 발권 알림은 더 나쁘다 — 이미 보낸 링크의
 * 토큰 행이 사라져 <b>열리지 않는 링크</b>가 손에 남는다.
 *
 * <p>구조로 막으려면 발송을 트랜잭션 밖으로 빼야 하고, 그건 #86 의 범위다. 여기서는 최소한
 * <b>조용히 사라지지 않게</b> 한다 — 발송이 성공한 트랜잭션이 롤백되면 {@code ERROR} 로 남긴다.
 */
@Slf4j
@Service
public class NotificationRetryService {

    private final NotificationHistoryMapper historyMapper;
    private final NotificationRepository notificationRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final NotificationTextRebuilders textRebuilders;
    private final SmsSender smsSender;

    public NotificationRetryService(
            NotificationHistoryMapper historyMapper,
            NotificationRepository notificationRepository,
            MessageHistoryRepository messageHistoryRepository,
            NotificationTextRebuilders textRebuilders,
            SmsSender smsSender) {
        this.historyMapper = historyMapper;
        this.notificationRepository = notificationRepository;
        this.messageHistoryRepository = messageHistoryRepository;
        this.textRebuilders = textRebuilders;
        this.smsSender = smsSender;
    }

    /**
     * 한 건을 다시 보낸다.
     *
     * <p>{@code READ_COMMITTED} 를 명시하는 이유는 ②가 <b>잠금을 얻은 뒤 다시 읽어야</b> 하기 때문이다.
     * 격리 수준이 올라가면 트랜잭션 시작 시점의 스냅샷을 계속 보게 되어, 앞선 요청이 이미 보낸 사실이
     * 안 보인다.
     *
     * <p>발송에 실패해도 예외를 던지지 않는다. 그것은 요청 처리의 실패가 아니라 <b>결과</b>이고, 이력에
     * 남아야 한다. 응답의 {@code success} 로 알려 준다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public NotificationRetryResult retry(Long notificationId) {
        // ① 먼저 잠근다. 상태를 여기서 함께 읽는다.
        String lockedStatus = historyMapper.lockNotification(notificationId);
        if (lockedStatus == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        // ② 잠금을 얻은 뒤의 상태로 판단한다.
        if (!NotificationStatus.FAILED.name().equals(lockedStatus)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_RETRYABLE);
        }

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        String phoneNumber = notification.getRecipientPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            // 상태가 FAILED 라도 보낼 곳이 없으면 못 보낸다. 눌러도 되는 것처럼 보이면 안 된다.
            throw new BusinessException(ErrorCode.NOTIFICATION_RECIPIENT_UNREACHABLE);
        }

        // ③ 본문을 다시 만든다. TICKET_ISSUED 는 여기서 새 접근 토큰이 발급된다.
        String smsText = textRebuilders.rebuild(notification);

        int attemptNo = historyMapper.nextAttemptNo(notificationId);
        notification.markRetried();

        // ④ 보낸다.
        Instant requestedAt = Instant.now();
        MessageSendResult sendResult = smsSender.send(phoneNumber, smsText);
        Instant completedAt = Instant.now();

        // ⑤ 시도를 남기고 상태를 정한다.
        messageHistoryRepository.save(
                MessageHistory.record(
                        notificationId,
                        MessageChannel.SMS,
                        attemptNo,
                        sendResult,
                        requestedAt,
                        completedAt));

        if (sendResult.success()) {
            notification.markSent(completedAt);
        } else {
            notification.markFailed(sendResult.errorCode());
        }

        log.info(
                "알림 재발송 notificationId={} template={} attemptNo={} 성공={}",
                notificationId,
                notification.getTemplateCode(),
                attemptNo,
                sendResult.success());

        if (sendResult.success()) {
            warnIfRolledBackAfterSending(notificationId, attemptNo);
        }

        return new NotificationRetryResult(
                notificationId,
                attemptNo,
                notification.getStatus().name(),
                sendResult.success(),
                sendResult.success() ? null : sendResult.errorCode());
    }

    /**
     * 문자는 나갔는데 트랜잭션이 롤백되면 크게 남긴다.
     *
     * <p>이 경우 DB 에는 아무 흔적이 없다. 알림은 {@code FAILED} 로 남고 이력도 없어서, <b>로그가
     * 유일한 증거</b>다. 그래서 {@code ERROR} 다 — 사람이 보고 손으로 대사해야 하는 상황이다.
     *
     * <p>{@code afterCompletion} 은 커밋 여부가 정해진 뒤에 불린다. 여기서 DB 를 건드릴 수는 없다
     * (트랜잭션이 이미 끝났다). 남길 수 있는 것은 로그뿐이다.
     */
    private void warnIfRolledBackAfterSending(Long notificationId, int attemptNo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                            log.error(
                                    "문자는 발송됐으나 기록이 롤백됐다 notificationId={} attemptNo={}",
                                    notificationId,
                                    attemptNo);
                        }
                    }
                });
    }
}
