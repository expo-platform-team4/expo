package com.expo.participation.dto;

import com.expo.participation.entity.ParticipationApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 참여 신청서 응답. */
@Schema(description = "참여 신청서")
public record ParticipationApplicationResponse(
        @Schema(description = "신청서 ID") Long id,
        @Schema(description = "기업 모집공고 ID") Long recruitmentNoticeId,
        @Schema(description = "참가 기업명 스냅샷") String companyNameSnapshot,
        @Schema(description = "참여 목적") String participationPurpose,
        @Schema(description = "전시 품목 설명") String exhibitDescription,
        @Schema(description = "선택한 부스 상품 ID") Long selectedBoothProductId,
        @Schema(description = "부스 주문 ID") Long boothOrderId,
        @Schema(description = "처리 상태") ParticipationApplicationStatus status,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
