package com.expo.checkin.repository;

import com.expo.checkin.entity.IssuedTicket;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** 발권 티켓 영속성 접근 인터페이스. */
public interface IssuedTicketRepository extends JpaRepository<IssuedTicket, Long> {

    /**
     * 스캔한 QR 로 티켓을 찾아 <b>행을 잠근다.</b>
     *
     * <p>잠그는 이유는 체크인이 "상태를 보고 → 바꾸는" check-then-act 이기 때문이다. 잠그지 않으면 같은 QR 을 동시에 두 번 찍었을 때 둘 다
     * {@code ISSUED} 를 보고 양쪽 다 입장 처리한다. 두 번째는 {@link
     * com.expo.checkin.entity.CheckInResult#ALREADY_USED} 로 막혀야 한다.
     *
     * <p>이 방어는 {@code READ COMMITTED} 를 전제한다. 잠금을 얻은 뒤 상태를 <b>다시 읽어야</b> 하는데, 격리 수준을 올리면 트랜잭션
     * 시작 시점의 스냅샷을 계속 보게 되어 방어가 무력해진다. 그래서 서비스에 격리 수준을 명시해 뒀다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IssuedTicket> findByQrTokenHash(String qrTokenHash);

    /** QR 이 안 찍힐 때 쓰는 수동 입력 경로. 잠그는 이유는 위와 같다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IssuedTicket> findByTicketCode(String ticketCode);
}
