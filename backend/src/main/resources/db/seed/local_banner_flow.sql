-- 로컬 배너 광고 검증용 시드 데이터.
--
-- ⚠ 이 파일은 Flyway 가 실행하지 않는다. db/migration 이 아니라 db/seed 에 있는 이유가 그것이다.
--   마이그레이션에 넣으면 운영 DB 에도 들어간다. 반드시 손으로 실행한다.
--
--   docker exec -i expo-postgres psql -U expo -d expo \
--     < backend/src/main/resources/db/seed/local_banner_flow.sql
--
-- 왜 필요한가 — 배너 화면은 셋인데(공개 노출 · 주최사 내 신청 · 관리자 심사) 각각 다른 상태의
-- 데이터를 보여준다. 승인 하나만 만들어 두면 "심사 대기 목록" 도 "반려 사유 표시" 도 확인할 수
-- 없다. 그래서 상태를 골고루 만든다.
--
-- 만드는 것
--   UNDER_REVIEW  2건  관리자 심사 화면에 뜬다. 둘은 기간이 겹쳐 충돌 판정에 잡힌다
--   APPROVED      3건  → 배너 ACTIVE 2 · SCHEDULED 1
--   REJECTED      1건  주최사 화면에서 반려 사유가 보인다
--
-- 배너는 신청 없이 존재할 수 없다(banner_application_id 가 NOT NULL). 이미지도 마찬가지다.
-- 그래서 파일 → 신청 → 배너 순으로 만든다. 운영에서 승인이 만드는 경로와 같은 모양이다.
--
-- 되돌리기는 파일 맨 아래에 있다.

BEGIN;

DO $$
DECLARE
  v_slot   bigint;
  v_host   bigint;
  v_file   bigint;
  v_app    bigint;
  v_expo1  bigint;
  v_expo2  bigint;
  v_expo3  bigint;
BEGIN
  SELECT id INTO v_slot FROM banner_slots WHERE active ORDER BY id LIMIT 1;
  IF v_slot IS NULL THEN
    RAISE EXCEPTION '활성 배너 슬롯이 없다. banner_slots 를 먼저 확인하라.';
  END IF;

  -- 공개된 박람회 셋을 고른다. 배너는 홍보 대상 박람회를 반드시 가리킨다.
  SELECT id INTO v_expo1 FROM expos
   WHERE review_status = 'APPROVED' AND visibility_status = 'PUBLIC' ORDER BY id LIMIT 1;
  SELECT id INTO v_expo2 FROM expos
   WHERE review_status = 'APPROVED' AND visibility_status = 'PUBLIC' AND id <> v_expo1
   ORDER BY id LIMIT 1;
  SELECT id INTO v_expo3 FROM expos
   WHERE review_status = 'APPROVED' AND visibility_status = 'PUBLIC' AND id NOT IN (v_expo1, v_expo2)
   ORDER BY id LIMIT 1;

  IF v_expo2 IS NULL THEN
    RAISE EXCEPTION '공개된 박람회가 2개 미만이다. 박람회 시드를 먼저 넣어라.';
  END IF;
  v_expo3 := COALESCE(v_expo3, v_expo1);

  SELECT host_client_id INTO v_host FROM expos WHERE id = v_expo1;

  -- 배너 이미지. 실제 파일은 없다 — 화면이 file_id 만 쓰므로 메타데이터로 충분하다.
  INSERT INTO file_metadata (storage_provider, bucket_name, storage_key, original_filename,
         content_type, file_size, file_status)
  VALUES ('S3', 'expo-local', 'seed/banner-main.png', 'banner-main.png',
          'image/png', 204800, 'ACTIVE')
  RETURNING id INTO v_file;

  -- ── 1. 지금 노출 중인 배너 2건 ─────────────────────────────────────────
  --    공개 화면(GET /api/banners/active)에 바로 뜬다.
  INSERT INTO banner_applications (client_user_id, expo_id, image_file_id, headline,
         requested_start_at, requested_end_at, review_status, submitted_at,
         reviewed_by_admin_id, reviewed_at)
  VALUES (v_host, v_expo1, v_file, '[시드] 지금 노출 중인 배너 A',
          now() - interval '2 day', now() + interval '12 day', 'APPROVED',
          now() - interval '3 day', NULL, now() - interval '2 day')
  RETURNING id INTO v_app;

  INSERT INTO banners (banner_application_id, banner_slot_id, expo_id, image_file_id, headline,
         start_at, end_at, display_status, sort_order, activated_at)
  VALUES (v_app, v_slot, v_expo1, v_file, '[시드] 지금 노출 중인 배너 A',
          now() - interval '2 day', now() + interval '12 day', 'ACTIVE', 0,
          now() - interval '2 day');

  INSERT INTO banner_applications (client_user_id, expo_id, image_file_id, headline,
         requested_start_at, requested_end_at, review_status, submitted_at, reviewed_at)
  VALUES (v_host, v_expo2, v_file, '[시드] 지금 노출 중인 배너 B',
          now() - interval '1 day', now() + interval '20 day', 'APPROVED',
          now() - interval '2 day', now() - interval '1 day')
  RETURNING id INTO v_app;

  INSERT INTO banners (banner_application_id, banner_slot_id, expo_id, image_file_id, headline,
         start_at, end_at, display_status, sort_order, activated_at)
  VALUES (v_app, v_slot, v_expo2, v_file, '[시드] 지금 노출 중인 배너 B',
          now() - interval '1 day', now() + interval '20 day', 'ACTIVE', 1,
          now() - interval '1 day');

  -- ── 2. 예약 배너 1건 ───────────────────────────────────────────────────
  --    아직 시작 전이라 공개 화면에 안 뜬다.
  --    내부 API 를 부르면 시작일 도달 시 ACTIVE 로 넘어간다.
  --      POST /api/internal/banners/sync-display-status
  INSERT INTO banner_applications (client_user_id, expo_id, image_file_id, headline,
         requested_start_at, requested_end_at, review_status, submitted_at, reviewed_at)
  VALUES (v_host, v_expo3, v_file, '[시드] 다음 주에 시작하는 예약 배너',
          now() + interval '7 day', now() + interval '30 day', 'APPROVED',
          now() - interval '1 day', now() - interval '1 day')
  RETURNING id INTO v_app;

  INSERT INTO banners (banner_application_id, banner_slot_id, expo_id, image_file_id, headline,
         start_at, end_at, display_status, sort_order)
  VALUES (v_app, v_slot, v_expo3, v_file, '[시드] 다음 주에 시작하는 예약 배너',
          now() + interval '7 day', now() + interval '30 day', 'SCHEDULED', 2);

  -- ── 3. 심사 대기 2건 ───────────────────────────────────────────────────
  --    관리자 심사 화면(GET /api/admin/banner-requests)에 뜬다.
  --    둘의 기간이 서로 겹치고 위 노출 배너와도 겹쳐, 충돌 판정을 확인할 수 있다.
  --      GET /api/admin/banner-requests/{id}/conflicts
  INSERT INTO banner_applications (client_user_id, expo_id, image_file_id, headline,
         requested_start_at, requested_end_at, review_status, submitted_at)
  VALUES (v_host, v_expo1, v_file, '[시드] 심사 대기 — 기간이 겹친다',
          now() + interval '3 day', now() + interval '17 day', 'UNDER_REVIEW',
          now() - interval '4 hour');

  INSERT INTO banner_applications (client_user_id, expo_id, image_file_id, headline,
         requested_start_at, requested_end_at, review_status, submitted_at)
  VALUES (v_host, v_expo2, v_file, '[시드] 심사 대기 — 기간이 안 겹친다',
          now() + interval '60 day', now() + interval '75 day', 'UNDER_REVIEW',
          now() - interval '2 hour');

  -- ── 4. 반려 1건 ────────────────────────────────────────────────────────
  --    주최사 화면에서 반려 사유가 보인다.
  INSERT INTO banner_applications (client_user_id, expo_id, image_file_id, headline,
         requested_start_at, requested_end_at, review_status, submitted_at,
         reviewed_at, rejection_reason)
  VALUES (v_host, v_expo1, v_file, '[시드] 반려된 신청',
          now() + interval '5 day', now() + interval '10 day', 'REJECTED',
          now() - interval '5 day', now() - interval '4 day',
          '배너 이미지 해상도가 슬롯 규격(1200x400)에 맞지 않습니다.');

  RAISE NOTICE '배너 시드 완료 — slot=% expos=%,%,%', v_slot, v_expo1, v_expo2, v_expo3;
END $$;

COMMIT;

-- 확인
SELECT review_status, count(*) AS 신청 FROM banner_applications
 WHERE headline LIKE '[시드]%' GROUP BY review_status ORDER BY review_status;

SELECT display_status, count(*) AS 배너 FROM banners
 WHERE headline LIKE '[시드]%' GROUP BY display_status ORDER BY display_status;

-- 되돌리기
--   DELETE FROM banners WHERE headline LIKE '[시드]%';
--   DELETE FROM banner_applications WHERE headline LIKE '[시드]%';
--   DELETE FROM file_metadata WHERE storage_key = 'seed/banner-main.png';
