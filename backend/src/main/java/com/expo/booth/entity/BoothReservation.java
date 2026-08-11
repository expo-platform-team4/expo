package com.expo.booth.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 진행 중 동일 부스가 다른 기업에 팔리지 않도록 거는 임시 확보.
 *
 * <p>부스 상품당 활성({@code ACTIVE}) 예약은 DB의 부분 UNIQUE 인덱스로 1건만 허용된다.
 */
@Getter
@Entity
@Table(name = "booth_reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothReservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_product_id", nullable = false)
    private Long boothProductId;

    @Column(name = "booth_order_id", nullable = false)
    private Long boothOrderId;

    @Column(name = "reserved_by_client_id", nullable = false)
    private Long reservedByClientId;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoothReservationStatus status;

    @Column(name = "active_guard", nullable = false)
    private boolean activeGuard = true;

    /** 주문 생성 시 부스 상품 임시 확보. */
    public static BoothReservation create(
            Long boothProductId,
            Long boothOrderId,
            Long reservedByClientId,
            LocalDateTime expiresAt) {
        BoothReservation reservation = new BoothReservation();
        reservation.boothProductId = boothProductId;
        reservation.boothOrderId = boothOrderId;
        reservation.reservedByClientId = reservedByClientId;
        reservation.reservedAt = LocalDateTime.now();
        reservation.expiresAt = expiresAt;
        reservation.status = BoothReservationStatus.ACTIVE;
        reservation.activeGuard = true;
        return reservation;
    }

    /** 주문 취소·만료로 확보 해제. */
    public void release() {
        this.status = BoothReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.activeGuard = false;
    }

    /** 결제 승인 완료로 확보를 확정 배정 전 단계로 전환. */
    public void confirm() {
        this.status = BoothReservationStatus.CONFIRMED;
        this.activeGuard = false;
    }
}
