-- 장소 충돌 검토 목록  →  com.expo.admin
-- 근거: docs/init_table_schema.md 6-12. 출력 필드는 명세에 정의되어 있다.
-- 같은 장소·홀·구역에서 사용 기간이 겹치는 요청을 하나의 충돌군으로 본다.
-- 정렬 기준은 submitted_at 오름차순(선착순)이다.
--
-- 요청이 홀을 여러 개 고를 수 있게 되면서 zone_id 단일 컬럼 대신 zone_ids 배열로 내려준다.
CREATE OR REPLACE VIEW v_admin_venue_conflicts AS
SELECT r.id                  AS request_id,
       r.host_client_id      AS host_client_id,
       r.virtual_venue_id    AS virtual_venue_id,
       r.venue_hall_id       AS hall_id,
       COALESCE(z.zone_ids, '{}') AS zone_ids,
       r.event_start_at      AS use_start_at,
       r.event_end_at        AS use_end_at,
       r.submitted_at        AS submitted_at,
       r.conflict_group_key  AS conflict_group_key,
       r.venue_decision      AS venue_decision,
       r.decided_by_admin_id AS decided_by_admin_id,
       r.decided_at          AS decided_at,
       r.decision_reason     AS cancellation_reason
FROM recruitment_notice_requests r
LEFT JOIN (
    SELECT request_id, array_agg(venue_zone_id ORDER BY venue_zone_id) AS zone_ids
    FROM recruitment_notice_request_zones
    GROUP BY request_id
) z ON z.request_id = r.id
WHERE r.status IN ('SUBMITTED', 'UNDER_REVIEW')
   OR r.venue_conflict_status = 'CONFLICT_PENDING';
