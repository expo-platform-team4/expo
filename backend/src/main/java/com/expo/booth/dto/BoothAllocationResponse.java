package com.expo.booth.dto;

import com.expo.booth.entity.BoothAllocationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 부스 확정 배정 응답. */
@Schema(description = "부스 확정 배정")
public record BoothAllocationResponse(
        @Schema(description = "배정 ID") Long id,
        @Schema(description = "참여 신청서 ID") Long applicationId,
        @Schema(description = "주문 ID") Long boothOrderId,
        @Schema(description = "부스 상품 ID") Long boothProductId,
        @Schema(description = "배정받은 클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "배정 일시") Instant allocatedAt,
        @Schema(description = "배정 상태") BoothAllocationStatus status,
        @Schema(description = "취소 일시") Instant canceledAt,
        @Schema(description = "취소 사유") String cancelReason,
        @Schema(description = "생성 일시") Instant createdAt,
        @Schema(description = "수정 일시") Instant updatedAt) {}
