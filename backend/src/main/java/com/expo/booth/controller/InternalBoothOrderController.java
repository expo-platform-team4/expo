package com.expo.booth.controller;

import com.expo.booth.dto.BoothOrderExpirationResult;
import com.expo.booth.service.BoothOrderService;
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
 * {@code SecurityConfig} 의 마지막 규칙이 {@code anyRequest().permitAll()} 이다. {@code
 * /api/internal/**} 에 {@code hasRole("ADMIN")} 을 명시해 두었다.
 *
 * <h2>스케줄러는 아직 없다</h2>
 *
 * {@code InternalSettlementController} 와 같은 이유로 {@code @Scheduled} 를 붙이지 않았다 — 인스턴스가
 * 여럿일 때 중복 실행을 막는 장치가 함께 필요해서 미뤘다. 여러 번 불러도 안전하도록 만들어 두었으므로 나중에 붙이기만 하면 된다.
 */
@Tag(name = "Internal Booth Order", description = "부스 주문 만료 정리 (내부 호출)")
@RestController
@RequestMapping("/api/internal/booth-orders")
public class InternalBoothOrderController {

    private final BoothOrderService boothOrderService;

    public InternalBoothOrderController(BoothOrderService boothOrderService) {
        this.boothOrderService = boothOrderService;
    }

    @Operation(
            summary = "만료 주문 일괄 정리",
            description =
                    "결제 대기 시간을 넘긴 주문을 만료 처리하고, 임시 확보한 부스 상품과 신청서 상태를 되돌립니다. "
                            + "여러 번 호출해도 이미 정리된 주문은 다시 처리하지 않습니다.")
    @PostMapping("/expire-due")
    public ResponseEntity<ApiResponse<BoothOrderExpirationResult>> expireDue() {
        return ResponseEntity.ok(ApiResponse.ok(boothOrderService.expireDue()));
    }
}
