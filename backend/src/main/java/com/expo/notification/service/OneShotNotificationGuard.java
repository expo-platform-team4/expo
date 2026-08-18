package com.expo.notification.service;

import com.expo.notification.repository.NotificationRecipientMapper;
import com.expo.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <b>한 번만 나가야 하는 알림</b>의 중복 발송을 막는다.
 *
 * <h2>왜 필요한가</h2>
 *
 * 박람회 취소 안내는 재발송되면 안 된다. 그런데 이벤트는 다시 발행될 수 있고, 관리자가 버튼을 두 번 누를 수도
 * 있다. 그때 수백 명에게 같은 문자가 두 번 간다.
 *
 * <h2>왜 DB 제약으로 못 막나</h2>
 *
 * 발권에서는 "이 주문이 이미 발권됐나" 를 UNIQUE 로 얹을 여지라도 있었다. 여기는 다르다 —
 * <b>취소 안내는 수신자마다 1행</b>이라 {@code (template, type, expoId)} 가 N행 있는 것이 정상이다.
 * 그 조합에 UNIQUE 를 걸면 두 번째 수신자부터 저장이 실패한다.
 *
 * <p>그래서 <b>잠금이 유일한 수단</b>이다. 박람회 행을 잠가 "검사 → 저장" 사이를 직렬화한다.
 *
 * <h2>순서가 전부다</h2>
 *
 * <pre>
 * ① 박람회 행 잠금      ← 여기서 두 번째 요청이 기다린다
 * ② 이미 보냈나 검사
 * ③ (호출부) 알림 저장
 * </pre>
 *
 * ①을 빼면 두 요청이 나란히 ②에서 "아직 안 보냄" 을 보고 양쪽 다 보낸다. 발권·체크인에서 같은 구조를
 * 이미 두 번 겪었다.
 */
@Slf4j
@Component
public class OneShotNotificationGuard {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientMapper recipientMapper;

    public OneShotNotificationGuard(
            NotificationRepository notificationRepository,
            NotificationRecipientMapper recipientMapper) {
        this.notificationRepository = notificationRepository;
        this.recipientMapper = recipientMapper;
    }

    /**
     * 발송 권한을 잡는다.
     *
     * <p><b>호출자의 트랜잭션 안에서 불러야 한다.</b> 잠금은 트랜잭션이 끝나면 풀리므로, 별도 트랜잭션에서
     * 잠그면 돌아오는 순간 이미 풀려 있어 아무 소용이 없다.
     *
     * @param expoId 잠글 박람회
     * @param templateCode 이미 보냈는지 판단할 템플릿
     * @param referenceType 참조 타입. 박람회 취소는 {@code EXPO}
     * @return 보내도 되면 {@code true}. 이미 보냈으면 {@code false}
     */
    public boolean claim(Long expoId, String templateCode, String referenceType) {
        // ① 먼저 잠근다. 두 번째 요청은 여기서 기다렸다가 ②에서 걸린다.
        recipientMapper.lockExpo(expoId);

        // ② 잠금을 얻은 뒤에 읽어야 앞선 요청의 결과가 보인다 (READ COMMITTED 전제).
        boolean alreadySent =
                notificationRepository.existsByTemplateCodeAndReferenceTypeAndReferenceId(
                        templateCode, referenceType, expoId);

        if (alreadySent) {
            log.info("알림 발송 생략 (이미 보냄) template={} expoId={}", templateCode, expoId);
            return false;
        }
        return true;
    }
}
