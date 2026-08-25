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
 * <h2>스케줄러가 이것을 부른다</h2>
 *
 * {@code BoothOrderExpirationScheduler} 가 주기적으로 같은 작업을 실행한다. {@code ScheduledJobRunner} 가
 * PostgreSQL 어드바이저리 락으로 <b>인스턴스 하나에서만</b> 돌게 막는다.
 *
 * <p>그래도 이 API 는 남겨 둔다. 다음 주기를 기다리지 않고 <b>지금 당장 반영해야 할 때</b>와,
 * 스케줄러가 도는지 의심스러울 때 손으로 확인할 수단이 필요하다.
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
