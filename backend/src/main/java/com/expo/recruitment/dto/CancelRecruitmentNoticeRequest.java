package com.expo.recruitment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 모집공고 직권 취소 요청. */
@Schema(description = "모집공고 직권 취소 요청")
public record CancelRecruitmentNoticeRequest(@Schema(description = "취소 사유") String reason) {}
