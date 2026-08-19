package com.expo.member.controller;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.MemberProfileResponse;
import com.expo.member.dto.NicknameChangeRequest;
import com.expo.member.service.MemberProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 일반 회원·클라이언트 공통 마이페이지 프로필 API (A-API-015, A-API-016). */
@Tag(name = "Member Profile", description = "마이페이지 프로필 API")
@RestController
@RequestMapping("/api/users/me")
public class MemberProfileController {

    private final MemberProfileService memberProfileService;

    public MemberProfileController(MemberProfileService memberProfileService) {
        this.memberProfileService = memberProfileService;
    }

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자(MEMBER, CLIENT) 본인의 프로필을 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMyProfile(
            @AuthenticationPrincipal AuthPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(
                ApiResponse.ok(memberProfileService.getMyProfile(principal.getMemberId())));
    }

    @Operation(
            summary = "닉네임 변경",
            description =
                    """
          로그인한 사용자 본인의 닉네임을 변경합니다.

          - 형식·중복 검증은 이메일·닉네임 중복확인(A-API-004)과 동일한 규칙을 씁니다.
          - 기존과 같은 닉네임을 다시 보내면 중복 오류 없이 그대로 처리됩니다.
          """)
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> changeNickname(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody NicknameChangeRequest request) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        memberProfileService.changeNickname(
                                principal.getMemberId(), request.nickname())));
    }

    private void requireAuthenticated(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
