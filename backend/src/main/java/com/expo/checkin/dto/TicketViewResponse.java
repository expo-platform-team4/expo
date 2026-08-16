package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * SMS 링크로 연 티켓 조회 응답.
 *
 * <p>주문 단위 링크라 티켓이 여러 장 나온다. 4매를 샀으면 4개다.
 */
@Schema(description = "티켓 조회 응답")
public record TicketViewResponse(
        @Schema(description = "주문번호", example = "ORD-20260810-0001") String orderNumber,
        @Schema(description = "발권 매수", example = "2") int ticketCount,
        @Schema(description = "입장권 목록") List<Ticket> tickets) {

    /** 입장권 한 장. */
    @Schema(description = "입장권")
    public record Ticket(
            @Schema(description = "입장권 ID", example = "11") Long issuedTicketId,
            @Schema(description = "티켓 코드. QR 이 안 찍힐 때 손으로 입력한다", example = "EXPO-20260810-000004")
                    String ticketCode,
            @Schema(
                            description =
                                    "QR 원문. 이 문자열을 그대로 QR 이미지로 만들면 된다. "
                                            + "DB 에 저장돼 있지 않고 조회할 때마다 다시 계산되며, 같은 티켓이면 언제나 같은 값이다",
                            example = "v1.NJCEnwM1vLsQ22h8XAmbwEK_f5kJgIMztnK3XwkhIcY")
                    String qrPayload,
            @Schema(
                            description = "ISSUED / CHECKED_IN / CANCELED / INVALIDATED",
                            example = "ISSUED")
                    String status,
            @Schema(description = "입장 시각. 아직 입장 전이면 null") Instant checkedInAt,
            @Schema(description = "박람회명", example = "2026 서울 국제 도서전") String expoTitle,
            @Schema(description = "행사 시작 시각") Instant expoStartAt,
            @Schema(description = "행사 종료 시각") Instant expoEndAt) {}
}
