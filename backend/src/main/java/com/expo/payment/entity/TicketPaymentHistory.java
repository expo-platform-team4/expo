package com.expo.payment.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 티켓 결제 요청·승인·실패 이력. */
@Getter
@Entity
@Table(name = "ticket_payment_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketPaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_payment_id", nullable = false)
    private Long ticketPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private TicketPaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TicketPaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private TicketPaymentStatus toStatus;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "pg_transaction_key", length = 200)
    private String pgTransactionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** 결제 이벤트 이력을 추가한다. */
    public static TicketPaymentHistory record(
            Long ticketPaymentId,
            TicketPaymentEventType eventType,
            TicketPaymentStatus fromStatus,
            TicketPaymentStatus toStatus,
            BigDecimal amount,
            String pgTransactionKey,
            String responsePayload) {
        TicketPaymentHistory history = new TicketPaymentHistory();
        history.ticketPaymentId = ticketPaymentId;
        history.eventType = eventType;
        history.fromStatus = fromStatus;
        history.toStatus = toStatus;
        history.amount = amount;
        history.pgTransactionKey = pgTransactionKey;
        history.responsePayload = responsePayload;
        history.occurredAt = Instant.now();
        return history;
    }
}
