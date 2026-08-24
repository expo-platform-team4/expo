package com.expo.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** EMAIL_VERIFICATIONS — 회원가입 이메일 본인인증 요청·결과 (V202608211800 스키마). */
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

    /** 가입토큰(signupTokenHash) 자체의 만료시각. {@link #markVerified} 시점에 채워진다. */
    @Column(name = "signup_token_expires_at")
    private Instant signupTokenExpiresAt;

    /** 낙관적 락. 같은 레코드에 대한 동시 확인(confirm)·소비(consume) 요청의 중복 처리를 막는다. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    public void markVerified(
            String signupTokenHash, Instant verifiedAt, Instant signupTokenExpiresAt) {
        this.status = EmailVerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
        this.signupTokenHash = signupTokenHash;
        this.signupTokenExpiresAt = signupTokenExpiresAt;
    }

    /** 회원가입이 이 인증의 토큰을 소비했다. 같은 토큰을 다른 가입에 다시 쓸 수 없게 한다. */
    public void markUsed(Long userId) {
        this.status = EmailVerificationStatus.USED;
        this.userId = userId;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    /** 가입토큰 자체가 만료됐는지. {@link #markVerified} 이전(토큰이 없는 상태)이면 항상 만료로 본다. */
    public boolean isSignupTokenExpired(Instant now) {
        return signupTokenExpiresAt == null || signupTokenExpiresAt.isBefore(now);
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
