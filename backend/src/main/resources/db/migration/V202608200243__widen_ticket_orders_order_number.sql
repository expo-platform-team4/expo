-- ticket_orders.order_number 를 VARCHAR(40) -> VARCHAR(50) 으로 넓힌다.
--
-- TicketOrderService 가 "TICKET-" + UUID.randomUUID() (7 + 36 = 43자) 로 채번하는데
-- 컬럼이 40자라 회원/비회원 주문 생성이 매번 DataIntegrityViolationException 으로 실패했다
-- (INSERT 시점 "value too long for type character varying(40)").
-- booth_orders.order_number 는 이미 VARCHAR(50) 이라 그 폭에 맞춘다.
--
-- PostgreSQL 은 뷰가 참조하는 컬럼의 타입을 바꾸는 것 자체를 막는다("cannot alter type of
-- a column used by a view or rule") — 폭을 넓히기만 해도 예외다. order_number 를 참조하는
-- 뷰 둘(v_member_mypage_orders, R__03 / v_admin_payment_refund_status, R__15)을 잠깐
-- 지웠다가 ALTER 뒤 정의를 그대로 복사해 되살린다. 두 R__ 파일 자체는 건드리지 않는다 —
-- 그 파일들이 여전히 정본이고, 이 마이그레이션의 재현 정의는 그 시점의 스냅샷일 뿐이다.

DROP VIEW v_member_mypage_orders;
DROP VIEW v_admin_payment_refund_status;

ALTER TABLE ticket_orders
    ALTER COLUMN order_number TYPE VARCHAR(50);

-- 아래 두 CREATE 는 R__03_v_member_mypage_orders.sql / R__15_v_admin_payment_refund_status.sql
-- 과 정확히 같은 정의다.

CREATE VIEW v_member_mypage_orders AS
SELECT o.id                     AS order_id,
       o.member_user_id         AS member_user_id,
       o.order_number           AS order_number,
       e.id                     AS expo_id,
       e.title                  AS expo_title,
       o.status                 AS order_status,
       p.status                 AS payment_status,
       r.status                 AS refund_status,
       o.total_quantity         AS total_quantity,
       o.ticket_subtotal_amount AS ticket_subtotal_amount,
       o.booking_fee_rate       AS booking_fee_rate,
       o.booking_fee_amount     AS booking_fee_amount,
       o.total_amount           AS total_amount,
       -- 환불 가능 여부: 행사 3일 전까지, 결제 완료 상태, 미사용 티켓만.
       (o.status = 'PAID'
        AND e.event_start_at > CURRENT_TIMESTAMP + INTERVAL '3 days'
        AND NOT EXISTS (SELECT 1
                          FROM issued_tickets it
                          JOIN ticket_order_items oi2 ON oi2.id = it.ticket_order_item_id
                         WHERE oi2.ticket_order_id = o.id
                           AND it.status = 'CHECKED_IN')
       )                        AS refundable,
       o.created_at             AS created_at
FROM ticket_orders o
LEFT JOIN LATERAL (
    SELECT oi.ticket_product_id
      FROM ticket_order_items oi
     WHERE oi.ticket_order_id = o.id
     ORDER BY oi.id
     LIMIT 1
) first_item ON TRUE
LEFT JOIN ticket_products tp ON tp.id = first_item.ticket_product_id
LEFT JOIN expos           e  ON e.id  = tp.expo_id
-- 결제·환불은 최신 1건만 노출한다.
LEFT JOIN LATERAL (
    SELECT tp2.status FROM ticket_payments tp2
     WHERE tp2.ticket_order_id = o.id
     ORDER BY tp2.created_at DESC, tp2.id DESC LIMIT 1
) p ON TRUE
LEFT JOIN LATERAL (
    SELECT tr.status FROM ticket_refunds tr
     WHERE tr.ticket_order_id = o.id
     ORDER BY tr.created_at DESC, tr.id DESC LIMIT 1
) r ON TRUE
WHERE o.member_user_id IS NOT NULL;

CREATE VIEW v_admin_payment_refund_status AS
SELECT 'TICKET'::VARCHAR(10)  AS payment_domain,
       o.id                   AS order_id,
       o.order_number         AS order_number,
       o.member_user_id       AS payer_user_id,
       tp.expo_id             AS expo_id,
       p.id                   AS payment_id,
       p.status               AS payment_status,
       p.requested_amount     AS requested_amount,
       p.approved_amount      AS approved_amount,
       p.approved_at          AS approved_at,
       r.id                   AS refund_id,
       r.status               AS refund_status,
       r.refund_amount        AS refund_amount,
       r.completed_at         AS refund_completed_at
FROM ticket_orders o
LEFT JOIN LATERAL (
    SELECT oi.ticket_product_id FROM ticket_order_items oi
     WHERE oi.ticket_order_id = o.id ORDER BY oi.id LIMIT 1
) fi ON TRUE
LEFT JOIN ticket_products tp ON tp.id = fi.ticket_product_id
LEFT JOIN LATERAL (
    SELECT x.id, x.status, x.requested_amount, x.approved_amount, x.approved_at
      FROM ticket_payments x
     WHERE x.ticket_order_id = o.id ORDER BY x.created_at DESC, x.id DESC LIMIT 1
) p ON TRUE
LEFT JOIN LATERAL (
    SELECT x.id, x.status, x.refund_amount, x.completed_at
      FROM ticket_refunds x
     WHERE x.ticket_order_id = o.id ORDER BY x.created_at DESC, x.id DESC LIMIT 1
) r ON TRUE
UNION ALL
SELECT 'BOOTH'::VARCHAR(10),
       bo.id,
       bo.order_number,
       bo.client_user_id,
       NULL::BIGINT,
       bp.id,
       bp.status,
       bp.requested_amount,
       bp.approved_amount,
       bp.approved_at,
       NULL::BIGINT,
       NULL::VARCHAR(30),
       NULL::NUMERIC(15,2),
       NULL::TIMESTAMPTZ
FROM booth_orders bo
LEFT JOIN LATERAL (
    SELECT x.id, x.status, x.requested_amount, x.approved_amount, x.approved_at
      FROM booth_payments x
     WHERE x.booth_order_id = bo.id ORDER BY x.created_at DESC, x.id DESC LIMIT 1
) bp ON TRUE;
