package com.expo.auth.entity;

/** password_reset_tokens.status — 비밀번호 재설정 토큰 상태 (V1 스키마 CHECK 제약과 동일). */
public enum PasswordResetStatus {
    /** 발급됨. 아직 쓰지 않음. */
    ISSUED,
    /** 새 비밀번호 저장까지 완료해 다 쓴 토큰. */
    USED,
    /** 유효 시간이 지나 못 쓰게 된 토큰. */
    EXPIRED,
    /** 같은 사용자가 재요청해 앞선 발급분을 무효화한 토큰. */
    REVOKED
}
