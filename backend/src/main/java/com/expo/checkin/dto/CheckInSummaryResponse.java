package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 체크인 현황판. 주최사가 현장에서 "지금 몇 명 들어왔나" 를 본다.
 *
 * @param issuedCount 발권된 전체 입장권. 취소·무효 포함이 아니라 <b>유효한 것만</b> 센다
 * @param checkedInCount 입장 완료
 * @param notCheckedInCount 아직 안 온 사람. {@code issuedCount - checkedInCount}
 * @param canceledCount 환불·무효로 못 쓰게 된 표. 위 셋과 별도로 센다
 */
@Schema(description = "체크인 현황")
public record CheckInSummaryResponse(
        @Schema(description = "유효 발권 매수", example = "120") long issuedCount,
        @Schema(description = "입장 완료", example = "83") long checkedInCount,
        @Schema(description = "미입장", example = "37") long notCheckedInCount,
        @Schema(description = "취소·무효", example = "4") long canceledCount) {}
