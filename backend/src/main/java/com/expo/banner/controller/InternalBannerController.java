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
 * <h2>스케줄러는 아직 없다</h2>
 *
 * {@code InternalSettlementController}·{@code InternalBoothOrderController} 와 같은 이유다 —
 * 인스턴스가 여럿일 때 중복 실행을 막는 장치가 함께 필요해서 미뤘다. 여러 번 불러도 안전하도록
 * 만들어 두었으므로 나중에 붙이기만 하면 된다.
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
