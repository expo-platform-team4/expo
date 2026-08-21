package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 마이페이지 "나의 티켓" 화면의 티켓 한 장 (A-API-021, A-API-022). */
@Schema(description = "내 티켓")
public record MemberTicketResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "발권 티켓 ID") Long issuedTicketId,
        @Schema(description = "티켓 코드") String ticketCode,
        @Schema(description = "티켓 상태 (ISSUED/CHECKED_IN/CANCELED/INVALIDATED)") String status,
        @Schema(description = "입장 시각 (미입장이면 null)") Instant checkedInAt,
        @Schema(description = "QR 원문. 입장에 쓸 수 없는 티켓이면 null") String qrPayload,
        @Schema(description = "이 주문 항목의 구매 수량") int orderItemQuantity) {}
