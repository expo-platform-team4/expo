package com.expo.booth.dto;

import com.expo.booth.entity.BoothContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 부스 콘텐츠 응답. */
@Schema(description = "부스 콘텐츠")
public record BoothContentResponse(
        @Schema(description = "콘텐츠 ID") Long id,
        @Schema(description = "부스 확정 배정 ID") Long boothAllocationId,
        @Schema(description = "작성 클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "기업 노출명") String companyDisplayName,
        @Schema(description = "콘텐츠 제목") String title,
        @Schema(description = "기업 소개") String companyDescription,
        @Schema(description = "부스 소개") String boothDescription,
        @Schema(description = "제품 소개") String productDescription,
        @Schema(description = "로고 파일 ID") Long logoFileId,
        @Schema(description = "대표 이미지 파일 ID") Long mainImageFileId,
        @Schema(description = "공개 상태") BoothContentStatus status,
        @Schema(description = "공개 일시") Instant publishedAt,
        @Schema(description = "보완 요청 일시") Instant correctionRequestedAt,
        @Schema(description = "보완 요청 사유") String correctionMessage,
        @Schema(description = "운영 확인 관리자 ID") Long checkedByAdminId,
        @Schema(description = "운영 확인 일시") Instant checkedAt,
        @Schema(description = "첨부 파일 목록") List<BoothContentFileResponse> files,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
