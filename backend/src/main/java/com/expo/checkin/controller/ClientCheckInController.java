package com.expo.checkin.controller;

import com.expo.checkin.dto.CheckInHistoryPage;
import com.expo.checkin.dto.CheckInResponse;
import com.expo.checkin.dto.CheckInSummaryResponse;
import com.expo.checkin.dto.ManualCheckInRequest;
import com.expo.checkin.dto.QrCheckInRequest;
import com.expo.checkin.service.CheckInQueryService;
import com.expo.checkin.service.CheckInService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
 * 주최사가 자기 박람회 입장객을 체크인한다.
 *
 * <p><b>경로가 {@code /api/client/} 인 것만으로는 부족하다.</b> 주최사와 참여 기업이 같은 {@code CLIENT} 역할을 쓰기 때문에,
 * 역할 검사만으로는 남의 박람회를 체크인하는 것을 막지 못한다. 실제 방어는 서비스의 주최자 검증이다.
 *
 * <p>입장 거절({@code ALREADY_USED} 등)은 4xx 가 아니라 <b>200 에 결과를 담아</b> 돌려준다. 이유는 {@link CheckInResponse}
 * 에 적어 뒀다.
 */
@Tag(name = "Check-in", description = "현장 입장 처리·현황 (주최사 전용)")
@RestController
@RequestMapping("/api/client/expos/{expoId}")
public class ClientCheckInController {

    private final CheckInService checkInService;
    private final CheckInQueryService checkInQueryService;

    public ClientCheckInController(
            CheckInService checkInService, CheckInQueryService checkInQueryService) {
        this.checkInService = checkInService;
        this.checkInQueryService = checkInQueryService;
    }

    @Operation(summary = "체크인 현황 조회", description = "유효 발권·입장 완료·미입장·취소 매수를 집계한다.")
    @GetMapping("/check-in")
    public ResponseEntity<ApiResponse<CheckInSummaryResponse>> summary(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long expoId) {
        return ResponseEntity.ok(
                ApiResponse.ok(checkInQueryService.summary(expoId, principal.getMemberId())));
    }

    @Operation(summary = "체크인 이력 조회", description = "최신순. 성공만이 아니라 거절 이력도 함께 나온다.")
    @GetMapping("/check-ins/history")
    public ResponseEntity<ApiResponse<CheckInHistoryPage>> history(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @Parameter(description = "0부터") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "최대 100") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        checkInQueryService.history(expoId, principal.getMemberId(), page, size)));
    }

    @Operation(
            summary = "QR 스캔 체크인",
            description =
                    "스캔한 QR 원문을 그대로 보내면 서버가 해시해 티켓을 찾는다. "
                            + "결과는 SUCCESS / ALREADY_USED / CANCELED_TICKET / WRONG_EXPO / INVALID_TOKEN.")
    @PostMapping("/check-ins/qr")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkInByQr(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @Valid @RequestBody QrCheckInRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        checkInService.checkInByQr(
                                expoId,
                                principal.getMemberId(),
                                request.qrPayload(),
                                clientIp(servletRequest))));
    }

    @Operation(
            summary = "티켓 코드 수동 체크인",
            description = "QR 이 안 찍힐 때 티켓 코드를 손으로 입력해 처리한다. 이력에 MANUAL_CODE 로 남는다.")
    @PostMapping("/check-ins/code")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkInByCode(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @Valid @RequestBody ManualCheckInRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        checkInService.checkInByCode(
                                expoId,
                                principal.getMemberId(),
                                request.ticketCode(),
                                clientIp(servletRequest))));
    }

    /**
     * 이력에 남길 요청 IP.
     *
     * <p>프록시 뒤에 있으면 {@code getRemoteAddr()} 이 프록시 주소가 된다. {@code X-Forwarded-For} 를 우선 보되
     * <b>클라이언트가 위조할 수 있는 헤더</b>라 감사 참고용으로만 쓴다 — 이 값으로 권한을 판단하지 않는다.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
