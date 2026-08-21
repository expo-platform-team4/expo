package com.expo.member.dto;

import java.time.Instant;

/**
 * {@code v_member_mypage_tickets} 뷰의 로우 하나 (티켓 1장). 박람회별로 묶기 전 단계의 평평한(flat) 형태라 화면
 * DTO({@link MemberTicketGroupResponse})와는 별도로 둔다.
 */
public record MemberTicketRow(
        Long orderId,
        Long issuedTicketId,
        String ticketCode,
        String status,
        Instant checkedInAt,
        Long expoId,
        String expoTitle,
        boolean secureQrAccess,
        Instant eventStartAt,
        Instant eventEndAt,
        int orderItemQuantity) {}
