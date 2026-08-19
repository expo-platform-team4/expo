package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.ExpoDto.OpeningRequestCreate;
import com.expo.expo.dto.ExpoDto.OpeningRequestResponse;
import com.expo.expo.dto.ExpoDto.OpeningRequestUpdate;
import com.expo.expo.service.ExpoService;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 박람회 개최 신청 API (클라이언트 전용) — API 명세 No.1~5.
 *
 * <p>경로 {@code /api/client/expo-opening-requests}. 사용자 식별은 {@link AuthPrincipal#getMemberId()}
 * 에서 꺼내며 요청 파라미터로 clientId 를 받지 않는다. 본인 소유 여부는 서비스에서 재검증한다.
 */
@Tag(name = "Expo Opening Request - Client", description = "박람회 개최 신청 API (클라이언트)")
@RestController
@RequestMapping("/api/client/expo-opening-requests")
public class ExpoOpeningRequestClientController {

    private final ExpoService expoService;

    public ExpoOpeningRequestClientController(ExpoService expoService) {
        this.expoService = expoService;
    }

    /** No.1 — 박람회 개최 신청 임시저장·등록 (EXPO-01~02, EXPO-17~18). */
    @Operation(summary = "개최 신청 임시저장·등록", description = "개최 신청서를 DRAFT 로 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody OpeningRequestCreate request) {
        Long id = expoService.createOpeningRequest(principal.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(id));
    }

    /** No.2 — 내 개최 신청 목록 조회 (EXPO-17). */
    @Operation(summary = "내 개최 신청 목록", description = "로그인한 클라이언트 본인의 신청 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OpeningRequestResponse>>> getMyList(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoService.getMyOpeningRequests(principal.getMemberId(), page, size)));
    }

    /** No.3 — 내 개최 신청 상세·심사상태 조회 (EXPO-04, EXPO-17). */
    @Operation(summary = "내 개최 신청 상세", description = "본인 신청의 상세와 심사 상태를 조회합니다.")
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<OpeningRequestResponse>> getMyDetail(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoService.getMyOpeningRequestDetail(principal.getMemberId(), requestId)));
    }

    /** No.4 — 승인 전 개최 신청 수정 (EXPO-05). */
    @Operation(summary = "승인 전 신청 수정", description = "승인 전 상태에서만 신청서를 수정합니다.")
    @PatchMapping("/{requestId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long requestId,
            @Valid @RequestBody OpeningRequestUpdate request) {
        expoService.updateOpeningRequest(principal.getMemberId(), requestId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** No.5 — 개최 신청 심사 제출 (EXPO-02). */
    @Operation(summary = "개최 신청 심사 제출", description = "임시저장·반려 상태의 신청서를 심사 요청합니다.")
    @PostMapping("/{requestId}/submit")
    public ResponseEntity<ApiResponse<Void>> submit(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long requestId) {
        expoService.submitOpeningRequest(principal.getMemberId(), requestId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
