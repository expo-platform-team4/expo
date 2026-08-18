package com.expo.settlement.entity;

import java.math.BigDecimal;

/**
 * 정산 금액 한 벌. <b>계산 규칙이 사는 유일한 곳이다.</b>
 *
 * <h2>규칙</h2>
 *
 * {@code docs/init_table_schema.md} 의 최종 확정 정책을 그대로 옮긴 것이다.
 *
 * <pre>
 * 티켓 순매출  = 판매원금 − 환불원금
 * 수수료 순액  = 예매 수수료 − 수수료 환불          ← 플랫폼 몫
 * 부스 매출    = 승인액                             ← 플랫폼 수수료 0원이라 전액
 * 송금액       = 티켓 순매출 + 부스 매출 + 조정
 * </pre>
 *
 * <h2>예매 수수료를 빼지 않는다</h2>
 *
 * 명세가 <b>"예매 수수료를 재차감하지 않음"</b> 이라고 못박았다. 구매자가 판매원금 <b>위에 추가로</b>
 * 낸 돈이라 애초에 주최사 몫이 아니기 때문이다. 그래서 송금액에 더하지도 빼지도 않고 기록만 한다.
 *
 * <p>여기서 실수하기 쉬운 두 가지가 있다.
 *
 * <ul>
 *   <li>수수료를 송금액에 <b>더하면</b> 플랫폼 수입을 주최사에 준다
 *   <li>수수료를 송금액에서 <b>빼면</b> 주최사가 받지도 않은 돈을 토해낸다 — "재차감" 이 이것이다
 * </ul>
 *
 * <h2>왜 순수 record 인가</h2>
 *
 * DB 도 스프링도 모른다. 금액 규칙만 담아 두면 <b>숫자만 넣고 검증할 수 있다.</b> 계산이 서비스
 * 안에 섞여 있으면 규칙 하나를 확인하려고 매퍼를 mock 해야 한다.
 *
 * <p>{@code entity} 패키지에 두는 이유는 이것이 정산 도메인의 <b>값 객체</b>이기 때문이다.
 * {@link Settlement} 가 이 값을 받아 자기 상태로 옮긴다 — 엔티티가 서비스나 dto 를 참조하는
 * 거꾸로 된 의존이 생기지 않게 한다.
 *
 * @param pgFeeReferenceAmount PG 수수료 <b>참고값</b>. 송금액에서 차감하지 않는다. 지금은 항상 0 이다 —
 *     결제 응답의 수수료를 저장하는 컬럼이 없어 원천이 없다. 추측치를 넣느니 0 을 둔다
 */
public record SettlementAmounts(
        BigDecimal grossTicketSalesAmount,
        BigDecimal ticketRefundAmount,
        BigDecimal netTicketSalesAmount,
        BigDecimal bookingFeeGrossAmount,
        BigDecimal bookingFeeRefundAmount,
        BigDecimal bookingFeeNetAmount,
        BigDecimal grossBoothSalesAmount,
        BigDecimal pgFeeReferenceAmount,
        BigDecimal adjustmentAmount,
        BigDecimal remittanceDueAmount) {

    /**
     * 집계값으로 정산 금액을 계산한다.
     *
     * @param adjustment 확정된 조정의 합. 음수일 수 있다 (차감 조정)
     */
    public static SettlementAmounts of(
            BigDecimal grossTicket,
            BigDecimal ticketRefund,
            BigDecimal bookingFeeGross,
            BigDecimal bookingFeeRefund,
            BigDecimal boothSales,
            BigDecimal adjustment) {

        BigDecimal netTicket = nz(grossTicket).subtract(nz(ticketRefund));
        BigDecimal netBookingFee = nz(bookingFeeGross).subtract(nz(bookingFeeRefund));

        // 송금액에 예매 수수료가 없다는 점이 이 식의 전부다.
        BigDecimal remittanceDue = netTicket.add(nz(boothSales)).add(nz(adjustment));

        return new SettlementAmounts(
                nz(grossTicket),
                nz(ticketRefund),
                netTicket,
                nz(bookingFeeGross),
                nz(bookingFeeRefund),
                netBookingFee,
                nz(boothSales),
                BigDecimal.ZERO,
                nz(adjustment),
                remittanceDue);
    }

    /**
     * {@code null} 을 0 으로 바꾼다.
     *
     * <p>집계 쿼리가 {@code COALESCE} 로 0 을 보장하지만, 여기서 한 번 더 막는다. 금액 계산에
     * {@code null} 이 섞이면 {@code NullPointerException} 이 아니라 <b>계산 자체가 안 되는데도
     * 어디서 비었는지 안 보이는</b> 상황이 된다.
     */
    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
