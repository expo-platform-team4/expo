-- 관리자 대시보드 처리 대기 건수 집계  →  com.expo.admin
-- 단일 행을 반환한다.
-- 주의: 출력 필드는 명세에 없어 추정한 것이다. 6-12 절이 나열한 관리자 업무
--       (박람회·배너 심사, 모집공고 생성 요청, 장소 충돌 검토, 참여 신청 운영 확인,
--       정산 상태)를 각각 건수 필드로 옮겼다. 문서 부록 E-2 참조.
CREATE OR REPLACE VIEW v_admin_dashboard_counts AS
SELECT
    (SELECT COUNT(*) FROM expo_opening_requests
      WHERE status IN ('SUBMITTED', 'UNDER_REVIEW'))                  AS pending_expo_review_count,
    (SELECT COUNT(*) FROM banner_applications
      WHERE review_status = 'UNDER_REVIEW')                           AS pending_banner_review_count,
    (SELECT COUNT(*) FROM recruitment_notice_requests
      WHERE status IN ('SUBMITTED', 'UNDER_REVIEW'))                  AS pending_notice_request_count,
    (SELECT COUNT(*) FROM recruitment_notice_requests
      WHERE venue_conflict_status = 'CONFLICT_PENDING')               AS venue_conflict_count,
    (SELECT COUNT(*) FROM participation_applications
      WHERE status = 'SUBMITTED' AND admin_checked_at IS NULL)        AS unchecked_application_count,
    (SELECT COUNT(*) FROM expo_change_requests
      WHERE status IN ('SUBMITTED', 'UNDER_REVIEW'))                  AS pending_change_request_count,
    (SELECT COUNT(*) FROM expo_cancellation_requests
      WHERE status = 'SUBMITTED')                                     AS pending_cancellation_request_count,
    (SELECT COUNT(*) FROM settlements
      WHERE status IN ('WAITING', 'CALCULATED', 'UNDER_REVIEW'))      AS settlement_waiting_count;
