package com.expo.auth.entity;

/**
 * EMAIL_VERIFICATIONS.status — V202608210900 마이그레이션 CHECK 제약과 동일.
 *
 * <ul>
 *   <li>{@code REQUESTED} — 인증 요청됨, 아직 완료 전. 메일 받고 코드 입력 대기
 *   <li>{@code VERIFIED} — 인증 성공. 코드 맞게 입력 완료, 회원가입용 토큰 발급됨
 *   <li>{@code FAILED} — 인증 실패 (코드 틀림)
 *   <li>{@code EXPIRED} — 만료·무효. 시간 지나거나 재요청으로 폐기
 *   <li>{@code USED} — 회원가입에서 토큰을 소비함. {@link PhoneVerificationStatus}에는 없는 상태다 — 휴대폰
 *       인증은 아직 회원가입이 결과를 검증하지 않지만, 이메일 인증은 검증하므로 재사용을 막아야 한다
 * </ul>
 */
public enum EmailVerificationStatus {
    REQUESTED,
    VERIFIED,
    FAILED,
    EXPIRED,
    USED
}
