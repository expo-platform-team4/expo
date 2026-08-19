package com.expo.settlement.service;

import com.expo.settlement.dto.SettlementGenerationResult;
import com.expo.settlement.dto.SettlementTarget;
import com.expo.settlement.repository.SettlementGenerationMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 끝난 박람회의 정산 대상을 만든다 (D-API-017).
 *
 * <h2>7과 14는 다른 곳에 쓰인다</h2>
 *
 * 명세는 "행사 종료 후 7~14일 이내에 대상을 생성한다" 이다. 이 둘을 <b>조회 조건의 양끝으로 쓰지
 * 않는다.</b>
 *
 * <pre>
 * 7일  → 대상이 되는 시점. "종료하고 7일이 지났나"
 * 14일 → 만들어진 정산의 기한(settlement_due_at). "언제까지 처리해야 하나"
 * </pre>
 *
 * <p>상한(14일)을 조회 조건에 넣으면 <b>배치가 하루라도 밀렸을 때 그 사이 끝난 박람회가 영원히
 * 정산되지 않는다.</b> 조용히 빠지고 아무도 모른다. 하한만 보면 늦게 돌아도 밀린 것까지 함께 잡는다.
 *
 * <h2>중복 생성은 DB 가 막는다</h2>
 *
 * {@code settlements.expo_id} 에 UNIQUE 가 있다. 조회에서 이미 있는 것을 빼지만, 두 요청이 동시에
 * 들어오면 둘 다 "없다" 를 보고 지나갈 수 있다. 그때 두 번째 INSERT 가 제약에 걸린다.
 *
 * <p>발권·박람회 취소에서는 이런 자연 키가 없어 행 잠금에 기대야 했는데, 여기는 <b>제약 하나로
 * 끝난다.</b> 잠금은 걸지 않는다.
 *
 * <h2>한 건이 실패해도 나머지는 만든다</h2>
 *
 * 박람회마다 트랜잭션을 나눈다. 배치 성격이라 20건 중 하나가 걸렸다고 19건을 되돌릴 이유가 없다.
 */
@Slf4j
@Service
public class SettlementGenerationService {

    /** 이만큼 지난 박람회부터 대상이 된다. */
    private static final Duration ELIGIBLE_AFTER = Duration.ofDays(7);

    /** 행사 종료로부터 이만큼이 정산 기한이다. */
    private static final Duration DUE_AFTER = Duration.ofDays(14);

    /** 박람회당 정산 하나를 강제하는 제약. 이 위반만 "이미 있다" 로 본다. */
    private static final String DUPLICATE_EXPO_CONSTRAINT = "settlements_expo_id_key";

    private final SettlementGenerationMapper generationMapper;
    private final SettlementCreator settlementCreator;

    public SettlementGenerationService(
            SettlementGenerationMapper generationMapper, SettlementCreator settlementCreator) {
        this.generationMapper = generationMapper;
        this.settlementCreator = settlementCreator;
    }

    /**
     * 대상을 찾아 정산을 만든다. <b>여러 번 불러도 안전하다.</b>
     *
     * <p>트랜잭션을 열지 않는다. 박람회 하나하나가 {@link SettlementCreator} 안에서 자기 트랜잭션을
     * 갖는다 — 여기서 열면 그 경계가 하나로 합쳐져 "한 건 실패에 전부 롤백" 이 된다.
     */
    public SettlementGenerationResult generateDue() {
        Instant endedBefore = Instant.now().minus(ELIGIBLE_AFTER);
        List<SettlementTarget> targets = generationMapper.findDueExpos(endedBefore);

        if (targets.isEmpty()) {
            log.info("정산 대상 없음 기준={}", endedBefore);
            return new SettlementGenerationResult(0, List.of());
        }

        List<SettlementGenerationResult.Created> created =
                targets.stream().map(this::createOrSkip).filter(Objects::nonNull).toList();

        log.info("정산 대상 생성 후보={}건 생성={}건", targets.size(), created.size());
        return new SettlementGenerationResult(created.size(), created);
    }

    /**
     * 한 건을 만든다. 경합으로 이미 만들어졌으면 {@code null} 을 돌려 건너뛴다.
     *
     * <p><b>{@code expo_id} UNIQUE 위반만 건너뛴다.</b> {@link DataIntegrityViolationException}
     * 을 통째로 삼키면 {@code host_client_id} FK 위반이나 {@code NOT NULL} 위반까지
     * "이미 있어 건너뜀" 이 되어, <b>만들어지지 않은 정산이 정상 응답에 묻힌다.</b>
     */
    private SettlementGenerationResult.Created createOrSkip(SettlementTarget target) {
        try {
            return settlementCreator.create(target, target.eventEndAt().plus(DUE_AFTER));
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateExpo(e)) {
                throw e;
            }
            // 동시에 두 번 불린 경우다. 결과는 "이미 있다" 로 같으므로 건너뛴다.
            log.info("정산이 이미 있어 건너뜀 expoId={}", target.expoId());
            return null;
        }
    }

    /**
     * 이 위반이 "박람회당 정산 하나" 제약인가.
     *
     * <p>제약 이름으로 판단한다. PostgreSQL 이 컬럼 하나짜리 UNIQUE 에 붙이는 기본 이름이
     * {@code settlements_expo_id_key} 다. 이름이 바뀌면 여기도 바꿔야 하지만, 그때는 <b>건너뛰지
     * 않고 예외가 올라오므로</b> 조용히 틀리지 않는다 — 안전한 쪽으로 실패한다.
     */
    private boolean isDuplicateExpo(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(DUPLICATE_EXPO_CONSTRAINT);
    }
}
