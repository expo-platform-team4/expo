-- 일반 회원 주문 목록 및 상세 요약  →  com.expo.member
-- 근거: docs/init_table_schema.md 6-2. 출력 필드는 명세에 정의되어 있다.
CREATE OR REPLACE VIEW v_member_mypage_orders AS
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
     ORDER BY tp2.created_at DESC LIMIT 1
) p ON TRUE
LEFT JOIN LATERAL (
    SELECT tr.status FROM ticket_refunds tr
     WHERE tr.ticket_order_id = o.id
     ORDER BY tr.created_at DESC LIMIT 1
) r ON TRUE
WHERE o.member_user_id IS NOT NULL;
