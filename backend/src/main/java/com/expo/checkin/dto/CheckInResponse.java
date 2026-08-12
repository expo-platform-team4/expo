package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 체크인 시도 결과.
 *
 * <p><b>거절도 HTTP 200 으로 돌려준다.</b> 현장 스캐너 화면은 "이미 입장한 표입니다" 를 크게 띄워야 하는데, 4xx 로 내리면 프런트가 에러 처리
 * 경로로 빠져 그 구분을 잃는다. 스캔이 처리된 이상 요청 자체는 성공이고, 통과 여부는 {@code result} 로 말한다.
 *
 * <p>주최자가 아니거나 박람회가 없는 경우는 이와 다르다. 그건 요청 자체가 잘못된 것이라 예외로 처리한다(403·404).
 *
 * @param result 판정. {@code SUCCESS} 외에는 입장 거절이다
 * @param admitted 들여보내도 되는가. {@code result == SUCCESS} 와 같지만 화면이 분기하기 쉬우라고 따로 준다
 * @param issuedTicketId 티켓을 찾은 경우에만. 위조 QR 이면 {@code null}
 * @param ticketCode 찾은 티켓의 코드. 위조 QR 이면 {@code null}
 * @param checkedInAt 입장 처리 시각. 이번에 통과했으면 지금, 이미 입장한 표면 <b>먼저 입장한 시각</b>이다
 * @param message 현장 운영자에게 그대로 보여줄 수 있는 문구
 */
@Schema(description = "체크인 결과")
public record CheckInResponse(
        @Schema(
                        description =
                                "SUCCESS / ALREADY_USED / CANCELED_TICKET / WRONG_EXPO /"
                                        + " INVALID_TOKEN",
                        example = "SUCCESS")
                String result,
        @Schema(description = "입장 허용 여부", example = "true") boolean admitted,
        @Schema(description = "입장권 ID") Long issuedTicketId,
        @Schema(description = "티켓 코드", example = "EXPO-20260810-000004") String ticketCode,
        @Schema(description = "입장 시각. 이미 입장한 표면 먼저 입장한 시각이다") Instant checkedInAt,
        @Schema(description = "운영자에게 보여줄 문구", example = "입장 처리되었습니다.") String message) {}
