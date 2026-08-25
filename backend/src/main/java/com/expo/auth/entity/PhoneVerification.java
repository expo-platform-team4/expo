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

/**
 * PHONE_VERIFICATIONS — 휴대폰 본인인증 요청·결과.
 *
 * <p>{@code verification_token_hash} 는 <b>인증번호의 해시만</b> 담는다. 인증 성공 시 발급하는
 * 회원가입용 토큰은 {@code signup_token_hash} 로 따로 둔다 — 한 컬럼이 시점에 따라 다른 값을
 * 담으면 {@link PhoneVerificationStatus#USED} 도입 후 "지금 이 값이 무엇인지"를 상태로 매번
 * 따져야 한다 (V202608250916).
 */
@Entity
@Table(name = "phone_verifications")
public class PhoneVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "verification_token_hash", nullable = false, length = 255)
    private String verificationTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PhoneVerificationStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** 인증 성공 시 발급하는 회원가입용 1회성 토큰의 해시. 원문은 응답으로만 나간다. */
    @Column(name = "signup_token_hash", length = 255)
    private String signupTokenHash;

    /**
     * 가입토큰 자체의 만료시각.
     *
     * <p>{@link #expiresAt} 는 "인증번호" 의 만료라 인증 성공 이후에는 검사되지 않는다. 이 값이
     * 없으면 가입토큰이 무기한 유효해진다.
     */
    @Column(name = "signup_token_expires_at")
    private Instant signupTokenExpiresAt;

    /** 낙관적 락. 같은 건에 confirm 이 동시에 들어와 상태 전이가 중복 적용되는 것을 막는다. */
    @Version
    @Column(nullable = false)
    private Long version;

    protected PhoneVerification() {}

    public static PhoneVerification createRequested(
            String phoneNumber,
            String verificationTokenHash,
            Instant requestedAt,
            Instant expiresAt) {
        PhoneVerification verification = new PhoneVerification();
        verification.phoneNumber = phoneNumber;
        verification.verificationTokenHash = verificationTokenHash;
        verification.status = PhoneVerificationStatus.REQUESTED;
        verification.requestedAt = requestedAt;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public void markExpired() {
        this.status = PhoneVerificationStatus.EXPIRED;
    }

    public void markFailed() {
        this.status = PhoneVerificationStatus.FAILED;
    }

    public void markVerified(
            String signupTokenHash, Instant verifiedAt, Instant signupTokenExpiresAt) {
        this.status = PhoneVerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
        this.signupTokenHash = signupTokenHash;
        this.signupTokenExpiresAt = signupTokenExpiresAt;
    }

    /** 회원가입이 이 인증을 소비했다. 같은 토큰으로 다시 가입할 수 없다. */
    public void markUsed(Long userId) {
        this.status = PhoneVerificationStatus.USED;
        this.userId = userId;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    /** 발급된 적이 없으면({@code null}) 만료로 본다 — 이 기능 이전에 만들어진 행이 그렇다. */
    public boolean isSignupTokenExpired(Instant now) {
        return signupTokenExpiresAt == null || signupTokenExpiresAt.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getVerificationTokenHash() {
        return verificationTokenHash;
    }

    public PhoneVerificationStatus getStatus() {
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

    public String getSignupTokenHash() {
        return signupTokenHash;
    }

    public Instant getSignupTokenExpiresAt() {
        return signupTokenExpiresAt;
    }
}
