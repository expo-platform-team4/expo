package com.expo.banner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * 광고 배너 노출 신청 등록 요청 (B-API-019: POST /api/client/banner-requests)
 *
 * 배너 이미지는 사전에 파일 업로드(B-DES-002)로 file_metadata 에 등록된 뒤
 * 그 file_id 를 참조한다. 신청은 별도 임시저장 없이 등록과 동시에 심사 요청
 * 상태(UNDER_REVIEW)로 전환된다.
 */
@Schema(description = "배너 노출 신청 등록 요청")
public record BannerApplicationCreateRequest(
        @Schema(description = "홍보 대상 박람회 ID") @NotNull(message = "박람회는 필수입니다.") Long expoId,
        @Schema(description = "배너 이미지 파일 ID") @NotNull(message = "배너 이미지는 필수입니다.") Long imageFileId,
        @Schema(description = "배너 문구") @Size(max = 150, message = "배너 문구는 150자를 초과할 수 없습니다.")
                String headline,
        @Schema(description = "희망 노출 시작일시")
                @NotNull(message = "희망 노출 시작일시는 필수입니다.")
                @Future(message = "희망 노출 시작일시는 미래여야 합니다.")
                OffsetDateTime requestedStartAt,
        @Schema(description = "희망 노출 종료일시") @NotNull(message = "희망 노출 종료일시는 필수입니다.")
                OffsetDateTime requestedEndAt) {}
