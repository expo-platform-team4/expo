-- [auth] 회원가입 이메일 본인인증 요청·결과.
--
-- phone_verifications 와 같은 패턴이지만 컬럼을 하나 더 둔다. phone_verifications 는
-- verification_token_hash 하나를 "코드 해시 → (인증 성공 시) 토큰 해시"로 재사용하는데,
-- 여기서는 코드 해시(verification_code_hash)와 회원가입용 1회성 토큰 해시
-- (signup_token_hash)를 컬럼으로 분리했다 — 같은 컬럼이 시점에 따라 다른 값을 담으면
-- USED 상태 도입 후 "이 값이 지금 코드 해시인지 토큰 해시인지"를 상태만 보고 매번
-- 따져야 해서다.
--
-- USED 상태는 phone_verifications 에는 없다 — 이메일 인증은 회원가입(AuthService.signup)이
-- 토큰을 실제로 소비하므로, 한 번 가입에 쓴 토큰을 다시 못 쓰게 구분할 상태가 필요하다.
CREATE TABLE email_verifications (
    id                      BIGSERIAL    PRIMARY KEY,
    user_id                 BIGINT       NULL,
    email                   VARCHAR(255) NOT NULL,
    verification_code_hash  VARCHAR(255) NOT NULL,
    signup_token_hash       VARCHAR(255) NULL,
    status                  VARCHAR(20)  NOT NULL
        CHECK (status IN ('REQUESTED', 'VERIFIED', 'FAILED', 'EXPIRED', 'USED')),
    requested_at            TIMESTAMPTZ  NOT NULL,
    verified_at             TIMESTAMPTZ  NULL,
    expires_at              TIMESTAMPTZ  NOT NULL
);

-- 회원가입 시 "이 이메일로 VERIFIED 상태인 인증이 있는가"를 조회하는 조건과 정확히 같다.
CREATE INDEX idx_email_verifications_email_status ON email_verifications (email, status);
