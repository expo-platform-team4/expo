package com.expo.participation.entity;

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

/**
 * 참여 기업의 신청서와 선택 부스.
 *
 * <p>기업은 공고 1건에서 부스 1개만 고른다. 관리자 승인·반려 모델은 사용하지 않으며, 결제 성공 즉시 신청이 완료된다.
 */
@Getter
@Entity
@Table(name = "participation_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipationApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recruitment_notice_id", nullable = false)
    private Long recruitmentNoticeId;

    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    @Column(name = "company_name_snapshot", nullable = false, length = 150)
    private String companyNameSnapshot;

    @Column(name = "participation_purpose", columnDefinition = "TEXT")
    private String participationPurpose;

    @Column(name = "exhibit_description", columnDefinition = "TEXT")
    private String exhibitDescription;

    @Column(name = "selected_booth_product_id")
    private Long selectedBoothProductId;

    @Column(name = "booth_order_id", unique = true)
    private Long boothOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ParticipationApplicationStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "admin_checked_at")
    private LocalDateTime adminCheckedAt;

    @Column(name = "admin_checked_by")
    private Long adminCheckedBy;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;
}
