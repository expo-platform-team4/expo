-- 홈·검색·필터·정렬 카드 목록  →  com.expo.expo
-- 근거: docs/init_table_schema.md 6-4. 출력 필드는 명세에 정의되어 있다.
--
-- 재고와 주문 항목을 ticket_products 에 함께 조인하면 상품당 재고 1행이 주문 항목
-- 수만큼 반복되어 SUM 이 부풀려진다. 그래서 두 집계를 CTE 로 미리 박람회 단위까지
-- 접은 뒤 1:1 로 붙인다.
CREATE OR REPLACE VIEW v_public_expo_cards AS
WITH product_rollup AS (
    -- 박람회별 가격·판매기간·재고 집계. ticket_inventories 는 상품과 1:1 이다.
    SELECT tp.expo_id                              AS expo_id,
           MIN(tp.price)                           AS minimum_price,
           MIN(tp.sales_start_at)                  AS sales_start_at,
           MAX(tp.sales_end_at)                    AS sales_end_at,
           COALESCE(SUM(ti.available_quantity), 0) AS available_quantity,
           COALESCE(SUM(ti.sold_quantity), 0)      AS sold_quantity
    FROM ticket_products tp
    LEFT JOIN ticket_inventories ti ON ti.ticket_product_id = tp.id
    GROUP BY tp.expo_id
),
order_rollup AS (
    -- 박람회별 주문 건수. 위 집계와 독립적으로 접는다.
    SELECT tp.expo_id                        AS expo_id,
           COUNT(DISTINCT toi.ticket_order_id) AS order_count
    FROM ticket_products tp
    JOIN ticket_order_items toi ON toi.ticket_product_id = tp.id
    GROUP BY tp.expo_id
)
SELECT e.id                                        AS expo_id,
       e.title                                     AS title,
       e.event_start_at                            AS event_start_at,
       e.event_end_at                              AS event_end_at,
       tstzrange(e.event_start_at, e.event_end_at) AS event_period,
       e.region_code                               AS region_code,
       pr.minimum_price                            AS minimum_price,
       COALESCE(pr.available_quantity, 0)          AS available_quantity,
       -- 판매 기간과 잔여 재고로 노출용 판매 상태를 계산한다.
       CASE
           WHEN e.event_status = 'CANCELED'             THEN 'CANCELED'
           WHEN pr.sales_start_at > CURRENT_TIMESTAMP   THEN 'SCHEDULED'
           WHEN pr.sales_end_at   < CURRENT_TIMESTAMP   THEN 'SALE_ENDED'
           WHEN COALESCE(pr.available_quantity, 0) <= 0 THEN 'SOLD_OUT'
           ELSE 'ON_SALE'
       END                                         AS display_sales_status,
       -- 인기순 정렬 기준. 명세는 "판매량·주문량 기반 계산"까지만 정하고 있어
       -- 가중치는 임의로 잡았다. 운영하며 조정할 값이다.
       COALESCE(pr.sold_quantity, 0) * 1.0
           + COALESCE(orr.order_count, 0) * 0.5    AS popularity_score
FROM expos e
LEFT JOIN product_rollup pr  ON pr.expo_id  = e.id
LEFT JOIN order_rollup   orr ON orr.expo_id = e.id
WHERE e.visibility_status = 'PUBLIC';
