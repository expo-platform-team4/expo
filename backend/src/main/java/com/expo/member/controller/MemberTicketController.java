package com.expo.member.controller;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.MemberTicketGroupResponse;
import com.expo.member.service.MemberTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 일반 회원 마이페이지 "나의 티켓" API (A-API-021, A-API-022). */
@Tag(name = "Member Ticket", description = "내 티켓·QR 조회 API")
@RestController
@RequestMapping("/api/users/me")
public class MemberTicketController {

    private final MemberTicketService memberTicketService;

    public MemberTicketController(MemberTicketService memberTicketService) {
        this.memberTicketService = memberTicketService;
    }

    @Operation(
            summary = "내 티켓·QR 목록 조회",
            description =
                    "로그인한 회원 본인의 발권 티켓을 박람회별로 묶어 조회합니다. "
                            + "입장에 쓸 수 있는 티켓(ISSUED/CHECKED_IN)만 QR 원문을 포함합니다.")
    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<MemberTicketGroupResponse>>> getMyTickets(
            @AuthenticationPrincipal AuthPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(
                ApiResponse.ok(memberTicketService.getMyTickets(principal.getMemberId())));
    }

    private void requireAuthenticated(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
