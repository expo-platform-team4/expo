package com.expo.common.config;

import java.math.BigDecimal;

/** 토스페이먼츠 결제 승인 API 연동. */
public interface TossPaymentClient {

    /**
     * 결제 승인 요청. 실패하면 {@link TossApiException} 을 던진다.
     *
     * @param idempotencyKey 같은 승인 요청이 중복 전송돼도 토스 쪽에서 한 번만 처리하도록 하는 키
     */
    TossConfirmResult confirmPayment(
            String paymentKey, String orderId, BigDecimal amount, String idempotencyKey);

    /** 프런트가 결제창을 띄울 때 쓰는 공개 클라이언트 키. */
    String getClientKey();
}
