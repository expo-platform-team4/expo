package com.expo.settlement.dto;

import java.math.BigDecimal;

/**
 * 티켓 환불 집계. 원금과 수수료를 나눠 돌려준다.
 *
 * <p>둘을 합쳐 받으면 안 된다. 원금 환불은 주최사 몫에서 빠지고 수수료 환불은 플랫폼 몫에서 빠져,
 * <b>서로 다른 계정에서 차감</b>된다.
 */
public record TicketRefundAggregate(
        BigDecimal ticketRefundAmount, BigDecimal bookingFeeRefundAmount) {}
