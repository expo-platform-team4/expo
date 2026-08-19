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
 * 박람회 하나의 정산.
 *
 * <h2>박람회당 하나다</h2>
 *
 * {@code settlements.expo_id} 에 UNIQUE 가 걸려 있다. 정산 대상 생성(D-API-017)이 두 번 불려도
 * <b>DB 가 두 번째를 거절한다.</b> 발권·박람회 취소에서는 이런 자연 키가 없어 잠금에 기댔는데,
 * 여기는 제약으로 끝난다.
 *
 * <h2>금액은 전부 {@code BigDecimal} 이고 0 으로 시작한다</h2>
 *
 * 컬럼 열 개가 전부 {@code NUMERIC(15,2) NOT NULL DEFAULT 0} 이다. 생성 시점에는 계산할 것이
 * 없으므로 0 이고, 재계산(D-API-014)이 채운다. <b>{@code double} 을 쓰지 않는다</b> — 돈을 부동소수로
 * 다루면 합계가 원 단위에서 어긋난다.
 *
 * <h2>상태 일곱 단계</h2>
 *
 * <pre>
 * WAITING → CALCULATED → UNDER_REVIEW → CONFIRMED → REMITTANCE_PENDING → REMITTED
 *                                                                      ON_HOLD
 * </pre>
 *
 * 이 엔티티는 {@code WAITING} 만 만든다. 나머지 전이는 각 API 가 붙을 때 메서드로 더한다.
 */
@Getter
@Entity
@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expo_id", nullable = false)
    private Long expoId;

    /** 정산금을 받을 주최사. {@code client_profiles.user_id} 를 가리킨다. */
    @Column(name = "host_client_id", nullable = false)
    private Long hostClientId;

    // --- 티켓 ---

    @Column(name = "gross_ticket_sales_amount", nullable = false)
    private BigDecimal grossTicketSalesAmount;

    @Column(name = "ticket_refund_amount", nullable = false)
    private BigDecimal ticketRefundAmount;

    @Column(name = "net_ticket_sales_amount", nullable = false)
    private BigDecimal netTicketSalesAmount;

    // --- 예매 수수료 (판매원금의 3%, 구매자가 추가 결제한다) ---

    @Column(name = "booking_fee_gross_amount", nullable = false)
    private BigDecimal bookingFeeGrossAmount;

    @Column(name = "booking_fee_refund_amount", nullable = false)
    private BigDecimal bookingFeeRefundAmount;

    @Column(name = "booking_fee_net_amount", nullable = false)
    private BigDecimal bookingFeeNetAmount;

    // --- 부스 (플랫폼 수수료 0원, 결제 후 환불 미지원) ---

    @Column(name = "gross_booth_sales_amount", nullable = false)
    private BigDecimal grossBoothSalesAmount;

    /** PG 수수료 <b>참고값</b>. 정산금에서 차감하지 않고 보여 주기만 한다. */
    @Column(name = "pg_fee_reference_amount", nullable = false)
    private BigDecimal pgFeeReferenceAmount;

    /** 관리자가 넣은 조정의 합계. {@code settlement_adjustments} 에서 확정된 것만 반영된다. */
    @Column(name = "adjustment_amount", nullable = false)
    private BigDecimal adjustmentAmount;

    /** 실제로 송금할 금액. 티켓 순매출 + 부스 매출 ± 조정. */
    @Column(name = "remittance_due_amount", nullable = false)
    private BigDecimal remittanceDueAmount;

    /** 정산 기한. 행사 종료 + 14일이다. */
    @Column(name = "settlement_due_at", nullable = false)
    private Instant settlementDueAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /** 확정한 관리자. {@code users.id} 다. */
    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SettlementStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    /**
     * 정산 대상을 만든다. 금액은 전부 0 이고 상태는 {@code WAITING} 이다.
     *
     * <p>여기서 금액을 계산하지 않는 이유는 <b>대상 선정과 금액 산출이 다른 시점의 일</b>이기 때문이다.
     * 대상은 행사 종료 7일 뒤에 정해지지만, 그때 환불이 아직 진행 중일 수 있다. 재계산은 관리자가
     * 원할 때 몇 번이든 다시 돌린다.
     *
     * @param settlementDueAt 정산 기한. 행사 종료 + 14일
     */
    public static Settlement waiting(Long expoId, Long hostClientId, Instant settlementDueAt) {
        Settlement settlement = new Settlement();
        settlement.expoId = expoId;
        settlement.hostClientId = hostClientId;
        settlement.settlementDueAt = settlementDueAt;
        settlement.status = SettlementStatus.WAITING;

        settlement.grossTicketSalesAmount = BigDecimal.ZERO;
        settlement.ticketRefundAmount = BigDecimal.ZERO;
        settlement.netTicketSalesAmount = BigDecimal.ZERO;
        settlement.bookingFeeGrossAmount = BigDecimal.ZERO;
        settlement.bookingFeeRefundAmount = BigDecimal.ZERO;
        settlement.bookingFeeNetAmount = BigDecimal.ZERO;
        settlement.grossBoothSalesAmount = BigDecimal.ZERO;
        settlement.pgFeeReferenceAmount = BigDecimal.ZERO;
        settlement.adjustmentAmount = BigDecimal.ZERO;
        settlement.remittanceDueAmount = BigDecimal.ZERO;
        return settlement;
    }

    /**
     * 계산 결과를 반영한다.
     *
     * <p>몇 번이든 다시 부를 수 있다. 환불이 뒤늦게 완료되거나 조정이 추가되면 금액이 달라지므로,
     * 재계산은 <b>덮어쓰기</b>지 누적이 아니다.
     *
     * <h2>{@code ON_HOLD} 는 상태를 건드리지 않는다</h2>
     *
     * 보류는 분쟁·조사로 <b>사람이 멈춰 둔 것</b>이다. 재계산이 상태까지 {@code CALCULATED} 로
     * 바꾸면 보류가 조용히 풀리고, 그 뒤 확정·송금까지 그대로 진행된다 — <b>멈춰 둔 이유가
     * 남아 있는데 돈이 나간다.</b> 보류 해제는 명시적인 조작이어야 한다.
     *
     * <p>그래서 보류 중에는 금액만 갱신한다. 조사하는 동안 최신 금액을 보는 것은 필요하고,
     * 그것과 "진행해도 된다" 는 별개다.
     */
    public void applyCalculation(SettlementAmounts amounts) {
        this.grossTicketSalesAmount = amounts.grossTicketSalesAmount();
        this.ticketRefundAmount = amounts.ticketRefundAmount();
        this.netTicketSalesAmount = amounts.netTicketSalesAmount();
        this.bookingFeeGrossAmount = amounts.bookingFeeGrossAmount();
        this.bookingFeeRefundAmount = amounts.bookingFeeRefundAmount();
        this.bookingFeeNetAmount = amounts.bookingFeeNetAmount();
        this.grossBoothSalesAmount = amounts.grossBoothSalesAmount();
        this.pgFeeReferenceAmount = amounts.pgFeeReferenceAmount();
        this.adjustmentAmount = amounts.adjustmentAmount();
        this.remittanceDueAmount = amounts.remittanceDueAmount();

        // 보류 중이면 금액만 갱신하고 상태는 그대로 둔다. 위 주석 참고.
        if (this.status != SettlementStatus.ON_HOLD) {
            this.status = SettlementStatus.CALCULATED;
        }
    }

    /**
     * 재계산할 수 있는 상태인가.
     *
     * <p>확정({@code CONFIRMED}) 이후로는 막는다. 확정은 "이 금액으로 송금한다" 는 선언이고, 그 뒤에
     * 금액이 조용히 바뀌면 <b>송금한 금액과 기록이 어긋난다.</b> 금액을 고쳐야 한다면 조정
     * ({@code settlement_adjustments})으로 남겨야 추적된다.
     */
    public boolean recalculable() {
        return status == SettlementStatus.WAITING
                || status == SettlementStatus.CALCULATED
                || status == SettlementStatus.UNDER_REVIEW
                || status == SettlementStatus.ON_HOLD;
    }

    /**
     * 확정할 수 있는 상태인가 (D-API-015).
     *
     * <p>{@code WAITING} 은 <b>제외한다.</b> 금액이 전부 0 인 상태라 "계산해 보지도 않고 확정" 이 되고,
     * 확정 뒤에는 재계산이 막히므로 <b>0원으로 굳어 버린다.</b> 최소한 한 번은 계산해야 한다.
     */
    public boolean confirmable() {
        return status == SettlementStatus.CALCULATED || status == SettlementStatus.UNDER_REVIEW;
    }

    // ON_HOLD 는 여기 없다. 보류를 푸는 것과 확정하는 것은 다른 조작이다.

    /**
     * 확정한다. <b>되돌리는 메서드는 두지 않는다.</b>
     *
     * <p>확정은 "이 금액으로 송금한다" 는 선언이다. 되돌리기를 열어 두면 송금한 뒤에 금액이 바뀌는
     * 길이 생긴다. 잘못 확정했다면 조정({@code settlement_adjustments})으로 남겨야 추적된다.
     *
     * @param confirmedBy 확정한 관리자. {@code users.id}
     */
    public void confirm(Long confirmedBy, Instant confirmedAt) {
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
    }

    /**
     * 송금 결과를 반영할 수 있는 상태인가 (D-API-016).
     *
     * <p>확정 전에는 기록할 수 없다. 금액이 아직 바뀔 수 있는데 송금부터 하면 <b>보낸 돈과 확정액이
     * 어긋난다.</b> {@code REMITTED} 뒤에도 다시 기록할 수 있는데, 첫 시도가 실패해 다시 보내는
     * 경우가 있어서다.
     */
    public boolean remittable() {
        return status == SettlementStatus.CONFIRMED
                || status == SettlementStatus.REMITTANCE_PENDING
                || status == SettlementStatus.REMITTED;
    }

    /**
     * 송금 결과에 따라 상태를 옮긴다.
     *
     * <p>성공이면 {@code REMITTED}, 아니면 {@code REMITTANCE_PENDING} 이다. 실패를 {@code ON_HOLD}
     * 로 두지 않는 이유는 <b>보류와 실패가 다르기 때문</b>이다 — 보류는 사람이 멈춘 것이고, 실패는
     * 다시 보내야 하는 것이다.
     */
    public void applyRemittance(RemittanceStatus remittanceStatus) {
        this.status =
                remittanceStatus == RemittanceStatus.REMITTED
                        ? SettlementStatus.REMITTED
                        : SettlementStatus.REMITTANCE_PENDING;
    }
}
