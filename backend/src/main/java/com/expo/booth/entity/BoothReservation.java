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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 진행 중 동일 부스 상품이 다른 기업에 팔리지 않도록 임시로 확보한 예약.
 *
 * <p>부스 상품 하나당 활성(ACTIVE) 예약은 1건만 허용되며, 그 제약은 DB 의 부분 UNIQUE 인덱스가 담당한다.
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
    private LocalDateTime reservedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoothReservationStatus status;

    @Column(name = "active_guard", nullable = false)
    private boolean activeGuard = true;
}
