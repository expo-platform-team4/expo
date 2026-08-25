package com.expo.banner.controller;

import com.expo.banner.dto.BannerApplicationCreateRequest;
import com.expo.banner.dto.BannerApplicationResponse;
import com.expo.banner.service.BannerApplicationService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 광고 배너 노출 신청 API (클라이언트 전용) — B-API-019~020.
 *
 * <p>경로 {@code /api/client/banner-requests}. 사용자 식별은 {@link AuthPrincipal#getMemberId()}
 * 에서 꺼내며 요청 파라미터로 clientId 를 받지 않는다 (ExpoOpeningRequestClientController 와 동일 컨벤션).
 */
@Tag(name = "Banner Request - Client", description = "광고 배너 노출 신청 API (클라이언트)")
@RestController
@RequestMapping("/api/client/banner-requests")
public class BannerRequestClientController {

    private final BannerApplicationService bannerApplicationService;

    public BannerRequestClientController(BannerApplicationService bannerApplicationService) {
        this.bannerApplicationService = bannerApplicationService;
    }

    /** B-API-019 — 광고 배너 노출 신청. 등록과 동시에 심사 요청(UNDER_REVIEW) 상태가 된다. */
    @Operation(summary = "배너 노출 신청", description = "배너 노출을 신청하고 즉시 심사 요청 상태로 전환합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody BannerApplicationCreateRequest request) {
        Long id = bannerApplicationService.createApplication(principal.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(id));
    }

    /**
     * 배너 신청 취소. 승인 전까지만 가능하다.
     *
     * <p>본인 것만 취소된다 — 남의 신청 ID 를 넣으면 403 이다.
     */
    @Operation(summary = "배너 신청 취소", description = "승인 전인 본인의 배너 신청을 취소합니다.")
    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        bannerApplicationService.cancelApplication(principal.getMemberId(), requestId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** B-API-020 — 내 배너 신청 목록·상태 조회. */
    @Operation(summary = "내 배너 신청 목록", description = "로그인한 클라이언트 본인의 배너 신청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BannerApplicationResponse>>> getMyList(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        bannerApplicationService.getMyApplications(
                                principal.getMemberId(), page, size)));
    }
}
