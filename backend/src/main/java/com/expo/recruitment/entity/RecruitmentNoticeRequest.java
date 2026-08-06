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
}
