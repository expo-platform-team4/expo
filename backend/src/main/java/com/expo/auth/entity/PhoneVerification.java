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

/** PHONE_VERIFICATIONS — 휴대폰 본인인증 요청·결과 (V1 스키마). */
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

    public void markVerified(String signupTokenHash, Instant verifiedAt) {
        this.status = PhoneVerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
        this.verificationTokenHash = signupTokenHash;
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
}
