package com.expo.refund.service;

/** 토스 취소 호출에 필요한 환불·결제 식별자 snapshot. */
record TicketRefundExecutionTarget(
        Long refundId,
        Long paymentId,
        String paymentKey,
        String cancelReason,
        String idempotencyKey,
        boolean alreadyCompleted) {}
