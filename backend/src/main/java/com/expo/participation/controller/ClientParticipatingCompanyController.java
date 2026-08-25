package com.expo.participation.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.participation.dto.ClientParticipatingCompanyResponse;
import com.expo.participation.service.ClientParticipatingCompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주최사가 자기 박람회의 참여 기업 목록을 본다. */
@Tag(name = "Client Participating Company", description = "주최사 참여 기업 목록 조회")
@RestController
@RequestMapping("/api/client/expos/{expoId}/participating-companies")
@RequiredArgsConstructor
public class ClientParticipatingCompanyController {

    private final ClientParticipatingCompanyService clientParticipatingCompanyService;

    @Operation(summary = "참여 기업 목록 조회", description = "박람회 주최사 본인만 조회할 수 있다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientParticipatingCompanyResponse>>> list(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long expoId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientParticipatingCompanyService.list(expoId, principal.getMemberId())));
    }
}
