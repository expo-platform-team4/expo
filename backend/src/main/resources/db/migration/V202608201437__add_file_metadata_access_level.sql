-- file_metadata 에 공개 범위를 추가한다.
--
-- V1 의 file_metadata 는 file_status(ACTIVE/DELETED/QUARANTINED) 만 갖는다. 그건 파일이
-- "살아 있느냐" 이지 "누가 볼 수 있느냐" 가 아니다. 그래서 파일 하나를 놓고 인증 없이 내줘도
-- 되는지 판정할 근거가 테이블에 없었다.
--
-- 참조 테이블에서 역추적하는 방법은 쓰지 않는다. file_metadata 를 가리키는 FK 가 11개 테이블에
-- 12개라, 파일 한 건을 내줄 때마다 그 전부를 뒤져야 하고 참조가 없는 파일은 판정 자체가 불가능하다.
--
-- 기본값은 PRIVATE 다. 판단이 안 서면 막는 쪽으로 닫힌다. 기존 행은 0건이라 백필은 필요 없다.
ALTER TABLE file_metadata
    ADD COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
        CHECK (access_level IN ('PUBLIC', 'PRIVATE'));

COMMENT ON COLUMN file_metadata.access_level IS
    'PUBLIC = 인증 없이 조회 가능(박람회 이미지 등), PRIVATE = 업로더 본인과 관리자만(정산 리포트 등)';
