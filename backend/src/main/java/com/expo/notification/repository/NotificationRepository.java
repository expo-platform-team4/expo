package com.expo.notification.repository;

import com.expo.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/** 알림 작업 영속성 접근 인터페이스. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 같은 대상에 같은 알림을 이미 만든 적이 있나.
     *
     * <p>박람회 취소처럼 <b>한 번만 나가야 하는</b> 알림에서 재발송을 막는 데 쓴다. 이벤트가 다시 발행되거나
     * 관리자가 두 번 눌러도 문자가 두 번 가면 안 된다.
     *
     * <p>이 조합에 <b>UNIQUE 제약을 걸 수는 없다.</b> 박람회 취소는 수신자마다 1행이라 같은
     * {@code (template, type, id)} 가 N행 존재하는 것이 정상이기 때문이다. 발권의 중복 방어와 다른 점이
     * 여기다 — 거기서는 DB 제약을 얹을 여지라도 있었다. 그래서 여기서는 <b>박람회 행을 잠가</b>
     * 검사와 저장 사이를 직렬화한다({@code NotificationRecipientMapper.lockExpo}).
     */
    boolean existsByTemplateCodeAndReferenceTypeAndReferenceId(
            String templateCode, String referenceType, Long referenceId);
}
