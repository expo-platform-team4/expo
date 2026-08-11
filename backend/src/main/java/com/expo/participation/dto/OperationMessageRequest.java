package com.expo.participation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 메모가 필요 없는 운영 처리(확인·보완 완료) 요청. */
@Schema(description = "운영 처리 요청")
public record OperationMessageRequest(@Schema(description = "메모") String message) {}
