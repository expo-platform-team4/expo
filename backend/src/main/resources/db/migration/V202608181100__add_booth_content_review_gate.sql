-- 부스 콘텐츠 공개 전에 관리자 사전검수를 거치도록 UNDER_REVIEW 상태를 추가한다.
-- 지금까지는 클라이언트가 DRAFT/CORRECTION_REQUESTED 에서 바로 PUBLISHED 로 공개할 수 있었는데,
-- 이제는 클라이언트가 "검수 요청"(UNDER_REVIEW)만 할 수 있고, 관리자가 승인해야 PUBLISHED 로 넘어간다.
ALTER TABLE booth_contents DROP CONSTRAINT booth_contents_status_check;
ALTER TABLE booth_contents
    ADD CONSTRAINT booth_contents_status_check
    CHECK (status IN ('DRAFT', 'UNDER_REVIEW', 'PUBLISHED', 'CORRECTION_REQUESTED', 'HIDDEN'));

-- 관리자 승인(공개) 이력을 남기기 위한 운영 변경 종류 추가.
ALTER TABLE booth_management_histories DROP CONSTRAINT booth_management_histories_action_type_check;
ALTER TABLE booth_management_histories
    ADD CONSTRAINT booth_management_histories_action_type_check
    CHECK (action_type IN ('ALLOCATION_CORRECTED', 'INFORMATION_UPDATED', 'CONTENT_APPROVED',
                           'CORRECTION_REQUESTED', 'CONTENT_HIDDEN', 'CONTENT_RESTORED'));
