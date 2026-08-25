package com.expo.participation.dto;

import com.expo.participation.entity.ParticipationApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 주최사(박람회 개설 클라이언트)가 보는 참여 기업 한 건.
 *
 * <p>{@link AdminParticipationApplicationResponse} 와 달리 관리자 전용 필드(운영 확인 시각·메모)는
 * 뺀다 — 주최사는 자기 박람회에 누가 참여하는지만 알면 되지, 관리자 내부 처리 이력까지 볼 필요는 없다.
 */
@Schema(description = "주최사용 참여 기업")
public record ClientParticipatingCompanyResponse(
        @Schema(description = "신청서 ID") Long applicationId,
        @Schema(description = "참가 기업명") String companyName,
        @Schema(description = "참여 목적") String participationPurpose,
        @Schema(description = "전시 품목 설명") String exhibitDescription,
        @Schema(description = "선택한 부스 상품 ID") Long selectedBoothProductId,
        @Schema(description = "처리 상태") ParticipationApplicationStatus status,
        @Schema(description = "제출 일시") Instant submittedAt,
        @Schema(description = "신청 일시") Instant createdAt) {}
