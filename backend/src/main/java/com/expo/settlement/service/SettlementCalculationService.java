package com.expo.settlement.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.settlement.dto.SettlementCalculationResult;
import com.expo.settlement.dto.TicketAggregate;
import com.expo.settlement.dto.TicketRefundAggregate;
import com.expo.settlement.entity.Settlement;
import com.expo.settlement.entity.SettlementAmounts;
import com.expo.settlement.entity.SettlementItem;
import com.expo.settlement.entity.SettlementItemType;
import com.expo.settlement.repository.SettlementCalculationMapper;
import com.expo.settlement.repository.SettlementItemRepository;
import com.expo.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버 기준으로 정산액을 다시 계산한다 (D-API-014).
 *
 * <h2>몇 번이든 다시 돌린다</h2>
 *
 * 정산 대상은 행사 종료 7일 뒤에 만들어지는데, 그때 환불이 아직 진행 중일 수 있다. 조정도 나중에
 * 붙는다. 그래서 재계산은 <b>덮어쓰기</b>다 — 이전 결과에 더하지 않는다.
 *
 * <p>{@code settlement_items} 도 지우고 다시 만든다. 누적하면 항목이 두 벌 쌓여 상세 화면의 합이
 * 실제 금액의 두 배가 된다.
 *
 * <h2>확정 뒤에는 막는다</h2>
 *
 * {@code CONFIRMED} 부터는 "이 금액으로 송금한다" 는 선언이다. 그 뒤에 금액이 조용히 바뀌면
 * <b>송금한 돈과 기록이 어긋난다.</b> 금액을 고쳐야 하면 조정으로 남겨야 추적된다.
 *
 * <h2>3% 를 여기서 계산하지 않는다</h2>
 *
 * {@code ticket_payments} 가 판매원금과 예매 수수료를 이미 나눠 저장한다. 3% 는 결제를 만들 때
 * 적용하는 규칙이고, 정산은 <b>기록된 값을 합산</b>할 뿐이다. 정산에도 3% 를 박으면 규칙이 두 곳에
 * 생겨 한쪽만 바뀌는 날이 온다.
 */
@Slf4j
@Service
public class SettlementCalculationService {

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    private final SettlementCalculationMapper calculationMapper;

    public SettlementCalculationService(
            SettlementRepository settlementRepository,
            SettlementItemRepository settlementItemRepository,
            SettlementCalculationMapper calculationMapper) {
        this.settlementRepository = settlementRepository;
        this.settlementItemRepository = settlementItemRepository;
        this.calculationMapper = calculationMapper;
    }

    /** 정산 하나를 다시 계산한다. */
    @Transactional
    public SettlementCalculationResult calculate(Long settlementId) {
        Settlement settlement =
                settlementRepository
                        .findById(settlementId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (!settlement.recalculable()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_RECALCULABLE);
        }

        SettlementAmounts amounts = aggregate(settlement);
        settlement.applyCalculation(amounts);
        rewriteItems(settlementId, amounts);

        log.info(
                "정산 재계산 settlementId={} expoId={} 송금액={}",
                settlementId,
                settlement.getExpoId(),
                amounts.remittanceDueAmount());

        return SettlementCalculationResult.of(settlementId, settlement.getStatus().name(), amounts);
    }

    /** 네 원천에서 각각 집계한다. 하나가 0 이어도 나머지는 영향을 받지 않는다. */
    private SettlementAmounts aggregate(Settlement settlement) {
        Long expoId = settlement.getExpoId();

        TicketAggregate ticket = calculationMapper.aggregateTicket(expoId);
        TicketRefundAggregate refund = calculationMapper.aggregateTicketRefund(expoId);
        BigDecimal boothSales = calculationMapper.aggregateBoothSales(expoId);
        BigDecimal adjustment = calculationMapper.aggregateAdjustment(settlement.getId());

        return SettlementAmounts.of(
                ticket.grossTicketSalesAmount(),
                refund.ticketRefundAmount(),
                ticket.bookingFeeGrossAmount(),
                refund.bookingFeeRefundAmount(),
                boothSales,
                adjustment);
    }

    /**
     * 항목을 갈아엎는다.
     *
     * <p>0 인 항목은 남기지 않는다. "부스 매출 0원" 을 굳이 한 줄 남기면 상세 화면이 의미 없는 행으로
     * 채워진다. 다만 <b>환불은 0이 아니면 음수로</b> 남긴다 — 합을 그대로 더하면 송금액이 나오게 하려는
     * 것이고, 화면에서 부호를 다시 뒤집지 않아도 된다.
     */
    private void rewriteItems(Long settlementId, SettlementAmounts amounts) {
        settlementItemRepository.deleteAllBySettlementId(settlementId);

        Instant now = Instant.now();
        List<SettlementItem> items = new ArrayList<>();

        addIfNonZero(
                items,
                settlementId,
                SettlementItemType.TICKET_SALE,
                amounts.grossTicketSalesAmount(),
                now);
        addIfNonZero(
                items,
                settlementId,
                SettlementItemType.TICKET_REFUND,
                amounts.ticketRefundAmount().negate(),
                now);
        addIfNonZero(
                items,
                settlementId,
                SettlementItemType.BOOKING_FEE,
                amounts.bookingFeeGrossAmount(),
                now);
        addIfNonZero(
                items,
                settlementId,
                SettlementItemType.BOOKING_FEE_REFUND,
                amounts.bookingFeeRefundAmount().negate(),
                now);
        addIfNonZero(
                items,
                settlementId,
                SettlementItemType.BOOTH_SALE,
                amounts.grossBoothSalesAmount(),
                now);
        addIfNonZero(
                items,
                settlementId,
                SettlementItemType.ADJUSTMENT,
                amounts.adjustmentAmount(),
                now);

        settlementItemRepository.saveAll(items);
    }

    private void addIfNonZero(
            List<SettlementItem> items,
            Long settlementId,
            SettlementItemType type,
            BigDecimal amount,
            Instant occurredAt) {
        if (amount.signum() != 0) {
            items.add(SettlementItem.of(settlementId, type, amount, occurredAt));
        }
    }
}
