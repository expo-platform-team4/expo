package com.expo.auth.entity;

/**
 * PHONE_VERIFICATIONS.status — V202608250916 마이그레이션 CHECK 제약과 동일.
 *
 * <ul>
 *   <li>{@code REQUESTED} — 인증 요청됨, 아직 완료 전. 문자 받고 인증번호 입력 대기
 *   <li>{@code VERIFIED} — 인증 성공. 인증번호 맞게 입력 완료, 회원가입용 토큰 발급됨
 *   <li>{@code FAILED} — 인증 실패 (인증번호 틀림)
 *   <li>{@code EXPIRED} — 만료·무효. 시간 지나거나 재요청으로 폐기
 *   <li>{@code USED} — 회원가입에서 토큰을 소비함. 같은 토큰으로 계정을 여러 개 만들지 못하게 막는다
 * </ul>
 *
 * <p>{@link EmailVerificationStatus} 와 값이 같다. 두 인증이 같은 흐름을 따르므로 상태도 같아야
 * 회원가입 쪽에서 둘을 다르게 다룰 이유가 없어진다.
 */
public enum PhoneVerificationStatus {
    REQUESTED,
    VERIFIED,
    FAILED,
    EXPIRED,
    USED
}
