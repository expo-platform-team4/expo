package com.expo.expo.entity;

import com.expo.expo.entity.ExpoEnums.OpeningRequestStatus;
import com.expo.expo.exception.ExpoStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 박람회 개최 신청 (V1: expo_opening_requests)
 *
 * 임시저장(희-EXPO-01)·심사요청(희-EXPO-02)·개최신청(희-EXPO-17) 흐름을 담당.
 * 승인되면 별도의 expos 레코드가 생성되어 공개·판매의 기준이 된다.
 *
 * ※ V1 스키마는 title/description/행사·판매일시가 전부 NOT NULL 이므로
 *   임시저장(DRAFT) 단계에서도 이 값들은 입력되어야 한다. (정의서 v2와 다른 점)
 */
@Entity
@Table(name = "expo_opening_requests")
@Getter
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
    private OffsetDateTime eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private OffsetDateTime eventEndAt;

    /** 판매 시작·종료 일시 (희-EXPO-10 의 기준, CHECK: end > start) */
    @Column(name = "sales_start_at", nullable = false)
    private OffsetDateTime salesStartAt;

    @Column(name = "sales_end_at", nullable = false)
    private OffsetDateTime salesEndAt;

    /** 희망 장소 (venue → hall → zone 계층, zone 은 hall 필수 — DB CHECK) */
    @Column(name = "desired_venue_id")
    private Long desiredVenueId;

    @Column(name = "desired_venue_hall_id")
    private Long desiredVenueHallId;

    @Column(name = "desired_venue_zone_id")
    private Long desiredVenueZoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private OpeningRequestStatus status = OpeningRequestStatus.DRAFT;

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
    private ExpoOpeningRequest(
            Long hostClientId,
            Long recruitmentResultId,
            String title,
            String description,
            OffsetDateTime eventStartAt,
            OffsetDateTime eventEndAt,
            OffsetDateTime salesStartAt,
            OffsetDateTime salesEndAt,
            Long desiredVenueId,
            Long desiredVenueHallId,
            Long desiredVenueZoneId) {
        this.hostClientId = hostClientId;
        this.recruitmentResultId = recruitmentResultId;
        this.title = title;
        this.description = description;
        this.eventStartAt = eventStartAt;
        this.eventEndAt = eventEndAt;
        this.salesStartAt = salesStartAt;
        this.salesEndAt = salesEndAt;
        this.desiredVenueId = desiredVenueId;
        this.desiredVenueHallId = desiredVenueHallId;
        this.desiredVenueZoneId = desiredVenueZoneId;
        this.status = OpeningRequestStatus.DRAFT;
        validate();
    }

    /* ==================== 비즈니스 로직 ==================== */

    /**
     * 승인 전 클라이언트 직접 수정 (희-EXPO-05).
     * APPROVED 이후에는 차단 — 승인된 건은 expo_change_requests 로만 수정 요청 가능 (희-EXPO-06).
     */
    public void updateByClient(
            String title,
            String description,
            OffsetDateTime eventStartAt,
            OffsetDateTime eventEndAt,
            OffsetDateTime salesStartAt,
            OffsetDateTime salesEndAt,
            Long desiredVenueId,
            Long desiredVenueHallId,
            Long desiredVenueZoneId) {
        if (!status.isEditableByClient()) {
            throw new ExpoStateException(
                    "승인된 신청은 직접 수정할 수 없습니다. 수정 요청(change request)을 이용해 주세요. (현재 상태: "
                            + status
                            + ")");
        }
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (eventStartAt != null) {
            this.eventStartAt = eventStartAt;
        }
        if (eventEndAt != null) {
            this.eventEndAt = eventEndAt;
        }
        if (salesStartAt != null) {
            this.salesStartAt = salesStartAt;
        }
        if (salesEndAt != null) {
            this.salesEndAt = salesEndAt;
        }
        if (desiredVenueId != null) {
            this.desiredVenueId = desiredVenueId;
        }
        if (desiredVenueHallId != null) {
            this.desiredVenueHallId = desiredVenueHallId;
        }
        if (desiredVenueZoneId != null) {
            this.desiredVenueZoneId = desiredVenueZoneId;
        }
        validate();
    }

    /** 심사 요청 (희-EXPO-02) — DRAFT/REJECTED 에서만 가능 */
    public void submit() {
        if (!status.isSubmittable()) {
            throw new ExpoStateException("심사 요청이 불가능한 상태입니다. (현재 상태: " + status + ")");
        }
        this.status = OpeningRequestStatus.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
        this.rejectionReason = null;
    }

    /** 관리자 심사 시작 */
    public void startReview(Long adminId) {
        if (status != OpeningRequestStatus.SUBMITTED) {
            throw new ExpoStateException("심사요청 상태가 아닙니다. (현재 상태: " + status + ")");
        }
        this.status = OpeningRequestStatus.UNDER_REVIEW;
        this.reviewedByAdminId = adminId;
    }

    /** 승인 — 이후 서비스 계층에서 expos 레코드 생성으로 이어진다 (희-EXPO-09) */
    public void approve(Long adminId) {
        requireReviewable();
        this.status = OpeningRequestStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = OffsetDateTime.now();
    }

    /** 반려 */
    public void reject(Long adminId, String reason) {
        requireReviewable();
        this.status = OpeningRequestStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = OffsetDateTime.now();
        this.rejectionReason = reason;
    }

    /** 신청 취소 (승인 전) */
    public void cancel() {
        if (status == OpeningRequestStatus.APPROVED) {
            throw new ExpoStateException("승인된 신청은 취소할 수 없습니다. 박람회 취소 요청을 이용해 주세요.");
        }
        this.status = OpeningRequestStatus.CANCELED;
    }

    public boolean isOwnedBy(Long clientId) {
        return this.hostClientId != null && this.hostClientId.equals(clientId);
    }

    /* ==================== 검증 ==================== */

    private void requireReviewable() {
        if (status != OpeningRequestStatus.SUBMITTED
                && status != OpeningRequestStatus.UNDER_REVIEW) {
            throw new ExpoStateException("심사 처리가 불가능한 상태입니다. (현재 상태: " + status + ")");
        }
    }

    /** DB CHECK(ck_expo_opening_requests_event/sales, zone→hall)와 동일 규칙을 앱에서 선검증 */
    private void validate() {
        if (eventStartAt != null && eventEndAt != null && !eventEndAt.isAfter(eventStartAt)) {
            throw new ExpoStateException("행사 종료일시는 시작일시 이후여야 합니다.");
        }
        if (salesStartAt != null && salesEndAt != null && !salesEndAt.isAfter(salesStartAt)) {
            throw new ExpoStateException("판매 종료일시는 시작일시 이후여야 합니다.");
        }
        if (desiredVenueZoneId != null && desiredVenueHallId == null) {
            throw new ExpoStateException("구역을 지정하려면 홀을 먼저 지정해야 합니다.");
        }
        if (desiredVenueHallId != null && desiredVenueId == null) {
            throw new ExpoStateException("홀을 지정하려면 장소를 먼저 지정해야 합니다.");
        }
    }
}
