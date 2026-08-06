package com.expo.expo.domain;

import java.time.LocalDate;

/**
 * 판매 상태 배지 (희-EXPO-10, 희-SRCH-06, 희-SRCH-13)
 *
 * 테이블 정의서 비고 4에 따라 DB 컬럼으로 보유하지 않고,
 * start_date/end_date 와 TICKET_TYPE 잔여수량 기준으로 조회 시점에 계산하는 파생 값.
 */
public enum SaleStatus {

    /** 승인(공개)되었으나 티켓 판매 개시 전 (희-SRCH-13 `판매 예정`) */
    UPCOMING("판매예정"),

    /** 판매중 */
    ON_SALE("판매중"),

    /** 모든 티켓 종류 매진 */
    SOLD_OUT("매진"),

    /** 판매 종료(판매 기간 경과, 행사 진행 중) */
    SALE_ENDED("판매종료"),

    /** 행사 종료 */
    EVENT_ENDED("행사종료");

    private final String label;

    SaleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 판매 상태 자동 계산 (희-EXPO-10)
     *
     * @param today          기준일
     * @param startDate      행사 시작일
     * @param endDate        행사 종료일
     * @param hasTicketTypes 등록된 티켓 종류 존재 여부 (티켓은 승인 후 별도 등록됨)
     * @param allSoldOut     모든 티켓 종류 매진 여부
     */
    public static SaleStatus calculate(LocalDate today,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       boolean hasTicketTypes,
                                       boolean allSoldOut) {
        if (endDate != null && today.isAfter(endDate)) {
            return EVENT_ENDED;
        }
        if (!hasTicketTypes) {
            // 승인은 되었지만 티켓 미등록 → 판매 개시 전
            return UPCOMING;
        }
        if (allSoldOut) {
            return SOLD_OUT;
        }
        return ON_SALE;
    }
}
