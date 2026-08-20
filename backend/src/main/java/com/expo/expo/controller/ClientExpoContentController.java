package com.expo.expo.controller;

import com.expo.common.response.ApiResponse;
import com.expo.expo.dto.AttachExpoFileRequest;
import com.expo.expo.dto.AttachExpoImageRequest;
import com.expo.expo.dto.ExpoFileResponse;
import com.expo.expo.dto.ExpoImageResponse;
import com.expo.expo.service.ExpoContentService;
import com.expo.jwt.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주최사가 자기 박람회의 이미지·자료를 관리한다 (이슈 #93).
 *
 * <p>파일은 {@code POST /api/files} 로 먼저 올리고, 받은 {@code fileId} 를 여기로 보내 연결한다.
 */
@Tag(name = "Client Expo Content", description = "주최사 박람회 이미지·자료 관리")
@RestController
@RequestMapping("/api/client/expos/{expoId}")
@RequiredArgsConstructor
public class ClientExpoContentController {

    private final ExpoContentService expoContentService;

    @Operation(summary = "박람회 이미지 연결", description = "대표 이미지를 새로 지정하면 기존 대표는 상세 이미지로 내려간다.")
    @PostMapping("/images")
    public ResponseEntity<ApiResponse<ExpoImageResponse>> attachImage(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @Valid @RequestBody AttachExpoImageRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoContentService.attachImage(expoId, principal.getMemberId(), request)));
    }

    @Operation(summary = "박람회 이미지 목록")
    @GetMapping("/images")
    public ResponseEntity<ApiResponse<List<ExpoImageResponse>>> listImages(
            @PathVariable Long expoId) {
        return ResponseEntity.ok(ApiResponse.ok(expoContentService.listImages(expoId)));
    }

    @Operation(summary = "박람회 이미지 연결 해제")
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> detachImage(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @PathVariable Long imageId) {
        expoContentService.detachImage(expoId, imageId, principal.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "박람회 자료 연결", description = "소개 PDF·카탈로그·리플렛 등.")
    @PostMapping("/files")
    public ResponseEntity<ApiResponse<ExpoFileResponse>> attachFile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @Valid @RequestBody AttachExpoFileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        expoContentService.attachFile(expoId, principal.getMemberId(), request)));
    }

    @Operation(summary = "박람회 자료 목록")
    @GetMapping("/files")
    public ResponseEntity<ApiResponse<List<ExpoFileResponse>>> listFiles(
            @PathVariable Long expoId) {
        return ResponseEntity.ok(ApiResponse.ok(expoContentService.listFiles(expoId)));
    }

    @Operation(summary = "박람회 자료 연결 해제")
    @DeleteMapping("/files/{expoFileId}")
    public ResponseEntity<ApiResponse<Void>> detachFile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expoId,
            @PathVariable Long expoFileId) {
        expoContentService.detachFile(expoId, expoFileId, principal.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
