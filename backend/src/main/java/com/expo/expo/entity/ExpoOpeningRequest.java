package com.expo.expo.entity;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 박람회 개최 신청 (V1: expo_opening_requests)
 *
 * <p>임시저장(희-EXPO-01)·심사요청(희-EXPO-02)·개최신청(희-EXPO-17) 흐름을 담당. 승인되면 별도의 expos 레코드가 생성되어
 * 공개·판매의 기준이 된다.
 *
 * <p>※ V1 스키마는 title/description/행사·판매일시가 전부 NOT NULL 이므로 임시저장(DRAFT) 단계에서도 이 값들은 입력되어야 한다.
 */
@Getter
@Entity
@Table(name = "expo_opening_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpoOpeningRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주최 클라이언트 — FK(client_profiles.user_id) */
    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    /** 모집공고 경로로 개최하는 경우의 모집 결과 — FK(recruitment_results.id) */
    @Column(name = "recruitment_result_id")
    private Long recruitmentResultId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    /** 행사 시작·종료 일시 (CHECK: end > start) */
    @Column(name = "event_start_at", nullable = false)
    private Instant eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private Instant eventEndAt;

    /** 판매 시작·종료 일시 (희-EXPO-10 의 기준, CHECK: end > start) */
    @Column(name = "sales_start_at", nullable = false)
    private Instant salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private Instant salesEndAt;

    /** 희망 장소 (venue → hall → zone 계층, zone 은 hall 필수 — DB CHECK) */
    @Column(name = "desired_venue_id")
    private Long desiredVenueId;

    @Column(name = "desired_venue_hall_id")
    private Long desiredVenueHallId;

    @Column(name = "desired_venue_zone_id")
    private Long desiredVenueZoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ExpoOpeningRequestStatus status = ExpoOpeningRequestStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 개최 신청 생성 (임시저장 또는 즉시 심사요청).
     *
     * @param submitNow true 면 SUBMITTED(심사요청)로, false 면 DRAFT(임시저장)로 생성
     */
    public static ExpoOpeningRequest create(
            Long hostClientId,
            String title,
            String description,
            ExpoPeriod eventPeriod,
            ExpoPeriod salesPeriod,
            DesiredVenue desiredVenue,
            boolean submitNow,
            Instant now) {
        ExpoOpeningRequest request = new ExpoOpeningRequest();
        request.hostClientId = hostClientId;
        request.title = title;
        request.description = description;
        request.applyPeriods(eventPeriod, salesPeriod);
        request.applyVenue(desiredVenue);
        request.status =
                submitNow ? ExpoOpeningRequestStatus.SUBMITTED : ExpoOpeningRequestStatus.DRAFT;
        request.submittedAt = submitNow ? now : null;
        request.validate();
        return request;
    }

    /* ==================== 비즈니스 로직 ==================== */

    private void applyPeriods(ExpoPeriod eventPeriod, ExpoPeriod salesPeriod) {
        this.eventStartAt = eventPeriod.startAt();
        this.eventEndAt = eventPeriod.endAt();
        this.salesStartAt = salesPeriod.startAt();
        this.salesEndAt = salesPeriod.endAt();
    }

    private void applyVenue(DesiredVenue desiredVenue) {
        this.desiredVenueId = desiredVenue.venueId();
        this.desiredVenueHallId = desiredVenue.hallId();
        this.desiredVenueZoneId = desiredVenue.zoneId();
    }

    /**
     * 승인 전 클라이언트 직접 수정 (희-EXPO-05). 임시저장(DRAFT) 상태에서만 가능하며, 심사 요청 뒤에는 관리자가 보는 값이 바뀌면 안 된다.
     * APPROVED 이후에는 expo_change_requests 로만 수정 요청 가능 (희-EXPO-06).
     */
    public void updateContent(
            String title,
            String description,
            ExpoPeriod eventPeriod,
            ExpoPeriod salesPeriod,
            DesiredVenue desiredVenue) {
        if (status != ExpoOpeningRequestStatus.DRAFT) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_EDITABLE);
        }
        this.title = title;
        this.description = description;
        applyPeriods(eventPeriod, salesPeriod);
        applyVenue(desiredVenue);
        validate();
    }

    /** 임시저장 → 심사 요청. */
    public void submit(Instant now) {
        if (status != ExpoOpeningRequestStatus.DRAFT) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_SUBMITTABLE);
        }
        this.status = ExpoOpeningRequestStatus.SUBMITTED;
        this.submittedAt = now;
        this.rejectionReason = null;
    }

    /** 관리자 심사 시작. */
    public void startReview(Long adminId) {
        if (status != ExpoOpeningRequestStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_REVIEWABLE);
        }
        this.status = ExpoOpeningRequestStatus.UNDER_REVIEW;
        this.reviewedByAdminId = adminId;
    }

    /** 승인 — 이후 서비스 계층에서 expos 레코드 생성으로 이어진다 (희-EXPO-09). */
    public void approve(Long adminId, Instant now) {
        requireReviewable();
        this.status = ExpoOpeningRequestStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.rejectionReason = null;
    }

    /** 반려. */
    public void reject(Long adminId, String reason, Instant now) {
        requireReviewable();
        this.status = ExpoOpeningRequestStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.rejectionReason = reason;
    }

    /** 신청 취소 (승인 전). */
    public void cancel() {
        if (!isPendingReview()) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_ALREADY_REVIEWED);
        }
        this.status = ExpoOpeningRequestStatus.CANCELED;
    }

    /** 심사 대기 중인가 — 관리자 목록에 떠야 하는 상태. */
    public boolean isPendingReview() {
        return status == ExpoOpeningRequestStatus.DRAFT
                || status == ExpoOpeningRequestStatus.SUBMITTED
                || status == ExpoOpeningRequestStatus.UNDER_REVIEW;
    }

    public boolean isOwnedBy(Long clientId) {
        return this.hostClientId != null && this.hostClientId.equals(clientId);
    }

    /* ==================== 검증 ==================== */

    /** 심사할 수 있는 상태인지. 이미 승인·반려·철회된 건을 다시 심사하지 못하게 막는다. */
    private void requireReviewable() {
        if (status != ExpoOpeningRequestStatus.SUBMITTED
                && status != ExpoOpeningRequestStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_REVIEWABLE);
        }
    }

    /** DB CHECK(ck_expo_opening_requests_event/sales, zone→hall)와 동일 규칙을 앱에서 선검증. */
    private void validate() {
        if (eventStartAt != null && eventEndAt != null && !eventEndAt.isAfter(eventStartAt)) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_EDITABLE);
        }
        if (salesStartAt != null && salesEndAt != null && !salesEndAt.isAfter(salesStartAt)) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_EDITABLE);
        }
        if (desiredVenueZoneId != null && desiredVenueHallId == null) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_EDITABLE);
        }
        if (desiredVenueHallId != null && desiredVenueId == null) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_EDITABLE);
        }
    }
}
