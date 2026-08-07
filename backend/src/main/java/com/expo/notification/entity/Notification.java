package com.expo.notification.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * NOTIFICATIONS 테이블 — 채널 독립적인 알림 작업 한 건과 그 발송 상태.
 *
 * <p>실제 발송 시도별 외부 응답은 {@link MessageHistory} 가 갖는다. 한 알림에 시도가 여러 건 달릴 수 있다.
 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 회원 수신자. 비회원 주문이면 NULL 이고 {@code recipientPhoneNumber} 만 채워진다. */
    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    /**
     * 수신 번호.
     *
     * <p>DB 에는 "둘 중 하나는 반드시 있어야 한다" 는 제약이 없다. 회원이라도 {@code users.phone_number} 가 NULL 일 수
     * 있으므로 서비스에서 막아야 한다.
     */
    @Column(name = "recipient_phone_number", length = 20)
    private String recipientPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    /** {@code ORDER}, {@code PAYMENT}, {@code TICKET} 등. 열거가 개방형이라 DB CHECK 도 enum 도 두지 않는다. */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    /** 템플릿 변수(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
