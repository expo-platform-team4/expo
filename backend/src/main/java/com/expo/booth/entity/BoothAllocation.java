package com.expo.booth.entity;

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

/** 결제 성공 후 부스를 참여 기업에 확정 배정한 기록. */
@Getter
@Entity
@Table(name = "booth_allocations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothAllocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "booth_order_id", nullable = false, unique = true)
    private Long boothOrderId;

    @Column(name = "booth_product_id", nullable = false, unique = true)
    private Long boothProductId;

    @Column(name = "client_user_id", nullable = false)
    private Long clientUserId;

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoothAllocationStatus status;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /** 결제 승인 완료로 부스 확정 배정. */
    public static BoothAllocation create(
            Long applicationId, Long boothOrderId, Long boothProductId, Long clientUserId) {
        BoothAllocation allocation = new BoothAllocation();
        allocation.applicationId = applicationId;
        allocation.boothOrderId = boothOrderId;
        allocation.boothProductId = boothProductId;
        allocation.clientUserId = clientUserId;
        allocation.allocatedAt = LocalDateTime.now();
        allocation.status = BoothAllocationStatus.ASSIGNED;
        return allocation;
    }

    /** 관리자 배정 취소. 부스 이중 배정 등 운영상 정정이 필요할 때만 쓴다. */
    public void cancel(String reason) {
        this.status = BoothAllocationStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }
}
