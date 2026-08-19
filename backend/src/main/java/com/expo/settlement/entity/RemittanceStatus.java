package com.expo.settlement.entity;

/**
 * 송금 결과. {@code remittances_status_check} 와 값이 같아야 한다.
 *
 * <p>은행 자동 송금은 MVP 범위 밖이라 <b>사람이 계좌이체를 하고 결과만 기록</b>한다. 그래서 여기 값은
 * 시스템이 진행시키는 상태가 아니라 <b>사람이 알려 주는 사실</b>이다.
 */
public enum RemittanceStatus {
    PENDING, // 예정
    PROCESSING, // 처리 중
    REMITTED, // 송금 완료
    FAILED, // 실패 (계좌 오류 등)
    CANCELED // 취소
}
