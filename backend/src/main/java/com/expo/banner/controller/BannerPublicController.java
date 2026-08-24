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
 * <p>경로 {@code /api/banners}. SecurityConfig 에서 {@code GET /api/banners/**} 를 permitAll 로 연다
 * (ExpoPublicController 와 동일 컨벤션).
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
