package com.expo.banner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** BANNER_REVIEW_HISTORIES 테이블 — 배너 신청 심사(제출/승인/반려/취소) 이력. */
@Entity
@Table(name = "banner_review_histories")
public class BannerReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 심사 대상 배너 신청 ID (banner_applications.id).
     *
     * <p>BannerApplication 엔티티가 아직 저장소에 없어 {@code @ManyToOne} 대신 ID만 보관한다.
     * BannerApplication 엔티티가 정의되면 이 필드를 참조로 교체한다.
     */
    @Column(name = "banner_application_id", nullable = false)
    private Long bannerApplicationId;

    /** 심사한 관리자 ID (users.id). 도메인 경계상 User 엔티티를 직접 참조하지 않고 ID만 보관한다. */
    @Column(name = "reviewer_admin_id", nullable = false)
    private Long reviewerAdminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BannerReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected BannerReviewHistory() {}

    private BannerReviewHistory(
            Long bannerApplicationId,
            Long reviewerAdminId,
            BannerReviewDecision decision,
            String reason,
            String fromStatus,
            String toStatus,
            Instant reviewedAt) {
        this.bannerApplicationId = bannerApplicationId;
        this.reviewerAdminId = reviewerAdminId;
        this.decision = decision;
        this.reason = reason;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reviewedAt = reviewedAt;
    }

    /** 심사 이력 한 건을 기록한다. reviewedAt 은 처리 시각(now)을 호출부에서 넘긴다. */
    public static BannerReviewHistory record(
            Long bannerApplicationId,
            Long reviewerAdminId,
            BannerReviewDecision decision,
            String reason,
            String fromStatus,
            String toStatus,
            Instant reviewedAt) {
        return new BannerReviewHistory(
                bannerApplicationId,
                reviewerAdminId,
                decision,
                reason,
                fromStatus,
                toStatus,
                reviewedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getBannerApplicationId() {
        return bannerApplicationId;
    }

    public Long getReviewerAdminId() {
        return reviewerAdminId;
    }

    public BannerReviewDecision getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
