package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 공개(비로그인) 부스 콘텐츠 응답.
 *
 * <p>{@link BoothContentResponse} 와 달리 작성 기업 사용자 ID, 보완 요청 사유, 운영 확인 관리자 정보처럼 방문자에게
 * 노출하면 안 되는 내부 필드는 뺀다.
 */
@Schema(description = "공개 부스 콘텐츠")
public record PublicBoothContentResponse(
        @Schema(description = "콘텐츠 ID") Long id,
        @Schema(description = "부스 확정 배정 ID") Long boothAllocationId,
        @Schema(description = "기업 노출명") String companyDisplayName,
        @Schema(description = "콘텐츠 제목") String title,
        @Schema(description = "기업 소개") String companyDescription,
        @Schema(description = "부스 소개") String boothDescription,
        @Schema(description = "제품 소개") String productDescription,
        @Schema(description = "로고 파일 ID") Long logoFileId,
        @Schema(description = "대표 이미지 파일 ID") Long mainImageFileId,
        @Schema(description = "공개 일시") Instant publishedAt,
        @Schema(description = "첨부 파일 목록") List<BoothContentFileResponse> files,
        @Schema(description = "외부 링크 목록") List<ExternalLinkResponse> links) {}
