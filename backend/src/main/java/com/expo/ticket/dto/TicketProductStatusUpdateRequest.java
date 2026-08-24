package com.expo.ticket.dto;

import com.expo.ticket.entity.TicketProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 티켓 상품 판매 상태 전환 요청. */
@Schema(description = "티켓 상품 판매 상태 전환 요청")
public record TicketProductStatusUpdateRequest(
        @Schema(description = "전환할 판매 상태. ON_SALE·CANCELED 만 허용")
                @NotNull(message = "판매 상태는 필수입니다.")
                TicketProductStatus status) {}
