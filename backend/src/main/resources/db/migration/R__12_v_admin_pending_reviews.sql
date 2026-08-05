-- 심사 대기 목록 통합  →  com.expo.admin
-- 주의: 출력 필드와 UNION ALL 구조 모두 추정이다. 6-12 절은 "박람회·배너 심사,
--       모집공고 생성 요청 …을 통합 조회한다"는 서술만 있다. 세 원천을 한 뷰로
--       합치는 구조를 가정했다. 화면을 나눠 쓸 계획이었다면 뷰를 쪼개야 한다.
--       문서 D-4 14 참조.
CREATE OR REPLACE VIEW v_admin_pending_reviews AS
SELECT 'EXPO_OPENING'::VARCHAR(20) AS review_target_type,
       r.id                        AS target_id,
       r.title                     AS title,
       r.host_client_id            AS requester_client_id,
       r.status                    AS status,
       r.submitted_at              AS submitted_at,
       r.created_at                AS created_at
FROM expo_opening_requests r
WHERE r.status IN ('SUBMITTED', 'UNDER_REVIEW')
UNION ALL
SELECT 'BANNER'::VARCHAR(20),
       b.id,
       COALESCE(b.headline, e.title),
       b.client_user_id,
       b.review_status,
       b.submitted_at,
       b.created_at
FROM banner_applications b
JOIN expos e ON e.id = b.expo_id
WHERE b.review_status = 'UNDER_REVIEW'
UNION ALL
SELECT 'NOTICE_REQUEST'::VARCHAR(20),
       n.id,
       n.title,
       n.host_client_id,
       n.status,
       n.submitted_at,
       n.created_at
FROM recruitment_notice_requests n
WHERE n.status IN ('SUBMITTED', 'UNDER_REVIEW');
