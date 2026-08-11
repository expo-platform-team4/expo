package com.expo.participation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 참여 신청 운영 확인과 보완 요청 이력. 승인·반려 이력이 아니다.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없는 append-only 로그라 {@code BaseTimeEntity} 를
 * 상속하지 않는다.
 */
@Getter
@Entity
@Table(name = "application_operation_histories")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationOperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ApplicationOperationActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "processed_by_admin_id", nullable = false)
    private Long processedByAdminId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 참여 신청 운영 확인·보완 요청 이력 기록. */
    public static ApplicationOperationHistory create(
            Long applicationId,
            ApplicationOperationActionType actionType,
            String message,
            Long processedByAdminId) {
        ApplicationOperationHistory history = new ApplicationOperationHistory();
        history.applicationId = applicationId;
        history.actionType = actionType;
        history.message = message;
        history.processedByAdminId = processedByAdminId;
        return history;
    }
}
