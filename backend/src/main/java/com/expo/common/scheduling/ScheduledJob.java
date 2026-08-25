package com.expo.common.scheduling;

/**
 * 인스턴스 하나만 실행해야 하는 주기 작업 목록.
 *
 * <h2>번호는 잠금 식별자다 — 절대 바꾸지 않는다</h2>
 *
 * 각 상수의 번호가 PostgreSQL 어드바이저리 락의 키가 된다. 번호를 바꾸거나 재사용하면
 * <b>배포 도중 두 인스턴스가 서로 다른 키로 같은 작업을 잡는다</b> — 구버전은 옛 번호를,
 * 신버전은 새 번호를 걸어 둘 다 성공하고, 정확히 막으려던 중복 실행이 일어난다.
 *
 * <p>작업이 없어져도 그 번호는 비워 둔다. 새 작업은 항상 다음 번호를 쓴다.
 */
public enum ScheduledJob {

    /** 만료된 티켓 예약을 해제한다. */
    TICKET_RESERVATION_EXPIRATION(1),

    /** 결제 기한이 지난 부스 주문을 만료시킨다. */
    BOOTH_ORDER_EXPIRATION(2),

    /** 모집공고를 신청 시작·종료 시각에 맞춰 전환한다. */
    RECRUITMENT_NOTICE_SCHEDULE(3),

    /** 행사가 끝난 지 7일 지난 박람회의 정산 대상을 만든다. */
    SETTLEMENT_GENERATION(4),

    /** 배너를 시작·종료 시각에 맞춰 켜고 끈다. */
    BANNER_DISPLAY_SYNC(5);

    private final int lockId;

    ScheduledJob(int lockId) {
        this.lockId = lockId;
    }

    int lockId() {
        return lockId;
    }
}
