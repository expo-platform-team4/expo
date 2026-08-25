package com.expo.banner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 배너 노출 상태 정리 결과.
 *
 * <p>개수와 <b>어느 배너인지</b>를 함께 준다. 배치 성격의 API 라 부른 사람이 곧바로 대사할 수 있어야
 * 한다 — 개수만 주면 "3건 켜졌다" 는 알아도 무엇이 켜졌는지 확인하려고 DB 를 따로 열어야 한다.
 */
@Schema(description = "배너 노출 상태 정리 결과")
public record BannerDisplaySyncResult(
        @Schema(description = "새로 노출 시작한 배너 수", example = "2") int activatedCount,
        @Schema(description = "노출을 끝낸 배너 수", example = "1") int endedCount,
        @Schema(description = "노출 시작한 배너 ID") List<Long> activatedBannerIds,
        @Schema(description = "노출 끝낸 배너 ID") List<Long> endedBannerIds) {}
