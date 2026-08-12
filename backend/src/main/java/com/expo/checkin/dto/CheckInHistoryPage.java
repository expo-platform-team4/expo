package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 체크인 이력 한 페이지.
 *
 * @param totalCount 전체 건수. 화면이 페이지 수를 계산한다
 */
@Schema(description = "체크인 이력 페이지")
public record CheckInHistoryPage(
        @Schema(description = "전체 건수", example = "412") long totalCount,
        @Schema(description = "요청한 페이지 번호 (0부터)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "이력 목록") List<CheckInHistoryRow> items) {}
