-- 일반 회원 마이페이지 프로필  →  com.expo.member
-- 근거: docs/init_table_schema.md 6-2. 출력 필드는 명세에 정의되어 있다.
-- profile_image_url 은 storage_key 기반 서명 URL 을 애플리케이션이 만들어야 하므로
-- 뷰에서는 원본 키만 넘긴다.
CREATE OR REPLACE VIEW v_member_mypage_profile AS
SELECT u.id                       AS member_user_id,
       u.nickname                 AS nickname,
       u.profile_image_file_id    AS profile_image_file_id,
       fm.storage_key             AS profile_image_storage_key,
       u.profile_image_updated_at AS profile_image_updated_at
FROM users u
LEFT JOIN file_metadata fm ON fm.id = u.profile_image_file_id
WHERE u.role = 'MEMBER';
