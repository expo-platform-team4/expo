-- 희망 부스 구성·제출 자료 요구사항은 실제로는 자유 텍스트로 쓰인다 - 관리자가
-- "사업자등록증 사본 제출" 같은 안내문을 그대로 적고, 화면도 그 값을 파싱 없이 그대로
-- 보여준다(JSON 구조를 읽어서 렌더링하는 코드가 없다).
--
-- 그런데 컬럼이 jsonb라서, 자유 텍스트를 넣으면 PostgreSQL이 "invalid input syntax for
-- type json"으로 거부하고 그게 뭉뚱그려진 "입력값이 올바르지 않습니다"로 사용자에게
-- 노출됐다. 실제 쓰임(자유 텍스트)에 맞게 컬럼 타입을 text로 바꾼다.
--
-- 두 컬럼 다 지금 전부 NULL이라(이 값을 실제로 채운 요청·공고가 아직 없다) 데이터
-- 변환 걱정 없이 타입만 바꾸면 된다.
ALTER TABLE recruitment_notice_requests
    ALTER COLUMN requested_booth_config TYPE TEXT;

ALTER TABLE recruitment_notices
    ALTER COLUMN submission_requirements TYPE TEXT;

COMMENT ON COLUMN recruitment_notice_requests.requested_booth_config IS
    '희망 부스 구성 메모. 자유 텍스트(JSON이 아니다).';
COMMENT ON COLUMN recruitment_notices.submission_requirements IS
    '제출 자료 요구사항. 자유 텍스트(JSON이 아니다) - 신청 기업에게 그대로 노출된다.';
