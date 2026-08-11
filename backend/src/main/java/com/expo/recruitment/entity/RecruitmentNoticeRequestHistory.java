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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모집공고 생성 요청의 제출·검토·장소 허용/취소·공고 생성 연결 이력.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 로그라 {@code BaseTimeEntity} 를
 * 상속하지 않는다. {@code from_status}/{@code to_status} 는 {@code action_type} 에 따라 신청 상태값일 수도,
 * 장소 결정값일 수도 있어 단일 enum으로 강제하지 않고 문자열로 둔다.
 */
@Getter
@Entity
@Table(name = "recruitment_notice_request_histories")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentNoticeRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private RecruitmentNoticeRequestActionType actionType;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "processed_by_admin_id")
    private Long processedByAdminId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
