package com.expo.settlement.controller;

import com.expo.common.response.ApiResponse;
import com.expo.settlement.dto.SettlementGenerationResult;
import com.expo.settlement.service.SettlementGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 배치·운영 호출 (D-API-017).
 *
 * <h2>인가를 반드시 명시해야 하는 경로다</h2>
 *
 * {@code SecurityConfig} 의 마지막 규칙이 {@code anyRequest().permitAll()} 이다. 즉 규칙을 안 적으면
 * <b>누구나 부를 수 있는 경로가 된다.</b> 정산 대상을 만드는 API 라 {@code /api/internal/**} 에
 * {@code hasRole("ADMIN")} 을 명시해 두었다.
 *
 * <h2>스케줄러가 이것을 부른다</h2>
 *
 * 명세는 "행사 종료 후 7~14일 이내 대상 생성" 이다. {@code SettlementGenerationScheduler} 가
 * 매일 새벽에 실행한다 — 대상 조건이 "행사 종료 후 7일" 이라 분 단위로 볼 이유가 없는 유일한
 * 작업이다. {@code ScheduledJobRunner} 가 인스턴스 하나에서만 돌게 막는다.
 *
 * <p>그래도 이 API 는 남겨 둔다. 다음 주기를 기다리지 않고 <b>지금 당장 반영해야 할 때</b>와,
 * 스케줄러가 도는지 의심스러울 때 손으로 확인할 수단이 필요하다.
 */
@Tag(name = "Internal Settlement", description = "정산 대상 생성 (내부 호출)")
@RestController
@RequestMapping("/api/internal/settlements")
public class InternalSettlementController {

    private final SettlementGenerationService settlementGenerationService;

    public InternalSettlementController(SettlementGenerationService settlementGenerationService) {
        this.settlementGenerationService = settlementGenerationService;
    }

    @Operation(
            summary = "정산 대상 생성",
            description =
                    "행사가 끝난 지 7일이 지났고 아직 정산이 없는 박람회의 정산을 만듭니다. "
                            + "취소된 박람회와 미승인 박람회는 제외합니다. "
                            + "여러 번 호출해도 이미 만들어진 정산은 다시 만들지 않습니다. "
                            + "금액은 0으로 시작하며 재계산 API가 채웁니다.")
    @PostMapping("/generate-due")
    public ResponseEntity<ApiResponse<SettlementGenerationResult>> generateDue() {
        return ResponseEntity.ok(ApiResponse.ok(settlementGenerationService.generateDue()));
    }
}
