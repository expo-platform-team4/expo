package com.expo.ticket.entity;

/** 결제 대기 중 확보한 티켓 재고의 상태. */
public enum InventoryReservationStatus {
    ACTIVE, // 결제 대기 중 임시 재고 확보
    CONFIRMED, // 결제 성공으로 판매 확정
    EXPIRED, // 결제 시간이 지나 자동 만료
    RELEASED // 결제 실패/취소 등으로 재고 반환
}
