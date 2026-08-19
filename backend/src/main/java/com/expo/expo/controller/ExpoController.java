package com.expo.expo.controller;

import com.expo.expo.dto.ExpoDto.*;
import com.expo.expo.entity.ExpoEnums;
import com.expo.expo.service.ExpoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 박람회 REST API — 도메인 /expo/** (V1 스키마 기준)
 *
 * [클라이언트 — 개최 신청]
 *  POST   /expo/requests                        임시저장 생성 (희-EXPO-01, 희-EXPO-17)
 *  PATCH  /expo/requests/{requestId}            승인 전 직접 수정 (희-EXPO-05 / 승인 후 409 — 희-EXPO-06)
 *  POST   /expo/requests/{requestId}/submit     심사 요청 (희-EXPO-02)
 *  POST   /expo/requests/{requestId}/cancel     신청 취소 (승인 전)
 *  GET    /expo/requests/my                     내 신청 목록
 *  GET    /expo/requests/{requestId}            신청 상세
 *
 * [관리자 — 심사]
 *  POST   /expo/requests/{requestId}/review     심사 시작
 *  POST   /expo/requests/{requestId}/approve    승인 → expos 생성 + 자동 공개 (희-EXPO-09, 희-SRCH-12)
 *  POST   /expo/requests/{requestId}/reject     반려
 *
 * [클라이언트 — 공개 박람회 부속]
 *  POST   /expo/{expoId}/images                 대표(THUMBNAIL)·상세 이미지 등록 (희-EXPO-15)
 *  POST   /expo/{expoId}/files                  PDF·카탈로그·리플렛·홍보영상 등록 (희-EXPO-14/15)
 *  POST   /expo/{expoId}/links                  외부 링크 등록 (희-EXPO-13)
 *  POST   /expo/{expoId}/change-requests        승인 후 수정 요청 (희-EXPO-06 대응 경로)
 *
 * [공개 조회]
 *  GET    /expo                                 목록 검색 (희-SRCH-01~13)
 *  GET    /expo/{expoId}                        상세 조회 (판매 상태 자동 계산 — 희-EXPO-10)
 */
@RestController
@RequestMapping("/expo")
@RequiredArgsConstructor
public class ExpoController {

    private final ExpoService expoService;

    /* ==================== 개최 신청 (클라이언트) ==================== */

    @PostMapping("/requests")
    public ResponseEntity<Long> createOpeningRequest(
            @Valid @RequestBody OpeningRequestCreate request) {
        Long id = expoService.createOpeningRequest(request);
        return ResponseEntity.created(URI.create("/expo/requests/" + id)).body(id);
    }

    @PatchMapping("/requests/{requestId}")
    public ResponseEntity<Void> updateOpeningRequest(
            @PathVariable Long requestId, @Valid @RequestBody OpeningRequestUpdate request) {
        expoService.updateOpeningRequest(requestId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/submit")
    public ResponseEntity<Void> submitOpeningRequest(
            @PathVariable Long requestId, @RequestParam Long clientId) {
        expoService.submitOpeningRequest(requestId, clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/cancel")
    public ResponseEntity<Void> cancelOpeningRequest(
            @PathVariable Long requestId, @RequestParam Long clientId) {
        expoService.cancelOpeningRequest(requestId, clientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests/my")
    public ResponseEntity<Page<OpeningRequestResponse>> getMyOpeningRequests(
            @RequestParam Long clientId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(expoService.getMyOpeningRequests(clientId, page, size));
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<OpeningRequestResponse> getOpeningRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(expoService.getOpeningRequestDetail(requestId));
    }

    /* ==================== 심사 (관리자) ==================== */

    @PostMapping("/requests/{requestId}/review")
    public ResponseEntity<Void> startReview(
            @PathVariable Long requestId, @RequestParam Long adminId) {
        expoService.startReview(requestId, adminId);
        return ResponseEntity.noContent().build();
    }

    /** 희-EXPO-09 승인 → expos 생성·자동 공개, 생성된 expoId 반환 */
    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<Long> approveOpeningRequest(
            @PathVariable Long requestId, @Valid @RequestBody OpeningRequestApprove request) {
        Long expoId = expoService.approveOpeningRequest(requestId, request);
        return ResponseEntity.created(URI.create("/expo/" + expoId)).body(expoId);
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Void> rejectOpeningRequest(
            @PathVariable Long requestId, @Valid @RequestBody OpeningRequestReject request) {
        expoService.rejectOpeningRequest(requestId, request);
        return ResponseEntity.noContent().build();
    }

    /* ==================== 부속 자료 ==================== */

    @PostMapping("/{expoId}/images")
    public ResponseEntity<Long> addImage(
            @PathVariable Long expoId, @Valid @RequestBody ExpoImageCreate request) {
        Long id = expoService.addImage(expoId, request);
        return ResponseEntity.created(URI.create("/expo/" + expoId + "/images/" + id)).body(id);
    }

    @PostMapping("/{expoId}/files")
    public ResponseEntity<Long> addFile(
            @PathVariable Long expoId, @Valid @RequestBody ExpoFileCreate request) {
        Long id = expoService.addFile(expoId, request);
        return ResponseEntity.created(URI.create("/expo/" + expoId + "/files/" + id)).body(id);
    }

    @PostMapping("/{expoId}/links")
    public ResponseEntity<Long> addExternalLink(
            @PathVariable Long expoId, @Valid @RequestBody ExternalLinkCreate request) {
        Long id = expoService.addExternalLink(expoId, request);
        return ResponseEntity.created(URI.create("/expo/" + expoId + "/links/" + id)).body(id);
    }

    @PostMapping("/{expoId}/change-requests")
    public ResponseEntity<Long> createChangeRequest(
            @PathVariable Long expoId, @Valid @RequestBody ChangeRequestCreate request) {
        Long id = expoService.createChangeRequest(expoId, request);
        return ResponseEntity.created(URI.create("/expo/" + expoId + "/change-requests/" + id))
                .body(id);
    }

    /* ==================== 공개 조회 ==================== */

    /**
     * 목록 검색 (희-SRCH-01~13)
     * 예) GET /expo?keyword=푸드&categoryId=2&regionCode=SEOUL&fromDate=2026-09-01&toDate=2026-09-30
     *        &minPrice=10000&maxPrice=30000&saleStatus=ON_SALE&sort=DEADLINE&page=1&size=12
     */
    @GetMapping
    public ResponseEntity<Page<ExpoCardResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate toDate,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) ExpoEnums.SaleStatus saleStatus,
            @RequestParam(required = false) SearchCondition.Sort sort,
            @RequestParam(defaultValue = "1") int page, // 희-SRCH-10 페이지 번호 방식
            @RequestParam(defaultValue = "12") int size) {

        SearchCondition condition =
                new SearchCondition(
                        keyword,
                        categoryId,
                        regionCode,
                        fromDate,
                        toDate,
                        minPrice,
                        maxPrice,
                        saleStatus,
                        sort);
        return ResponseEntity.ok(expoService.search(condition, page, size));
    }

    @GetMapping("/{expoId}")
    public ResponseEntity<ExpoDetailResponse> getDetail(@PathVariable Long expoId) {
        return ResponseEntity.ok(expoService.getDetail(expoId));
    }
}
