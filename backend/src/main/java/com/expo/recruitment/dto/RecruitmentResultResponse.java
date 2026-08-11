package com.expo.recruitment.dto;

import com.expo.recruitment.entity.RecruitmentResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 모집 결과 스냅샷 응답. */
@Schema(description = "모집 결과")
public record RecruitmentResultResponse(
        @Schema(description = "결과 ID") Long id,
        @Schema(description = "모집공고 ID") Long recruitmentNoticeId,
        @Schema(description = "주최자 클라이언트 사용자 ID") Long hostClientId,
        @Schema(description = "확정 기업 수") Integer confirmedCompanyCount,
        @Schema(description = "확정 부스 수") Integer confirmedBoothCount,
        @Schema(description = "부스 매출 합계") BigDecimal totalBoothSalesAmount,
        @Schema(description = "결과 상태") RecruitmentResultStatus status,
        @Schema(description = "생성 일시") LocalDateTime generatedAt,
        @Schema(description = "전달 일시") LocalDateTime deliveredAt,
        @Schema(description = "주최자 확인 일시") LocalDateTime confirmedByHostAt,
        @Schema(description = "결과 항목 목록") List<RecruitmentResultItemResponse> items,
        @Schema(description = "생성 일시") LocalDateTime createdAt) {}
