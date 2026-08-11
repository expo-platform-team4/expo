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

/** 토스페이먼츠 부스 결제. 승인 완료 후 환불은 지원하지 않는다. CANCELED 는 승인 전 취소 흐름 전용이다. */
@Getter
@Entity
@Table(name = "booth_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothPayment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_order_id", nullable = false)
    private Long boothOrderId;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "pg_order_id", nullable = false, unique = true, length = 100)
    private String pgOrderId;

    @Column(length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoothPaymentStatus status;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "canceled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal canceledAmount = BigDecimal.ZERO;

    @Column(name = "last_failure_code", length = 100)
    private String lastFailureCode;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** 결제 요청 생성. READY 상태로 시작한다. */
    public static BoothPayment create(
            Long boothOrderId,
            String pgOrderId,
            BigDecimal requestedAmount,
            String idempotencyKey) {
        BoothPayment payment = new BoothPayment();
        payment.boothOrderId = boothOrderId;
        payment.pgOrderId = pgOrderId;
        payment.requestedAmount = requestedAmount;
        payment.canceledAmount = BigDecimal.ZERO;
        payment.idempotencyKey = idempotencyKey;
        payment.status = BoothPaymentStatus.READY;
        return payment;
    }

    /** 토스 결제창으로 넘어가 승인 대기 중임을 표시. */
    public void markInProgress() {
        this.status = BoothPaymentStatus.IN_PROGRESS;
    }

    /** 토스 결제 승인 성공. */
    public void approve(String paymentKey, String method, BigDecimal approvedAmount) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.approvedAmount = approvedAmount;
        this.approvedAt = LocalDateTime.now();
        this.status = BoothPaymentStatus.APPROVED;
    }

    /** 토스 결제 승인 실패. */
    public void fail(String failureCode) {
        this.lastFailureCode = failureCode;
        this.status = BoothPaymentStatus.FAILED;
    }

    /** 승인 전 취소. */
    public void cancel() {
        this.status = BoothPaymentStatus.CANCELED;
    }
}
