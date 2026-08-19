package com.expo.ticket.entity;

import com.expo.common.entity.BaseTimeEntity;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * `Persistable&lt;Long&gt;` 를 구현하는 이유 — 실제로 겪은 저장 안 되는 버그를 고친 결과다.
 *
 * <p>`@MapsId` 로 PK 를 부모({@link TicketOrder})에서 빌려 쓰는 엔티티라 `@GeneratedValue` 가
 * 없다. 이 상태로 {@code guestOrderInfoRepository.save(guestOrder)} 를 호출하면(수정 전
 * 코드) 반환된 엔티티의 {@code id} 는 채워져 있고 트랜잭션도 예외 없이 커밋되는데, 정작
 * `guest_order_infos` 행은 DB 에 전혀 만들어지지 않았다 — 응답은 메모리에 있는 값을 그대로
 * 돌려주니 성공한 것처럼 보이지만, 이후 비회원 주문 조회는 항상 "주문 정보가 일치하지
 * 않습니다" 로 실패했다. Spring Data 의 기본 {@code isNew()} 판정("`@Id` 필드가 null 이면
 * 새 엔티티")이 `@MapsId` 조합에서 믿을 수 없다는 뜻으로 보고, `Persistable` 을 구현해
 * {@code isNew} 플래그로 직접 판정하게 했다.
 *
 * <p>그것만으로는 부족했다 — {@code isNew()} 는 고쳐도(=persist() 경로를 타도) 커밋 시점의
 * auto-flush 가 이 엔티티의 INSERT 를 여전히 누락했다. {@link com.expo.ticket.service.TicketOrderService}
 * 에서 `save()` 대신 `saveAndFlush()` 로 그 자리에서 즉시 INSERT 하게 만든 뒤에야 실제로
 * 저장되는 것을 로그로 확인했다. auto-flush 가 정확히 왜 이 엔티티만 건너뛰었는지는 끝까지
 * 못 밝혔다 — `@MapsId` + `FetchType.LAZY` 조합의 Hibernate 7.4.1 특이 동작으로 의심된다.
 * 재현 스크립트: `docs/init_table_schema.md` 대신 이 클래스와 `TicketOrderService.guestCreateOrder`
 * 를 같이 읽을 것. 원인을 더 파고들 사람을 위해 남겨 둔다.
 */
@Entity
@Table(name = "guest_order_infos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestOrder extends BaseTimeEntity implements Persistable<Long> {

    @Id private Long ticketOrderId;

    @Transient private boolean isNew = true;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_order_id")
    private TicketOrder ticketOrder;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = true)
    private Integer age;

    @Column(name = "lookup_password_hash", nullable = false, length = 255)
    private String lookupPasswordHash;

    @Column(name = "failed_lookup_count", nullable = false)
    private int failedLookupCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder
    private GuestOrder(
            TicketOrder ticketOrder,
            String guestName,
            String phoneNumber,
            Integer age,
            String lookupPasswordHash) {
        this.ticketOrder = ticketOrder;
        this.guestName = guestName;
        this.phoneNumber = phoneNumber;
        this.age = age;
        this.lookupPasswordHash = lookupPasswordHash;
        this.failedLookupCount = 0;
    }

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    public void validateNotLocked() {
        if (lockedUntil != null && Instant.now().isBefore(lockedUntil)) {
            throw new BusinessException(ErrorCode.GUEST_ORDER_LOOKUP_LOCKED);
        }
    }

    public void recordFailedAttempt() {
        this.failedLookupCount++;
        if (this.failedLookupCount >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = Instant.now().plus(LOCK_DURATION);
        }
    }

    public void resetFailedAttempts() {
        this.failedLookupCount = 0;
        this.lockedUntil = null;
    }

    @Override
    public Long getId() {
        return ticketOrderId;
    }

    @PostPersist
    @PostLoad
    private void markNotNew() {
        this.isNew = false;
    }
}
