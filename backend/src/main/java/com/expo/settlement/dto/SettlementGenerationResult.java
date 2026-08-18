package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 정산 대상 생성 결과.
 *
 * <p>몇 건을 만들었는지와 <b>어느 박람회인지</b>를 함께 준다. 개수만 주면 "3건 생겼다" 는 알아도 무엇이
 * 생겼는지 확인하려고 DB 를 따로 열어야 한다. 배치 성격의 API 라 호출한 사람이 곧바로 대사할 수 있어야 한다.
 */
@Schema(description = "정산 대상 생성 결과")
public record SettlementGenerationResult(
        @Schema(description = "새로 만든 정산 수", example = "3") int createdCount,
        @Schema(description = "만들어진 정산") List<Created> created) {

    /** 새로 만든 정산 하나. */
    @Schema(description = "생성된 정산")
    public record Created(
            @Schema(description = "정산 ID", example = "12") Long settlementId,
            @Schema(description = "박람회 ID", example = "7") Long expoId,
            @Schema(description = "박람회명", example = "2026 서울 국제 도서전") String expoTitle,
            @Schema(description = "정산 기한. 행사 종료 + 14일") java.time.Instant settlementDueAt) {}
}
