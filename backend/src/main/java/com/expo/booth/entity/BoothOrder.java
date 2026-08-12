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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여 신청에서 고른 단일 부스 상품의 주문.
 *
 * <p>부스가 1개만 선택되므로 주문 항목 테이블을 두지 않고 상품을 직접 연결한다. 결제(토스페이먼츠) 연동 전까지는 {@code
 * PENDING_PAYMENT} 상태의 주문 생성·취소만 다룬다.
 */
@Getter
@Entity
@Table(name = "booth_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 신청서 ID. DB 는 취소·만료된 주문을 제외한 활성 주문에만 유일성을 강제한다({@code
     * uq_booth_orders_active_application} 부분 UNIQUE 인덱스) — 취소 후 재주문을 허용하기 위해서다.
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    @Column(name = "booth_product_id", nullable = false)
    private Long boothProductId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoothOrderStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** 부스 상품 주문 생성. 단일 부스이므로 결제액은 항상 단가와 같다. */
    public static BoothOrder create(
            Long applicationId,
            Long clientUserId,
            Long boothProductId,
            String orderNumber,
            BigDecimal unitPrice,
            String idempotencyKey,
            Instant expiresAt) {
        BoothOrder order = new BoothOrder();
        order.applicationId = applicationId;
        order.clientUserId = clientUserId;
        order.boothProductId = boothProductId;
        order.orderNumber = orderNumber;
        order.unitPrice = unitPrice;
        order.totalAmount = unitPrice;
        order.idempotencyKey = idempotencyKey;
        order.expiresAt = expiresAt;
        order.status = BoothOrderStatus.PENDING_PAYMENT;
        return order;
    }

    /** 결제 전 주문 취소. */
    public void cancel() {
        this.status = BoothOrderStatus.CANCELED;
    }

    /** 결제 승인 완료. */
    public void markPaid() {
        this.status = BoothOrderStatus.PAYMENT_COMPLETED;
        this.paidAt = Instant.now();
    }
}
