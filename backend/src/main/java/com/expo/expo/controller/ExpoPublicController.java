package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.ExpoDto.ExpoCardResponse;
import com.expo.expo.dto.ExpoDto.ExpoDetailResponse;
import com.expo.expo.dto.ExpoDto.SearchCondition;
import com.expo.expo.dto.ExpoDto.SearchCondition.Sort;
import com.expo.expo.entity.ExpoEnums.SaleStatus;
import com.expo.expo.service.ExpoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 박람회 조회 API (비로그인 허용) — API 명세 No.10~11.
 *
 * <p>경로 {@code /api/expos}. SecurityConfig 에서 {@code GET /api/expos/**} 를 permitAll 로 연다.
 * 검색 파라미터는 checkstyle ParameterNumber 규칙에 맞춰 {@link SearchCondition} 하나로 묶는다.
 *
 * <p>No.12(파일 조회) · No.13(외부 링크 조회) 은 다음 PR 범위이며, 현재 상세 응답
 * ({@link ExpoDetailResponse}) 에 파일·링크가 포함되어 있어 임시로 대체 가능하다.
 */
@Tag(name = "Expo - Public", description = "공개 박람회 조회 API")
@RestController
@RequestMapping("/api/expos")
public class ExpoPublicController {

    private final ExpoService expoService;

    public ExpoPublicController(ExpoService expoService) {
        this.expoService = expoService;
    }

    /** No.10 — 공개 박람회 검색·필터·정렬·페이지 조회 (UC-06, 희-SRCH-01~13). */
    @Operation(summary = "공개 박람회 검색", description = "승인·공개된 박람회를 검색·필터·정렬합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpoCardResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate toDate,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) SaleStatus saleStatus,
            @RequestParam(required = false) Sort sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        SearchCondition condition =
                new SearchCondition(
                        keyword,
                        categoryId,
                        regionCode,
                        fromDate,
                        toDate,
                        minPrice,
                        maxPrice,
                        saleStatus,
                        sort);
        return ResponseEntity.ok(ApiResponse.ok(expoService.search(condition, page, size)));
    }

    /** No.11 — 공개 박람회 상세 조회 (UC-06). */
    @Operation(summary = "공개 박람회 상세", description = "판매 상태·이미지·자료·링크를 포함해 조회합니다.")
    @GetMapping("/{expoId}")
    public ResponseEntity<ApiResponse<ExpoDetailResponse>> getDetail(@PathVariable Long expoId) {
        return ResponseEntity.ok(ApiResponse.ok(expoService.getDetail(expoId)));
    }
}
