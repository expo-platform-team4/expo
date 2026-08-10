package com.expo.booth.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 토스페이먼츠 결제 승인 API 성공 응답에서 뽑아 쓰는 값. */
public record TossConfirmResult(
        String paymentKey,
        String orderId,
        String status,
        String method,
        BigDecimal totalAmount,
        LocalDateTime approvedAt,
        String rawResponse) {}
