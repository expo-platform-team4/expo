-- 참여 신청 취소·결제 실패 후 같은 공고에 재신청할 수 있도록, (recruitment_notice_id, client_user_id)
-- 단일 컬럼 조합 UNIQUE 를 "유효한(취소·결제 실패가 아닌) 신청 1건만" 허용하는 부분 UNIQUE 인덱스로 바꾼다.
ALTER TABLE participation_applications DROP CONSTRAINT uq_participation_applications_client;
CREATE UNIQUE INDEX uq_participation_applications_active_client
    ON participation_applications (recruitment_notice_id, client_user_id)
    WHERE status NOT IN ('PAYMENT_FAILED', 'CANCELED');
