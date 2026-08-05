-- 박람회별 일자별 티켓 판매·환불 현황  →  com.expo.settlement
-- 주의: 출력 필드는 명세에 없어 추정한 것이다. expo_daily_sales_summaries 가
--       정확히 이 용도의 집계 테이블이라 그대로 노출하고 소유자 필터 키만 덧붙였다.
CREATE OR REPLACE VIEW v_client_dashboard_daily_sales AS
SELECT e.host_client_id                  AS client_user_id,
       s.expo_id                         AS expo_id,
       e.title                           AS expo_title,
       s.sales_date                      AS sales_date,
       s.paid_order_count                AS paid_order_count,
       s.canceled_order_count            AS canceled_order_count,
       s.sold_ticket_quantity            AS sold_ticket_quantity,
       s.refund_ticket_quantity          AS refund_ticket_quantity,
       s.ticket_sales_amount             AS ticket_sales_amount,
       s.booking_fee_amount              AS booking_fee_amount,
       s.refund_ticket_amount            AS refund_ticket_amount,
       s.refund_booking_fee_amount       AS refund_booking_fee_amount,
       s.buyer_payment_amount            AS buyer_payment_amount,
       s.client_settlement_base_amount   AS client_settlement_base_amount,
       s.calculated_at                   AS calculated_at
FROM expo_daily_sales_summaries s
JOIN expos e ON e.id = s.expo_id;
