package com.expo.common.config;

import java.math.BigDecimal;
import java.time.Instant;

/** 토스페이먼츠 결제 취소 API 성공 응답에서 필요한 값. */
public record TossCancelResult(
        String transactionKey, BigDecimal cancelAmount, Instant canceledAt, String rawResponse) {}
