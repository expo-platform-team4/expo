-- 회원·클라이언트 계정 검색·상세 조회  →  com.expo.admin
-- 주의: users 는 role(MEMBER/CLIENT/ADMIN) 공통 계정 테이블이고, client_profiles 는
--       role=CLIENT 인 계정에만 존재하는 1:1 사업자 프로필이라 LEFT JOIN 한다.
CREATE OR REPLACE VIEW v_admin_users AS
SELECT u.id                          AS user_id,
       u.email                       AS email,
       u.nickname                    AS nickname,
       u.role                        AS role,
       u.account_status              AS account_status,
       u.phone_number                AS phone_number,
       u.phone_verified_at           AS phone_verified_at,
       u.last_login_at               AS last_login_at,
       u.withdrawn_at                AS withdrawn_at,
       u.created_at                  AS created_at,
       cp.company_name               AS company_name,
       cp.business_number            AS business_number,
       cp.representative_name        AS representative_name,
       cp.business_address           AS business_address,
       cp.business_type              AS business_type,
       cp.business_number_verified   AS business_number_verified,
       cp.business_verified_at       AS business_verified_at,
       cp.verification_provider      AS verification_provider
FROM users u
LEFT JOIN client_profiles cp ON cp.user_id = u.id;
