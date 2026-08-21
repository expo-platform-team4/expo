package com.expo.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** PASSWORD_RESET_TOKENS — 비밀번호 재설정 1회용 토큰 (V1 스키마). 원문 대신 해시만 저장한다. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PasswordResetStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "requested_ip", length = 45)
    private String requestedIp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetToken() {}

    public static PasswordResetToken issue(
            Long userId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            String requestedIp) {
        PasswordResetToken token = new PasswordResetToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.status = PasswordResetStatus.ISSUED;
        token.issuedAt = issuedAt;
        token.expiresAt = expiresAt;
        token.requestedIp = requestedIp;
        token.createdAt = issuedAt;
        return token;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void markExpired() {
        this.status = PasswordResetStatus.EXPIRED;
    }

    public void markRevoked() {
        this.status = PasswordResetStatus.REVOKED;
    }

    public void markUsed(Instant at) {
        this.status = PasswordResetStatus.USED;
        this.usedAt = at;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public PasswordResetStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
