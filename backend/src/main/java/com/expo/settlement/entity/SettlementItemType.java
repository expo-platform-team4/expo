package com.expo.settlement.entity;

/**
 * 정산 항목 종류. {@code settlement_items_item_type_check} 와 값이 같아야 한다.
 *
 * <p>{@code includedInRemittance} 가 <b>주최사에게 송금되는 몫인지</b>를 가른다. 예매 수수료는 구매자가
 * 판매원금 위에 추가로 낸 플랫폼 몫이라 기록만 하고 송금하지 않는다.
 */
public enum SettlementItemType {
    TICKET_SALE(true), // 티켓 판매원금
    TICKET_REFUND(true), // 티켓 환불 (음수로 기록)
    BOOKING_FEE(false), // 예매 수수료 — 플랫폼 몫
    BOOKING_FEE_REFUND(false), // 수수료 환불 — 플랫폼 몫에서 차감
    BOOTH_SALE(true), // 부스 매출 (플랫폼 수수료 0원)
    ADJUSTMENT(true); // 관리자 조정

    private final boolean includedInRemittance;

    SettlementItemType(boolean includedInRemittance) {
        this.includedInRemittance = includedInRemittance;
    }

    public boolean isIncludedInRemittance() {
        return includedInRemittance;
    }
}
