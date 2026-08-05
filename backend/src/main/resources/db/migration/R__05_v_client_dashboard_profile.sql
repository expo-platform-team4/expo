-- 클라이언트 페이지 상단 프로필  →  com.expo.member
-- 근거: docs/init_table_schema.md 6-9. 출력 필드는 명세에 정의되어 있다.
-- profile_image_url 은 storage_key 기반 서명 URL 을 애플리케이션이 만들어야 하므로
-- 뷰에서는 원본 키만 넘긴다.
CREATE OR REPLACE VIEW v_client_dashboard_profile AS
SELECT u.id                       AS client_user_id,
       u.nickname                 AS nickname,
       cp.company_name            AS company_name,
       u.profile_image_file_id    AS profile_image_file_id,
       fm.storage_key             AS profile_image_storage_key,
       u.profile_image_updated_at AS profile_image_updated_at
FROM users u
JOIN client_profiles cp ON cp.user_id = u.id
LEFT JOIN file_metadata fm ON fm.id = u.profile_image_file_id;
