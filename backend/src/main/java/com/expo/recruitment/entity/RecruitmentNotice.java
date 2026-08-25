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

    /** 근거 요청의 {@code expoId} 를 그대로 이어받는다(host_client_id 와 같은 비정규화 패턴). */
    @Column(name = "expo_id")
    private Long expoId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    /** 제출 자료 요구사항. 자유 텍스트다(JSON이 아니다) - 신청 기업에게 그대로 노출된다. */
    @Column(name = "submission_requirements", columnDefinition = "TEXT")
    private String submissionRequirements;

    @Column(name = "application_start_at", nullable = false)
    private Instant applicationStartAt;

    @Column(name = "application_end_at", nullable = false)
    private Instant applicationEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitmentNoticeStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_by_admin_id", nullable = false)
    private Long createdByAdminId;

    /** 기업 모집 공고 초안 생성. 상태는 DRAFT 로 고정한다. */
    public static RecruitmentNotice create(
            Long requestId,
            Long hostClientId,
            String title,
            String content,
            Instant applicationStartAt,
            Instant applicationEndAt,
            Long createdByAdminId) {
        RecruitmentNotice notice = new RecruitmentNotice();
        notice.requestId = requestId;
        notice.hostClientId = hostClientId;
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

    /** 근거 요청의 expoId 를 이어받는다. */
    public RecruitmentNotice withExpoId(Long expoId) {
        this.expoId = expoId;
        return this;
    }

    /** 공고 내용·조건 수정. */
    public void update(
            String title,
            String content,
            String eligibility,
            String submissionRequirements,
            Instant applicationStartAt,
            Instant applicationEndAt) {
        this.title = title;
        this.content = content;
        this.eligibility = eligibility;
        this.submissionRequirements = submissionRequirements;
        this.applicationStartAt = applicationStartAt;
        this.applicationEndAt = applicationEndAt;
    }

    /**
     * 공고 게시. 신청 시작일이 이미 지났으면 바로 OPEN, 아직 안 됐으면 SCHEDULED(게시는 됐지만 신청은 아직 안
     * 열림)로 전환한다 - 시작일을 미래로 정해도 게시 버튼을 누르는 즉시 신청이 열려버리는 걸 막는다.
     */
    public void publish() {
        this.status =
                Instant.now().isBefore(applicationStartAt)
                        ? RecruitmentNoticeStatus.SCHEDULED
                        : RecruitmentNoticeStatus.OPEN;
        this.publishedAt = Instant.now();
    }

    /** 예정된 신청 시작일이 도래해 시스템이 SCHEDULED 를 OPEN 으로 전환. */
    public void activate() {
        this.status = RecruitmentNoticeStatus.OPEN;
    }

    /**
     * 기업 모집 조기 마감. 신규 신청은 이미 막힌다({@code ParticipationApplicationService.create()} 가
     * OPEN 상태만 허용). 신규 결제(부스 주문 생성)는 {@code BoothOrderService.create()} 가 여기서 바뀐 상태를
     * 다시 조회해서 막는다 - 마감 전에 이미 만들어둔 초안 신청서로 결제를 이어가는 걸 막으려면 상태만 바꾸는 것으로는
     * 부족하다.
     */
    public void close() {
        this.status = RecruitmentNoticeStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    /** 신청 종료일이 지나 시스템이 자동으로 마감 처리. 관리자 직권 조기 마감({@link #close()})과 결과 상태는 같다. */
    public void expire() {
        this.status = RecruitmentNoticeStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    /** 관리자 직권 취소. 마감(정상 종료)과 달리 공고 자체를 무효화한다. */
    public void cancel() {
        this.status = RecruitmentNoticeStatus.CANCELED;
    }
}
