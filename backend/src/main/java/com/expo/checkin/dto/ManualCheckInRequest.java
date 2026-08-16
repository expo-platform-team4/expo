package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 티켓 코드 수동 입력 체크인 요청. QR 이 안 찍힐 때 쓴다.
 *
 * @param ticketCode 관람객이 화면에서 읽어 주는 코드
 */
@Schema(description = "티켓 코드 수동 체크인 요청")
public record ManualCheckInRequest(
        @Schema(description = "티켓 코드", example = "EXPO-20260810-000004")
                @NotBlank(message = "티켓 코드가 비어 있습니다.")
                String ticketCode) {}
