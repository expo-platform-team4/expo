package com.expo.member.controller;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.member.dto.ChangePasswordRequest;
import com.expo.member.dto.MemberProfileResponse;
import com.expo.member.dto.NicknameChangeRequest;
import com.expo.member.dto.ProfileImageChangeRequest;
import com.expo.member.service.MemberPasswordService;
import com.expo.member.service.MemberProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 일반 회원·클라이언트 공통 마이페이지 프로필 API (A-API-015, A-API-016, A-API-017). */
@Tag(name = "Member Profile", description = "마이페이지 프로필 API")
@RestController
@RequestMapping("/api/users/me")
public class MemberProfileController {

    private final MemberProfileService memberProfileService;
    private final MemberPasswordService memberPasswordService;

    public MemberProfileController(
            MemberProfileService memberProfileService,
            MemberPasswordService memberPasswordService) {
        this.memberProfileService = memberProfileService;
        this.memberPasswordService = memberPasswordService;
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

    @Operation(
            summary = "비밀번호 변경 (로그인 상태)",
            description =
                    "현재 비밀번호 확인 후 새 비밀번호로 교체합니다. 이메일로 재설정하는 흐름(A-API-013·014)과는 "
                            + "별개입니다 — 그건 비밀번호를 잊었을 때, 이건 로그인된 상태에서 바꿀 때 씁니다.")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        requireAuthenticated(principal);
        memberPasswordService.changePassword(
                principal.getMemberId(),
                request.currentPassword(),
                request.newPassword(),
                request.newPasswordConfirm());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(
            summary = "프로필 이미지 등록·교체",
            description =
                    """
          로그인한 사용자 본인의 프로필 이미지를 등록하거나 교체합니다.

          - 이미지 바이트는 먼저 `POST /api/files`(purpose=PROFILE_IMAGE)로 올립니다. 이 API는 그 응답의
            fileId를 받아 내 계정에 연결하기만 합니다.
          - 내가 올린 파일이 아니면 실패합니다(다른 사람 파일 도용 방지).
          """)
    @PatchMapping("/profile-image")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> changeProfileImage(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ProfileImageChangeRequest request) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(
                ApiResponse.ok(
                        memberProfileService.changeProfileImage(
                                principal.getMemberId(), request.fileId())));
    }

    @Operation(summary = "프로필 이미지 삭제", description = "등록된 프로필 이미지 연결을 지웁니다. 이후 기본 이미지로 보입니다.")
    @DeleteMapping("/profile-image")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> removeProfileImage(
            @AuthenticationPrincipal AuthPrincipal principal) {
        requireAuthenticated(principal);
        return ResponseEntity.ok(
                ApiResponse.ok(memberProfileService.removeProfileImage(principal.getMemberId())));
    }

    private void requireAuthenticated(AuthPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
