package com.expo.refund.event;

/** 환불 요청 행이 커밋된 뒤 실제 PG 취소를 시작하기 위한 이벤트. */
public record TicketRefundRequestedEvent(Long refundId) {}
