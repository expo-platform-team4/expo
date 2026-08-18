package com.expo.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * TICKET_ACCESS_TOKENS 테이블 — SMS 로 보내는 QR 확인 URL 의 단기 접근 토큰.
 *
 * <p>이 테이블은 {@code created_at} 만 있고 {@code updated_at} 이 없어 {@code BaseTimeEntity} 를 상속하지 않고
 * {@code createdAt} 만 직접 auditing 한다.
 *
 * <p>토큰 원문은 저장하지 않는다. {@code tokenHash} 로만 대조한다. 이 토큰 자체가 인증 수단이라 로그에도 남기지 않는다.
 */
@Getter
@Entity
@Table(name = "ticket_access_tokens")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** order 도메인 엔티티가 아직 없으므로 FK ID만 매핑한다. */
    @Column(name = "ticket_order_id", nullable = false)
    private Long ticketOrderId;

    /** 특정 티켓 QR 로 바로 가는 링크일 때만 채운다. 주문 전체 조회면 NULL 이다. */
    @Column(name = "issued_ticket_id")
    private Long issuedTicketId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketAccessTokenScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketAccessTokenStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    private int accessCount;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private TicketAccessToken(
            Long ticketOrderId,
            Long issuedTicketId,
            String tokenHash,
            TicketAccessTokenScope scope,
            Instant expiresAt) {
        this.ticketOrderId = ticketOrderId;
        this.issuedTicketId = issuedTicketId;
        this.tokenHash = tokenHash;
        this.scope = scope;
        this.expiresAt = expiresAt;
        this.status = TicketAccessTokenStatus.ACTIVE;
        this.accessCount = 0;
    }

    /**
     * 주문 전체를 보여주는 링크용 토큰을 발급한다. SMS 로 나가는 링크가 이것이다.
     *
     * @param tokenHash 원문이 아니라 해시. 원문은 URL 로만 나가고 저장하지 않는다
     * @param expiresAt 만료 시각. 입장 당일에 링크가 죽으면 안 되므로 박람회 종료 이후로 잡는다
     */
    public static TicketAccessToken forOrder(
            Long ticketOrderId, String tokenHash, Instant expiresAt) {
        return new TicketAccessToken(
                ticketOrderId, null, tokenHash, TicketAccessTokenScope.ORDER_VIEW, expiresAt);
    }

    /**
     * 이 링크를 끊는다. 알림을 재발송하면서 새 토큰을 발급할 때 이전 것을 여기로 넘긴다.
     *
     * <p>지우지 않고 상태만 바꾸는 이유는 <b>조회 화면이 셋을 구분해야 하기 때문</b>이다. 지워 버리면
     * "없는 링크"(404)가 되어, 만료(410)·폐기(403)를 나눠 둔 의미가 사라진다. 폐기된 링크를 연 사람에게는
     * 재발급을 안내하면 안 된다.
     *
     * <p>이미 폐기됐으면 아무것도 하지 않는다 — 재발송을 두 번 눌러도 {@code revokedAt} 이 흔들리지 않게 한다.
     */
    public void revoke(Instant revokedAt) {
        if (this.status == TicketAccessTokenStatus.REVOKED) {
            return;
        }
        this.status = TicketAccessTokenStatus.REVOKED;
        this.revokedAt = revokedAt;
    }
}
