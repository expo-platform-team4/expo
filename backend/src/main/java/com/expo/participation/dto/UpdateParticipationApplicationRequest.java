package com.expo.participation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 참여 신청서 수정 요청. 초안 상태에서만 가능하다. */
@Schema(description = "참여 신청서 수정 요청")
public record UpdateParticipationApplicationRequest(
        @Schema(description = "참가 기업명 스냅샷", example = "(주)엑스포플랫폼")
                @NotBlank(message = "기업명은 필수입니다.")
                @Size(max = 150, message = "기업명은 150자 이하여야 합니다.")
                String companyNameSnapshot,
        @Schema(description = "참여 목적") String participationPurpose,
        @Schema(description = "전시 품목 설명") String exhibitDescription,
        @Schema(description = "선택한 부스 상품 ID") Long selectedBoothProductId) {}
