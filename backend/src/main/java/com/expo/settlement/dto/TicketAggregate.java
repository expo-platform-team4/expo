package com.expo.settlement.dto;

import java.math.BigDecimal;

/**
 * 티켓 결제 집계.
 *
 * @param grossTicketSalesAmount 판매원금 합. 주최사 몫의 출발점이다
 * @param bookingFeeGrossAmount 예매 수수료 합. 구매자가 원금 위에 추가로 낸 <b>플랫폼 몫</b>이다
 */
public record TicketAggregate(
        BigDecimal grossTicketSalesAmount, BigDecimal bookingFeeGrossAmount) {}
