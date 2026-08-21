package com.expo.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 정산 금액의 구성 항목 하나.
 *
 * <p>{@code includedInRemittance} 가 <b>정산금에 들어갔는지</b>를 가른다. 예매 수수료는 구매자가
 * 판매원금 위에 추가로 낸 플랫폼 몫이라 {@code false} 이고, 화면은 이 값만 보고 "받는 금액" 과
 * "참고 금액" 을 나눌 수 있다.
 *
 * <p>금액은 <b>더하면 정산금이 되도록</b> 부호가 맞춰져 있다. 환불은 음수다.
 */
@Schema(description = "정산 구성 항목")
public record SettlementItemResponse(
        @Schema(
                        description =
                                "TICKET_SALE / TICKET_REFUND / BOOKING_FEE / "
                                        + "BOOKING_FEE_REFUND / BOOTH_SALE / ADJUSTMENT",
                        example = "TICKET_SALE")
                String itemType,
        @Schema(description = "금액. 환불은 음수", example = "20000") BigDecimal amount,
        @Schema(description = "정산금에 포함되는 항목인가", example = "true") boolean includedInRemittance,
        @Schema(description = "계산 시각") Instant occurredAt) {}
