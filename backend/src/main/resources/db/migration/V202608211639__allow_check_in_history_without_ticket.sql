-- 위조·미등록 QR 스캔 시도를 이력에 남길 수 있게 한다 (이슈 #73).
--
-- check_in_histories.result 의 CHECK 는 처음부터 INVALID_TOKEN 을 허용했는데, 정작
-- issued_ticket_id 가 NOT NULL 이라 그 값으로 행을 만들 수가 없었다. CHECK 와 FK 제약이
-- 서로 어긋나 있었던 셈이다. 그래서 다섯 갈래 중 위조만 이력에서 빠져 감사 화면이
-- 반쪽짜리였다 — 운영자가 "오늘 거절된 시도" 를 봐도 위조는 안 보인다.
--
-- 반복 위조 시도는 공격 탐지의 핵심 신호다. 로그에는 남지만 IP 가 마스킹되어 있고
-- (docs/logging.md 7절), 로그를 파싱하지 않으면 집계할 수 없다. 이력 테이블에 남기면
-- request_ip 원문이 함께 남아 "같은 출처에서 몇 번" 을 SQL 로 셀 수 있다.
--
-- 잃는 것은 "이력은 반드시 티켓을 가리킨다" 는 불변식이다. 대신 result 가 그 자리를 메운다 —
-- issued_ticket_id 가 NULL 인 행은 INVALID_TOKEN 뿐이고, 아래 CHECK 가 그것을 강제한다.
ALTER TABLE check_in_histories
    ALTER COLUMN issued_ticket_id DROP NOT NULL;

-- NULL 을 허용하되 아무 데서나 비어 있지는 않게 막는다.
-- 티켓을 찾은 판정(SUCCESS/ALREADY_USED/...)은 여전히 티켓을 가리켜야 한다.
ALTER TABLE check_in_histories
    ADD CONSTRAINT ck_check_in_histories_ticket_required
        CHECK (issued_ticket_id IS NOT NULL OR result = 'INVALID_TOKEN');

COMMENT ON COLUMN check_in_histories.issued_ticket_id IS
    '스캔이 가리킨 발권 티켓. INVALID_TOKEN(위조·미등록 QR)일 때만 NULL 이다';
