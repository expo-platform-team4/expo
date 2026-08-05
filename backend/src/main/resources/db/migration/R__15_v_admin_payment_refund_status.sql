-- 티켓·부스 결제와 환불 상태 통합 조회  →  com.expo.admin
-- 주의: 출력 필드와 UNION ALL 구조 모두 추정이다. 6-12 절의 "결제·환불·정산 상태를
--       통합 조회한다"는 서술이 근거의 전부다. 문서 D-4 14 참조.
--       부스는 승인 완료 후 환불을 지원하지 않으므로 환불 필드가 항상 NULL 이다.
CREATE OR REPLACE VIEW v_admin_payment_refund_status AS
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
