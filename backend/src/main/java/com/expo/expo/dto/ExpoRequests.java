package com.expo.expo.dto;

import com.expo.expo.domain.ExpoFileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 박람회 도메인 Request DTO 모음
 */
public final class ExpoRequests {

    private ExpoRequests() {
    }

    /**
     * 임시저장 / 개최 신청 요청 (희-EXPO-01, 희-EXPO-17)
     * DRAFT 단계에서는 title 등 NULL 허용(정의서 비고 3) — 심사 요청 시 도메인에서 필수 검증
     */
    public record ExpoDraftRequest(
        @NotNull Long clientId,
        @NotNull Long categoryId,
        @Size(max = 200) String desiredVenue,
        @Size(max = 200) String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 500) String thumbnailUrl,   // 희-EXPO-15 대표 이미지 URL
        @Size(max = 100) String region
    ) {
    }

    /**
     * 승인 전 클라이언트 직접 수정 요청 (희-EXPO-05)
     * null 필드는 변경하지 않음(부분 수정)
     */
    public record ExpoUpdateRequest(
        @NotNull Long clientId,
        Long categoryId,
        @Size(max = 200) String desiredVenue,
        @Size(max = 200) String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 500) String thumbnailUrl,
        @Size(max = 100) String region
    ) {
    }

    /** 승인 요청 — 승인 시점에 확정 장소 배정 (희-EXPO-09, 정의서 비고 2) */
    public record ExpoApproveRequest(
        @NotNull Long venueId
    ) {
    }

    // 반려 사유는 심사 이력 도메인(expo_review_history, 타 담당)에서 관리 — 별도 Request 없음

    /** 취소 요청 */
    public record ExpoCancelRequest(
        @NotBlank @Size(max = 1000) String cancelReason
    ) {
    }

    /**
     * 첨부 자료 등록 (희-EXPO-13 외부 링크, 희-EXPO-14 PDF·이미지·영상, 희-EXPO-15 카탈로그·리플렛)
     * 실제 바이너리 업로드는 스토리지(S3 등)에 선행 업로드 후 URL을 등록하는 방식
     */
    public record ExpoFileRequest(
        @NotNull ExpoFileType fileType,
        @NotBlank @Size(max = 1000) String url,
        @Size(max = 300) String displayName
    ) {
    }
}
