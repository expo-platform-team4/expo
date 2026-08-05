-- 일반 회원 발권 티켓 및 QR 상세  →  com.expo.member
-- 근거: docs/init_table_schema.md 6-2. 출력 필드는 명세에 정의되어 있다.
CREATE OR REPLACE VIEW v_member_mypage_tickets AS
SELECT o.member_user_id  AS member_user_id,
       o.id              AS order_id,
       it.id             AS issued_ticket_id,
       it.ticket_code    AS ticket_code,
       it.status         AS status,
       it.checked_in_at  AS checked_in_at,
       e.id              AS expo_id,
       e.title           AS expo_title,
       -- QR 접근 가능 여부: 유효한 접근 토큰이 하나라도 있으면 TRUE
       EXISTS (SELECT 1
                 FROM ticket_access_tokens tat
                WHERE tat.issued_ticket_id = it.id
                  AND tat.status = 'ACTIVE'
                  AND tat.expires_at > CURRENT_TIMESTAMP) AS secure_qr_access
FROM issued_tickets    it
JOIN ticket_order_items oi ON oi.id = it.ticket_order_item_id
JOIN ticket_orders      o  ON o.id  = oi.ticket_order_id
JOIN expos              e  ON e.id  = it.expo_id
WHERE o.member_user_id IS NOT NULL;
