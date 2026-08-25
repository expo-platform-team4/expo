package com.expo.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.support.PostgresContainerConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 주기 작업이 <b>정말로 한 번만</b> 실행되는지 본다.
 *
 * <h2>왜 실제 DB 인가</h2>
 *
 * 검증 대상이 PostgreSQL 의 어드바이저리 락 그 자체다. mock 으로는 "잠금을 요청했다" 까지만
 * 확인되고, <b>두 번째 요청이 정말 거절되는지는 확인되지 않는다</b> — 그게 이 코드의 전부인데.
 *
 * <h2>트랜잭션을 걸지 않는다</h2>
 *
 * 다른 통합 테스트와 달리 {@code @Transactional} 이 없다. 이 잠금은 <b>세션 스코프</b>라
 * 테스트를 트랜잭션으로 감싸면 잠금의 생애가 테스트 트랜잭션에 묶여 실제 동작과 달라진다.
 * 대신 이 테스트는 테이블을 건드리지 않아 정리할 것이 없다.
 *
 * <h2>잠금은 반드시 직접 푼다</h2>
 *
 * 테스트가 잡은 잠금을 {@code finally} 로 푼다. <b>연결을 닫는 것으로는 안 풀린다</b> —
 * 풀에서 꺼낸 연결이라 닫으면 풀로 돌아갈 뿐 PostgreSQL 세션은 살아 있다. 처음에 이걸 빠뜨렸다가
 * 앞 테스트가 잠금을 물고 있어 뒤 테스트가 실패했다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class ScheduledJobRunnerIntegrationTest {

    private static final ScheduledJob JOB = ScheduledJob.BANNER_DISPLAY_SYNC;

    @Autowired private ScheduledJobRunner runner;
    @Autowired private DataSource dataSource;

    /** 아무도 안 잡고 있으면 실행하고 반환값을 돌려준다. */
    @Test
    void runsTaskAndReturnsItsResult() {
        Optional<String> result = runner.runExclusively(JOB, () -> "실행됨");

        assertThat(result).contains("실행됨");
    }

    /**
     * <b>이미 잡혀 있으면 실행하지 않는다.</b> 이 테스트가 이 클래스의 존재 이유다.
     *
     * <p>다른 연결에서 같은 잠금을 미리 잡아 둔다 — 다른 인스턴스가 이미 돌고 있는 상황과 같다.
     */
    @Test
    void skipsTaskWhenAnotherHolderHasTheLock() throws SQLException {
        AtomicInteger runCount = new AtomicInteger();

        try (Connection holder = dataSource.getConnection()) {
            assertThat(acquire(holder)).isTrue();
            try {
                Optional<String> result =
                        runner.runExclusively(
                                JOB,
                                () -> {
                                    runCount.incrementAndGet();
                                    return "실행됨";
                                });

                assertThat(result).isEmpty();
                assertThat(runCount).hasValue(0); // 기다렸다 도는 게 아니라 아예 건너뛴다
            } finally {
                release(holder);
            }
        }
    }

    /** 잠금이 풀리면 다음 차례에 다시 잡는다. 주기 작업이라 건너뛴 것은 다음에 돌면 된다. */
    @Test
    void runsAgainAfterTheLockIsReleased() throws SQLException {
        try (Connection holder = dataSource.getConnection()) {
            assertThat(acquire(holder)).isTrue();
            assertThat(runner.runExclusively(JOB, () -> "실행됨")).isEmpty();

            assertThat(release(holder)).isTrue();
            assertThat(runner.runExclusively(JOB, () -> "실행됨")).contains("실행됨");
        }
    }

    /**
     * <b>작업이 터져도 잠금은 풀린다.</b>
     *
     * <p>안 풀리면 그 인스턴스가 살아 있는 동안 <b>그 작업이 영영 안 돈다.</b> 게다가 조용하다 —
     * 예외는 한 번 로그에 남고 끝이며, 이후로는 매번 "다른 인스턴스가 실행 중" 으로 건너뛴다.
     */
    @Test
    void releasesLockWhenTaskThrows() {
        assertThatThrownBy(
                        () ->
                                runner.runExclusively(
                                        JOB,
                                        () -> {
                                            throw new IllegalStateException("작업 실패");
                                        }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(runner.runExclusively(JOB, () -> "실행됨")).contains("실행됨");
    }

    /** 작업이 다르면 서로 막지 않는다. 번호가 곧 잠금 식별자다. */
    @Test
    void differentJobsDoNotBlockEachOther() throws SQLException {
        try (Connection holder = dataSource.getConnection()) {
            assertThat(acquire(holder)).isTrue();
            try {
                assertThat(runner.runExclusively(ScheduledJob.SETTLEMENT_GENERATION, () -> "실행됨"))
                        .contains("실행됨");
            } finally {
                release(holder);
            }
        }
    }

    /** 잠금 번호는 서로 달라야 한다. 겹치면 두 작업이 이유 없이 서로를 막는다. */
    @Test
    void everyJobHasItsOwnLockId() {
        assertThat(ScheduledJob.values()).extracting(ScheduledJob::lockId).doesNotHaveDuplicates();
    }

    private boolean acquire(Connection connection) throws SQLException {
        return call(connection, "pg_try_advisory_lock");
    }

    private boolean release(Connection connection) throws SQLException {
        return call(connection, "pg_advisory_unlock");
    }

    private boolean call(Connection connection, String function) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT " + function + "(?, ?)")) {
            statement.setInt(1, ScheduledJobRunner.LOCK_NAMESPACE);
            statement.setInt(2, JOB.lockId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }
}
