package com.expo.member.controller;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.MemberOrderResponse;
import com.expo.member.service.MemberOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 일반 회원 마이페이지 "예매 내역" API (A-API-019, A-API-020). */
@Tag(name = "Member Order", description = "내 주문 조회 API")
@RestController
@RequestMapping("/api/users/me")
public class MemberOrderController {

    private final MemberOrderService memberOrderService;

    public MemberOrderController(MemberOrderService memberOrderService) {
        this.memberOrderService = memberOrderService;
    }

    @Operation(
            summary = "내 주문 목록/상세 조회",
            description =
                    "로그인한 회원 본인의 주문을 최신순으로 조회합니다. 목록·상세를 나누지 않으며, "
                            + "각 항목이 결제·환불 최신 상태와 환불 가능 여부까지 담고 있습니다.")
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<MemberOrderResponse>>> getMyOrders(
            @AuthenticationPrincipal AuthPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(
                ApiResponse.ok(memberOrderService.getMyOrders(principal.getMemberId())));
    }

    private void requireAuthenticated(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
