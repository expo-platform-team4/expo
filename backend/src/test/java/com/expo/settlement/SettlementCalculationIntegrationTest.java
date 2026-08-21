package com.expo.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.settlement.dto.SettlementCalculationResult;
import com.expo.settlement.entity.SettlementItem;
import com.expo.settlement.entity.SettlementItemType;
import com.expo.settlement.repository.SettlementItemRepository;
import com.expo.settlement.service.SettlementCalculationService;
import com.expo.support.PostgresContainerConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 금액 집계를 <b>실제 PostgreSQL 위에서</b> 검증한다.
 *
 * <h2>왜 단위 테스트로는 부족한가</h2>
 *
 * 이 기능의 어려운 부분은 계산식이 아니라 <b>집계 쿼리</b>다.
 *
 * <ul>
 *   <li>결제를 박람회로 잇는 경로가 맞나 — 주문 → 항목 → 상품 → 박람회
 *   <li>부스는 다섯 단계를 탄다 — 결제 → 주문 → 배정 → 참여업체 → 박람회
 *   <li>상태 조건이 맞나 — {@code DONE} 만, {@code COMPLETED} 만, {@code APPROVED} 만
 *   <li>다른 박람회의 매출이 섞이지 않나
 * </ul>
 *
 * <p>매퍼를 mock 하면 이 중 <b>어느 것도 검증되지 않는다.</b> 금액이 틀리면 돈이 잘못 나가는
 * 기능이라, 숫자를 넣고 숫자를 확인한다.
 *
 * <h2>격리</h2>
 *
 * {@code @Transactional} 이라 각 테스트가 끝나면 롤백된다. 컨테이너는 클래스 전체에서 하나다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class SettlementCalculationIntegrationTest {

    @Autowired private SettlementCalculationService calculationService;
    @Autowired private SettlementItemRepository settlementItemRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long expoId;
    private Long otherExpoId;
    private Long settlementId;
    private Long clientUserId;

    @BeforeEach
    void seed() {
        clientUserId = insertClient("host@espotic.com", "주최사");
        expoId = insertExpo(clientUserId, "정산 검증 박람회");
        otherExpoId = insertExpo(clientUserId, "다른 박람회");
        settlementId = insertSettlement(expoId, clientUserId);
    }

    // ------------------------------------------------------------------
    // 티켓
    // ------------------------------------------------------------------

    /**
     * 판매원금과 예매 수수료가 <b>따로</b> 집계된다.
     *
     * <p>10,000원짜리 2매 = 20,000원, 수수료 3% = 600원. 정산은 이 3% 를 계산하지 않고
     * {@code ticket_payments} 에 기록된 값을 합산할 뿐이다.
     */
    @Test
    void aggregatesTicketPrincipalAndBookingFeeSeparately() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        insertTicketPayment(orderId, "DONE", 20_000, 600);

        SettlementCalculationResult result = calculationService.calculate(settlementId);

        assertThat(result.grossTicketSalesAmount()).isEqualByComparingTo("20000");
        assertThat(result.bookingFeeGrossAmount()).isEqualByComparingTo("600");
    }

    /** <b>예매 수수료는 송금액에 들어가지 않는다.</b> 구매자가 원금 위에 추가로 낸 플랫폼 몫이다. */
    @Test
    void bookingFeeIsNotPaidOutToHost() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        insertTicketPayment(orderId, "DONE", 20_000, 600);

        SettlementCalculationResult result = calculationService.calculate(settlementId);

        assertThat(result.remittanceDueAmount()).isEqualByComparingTo("20000");
        assertThat(result.bookingFeeNetAmount()).isEqualByComparingTo("600");
    }

    /** 결제가 완료되지 않은 것은 매출이 아니다. */
    @Test
    void ignoresPaymentsThatAreNotDone() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        insertTicketPayment(orderId, "READY", 20_000, 600);
        Long orderId2 = insertPaidTicketOrder(expoId, 30_000, 900);
        insertTicketPayment(orderId2, "CANCELED", 30_000, 900);

        assertThat(calculationService.calculate(settlementId).grossTicketSalesAmount())
                .isEqualByComparingTo("0");
    }

    /** 환불은 원금과 수수료가 <b>서로 다른 계정에서</b> 빠진다. */
    @Test
    void subtractsRefundFromMatchingBucket() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        Long paymentId = insertTicketPayment(orderId, "DONE", 20_000, 600);
        insertTicketRefund(orderId, paymentId, "COMPLETED", 10_000, 300);

        SettlementCalculationResult result = calculationService.calculate(settlementId);

        assertThat(result.netTicketSalesAmount()).isEqualByComparingTo("10000");
        assertThat(result.bookingFeeNetAmount()).isEqualByComparingTo("300");
        assertThat(result.remittanceDueAmount()).isEqualByComparingTo("10000");
    }

    /**
     * 완료되지 않은 환불은 빼지 않는다.
     *
     * <p>{@code FAILED} 환불까지 차감하면 나가지도 않은 돈을 주최사 몫에서 뺀다.
     */
    @Test
    void ignoresRefundsThatAreNotCompleted() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        Long paymentId = insertTicketPayment(orderId, "DONE", 20_000, 600);
        insertTicketRefund(orderId, paymentId, "FAILED", 20_000, 600);

        assertThat(calculationService.calculate(settlementId).netTicketSalesAmount())
                .isEqualByComparingTo("20000");
    }

    /** <b>다른 박람회의 매출이 섞이면 안 된다.</b> 조인 경로가 틀리면 여기서 드러난다. */
    @Test
    void doesNotMixOtherExposSales() {
        Long mine = insertPaidTicketOrder(expoId, 20_000, 600);
        insertTicketPayment(mine, "DONE", 20_000, 600);
        Long theirs = insertPaidTicketOrder(otherExpoId, 99_000, 2_970);
        insertTicketPayment(theirs, "DONE", 99_000, 2_970);

        assertThat(calculationService.calculate(settlementId).grossTicketSalesAmount())
                .isEqualByComparingTo("20000");
    }

    // ------------------------------------------------------------------
    // 재계산
    // ------------------------------------------------------------------

    /**
     * 두 번 계산해도 <b>누적되지 않는다.</b>
     *
     * <p>항목을 지우지 않고 다시 넣으면 상세 화면의 합이 실제 금액의 두 배가 된다.
     */
    @Test
    void recalculationOverwritesInsteadOfAccumulating() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        insertTicketPayment(orderId, "DONE", 20_000, 600);

        calculationService.calculate(settlementId);
        SettlementCalculationResult second = calculationService.calculate(settlementId);

        assertThat(second.grossTicketSalesAmount()).isEqualByComparingTo("20000");

        List<SettlementItem> items =
                settlementItemRepository.findBySettlementIdOrderByIdAsc(settlementId);
        assertThat(items).hasSize(2); // TICKET_SALE, BOOKING_FEE
    }

    /** 항목은 <b>합하면 송금액이 되도록</b> 부호를 맞춰 남긴다. 환불은 음수다. */
    @Test
    void writesItemsWithSignsThatSumToPayout() {
        Long orderId = insertPaidTicketOrder(expoId, 20_000, 600);
        Long paymentId = insertTicketPayment(orderId, "DONE", 20_000, 600);
        insertTicketRefund(orderId, paymentId, "COMPLETED", 5_000, 150);

        calculationService.calculate(settlementId);

        Map<SettlementItemType, SettlementItem> byType =
                settlementItemRepository.findBySettlementIdOrderByIdAsc(settlementId).stream()
                        .collect(
                                Collectors.toMap(SettlementItem::getItemType, Function.identity()));

        assertThat(byType.get(SettlementItemType.TICKET_SALE).getAmount())
                .isEqualByComparingTo("20000");
        assertThat(byType.get(SettlementItemType.TICKET_REFUND).getAmount())
                .isEqualByComparingTo("-5000");

        // 송금 대상 항목만 더하면 송금액이 나온다
        BigDecimal payout =
                byType.values().stream()
                        .filter(SettlementItem::isIncludedInRemittance)
                        .map(SettlementItem::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(payout).isEqualByComparingTo("15000");
    }

    /** 매출이 하나도 없으면 전부 0 이고 항목도 없다. */
    @Test
    void producesZerosWhenNothingSold() {
        SettlementCalculationResult result = calculationService.calculate(settlementId);

        assertThat(result.remittanceDueAmount()).isEqualByComparingTo("0");
        assertThat(settlementItemRepository.findBySettlementIdOrderByIdAsc(settlementId)).isEmpty();
    }

    // ------------------------------------------------------------------
    // 시드 헬퍼 — 스키마가 요구하는 NOT NULL 을 최소로 채운다
    // ------------------------------------------------------------------

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

    private Long insertExpo(Long hostClientId, String title) {
        return jdbc.queryForObject(
                """
                INSERT INTO expos (host_client_id, title, description, region_code,
                       event_start_at, event_end_at, sales_start_at, sales_end_at,
                       review_status, visibility_status, event_status)
                VALUES (?, ?, 'x', '11',
                        now() - interval '12 day', now() - interval '10 day',
                        now() - interval '40 day', now() - interval '10 day',
                        'APPROVED', 'PUBLIC', 'ENDED')
                RETURNING id
                """,
                Long.class,
                hostClientId,
                title);
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

    /** 주문 → 항목 → 상품 → 박람회 사슬을 만든다. 집계가 이 경로를 탄다. */
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

    private Long insertTicketPayment(Long orderId, String status, int subtotal, int bookingFee) {
        return jdbc.queryForObject(
                """
                INSERT INTO ticket_payments (ticket_order_id, payment_key, pg_order_id, method,
                       status, requested_amount, approved_amount, ticket_subtotal_amount,
                       booking_fee_amount, idempotency_key, approved_at)
                VALUES (?, ?, ?, 'CARD', ?, ?, ?, ?, ?, ?, now())
                RETURNING id
                """,
                Long.class,
                orderId,
                "PAY-" + System.nanoTime(),
                "PG-" + System.nanoTime(),
                status,
                subtotal + bookingFee,
                subtotal + bookingFee,
                subtotal,
                bookingFee,
                "IDEM-" + System.nanoTime());
    }

    private void insertTicketRefund(
            Long orderId, Long paymentId, String status, int refundTicket, int refundFee) {
        jdbc.update(
                """
                INSERT INTO ticket_refunds (ticket_order_id, ticket_payment_id,
                       refund_ticket_amount, refund_booking_fee_amount, refund_amount,
                       status, requested_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, now(), now())
                """,
                orderId,
                paymentId,
                refundTicket,
                refundFee,
                refundTicket + refundFee,
                status);
    }
}
