package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 부스 콘텐츠 수정 요청. */
@Schema(description = "부스 콘텐츠 수정 요청")
public record UpdateBoothContentRequest(
        @Schema(description = "기업 노출명") @NotBlank(message = "기업 노출명은 필수입니다.")
                String companyDisplayName,
        @Schema(description = "콘텐츠 제목") @NotBlank(message = "콘텐츠 제목은 필수입니다.") String title,
        @Schema(description = "기업 소개") String companyDescription,
        @Schema(description = "부스 소개") String boothDescription,
        @Schema(description = "제품 소개") String productDescription,
        @Schema(description = "로고 파일 ID") Long logoFileId,
        @Schema(description = "대표 이미지 파일 ID") Long mainImageFileId) {}
