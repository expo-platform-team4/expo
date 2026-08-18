package com.expo.notification.repository;

import com.expo.notification.dto.NotificationHistoryRow;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 관리자용 알림 이력 조회와 재발송 보조 쿼리.
 *
 * <p>JPA 가 아니라 MyBatis 인 이유는 <b>두 테이블을 요약해 한 줄로 만들기 때문</b>이다. 알림 1행에 시도 건수와 마지막 시도를
 * 붙인 모양은 어느 엔티티와도 대응하지 않는다. 엔티티로 읽으면 알림마다 이력을 전부 끌어오게 되어, 1,000명짜리 대량 발송에서
 * 그대로 터진다.
 */
@Mapper
public interface NotificationHistoryMapper {

    /**
     * 알림 목록. 최신순이다.
     *
     * @param size 한 페이지 크기. 호출부가 상한을 건다
     * @param offset 건너뛸 개수. <b>{@code long} 이다</b> — {@code int} 로 계산하면 큰 페이지에서 오버플로해 음수가 되고,
     *     PostgreSQL 이 {@code OFFSET must not be negative} 로 거절한다
     */
    List<NotificationHistoryRow> search(
            @Param("status") String status,
            @Param("templateCode") String templateCode,
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("size") int size,
            @Param("offset") long offset);

    /** 같은 조건의 전체 건수. */
    long countSearch(
            @Param("status") String status,
            @Param("templateCode") String templateCode,
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);

    /**
     * 알림 행을 잠그고 현재 상태를 돌려준다. 없으면 {@code null}.
     *
     * <p><b>호출자의 트랜잭션 안에서 불러야 한다.</b> 잠금은 트랜잭션이 끝나면 풀린다.
     *
     * @return {@code notifications.status} 문자열. 행이 없으면 {@code null}
     */
    String lockNotification(@Param("notificationId") Long notificationId);

    /**
     * 다음 시도 번호. 이력이 없으면 {@code 1}.
     *
     * <p>{@code message_histories} 에 {@code (notification_id, attempt_no)} UNIQUE 가 걸려 있어, 이 값을 잘못
     * 세면 INSERT 가 실패한다. 잠금 안에서 불러야 두 요청이 같은 번호를 받지 않는다.
     */
    int nextAttemptNo(@Param("notificationId") Long notificationId);
}
