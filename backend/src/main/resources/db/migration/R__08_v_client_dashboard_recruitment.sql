-- 모집 공고와 신청 완료 수, 확정 배정 수  →  com.expo.recruitment
-- 주의: 출력 필드는 명세에 없어 추정한 것이다. 용도 문장의 세 항목을 그대로 옮겼다.
CREATE OR REPLACE VIEW v_client_dashboard_recruitment AS
SELECT rn.host_client_id       AS host_client_id,
       rn.id                   AS recruitment_notice_id,
       rn.title                AS title,
       rn.status               AS status,
       rn.application_start_at AS application_start_at,
       rn.application_end_at   AS application_end_at,
       rn.published_at         AS published_at,
       rn.closed_at            AS closed_at,
       COUNT(DISTINCT pa.id) FILTER (WHERE pa.status = 'SUBMITTED') AS submitted_application_count,
       COUNT(DISTINCT ba.id) FILTER (WHERE ba.status = 'ASSIGNED')  AS confirmed_allocation_count
FROM recruitment_notices rn
LEFT JOIN participation_applications pa ON pa.recruitment_notice_id = rn.id
LEFT JOIN booth_allocations          ba ON ba.application_id = pa.id
GROUP BY rn.host_client_id, rn.id, rn.title, rn.status,
         rn.application_start_at, rn.application_end_at, rn.published_at, rn.closed_at;
