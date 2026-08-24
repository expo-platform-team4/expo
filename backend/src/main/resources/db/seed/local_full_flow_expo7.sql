-- 프론트 화면 확인용 전체 플로우 시드 (판매 → 발권 → 체크인 → 부스 → 정산).
--
-- ⚠ Flyway 가 실행하지 않는다. db/migration 이 아니라 db/seed 에 있는 이유가 그것이다.
--   운영 DB 에 들어가면 안 된다. 반드시 손으로 실행한다.
--
--   docker exec -i expo-postgres psql -U expo -d expo \
--     -v qr_secret="$(grep '^QR_TOKEN_SECRET=' backend/.env | cut -d= -f2-)" \
--     < backend/src/main/resources/db/seed/local_full_flow_expo7.sql
--
-- ── 왜 필요한가 ───────────────────────────────────────────────────────────────
-- 부스 관리·정산 리포트·체크인·QR 화면은 전부 "결제 완료된 주문"에서 출발하는데,
-- 주문을 PAID 로 전이시키는 API 가 아직 없다(결제 미연동, 이슈 #107). 그래서 UI 조작
-- 만으로는 이 화면들에 데이터를 만들 수 없고 SQL 로 직접 넣는 수밖에 없다.
--
-- ── 누구 기준인가 ─────────────────────────────────────────────────────────────
-- 기존 시드(local_ticket_flow.sql)는 seed-client/seed-member 를 쓰지만, 이 파일은
-- **로그인해서 화면을 볼 수 있는 계정** 기준으로 넣는다. 비밀번호가 'Test1234!' 인
-- e2e 계정들이다. 역할이 갈리므로 화면도 계정별로 나뉜다:
--
--   e2e-client@example.com    (주최사)   → 내 박람회·체크인·정산 리포트
--   e2e-exhibitor@example.com (참가기업) → 부스 관리 (배정받는 쪽)
--   e2e-member@example.com    (구매자)   → 티켓 구매자
--
-- 체크인 화면은 ExpoHostVerifier 로 "자기 박람회"만 보여준다. 그래서 반드시 주최사가
-- e2e-client 인 박람회(= '이엔이 테스트 박람회')에 티켓을 매달아야 화면에 뜬다.
--
-- ── QR 해시 ──────────────────────────────────────────────────────────────────
-- issued_tickets.qr_token_hash = sha256_hex( 'v1.' || base64url(HMAC-SHA256(시크릿, ticket_code)) )
-- QrTokenGenerator + TokenHasher 와 같은 계산을 pgcrypto 로 재현한다. 시크릿을 파일에
-- 박지 않으려고 psql 변수 :qr_secret 으로 받는다(위 실행 예시 참고). 값이 실제 백엔드와
-- 같아야 스캔 화면에서 이 티켓이 인식된다.
--
-- 여러 번 실행해도 되도록 만들었다 — 이미 있으면 재사용하거나 건너뛴다.

\if :{?qr_secret}
\else
\echo '!! qr_secret 변수가 없다. QR 스캔이 동작하지 않는 해시가 들어간다.'
\echo '!! 위 주석의 실행 예시대로 -v qr_secret=... 를 붙여 다시 실행할 것.'
\set qr_secret 'MISSING-SECRET-SCAN-WILL-NOT-WORK'
\endif

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- 0. 기준 대상 확인 ----------------------------------------------------------
-- 없으면 여기서 멈춘다. 조용히 절반만 들어가는 것보다 낫다.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'e2e-client@example.com') THEN
        RAISE EXCEPTION 'e2e-client@example.com 이 없다. 회원가입 API 로 먼저 만들 것.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM expos WHERE title = '이엔이 테스트 박람회') THEN
        RAISE EXCEPTION '이엔이 테스트 박람회 가 없다.';
    END IF;
END $$;

-- 1. 박람회를 "진행 중"으로 --------------------------------------------------
-- 체크인 현황 화면이 의미를 가지려면 행사가 열려 있어야 한다. 대시보드의
-- "진행중인 박람회" 타일도 이걸로 1이 된다.
UPDATE expos
   SET event_start_at = now() - INTERVAL '1 day',
       event_end_at   = now() + INTERVAL '2 days',
       sales_start_at = now() - INTERVAL '10 days',
       sales_end_at   = now() + INTERVAL '1 day',
       event_status   = 'ONGOING'
 WHERE title = '이엔이 테스트 박람회';

-- 2. 티켓 상품 + 재고 ---------------------------------------------------------
INSERT INTO ticket_products (expo_id, name, description, price, sales_start_at, sales_end_at,
                             max_quantity_per_order, status)
SELECT e.id, '얼리버드 1일권', '오전 입장 가능한 1일 관람권입니다.', 18000,
       now() - INTERVAL '10 days', now() + INTERVAL '1 day', 4, 'ON_SALE'
  FROM expos e
 WHERE e.title = '이엔이 테스트 박람회'
   AND NOT EXISTS (SELECT 1 FROM ticket_products p WHERE p.expo_id = e.id AND p.name = '얼리버드 1일권');

INSERT INTO ticket_inventories (ticket_product_id, total_quantity, reserved_quantity, sold_quantity)
SELECT p.id, 500, 0, 3
  FROM ticket_products p
  JOIN expos e ON e.id = p.expo_id
 WHERE e.title = '이엔이 테스트 박람회' AND p.name = '얼리버드 1일권'
   AND NOT EXISTS (SELECT 1 FROM ticket_inventories i WHERE i.ticket_product_id = p.id);

-- 3. 결제 완료 주문 + 항목 -----------------------------------------------------
-- 결제 API 가 없어 PAID 를 직접 넣는다. 예약 수수료 3% 는 TicketOrderService 와 같은 값.
INSERT INTO ticket_orders (order_number, member_user_id, orderer_type, ticket_subtotal_amount,
                           booking_fee_rate, booking_fee_amount, total_amount, total_quantity,
                           status, paid_at)
SELECT 'TICKET-SEED-EXPO7-0001', u.id, 'MEMBER', 54000, 0.03, 1620, 55620, 3, 'PAID', now() - INTERVAL '2 days'
  FROM users u
 WHERE u.email = 'e2e-member@example.com'
   AND NOT EXISTS (SELECT 1 FROM ticket_orders o WHERE o.order_number = 'TICKET-SEED-EXPO7-0001');

INSERT INTO ticket_order_items (ticket_order_id, ticket_product_id, quantity, unit_price, item_subtotal_amount)
SELECT o.id, p.id, 3, 18000, 54000
  FROM ticket_orders o
  JOIN ticket_products p ON p.name = '얼리버드 1일권'
  JOIN expos e ON e.id = p.expo_id AND e.title = '이엔이 테스트 박람회'
 WHERE o.order_number = 'TICKET-SEED-EXPO7-0001'
   AND NOT EXISTS (SELECT 1 FROM ticket_order_items i WHERE i.ticket_order_id = o.id);

-- 4. 발권 3매 (2매 미입장 / 1매 입장완료) --------------------------------------
-- qr_token_hash 는 백엔드와 같은 방식으로 계산한다(파일 상단 주석 참고).
INSERT INTO issued_tickets (ticket_order_item_id, expo_id, ticket_code, qr_token_hash, status, issued_at, checked_in_at)
SELECT i.id,
       e.id,
       code.ticket_code,
       encode(digest('v1.' || translate(rtrim(encode(hmac(code.ticket_code, :'qr_secret', 'sha256'), 'base64'), '='), '+/', '-_'), 'sha256'), 'hex'),
       code.status,
       now() - INTERVAL '2 days',
       code.checked_in_at
  FROM ticket_order_items i
  JOIN ticket_orders o ON o.id = i.ticket_order_id AND o.order_number = 'TICKET-SEED-EXPO7-0001'
  JOIN ticket_products p ON p.id = i.ticket_product_id
  JOIN expos e ON e.id = p.expo_id
 CROSS JOIN (VALUES
        ('EXPO-SEED7-000001', 'CHECKED_IN', now() - INTERVAL '3 hours'),
        ('EXPO-SEED7-000002', 'ISSUED',     NULL::timestamptz),
        ('EXPO-SEED7-000003', 'ISSUED',     NULL::timestamptz)
      ) AS code(ticket_code, status, checked_in_at)
 WHERE NOT EXISTS (SELECT 1 FROM issued_tickets t WHERE t.ticket_code = code.ticket_code);

-- 5. QR 링크 접근 토큰 ---------------------------------------------------------
-- /tickets?token=<원문> 으로 여는 화면용. 원문은 저장하지 않는 게 원칙이라 여기서만
-- 알아볼 수 있게 고정 문자열을 쓴다. 파일 맨 끝에서 접속 URL 을 출력한다.
INSERT INTO ticket_access_tokens (ticket_order_id, issued_ticket_id, token_hash, scope, status, expires_at)
SELECT o.id, NULL,
       encode(digest('seed-expo7-order-view-token', 'sha256'), 'hex'),
       'ORDER_VIEW', 'ACTIVE', now() + INTERVAL '30 days'
  FROM ticket_orders o
 WHERE o.order_number = 'TICKET-SEED-EXPO7-0001'
   AND NOT EXISTS (
        SELECT 1 FROM ticket_access_tokens t
         WHERE t.token_hash = encode(digest('seed-expo7-order-view-token', 'sha256'), 'hex'));

-- 6. 체크인 이력 (성공 1 + 실패 1) --------------------------------------------
-- 이력 화면의 성공/실패 필터를 실제로 눌러볼 수 있게 둘 다 넣는다.
INSERT INTO check_in_histories (issued_ticket_id, expo_id, processed_by_client_id, method, result, checked_at, detail)
SELECT t.id, t.expo_id, u.id, h.method, h.result, h.checked_at, h.detail
  FROM issued_tickets t
  JOIN users u ON u.email = 'e2e-client@example.com'
 CROSS JOIN (VALUES
        ('QR',          'SUCCESS',      now() - INTERVAL '3 hours', '정상 입장'),
        ('MANUAL_CODE', 'ALREADY_USED', now() - INTERVAL '2 hours', '이미 입장한 티켓입니다.')
      ) AS h(method, result, checked_at, detail)
 WHERE t.ticket_code = 'EXPO-SEED7-000001'
   AND NOT EXISTS (SELECT 1 FROM check_in_histories c WHERE c.issued_ticket_id = t.id);

-- 7. 부스 배정 (참가기업 e2e-exhibitor 기준) ----------------------------------
-- 이미 만들어 둔 모집공고(id 기준 아님, 제목으로 찾는다)와 참여신청 위에 얹는다.
INSERT INTO booths (venue_zone_id, booth_number, shape_code, width, depth, operational_status)
SELECT 1, 'A-101', 'RECTANGLE', 3.0, 3.0, 'ACTIVE'
 WHERE NOT EXISTS (SELECT 1 FROM booths b WHERE b.booth_number = 'A-101');

INSERT INTO booth_products (recruitment_notice_id, booth_id, supply_price, vat_amount, total_price,
                            vat_included, sales_status)
SELECT n.id, b.id, 1000000, 100000, 1100000, TRUE, 'SOLD'
  FROM recruitment_notices n
  JOIN booths b ON b.booth_number = 'A-101'
 WHERE n.title = '이엔이 테스트 박람회 부스 모집'
   AND NOT EXISTS (SELECT 1 FROM booth_products bp WHERE bp.booth_id = b.id);

-- booth_orders 에는 (application_id, booth_product_id) 복합 FK 가 걸려 있어
-- participation_applications(id, selected_booth_product_id) 를 참조한다 — "신청서에 고른
-- 부스"와 "실제 주문한 부스"가 어긋나는 걸 DB 가 막는다. 그래서 주문을 만들기 전에
-- 신청서의 selected_booth_product_id 를 먼저 채워야 한다.
UPDATE participation_applications pa
   SET selected_booth_product_id = bp.id
  FROM recruitment_notices n
  JOIN booth_products bp ON bp.recruitment_notice_id = n.id
 WHERE n.title = '이엔이 테스트 박람회 부스 모집'
   AND pa.recruitment_notice_id = n.id
   AND pa.selected_booth_product_id IS DISTINCT FROM bp.id;

INSERT INTO booth_orders (application_id, client_user_id, booth_product_id, order_number,
                          unit_price, total_amount, status, expires_at, paid_at, idempotency_key)
SELECT pa.id, pa.client_user_id, bp.id, 'BOOTH-SEED-EXPO7-0001',
       1100000, 1100000, 'PAYMENT_COMPLETED', now() + INTERVAL '7 days', now() - INTERVAL '1 day',
       'seed-booth-expo7-0001'
  FROM participation_applications pa
  JOIN recruitment_notices n ON n.id = pa.recruitment_notice_id AND n.title = '이엔이 테스트 박람회 부스 모집'
  JOIN booth_products bp ON bp.recruitment_notice_id = n.id
 WHERE NOT EXISTS (SELECT 1 FROM booth_orders o WHERE o.order_number = 'BOOTH-SEED-EXPO7-0001');

INSERT INTO booth_payments (booth_order_id, pg_order_id, status, requested_amount, approved_amount,
                            approved_at, idempotency_key, method)
SELECT o.id, 'PG-SEED-EXPO7-0001', 'APPROVED', 1100000, 1100000, now() - INTERVAL '1 day',
       'seed-booth-pay-expo7-0001', 'CARD'
  FROM booth_orders o
 WHERE o.order_number = 'BOOTH-SEED-EXPO7-0001'
   AND NOT EXISTS (SELECT 1 FROM booth_payments p WHERE p.pg_order_id = 'PG-SEED-EXPO7-0001');

-- v_client_dashboard_booths 가 pa.booth_order_id 로 조인한다. 안 걸어주면 부스 관리
-- 화면에서 주문·결제 열이 비어 보인다.
UPDATE participation_applications pa
   SET booth_order_id = o.id,
       status = 'SUBMITTED'
  FROM booth_orders o
 WHERE o.order_number = 'BOOTH-SEED-EXPO7-0001'
   AND pa.id = o.application_id;

INSERT INTO booth_allocations (application_id, booth_order_id, booth_product_id, client_user_id,
                               allocated_at, status)
SELECT o.application_id, o.id, o.booth_product_id, o.client_user_id, now() - INTERVAL '1 day', 'ASSIGNED'
  FROM booth_orders o
 WHERE o.order_number = 'BOOTH-SEED-EXPO7-0001'
   AND NOT EXISTS (SELECT 1 FROM booth_allocations a WHERE a.booth_order_id = o.id);

-- 8. 정산 (주최사 e2e-client 기준) ---------------------------------------------
-- 티켓 순매출 54,000 + 부스 1,100,000, 예약 수수료 1,620 은 플랫폼 몫이라 송금액에서 뺀다.
INSERT INTO settlements (expo_id, host_client_id, gross_ticket_sales_amount, ticket_refund_amount,
                         net_ticket_sales_amount, booking_fee_gross_amount, booking_fee_refund_amount,
                         booking_fee_net_amount, gross_booth_sales_amount, adjustment_amount,
                         remittance_due_amount, settlement_due_at, status)
SELECT e.id, e.host_client_id, 54000, 0, 54000, 1620, 0, 1620, 1100000, 0,
       1154000, now() + INTERVAL '14 days', 'CALCULATED'
  FROM expos e
 WHERE e.title = '이엔이 테스트 박람회'
   AND NOT EXISTS (SELECT 1 FROM settlements s WHERE s.expo_id = e.id);

INSERT INTO settlement_items (settlement_id, item_type, source_type, source_id, amount,
                              included_in_remittance, occurred_at)
SELECT s.id, v.item_type, v.source_type, NULL, v.amount, v.included, now() - INTERVAL '1 day'
  FROM settlements s
  JOIN expos e ON e.id = s.expo_id AND e.title = '이엔이 테스트 박람회'
 CROSS JOIN (VALUES
        ('TICKET_SALE', 'TICKET_ORDER', 54000,   TRUE),
        ('BOOKING_FEE', 'TICKET_ORDER', 1620,    FALSE),
        ('BOOTH_SALE',  'BOOTH_ORDER',  1100000, TRUE)
      ) AS v(item_type, source_type, amount, included)
 WHERE NOT EXISTS (SELECT 1 FROM settlement_items si WHERE si.settlement_id = s.id);

COMMIT;

-- 결과 요약 -------------------------------------------------------------------
\echo ''
\echo '=== 시드 결과 ==='
SELECT '발권 티켓' AS 항목, count(*)::text AS 값 FROM issued_tickets
UNION ALL SELECT '체크인 이력', count(*)::text FROM check_in_histories
UNION ALL SELECT '부스 배정',   count(*)::text FROM booth_allocations
UNION ALL SELECT '정산',        count(*)::text FROM settlements
UNION ALL SELECT '정산 항목',   count(*)::text FROM settlement_items;

\echo ''
\echo '=== QR 확인 화면 접속 주소 ==='
\echo 'http://localhost:3000/tickets?token=seed-expo7-order-view-token'
\echo ''
\echo '=== 스캔 화면에서 쓸 티켓 코드 (수동 입력 탭) ==='
\echo '  EXPO-SEED7-000002  (미입장 → 입장 성공해야 함)'
\echo '  EXPO-SEED7-000001  (이미 입장 → ALREADY_USED 나와야 함)'
