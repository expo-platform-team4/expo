-- =============================================================================
-- dev_seed_expo.sql — 박람회 도메인 개발용 시드 (V1__init_schema.sql 기준)
--
-- ※ Flyway 버전 체인에 넣지 말 것. 로컬/개발 DB 전용 시드 스크립트다.
--    (팀이 시드도 마이그레이션으로 관리하기로 합의하면 V2__seed_expo_dev.sql 등으로 개명)
--
-- 기준일: 2026-08-07. 판매 상태(희-EXPO-10) 검증 시나리오:
--   expo 1  판매중(ON_SALE)     — 얼리버드만 매진
--   expo 2  판매예정(UPCOMING)  — sales_start_at 미래 (희-SRCH-13)
--   expo 3  매진(SOLD_OUT)      — 전 상품 available_quantity = 0
--   expo 4  판매종료(SALE_ENDED) — 판매 마감, 행사는 예정
--   expo 5  행사종료(EVENT_ENDED)
--   expo 6  인기 1위(POPULAR)   — 누적 판매 최다 (희-SRCH-09)
-- =============================================================================

-- ---------- 사용자 / 클라이언트 ----------
INSERT INTO users (id, email, password_hash, nickname, role, account_status)
VALUES (1, 'admin@expo.local', '{bcrypt}dev', '운영관리자', 'ADMIN', 'ACTIVE'),
       (2, 'hanbit@expo.local', '{bcrypt}dev', '한빛엑스포', 'CLIENT', 'ACTIVE'),
       (3, 'global@expo.local', '{bcrypt}dev', '글로벌페어', 'CLIENT', 'ACTIVE'),
       (4, 'gwangju@expo.local', '{bcrypt}dev', '광주컨벤션', 'CLIENT', 'ACTIVE');

INSERT INTO client_profiles (user_id, business_number, company_name, representative_name, business_address)
VALUES (2, '123-45-67890', '(주)한빛엑스포', '김한빛', '서울특별시 강남구 테헤란로 1'),
       (3, '234-56-78901', '글로벌페어 주식회사', '이글로', '경기도 고양시 일산서구 킨텍스로 2'),
       (4, '345-67-89012', '광주컨벤션기획', '박광주', '광주광역시 서구 상무누리로 3');

-- ---------- 카테고리 / 장소 ----------
INSERT INTO categories (id, name, slug, sort_order)
VALUES (1, 'IT·테크', 'it-tech', 1),
       (2, '푸드·외식', 'food', 2),
       (3, '뷰티·패션', 'beauty', 3),
       (4, '취업·교육', 'career', 4),
       (5, '문화·예술', 'culture', 5);

INSERT INTO virtual_venues (id, name, address, region_code, operational_status)
VALUES (1, '코엑스', '서울특별시 강남구 영동대로 513', 'SEOUL', 'ACTIVE'),
       (2, '킨텍스', '경기도 고양시 일산서구 킨텍스로 217', 'GYEONGGI', 'ACTIVE'),
       (3, '김대중컨벤션센터', '광주광역시 서구 상무누리로 30', 'GWANGJU', 'ACTIVE'),
       (4, '벡스코', '부산광역시 해운대구 APEC로 55', 'BUSAN', 'ACTIVE');

-- ---------- 파일 메타 (썸네일·카탈로그·영상) ----------
INSERT INTO file_metadata (id, uploader_user_id, storage_provider, bucket_name, storage_key,
                           original_filename, content_type, file_size, file_status)
VALUES (1, 2, 'S3', 'expo-dev', 'expo/1/thumbnail.jpg', 'ai-robotics.jpg', 'image/jpeg', 812345, 'ACTIVE'),
       (2, 3, 'S3', 'expo-dev', 'expo/2/thumbnail.jpg', 'k-beauty.jpg', 'image/jpeg', 734211, 'ACTIVE'),
       (3, 4, 'S3', 'expo-dev', 'expo/3/thumbnail.jpg', 'gwangju-food.jpg', 'image/jpeg', 698765, 'ACTIVE'),
       (4, 3, 'S3', 'expo-dev', 'expo/4/thumbnail.jpg', 'career-fair.jpg', 'image/jpeg', 654321, 'ACTIVE'),
       (5, 3, 'S3', 'expo-dev', 'expo/5/thumbnail.jpg', 'career-h1.jpg', 'image/jpeg', 612345, 'ACTIVE'),
       (6, 2, 'S3', 'expo-dev', 'expo/6/thumbnail.jpg', 'busan-illust.jpg', 'image/jpeg', 587654, 'ACTIVE'),
       (7, 2, 'S3', 'expo-dev', 'expo/1/catalog.pdf', 'catalog-2026.pdf', 'application/pdf', 4123456, 'ACTIVE'),
       (8, 2, 'S3', 'expo-dev', 'expo/1/promo.mp4', 'promo-60s.mp4', 'video/mp4', 98765432, 'ACTIVE'),
       (9, 2, 'S3', 'expo-dev', 'expo/1/leaflet.jpg', 'leaflet-ko.jpg', 'image/jpeg', 423456, 'ACTIVE');

-- ---------- 개최 신청 (expo_opening_requests) ----------
-- 심사 전 상태 (희-EXPO-01/02/05 검증용)
INSERT INTO expo_opening_requests
(id, host_client_id, title, description, event_start_at, event_end_at,
 sales_start_at, sales_end_at, desired_venue_id, status, submitted_at,
 reviewed_by_admin_id, reviewed_at, rejection_reason)
VALUES (1, 2, '2027 스마트팩토리 엑스포(작성중)', '스마트 제조 혁신 전시회 기획안',
        '2027-03-10 10:00:00+09', '2027-03-13 18:00:00+09',
        '2027-01-05 10:00:00+09', '2027-03-09 18:00:00+09', 1, 'DRAFT', NULL, NULL, NULL, NULL),
       (2, 3, '2026 글로벌 푸드 페스타', '전 세계 미식 트렌드를 한자리에서 만나는 푸드 박람회',
        '2026-11-05 10:00:00+09', '2026-11-08 18:00:00+09',
        '2026-09-01 10:00:00+09', '2026-11-04 18:00:00+09', 2, 'SUBMITTED',
        '2026-08-04 09:30:00+09', NULL, NULL, NULL),
       (3, 4, '2026 호남권 취업박람회', '지역 우수기업 200개사가 참여하는 채용 박람회',
        '2026-10-15 09:00:00+09', '2026-10-16 18:00:00+09',
        '2026-09-01 10:00:00+09', '2026-10-14 18:00:00+09', 3, 'UNDER_REVIEW',
        '2026-08-03 14:00:00+09', 1, NULL, NULL),
       (4, 2, '해운대 아트페어 2026', '신진 작가 중심의 현대미술 전시·판매전',
        '2026-12-01 10:00:00+09', '2026-12-05 18:00:00+09',
        '2026-10-01 10:00:00+09', '2026-11-30 18:00:00+09', 4, 'REJECTED',
        '2026-07-28 10:00:00+09', 1, '2026-07-30 16:00:00+09',
        '행사 상세 프로그램 및 안전 관리 계획서가 누락되었습니다. 보완 후 재요청 바랍니다.');

-- 승인 완료 → expos 로 이어지는 신청 (id 5~10 ↔ expos 1~6)
INSERT INTO expo_opening_requests
(id, host_client_id, title, description, event_start_at, event_end_at,
 sales_start_at, sales_end_at, desired_venue_id, status, submitted_at,
 reviewed_by_admin_id, reviewed_at)
VALUES (5, 2, '2026 서울 AI & 로보틱스 엑스포', '국내 최대 규모 AI·로봇 산업 전시회',
        '2026-09-10 10:00:00+09', '2026-09-13 18:00:00+09',
        '2026-07-01 10:00:00+09', '2026-09-09 18:00:00+09', 1, 'APPROVED',
        '2026-06-28 10:00:00+09', 1, '2026-07-01 09:00:00+09'),
       (6, 3, '2026 K-뷰티 월드', 'K-뷰티 브랜드 총출동, 신제품 체험·B2B 상담회',
        '2026-12-18 10:00:00+09', '2026-12-20 18:00:00+09',
        '2026-09-01 10:00:00+09', '2026-12-17 18:00:00+09', 1, 'APPROVED',
        '2026-08-01 10:00:00+09', 1, '2026-08-04 15:00:00+09'),
       (7, 4, '광주 미식 위크 2026', '남도 미식 문화와 로컬 셰프의 만남',
        '2026-10-01 10:00:00+09', '2026-10-04 20:00:00+09',
        '2026-07-15 10:00:00+09', '2026-09-30 18:00:00+09', 3, 'APPROVED',
        '2026-06-10 10:00:00+09', 1, '2026-06-12 10:00:00+09'),
       (8, 3, '2026 커리어 페어 시즌2', '하반기 공채 대비 대규모 취업 박람회',
        '2026-08-20 09:00:00+09', '2026-08-23 18:00:00+09',
        '2026-06-01 10:00:00+09', '2026-08-01 18:00:00+09', 2, 'APPROVED',
        '2026-05-20 10:00:00+09', 1, '2026-05-25 10:00:00+09'),
       (9, 3, '2026 상반기 커리어 페어', '상반기 공채 대비 대규모 취업 박람회',
        '2026-06-01 09:00:00+09', '2026-06-03 18:00:00+09',
        '2026-04-10 10:00:00+09', '2026-05-31 18:00:00+09', 2, 'APPROVED',
        '2026-04-01 10:00:00+09', 1, '2026-04-05 10:00:00+09'),
       (10, 2, '부산 국제 일러스트 페스티벌', '아시아 최대 일러스트·굿즈 페스티벌',
        '2026-09-25 10:00:00+09', '2026-09-27 20:00:00+09',
        '2026-05-10 10:00:00+09', '2026-09-24 18:00:00+09', 4, 'APPROVED',
        '2026-05-01 10:00:00+09', 1, '2026-05-03 10:00:00+09');

-- ---------- 공개 박람회 (expos) — 승인=자동 공개 PUBLIC (희-EXPO-09, 희-SRCH-12) ----------
INSERT INTO expos
(id, host_client_id, opening_request_id, title, description, region_code,
 event_start_at, event_end_at, sales_start_at, sales_end_at,
 review_status, visibility_status, event_status,
 approved_by_admin_id, approved_at)
VALUES
  -- 1) 판매중
  (1, 2, 5, '2026 서울 AI & 로보틱스 엑스포', '국내 최대 규모 AI·로봇 산업 전시회', 'SEOUL',
   '2026-09-10 10:00:00+09', '2026-09-13 18:00:00+09',
   '2026-07-01 10:00:00+09', '2026-09-09 18:00:00+09',
   'APPROVED', 'PUBLIC', 'SCHEDULED', 1, '2026-07-01 09:00:00+09'),
  -- 2) 판매예정 (sales_start_at 미래 — 희-SRCH-13)
  (2, 3, 6, '2026 K-뷰티 월드', 'K-뷰티 브랜드 총출동, 신제품 체험·B2B 상담회', 'SEOUL',
   '2026-12-18 10:00:00+09', '2026-12-20 18:00:00+09',
   '2026-09-01 10:00:00+09', '2026-12-17 18:00:00+09',
   'APPROVED', 'PUBLIC', 'SCHEDULED', 1, '2026-08-04 15:00:00+09'),
  -- 3) 매진
  (3, 4, 7, '광주 미식 위크 2026', '남도 미식 문화와 로컬 셰프의 만남', 'GWANGJU',
   '2026-10-01 10:00:00+09', '2026-10-04 20:00:00+09',
   '2026-07-15 10:00:00+09', '2026-09-30 18:00:00+09',
   'APPROVED', 'PUBLIC', 'SCHEDULED', 1, '2026-06-12 10:00:00+09'),
  -- 4) 판매종료 (판매 마감, 행사 예정)
  (4, 3, 8, '2026 커리어 페어 시즌2', '하반기 공채 대비 대규모 취업 박람회', 'GYEONGGI',
   '2026-08-20 09:00:00+09', '2026-08-23 18:00:00+09',
   '2026-06-01 10:00:00+09', '2026-08-01 18:00:00+09',
   'APPROVED', 'PUBLIC', 'SCHEDULED', 1, '2026-05-25 10:00:00+09'),
  -- 5) 행사종료
  (5, 3, 9, '2026 상반기 커리어 페어', '상반기 공채 대비 대규모 취업 박람회', 'GYEONGGI',
   '2026-06-01 09:00:00+09', '2026-06-03 18:00:00+09',
   '2026-04-10 10:00:00+09', '2026-05-31 18:00:00+09',
   'APPROVED', 'PUBLIC', 'ENDED', 1, '2026-04-05 10:00:00+09'),
  -- 6) 인기 1위 (판매량 최다 — 희-SRCH-09)
  (6, 2, 10, '부산 국제 일러스트 페스티벌', '아시아 최대 일러스트·굿즈 페스티벌', 'BUSAN',
   '2026-09-25 10:00:00+09', '2026-09-27 20:00:00+09',
   '2026-05-10 10:00:00+09', '2026-09-24 18:00:00+09',
   'APPROVED', 'PUBLIC', 'SCHEDULED', 1, '2026-05-03 10:00:00+09');

-- ---------- 장소 예약 + 박람회 배정 (EXPO_DIRECT 경로) ----------
INSERT INTO venue_reservations
(id, reservation_source_type, opening_request_id, virtual_venue_id,
 use_start_at, use_end_at, status, confirmed_by_admin_id, confirmed_at)
VALUES (1, 'EXPO_DIRECT', 5, 1, '2026-09-09 00:00:00+09', '2026-09-14 00:00:00+09', 'CONFIRMED', 1,
        '2026-07-01 09:00:00+09'),
       (2, 'EXPO_DIRECT', 6, 1, '2026-12-17 00:00:00+09', '2026-12-21 00:00:00+09', 'CONFIRMED', 1,
        '2026-08-04 15:00:00+09'),
       (3, 'EXPO_DIRECT', 7, 3, '2026-09-30 00:00:00+09', '2026-10-05 00:00:00+09', 'CONFIRMED', 1,
        '2026-06-12 10:00:00+09'),
       (4, 'EXPO_DIRECT', 8, 2, '2026-08-19 00:00:00+09', '2026-08-24 00:00:00+09', 'CONFIRMED', 1,
        '2026-05-25 10:00:00+09'),
       (5, 'EXPO_DIRECT', 9, 2, '2026-05-31 00:00:00+09', '2026-06-04 00:00:00+09', 'CONFIRMED', 1,
        '2026-04-05 10:00:00+09'),
       (6, 'EXPO_DIRECT', 10, 4, '2026-09-24 00:00:00+09', '2026-09-28 00:00:00+09', 'CONFIRMED', 1,
        '2026-05-03 10:00:00+09');

INSERT INTO expo_venue_assignments (id, expo_id, venue_reservation_id, assigned_by_admin_id)
VALUES (1, 1, 1, 1),
       (2, 2, 2, 1),
       (3, 3, 3, 1),
       (4, 4, 4, 1),
       (5, 5, 5, 1),
       (6, 6, 6, 1);

-- ---------- 카테고리 연결 (expo_categories N:M — 희-SRCH-02) ----------
INSERT INTO expo_categories (expo_id, category_id)
VALUES (1, 1),
       (2, 3),
       (3, 2),
       (4, 4),
       (5, 4),
       (6, 5),
       (1, 4);

-- ---------- 썸네일 (expo_images THUMBNAIL — 희-EXPO-15) ----------
INSERT INTO expo_images (id, expo_id, file_id, image_type, alt_text, sort_order)
VALUES (1, 1, 1, 'THUMBNAIL', 'AI 로보틱스 엑스포 대표 이미지', 0),
       (2, 2, 2, 'THUMBNAIL', 'K-뷰티 월드 대표 이미지', 0),
       (3, 3, 3, 'THUMBNAIL', '광주 미식 위크 대표 이미지', 0),
       (4, 4, 4, 'THUMBNAIL', '커리어 페어 시즌2 대표 이미지', 0),
       (5, 5, 5, 'THUMBNAIL', '상반기 커리어 페어 대표 이미지', 0),
       (6, 6, 6, 'THUMBNAIL', '부산 일러스트 페스티벌 대표 이미지', 0);

-- ---------- 소개 자료 (expo_files — 희-EXPO-14/15) ----------
INSERT INTO expo_files (id, expo_id, file_id, file_purpose, title, sort_order)
VALUES (1, 1, 7, 'CATALOG', '공식 카탈로그 2026', 0),
       (2, 1, 8, 'PROMO_VIDEO', '홍보영상(60초)', 1),
       (3, 1, 9, 'LEAFLET', '리플렛(국문)', 2);

-- ---------- 외부 링크 (external_links — 희-EXPO-13) ----------
INSERT INTO external_links (id, expo_id, link_type, label, url, sort_order)
VALUES (1, 1, 'HOMEPAGE', '공식 홈페이지', 'https://ai-robotics-expo.example.com', 0),
       (2, 1, 'RESERVATION', '사전등록', 'https://ai-robotics-expo.example.com/pre', 1),
       (3, 6, 'HOMEPAGE', '공식 홈페이지', 'https://busan-illust.example.com', 0);

-- ---------- 티켓 상품 + 재고 (희-SRCH-05 가격 / 매진·인기 판정) ----------
INSERT INTO ticket_products (id, expo_id, name, price, sales_start_at, sales_end_at, status)
VALUES
  -- expo 1 (판매중): 최저가 15,000
  (1, 1, '얼리버드', 15000, '2026-07-01 10:00:00+09', '2026-07-31 18:00:00+09', 'SOLD_OUT'),
  (2, 1, '일반권', 20000, '2026-07-01 10:00:00+09', '2026-09-09 18:00:00+09', 'ON_SALE'),
  (3, 1, 'VIP', 50000, '2026-07-01 10:00:00+09', '2026-09-09 18:00:00+09', 'ON_SALE'),
  -- expo 2 (판매예정): 상품은 등록, 판매 전
  (4, 2, '일반권', 18000, '2026-09-01 10:00:00+09', '2026-12-17 18:00:00+09', 'DRAFT'),
  -- expo 3 (매진)
  (5, 3, '일반권', 12000, '2026-07-15 10:00:00+09', '2026-09-30 18:00:00+09', 'SOLD_OUT'),
  (6, 3, 'VIP 테이스팅', 45000, '2026-07-15 10:00:00+09', '2026-09-30 18:00:00+09', 'SOLD_OUT'),
  -- expo 4 (판매종료)
  (7, 4, '일반권', 10000, '2026-06-01 10:00:00+09', '2026-08-01 18:00:00+09', 'SALE_ENDED'),
  -- expo 5 (행사종료)
  (8, 5, '무료입장권', 0, '2026-04-10 10:00:00+09', '2026-05-31 18:00:00+09', 'SALE_ENDED'),
  -- expo 6 (인기 1위): 누적 판매 최다
  (9, 6, '얼리버드', 18000, '2026-05-10 10:00:00+09', '2026-06-30 18:00:00+09', 'SOLD_OUT'),
  (10, 6, '일반권', 25000, '2026-05-10 10:00:00+09', '2026-09-24 18:00:00+09', 'ON_SALE');

INSERT INTO ticket_inventories (id, ticket_product_id, total_quantity, reserved_quantity, sold_quantity)
VALUES (1, 1, 500, 0, 500),   -- expo1 얼리버드 매진
       (2, 2, 2000, 12, 850),
       (3, 3, 100, 1, 42),
       (4, 4, 1500, 0, 0),    -- expo2 판매 전
       (5, 5, 800, 0, 800),   -- expo3 전량 매진
       (6, 6, 60, 0, 60),
       (7, 7, 1000, 0, 320),  -- expo4 판매종료
       (8, 8, 5000, 0, 3200), -- expo5 행사종료
       (9, 9, 3000, 0, 3000), -- expo6 얼리버드 매진
       (10, 10, 7000, 0, 5400);
-- expo6 일반권 판매중 (누적 8,400 = 인기 1위)

-- ---------- 시퀀스 보정 ----------
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('virtual_venues_id_seq', (SELECT MAX(id) FROM virtual_venues));
SELECT setval('file_metadata_id_seq', (SELECT MAX(id) FROM file_metadata));
SELECT setval('expo_opening_requests_id_seq', (SELECT MAX(id) FROM expo_opening_requests));
SELECT setval('expos_id_seq', (SELECT MAX(id) FROM expos));
SELECT setval('venue_reservations_id_seq', (SELECT MAX(id) FROM venue_reservations));
SELECT setval('expo_venue_assignments_id_seq', (SELECT MAX(id) FROM expo_venue_assignments));
SELECT setval('expo_images_id_seq', (SELECT MAX(id) FROM expo_images));
SELECT setval('expo_files_id_seq', (SELECT MAX(id) FROM expo_files));
SELECT setval('external_links_id_seq', (SELECT MAX(id) FROM external_links));
SELECT setval('ticket_products_id_seq', (SELECT MAX(id) FROM ticket_products));
SELECT setval('ticket_inventories_id_seq', (SELECT MAX(id) FROM ticket_inventories));
