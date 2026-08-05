-- 참여 신청별 부스 진행 상태  →  com.expo.booth
-- 주의: 출력 필드는 명세에 없어 추정한 것이다. 용도 문장의 다섯 항목(신청 상태,
--       부스 주문, 결제 상태, 결제 완료 시각, 확정 부스)을 그대로 옮기고
--       관리자용 v_admin_participation_operations 의 구성을 참고했다. 문서 부록 E-2 참조.
CREATE OR REPLACE VIEW v_client_dashboard_booths AS
SELECT pa.client_user_id        AS client_user_id,
       pa.recruitment_notice_id AS recruitment_notice_id,
       pa.id                    AS application_id,
       pa.status                AS application_status,
       bo.id                    AS booth_order_id,
       bo.status                AS booth_order_status,
       bp.status                AS payment_status,
       bo.paid_at               AS paid_at,
       ba.id                    AS booth_allocation_id,
       ba.status                AS allocation_status,
       b.booth_number           AS booth_number
FROM participation_applications pa
LEFT JOIN booth_orders      bo ON bo.id = pa.booth_order_id
LEFT JOIN booth_allocations ba ON ba.application_id = pa.id
LEFT JOIN booth_products    bpr ON bpr.id = ba.booth_product_id
LEFT JOIN booths            b  ON b.id = bpr.booth_id
-- 결제는 최신 1건만 노출한다.
LEFT JOIN LATERAL (
    SELECT p.status FROM booth_payments p
     WHERE p.booth_order_id = bo.id
     ORDER BY p.created_at DESC, p.id DESC LIMIT 1
) bp ON TRUE;
