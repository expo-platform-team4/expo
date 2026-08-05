-- 홈·검색·필터·정렬 카드 목록  →  com.expo.expo
-- 근거: docs/init_table_schema.md 6-4. 출력 필드는 명세에 정의되어 있다.
CREATE OR REPLACE VIEW v_public_expo_cards AS
SELECT e.id                                    AS expo_id,
       e.title                                 AS title,
       e.event_start_at                        AS event_start_at,
       e.event_end_at                          AS event_end_at,
       tstzrange(e.event_start_at, e.event_end_at) AS event_period,
       e.region_code                           AS region_code,
       MIN(tp.price)                           AS minimum_price,
       COALESCE(SUM(ti.available_quantity), 0) AS available_quantity,
       -- 판매 기간과 잔여 재고로 노출용 판매 상태를 계산한다.
       CASE
           WHEN e.event_status = 'CANCELED'                     THEN 'CANCELED'
           WHEN MIN(tp.sales_start_at) > CURRENT_TIMESTAMP      THEN 'SCHEDULED'
           WHEN MAX(tp.sales_end_at)   < CURRENT_TIMESTAMP      THEN 'SALE_ENDED'
           WHEN COALESCE(SUM(ti.available_quantity), 0) <= 0    THEN 'SOLD_OUT'
           ELSE 'ON_SALE'
       END                                     AS display_sales_status,
       -- 인기순 정렬 기준. 명세는 "판매량·주문량 기반 계산"까지만 정하고 있어
       -- 가중치는 임의로 잡았다. 운영하며 조정할 값이다.
       COALESCE(SUM(ti.sold_quantity), 0) * 1.0
           + COUNT(DISTINCT toi.ticket_order_id) * 0.5          AS popularity_score
FROM expos e
LEFT JOIN ticket_products    tp  ON tp.expo_id = e.id
LEFT JOIN inventory_by_product inv ON inv.ticket_product_id = tp.id
LEFT JOIN orders_by_expo os ON os.expo_id = e.id
WHERE e.visibility_status = 'PUBLIC'
GROUP BY e.id, e.title, e.event_start_at, e.event_end_at, e.region_code, e.event_status;
