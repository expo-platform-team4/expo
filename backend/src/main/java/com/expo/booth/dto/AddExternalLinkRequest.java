package com.expo.booth.dto;

import com.expo.booth.entity.ExternalLinkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 부스 콘텐츠 외부 링크 등록 요청. */
@Schema(description = "부스 콘텐츠 외부 링크 등록 요청")
public record AddExternalLinkRequest(
        @Schema(description = "링크 종류") @NotNull(message = "링크 종류는 필수입니다.")
                ExternalLinkType linkType,
        @Schema(description = "표시 라벨") @Size(max = 100, message = "표시 라벨은 100자 이하여야 합니다.")
                String label,
        @Schema(description = "URL")
                @NotBlank(message = "URL은 필수입니다.")
                @Size(max = 1000, message = "URL은 1000자 이하여야 합니다.")
                String url,
        @Schema(description = "노출 순서") Integer sortOrder) {}
