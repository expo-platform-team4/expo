package com.expo.ticket.dto;

/** 비회원 주문 조회 인증에 필요한, 트랜잭션 밖에서 안전하게 사용할 값. */
public record GuestTicketOrderSearchSnapshot(
        Long ticketOrderId,
        String phoneNumber,
        String lookupPasswordHash,
        GuestTicketSearchResponse response) {}
