package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 박람회 개최 신청 반려 요청. 사유는 주최사에게 그대로 보여주므로 필수다. */
@Schema(description = "박람회 개최 신청 반려 요청")
public record RejectExpoOpeningRequestRequest(
        @Schema(description = "반려 사유", example = "행사 기간이 기존 박람회와 겹칩니다.")
                @NotBlank(message = "반려 사유는 필수입니다.")
                String reason) {}
