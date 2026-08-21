package com.expo.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 토스 결제 실패 뒤 임시 재고 해제를 요청할 때 보내는 값. */
public record FailTicketPaymentRequest(
        @NotBlank(message = "주문 ID는 필수입니다.") String orderId,
        @NotBlank(message = "실패 코드는 필수입니다.") @Size(max = 100, message = "실패 코드는 100자 이하여야 합니다.")
                String failureCode) {}
