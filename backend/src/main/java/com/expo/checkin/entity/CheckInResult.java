package com.expo.checkin.entity;

/** 체크인 검증 결과. 실패도 이력으로 남긴다. */
public enum CheckInResult {
    SUCCESS, // 입장 처리 완료
    ALREADY_USED, // 이미 입장한 티켓
    CANCELED_TICKET, // 환불·무효 처리된 티켓
    WRONG_EXPO, // 다른 박람회의 티켓
    INVALID_TOKEN // QR·코드가 유효하지 않음
}
