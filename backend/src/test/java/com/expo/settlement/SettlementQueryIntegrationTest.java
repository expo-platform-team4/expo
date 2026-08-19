package com.expo.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import com.expo.settlement.dto.AdminSettlementResponse;
import com.expo.settlement.dto.ClientSettlementDetail;
import com.expo.settlement.dto.ClientSettlementResponse;
import com.expo.settlement.dto.SettlementPage;
import com.expo.settlement.service.SettlementCalculationService;
import com.expo.settlement.service.SettlementQueryService;
import com.expo.support.PostgresContainerConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 조회의 <b>권한 경계</b>를 실제 DB 로 확인한다.
 *
 * <p>세 API 중 클라이언트 둘은 "역할이 CLIENT 인가" 만으로는 부족하다 — <b>다른 주최사도
 * CLIENT</b> 다. 실제 소유 판정은 쿼리 조건이 하고, 그것이 맞는지는 <b>주최사 둘을 만들어야</b>
 * 확인된다. mock 으로는 조건이 붙었는지조차 알 수 없다.
 *
 * <p>뷰({@code v_client_dashboard_settlements}, {@code v_admin_settlement_status})를 읽으므로
 * 뷰가 깨져도 여기서 드러난다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class SettlementQueryIntegrationTest {

    @Autowired private SettlementQueryService queryService;
    @Autowired private SettlementCalculationService calculationService;
    @Autowired private JdbcTemplate jdbc;

    private Long hostA;
    private Long hostB;
    private Long settlementA;
    private Long settlementB;

    @BeforeEach
    void seed() {
        hostA = insertClient("a@espotic.com", "가나 주최사");
        hostB = insertClient("b@espotic.com", "다라 주최사");

        Long expoA = insertExpo(hostA, "A 박람회");
        Long expoB = insertExpo(hostB, "B 박람회");

        settlementA = insertSettlement(expoA, hostA);
        settlementB = insertSettlement(expoB, hostB);
    }

    // ------------------------------------------------------------------
    // D-API-011 목록
    // ------------------------------------------------------------------

    /** <b>내 정산만 나온다.</b> 다른 주최사 것이 섞이면 안 된다. */
    @Test
    void listReturnsOnlyMySettlements() {
        SettlementPage<ClientSettlementResponse> page =
                queryService.findMySettlements(hostA, 0, 20);

        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items())
                .singleElement()
                .satisfies(item -> assertThat(item.settlementId()).isEqualTo(settlementA));
    }

    /** 정산이 없는 주최사는 빈 목록이다. 예외가 아니다. */
    @Test
    void listIsEmptyForHostWithoutSettlement() {
        Long hostC = insertClient("c@espotic.com", "마바 주최사");

        assertThat(queryService.findMySettlements(hostC, 0, 20).items()).isEmpty();
    }

    /** 페이지 값이 이상해도 안전하다. 큰 페이지에서 offset 오버플로가 나면 500 이 된다. */
    @Test
    void listClampsPagingValues() {
        SettlementPage<ClientSettlementResponse> page =
                queryService.findMySettlements(hostA, Integer.MAX_VALUE, 9_999);

        assertThat(page.size()).isEqualTo(100);
        assertThat(page.items()).isEmpty(); // 그런 페이지는 없다. 터지지는 않는다
    }

    // ------------------------------------------------------------------
    // D-API-012 상세
    // ------------------------------------------------------------------

    /** 상세에 금액 구성 항목이 함께 온다. 이것이 "티켓/부스 구분" 이다. */
    @Test
    void detailIncludesBreakdownItems() {
        Long orderId = insertPaidTicketOrder(settlementExpoId(settlementA), 20_000, 600);
        insertTicketPayment(orderId, 20_000, 600);
        calculationService.calculate(settlementA);

        ClientSettlementDetail detail = queryService.findMySettlement(settlementA, hostA);

        assertThat(detail.settlement().remittanceDueAmount()).isEqualByComparingTo("20000");
        assertThat(detail.items())
                .extracting(item -> item.itemType())
                .containsExactlyInAnyOrder("TICKET_SALE", "BOOKING_FEE");
    }

    /** 예매 수수료 항목은 <b>정산금에 포함되지 않는다</b>고 표시된다. */
    @Test
    void bookingFeeItemIsMarkedAsNotPaidOut() {
        Long orderId = insertPaidTicketOrder(settlementExpoId(settlementA), 20_000, 600);
        insertTicketPayment(orderId, 20_000, 600);
        calculationService.calculate(settlementA);

        ClientSettlementDetail detail = queryService.findMySettlement(settlementA, hostA);

        assertThat(detail.items())
                .filteredOn(item -> "BOOKING_FEE".equals(item.itemType()))
                .singleElement()
                .satisfies(item -> assertThat(item.includedInRemittance()).isFalse());
    }

    /**
     * <b>남의 정산은 404 다.</b>
     *
     * <p>403 이 아닌 것이 의도다. 403 을 주면 "그 ID 의 정산이 존재한다" 는 사실이 새어 나간다.
     */
    @Test
    void detailOfAnotherHostIsNotFound() {
        assertThatThrownBy(() -> queryService.findMySettlement(settlementB, hostA))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    /** 아예 없는 정산도 같은 404 다. 존재 여부가 구분되지 않아야 한다. */
    @Test
    void detailOfMissingSettlementIsNotFound() {
        assertThatThrownBy(() -> queryService.findMySettlement(999_999L, hostA))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    // ------------------------------------------------------------------
    // D-API-013 관리자
    // ------------------------------------------------------------------

    /** 관리자는 <b>전부</b> 본다. 업체명도 함께 온다. */
    @Test
    void adminSeesEverySettlementWithCompanyName() {
        SettlementPage<AdminSettlementResponse> page =
                queryService.searchSettlements(null, null, null, null, 0, 20);

        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.items())
                .extracting(AdminSettlementResponse::companyName)
                .containsExactlyInAnyOrder("가나 주최사", "다라 주최사");
    }

    /** 주최사로 좁힐 수 있다. */
    @Test
    void adminCanFilterByHost() {
        SettlementPage<AdminSettlementResponse> page =
                queryService.searchSettlements(null, hostB, null, null, 0, 20);

        assertThat(page.items())
                .singleElement()
                .satisfies(item -> assertThat(item.settlementId()).isEqualTo(settlementB));
    }

    /** 상태로 좁힐 수 있다. 재계산한 것만 CALCULATED 다. */
    @Test
    void adminCanFilterByStatus() {
        calculationService.calculate(settlementA);

        assertThat(queryService.searchSettlements("CALCULATED", null, null, null, 0, 20).items())
                .singleElement()
                .satisfies(item -> assertThat(item.settlementId()).isEqualTo(settlementA));
        assertThat(queryService.searchSettlements("WAITING", null, null, null, 0, 20).totalCount())
                .isEqualTo(1);
    }

    /** 기한으로 좁힐 수 있다. 임박한 것을 먼저 처리하려는 용도다. */
    @Test
    void adminCanFilterByDueDate() {
        Instant farFuture = Instant.now().plus(365, ChronoUnit.DAYS);

        assertThat(queryService.searchSettlements(null, null, null, farFuture, 0, 20).totalCount())
                .isEqualTo(2);
        assertThat(
                        queryService
                                .searchSettlements(
                                        null,
                                        null,
                                        null,
                                        Instant.now().minus(1, ChronoUnit.DAYS),
                                        0,
                                        20)
                                .totalCount())
                .isZero();
    }

    // ------------------------------------------------------------------
    // 시드
    // ------------------------------------------------------------------

    private Long settlementExpoId(Long settlementId) {
        return jdbc.queryForObject(
                "SELECT expo_id FROM settlements WHERE id = ?", Long.class, settlementId);
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
