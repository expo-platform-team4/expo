-- 모집공고(요청)에 어느 박람회를 위한 것인지 연결한다.
--
-- 지금까지 recruitment_notice_requests·recruitment_notices 는 expo_id 를 전혀 안 가지고
-- 있어서, 이 모집공고가 어느 박람회를 위한 건지 표현할 방법이 없었다(booths 도 venue_zone_id
-- 만 가져서 마찬가지다).
--
-- "클라이언트는 박람회 개최 신청만 하고, 기업 모집·부스 배치는 관리자(플랫폼)가 담당한다"는
-- 방향으로 서비스 흐름을 정리하면서, 관리자가 "이 승인된 박람회를 위해 기업을 모집한다"를
-- 표현할 수 있어야 한다 — 그 첫 단계로 FK 만 먼저 추가한다.
--
-- NULL 허용이다. 지금 당장은 어떤 서비스 로직도 이 컬럼을 채우지 않는다(모집공고 생성 주체를
-- 클라이언트→관리자로 옮기는 작업이 뒤따라야 실제로 값이 들어간다). 기존 행은 전부 NULL로 남는다.
ALTER TABLE recruitment_notice_requests
    ADD COLUMN expo_id BIGINT REFERENCES expos(id);

-- host_client_id 와 같은 비정규화 패턴 — 공고 생성 시 요청의 expo_id 를 그대로 이어받는다.
ALTER TABLE recruitment_notices
    ADD COLUMN expo_id BIGINT REFERENCES expos(id);

CREATE INDEX idx_recruitment_notice_requests_expo ON recruitment_notice_requests (expo_id);
CREATE INDEX idx_recruitment_notices_expo ON recruitment_notices (expo_id);

COMMENT ON COLUMN recruitment_notice_requests.expo_id IS
    '이 모집공고 요청이 어느 박람회를 위한 것인지. NULL 이면 아직 연결 안 된 요청(과거 데이터 또는 전환 전).';
COMMENT ON COLUMN recruitment_notices.expo_id IS
    'recruitment_notice_requests.expo_id 를 공고 생성 시 그대로 이어받는다.';
