package com.expo.expo.dto;

import com.expo.expo.entity.ExpoOpeningRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 박람회 개최 신청 응답.
 *
 * <p>관리자 목록 화면이 "주최자·기업명" 을 보여줘야 해서 {@code hostCompanyName} 을 함께 싣는다 — 화면이 회사명을 다시
 * 조회하지 않아도 되게 한다. 주최사 본인 화면에서는 자기 회사라 쓰지 않는다.
 *
 * <p>{@code createdExpoId} 는 승인된 건에만 있다. 승인 후 "내 박람회" 로 넘어가는 링크에 쓴다.
 */
@Schema(description = "박람회 개최 신청")
public record ExpoOpeningRequestResponse(
        @Schema(description = "신청 ID") Long id,
        @Schema(description = "주최 클라이언트 ID") Long hostClientId,
        @Schema(description = "주최 기업명") String hostCompanyName,
        @Schema(description = "박람회명") String title,
        @Schema(description = "상세 소개") String description,
        @Schema(description = "행사 시작 일시") Instant eventStartAt,
        @Schema(description = "행사 종료 일시") Instant eventEndAt,
        @Schema(description = "판매 시작 일시") Instant salesStartAt,
        @Schema(description = "판매 종료 일시") Instant salesEndAt,
        @Schema(description = "희망 가상 장소 ID") Long desiredVenueId,
        @Schema(description = "희망 가상 장소명") String desiredVenueName,
        @Schema(description = "희망 전시관 ID") Long desiredVenueHallId,
        @Schema(description = "희망 구역 ID") Long desiredVenueZoneId,
        @Schema(description = "카테고리 ID 목록") List<Long> categoryIds,
        @Schema(description = "처리 상태") ExpoOpeningRequestStatus status,
        @Schema(description = "심사 요청 시각") Instant submittedAt,
        @Schema(description = "심사한 관리자 ID") Long reviewedByAdminId,
        @Schema(description = "심사 시각") Instant reviewedAt,
        @Schema(description = "반려 사유") String rejectionReason,
        @Schema(description = "승인으로 만들어진 박람회 ID") Long createdExpoId,
        @Schema(description = "신청 생성 시각") Instant createdAt) {}
