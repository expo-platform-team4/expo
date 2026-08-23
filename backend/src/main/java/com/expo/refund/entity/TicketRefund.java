package com.expo.refund.entity;

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

/** 티켓 주문의 전체 환불 요청과 처리 결과. */
@Getter
@Entity
@Table(name = "ticket_refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketRefund extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_order_id", nullable = false, unique = true)
    private Long ticketOrderId;

    @Column(name = "ticket_payment_id", nullable = false)
    private Long ticketPaymentId;

    @Column(name = "refund_ticket_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundTicketAmount;

    @Column(name = "refund_booking_fee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundBookingFeeAmount;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketRefundStatus status;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "pg_refund_key", unique = true, length = 200)
    private String pgRefundKey;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_failure_code", length = 100)
    private String lastFailureCode;

    @Column(name = "processed_by_admin_id")
    private Long processedByAdminId;

    /** 주문 금액 스냅샷을 기준으로 전체 환불 요청을 생성한다. */
    public static TicketRefund create(
            Long ticketOrderId,
            Long ticketPaymentId,
            BigDecimal refundTicketAmount,
            BigDecimal refundBookingFeeAmount,
            String reason) {
        TicketRefund refund = new TicketRefund();
        refund.ticketOrderId = ticketOrderId;
        refund.ticketPaymentId = ticketPaymentId;
        refund.refundTicketAmount = refundTicketAmount;
        refund.refundBookingFeeAmount = refundBookingFeeAmount;
        refund.refundAmount = refundTicketAmount.add(refundBookingFeeAmount);
        refund.status = TicketRefundStatus.REQUESTED;
        refund.reason = reason;
        refund.requestedAt = Instant.now();
        return refund;
    }

    /** PG 취소 요청을 시작한다. */
    public void markProcessing() {
        this.status = TicketRefundStatus.PROCESSING;
    }

    /** PG 취소와 로컬 상태 반영이 완료됐다. */
    public void complete(String pgRefundKey) {
        this.pgRefundKey = pgRefundKey;
        this.completedAt = Instant.now();
        this.status = TicketRefundStatus.COMPLETED;
    }

    /** PG 취소 실패 결과를 기록한다. */
    public void fail(String failureCode) {
        this.lastFailureCode = failureCode;
        this.status = TicketRefundStatus.FAILED;
    }
}
