package com.expo.participation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 참여 신청서 관리자 메모 갱신 요청. */
@Schema(description = "관리자 메모 갱신 요청")
public record UpdateMemoRequest(
        @Schema(description = "관리자 메모") @NotBlank(message = "메모는 필수입니다.") String memo) {}
