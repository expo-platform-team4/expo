package com.expo.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.support.PostgresContainerConfig;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중복 발권을 <b>DB 가</b> 막는지 확인한다 (이슈 #75).
 *
 * <p>애플리케이션 방어({@code FOR UPDATE} + {@code countIssuedTickets})는 두 전제에 기대고 있다 — 잠금과 개수 조회의
 * 순서, 그리고 {@code READ COMMITTED} 격리 수준. <b>둘 중 하나를 어겨도 모든 단위 테스트가 통과한다.</b> 그래서 마지막 방어선을 DB 에
 * 두었고, 그것이 실제로 서 있는지는 진짜 PostgreSQL 로만 확인할 수 있다.
 *
 * <p>제약은 {@code ticket_access_tokens} 에 걸려 있다. 발권이 주문당 {@code ORDER_VIEW} 토큰을 정확히 하나 만들고 그
 * 저장이 티켓 저장과 같은 트랜잭션이라, 토큰이 막히면 티켓도 함께 롤백된다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class DuplicateIssuanceConstraintTest {

    @Autowired private JdbcTemplate jdbc;

    private Long orderId;

    @BeforeEach
    void setUp() {
        orderId =
                jdbc.queryForObject(
                        """
                        INSERT INTO ticket_orders (
                            order_number, orderer_type, ticket_subtotal_amount,
                            booking_fee_amount, total_amount, total_quantity, status)
                        VALUES (?, 'GUEST', 10000, 300, 10300, 1, 'PAID')
                        RETURNING id
                        """,
                        Long.class,
                        "ORD-" + System.nanoTime());
    }

    @Test
    void blocksASecondActiveOrderViewTokenForTheSameOrder() {
        insertToken("hash-first", "ORDER_VIEW", "ACTIVE");

        assertThatThrownBy(() -> insertToken("hash-second", "ORDER_VIEW", "ACTIVE"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    /** 알림 재발송은 살아 있는 토큰을 끊고 새로 발급한다. 그 흐름이 막히면 안 된다. */
    @Test
    void allowsANewTokenOnceThePreviousOneIsRevoked() {
        insertToken("hash-old", "ORDER_VIEW", "ACTIVE");
        jdbc.update(
                "UPDATE ticket_access_tokens SET status = 'REVOKED', revoked_at = now()"
                        + " WHERE ticket_order_id = ?",
                orderId);

        assertThatCode(() -> insertToken("hash-new", "ORDER_VIEW", "ACTIVE"))
                .doesNotThrowAnyException();
        assertThat(countActiveOrderViewTokens()).isEqualTo(1);
    }

    /** 제약은 조회 링크에만 건다. QR 토큰은 티켓마다 하나라 주문당 여러 건이 정상이다. */
    @Test
    void doesNotConstrainQrViewTokens() {
        insertToken("hash-qr-1", "QR_VIEW", "ACTIVE");

        assertThatCode(() -> insertToken("hash-qr-2", "QR_VIEW", "ACTIVE"))
                .doesNotThrowAnyException();
    }

    private void insertToken(String tokenHash, String scope, String status) {
        jdbc.update(
                """
                INSERT INTO ticket_access_tokens (
                    ticket_order_id, token_hash, scope, status, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                orderId,
                tokenHash,
                scope,
                status,
                java.sql.Timestamp.from(Instant.now().plusSeconds(86400)));
    }

    private Integer countActiveOrderViewTokens() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM ticket_access_tokens"
                        + " WHERE ticket_order_id = ? AND scope = 'ORDER_VIEW' AND status = 'ACTIVE'",
                Integer.class,
                orderId);
    }
}
