package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 닉네임 변경 요청 (A-API-016). */
@Schema(description = "닉네임 변경 요청")
public record NicknameChangeRequest(
        @Schema(description = "새 닉네임 (2자 이상 50자 이하)", example = "expo_member")
                @NotBlank(message = "닉네임은 필수입니다.")
                String nickname) {}
