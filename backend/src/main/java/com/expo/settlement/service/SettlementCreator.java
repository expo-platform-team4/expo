package com.expo.settlement.service;

import com.expo.settlement.dto.SettlementGenerationResult;
import com.expo.settlement.dto.SettlementTarget;
import com.expo.settlement.entity.Settlement;
import com.expo.settlement.repository.SettlementRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 한 건을 <b>자기 트랜잭션에서</b> 만든다.
 *
 * <h2>왜 별도 클래스인가</h2>
 *
 * {@link SettlementGenerationService} 안의 메서드로 두면 같은 클래스 안 호출이 되어 Spring 프록시를
 * 타지 않고, {@code @Transactional} 이 <b>적용되지 않는다.</b> 그러면 박람회마다 트랜잭션을 나누려던
 * 의도가 사라져 한 건의 제약 위반이 전체를 되돌린다.
 *
 * <p>같은 함정을 환불 알림 발행에서 한 번 밟았다 — 그때는 알림이 0건인데 에러도 안 났다.
 * 여기서는 20건 중 하나가 걸리면 19건이 함께 사라진다.
 *
 * <h2>{@code saveAndFlush} 인 이유</h2>
 *
 * {@code save} 만 하면 INSERT 가 커밋 시점까지 미뤄져, {@code expo_id} UNIQUE 위반이 이 메서드
 * <b>밖에서</b> 터진다. 호출부가 "이 박람회는 건너뛴다" 로 처리하려면 여기서 나야 한다.
 */
@Slf4j
@Service
public class SettlementCreator {

    private final SettlementRepository settlementRepository;

    public SettlementCreator(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SettlementGenerationResult.Created create(
            SettlementTarget target, Instant settlementDueAt) {
        Settlement saved =
                settlementRepository.saveAndFlush(
                        Settlement.waiting(
                                target.expoId(), target.hostClientId(), settlementDueAt));

        log.info(
                "정산 대상 생성 settlementId={} expoId={} 기한={}",
                saved.getId(),
                target.expoId(),
                settlementDueAt);

        return new SettlementGenerationResult.Created(
                saved.getId(), target.expoId(), target.expoTitle(), settlementDueAt);
    }
}
