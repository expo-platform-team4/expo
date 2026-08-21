-- 같은 주문을 두 번 발권하는 것을 DB 가 막는다 (이슈 #75).
--
-- 지금 방어는 애플리케이션에만 있다 — findOrderForIssuance 가 주문 행을 FOR UPDATE 로
-- 잠그고, 그 뒤 countIssuedTickets 로 세어 본다. 동작은 하지만 두 가지 전제에 기대고 있고
-- 둘 다 코드를 읽어야만 알 수 있다.
--
--   1. 잠금과 개수 조회의 순서 — 두 줄을 바꾸면 방어가 사라진다
--   2. READ COMMITTED 격리 수준 — 올리면 옛 스냅샷을 보고 뚫린다
--
-- 어느 쪽을 어겨도 모든 테스트가 통과한다. 그래서 DB 로도 강제한다.
--
-- issued_tickets 의 기존 UNIQUE(ticket_code, qr_token_hash)는 도움이 안 된다. 셋 다
-- 발권할 때마다 새로 만들어지는 값이라 "이 티켓이 중복인가" 만 볼 뿐, "이 주문이 이미
-- 발권됐나" 는 보지 못한다.
--
-- 대신 접근 토큰에 건다. 발권은 주문당 ORDER_VIEW 토큰을 정확히 하나 만들고, 그 저장이
-- 티켓 저장과 같은 트랜잭션 안에 있다(TicketIssueService.issue). 따라서 두 번째 발권은
-- 이 인덱스에 걸려 INSERT 가 실패하고, 트랜잭션이 통째로 롤백되어 티켓도 남지 않는다.
--
-- status='ACTIVE' 로 한정하는 이유는 알림 재발송 때문이다. 재발송은 살아 있는 토큰을
-- REVOKED 로 바꾸고 새 토큰을 발급한다(TicketIssuedTextRebuilder). 상태를 빼면 그
-- 정상 동작이 막힌다.
CREATE UNIQUE INDEX uq_ticket_access_tokens_active_order_view
    ON ticket_access_tokens (ticket_order_id)
    WHERE scope = 'ORDER_VIEW' AND status = 'ACTIVE';

COMMENT ON INDEX uq_ticket_access_tokens_active_order_view IS
    '주문당 살아 있는 조회 링크는 하나. 중복 발권을 DB 수준에서 막는 역할도 한다 (이슈 #75)';
