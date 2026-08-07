-- 일반 회원 발권 티켓 및 QR 상세  →  com.expo.member
-- 근거: docs/init_table_schema.md 6-2. 출력 필드는 명세에 정의되어 있다.
--
-- 컬럼을 추가할 때는 반드시 SELECT 목록 맨 끝에 붙인다.
-- CREATE OR REPLACE VIEW 는 기존 컬럼의 이름·타입·순서를 바꾸지 못하고, 중간에 끼워 넣으면
-- "cannot change name of view column" 으로 마이그레이션이 실패한다.
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
                  AND tat.expires_at > CURRENT_TIMESTAMP) AS secure_qr_access,
       -- 마이페이지는 "어떤 박람회에 며칠, 몇 명 입장 가능한지" 를 보여준다. 여기까지의 필드에는
       -- 행사 기간도 수량도 없어 화면이 티켓 코드와 상태밖에 그릴 수 없었다.
       e.event_start_at  AS event_start_at,
       e.event_end_at    AS event_end_at,
       -- 이 티켓이 속한 주문 항목의 구매 수량. 티켓 1장이 아니라 "몇 장짜리 주문인지" 를 뜻한다.
       oi.quantity       AS order_item_quantity
FROM issued_tickets    it
JOIN ticket_order_items oi ON oi.id = it.ticket_order_item_id
JOIN ticket_orders      o  ON o.id  = oi.ticket_order_id
JOIN expos              e  ON e.id  = it.expo_id
WHERE o.member_user_id IS NOT NULL;
