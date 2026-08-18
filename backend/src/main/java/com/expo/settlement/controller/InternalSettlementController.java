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
 * <h2>스케줄러는 아직 없다</h2>
 *
 * 명세는 "행사 종료 후 7~14일 이내 대상 생성" 이지만, 지금은 <b>사람이 부르는 API</b> 만 있다.
 * {@code @Scheduled} 를 붙이려면 인스턴스가 여럿일 때 중복 실행을 막는 장치가 함께 필요해서 미뤘다.
 * 여러 번 불러도 안전하도록 만들어 두었으므로 나중에 붙이기만 하면 된다.
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
