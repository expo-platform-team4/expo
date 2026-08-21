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

/** EMAIL_VERIFICATIONS — 회원가입 이메일 본인인증 요청·결과 (V202608210900 스키마). */
@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "verification_code_hash", nullable = false, length = 255)
    private String verificationCodeHash;

    @Column(name = "signup_token_hash", length = 255)
    private String signupTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailVerificationStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected EmailVerification() {}

    public static EmailVerification createRequested(
            String email, String verificationCodeHash, Instant requestedAt, Instant expiresAt) {
        EmailVerification verification = new EmailVerification();
        verification.email = email;
        verification.verificationCodeHash = verificationCodeHash;
        verification.status = EmailVerificationStatus.REQUESTED;
        verification.requestedAt = requestedAt;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public void markExpired() {
        this.status = EmailVerificationStatus.EXPIRED;
    }

    public void markFailed() {
        this.status = EmailVerificationStatus.FAILED;
    }

    public void markVerified(String signupTokenHash, Instant verifiedAt) {
        this.status = EmailVerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
        this.signupTokenHash = signupTokenHash;
    }

    /** 회원가입이 이 인증의 토큰을 소비했다. 같은 토큰을 다른 가입에 다시 쓸 수 없게 한다. */
    public void markUsed(Long userId) {
        this.status = EmailVerificationStatus.USED;
        this.userId = userId;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getVerificationCodeHash() {
        return verificationCodeHash;
    }

    public String getSignupTokenHash() {
        return signupTokenHash;
    }

    public EmailVerificationStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
