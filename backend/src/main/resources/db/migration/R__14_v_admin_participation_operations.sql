-- 참여 신청 운영 확인 목록  →  com.expo.admin
-- 근거: docs/init_table_schema.md 6-12. 출력 필드는 명세에 정의되어 있다.
CREATE OR REPLACE VIEW v_admin_participation_operations AS
SELECT pa.id                        AS application_id,
       pa.recruitment_notice_id     AS recruitment_notice_id,
       pa.client_user_id            AS client_user_id,
       pa.company_name_snapshot     AS company_name,
       pa.status                    AS application_status,
       pa.selected_booth_product_id AS selected_booth_product_id,
       bo.id                        AS booth_order_id,
       bo.status                    AS booth_order_status,
       bp.status                    AS payment_status,
       ba.id                        AS booth_allocation_id,
       ba.status                    AS allocation_status,
       pa.admin_checked_at          AS admin_checked_at,
       pa.admin_checked_by          AS admin_checked_by,
       op.action_type               AS latest_operation_type,
       op.message                   AS latest_operation_message,
       pa.created_at                AS created_at,
       pa.submitted_at              AS submitted_at
FROM participation_applications pa
LEFT JOIN booth_orders      bo ON bo.id = pa.booth_order_id
LEFT JOIN booth_allocations ba ON ba.application_id = pa.id
-- 결제와 운영 이력은 최신 1건만 노출한다.
LEFT JOIN LATERAL (
    SELECT p.status FROM booth_payments p
     WHERE p.booth_order_id = bo.id
     ORDER BY p.created_at DESC LIMIT 1
) bp ON TRUE
LEFT JOIN LATERAL (
    SELECT h.action_type, h.message FROM application_operation_histories h
     WHERE h.application_id = pa.id
     ORDER BY h.created_at DESC LIMIT 1
) op ON TRUE;
