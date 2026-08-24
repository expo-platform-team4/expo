package com.expo.banner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 배너 신청 반려 요청 (B-API-023: POST /api/admin/banner-requests/{requestId}/reject) */
@Schema(description = "배너 신청 반려 요청")
public record BannerApplicationRejectRequest(
        @Schema(description = "반려 사유")
                @NotBlank(message = "반려 사유는 필수입니다.")
                @Size(max = 1000, message = "반려 사유는 1000자를 초과할 수 없습니다.")
                String reason) {}
