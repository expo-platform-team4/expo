package com.expo.banner.controller;

import com.expo.banner.dto.BannerDisplaySyncResult;
import com.expo.banner.service.BannerDisplayStatusService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 배치·운영 호출.
 *
 * <h2>인가를 반드시 명시해야 하는 경로다</h2>
 *
 * {@code /api/internal/**} 는 {@code SecurityConfig} 에서 {@code hasRole("ADMIN")} 이다.
 *
 * <h2>스케줄러가 이것을 부른다</h2>
 *
 * {@code BannerDisplayStatusScheduler} 가 주기적으로 같은 작업을 실행한다. {@code ScheduledJobRunner} 가
 * PostgreSQL 어드바이저리 락으로 <b>인스턴스 하나에서만</b> 돌게 막는다.
 *
 * <p>그래도 이 API 는 남겨 둔다. 다음 주기를 기다리지 않고 <b>지금 당장 반영해야 할 때</b>와,
 * 스케줄러가 도는지 의심스러울 때 손으로 확인할 수단이 필요하다.
 */
@Tag(name = "Internal Banner", description = "배너 노출 상태 정리 (내부 호출)")
@RestController
@RequestMapping("/api/internal/banners")
public class InternalBannerController {

    private final BannerDisplayStatusService bannerDisplayStatusService;

    public InternalBannerController(BannerDisplayStatusService bannerDisplayStatusService) {
        this.bannerDisplayStatusService = bannerDisplayStatusService;
    }

    @Operation(
            summary = "배너 노출 상태 정리",
            description =
                    "시작 시각이 지난 예약 배너를 노출 상태로 바꾸고, 종료 시각이 지난 배너를 끝냅니다. "
                            + "이 호출이 없으면 예약된 배너가 시작일이 지나도 영영 노출되지 않습니다. "
                            + "여러 번 호출해도 이미 정리된 배너는 다시 처리하지 않습니다.")
    @PostMapping("/sync-display-status")
    public ResponseEntity<ApiResponse<BannerDisplaySyncResult>> syncDisplayStatus() {
        return ResponseEntity.ok(ApiResponse.ok(bannerDisplayStatusService.sync()));
    }
}
