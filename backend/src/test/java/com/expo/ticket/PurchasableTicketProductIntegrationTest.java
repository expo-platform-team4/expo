package com.expo.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.expo.support.PostgresContainerConfig;
import com.expo.ticket.dto.PurchasableTicketProductResponse;
import com.expo.ticket.service.PurchasableTicketProductService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * "구매 가능한 티켓" 이 <b>정말로 지금 살 수 있는 것만</b> 돌려주는지 본다.
 *
 * <h2>왜 실제 DB 인가</h2>
 *
 * 이 조회의 어려운 부분은 계산이 아니라 <b>조건 조합</b>이다 — 상태 컬럼, 판매 기간, 재고 셋이
 * 함께 걸린다. mock 으로는 조건이 붙었는지조차 알 수 없다.
 *
 * <h2>여기서 잡는 회귀</h2>
 *
 * 예전에는 {@code status = ON_SALE} 과 재고만 봤다. 판매 기간이 끝나도 <b>그 컬럼을 바꿔 주는
 * 코드가 없어</b> {@code ON_SALE} 로 남아 있어서, 기간이 지난 상품이 목록에 그대로 나왔다.
 *
 * <p>그 결과 박람회 상세 화면에서 <b>윗줄은 "판매 종료", 아래 티켓 카드는 "판매중"</b> 이 됐다.
 * 윗줄은 뷰가 기간으로 계산하고 아래는 이 조회를 쓰기 때문이다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
@Transactional
class PurchasableTicketProductIntegrationTest {

    @Autowired private PurchasableTicketProductService service;
    @Autowired private JdbcTemplate jdbc;

    private Long expoId;

    @BeforeEach
    void seed() {
        Long host = insertClient();
        expoId = insertExpo(host);
    }

    private List<String> purchasableNames() {
        return service.purchasableTicket(expoId).stream()
                .map(PurchasableTicketProductResponse::name)
                .toList();
    }

    /** 판매 중인 상품은 나온다. */
    @Test
    void includesProductInSalesWindow() {
        insertProduct("판매중 티켓", "ON_SALE", "-1 day", "+7 day", 100);

        assertThat(purchasableNames()).containsExactly("판매중 티켓");
    }

    /**
     * <b>판매 기간이 끝난 상품은 빠진다.</b>
     *
     * <p>이것이 이 테스트의 이유다. {@code status} 는 여전히 {@code ON_SALE} 이다 — 기간이 지나도
     * 그 컬럼을 바꿔 주는 코드가 없기 때문이다. 상태만 보면 이 상품이 목록에 남는다.
     */
    @Test
    void excludesProductWhoseSalesWindowClosed() {
        insertProduct("기간 지난 티켓", "ON_SALE", "-30 day", "-1 day", 100);

        assertThat(purchasableNames()).isEmpty();
    }

    /** 아직 판매 시작 전인 상품도 빠진다. */
    @Test
    void excludesProductBeforeSalesStart() {
        insertProduct("판매 예정 티켓", "ON_SALE", "+1 day", "+30 day", 100);

        assertThat(purchasableNames()).isEmpty();
    }

    /** 재고가 없으면 빠진다. 기간이 유효해도 살 수 없다. */
    @Test
    void excludesSoldOutProduct() {
        insertProduct("품절 티켓", "ON_SALE", "-1 day", "+7 day", 0);

        assertThat(purchasableNames()).isEmpty();
    }

    /** 판매 상태가 아닌 상품은 기간이 유효해도 빠진다. ({@code DRAFT}·{@code CANCELED} 등) */
    @Test
    void excludesProductNotOnSale() {
        insertProduct("초안 티켓", "DRAFT", "-1 day", "+7 day", 100);

        assertThat(purchasableNames()).isEmpty();
    }

    /** 여럿 중 조건을 다 만족하는 것만 골라 낸다. */
    @Test
    void picksOnlyTrulyPurchasableAmongMany() {
        insertProduct("살 수 있다", "ON_SALE", "-1 day", "+7 day", 100);
        insertProduct("기간 지남", "ON_SALE", "-30 day", "-1 day", 100);
        insertProduct("아직 이르다", "ON_SALE", "+1 day", "+30 day", 100);
        insertProduct("품절", "ON_SALE", "-1 day", "+7 day", 0);

        assertThat(purchasableNames()).containsExactly("살 수 있다");
    }

    // ------------------------------------------------------------------
    // 시드
    // ------------------------------------------------------------------

    private Long insertClient() {
        Long userId =
                jdbc.queryForObject(
                        """
                        INSERT INTO users (email, nickname, role, account_status)
                        VALUES ('host-purchasable@espotic.com', '주최사', 'CLIENT', 'ACTIVE')
                        RETURNING id
                        """,
                        Long.class);
        jdbc.update(
                """
                INSERT INTO client_profiles (user_id, company_name, business_number,
                       representative_name, business_address)
                VALUES (?, '주최사', ?, '대표', '서울')
                """,
                userId,
                "%010d".formatted(userId));
        return userId;
    }

    private Long insertExpo(Long hostClientId) {
        return jdbc.queryForObject(
                """
                INSERT INTO expos (host_client_id, title, description, region_code,
                       event_start_at, event_end_at, sales_start_at, sales_end_at,
                       review_status, visibility_status, event_status)
                VALUES (?, '구매가능 검증 박람회', 'x', '11',
                        now() + interval '10 day', now() + interval '12 day',
                        now() - interval '30 day', now() + interval '9 day',
                        'APPROVED', 'PUBLIC', 'SCHEDULED')
                RETURNING id
                """,
                Long.class,
                hostClientId);
    }

    /**
     * 티켓 상품 하나. 재고는 상품과 1:1 이다.
     *
     * @param startOffset {@code now()} 기준 간격. 예: {@code "-1 day"}
     * @param available 살 수 있는 수량. 0 이면 품절이다
     */
    private void insertProduct(
            String name, String status, String startOffset, String endOffset, int available) {
        Long productId =
                jdbc.queryForObject(
                        """
                        INSERT INTO ticket_products (expo_id, name, price,
                               sales_start_at, sales_end_at, status)
                        VALUES (?, ?, 10000,
                                now() + interval '%s', now() + interval '%s', ?)
                        RETURNING id
                        """
                                .formatted(startOffset, endOffset),
                        Long.class,
                        expoId,
                        name,
                        status);
        jdbc.update(
                """
                INSERT INTO ticket_inventories (ticket_product_id, total_quantity,
                       reserved_quantity, sold_quantity)
                VALUES (?, ?, 0, 0)
                """,
                productId,
                available);
    }
}
