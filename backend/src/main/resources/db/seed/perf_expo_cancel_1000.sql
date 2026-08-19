-- 박람회 취소 알림 규모 실측용 시드 (expo-docs/07-SCALE.md 9-6)
--
-- 비회원 주문 1,000건 + 주문마다 티켓 2장 = 티켓 2,000장.
-- 비회원으로 만드는 이유는 수신번호를 1,000개 서로 다르게 주기 위해서다.
-- 회원으로 만들면 users 의 이메일·닉네임 UNIQUE 를 1,000번 피해 가야 한다.
--
-- 실행:  docker exec -i expo-postgres psql -U expo -d expo < 이 파일
-- Flyway 가 실행하지 않는다. db/migration 에 두면 운영 DB 에도 들어간다.
DO $$
DECLARE
  v_expo_id   bigint;
  v_product_id bigint;
  v_host      bigint;
BEGIN
  SELECT id INTO v_host FROM users WHERE role='CLIENT' LIMIT 1;

  INSERT INTO expos (host_client_id, title, description, region_code,
                     event_start_at, event_end_at, sales_start_at, sales_end_at,
                     review_status, visibility_status, event_status)
  VALUES (v_host, '실측용 박람회 1000', '9-6 실측용', '11',
          now()+interval '30 day', now()+interval '32 day',
          now()-interval '1 day', now()+interval '29 day',
          'APPROVED', 'PUBLIC', 'SCHEDULED')
  RETURNING id INTO v_expo_id;

  INSERT INTO ticket_products (expo_id, name, price, sales_start_at, sales_end_at, status)
  VALUES (v_expo_id, '실측 티켓', 10000,
          now()-interval '1 day', now()+interval '29 day', 'ON_SALE')
  RETURNING id INTO v_product_id;

  -- 주문 1,000건
  INSERT INTO ticket_orders (order_number, orderer_type, ticket_subtotal_amount,
        booking_fee_amount, total_amount, total_quantity, status, paid_at)
  SELECT 'ORD-PERF-' || lpad(i::text, 6, '0'), 'GUEST', 20000, 0, 20000, 2, 'PAID', now()
  FROM generate_series(1, 1000) i;

  -- 비회원 정보 (번호를 전부 다르게)
  INSERT INTO guest_order_infos (ticket_order_id, guest_name, phone_number,
        lookup_password_hash, failed_lookup_count, created_at, updated_at)
  SELECT o.id, '실측' || row_number() OVER (ORDER BY o.id),
         '0102' || lpad((row_number() OVER (ORDER BY o.id))::text, 7, '0'),
         'x', 0, now(), now()
  FROM ticket_orders o WHERE o.order_number LIKE 'ORD-PERF-%';

  -- 항목
  INSERT INTO ticket_order_items (ticket_order_id, ticket_product_id, quantity,
        unit_price, item_subtotal_amount)
  SELECT o.id, v_product_id, 2, 10000, 20000
  FROM ticket_orders o WHERE o.order_number LIKE 'ORD-PERF-%';

  -- 티켓 2장씩 = 2,000장
  INSERT INTO issued_tickets (ticket_order_item_id, expo_id, ticket_code, qr_token_hash,
        status, issued_at)
  SELECT oi.id, v_expo_id,
         'EXPO-PERF-' || lpad((row_number() OVER (ORDER BY oi.id, g.n))::text, 8, '0'),
         md5(random()::text || oi.id || g.n),
         'ISSUED', now()
  FROM ticket_order_items oi
  JOIN ticket_orders o ON o.id = oi.ticket_order_id
  CROSS JOIN generate_series(1,2) g(n)
  WHERE o.order_number LIKE 'ORD-PERF-%';

  RAISE NOTICE '실측용 expo_id = %', v_expo_id;
END $$;

SELECT (SELECT id FROM expos WHERE title='실측용 박람회 1000') AS expo_id,
       (SELECT count(*) FROM issued_tickets it JOIN expos e ON e.id=it.expo_id
        WHERE e.title='실측용 박람회 1000') AS 티켓수;
