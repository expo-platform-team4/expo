package com.expo.member.controller;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.MemberWithdrawalRequest;
import com.expo.member.service.MemberWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 일반 회원 마이페이지 "회원 탈퇴" API (A-API-018). */
@Tag(name = "Member Withdrawal", description = "회원 탈퇴 API")
@RestController
@RequestMapping("/api/users/me")
public class MemberWithdrawalController {

    private final MemberWithdrawalService memberWithdrawalService;

    public MemberWithdrawalController(MemberWithdrawalService memberWithdrawalService) {
        this.memberWithdrawalService = memberWithdrawalService;
    }

    @Operation(
            summary = "회원 탈퇴",
            description =
                    "현재 비밀번호로 본인 확인 후 탈퇴 처리합니다. 계정은 소프트 삭제되며(users.account_status),"
                            + " 주문·티켓 등 다른 기록은 그대로 남습니다. 성공하면 모든 Refresh Token 이 폐기됩니다.")
    @PostMapping("/withdrawal")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MemberWithdrawalRequest request) {
        requireAuthenticated(principal);
        memberWithdrawalService.withdraw(principal.getMemberId(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void requireAuthenticated(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
