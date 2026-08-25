package com.expo.common.scheduling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주기 작업을 <b>인스턴스 하나에서만</b> 실행시킨다.
 *
 * <h2>왜 필요한가</h2>
 *
 * 저장소에 {@code @Scheduled} 를 못 붙이고 <b>수동 내부 API 로 남겨 둔 작업이 넷</b> 있었다 —
 * 정산 생성, 부스 주문 만료, 모집공고 일정 처리, 배너 노출 정리. 넷 다 같은 이유였다.
 * 인스턴스가 여럿일 때 중복 실행을 막을 장치가 없다는 것.
 *
 * <p>작업들은 이미 <b>여러 번 불러도 안전하게</b> 짜여 있다(대상 조회 조건이 곧 "아직 처리되지
 * 않은 것" 이다). 그래도 동시에 두 인스턴스가 같은 대상을 집으면 이야기가 달라진다 — 순차 재실행이
 * 안전한 것과 <b>동시 실행이 안전한 것은 다른 문제</b>다. 이 잠금이 그 차이를 메운다.
 *
 * <h2>왜 어드바이저리 락인가</h2>
 *
 * PostgreSQL 이 이미 갖고 있는 기능이라 <b>새 의존성도, 잠금용 테이블도, 정리 배치도 없다.</b>
 * 잡은 인스턴스가 죽으면 <b>세션이 끊기면서</b> 잠금이 자동으로 풀린다 — 만료 시각을 들고 있는
 * 방식이 겪는 "죽은 인스턴스가 잡아 둔 잠금" 문제가 생기지 않는다.
 *
 * <p>다만 <b>세션이 끝나는 것과 {@link Connection#close()} 는 다르다.</b> 풀에서 꺼낸 연결을
 * 닫으면 풀로 돌아갈 뿐 PostgreSQL 세션은 그대로 살아 있고, <b>잠금도 그대로 남는다.</b>
 * 그래서 아래에서 {@code finally} 로 반드시 직접 푼다 — 닫히니까 풀리겠지에 기대지 않는다.
 * (이 사실은 테스트에서 드러났다. 풀어 주지 않은 테스트 하나가 뒤 테스트를 전부 막았다)
 *
 * <h2>왜 트랜잭션 스코프가 아닌가</h2>
 *
 * {@code pg_try_advisory_xact_lock} 을 쓰면 코드가 더 짧지만, 그러려면 <b>작업 전체를 트랜잭션
 * 하나로 감싸야 한다.</b> 그럴 수 없다 — {@code SettlementGenerationService.generateDue()} 는
 * <b>일부러 트랜잭션을 열지 않는다.</b> 박람회 하나하나가 자기 트랜잭션을 가져야 "한 건 실패에
 * 전부 롤백" 을 피할 수 있기 때문이다. 감싸는 순간 그 설계가 무너진다.
 *
 * <p>그래서 세션 스코프로 잡고 {@code finally} 에서 직접 푼다. 작업은 자기 트랜잭션 경계를
 * 그대로 유지한다.
 *
 * <h2>연결 하나를 붙들고 있다</h2>
 *
 * 잠금용 연결은 작업이 끝날 때까지 <b>아무 일도 안 하면서</b> 풀에서 빠져 있다. 작업이 쓰는
 * 연결과는 별개다. 여기 등록된 작업들은 초 단위로 끝나는 정리 작업이라 감당할 수 있는 비용이고,
 * 그 대가로 긴 트랜잭션을 열어 두지 않는다(스냅숏을 붙들어 vacuum 을 막지 않는다).
 */
@Slf4j
@Component
public class ScheduledJobRunner {

    /**
     * 어드바이저리 락 네임스페이스.
     *
     * <p>두 정수를 받는 형태를 쓴다. 다른 곳에서 어드바이저리 락을 쓰게 되더라도 네임스페이스가
     * 다르면 번호가 겹쳐도 서로를 막지 않는다.
     */
    static final int LOCK_NAMESPACE = 8425;

    private static final String TRY_LOCK = "SELECT pg_try_advisory_lock(?, ?)";
    private static final String UNLOCK = "SELECT pg_advisory_unlock(?, ?)";

    private final DataSource dataSource;

    public ScheduledJobRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 잠금을 잡은 경우에만 작업을 실행한다.
     *
     * <p>이미 다른 인스턴스가 실행 중이면 <b>기다리지 않고 건너뛴다.</b> 주기 작업이라 다음 차례에
     * 다시 온다 — 줄을 서면 인스턴스 수만큼 같은 작업이 연달아 도는 꼴이 된다.
     *
     * <p>작업이 던진 예외는 그대로 올려보낸다. 스케줄러 쪽에서 로그로 남아야 원인을 볼 수 있다.
     * 잠금은 예외가 나도 풀린다.
     *
     * @return 실행했으면 작업의 반환값, 건너뛰었으면 빈 값
     */
    public <T> Optional<T> runExclusively(ScheduledJob job, Supplier<T> task) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryLock(connection, job)) {
                log.debug("다른 인스턴스가 실행 중이라 건너뛴다 job={}", job);
                return Optional.empty();
            }
            try {
                return Optional.ofNullable(task.get());
            } finally {
                unlock(connection, job);
            }
        } catch (SQLException e) {
            // 잠금 자체가 실패한 경우다. 작업을 돌리지 않는다 — 확인 없이 도는 것보다 건너뛰는 편이 낫다.
            log.warn("작업 잠금을 처리하지 못했다 job={}", job, e);
            return Optional.empty();
        }
    }

    private boolean tryLock(Connection connection, ScheduledJob job) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(TRY_LOCK)) {
            statement.setInt(1, LOCK_NAMESPACE);
            statement.setInt(2, job.lockId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private void unlock(Connection connection, ScheduledJob job) {
        try (PreparedStatement statement = connection.prepareStatement(UNLOCK)) {
            statement.setInt(1, LOCK_NAMESPACE);
            statement.setInt(2, job.lockId());
            statement.execute();
        } catch (SQLException e) {
            // 여기까지 오는 경우는 연결 자체가 깨진 때다. 그러면 세션도 함께 끝나 서버가 잠금을
            // 놓아 준다. 작업은 이미 끝났으므로 실패로 만들지 않고 기록만 남긴다.
            log.warn("작업 잠금을 풀지 못했다 job={}", job, e);
        }
    }
}
