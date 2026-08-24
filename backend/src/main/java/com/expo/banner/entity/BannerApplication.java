package com.expo.banner.entity;

import com.expo.banner.exception.BannerStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * 광고 배너 노출 신청 (V1: banner_applications)
 *
 * 클라이언트가 자신의 박람회(expo_id)에 대한 배너 노출을 신청한다.
 * B-API-019 는 별도의 임시저장 단계 없이 신청과 동시에 심사를 요청하므로,
 * 서비스 계층에서 {@link #builder()} 로 생성한 뒤 바로 {@link #submit()} 을 호출한다
 * (DRAFT 상태 자체는 스키마 호환을 위해 남아 있다).
 *
 * 승인되면 {@link Banner} 레코드가 별도로 생성되어 실제 노출을 담당한다
 * (B-API-022, 즉시/예약 자동 활성화).
 */
@Entity
@Table(name = "banner_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BannerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청 클라이언트 — FK(client_profiles.user_id) */
    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    /** 배너가 홍보할 대상 박람회 — FK(expos.id) */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** 배너 이미지 — FK(file_metadata.id) */
    @Column(name = "image_file_id", nullable = false)
    private Long imageFileId;

    @Column(name = "headline", length = 150)
    private String headline;

    /** 희망 노출 시작·종료 일시 (CHECK: end > start) */
    @Column(name = "requested_start_at", nullable = false)
    private OffsetDateTime requestedStartAt;

    @Column(name = "requested_end_at", nullable = false)
    private OffsetDateTime requestedEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 20, nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.DRAFT;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private BannerApplication(
            Long clientUserId,
            Long expoId,
            Long imageFileId,
            String headline,
            OffsetDateTime requestedStartAt,
            OffsetDateTime requestedEndAt) {
        this.clientUserId = clientUserId;
        this.expoId = expoId;
        this.imageFileId = imageFileId;
        this.headline = headline;
        this.requestedStartAt = requestedStartAt;
        this.requestedEndAt = requestedEndAt;
        this.reviewStatus = ReviewStatus.DRAFT;
        validate();
    }

    /* ==================== 비즈니스 로직 ==================== */

    /** 심사 요청 — DRAFT/REJECTED 에서만 가능 (banner_review_histories 의 SUBMIT 이력과 짝) */
    public void submit() {
        if (reviewStatus != ReviewStatus.DRAFT && reviewStatus != ReviewStatus.REJECTED) {
            throw new BannerStateException("심사 요청이 불가능한 상태입니다. (현재 상태: " + reviewStatus + ")");
        }
        this.reviewStatus = ReviewStatus.UNDER_REVIEW;
        this.submittedAt = OffsetDateTime.now();
        this.rejectionReason = null;
    }

    /** 승인 — 이후 서비스 계층에서 Banner 레코드 생성으로 이어진다 (B-API-022) */
    public void approve(Long adminId) {
        requireReviewable();
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = OffsetDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(Long adminId, String reason) {
        requireReviewable();
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = OffsetDateTime.now();
        this.rejectionReason = reason;
    }

    /** 신청 취소 (승인 전) */
    public void cancel() {
        if (reviewStatus == ReviewStatus.APPROVED) {
            throw new BannerStateException("승인된 배너 신청은 취소할 수 없습니다.");
        }
        this.reviewStatus = ReviewStatus.CANCELED;
    }

    public boolean isOwnedBy(Long clientId) {
        return this.clientUserId != null && this.clientUserId.equals(clientId);
    }

    /* ==================== 검증 ==================== */

    private void requireReviewable() {
        if (reviewStatus != ReviewStatus.UNDER_REVIEW) {
            throw new BannerStateException("심사 처리가 불가능한 상태입니다. (현재 상태: " + reviewStatus + ")");
        }
    }

    private void validate() {
        if (requestedStartAt != null
                && requestedEndAt != null
                && !requestedEndAt.isAfter(requestedStartAt)) {
            throw new BannerStateException("노출 종료일시는 시작일시 이후여야 합니다.");
        }
    }

    /** banner_applications.review_status */
    public enum ReviewStatus {
        DRAFT,
        UNDER_REVIEW,
        REJECTED,
        APPROVED,
        CANCELED
    }
}
