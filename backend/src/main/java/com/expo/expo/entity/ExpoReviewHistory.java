package com.expo.expo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** EXPO_REVIEW_HISTORIES 테이블 — 박람회 심사(제출/승인/반려) 이력. */
@Entity
@Table(name = "expo_review_histories")
public class ExpoReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 심사 대상 박람회 ID (expos.id / expo.expo_id).
     *
     * <p>원래는 Expo 엔티티를 {@code @ManyToOne} 으로 참조해야 하지만, 지금 dev 에는 V1
     * 스키마(expos)와 이번에 새로 머지된 마이그레이션(expo)이 컬럼 구조부터 다르게 공존하고 있어
     * 어느 쪽이 정본인지 아직 팀 확인 전이다. 정본이 정해지고 Expo 엔티티가 안정되면 이 필드를
     * {@code @ManyToOne Expo expo} 참조로 교체한다.
     */
    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** 심사한 관리자 ID (users.id). 도메인 경계상 User 엔티티를 직접 참조하지 않고 ID만 보관한다. */
    @Column(name = "reviewer_admin_id", nullable = false)
    private Long reviewerAdminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected ExpoReviewHistory() {}

    private ExpoReviewHistory(
            Long expoId,
            Long reviewerAdminId,
            ReviewDecision decision,
            String reason,
            String fromStatus,
            String toStatus,
            Instant reviewedAt) {
        this.expoId = expoId;
        this.reviewerAdminId = reviewerAdminId;
        this.decision = decision;
        this.reason = reason;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reviewedAt = reviewedAt;
    }

    /** 심사 이력 한 건을 기록한다. reviewedAt 은 처리 시각(now)을 호출부에서 넘긴다. */
    public static ExpoReviewHistory record(
            Long expoId,
            Long reviewerAdminId,
            ReviewDecision decision,
            String reason,
            String fromStatus,
            String toStatus,
            Instant reviewedAt) {
        return new ExpoReviewHistory(
                expoId, reviewerAdminId, decision, reason, fromStatus, toStatus, reviewedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getExpoId() {
        return expoId;
    }

    public Long getReviewerAdminId() {
        return reviewerAdminId;
    }

    public ReviewDecision getDecision() {
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
