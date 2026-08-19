package com.expo.participation.controller;

import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import com.expo.participation.dto.CreateParticipationApplicationRequest;
import com.expo.participation.dto.ParticipationApplicationResponse;
import com.expo.participation.service.ParticipationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여 기업의 참여 신청서 작성·조회. */
@Tag(name = "Client Participation Application", description = "참여 기업 신청서 작성·조회")
@RestController
@RequestMapping("/api/client/participation-applications")
public class ParticipationApplicationController {

    private final ParticipationApplicationService participationApplicationService;

    public ParticipationApplicationController(
            ParticipationApplicationService participationApplicationService) {
        this.participationApplicationService = participationApplicationService;
    }

    @Operation(summary = "참여 신청서 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<ParticipationApplicationResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateParticipationApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                participationApplicationService.create(
                                        principal.getMemberId(), request)));
    }

    @Operation(summary = "내 참여 신청서 상세 조회")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ParticipationApplicationResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long applicationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        participationApplicationService.getMine(
                                applicationId, principal.getMemberId())));
    }
}
