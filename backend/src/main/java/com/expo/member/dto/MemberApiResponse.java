package com.expo.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "member API 응답")
public record MemberApiResponse<T>(
        @Schema(description = "성공 여부") boolean success,
        @Schema(description = "응답 데이터") T data,
        @Schema(description = "오류 메시지") String message) {

    public static <T> MemberApiResponse<T> ok(T data) {
        return new MemberApiResponse<>(true, data, null);
    }

    public static MemberApiResponse<Void> fail(String message) {
        return new MemberApiResponse<>(false, null, message);
    }
}
