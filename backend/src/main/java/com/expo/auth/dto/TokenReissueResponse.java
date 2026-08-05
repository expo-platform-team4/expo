package com.expo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access Token 재발급 응답")
public record TokenReissueResponse(
    @Schema(description = "새 JWT Access Token") String accessToken,
    @Schema(description = "Access Token 유효 시간(분)", example = "30")
        int accessTokenExpiresInMinutes) {}
