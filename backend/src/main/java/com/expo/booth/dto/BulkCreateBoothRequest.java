package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 부스 공간 일괄 등록 요청. */
@Schema(description = "부스 공간 일괄 등록 요청")
public record BulkCreateBoothRequest(
        @Schema(description = "등록할 부스 목록") @NotEmpty(message = "등록할 부스가 1개 이상이어야 합니다.")
                List<@NotNull @Valid CreateBoothRequest> booths) {}
