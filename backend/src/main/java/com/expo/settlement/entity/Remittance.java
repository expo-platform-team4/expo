package com.expo.settlement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 송금 결과 (D-API-016).
 *
 * <h2>시스템이 송금하지 않는다</h2>
 *
 * 명세가 <b>"은행 자동 송금은 MVP 에 포함하지 않는다"</b> 고 못박았다. 담당자가 은행에서 계좌이체를
 * 하고, 그 결과를 여기에 적는다. 그래서 이 엔티티는 <b>사실의 기록</b>이지 작업 지시가 아니다.
 *
 * <h2>한 정산에 여러 건일 수 있다</h2>
 *
 * {@code settlement_id} 에 UNIQUE 가 없다. 계좌가 틀려 실패한 뒤 다시 보내는 경우가 있어서다.
 * 그래서 조회 뷰가 <b>가장 최근 한 건</b>만 끌어온다 ({@code ORDER BY created_at DESC, id DESC}).
 *
 * <p>실패 기록을 지우고 다시 넣지 않는 이유는 <b>"왜 늦었나" 가 남아야 하기 때문</b>이다. 돈이
 * 오가는 일이라 시도 이력 자체가 감사 대상이다.
 */
@Getter
@Entity
@Table(name = "remittances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Remittance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "remitted_amount")
    private BigDecimal remittedAmount;

    @Column(name = "remitted_at")
    private Instant remittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RemittanceStatus status;

    /** 이체 증빙. 은행 거래번호를 적는다. */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "memo")
    private String memo;

    /** 기록한 관리자. {@code users.id} 다. */
    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    /**
     * 송금 결과를 기록한다.
     *
     * <p>{@code remittedAt} 은 <b>성공했을 때만</b> 채운다. 실패한 시도에 완료 시각을 남기면 뷰가
     * "언제 송금됐나" 를 물을 때 거짓을 답한다.
     */
    public static Remittance record(
            Long settlementId,
            RemittanceStatus status,
            BigDecimal remittedAmount,
            String referenceNumber,
            String memo,
            Long processedBy,
            Instant now) {
        Remittance remittance = new Remittance();
        remittance.settlementId = settlementId;
        remittance.status = status;
        remittance.remittedAmount = remittedAmount;
        remittance.remittedAt = status == RemittanceStatus.REMITTED ? now : null;
        remittance.referenceNumber = referenceNumber;
        remittance.memo = memo;
        remittance.processedBy = processedBy;
        return remittance;
    }
}
