package com.expo.auth.entity;

/**
 * PHONE_VERIFICATIONS.status — V1 마이그레이션 CHECK 제약과 동일. REQUESTED 인증 요청됨, 아직 완료 전 문자 받고 번호 입력 대기
 * VERIFIED 인증 성공 번호 맞게 입력 완료 FAILED 인증 실패 (번호 틀림 등) 잘못된 번호 여러 번 EXPIRED 만료·무효 시간 지나거나 재발송으로 폐기
 */
public enum PhoneVerificationStatus {
  REQUESTED,
  VERIFIED,
  FAILED,
  EXPIRED
}
