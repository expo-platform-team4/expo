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
}
