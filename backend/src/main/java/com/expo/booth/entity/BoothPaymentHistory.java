package com.expo.booth.entity;

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

/**
 * 부스 결제 요청·승인·실패·승인 전 취소 이력. 물리 삭제하지 않는다.
 *
 * <p>이 테이블은 {@code created_at}/{@code updated_at} 이 없고 {@code occurred_at} 하나만 갖는 append-only 이벤트
 * 로그라 {@code BaseTimeEntity} 를 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "booth_payment_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothPaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_payment_id", nullable = false)
    private Long boothPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private BoothPaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private BoothPaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private BoothPaymentStatus toStatus;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "pg_transaction_key", length = 200)
    private String pgTransactionKey;

    /** PG 응답 원문(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** 결제 이벤트 이력 기록. */
    public static BoothPaymentHistory record(
            Long boothPaymentId,
            BoothPaymentEventType eventType,
            BoothPaymentStatus fromStatus,
            BoothPaymentStatus toStatus,
            BigDecimal amount,
            String pgTransactionKey,
            String responsePayload) {
        BoothPaymentHistory history = new BoothPaymentHistory();
        history.boothPaymentId = boothPaymentId;
        history.eventType = eventType;
        history.fromStatus = fromStatus;
        history.toStatus = toStatus;
        history.amount = amount;
        history.pgTransactionKey = pgTransactionKey;
        history.responsePayload = responsePayload;
        history.occurredAt = LocalDateTime.now();
        return history;
    }
}
