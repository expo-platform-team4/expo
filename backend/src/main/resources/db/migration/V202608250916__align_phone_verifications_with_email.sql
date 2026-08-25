-- [auth] 회원가입이 휴대폰 인증 결과를 실제로 검증하도록 phone_verifications 를 email_verifications 와 맞춘다.
--
-- 지금까지 회원가입(AuthService.signup)은 이메일 인증 토큰만 소비했다. 휴대폰은 인증을 통과해도
-- 그 결과를 아무도 확인하지 않아, API 를 직접 부르면 남의 번호로도 가입할 수 있었다.
-- 화면의 버튼만 잠그고 있던 셈이다.
--
-- 토큰을 소비하려면 세 가지가 없다.
--
-- 1) signup_token_hash — 지금은 verification_token_hash 하나를 "코드 해시 → (인증 성공 시)
--    토큰 해시"로 재사용한다. V202608211800(email) 이 이 방식을 따르지 않은 이유를 이미 적어 뒀다:
--    같은 컬럼이 시점에 따라 다른 값을 담으면 USED 도입 후 "이 값이 지금 코드 해시인지 토큰
--    해시인지"를 상태만 보고 매번 따져야 한다. 그 판단을 여기서도 하지 않도록 컬럼을 나눈다.
--
-- 2) signup_token_expires_at — expires_at 은 "인증번호"의 만료시각(기본 3분)이라 코드 확인
--    이후에는 더 이상 검사되지 않는다. 별도 만료가 없으면 가입토큰이 무기한 유효해진다.
--
-- 3) USED 상태 — 한 번 가입에 쓴 토큰을 다시 못 쓰게 구분한다. 없으면 같은 토큰으로
--    여러 계정을 만들 수 있다.
--
-- version 은 낙관적 락이다. 같은 레코드에 확인(confirm) 요청이 동시에 들어와 상태 전이가
-- 중복 적용되는 걸 막는다 (JPA @Version). email_verifications 와 같은 이유다.
--
-- 기존 VERIFIED 행은 signup_token_hash 가 NULL 로 남아 어떤 토큰과도 매칭되지 않는다.
-- 되살리지 않는다 — 이 기능 이전에 만들어진 인증이라 소비될 이유가 없고, 옛 값을 옮기면
-- 만료시각 없는 토큰이 살아난다.

ALTER TABLE phone_verifications
    ADD COLUMN signup_token_hash       VARCHAR(255) NULL,
    ADD COLUMN signup_token_expires_at TIMESTAMPTZ  NULL,
    ADD COLUMN version                 BIGINT       NOT NULL DEFAULT 0;

-- V1 이 인라인 무명 CHECK 로 만들었고, PostgreSQL 기본 이름이 아래와 같다.
-- (pg_constraint 로 확인한 실제 이름이다 — 추측이 아니다)
ALTER TABLE phone_verifications DROP CONSTRAINT phone_verifications_status_check;
ALTER TABLE phone_verifications ADD CONSTRAINT phone_verifications_status_check
    CHECK (status IN ('REQUESTED', 'VERIFIED', 'FAILED', 'EXPIRED', 'USED'));

-- 회원가입 시 "이 번호로 VERIFIED 상태인 인증이 있는가"를 조회하는 조건과 정확히 같다.
-- email_verifications 의 idx_email_verifications_email_status 와 짝이다.
CREATE INDEX idx_phone_verifications_phone_status ON phone_verifications (phone_number, status);
