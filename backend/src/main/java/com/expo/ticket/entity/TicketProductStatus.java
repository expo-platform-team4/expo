package com.expo.ticket.entity;

/** 티켓 상품의 판매 상태. */
public enum TicketProductStatus {
    DRAFT, // 작성 중, 아직 판매 X
    ON_SALE, // 판매중
    SOLD_OUT, // 재고 소진
    SALE_ENDED, // 판매 기간 종료
    CANCELED // 판매 취소
}
