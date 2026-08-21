package com.expo.payment.entity;

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

/** 토스페이먼츠 티켓 결제 요청과 승인 결과. */
@Getter
@Entity
@Table(name = "ticket_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketPayment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_order_id", nullable = false)
    private Long ticketOrderId;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "pg_order_id", nullable = false, unique = true, length = 100)
    private String pgOrderId;

    @Column(length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPaymentStatus status;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "ticket_subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal ticketSubtotalAmount;

    @Column(name = "booking_fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bookingFeeAmount;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "canceled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal canceledAmount = BigDecimal.ZERO;

    @Column(name = "last_failure_code", length = 100)
    private String lastFailureCode;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    /** 결제 요청을 READY 상태로 생성한다. */
    public static TicketPayment create(
            Long ticketOrderId,
            String pgOrderId,
            BigDecimal requestedAmount,
            BigDecimal ticketSubtotalAmount,
            BigDecimal bookingFeeAmount,
            String idempotencyKey) {
        TicketPayment payment = new TicketPayment();
        payment.ticketOrderId = ticketOrderId;
        payment.pgOrderId = pgOrderId;
        payment.requestedAmount = requestedAmount;
        payment.ticketSubtotalAmount = ticketSubtotalAmount;
        payment.bookingFeeAmount = bookingFeeAmount;
        payment.canceledAmount = BigDecimal.ZERO;
        payment.idempotencyKey = idempotencyKey;
        payment.status = TicketPaymentStatus.READY;
        return payment;
    }

    /** 토스 결제창으로 넘어가 승인 대기 중임을 표시한다. */
    public void markInProgress() {
        this.status = TicketPaymentStatus.IN_PROGRESS;
    }

    /** 토스 결제 승인 성공. */
    public void approve(String paymentKey, String method, BigDecimal approvedAmount) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.approvedAmount = approvedAmount;
        this.approvedAt = Instant.now();
        this.status = TicketPaymentStatus.DONE;
    }

    /** 토스 결제 승인 실패. */
    public void fail(String failureCode) {
        this.lastFailureCode = failureCode;
        this.status = TicketPaymentStatus.FAILED;
    }

    /** 승인 전 취소 또는 승인된 결제의 PG 취소. */
    public void cancel(BigDecimal canceledAmount) {
        this.canceledAmount = canceledAmount;
        this.status = TicketPaymentStatus.CANCELED;
    }
}
