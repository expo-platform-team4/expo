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

/** 허용된 장소 요청을 기준으로 관리자가 작성·게시하는 기업 모집공고. */
@Getter
@Entity
@Table(name = "recruitment_notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentNotice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;

    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    @Column(name = "venue_reservation_id", nullable = false, unique = true)
    private Long venueReservationId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    /** 제출 자료 스냅샷(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "submission_requirements", columnDefinition = "jsonb")
    private String submissionRequirements;

    @Column(name = "application_start_at", nullable = false)
    private LocalDateTime applicationStartAt;

    @Column(name = "application_end_at", nullable = false)
    private LocalDateTime applicationEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitmentNoticeStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_by_admin_id", nullable = false)
    private Long createdByAdminId;

    /** 기업 모집 공고 초안 생성. 상태는 DRAFT 로 고정한다. */
    public static RecruitmentNotice create(
            Long requestId,
            Long hostClientId,
            Long venueReservationId,
            String title,
            String content,
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt,
            Long createdByAdminId) {
        RecruitmentNotice notice = new RecruitmentNotice();
        notice.requestId = requestId;
        notice.hostClientId = hostClientId;
        notice.venueReservationId = venueReservationId;
        notice.title = title;
        notice.content = content;
        notice.applicationStartAt = applicationStartAt;
        notice.applicationEndAt = applicationEndAt;
        notice.createdByAdminId = createdByAdminId;
        notice.status = RecruitmentNoticeStatus.DRAFT;
        return notice;
    }

    /** 자격 요건과 제출 자료 요구사항 지정. */
    public RecruitmentNotice withDetails(String eligibility, String submissionRequirements) {
        this.eligibility = eligibility;
        this.submissionRequirements = submissionRequirements;
        return this;
    }

    /** 공고 내용·조건 수정. */
    public void update(
            String title,
            String content,
            String eligibility,
            String submissionRequirements,
            LocalDateTime applicationStartAt,
            LocalDateTime applicationEndAt) {
        this.title = title;
        this.content = content;
        this.eligibility = eligibility;
        this.submissionRequirements = submissionRequirements;
        this.applicationStartAt = applicationStartAt;
        this.applicationEndAt = applicationEndAt;
    }

    /** 공고 게시. */
    public void publish() {
        this.status = RecruitmentNoticeStatus.OPEN;
        this.publishedAt = LocalDateTime.now();
    }

    /** 기업 모집 조기 마감. 신규 결제를 차단한다. */
    public void close() {
        this.status = RecruitmentNoticeStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}
