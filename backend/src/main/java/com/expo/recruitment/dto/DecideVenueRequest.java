package com.expo.recruitment.dto;

import com.expo.recruitment.entity.VenueDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 모집공고 생성 요청의 장소 충돌 판정 요청. */
@Schema(description = "장소 충돌 판정 요청")
public record DecideVenueRequest(
        @Schema(description = "장소 결정. ALLOWED 또는 CANCELED 만 허용") @NotNull(message = "장소 결정은 필수입니다.")
                VenueDecision decision,
        @Schema(description = "판정 사유") String reason) {}
