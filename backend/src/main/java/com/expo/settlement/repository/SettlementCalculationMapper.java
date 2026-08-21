package com.expo.settlement.repository;

import com.expo.settlement.dto.TicketAggregate;
import com.expo.settlement.dto.TicketRefundAggregate;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 정산 금액을 집계한다 (D-API-014).
 *
 * <p>넷으로 나눈 이유는 <b>원천이 다르기 때문</b>이다. 한 쿼리로 합치면 티켓이 없는 박람회에서 부스
 * 매출까지 사라지는 식의 조인 사고가 나기 쉽다. 각각 독립적으로 0 이 나오는 편이 안전하다.
 */
@Mapper
public interface SettlementCalculationMapper {

    /** 결제 완료된 티켓의 판매원금·예매 수수료. */
    TicketAggregate aggregateTicket(@Param("expoId") Long expoId);

    /** 환불 완료된 금액. 원금과 수수료가 나뉘어 있다. */
    TicketRefundAggregate aggregateTicketRefund(@Param("expoId") Long expoId);

    /**
     * 결제 완료된 부스 매출.
     *
     * <p>{@code expo_companies} 를 거쳐야 박람회에 닿는다. 그 테이블을 채우는 코드가 아직 없어
     * 현재는 시드로만 값이 나온다.
     */
    BigDecimal aggregateBoothSales(@Param("expoId") Long expoId);

    /** 확정({@code APPROVED})된 조정의 합. 없으면 0. */
    BigDecimal aggregateAdjustment(@Param("settlementId") Long settlementId);
}
