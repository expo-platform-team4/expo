package com.expo.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expo.settlement.dto.SettlementGenerationResult;
import com.expo.settlement.dto.SettlementTarget;
import com.expo.settlement.repository.SettlementGenerationMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 정산 대상 생성의 규칙을 못박는다.
 *
 * <p>여기서 지키는 것은 <b>날짜 둘의 역할이 다르다</b>는 것과, <b>여러 번 불러도 안전하다</b>는 것이다.
 * 둘 다 틀려도 예외가 안 난다 — 대상이 조용히 빠지거나 정산이 두 벌 생긴다.
 *
 * <p>대상 선정 쿼리 자체(취소·미승인 제외, 이미 있는 것 제외)는 <b>여기서 검증되지 않는다.</b>
 * 매퍼가 mock 이기 때문이다. 그쪽은 실제 DB 를 붙여 확인한다.
 */
class SettlementGenerationServiceTest {

    private final SettlementGenerationMapper generationMapper =
            Mockito.mock(SettlementGenerationMapper.class);
    private final SettlementCreator settlementCreator = Mockito.mock(SettlementCreator.class);

    private SettlementGenerationService service;

    @BeforeEach
    void setUp() {
        service = new SettlementGenerationService(generationMapper, settlementCreator);
    }

    /**
     * 실제 PostgreSQL 이 내는 모양의 예외.
     *
     * <p>메시지에 제약 이름이 들어가야 한다 — 서비스가 <b>그 이름으로</b> "박람회당 정산 하나" 위반인지
     * 판단하기 때문이다. 예전 테스트는 아무 메시지나 넣어서, 제약을 가려내는 코드를 넣었을 때
     * 통과하지 못했다.
     */
    private DataIntegrityViolationException duplicateExpo() {
        return new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique "
                        + "constraint \"settlements_expo_id_key\"]");
    }

    private SettlementTarget target(Long expoId, Instant eventEndAt) {
        return new SettlementTarget(expoId, 100L, "박람회" + expoId, eventEndAt);
    }

    private void given(SettlementTarget... targets) {
        when(generationMapper.findDueExpos(any())).thenReturn(List.of(targets));
        when(settlementCreator.create(any(), any()))
                .thenAnswer(
                        call -> {
                            SettlementTarget t = call.getArgument(0);
                            return new SettlementGenerationResult.Created(
                                    t.expoId() + 1000,
                                    t.expoId(),
                                    t.expoTitle(),
                                    call.getArgument(1));
                        });
    }

    /**
     * 조회 기준은 <b>"지금 - 7일"</b> 하나다.
     *
     * <p>상한(14일)을 조건에 넣으면 배치가 하루만 밀려도 그 사이 끝난 박람회가 영원히 빠진다.
     */
    @Test
    void looksOnlyAtLowerBoundOfSevenDays() {
        given();
        Instant before = Instant.now();

        service.generateDue();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(generationMapper).findDueExpos(captor.capture());

        Instant expected = before.minus(7, ChronoUnit.DAYS);
        assertThat(captor.getValue())
                .isCloseTo(expected, org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
    }

    /** 기한은 행사 종료 + 14일이다. 생성 시각이 아니라 <b>종료 시각</b>이 기준이다. */
    @Test
    void setsDueDateFourteenDaysAfterEventEnd() {
        Instant endedAt = Instant.parse("2026-08-01T10:00:00Z");
        given(target(7L, endedAt));

        service.generateDue();

        verify(settlementCreator).create(any(), eq(endedAt.plus(Duration.ofDays(14))));
    }

    /**
     * 배치가 늦게 돌아도 밀린 것을 함께 잡는다.
     *
     * <p>종료 후 100일 지난 박람회도 대상이다 — 상한이 없기 때문이다.
     */
    @Test
    void picksUpLongOverdueExpos() {
        Instant longAgo = Instant.now().minus(100, ChronoUnit.DAYS);
        given(target(7L, longAgo));

        assertThat(service.generateDue().createdCount()).isEqualTo(1);
    }

    /** 대상이 없으면 아무것도 만들지 않는다. */
    @Test
    void createsNothingWhenNoTarget() {
        given();

        SettlementGenerationResult result = service.generateDue();

        assertThat(result.createdCount()).isZero();
        assertThat(result.created()).isEmpty();
        verify(settlementCreator, never()).create(any(), any());
    }

    /**
     * 동시에 두 번 불려 UNIQUE 에 걸리면 <b>그 건만 건너뛴다.</b>
     *
     * <p>제약 위반은 "이미 있다" 와 결과가 같다. 예외를 밖으로 던지면 배치 전체가 실패한다.
     */
    @Test
    void skipsDuplicateWithoutFailingWholeBatch() {
        given(
                target(7L, Instant.now().minus(10, ChronoUnit.DAYS)),
                target(8L, Instant.now().minus(10, ChronoUnit.DAYS)));
        // doThrow 를 쓴다. when(mock.create(...)) 형태로 다시 스텁하면 그 호출이 앞서 걸어 둔
        // thenAnswer 를 null 인자로 실제 실행해 버려 NPE 가 난다.
        doThrow(duplicateExpo())
                .when(settlementCreator)
                .create(Mockito.argThat(t -> t != null && t.expoId().equals(7L)), any());

        SettlementGenerationResult result = service.generateDue();

        // 7번은 건너뛰고 8번은 만들어진다
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.created().get(0).expoId()).isEqualTo(8L);
    }

    /** 전부 중복이어도 예외가 밖으로 나가지 않는다. */
    @Test
    void survivesWhenEveryTargetIsDuplicate() {
        given(target(7L, Instant.now().minus(10, ChronoUnit.DAYS)));
        doThrow(duplicateExpo()).when(settlementCreator).create(any(), any());

        assertThatCode(() -> service.generateDue()).doesNotThrowAnyException();
        assertThat(service.generateDue().createdCount()).isZero();
    }

    /**
     * <b>UNIQUE 가 아닌 제약 위반은 삼키지 않는다.</b>
     *
     * <p>{@code DataIntegrityViolationException} 을 통째로 잡으면 {@code host_client_id} FK 위반이나
     * {@code NOT NULL} 위반까지 "이미 있어 건너뜀" 이 되어, <b>만들어지지 않은 정산이 정상 응답에
     * 묻힌다.</b> 배치가 "0건 생성" 을 돌려주는데 이유는 아무 데도 없는 상황이다.
     */
    @Test
    void propagatesNonDuplicateConstraintViolations() {
        given(target(7L, Instant.now().minus(10, ChronoUnit.DAYS)));
        doThrow(
                        new DataIntegrityViolationException(
                                "could not execute statement [ERROR: insert or update on table "
                                        + "\"settlements\" violates foreign key constraint "
                                        + "\"fk_settlements_host\"]"))
                .when(settlementCreator)
                .create(any(), any());

        assertThatThrownBy(() -> service.generateDue())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
