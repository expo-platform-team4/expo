package com.expo.booth.entity;

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
 * 부스 배정·콘텐츠 운영상 변경 이력. 신청 승인·반려 이력이 아니다.
 *
 * <p>이 테이블은 {@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 로그라 {@code BaseTimeEntity}
 * 를 상속하지 않고 {@code createdAt} 만 직접 auditing 한다.
 */
@Getter
@Entity
@Table(name = "booth_management_histories")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothManagementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booth_allocation_id", nullable = false)
    private Long boothAllocationId;

    @Column(name = "booth_content_id")
    private Long boothContentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private BoothManagementActionType actionType;

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

    /** 부스 배정·콘텐츠 운영 변경 이력 기록. */
    public static BoothManagementHistory create(
            Long boothAllocationId,
            Long boothContentId,
            BoothManagementActionType actionType,
            String reason,
            Long processedByAdminId) {
        BoothManagementHistory history = new BoothManagementHistory();
        history.boothAllocationId = boothAllocationId;
        history.boothContentId = boothContentId;
        history.actionType = actionType;
        history.reason = reason;
        history.processedByAdminId = processedByAdminId;
        return history;
    }
}
