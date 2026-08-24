package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.PublicExpoCardResponse;
import com.expo.expo.dto.PublicExpoDetailResponse;
import com.expo.expo.dto.PublicExpoQuery;
import com.expo.expo.service.PublicExpoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈·목록·상세에서 쓰는 공개 박람회 조회 (이슈 #107).
 *
 * <p>비회원도 부를 수 있어야 한다. {@code /api/expos} 는 SecurityConfig 의 {@code anyRequest().permitAll()} 에
 * 걸려 인증을 요구하지 않는다 — 같은 자리에 {@code /api/expos/{id}/ticket-products/purchasable} 가 이미 있다.
 */
@Tag(name = "Public Expo", description = "공개 박람회 목록·상세")
@RestController
@RequestMapping("/api/expos")
@RequiredArgsConstructor
public class PublicExpoController {

    private final PublicExpoService publicExpoService;

    @Operation(summary = "박람회 목록", description = "공개된 박람회만 나온다. 지역·검색어로 거르고 행사일순·인기순으로 정렬한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicExpoCardResponse>>> list(
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        publicExpoService.listCards(
                                new PublicExpoQuery(regionCode, keyword, sort))));
    }

    @Operation(summary = "박람회 상세", description = "소개글과 이미지·자료 목록을 함께 준다.")
    @GetMapping("/{expoId}")
    public ResponseEntity<ApiResponse<PublicExpoDetailResponse>> getDetail(
            @PathVariable Long expoId) {
        return ResponseEntity.ok(ApiResponse.ok(publicExpoService.getDetail(expoId)));
    }
}
