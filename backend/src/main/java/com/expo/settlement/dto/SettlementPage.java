package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 정산 목록 페이지. 관리자 감사 이력·알림 이력과 같은 모양이다. */
@Schema(description = "정산 목록 페이지")
public record SettlementPage<T>(
        @Schema(description = "전체 건수", example = "42") long totalCount,
        @Schema(description = "요청한 페이지 번호 (0부터)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "정산 목록") List<T> items) {}
