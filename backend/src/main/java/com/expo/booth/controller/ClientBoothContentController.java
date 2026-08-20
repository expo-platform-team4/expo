package com.expo.booth.controller;

import com.expo.booth.dto.AddBoothContentFileRequest;
import com.expo.booth.dto.AddExternalLinkRequest;
import com.expo.booth.dto.BoothContentFileResponse;
import com.expo.booth.dto.BoothContentResponse;
import com.expo.booth.dto.CreateBoothContentRequest;
import com.expo.booth.dto.ExternalLinkResponse;
import com.expo.booth.dto.ReorderBoothContentFileRequest;
import com.expo.booth.dto.ReorderExternalLinkRequest;
import com.expo.booth.dto.UpdateBoothContentRequest;
import com.expo.booth.dto.UpdateExternalLinkRequest;
import com.expo.booth.service.ClientBoothContentService;
import com.expo.common.response.ApiResponse;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여 기업의 부스 콘텐츠 작성·수정·공개와 첨부 파일 관리. */
@Tag(name = "Client Booth Content", description = "참여 기업 부스 콘텐츠 관리")
@RestController
@RequestMapping("/api/client/booth-contents")
public class ClientBoothContentController {

    private final ClientBoothContentService clientBoothContentService;

    public ClientBoothContentController(ClientBoothContentService clientBoothContentService) {
        this.clientBoothContentService = clientBoothContentService;
    }

    @Operation(summary = "부스 콘텐츠 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<BoothContentResponse>> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateBoothContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                clientBoothContentService.create(
                                        request, principal.getMemberId())));
    }

    @Operation(summary = "내 부스 콘텐츠 상세 조회")
    @GetMapping("/{contentId}")
    public ResponseEntity<ApiResponse<BoothContentResponse>> getMine(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contentId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.getMine(contentId, principal.getMemberId())));
    }

    @Operation(summary = "배정 ID로 내 부스 콘텐츠 조회 (상태 무관)")
    @GetMapping("/by-allocation/{boothAllocationId}")
    public ResponseEntity<ApiResponse<BoothContentResponse>> getMineByAllocation(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long boothAllocationId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.getMineByAllocation(
                                boothAllocationId, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 본문 수정")
    @PutMapping("/{contentId}")
    public ResponseEntity<ApiResponse<BoothContentResponse>> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateBoothContentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.update(
                                contentId, request, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 검수 요청 (관리자 승인 후 공개된다)")
    @PostMapping("/{contentId}/submit-for-review")
    public ResponseEntity<ApiResponse<BoothContentResponse>> submitForReview(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long contentId) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.submitForReview(
                                contentId, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 첨부 파일 등록")
    @PostMapping("/{contentId}/files")
    public ResponseEntity<ApiResponse<BoothContentFileResponse>> addFile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @Valid @RequestBody AddBoothContentFileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                clientBoothContentService.addFile(
                                        contentId, request, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 첨부 파일 삭제")
    @DeleteMapping("/{contentId}/files/{fileEntryId}")
    public ResponseEntity<Void> removeFile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @PathVariable Long fileEntryId) {
        clientBoothContentService.removeFile(contentId, fileEntryId, principal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "부스 콘텐츠 첨부 파일 노출 순서 변경")
    @PutMapping("/{contentId}/files/{fileEntryId}/sort-order")
    public ResponseEntity<ApiResponse<BoothContentFileResponse>> reorderFile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @PathVariable Long fileEntryId,
            @Valid @RequestBody ReorderBoothContentFileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.reorderFile(
                                contentId,
                                fileEntryId,
                                request.sortOrder(),
                                principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 외부 링크 등록")
    @PostMapping("/{contentId}/links")
    public ResponseEntity<ApiResponse<ExternalLinkResponse>> addLink(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @Valid @RequestBody AddExternalLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                clientBoothContentService.addLink(
                                        contentId, request, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 외부 링크 수정")
    @PutMapping("/{contentId}/links/{linkId}")
    public ResponseEntity<ApiResponse<ExternalLinkResponse>> updateLink(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateExternalLinkRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.updateLink(
                                contentId, linkId, request, principal.getMemberId())));
    }

    @Operation(summary = "부스 콘텐츠 외부 링크 삭제")
    @DeleteMapping("/{contentId}/links/{linkId}")
    public ResponseEntity<Void> removeLink(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @PathVariable Long linkId) {
        clientBoothContentService.removeLink(contentId, linkId, principal.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "부스 콘텐츠 외부 링크 노출 순서 변경")
    @PutMapping("/{contentId}/links/{linkId}/sort-order")
    public ResponseEntity<ApiResponse<ExternalLinkResponse>> reorderLink(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long contentId,
            @PathVariable Long linkId,
            @Valid @RequestBody ReorderExternalLinkRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        clientBoothContentService.reorderLink(
                                contentId, linkId, request.sortOrder(), principal.getMemberId())));
    }
}
