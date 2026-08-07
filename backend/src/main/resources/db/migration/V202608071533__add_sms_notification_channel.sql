-- 알림을 SMS 로 보낼 수 있게 하고, 발송 이력 테이블을 채널 중립으로 바꾼다.
--
-- 배경: V1 은 알림을 카카오 알림톡 전용으로 설계했다 (docs/init_table_schema.md 6-8).
--       실제 발송은 Solapi SMS 로 확정되어 channel CHECK 와 이력 테이블 이름이 실제와 어긋난다.
--       kakao_message_histories 는 참조하는 코드도 데이터도 없어 지금 바꾸는 비용이 0 이다.
-- 함께: issued_tickets.ticket_code 채번용 시퀀스를 만든다.


-- =============================================================================
-- 1. 알림 채널에 SMS 추가
-- =============================================================================

-- V1 이 CHECK 에 이름을 주지 않아 PostgreSQL 기본 이름(notifications_channel_check)이 붙어 있다.
-- 실제 DB 에서 확인한 이름이다. IF EXISTS 를 쓰지 않는다 — 이름이 다르면 조용히 넘어가는 대신
-- 여기서 실패해야 옛 CHECK 가 남은 채로 SMS 가 막히는 사고를 막을 수 있다.
ALTER TABLE notifications DROP CONSTRAINT notifications_channel_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_channel_check
    CHECK (channel IN ('KAKAO', 'SMS', 'EMAIL', 'IN_APP'));


-- =============================================================================
-- 2. kakao_message_histories → message_histories
-- =============================================================================

-- 테이블만 rename 하면 인덱스·제약·시퀀스는 옛 이름으로 남는다. 전부 같이 바꾼다.
ALTER TABLE kakao_message_histories RENAME TO message_histories;

ALTER TABLE message_histories
    RENAME CONSTRAINT fk_kakao_histories_notification TO fk_message_histories_notification;
ALTER TABLE message_histories
    RENAME CONSTRAINT uq_kakao_message_histories_attempt TO uq_message_histories_attempt;
ALTER TABLE message_histories
    RENAME CONSTRAINT kakao_message_histories_status_check TO message_histories_status_check;

ALTER INDEX kakao_message_histories_pkey RENAME TO message_histories_pkey;
ALTER SEQUENCE kakao_message_histories_id_seq RENAME TO message_histories_id_seq;

-- PostgreSQL 17 부터 NOT NULL 도 이름 있는 제약으로 카탈로그에 들어간다 (`<테이블>_<컬럼>_not_null`).
-- 이것까지 바꾸지 않으면 \d message_histories 에 kakao_ 이름이 그대로 남는다.
ALTER TABLE message_histories
    RENAME CONSTRAINT kakao_message_histories_id_not_null TO message_histories_id_not_null;
ALTER TABLE message_histories
    RENAME CONSTRAINT kakao_message_histories_notification_id_not_null
                   TO message_histories_notification_id_not_null;
ALTER TABLE message_histories
    RENAME CONSTRAINT kakao_message_histories_status_not_null TO message_histories_status_not_null;
ALTER TABLE message_histories
    RENAME CONSTRAINT kakao_message_histories_attempt_no_not_null
                   TO message_histories_attempt_no_not_null;
ALTER TABLE message_histories
    RENAME CONSTRAINT kakao_message_histories_requested_at_not_null
                   TO message_histories_requested_at_not_null;

-- 시도별 발송 채널.
-- notifications.channel 이 있는데도 여기 두는 이유는, 한 알림의 시도마다 채널이 달라질 수 있어서다.
-- Solapi 는 알림톡 실패 시 SMS 로 대체발송하는 것이 표준 동작이라 알림 단위 채널로는 표현되지 않는다.
--
-- 기존 행은 전부 카카오 발송이었으므로 DEFAULT 로 채운 뒤 기본값을 뗀다.
-- 기본값을 남겨 두면 애플리케이션이 채널을 빠뜨려도 조용히 KAKAO 로 기록된다.
ALTER TABLE message_histories ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'KAKAO';
ALTER TABLE message_histories ALTER COLUMN channel DROP DEFAULT;
ALTER TABLE message_histories ADD CONSTRAINT message_histories_channel_check
    CHECK (channel IN ('KAKAO', 'SMS', 'EMAIL'));


-- =============================================================================
-- 3. 발권 티켓 코드 채번 시퀀스
-- =============================================================================

-- issued_tickets.ticket_code 는 'EXPO-생성일자-일련번호 6자리' 형식이다 (6-7 절).
-- 일련번호를 애플리케이션 COUNT 로 매기면 동시 발권에서 같은 값이 두 번 나온다.
-- ticket_code 가 UNIQUE 라 그때는 INSERT 가 실패한다. 채번은 DB 에 맡긴다.
CREATE SEQUENCE seq_issued_ticket_code AS BIGINT START WITH 1 INCREMENT BY 1 NO CYCLE;
