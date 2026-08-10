package com.expo.venue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 가상 장소 등록 요청. */
@Schema(description = "가상 장소 등록 요청")
public record CreateVirtualVenueRequest(
        @Schema(description = "장소명", example = "코엑스")
                @NotBlank(message = "장소명은 필수입니다.")
                @Size(max = 150, message = "장소명은 150자 이하여야 합니다.")
                String name,
        @Schema(description = "주소", example = "서울특별시 강남구 영동대로 513")
                @NotBlank(message = "주소는 필수입니다.")
                @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
                String address,
        @Schema(description = "검색용 지역 코드", example = "SEOUL")
                @NotBlank(message = "지역 코드는 필수입니다.")
                @Size(max = 30, message = "지역 코드는 30자 이하여야 합니다.")
                String regionCode,
        @Schema(description = "장소 설명") String description,
        @Schema(description = "전체 배치도 파일 ID") Long mapFileId) {}
