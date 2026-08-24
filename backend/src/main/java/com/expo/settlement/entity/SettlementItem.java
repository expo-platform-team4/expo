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
 * 정산 금액의 구성 항목. <b>"이 숫자가 어디서 왔나" 를 남긴다.</b>
 *
 * <h2>집계 단위로 남긴다</h2>
 *
 * 결제 한 건마다 한 행이 아니라 <b>항목 종류마다 한 행</b>이다. 그래서 {@code source_type}·
 * {@code source_id} 는 비어 있다 — 스키마가 둘 다 nullable 인 것이 이 사용을 허용한다.
 *
 * <p>1,000건 팔린 박람회에서 결제마다 행을 남기면 정산 하나가 1,000행이 되는데, 정산 상세 화면이
 * 필요로 하는 것은 "티켓 얼마, 부스 얼마" 다. 건별 추적이 필요해지면 그때 행을 더 잘게 쪼개면 되고,
 * 스키마는 이미 그것도 받는다.
 *
 * <h2>재계산하면 갈아엎는다</h2>
 *
 * 재계산은 몇 번이든 돌 수 있다. 이전 항목을 지우고 다시 만든다 — 누적하면 금액이 두 배가 된다.
 */
@Getter
@Entity
@Table(name = "settlement_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private SettlementItemType itemType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    /** 주최사에게 송금되는 몫인가. 예매 수수료는 {@code false} 다. */
    @Column(name = "included_in_remittance", nullable = false)
    private boolean includedInRemittance;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /**
     * 항목 하나를 만든다. 송금 포함 여부는 <b>종류가 정한다</b> — 호출부가 정하게 두면 같은 종류가
     * 곳에 따라 다르게 기록될 수 있다.
     *
     * @param occurredAt 계산 시각. 집계 행이라 개별 결제 시각이 아니다
     */
    public static SettlementItem of(
            Long settlementId, SettlementItemType itemType, BigDecimal amount, Instant occurredAt) {
        SettlementItem item = new SettlementItem();
        item.settlementId = settlementId;
        item.itemType = itemType;
        item.amount = amount;
        item.includedInRemittance = itemType.isIncludedInRemittance();
        item.occurredAt = occurredAt;
        return item;
    }
}
