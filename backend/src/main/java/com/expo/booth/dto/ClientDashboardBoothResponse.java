package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 참여 신청별 부스 진행 상태 ({@code v_client_dashboard_booths} 뷰 기반). */
@Schema(description = "클라이언트 마이페이지 - 부스 진행 현황")
public record ClientDashboardBoothResponse(
        @Schema(description = "클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "모집공고 ID") Long recruitmentNoticeId,
        @Schema(description = "참여 신청 ID") Long applicationId,
        @Schema(description = "참여 신청 상태") String applicationStatus,
        @Schema(description = "부스 주문 ID") Long boothOrderId,
        @Schema(description = "부스 주문 상태") String boothOrderStatus,
        @Schema(description = "결제 상태") String paymentStatus,
        @Schema(description = "결제 완료 시각") Instant paidAt,
        @Schema(description = "부스 배정 ID") Long boothAllocationId,
        @Schema(description = "부스 배정 상태") String allocationStatus,
        @Schema(description = "부스 번호") String boothNumber) {}
