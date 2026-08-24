package com.expo.settlement.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.settlement.dto.RemittanceRecordRequest;
import com.expo.settlement.dto.RemittanceRecordResult;
import com.expo.settlement.dto.SettlementConfirmResult;
import com.expo.settlement.entity.Remittance;
import com.expo.settlement.entity.RemittanceStatus;
import com.expo.settlement.entity.Settlement;
import com.expo.settlement.repository.RemittanceRepository;
import com.expo.settlement.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 확정과 송금 결과 기록 (D-API-015 · 016).
 *
 * <h2>둘이 한 서비스인 이유</h2>
 *
 * 확정과 송금은 <b>같은 상태 기계의 이웃한 두 칸</b>이다. 확정이 송금의 전제이고, 확정 조건이
 * 바뀌면 송금 조건도 함께 봐야 한다. 나눠 두면 한쪽만 고치는 날이 온다.
 *
 * <h2>확정은 되돌릴 수 없다</h2>
 *
 * 되돌리는 API 를 만들지 않았다. 확정은 "이 금액으로 송금한다" 는 선언이라, 되돌리기를 열면
 * <b>송금 뒤에 금액이 바뀌는 길</b>이 생긴다. 잘못 확정했다면 조정으로 남겨야 추적된다.
 *
 * <h2>송금은 시스템이 하지 않는다</h2>
 *
 * 명세가 "은행 자동 송금은 MVP 에 포함하지 않는다" 고 못박았다. 담당자가 이체하고 결과를 적는다.
 * 그래서 이 API 는 <b>이미 일어난 일을 기록</b>하는 것이고, 금액이 확정액과 달라도 거절하지 않는다 —
 * 거절해 봐야 사실이 사라질 뿐이다. 대신 응답과 로그로 눈에 띄게 알린다.
 */
@Slf4j
@Service
public class SettlementConfirmService {

    private final SettlementRepository settlementRepository;
    private final RemittanceRepository remittanceRepository;

    public SettlementConfirmService(
            SettlementRepository settlementRepository, RemittanceRepository remittanceRepository) {
        this.settlementRepository = settlementRepository;
        this.remittanceRepository = remittanceRepository;
    }

    /**
     * 검토를 마치고 확정한다 (D-API-015).
     *
     * <p>{@code WAITING} 에서는 확정할 수 없다. 금액이 전부 0 인 상태이고, 확정 뒤에는 재계산이
     * 막히므로 <b>0원으로 굳어 버린다.</b>
     */
    @Transactional
    public SettlementConfirmResult confirm(Long settlementId, Long adminUserId) {
        Settlement settlement = load(settlementId);

        if (!settlement.confirmable()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_CONFIRMABLE);
        }

        Instant now = Instant.now();
        settlement.confirm(adminUserId, now);

        log.info(
                "정산 확정 settlementId={} 송금예정액={} 확정자={}",
                settlementId,
                settlement.getRemittanceDueAmount(),
                adminUserId);

        return new SettlementConfirmResult(
                settlementId,
                settlement.getStatus().name(),
                settlement.getRemittanceDueAmount(),
                now,
                adminUserId);
    }

    /**
     * 외부 송금 결과를 기록한다 (D-API-016).
     *
     * <p>확정 전에는 기록할 수 없다. 금액이 아직 바뀔 수 있는데 송금부터 하면 보낸 돈과 확정액이
     * 어긋난다.
     */
    @Transactional
    public RemittanceRecordResult recordRemittance(
            Long settlementId, RemittanceRecordRequest request, Long adminUserId) {
        Settlement settlement = load(settlementId);

        if (!settlement.remittable()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_REMITTABLE);
        }

        RemittanceStatus remittanceStatus = parseStatus(request.status());
        Instant now = Instant.now();

        Remittance remittance =
                remittanceRepository.save(
                        Remittance.record(
                                settlementId,
                                remittanceStatus,
                                request.remittedAmount(),
                                request.referenceNumber(),
                                request.memo(),
                                adminUserId,
                                now));

        settlement.applyRemittance(remittanceStatus);

        boolean matches = amountMatchesDue(settlement.getRemittanceDueAmount(), request);
        if (!matches) {
            // 거절하지 않는다. 이미 일어난 일이다. 다만 조용히 넘기면 아무도 모른다.
            log.error(
                    "송금액이 확정액과 다르다 settlementId={} 확정액={} 이체액={}",
                    settlementId,
                    settlement.getRemittanceDueAmount(),
                    request.remittedAmount());
        }

        log.info(
                "송금 결과 기록 settlementId={} 송금상태={} 정산상태={} 기록자={}",
                settlementId,
                remittanceStatus,
                settlement.getStatus(),
                adminUserId);

        return new RemittanceRecordResult(
                settlementId,
                remittance.getId(),
                settlement.getStatus().name(),
                remittanceStatus.name(),
                settlement.getRemittanceDueAmount(),
                request.remittedAmount(),
                matches,
                remittance.getRemittedAt());
    }

    private Settlement load(Long settlementId) {
        return settlementRepository
                .findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
    }

    /**
     * 확정액과 이체액이 같은가.
     *
     * <p>성공하지 않은 기록은 비교하지 않는다 — 실패한 이체에 금액을 안 적는 것이 정상이고, 그것을
     * "불일치" 로 알리면 진짜 불일치가 묻힌다.
     */
    private boolean amountMatchesDue(BigDecimal due, RemittanceRecordRequest request) {
        if (!RemittanceStatus.REMITTED.name().equals(request.status())) {
            return true;
        }
        return request.remittedAmount() != null && due.compareTo(request.remittedAmount()) == 0;
    }

    private RemittanceStatus parseStatus(String status) {
        try {
            return RemittanceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REMITTANCE_STATUS);
        }
    }
}
