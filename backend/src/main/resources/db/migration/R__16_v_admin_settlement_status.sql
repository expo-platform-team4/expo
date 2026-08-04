-- 정산과 송금 상태 통합 조회  →  com.expo.admin
-- 주의: 출력 필드는 명세에 없어 추정한 것이다. v_client_dashboard_settlements 의
--       필수 출력 필드에서 클라이언트 전용 항목을 덜어내고 관리자에게 필요한
--       확정·송금 정보를 남기는 구성으로 잡았다. 문서 부록 E-2 참조.
CREATE OR REPLACE VIEW v_admin_settlement_status AS
SELECT s.id                    AS settlement_id,
       s.status                AS status,
       s.expo_id               AS expo_id,
       e.title                 AS expo_title,
       e.event_end_at          AS event_end_at,
       s.host_client_id        AS host_client_id,
       cp.company_name         AS company_name,
       s.settlement_due_at     AS settlement_due_at,
       s.remittance_due_amount AS remittance_due_amount,
       s.adjustment_amount     AS adjustment_amount,
       s.confirmed_at          AS confirmed_at,
       s.confirmed_by          AS confirmed_by,
       rm.remitted_amount      AS remitted_amount,
       rm.remitted_at          AS remitted_at,
       rm.status               AS remittance_status
FROM settlements s
JOIN expos           e  ON e.id = s.expo_id
JOIN client_profiles cp ON cp.user_id = s.host_client_id
LEFT JOIN LATERAL (
    SELECT r.remitted_amount, r.remitted_at, r.status
      FROM remittances r
     WHERE r.settlement_id = s.id
     ORDER BY r.created_at DESC LIMIT 1
) rm ON TRUE;
