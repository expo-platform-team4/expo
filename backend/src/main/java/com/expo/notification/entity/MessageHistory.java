package com.expo.notification.entity;

import com.expo.notification.dto.MessageSendResult;
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
 * MESSAGE_HISTORIES 테이블 — 발송 시도별 외부 시스템 응답. 한 알림에 여러 건이 쌓인다.
 *
 * <p>이 테이블은 {@code created_at}/{@code updated_at} 이 없고 {@code requested_at} 만 갖는 append-only 이벤트
 * 로그라 {@code BaseTimeEntity} 를 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "message_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    /** 이 시도가 실제로 사용한 채널. 알림 단위 채널과 다를 수 있다 — {@link MessageChannel} 참고. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageChannel channel;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    /** 대행사에 보낸 요청(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    /** 대행사 응답(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    /** 알림별 시도 번호. {@code (notification_id, attempt_no)} 가 UNIQUE 다. */
    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private MessageHistory(
            Long notificationId,
            MessageChannel channel,
            int attemptNo,
            MessageSendResult sendResult,
            Instant requestedAt,
            Instant completedAt) {
        this.notificationId = notificationId;
        this.channel = channel;
        this.attemptNo = attemptNo;
        this.status = sendResult.success() ? MessageStatus.SENT : MessageStatus.FAILED;
        this.providerMessageId = sendResult.providerMessageId();
        this.requestPayload = sendResult.requestPayload();
        this.responsePayload = sendResult.responsePayload();
        this.errorCode = sendResult.errorCode();
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
    }

    /**
     * 끝난 발송 시도 하나를 기록한다.
     *
     * <p>요청과 응답이 모두 끝난 뒤에 부르므로 {@code requestedAt} 과 {@code completedAt} 을 함께 받는다. 발송 전에
     * {@code REQUESTED} 행을 미리 남겼다가 나중에 갱신하는 방식은 쓰지 않는다 — 행 하나에 두 번 쓰게 되고, 우리는 동기 발송이라 그럴 이유가 없다.
     *
     * @param attemptNo 이 알림의 몇 번째 시도인지. {@code (notification_id, attempt_no)} 가 UNIQUE 다
     * @param sendResult 대행사 응답. 성공·실패와 원문이 함께 들어 있다
     */
    public static MessageHistory record(
            Long notificationId,
            MessageChannel channel,
            int attemptNo,
            MessageSendResult sendResult,
            Instant requestedAt,
            Instant completedAt) {
        return new MessageHistory(
                notificationId, channel, attemptNo, sendResult, requestedAt, completedAt);
    }
}
