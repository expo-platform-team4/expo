package com.expo.participation.dto;

import com.expo.participation.entity.ParticipationApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 관리자용 참여 신청서 응답. 운영 확인 정보를 포함한다. */
@Schema(description = "관리자용 참여 신청서")
public record AdminParticipationApplicationResponse(
        @Schema(description = "신청서 ID") Long id,
        @Schema(description = "기업 모집공고 ID") Long recruitmentNoticeId,
        @Schema(description = "신청 기업 사용자 ID") Long clientUserId,
        @Schema(description = "참가 기업명 스냅샷") String companyNameSnapshot,
        @Schema(description = "참여 목적") String participationPurpose,
        @Schema(description = "전시 품목 설명") String exhibitDescription,
        @Schema(description = "선택한 부스 상품 ID") Long selectedBoothProductId,
        @Schema(description = "부스 주문 ID") Long boothOrderId,
        @Schema(description = "처리 상태") ParticipationApplicationStatus status,
        @Schema(description = "제출 일시") LocalDateTime submittedAt,
        @Schema(description = "관리자 확인 일시") LocalDateTime adminCheckedAt,
        @Schema(description = "확인한 관리자 ID") Long adminCheckedBy,
        @Schema(description = "관리자 메모") String adminMemo,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt) {}
