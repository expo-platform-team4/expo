package com.expo.checkin.dto;

import java.time.Instant;

/**
 * 조회 화면에 보여줄 입장권 한 장. {@code issued_tickets} 조회 결과 투영이다.
 *
 * <p><b>QR 원문은 여기에 없다.</b> DB 에 저장돼 있지 않기 때문이다. 서비스가 {@code ticketCode} 로 다시 계산해 응답에 채운다.
 *
 * @param issuedTicketId {@code issued_tickets.id}
 * @param ticketCode QR 이 안 찍힐 때 손으로 입력하는 값이자, QR 원문을 다시 만드는 입력
 * @param status {@code ISSUED} / {@code CHECKED_IN} / {@code CANCELED} / {@code INVALIDATED}
 * @param checkedInAt 입장 시각. 아직 입장 전이면 {@code null}
 * @param expoTitle 박람회명
 * @param expoStartAt 행사 시작 시각
 * @param expoEndAt 행사 종료 시각
 */
public record TicketViewTicket(
        Long issuedTicketId,
        String ticketCode,
        String status,
        Instant checkedInAt,
        String expoTitle,
        Instant expoStartAt,
        Instant expoEndAt) {}
