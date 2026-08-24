package com.expo.booth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 만료 주문 일괄 정리 결과. */
@Schema(description = "만료 주문 정리 결과")
public record BoothOrderExpirationResult(
        @Schema(description = "만료 처리한 주문 수", example = "3") int expiredCount,
        @Schema(description = "만료 처리된 주문") List<Expired> expired) {

    /** 만료 처리된 주문 하나. */
    @Schema(description = "만료된 주문")
    public record Expired(
            @Schema(description = "주문 ID", example = "12") Long orderId,
            @Schema(description = "주문번호", example = "BO12345") String orderNumber,
            @Schema(description = "신청서 ID", example = "7") Long applicationId) {}
}
