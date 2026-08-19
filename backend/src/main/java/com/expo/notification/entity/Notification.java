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

    private Notification(
            Long recipientUserId,
            String recipientPhoneNumber,
            NotificationChannel channel,
            String templateCode,
            String referenceType,
            Long referenceId,
            String payload,
            NotificationStatus status) {
        this.recipientUserId = recipientUserId;
        this.recipientPhoneNumber = recipientPhoneNumber;
        this.channel = channel;
        this.templateCode = templateCode;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.payload = payload;
        this.status = status;
        this.retryCount = 0;
    }

    /**
     * 발송을 시도할 알림을 만든다. 상태는 {@link NotificationStatus#PENDING} 으로 시작한다.
     *
     * @param payload 템플릿 변수 JSON. <b>접근 토큰 원문을 넣지 않는다</b> — 저장하는 순간 해시만 두는 설계가 무너진다
     */
    public static Notification pending(
            Long recipientUserId,
            String recipientPhoneNumber,
            NotificationChannel channel,
            String templateCode,
            String referenceType,
            Long referenceId,
            String payload) {
        return new Notification(
                recipientUserId,
                recipientPhoneNumber,
                channel,
                templateCode,
                referenceType,
                referenceId,
                payload,
                NotificationStatus.PENDING);
    }

    /**
     * 보낼 수 없어 시도조차 하지 않은 알림을 만든다.
     *
     * <p>수신 번호가 없는 경우가 여기다. 실패({@code FAILED})와 구분하는 이유는, 재시도로 해결되는 문제가 아니기 때문이다.
     */
    public static Notification canceled(
            Long recipientUserId,
            NotificationChannel channel,
            String templateCode,
            String referenceType,
            Long referenceId,
            String payload,
            String reason) {
        Notification notification =
                new Notification(
                        recipientUserId,
                        null,
                        channel,
                        templateCode,
                        referenceType,
                        referenceId,
                        payload,
                        NotificationStatus.CANCELED);
        notification.lastError = reason;
        return notification;
    }

    /** 대행사가 접수했다. */
    public void markSent(Instant sentAt) {
        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt;
        this.lastError = null;
    }

    /**
     * 발송에 실패했다.
     *
     * <p>재시도 워커가 아직 없으므로 {@link NotificationStatus#RETRYING} 이 아니라 곧바로 {@code FAILED} 로 둔다.
     * 워커를 붙일 때 이 메서드가 바뀔 자리다.
     */
    public void markFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.lastError = errorMessage;
    }

    /**
     * 다시 보내려고 집어 들었다. {@code retry_count} 를 올린다.
     *
     * <p>상태는 여기서 바꾸지 않는다. 결과가 나온 뒤 {@link #markSent}/{@link #markFailed} 가 정한다 —
     * 미리 바꿔 두면 발송 도중에 죽었을 때 "보내는 중" 인지 "실패" 인지 알 수 없게 된다.
     *
     * <p>세는 것은 <b>재발송 횟수</b>이지 시도 횟수가 아니다. 최초 발송은 재발송이 아니므로 0 에서 시작하고,
     * {@code message_histories} 의 시도 번호와는 항상 1 만큼 차이가 난다.
     */
    public void markRetried() {
        this.retryCount++;
    }
}
