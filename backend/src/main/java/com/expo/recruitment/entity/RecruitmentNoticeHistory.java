package com.expo.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모집공고 작성·게시·수정·마감·취소 이력.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 로그라 {@code BaseTimeEntity} 를
 * 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "recruitment_notice_histories")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentNoticeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recruitment_notice_id", nullable = false)
    private Long recruitmentNoticeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private RecruitmentNoticeActionType actionType;

    /** 변경 전 값 스냅샷(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;

    /** 변경 후 값 스냅샷(JSONB). 전용 JSON 컨버터가 생기기 전까지는 원문 문자열로 다룬다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "processed_by_admin_id", nullable = false)
    private Long processedByAdminId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 모집공고 운영 변경 이력 기록. */
    public static RecruitmentNoticeHistory create(
            Long recruitmentNoticeId,
            RecruitmentNoticeActionType actionType,
            String beforeData,
            String afterData,
            String reason,
            Long processedByAdminId) {
        RecruitmentNoticeHistory history = new RecruitmentNoticeHistory();
        history.recruitmentNoticeId = recruitmentNoticeId;
        history.actionType = actionType;
        history.beforeData = beforeData;
        history.afterData = afterData;
        history.reason = reason;
        history.processedByAdminId = processedByAdminId;
        return history;
    }
}
