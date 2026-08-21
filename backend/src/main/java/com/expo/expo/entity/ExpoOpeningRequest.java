package com.expo.expo.entity;

import com.expo.common.entity.BaseTimeEntity;
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
import lombok.Getter;

/**
 * 주최 클라이언트의 박람회 개최 신청과 관리자 심사 결과.
 *
 * <p>테이블은 V1 부터 있었지만 Java 계층이 비어 있었다(이슈 #116). 스키마를 그대로 매핑한다 — 마이그레이션을 새로 만들지
 * 않았다.
 *
 * <p><b>희망 장소를 필수로 받는다.</b> 테이블상으로는 {@code desired_venue_id} 가 nullable 이지만, 승인 시 만들
 * {@code expos} 행의 {@code region_code} 가 NOT NULL 인데 신청서에는 지역 컬럼이 없다. 장소에서 지역을 파생시키는 것이
 * 유일하게 값을 조작하지 않는 방법이라 서비스에서 필수로 강제한다.
 */
@Getter
@Entity
@Table(name = "expo_opening_requests")
public class ExpoOpeningRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_start_at", nullable = false)
    private Instant eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private Instant eventEndAt;

    @Column(name = "sales_start_at", nullable = false)
    private Instant salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private Instant salesEndAt;

    @Column(name = "desired_venue_id")
    private Long desiredVenueId;

    @Column(name = "desired_venue_hall_id")
    private Long desiredVenueHallId;

    @Column(name = "desired_venue_zone_id")
    private Long desiredVenueZoneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpoOpeningRequestStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** 값 객체로 받아 필드에 옮긴다. 파라미터를 늘어놓지 않으려고 무인자 생성자 + 대입을 쓴다. */
    private ExpoOpeningRequest() {}

    /**
     * 신청서를 만든다.
     *
     * @param submitNow {@code true} 면 바로 심사 요청(SUBMITTED), {@code false} 면 임시저장(DRAFT). 디자인의
     *     "심사요청"·"임시저장" 두 버튼에 각각 대응한다.
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
        return request;
    }

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

    /** 임시저장 상태에서만 내용을 고칠 수 있다. 심사 요청 뒤에는 관리자가 보는 값이 바뀌면 안 된다. */
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
    }

    /** 임시저장 → 심사 요청. */
    public void submit(Instant now) {
        if (status != ExpoOpeningRequestStatus.DRAFT) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_SUBMITTABLE);
        }
        this.status = ExpoOpeningRequestStatus.SUBMITTED;
        this.submittedAt = now;
    }

    /** 주최사가 철회한다. 아직 심사 결과가 나오지 않은 건만 가능하다. */
    public void cancel() {
        if (!isPendingReview()) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_ALREADY_REVIEWED);
        }
        this.status = ExpoOpeningRequestStatus.CANCELED;
    }

    public void approve(Long adminId, Instant now) {
        requireReviewable();
        this.status = ExpoOpeningRequestStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.rejectionReason = null;
    }

    public void reject(Long adminId, String reason, Instant now) {
        requireReviewable();
        this.status = ExpoOpeningRequestStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.rejectionReason = reason;
    }

    /** 심사 대기 중인가 — 관리자 목록에 떠야 하는 상태. */
    public boolean isPendingReview() {
        return status == ExpoOpeningRequestStatus.DRAFT
                || status == ExpoOpeningRequestStatus.SUBMITTED
                || status == ExpoOpeningRequestStatus.UNDER_REVIEW;
    }

    public boolean isOwnedBy(Long clientUserId) {
        return hostClientId.equals(clientUserId);
    }

    /** 심사할 수 있는 상태인지. 이미 승인·반려·철회된 건을 다시 심사하지 못하게 막는다. */
    private void requireReviewable() {
        if (status != ExpoOpeningRequestStatus.SUBMITTED
                && status != ExpoOpeningRequestStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.EXPO_OPENING_REQUEST_NOT_REVIEWABLE);
        }
    }
}
