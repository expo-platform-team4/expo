package com.expo.checkin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * QR 스캔 체크인 요청.
 *
 * @param qrPayload 스캐너가 읽은 <b>문자열 그대로</b>. 가공하지 않고 보낸다 — 서버가 해시해서 대조한다
 */
@Schema(description = "QR 체크인 요청")
public record QrCheckInRequest(
        @Schema(
                        description = "스캔한 QR 원문",
                        example = "v1.NJCEnwM1vLsQ22h8XAmbwEK_f5kJgIMztnK3XwkhIcY")
                @NotBlank(message = "QR 값이 비어 있습니다.")
                String qrPayload) {}
