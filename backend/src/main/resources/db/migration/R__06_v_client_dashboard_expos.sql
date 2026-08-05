-- 클라이언트 본인 등록 박람회 목록  →  com.expo.expo
-- 주의: 출력 필드는 명세에 없어 추정한 것이다. 원본은 "본인 등록 박람회 목록,
--       심사 및 노출 상태"라는 한 줄 용도만 제시했다. 문서 부록 E-2 참조.
CREATE OR REPLACE VIEW v_client_dashboard_expos AS
SELECT e.host_client_id    AS client_user_id,
       e.id                AS expo_id,
       e.title             AS title,
       e.event_start_at    AS event_start_at,
       e.event_end_at      AS event_end_at,
       e.sales_start_at    AS sales_start_at,
       e.sales_end_at      AS sales_end_at,
       e.review_status     AS review_status,
       e.visibility_status AS visibility_status,
       e.event_status      AS event_status,
       e.approved_at       AS approved_at,
       COUNT(tp.id)        AS ticket_product_count
FROM expos e
LEFT JOIN ticket_products tp ON tp.expo_id = e.id
GROUP BY e.host_client_id, e.id, e.title, e.event_start_at, e.event_end_at,
         e.sales_start_at, e.sales_end_at, e.review_status, e.visibility_status,
         e.event_status, e.approved_at;
