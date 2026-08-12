package com.expo.checkin.controller;

import com.expo.checkin.dto.TicketViewResponse;
import com.expo.checkin.service.TicketViewService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SMS 링크로 여는 티켓 조회. <b>로그인이 필요 없다.</b>
 *
 * <p>{@code /api/public/**} 는 {@code SecurityConfig} 에서 {@code permitAll} 이다. 경로만 봐도 공개라는 것이 드러나도록
 * 이 접두어를 쓴다.
 *
 * <p>인증은 쿼리 파라미터의 토큰이 대신한다. 주문번호 같은 추측 가능한 값이 아니라 256비트 난수라 URL 에 실려도 안전하다.
 */
@Tag(name = "Ticket View", description = "SMS 링크 티켓·QR 조회 (비로그인)")
@RestController
@RequestMapping("/api/public/tickets")
public class PublicTicketViewController {

    private final TicketViewService ticketViewService;

    public PublicTicketViewController(TicketViewService ticketViewService) {
        this.ticketViewService = ticketViewService;
    }

    @Operation(
            summary = "접근 토큰으로 티켓·QR 조회",
            description =
                    "SMS 로 보낸 링크의 token 값으로 그 주문의 입장권을 전부 돌려준다. "
                            + "QR 원문은 저장돼 있지 않고 조회할 때마다 다시 계산된다. "
                            + "실패는 셋으로 갈린다 — 404 없는 링크, 410 만료, 403 폐기.")
    @GetMapping
    public ResponseEntity<ApiResponse<TicketViewResponse>> viewTickets(
            @Parameter(description = "SMS 링크의 접근 토큰", required = true) @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.ok(ticketViewService.view(token)));
    }
}
