package com.expo.expo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 클라이언트 본인 등록 박람회 목록 응답 ({@code v_client_dashboard_expos} 뷰 기반). */
@Schema(description = "클라이언트 마이페이지 - 등록 박람회")
public record ClientDashboardExpoResponse(
        @Schema(description = "클라이언트 사용자 ID") Long clientUserId,
        @Schema(description = "박람회 ID") Long expoId,
        @Schema(description = "박람회명") String title,
        @Schema(description = "행사 시작 시각") Instant eventStartAt,
        @Schema(description = "행사 종료 시각") Instant eventEndAt,
        @Schema(description = "판매 시작 시각") Instant salesStartAt,
        @Schema(description = "판매 종료 시각") Instant salesEndAt,
        @Schema(description = "심사 상태") String reviewStatus,
        @Schema(description = "공개 상태") String visibilityStatus,
        @Schema(description = "행사 진행 상태") String eventStatus,
        @Schema(description = "승인 시각") Instant approvedAt,
        @Schema(description = "등록된 티켓 상품 수") long ticketProductCount) {}
