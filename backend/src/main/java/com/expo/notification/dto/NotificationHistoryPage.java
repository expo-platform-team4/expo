package com.expo.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 알림 이력 페이지. 관리자 감사 이력 조회와 같은 모양이다. */
@Schema(description = "알림 이력 페이지")
public record NotificationHistoryPage(
        @Schema(description = "전체 건수", example = "1024") long totalCount,
        @Schema(description = "요청한 페이지 번호 (0부터)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "알림 목록") List<NotificationHistoryResponse> items) {}
