package com.expo.recruitment.entity;

import com.expo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 주최 클라이언트의 모집공고 생성 요청과 장소 중복 운영 결정. */
@Getter
@Entity
@Table(name = "recruitment_notice_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentNoticeRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "application_start_at", nullable = false)
    private LocalDateTime applicationStartAt;

    @Column(name = "application_end_at", nullable = false)
    private LocalDateTime applicationEndAt;

    @Column(name = "event_start_at", nullable = false)
    private LocalDateTime eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private LocalDateTime eventEndAt;

    @Column(name = "virtual_venue_id", nullable = false)
    private Long virtualVenueId;

    @Column(name = "venue_hall_id")
    private Long venueHallId;

    @Column(name = "venue_zone_id")
    private Long venueZoneId;

    @Column(name = "target_company_count")
    private Integer targetCompanyCount;

    /** 희망 부스 구성 스냅샷(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_booth_config", columnDefinition = "jsonb")
    private String requestedBoothConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecruitmentNoticeRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_conflict_status", nullable = false, length = 30)
    private VenueConflictStatus venueConflictStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_decision", nullable = false, length = 20)
    private VenueDecision venueDecision;

    @Column(name = "conflict_group_key", length = 100)
    private String conflictGroupKey;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "decided_by_admin_id")
    private Long decidedByAdminId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    /** 모집공고 생성 요청 작성. 상태는 DRAFT, 장소 충돌은 CLEAR, 장소 결정은 PENDING 으로 고정한다. */
    public static RecruitmentNoticeRequest create(
            Long hostClientId,
            String title,
            String description,
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            LocalDateTime eventStartAt,
            LocalDateTime eventEndAt,
            Long virtualVenueId) {
        RecruitmentNoticeRequest request = new RecruitmentNoticeRequest();
        request.hostClientId = hostClientId;
        request.title = title;
        request.description = description;
        request.applicationStartAt = applicationStartAt;
        request.applicationEndAt = applicationEndAt;
        request.eventStartAt = eventStartAt;
        request.eventEndAt = eventEndAt;
        request.virtualVenueId = virtualVenueId;
        request.status = RecruitmentNoticeRequestStatus.DRAFT;
        request.venueConflictStatus = VenueConflictStatus.CLEAR;
        request.venueDecision = VenueDecision.PENDING;
        return request;
    }

    /** 희망 장소 세부 정보와 부스 구성 지정. */
    public RecruitmentNoticeRequest withVenueDetails(
            Long venueHallId,
            Long venueZoneId,
            Integer targetCompanyCount,
            String requestedBoothConfig) {
        this.venueHallId = venueHallId;
        this.venueZoneId = venueZoneId;
        this.targetCompanyCount = targetCompanyCount;
        this.requestedBoothConfig = requestedBoothConfig;
        return this;
    }

    /** 장소 충돌 판정. ALLOWED 면 승인, CANCELED 면 반려로 처리한다. */
    public void decideVenue(VenueDecision decision, Long decidedByAdminId, String decisionReason) {
        this.venueDecision = decision;
        this.decidedByAdminId = decidedByAdminId;
        this.decisionReason = decisionReason;
        this.decidedAt = LocalDateTime.now();
        this.venueConflictStatus = VenueConflictStatus.RESOLVED;
        this.status =
                decision == VenueDecision.ALLOWED
                        ? RecruitmentNoticeRequestStatus.APPROVED
                        : RecruitmentNoticeRequestStatus.REJECTED;
    }
}
