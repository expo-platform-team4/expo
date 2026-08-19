package com.expo.settlement.entity;

/**
 * 정산 진행 단계. {@code settlements_status_check} 와 값이 같아야 한다.
 *
 * <pre>
 * WAITING → CALCULATED → UNDER_REVIEW → CONFIRMED → REMITTANCE_PENDING → REMITTED
 *                                                                      ON_HOLD
 * </pre>
 */
public enum SettlementStatus {
    WAITING, // 대상만 생성됨. 금액은 아직 0
    CALCULATED, // 서버가 금액을 계산함
    UNDER_REVIEW, // 관리자 검토 중
    CONFIRMED, // 확정. 금액이 더 이상 바뀌지 않는다
    REMITTANCE_PENDING, // 송금 대기
    REMITTED, // 송금 완료
    ON_HOLD // 보류. 분쟁·조사 등으로 진행을 멈춘 상태
}
