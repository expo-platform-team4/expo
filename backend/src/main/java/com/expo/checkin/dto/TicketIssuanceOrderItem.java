package com.expo.checkin.dto;

/**
 * 발권 대상 주문 항목 하나. {@code ticket_order_items} 조회 결과 투영이다.
 *
 * <p>{@code expoId} 는 {@code ticket_order_items → ticket_products → expos} 를 타고 온다. {@code
 * ticket_orders} 에는 박람회 컬럼이 없어서 항목을 거쳐야만 알 수 있다.
 *
 * @param orderItemId {@code ticket_order_items.id}. 발권 티켓의 FK 가 된다
 * @param expoId 체크인 시 대조할 박람회
 * @param quantity 이 항목의 구매 수량. <b>이 수만큼 입장권을 만든다</b>
 */
public record TicketIssuanceOrderItem(Long orderItemId, Long expoId, int quantity) {}
