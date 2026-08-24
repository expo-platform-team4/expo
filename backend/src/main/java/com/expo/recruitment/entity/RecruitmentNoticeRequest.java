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
import java.time.Instant;
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
    private Instant applicationStartAt;

    @Column(name = "application_end_at", nullable = false)
    private Instant applicationEndAt;

    @Column(name = "event_start_at", nullable = false)
    private Instant eventStartAt;

    @Column(name = "event_end_at", nullable = false)
    private Instant eventEndAt;

    @Column(name = "virtual_venue_id", nullable = false)
    private Long virtualVenueId;

    @Column(name = "venue_hall_id")
    private Long venueHallId;

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
    private Instant submittedAt;

    @Column(name = "decided_by_admin_id")
    private Long decidedByAdminId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    /** 모집공고 생성 요청 작성. 상태는 DRAFT, 장소 충돌은 CLEAR, 장소 결정은 PENDING 으로 고정한다. */
    public static RecruitmentNoticeRequest create(
            Long hostClientId,
            String title,
            String description,
            Instant applicationStartAt,
            Instant applicationEndAt,
            Instant eventStartAt,
            Instant eventEndAt,
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

    /** 희망 전시관(홀)과 부스 구성 지정. 그 전시관 안에서 고른 구역 목록은 별도 테이블에 저장한다. */
    public RecruitmentNoticeRequest withVenueDetails(
            Long venueHallId, Integer targetCompanyCount, String requestedBoothConfig) {
        this.venueHallId = venueHallId;
        this.targetCompanyCount = targetCompanyCount;
        this.requestedBoothConfig = requestedBoothConfig;
        return this;
    }

    /**
     * 요청 제출. 별도의 초안 수정 단계 없이 작성과 동시에 제출되므로 {@code create()} 직후 바로 호출된다.
     *
     * <p>이미 확정된 예약과 희망 기간이 겹치는지 사전 판정한 결과({@code venueConflictStatus})를 같이 반영한다 -
     * 겹침 여부를 전혀 계산 안 하고 항상 CLEAR 로 두면, 관리자가 장소 결정 화면에서 실제로는 겹치는 요청도 안 겹치는
     * 것처럼 보게 된다. 최종 방어는 여전히 실제 예약 확정 시점의 DB EXCLUDE 제약이 맡는다 - 이건 관리자 검토를 돕는
     * 사전 신호일 뿐이다.
     */
    public void submit(VenueConflictStatus venueConflictStatus) {
        this.venueConflictStatus = venueConflictStatus;
        this.status = RecruitmentNoticeRequestStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    /** 장소 충돌 판정. ALLOWED 면 승인, CANCELED 면 반려로 처리한다. */
    public void decideVenue(VenueDecision decision, Long decidedByAdminId, String decisionReason) {
        this.venueDecision = decision;
        this.decidedByAdminId = decidedByAdminId;
        this.decisionReason = decisionReason;
        this.decidedAt = Instant.now();
        this.venueConflictStatus = VenueConflictStatus.RESOLVED;
        this.status =
                decision == VenueDecision.ALLOWED
                        ? RecruitmentNoticeRequestStatus.APPROVED
                        : RecruitmentNoticeRequestStatus.REJECTED;
    }
}
