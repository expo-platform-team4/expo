package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 체크인 이력 한 줄. 성공만이 아니라 <b>거절도 나온다</b> — "안 들여보내 줬다" 는 항의를 가릴 근거가 이것이다.
 *
 * <p>위조·미등록 QR({@code INVALID_TOKEN})도 나온다(이슈 #73). 그 행은 가리킬 티켓이 없어 {@code
 * issuedTicketId} 와 {@code ticketCode} 가 <b>비어 있다</b> — 화면이 대체 문구를 넣어야 한다.
 *
 * @param issuedTicketId {@code INVALID_TOKEN} 이면 {@code null}
 * @param ticketCode {@code INVALID_TOKEN} 이면 {@code null}
 * @param result {@code SUCCESS} / {@code ALREADY_USED} / {@code CANCELED_TICKET} / {@code
 *     WRONG_EXPO} / {@code INVALID_TOKEN}
 * @param detail 거절 사유의 사람이 읽는 부연. 성공과 위조 시도는 {@code null}
 */
@Schema(description = "체크인 이력")
public record CheckInHistoryRow(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "입장권 ID") Long issuedTicketId,
        @Schema(description = "티켓 코드", example = "EXPO-20260810-000004") String ticketCode,
        @Schema(description = "QR / MANUAL_CODE", example = "QR") String method,
        @Schema(description = "판정", example = "SUCCESS") String result,
        @Schema(description = "처리 시각") Instant checkedAt,
        @Schema(description = "처리한 클라이언트 ID") Long processedByClientId,
        @Schema(description = "거절 사유 부연") String detail) {}
