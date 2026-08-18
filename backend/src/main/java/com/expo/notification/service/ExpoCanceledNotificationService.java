package com.expo.notification.service;

import com.expo.expo.event.ExpoCanceledEvent;
import com.expo.notification.dto.ExpoCancelTarget;
import com.expo.notification.dto.NotificationRequest;
import com.expo.notification.repository.NotificationRecipientMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 박람회 취소 안내를 관람객 전원에게 보낸다.
 *
 * <h2>앞의 알림들과 다른 점</h2>
 *
 * 발권·환불은 "주문 1건 → 문자 1통" 이었다. 이쪽은 <b>"박람회 1건 → 문자 N통"</b> 이라 셋이 더 붙는다.
 *
 * <pre>
 * ① 중복 방어   한 번만 나가야 한다. 박람회 행을 잠그고 확인한다
 * ② 대상 추리기 수신번호 단위로 중복을 제거한다
 * ③ 대량 발송   한 요청으로 보낸다. 낱건 반복은 50명만 넘어도 못 쓴다
 * </pre>
 *
 * <h2>트랜잭션이 하나여야 한다</h2>
 *
 * ①의 잠금과 ③의 저장이 <b>같은 트랜잭션</b>이어야 중복 방어가 성립한다. 잠금이 먼저 풀리면 두 요청이
 * 나란히 "아직 안 보냄" 을 보고 양쪽 다 보낸다. 그래서 여기서 트랜잭션을 열고,
 * {@link NotificationDispatcher#dispatchMany} 는 그것을 <b>이어받는다</b>({@code REQUIRED}).
 */
@Slf4j
@Service
public class ExpoCanceledNotificationService {

    private static final String TEMPLATE_CODE = "EXPO_CANCELED";

    /**
     * 참조 대상이 <b>박람회</b>다.
     *
     * <p>발권·환불은 {@code ORDER} 였다. 취소 안내는 주문에 딸린 사건이 아니라 박람회에 딸린 사건이고,
     * 수신자마다 1행이라 주문으로 묶을 수도 없다.
     */
    private static final String REFERENCE_TYPE = "EXPO";

    private final OneShotNotificationGuard guard;
    private final NotificationRecipientMapper recipientMapper;
    private final NotificationDispatcher dispatcher;
    private final ExpoCanceledMessageComposer messageComposer;

    public ExpoCanceledNotificationService(
            OneShotNotificationGuard guard,
            NotificationRecipientMapper recipientMapper,
            NotificationDispatcher dispatcher,
            ExpoCanceledMessageComposer messageComposer) {
        this.guard = guard;
        this.recipientMapper = recipientMapper;
        this.dispatcher = dispatcher;
        this.messageComposer = messageComposer;
    }

    /**
     * 취소를 받아 안내를 보낸다.
     *
     * <p>{@code REQUIRES_NEW} 인 이유는 {@code AFTER_COMMIT} 에서 불리기 때문이다. 이미 끝난 트랜잭션이
     * 스레드에 묶여 있어 새로 열지 않으면 저장이 커밋되지 않는다.
     *
     * <p>{@code READ_COMMITTED} 를 명시하는 이유는 가드가 <b>잠금을 얻은 뒤 다시 읽어야</b> 하기
     * 때문이다. 격리 수준이 올라가면 트랜잭션 시작 시점의 스냅샷을 계속 보게 되어 방어가 무력해진다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void notifyExpoCanceled(ExpoCanceledEvent event) {
        if (!guard.claim(event.expoId(), TEMPLATE_CODE, REFERENCE_TYPE)) {
            return;
        }

        List<ExpoCancelTarget> targets = recipientMapper.findExpoCancelTargets(event.expoId());
        if (targets.isEmpty()) {
            // 판 티켓이 없거나 전부 환불된 박람회. 정상이다.
            log.info("박람회 취소 안내 대상 없음 expoId={}", event.expoId());
            return;
        }

        log.info("박람회 취소 안내 발송 시작 expoId={} 대상={}명", event.expoId(), targets.size());

        String payload = messageComposer.payloadJson(event);
        String smsText = messageComposer.smsText(event);

        dispatcher.dispatchMany(
                targets.stream()
                        .map(target -> toRequest(target, event, payload, smsText))
                        .toList());
    }

    /** 문구와 payload 는 대상마다 같다. 한 번 만들어 돌려쓴다. */
    private NotificationRequest toRequest(
            ExpoCancelTarget target, ExpoCanceledEvent event, String payload, String smsText) {
        return new NotificationRequest(
                target.recipient(),
                TEMPLATE_CODE,
                REFERENCE_TYPE,
                event.expoId(),
                payload,
                smsText);
    }
}
