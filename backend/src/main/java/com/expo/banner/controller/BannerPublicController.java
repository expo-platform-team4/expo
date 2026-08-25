package com.expo.banner.controller;

import com.expo.banner.dto.ActiveBannerResponse;
import com.expo.banner.service.BannerApplicationService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 배너 조회 API (비로그인 허용) — B-API-024.
 *
 * <p>경로 {@code /api/banners}. SecurityConfig 가 {@code GET /api/banners/active} 를 permitAll 로
 * 연다 — 접두어 전체가 아니라 경로 하나다. {@code /api/expos} 와 같은 방식으로, 이 아래에 관리자용
 * 조회가 생겨도 저절로 공개되지 않게 하기 위해서다.
 *
 * <p>이 문단이 한동안 "permitAll 로 연다" 고만 적어 두고 실제 규칙은 없어서, 비로그인 요청이
 * {@code anyRequest().authenticated()} 에 걸려 401 이 났다. 문서가 코드를 앞질러 있었다.
 */
@Tag(name = "Banner - Public", description = "공개 배너 조회 API")
@RestController
@RequestMapping("/api/banners")
public class BannerPublicController {

    private final BannerApplicationService bannerApplicationService;

    public BannerPublicController(BannerApplicationService bannerApplicationService) {
        this.bannerApplicationService = bannerApplicationService;
    }

    /** B-API-024 — 현재 노출 가능한 메인 배너 최대 5개 조회 (슬롯 max_active_count 기준). */
    @Operation(summary = "활성 메인 배너 조회", description = "지금 노출 중인 메인 배너를 신청 순으로 조회합니다.")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ActiveBannerResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok(bannerApplicationService.getActiveBanners()));
    }
}
