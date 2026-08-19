package com.expo.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.settlement.dto.RemittanceRecordRequest;
import com.expo.settlement.dto.RemittanceRecordResult;
import com.expo.settlement.dto.SettlementConfirmResult;
import com.expo.settlement.service.SettlementCalculationService;
import com.expo.settlement.service.SettlementConfirmService;
import com.expo.support.PostgresContainerConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 상태 기계를 <b>끝까지</b> 통과시킨다 (D-API-014 · 015 · 016).
 *
 * <pre>
 * WAITING → CALCULATED → CONFIRMED → REMITTED
 * </pre>
 *
 * <p>여기서 지키는 것은 <b>순서</b>다. 계산 전 확정, 확정 전 송금, 확정 후 재계산 — 셋 다 막혀야
 * 한다. 하나라도 뚫리면 <b>보낸 돈과 기록이 어긋난다.</b>
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class SettlementLifecycleIntegrationTest {

    private static final Long ADMIN_ID = 1L;

    @Autowired private SettlementCalculationService calculationService;
    @Autowired private SettlementConfirmService confirmService;
    @Autowired private JdbcTemplate jdbc;

    private Long settlementId;
    private Long adminUserId;

    @BeforeEach
    void seed() {
        Long host = insertClient("host@espotic.com", "주최사");
        adminUserId = insertAdmin();
        Long expoId = insertExpo(host);
        settlementId = insertSettlement(expoId, host);

        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        insertTicketPayment(orderId, 20_000, 600);
    }

    private RemittanceRecordRequest remitted(String amount) {
        return new RemittanceRecordRequest("REMITTED", new BigDecimal(amount), "TRX-1", null);
    }

    // ------------------------------------------------------------------
    // 정상 흐름
    // ------------------------------------------------------------------

    /** 계산 → 확정 → 송금이 끝까지 이어진다. */
    @Test
    void runsWholeLifecycle() {
        calculationService.calculate(settlementId);

        SettlementConfirmResult confirmed = confirmService.confirm(settlementId, adminUserId);
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.remittanceDueAmount()).isEqualByComparingTo("20000");
        assertThat(confirmed.confirmedBy()).isEqualTo(adminUserId);

        RemittanceRecordResult remittance =
                confirmService.recordRemittance(settlementId, remitted("20000"), adminUserId);

        assertThat(remittance.settlementStatus()).isEqualTo("REMITTED");
        assertThat(remittance.amountMatchesDue()).isTrue();
        assertThat(remittance.remittedAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // 순서 방어
    // ------------------------------------------------------------------

    /**
     * <b>계산하지 않고 확정할 수 없다.</b>
     *
     * <p>{@code WAITING} 은 금액이 전부 0 이다. 확정하면 재계산이 막히므로 <b>0원으로 굳는다.</b>
     */
    @Test
    void cannotConfirmBeforeCalculating() {
        assertThatThrownBy(() -> confirmService.confirm(settlementId, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_CONFIRMABLE);
    }

    /** <b>확정하지 않고 송금 결과를 기록할 수 없다.</b> 금액이 아직 바뀔 수 있다. */
    @Test
    void cannotRecordRemittanceBeforeConfirming() {
        calculationService.calculate(settlementId);

        assertThatThrownBy(
                        () ->
                                confirmService.recordRemittance(
                                        settlementId, remitted("20000"), adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_REMITTABLE);
    }

    /** <b>확정한 뒤에는 재계산할 수 없다.</b> 송금한 돈과 기록이 어긋난다. */
    @Test
    void cannotRecalculateAfterConfirming() {
        calculationService.calculate(settlementId);
        confirmService.confirm(settlementId, adminUserId);

        assertThatThrownBy(() -> calculationService.calculate(settlementId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_RECALCULABLE);
    }

    /** 두 번 확정할 수 없다. 확정은 한 번뿐이다. */
    @Test
    void cannotConfirmTwice() {
        calculationService.calculate(settlementId);
        confirmService.confirm(settlementId, adminUserId);

        assertThatThrownBy(() -> confirmService.confirm(settlementId, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_CONFIRMABLE);
    }

    // ------------------------------------------------------------------
    // 송금 기록
    // ------------------------------------------------------------------

    /**
     * 금액이 달라도 <b>기록은 남긴다.</b>
     *
     * <p>이미 일어난 이체를 적는 API 라 거절하면 사실이 사라진다. 대신 불일치를 알린다.
     */
    @Test
    void recordsMismatchedAmountButFlagsIt() {
        calculationService.calculate(settlementId);
        confirmService.confirm(settlementId, adminUserId);

        RemittanceRecordResult result =
                confirmService.recordRemittance(settlementId, remitted("19500"), adminUserId);

        assertThat(result.amountMatchesDue()).isFalse();
        assertThat(result.remittedAmount()).isEqualByComparingTo("19500");
        assertThat(result.settlementStatus()).isEqualTo("REMITTED");
    }

    /**
     * 실패한 이체는 {@code REMITTANCE_PENDING} 으로 남고 <b>완료 시각이 없다.</b>
     *
     * <p>실패에 완료 시각을 남기면 "언제 송금됐나" 에 거짓을 답한다.
     */
    @Test
    void failedRemittanceLeavesSettlementPending() {
        calculationService.calculate(settlementId);
        confirmService.confirm(settlementId, adminUserId);

        RemittanceRecordResult result =
                confirmService.recordRemittance(
                        settlementId,
                        new RemittanceRecordRequest("FAILED", null, null, "계좌 오류"),
                        adminUserId);

        assertThat(result.settlementStatus()).isEqualTo("REMITTANCE_PENDING");
        assertThat(result.remittedAt()).isNull();
        // 실패에는 금액이 없다. 이것을 "불일치" 로 알리면 진짜 불일치가 묻힌다
        assertThat(result.amountMatchesDue()).isTrue();
    }

    /** 실패한 뒤 다시 보낼 수 있다. <b>이전 기록은 지우지 않는다</b> — 왜 늦었나가 남아야 한다. */
    @Test
    void canRetryRemittanceAfterFailure() {
        calculationService.calculate(settlementId);
        confirmService.confirm(settlementId, adminUserId);
        confirmService.recordRemittance(
                settlementId,
                new RemittanceRecordRequest("FAILED", null, null, "계좌 오류"),
                adminUserId);

        RemittanceRecordResult retry =
                confirmService.recordRemittance(settlementId, remitted("20000"), adminUserId);

        assertThat(retry.settlementStatus()).isEqualTo("REMITTED");
        Integer count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM remittances WHERE settlement_id = ?",
                        Integer.class,
                        settlementId);
        assertThat(count).isEqualTo(2);
    }

    /** 알 수 없는 송금 상태는 400 이다. DB CHECK 에 걸리기 전에 막는다. */
    @Test
    void rejectsUnknownRemittanceStatus() {
        calculationService.calculate(settlementId);
        confirmService.confirm(settlementId, adminUserId);

        assertThatThrownBy(
                        () ->
                                confirmService.recordRemittance(
                                        settlementId,
                                        new RemittanceRecordRequest("DONE", null, null, null),
                                        adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REMITTANCE_STATUS);
    }

    /**
     * <b>보류 중에 재계산해도 보류가 풀리지 않는다.</b>
     *
     * <p>재계산이 상태를 무조건 {@code CALCULATED} 로 바꾸면, 분쟁·조사로 멈춰 둔 정산이 조용히
     * 진행 가능해진다. 그 뒤 확정·송금까지 그대로 흘러가 <b>멈춰 둔 이유가 남아 있는데 돈이
     * 나간다.</b> 금액만 갱신하고 상태는 그대로여야 한다.
     */
    @Test
    void recalculationDoesNotReleaseHold() {
        jdbc.update("UPDATE settlements SET status = 'ON_HOLD' WHERE id = ?", settlementId);

        assertThat(calculationService.calculate(settlementId).status()).isEqualTo("ON_HOLD");
        assertThat(calculationService.calculate(settlementId).remittanceDueAmount())
                .isEqualByComparingTo("20000"); // 금액은 갱신된다

        // 보류가 유지되므로 확정으로 넘어갈 수 없다
        assertThatThrownBy(() -> confirmService.confirm(settlementId, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_CONFIRMABLE);
    }

    /** 없는 정산은 404 다. */
    @Test
    void confirmingMissingSettlementIsNotFound() {
        assertThatThrownBy(() -> confirmService.confirm(999_999L, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    // ------------------------------------------------------------------
    // 시드
    // ------------------------------------------------------------------

    private Long insertAdmin() {
        return jdbc.queryForObject(
                """
                INSERT INTO users (email, nickname, role, account_status)
                VALUES ('admin@espotic.com', '관리자', 'ADMIN', 'ACTIVE') RETURNING id
                """,
                Long.class);
    }

    private Long insertClient(String email, String company) {
        Long userId =
                jdbc.queryForObject(
                        """
                        INSERT INTO users (email, nickname, role, account_status)
                        VALUES (?, ?, 'CLIENT', 'ACTIVE') RETURNING id
                        """,
                        Long.class,
                        email,
                        company);
        jdbc.update(
                """
                INSERT INTO client_profiles (user_id, company_name, business_number,
                       representative_name, business_address)
                VALUES (?, ?, ?, '대표', '서울')
                """,
                userId,
                company,
                "%010d".formatted(userId));
        return userId;
    }

    private Long insertExpo(Long hostClientId) {
        return jdbc.queryForObject(
                """
                INSERT INTO expos (host_client_id, title, description, region_code,
                       event_start_at, event_end_at, sales_start_at, sales_end_at,
                       review_status, visibility_status, event_status)
                VALUES (?, '정산 흐름 박람회', 'x', '11',
                        now() - interval '12 day', now() - interval '10 day',
                        now() - interval '40 day', now() - interval '10 day',
                        'APPROVED', 'PUBLIC', 'ENDED')
                RETURNING id
                """,
                Long.class,
                hostClientId);
    }

    private Long insertSettlement(Long expoId, Long hostClientId) {
        return jdbc.queryForObject(
                """
                INSERT INTO settlements (expo_id, host_client_id, settlement_due_at, status)
                VALUES (?, ?, now() + interval '4 day', 'WAITING') RETURNING id
                """,
                Long.class,
                expoId,
                hostClientId);
    }

    private Long insertPaidTicketOrder(Long expoId, int subtotal, int bookingFee) {
        Long productId =
                jdbc.queryForObject(
                        """
                        INSERT INTO ticket_products (expo_id, name, price,
                               sales_start_at, sales_end_at, status)
                        VALUES (?, '티켓', ?, now() - interval '40 day',
                                now() - interval '10 day', 'ON_SALE')
                        RETURNING id
                        """,
                        Long.class,
                        expoId,
                        subtotal);
        Long orderId =
                jdbc.queryForObject(
                        """
                        INSERT INTO ticket_orders (order_number, orderer_type,
                               ticket_subtotal_amount, booking_fee_amount, total_amount,
                               total_quantity, status, paid_at)
                        VALUES (?, 'GUEST', ?, ?, ?, 1, 'PAID', now())
                        RETURNING id
                        """,
                        Long.class,
                        "ORD-" + System.nanoTime(),
                        subtotal,
                        bookingFee,
                        subtotal + bookingFee);
        jdbc.update(
                """
                INSERT INTO ticket_order_items (ticket_order_id, ticket_product_id, quantity,
                       unit_price, item_subtotal_amount)
                VALUES (?, ?, 1, ?, ?)
                """,
                orderId,
                productId,
                subtotal,
                subtotal);
        return orderId;
    }

    private void insertTicketPayment(Long orderId, int subtotal, int bookingFee) {
        jdbc.update(
                """
                INSERT INTO ticket_payments (ticket_order_id, payment_key, pg_order_id, method,
                       status, requested_amount, approved_amount, ticket_subtotal_amount,
                       booking_fee_amount, idempotency_key, approved_at)
                VALUES (?, ?, ?, 'CARD', 'DONE', ?, ?, ?, ?, ?, now())
                """,
                orderId,
                "PAY-" + System.nanoTime(),
                "PG-" + System.nanoTime(),
                subtotal + bookingFee,
                subtotal + bookingFee,
                subtotal,
                bookingFee,
                "IDEM-" + System.nanoTime());
    }
}
