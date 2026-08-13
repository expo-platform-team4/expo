package com.expo.admin.controller;

import com.expo.admin.dto.AdminUserDetailResponse;
import com.expo.admin.dto.AdminUserSummaryResponse;
import com.expo.admin.service.AdminUserService;
import com.expo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 회원·클라이언트 계정 조회 (E-API-012, E-API-013). */
@Tag(name = "Admin User", description = "관리자 계정 조회")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(
            summary = "회원·클라이언트 계정 검색·목록 조회",
            description = "이메일·닉네임·회사명으로 검색하고, 역할·계정 상태로 필터링합니다. 파라미터를 안 주면 전체 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserSummaryResponse>>> searchUsers(
            @Parameter(description = "이메일·닉네임·회사명 검색어") @RequestParam(required = false)
                    String keyword,
            @Parameter(description = "역할 필터 (MEMBER/CLIENT/ADMIN)") @RequestParam(required = false)
                    String role,
            @Parameter(description = "계정 상태 필터") @RequestParam(required = false)
                    String accountStatus) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminUserService.searchUsers(keyword, role, accountStatus)));
    }

    @Operation(summary = "계정·사업자 프로필·상태 상세 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getUser(userId)));
    }
}
