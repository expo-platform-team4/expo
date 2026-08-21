package com.expo.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.checkin.service.RowLockTimeout;
import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.support.PostgresContainerConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 잠금 대기 상한이 <b>실제 PostgreSQL 에서</b> 걸리는지 확인한다 (이슈 #74).
 *
 * <p>이건 mock 으로는 확인할 수 없다. {@code SET LOCAL lock_timeout} 이 통하는지, 초과 시 올라오는 SQLSTATE {@code
 * 55P03} 이 우리 {@link ErrorCode#RESOURCE_BUSY} 로 번역되는지는 진짜 DB 와 진짜 경합이 있어야 드러난다.
 *
 * <p>테스트에서는 상한을 아주 짧게 준다. 기본값(3초)으로 두면 테스트가 그만큼 멈춘다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@TestPropertySource(properties = "app.check-in.lock-timeout-millis=300")
class RowLockTimeoutIntegrationTest {

    @Autowired private RowLockTimeout rowLockTimeout;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    /**
     * 다른 커넥션이 행을 잡고 있는 동안 잠금을 시도하면 상한에서 끊겨야 한다.
     *
     * <p>상한이 없으면(PostgreSQL 기본값 0) 이 테스트는 <b>영원히 끝나지 않는다.</b>
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void translatesLockTimeoutIntoResourceBusy() throws Exception {
        Long orderId = insertOrder();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> holdRowLock(orderId, locked, release));
        holder.start();
        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

        try {
            assertThatThrownBy(() -> lockInOwnTransaction(orderId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_BUSY);
        } finally {
            release.countDown();
            holder.join(10_000);
        }
    }

    /** 경합이 없으면 그대로 통과해야 한다. 상한을 걸었다고 평상시 요청이 느려지거나 막히면 안 된다. */
    @Test
    @Transactional
    void passesThroughWhenNothingIsHoldingTheRow() {
        Long orderId = insertOrder();

        Long locked =
                rowLockTimeout.runWithTimeout(
                        () ->
                                jdbc.queryForObject(
                                        "SELECT id FROM ticket_orders WHERE id = ? FOR UPDATE",
                                        Long.class,
                                        orderId));

        assertThat(locked).isEqualTo(orderId);
    }

    /**
     * 진짜 트랜잭션 안에서 잠금을 시도한다.
     *
     * <p>{@code @Transactional} 을 붙인 private 메서드를 같은 클래스에서 부르면 프록시를 타지 않아 트랜잭션이 열리지 않는다.
     * {@code SET LOCAL} 은 트랜잭션 밖에서 의미가 없으므로 여기서는 {@link TransactionTemplate} 으로 직접 연다.
     */
    private void lockInOwnTransaction(Long orderId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                rowLockTimeout.runWithTimeout(
                                        () ->
                                                jdbc.queryForObject(
                                                        "SELECT id FROM ticket_orders WHERE id = ?"
                                                                + " FOR UPDATE",
                                                        Long.class,
                                                        orderId)));
    }

    /** 별도 커넥션에서 행을 잡고 신호를 받을 때까지 붙잡고 있는다. */
    private void holdRowLock(Long orderId, CountDownLatch locked, CountDownLatch release) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement =
                    connection.prepareStatement(
                            "SELECT id FROM ticket_orders WHERE id = ? FOR UPDATE")) {
                statement.setLong(1, orderId);
                statement.executeQuery().next();
            }
            locked.countDown();
            release.await(10, TimeUnit.SECONDS);
            connection.rollback();
        } catch (Exception e) {
            locked.countDown();
            throw new IllegalStateException(e);
        }
    }

    private Long insertOrder() {
        return jdbc.queryForObject(
                """
                INSERT INTO ticket_orders (
                    order_number, orderer_type, ticket_subtotal_amount,
                    booking_fee_amount, total_amount, total_quantity, status)
                VALUES (?, 'GUEST', 10000, 300, 10300, 1, 'PAID')
                RETURNING id
                """,
                Long.class,
                "ORD-LOCK-" + System.nanoTime());
    }
}
