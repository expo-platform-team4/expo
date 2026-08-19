-- 로컬 발권 검증용 시드 데이터.
--
-- ⚠ 이 파일은 Flyway 가 실행하지 않는다. db/migration 이 아니라 db/seed 에 있는 이유가 그것이다.
--   마이그레이션에 넣으면 운영 DB 에도 들어간다. 반드시 손으로 실행한다.
--
--   docker exec -i expo-postgres psql -U expo -d expo \
--     < backend/src/main/resources/db/seed/local_ticket_flow.sql
--
-- 왜 필요한가 — issued_tickets 는 ticket_order_items 와 expos 를 NOT NULL FK 로 참조한다.
-- 그런데 order·payment 도메인에 코드가 아직 없어서 주문을 API 로 만들 수가 없다.
-- 발권 서비스를 돌려보려면 그 앞단을 SQL 로 직접 채우는 수밖에 없다.
--
-- 여러 번 실행해도 되도록 만들었다. 실행할 때마다 주문이 하나씩 새로 생긴다.
-- (박람회·티켓상품·회원은 이미 있으면 재사용한다)

BEGIN;

-- 1. 주최 클라이언트와 구매 회원 -----------------------------------------------
INSERT INTO users (email, password_hash, nickname, role, account_status, phone_number)
VALUES ('seed-client@example.com', 'x', 'seed-client', 'CLIENT', 'ACTIVE', '01011112222')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, nickname, role, account_status, phone_number)
VALUES ('seed-member@example.com', 'x', 'seed-member', 'MEMBER', 'ACTIVE', '01033334444')
ON CONFLICT (email) DO NOTHING;

INSERT INTO client_profiles (user_id, business_number, company_name,
                             representative_name, business_address)
SELECT id, '0001112222', '시드 주식회사', '홍길동', '서울시 어딘가 1-2-3'
FROM users WHERE email = 'seed-client@example.com'
ON CONFLICT (user_id) DO NOTHING;

-- 2. 박람회 -------------------------------------------------------------------
-- 승인·공개 상태여야 판매 가능한 박람회다.
INSERT INTO expos (host_client_id, title, description, region_code,
                   event_start_at, event_end_at, sales_start_at, sales_end_at,
                   review_status, visibility_status, event_status)
SELECT u.id,
       '시드 박람회',
       '발권 검증용 시드 데이터',
       'SEOUL',
       CURRENT_TIMESTAMP + INTERVAL '30 days',
       CURRENT_TIMESTAMP + INTERVAL '32 days',
       CURRENT_TIMESTAMP - INTERVAL '1 day',
       CURRENT_TIMESTAMP + INTERVAL '29 days',
       'APPROVED', 'PUBLIC', 'SCHEDULED'
FROM users u
WHERE u.email = 'seed-client@example.com'
  AND NOT EXISTS (SELECT 1 FROM expos WHERE title = '시드 박람회');

-- 3. 티켓 상품과 재고 ----------------------------------------------------------
INSERT INTO ticket_products (expo_id, name, price, sales_start_at, sales_end_at, status)
SELECT e.id, '시드 1일권', 15000, e.sales_start_at, e.sales_end_at, 'ON_SALE'
FROM expos e
WHERE e.title = '시드 박람회'
  AND NOT EXISTS (SELECT 1 FROM ticket_products WHERE name = '시드 1일권');

INSERT INTO ticket_inventories (ticket_product_id, total_quantity)
SELECT tp.id, 1000
FROM ticket_products tp
WHERE tp.name = '시드 1일권'
ON CONFLICT (ticket_product_id) DO NOTHING;

-- 4. 결제 완료된 주문 ----------------------------------------------------------
-- 2매 주문. 발권하면 issued_tickets 가 2행 생겨야 한다.
-- order_number 는 실행할 때마다 달라야 하므로 시각을 섞는다.
WITH new_order AS (
    INSERT INTO ticket_orders (order_number, member_user_id, orderer_type,
                               ticket_subtotal_amount, booking_fee_amount, total_amount,
                               total_quantity, status, paid_at)
    SELECT 'ORD-SEED-' || to_char(CURRENT_TIMESTAMP, 'YYYYMMDDHH24MISSMS'),
           u.id, 'MEMBER',
           30000, 900, 30900,
           2, 'PAID', CURRENT_TIMESTAMP
    FROM users u
    WHERE u.email = 'seed-member@example.com'
    RETURNING id, order_number
)
INSERT INTO ticket_order_items (ticket_order_id, ticket_product_id, quantity,
                                unit_price, item_subtotal_amount)
SELECT no.id, tp.id, 2, 15000, 30000
FROM new_order no
CROSS JOIN ticket_products tp
WHERE tp.name = '시드 1일권';

COMMIT;

-- 방금 만든 주문 확인. 이 order_id 로 발권을 호출한다.
SELECT o.id   AS order_id,
       o.order_number,
       o.status,
       oi.quantity,
       tp.expo_id
FROM ticket_orders o
JOIN ticket_order_items oi ON oi.ticket_order_id = o.id
JOIN ticket_products    tp ON tp.id = oi.ticket_product_id
WHERE o.order_number LIKE 'ORD-SEED-%'
ORDER BY o.id DESC
LIMIT 5;
