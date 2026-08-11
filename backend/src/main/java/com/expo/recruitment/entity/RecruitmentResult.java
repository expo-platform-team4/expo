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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모집 마감 후 결제·배정 완료 기업을 집계한 결과 스냅샷.
 *
 * <p>{@code created_at} 만 있고 {@code updated_at} 이 없어 {@code BaseTimeEntity} 를 상속하지 않는다. 스냅샷이라
 * 생성 이후 값이 바뀌지 않는다는 의미이기도 하다.
 */
@Getter
@Entity
@Table(name = "recruitment_results")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recruitment_notice_id", nullable = false, unique = true)
    private Long recruitmentNoticeId;

    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    @Column(name = "confirmed_company_count", nullable = false)
    private Integer confirmedCompanyCount = 0;

    @Column(name = "confirmed_booth_count", nullable = false)
    private Integer confirmedBoothCount = 0;

    @Column(name = "total_booth_sales_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBoothSalesAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecruitmentResultStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "confirmed_by_host_at")
    private LocalDateTime confirmedByHostAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 모집 마감 후 결제·배정 완료 기업 집계로 결과 스냅샷 생성. */
    public static RecruitmentResult create(
            Long recruitmentNoticeId,
            Long hostClientId,
            Integer confirmedCompanyCount,
            Integer confirmedBoothCount,
            BigDecimal totalBoothSalesAmount) {
        RecruitmentResult result = new RecruitmentResult();
        result.recruitmentNoticeId = recruitmentNoticeId;
        result.hostClientId = hostClientId;
        result.confirmedCompanyCount = confirmedCompanyCount;
        result.confirmedBoothCount = confirmedBoothCount;
        result.totalBoothSalesAmount = totalBoothSalesAmount;
        result.status = RecruitmentResultStatus.GENERATED;
        result.generatedAt = LocalDateTime.now();
        return result;
    }

    /** 결과 항목 저장 후 집계값 반영. */
    public void applyAggregate(
            Integer confirmedCompanyCount,
            Integer confirmedBoothCount,
            BigDecimal totalBoothSalesAmount) {
        this.confirmedCompanyCount = confirmedCompanyCount;
        this.confirmedBoothCount = confirmedBoothCount;
        this.totalBoothSalesAmount = totalBoothSalesAmount;
    }

    /** 주최자에게 결과 전달. */
    public void deliver() {
        this.status = RecruitmentResultStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /** 주최자 결과 확인. */
    public void confirmByHost() {
        this.status = RecruitmentResultStatus.CONFIRMED;
        this.confirmedByHostAt = LocalDateTime.now();
    }

    /** 박람회 구성에 결과 반영 완료. */
    public void markUsedForExpo() {
        this.status = RecruitmentResultStatus.USED_FOR_EXPO;
    }

    /** 관리자 직권 취소. */
    public void cancel() {
        this.status = RecruitmentResultStatus.CANCELED;
    }
}
