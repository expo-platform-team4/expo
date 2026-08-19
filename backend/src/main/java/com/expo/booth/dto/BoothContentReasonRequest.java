package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 사유가 필요 없는 운영 처리(숨김·숨김 해제) 요청. */
@Schema(description = "부스 콘텐츠 운영 처리 요청")
public record BoothContentReasonRequest(@Schema(description = "사유") String reason) {}
