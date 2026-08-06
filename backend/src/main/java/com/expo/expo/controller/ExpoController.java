package com.expo.expo.controller;

import com.expo.expo.dto.ExpoRequests.*;
import com.expo.expo.dto.ExpoResponses.ExpoCardResponse;
import com.expo.expo.dto.ExpoResponses.ExpoDetailResponse;
import com.expo.expo.dto.ExpoSearchCondition;
import com.expo.expo.service.ExpoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

/**
 * 박람회 REST API — 도메인 /expo/**
 *
 * [클라이언트]
 *  POST   /expo                     임시저장 생성 (희-EXPO-01, 희-EXPO-17)
 *  PATCH  /expo/{expoId}            승인 전 직접 수정 (희-EXPO-05 / 승인 후 409 — 희-EXPO-06)
 *  POST   /expo/{expoId}/submit     심사 요청 (희-EXPO-02)
 *  POST   /expo/{expoId}/files      외부 링크·PDF·이미지·영상·카탈로그·리플렛 등록 (희-EXPO-13~15)
 *  POST   /expo/{expoId}/cancel     취소 요청
 *  DELETE /expo/{expoId}            논리 삭제
 *  GET    /expo/my                  내 등록 목록(임시저장 포함)
 *
 * [관리자]
 *  POST   /expo/{expoId}/review     심사 시작
 *  POST   /expo/{expoId}/approve    승인 → 자동 공개 (희-EXPO-09, 희-SRCH-12)
 *  POST   /expo/{expoId}/reject     반려
 *
 * [공개 조회]
 *  GET    /expo                     목록 검색 (희-SRCH-01 ~ 13)
 *  GET    /expo/{expoId}            상세 조회
 */
@RestController
@RequestMapping("/expo")
@RequiredArgsConstructor
public class ExpoController {

    private final ExpoService expoService;

    /* ==================== 클라이언트 ==================== */

    /** 희-EXPO-01 박람회 임시저장 (희-EXPO-17 개최 신청 시작) */
    @PostMapping
    public ResponseEntity<Long> createDraft(@Valid @RequestBody ExpoDraftRequest request) {
        Long expoId = expoService.saveDraft(request);
        return ResponseEntity.created(URI.create("/expo/" + expoId)).body(expoId);
    }

    /** 희-EXPO-05 승인 전 직접 수정 — 승인(PUBLISHED) 이후에는 도메인에서 차단(희-EXPO-06) */
    @PatchMapping("/{expoId}")
    public ResponseEntity<Void> update(@PathVariable Long expoId,
                                       @Valid @RequestBody ExpoUpdateRequest request) {
        expoService.update(expoId, request);
        return ResponseEntity.noContent().build();
    }

    /** 희-EXPO-02 박람회 등록 및 심사 요청 */
    @PostMapping("/{expoId}/submit")
    public ResponseEntity<Void> submit(@PathVariable Long expoId,
                                       @RequestParam Long clientId) {
        expoService.submit(expoId, clientId);
        return ResponseEntity.noContent().build();
    }

    /** 희-EXPO-13/14/15 외부 링크 및 자료(PDF·이미지·홍보영상·카탈로그·리플렛) 등록 */
    @PostMapping("/{expoId}/files")
    public ResponseEntity<Long> addFile(@PathVariable Long expoId,
                                        @Valid @RequestBody ExpoFileRequest request) {
        Long fileId = expoService.addFile(expoId, request);
        return ResponseEntity.created(URI.create("/expo/" + expoId + "/files/" + fileId)).body(fileId);
    }

    /** 취소 요청 */
    @PostMapping("/{expoId}/cancel")
    public ResponseEntity<Void> requestCancellation(@PathVariable Long expoId,
                                                    @RequestParam Long clientId,
                                                    @Valid @RequestBody ExpoCancelRequest request) {
        expoService.requestCancellation(expoId, clientId, request);
        return ResponseEntity.noContent().build();
    }

    /** 논리 삭제 */
    @DeleteMapping("/{expoId}")
    public ResponseEntity<Void> delete(@PathVariable Long expoId,
                                       @RequestParam Long clientId) {
        expoService.delete(expoId, clientId);
        return ResponseEntity.noContent().build();
    }

    /** 내 등록 박람회 목록 (임시저장 포함) */
    @GetMapping("/my")
    public ResponseEntity<Page<ExpoDetailResponse>> getMyExpos(@RequestParam Long clientId,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(expoService.getMyExpos(clientId, page, size));
    }

    /* ==================== 관리자 ==================== */

    /** 심사 시작 */
    @PostMapping("/{expoId}/review")
    public ResponseEntity<Void> startReview(@PathVariable Long expoId) {
        expoService.startReview(expoId);
        return ResponseEntity.noContent().build();
    }

    /** 희-EXPO-09 승인 → 자동 공개 + 확정 장소 배정 → 목록 자동 반영(희-SRCH-12) */
    @PostMapping("/{expoId}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long expoId,
                                        @Valid @RequestBody ExpoApproveRequest request) {
        expoService.approve(expoId, request);
        return ResponseEntity.noContent().build();
    }

    /** 반려 — 반려 사유 기록은 심사 이력 도메인(타 담당) API 담당 */
    @PostMapping("/{expoId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long expoId) {
        expoService.reject(expoId);
        return ResponseEntity.noContent().build();
    }

    /* ==================== 공개 조회 ==================== */

    /**
     * 목록 검색 (희-SRCH-01 ~ 13)
     * 예) GET /expo?keyword=푸드&categoryId=2&region=서울&fromDate=2026-09-01&toDate=2026-09-30
     *        &minPrice=10000&maxPrice=30000&saleStatus=ON_SALE&sort=DEADLINE&page=1&size=12
     */
    @GetMapping
    public ResponseEntity<Page<ExpoCardResponse>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(required = false) com.expo.expo.domain.SaleStatus saleStatus,
        @RequestParam(required = false) ExpoSearchCondition.ExpoSort sort,
        @RequestParam(defaultValue = "1") int page,      // 희-SRCH-10 페이지 번호 방식
        @RequestParam(defaultValue = "12") int size) {

        ExpoSearchCondition condition = new ExpoSearchCondition(
            keyword, categoryId, region, fromDate, toDate, minPrice, maxPrice, saleStatus, sort);
        return ResponseEntity.ok(expoService.search(condition, page, size));
    }

    /** 상세 조회 — 판매 상태(희-EXPO-10)·티켓·첨부 자료 포함 */
    @GetMapping("/{expoId}")
    public ResponseEntity<ExpoDetailResponse> getDetail(@PathVariable Long expoId) {
        return ResponseEntity.ok(expoService.getDetail(expoId));
    }
}
