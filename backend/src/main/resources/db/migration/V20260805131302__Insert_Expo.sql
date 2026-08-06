-- =====================================================================
-- V20260805131302__Insert_Expo.sql
-- EXPO 도메인 더미데이터 (기준일: 2026-08-05)
-- 각 기능(희-EXPO-*, 희-SRCH-*)을 검증할 수 있도록 상태별 데이터 구성
-- ※ 반려 사유·심사요청/처리/공개 일시는 심사 이력 도메인(타 담당)에서 관리
-- =====================================================================

-- ------------------------- 마스터 데이터 -------------------------
INSERT INTO client (client_id, company_name) VALUES
    (1, '(주)한빛엑스포'),
    (2, '글로벌페어 주식회사'),
    (3, '광주컨벤션기획');

INSERT INTO category (category_id, name) VALUES
    (1, 'IT·테크'),
    (2, '푸드·외식'),
    (3, '뷰티·패션'),
    (4, '취업·교육'),
    (5, '문화·예술');

INSERT INTO venue (venue_id, name) VALUES
    (1, '코엑스 A홀'),
    (2, '킨텍스 제1전시장'),
    (3, '김대중컨벤션센터'),
    (4, '벡스코 제2전시장');

SELECT setval('client_client_id_seq',     (SELECT MAX(client_id)   FROM client));
SELECT setval('category_category_id_seq', (SELECT MAX(category_id) FROM category));
SELECT setval('venue_venue_id_seq',       (SELECT MAX(venue_id)    FROM venue));

-- ------------------------- EXPO -------------------------
-- 1) DRAFT: 임시저장 — 필수값 일부 미입력 상태 허용 (희-EXPO-01, 비고 3)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (1, 1, 1, '코엑스 A홀', NULL, '2026 스마트테크 박람회(작성중)', NULL,
        NULL, NULL, NULL, '서울', 'DRAFT', '2026-08-01 10:00:00', '2026-08-03 15:20:00');

-- 2) SUBMITTED: 등록 및 심사 요청 완료 (희-EXPO-02, 희-EXPO-17) — 승인 전 직접 수정 가능 (희-EXPO-05)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (2, 2, 2, '킨텍스 제1전시장', NULL, '2026 글로벌 푸드 페스타', '전 세계 미식 트렌드를 한자리에서 만나는 푸드 박람회',
        '2026-11-05', '2026-11-08', 'https://cdn.example.com/expo/food-festa-2026.jpg', '경기', 'SUBMITTED',
        '2026-08-02 11:00:00', '2026-08-04 09:30:00');

-- 3) UNDER_REVIEW: 심사 중
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (3, 3, 4, '김대중컨벤션센터', NULL, '2026 호남권 취업박람회', '지역 우수기업 200개사가 참여하는 채용 박람회',
        '2026-10-15', '2026-10-16', 'https://cdn.example.com/expo/job-fair-honam.jpg', '광주', 'UNDER_REVIEW',
        '2026-08-01 09:00:00', '2026-08-04 10:00:00');

-- 4) REJECTED: 반려 — 반려 사유는 심사 이력 도메인에서 조회
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (4, 1, 5, '벡스코 제2전시장', NULL, '해운대 아트페어 2026', '신진 작가 중심의 현대미술 전시·판매전',
        '2026-12-01', '2026-12-05', 'https://cdn.example.com/expo/art-fair.jpg', '부산', 'REJECTED',
        '2026-07-25 09:00:00', '2026-07-30 16:00:00');

-- 5) PUBLISHED + 판매중: 승인 시 자동 공개·장소 확정 (희-EXPO-09, 비고 2) → 목록 자동 반영 (희-SRCH-12)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (5, 1, 1, '코엑스 A홀', 1, '2026 서울 AI & 로보틱스 엑스포', '국내 최대 규모 AI·로봇 산업 전시회',
        '2026-09-10', '2026-09-13', 'https://cdn.example.com/expo/ai-robotics.jpg', '서울', 'PUBLISHED',
        '2026-06-28 09:00:00', '2026-07-03 11:00:00');

-- 6) PUBLISHED + 판매예정: 승인되었으나 티켓 미등록 → `판매 예정` 배지 (희-SRCH-13, TICKET_TYPE 비고 4)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (6, 2, 3, '코엑스 A홀', 1, '2026 K-뷰티 월드', 'K-뷰티 브랜드 총출동, 신제품 체험·B2B 상담회',
        '2026-12-18', '2026-12-20', 'https://cdn.example.com/expo/k-beauty.jpg', '서울', 'PUBLISHED',
        '2026-07-29 09:00:00', '2026-08-04 15:00:00');

-- 7) PUBLISHED + 매진: 전 티켓 종류 sold=total (TICKET_TYPE 비고 2)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (7, 3, 2, '김대중컨벤션센터', 3, '광주 미식 위크 2026', '남도 미식 문화와 로컬 셰프의 만남',
        '2026-08-20', '2026-08-23', 'https://cdn.example.com/expo/gwangju-food.jpg', '광주', 'PUBLISHED',
        '2026-06-05 09:00:00', '2026-06-12 10:00:00');

-- 8) PUBLISHED + 행사종료: end_date 경과 → 행사종료 배지 (희-EXPO-10)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (8, 2, 4, '킨텍스 제1전시장', 2, '2026 상반기 커리어 페어', '상반기 공채 대비 대규모 취업 박람회',
        '2026-06-01', '2026-06-03', 'https://cdn.example.com/expo/career-fair-h1.jpg', '경기', 'PUBLISHED',
        '2026-03-28 09:00:00', '2026-04-05 10:00:00');

-- 9) PUBLISHED + 인기(판매량 최다): 인기순 정렬 검증용 (희-SRCH-09)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (9, 1, 5, '벡스코 제2전시장', 4, '부산 국제 일러스트 페스티벌', '아시아 최대 일러스트·굿즈 페스티벌',
        '2026-09-25', '2026-09-27', 'https://cdn.example.com/expo/busan-illust.jpg', '부산', 'PUBLISHED',
        '2026-04-25 09:00:00', '2026-05-03 10:00:00');

-- 10) PUBLISHED + 마감임박: end_date가 가장 가까운 판매중 행사 (희-SRCH-08)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, venue_id, title, description,
                  start_date, end_date, thumbnail_url, region, status, created_at, updated_at)
VALUES (10, 3, 1, '김대중컨벤션센터', 3, '광주 SW 위크 2026', '지역 SW 기업·스타트업 쇼케이스',
        '2026-08-07', '2026-08-09', 'https://cdn.example.com/expo/gwangju-sw.jpg', '광주', 'PUBLISHED',
        '2026-07-05 09:00:00', '2026-07-12 10:00:00');

-- 11) 논리 삭제된 데이터: 목록·조회 제외 확인용 (비고 6)
INSERT INTO expo (expo_id, client_id, category_id, desired_venue, title, region, status, is_deleted,
                  created_at, updated_at)
VALUES (11, 2, 1, '코엑스 A홀', '삭제된 테스트 박람회', '서울', 'DRAFT', TRUE,
        '2026-07-01 09:00:00', '2026-07-02 09:00:00');

SELECT setval('expo_expo_id_seq', (SELECT MAX(expo_id) FROM expo));

-- ------------------------- TICKET_TYPE -------------------------
-- expo 5 (판매중): 가격 필터·"15,000원부터" 표기 검증 (희-SRCH-05, TICKET_TYPE 비고 1)
INSERT INTO ticket_type (ticket_type_id, expo_id, name, price, total_quantity, sold_quantity) VALUES
    (1, 5, '얼리버드', 15000, 500, 500),      -- 얼리버드만 매진
    (2, 5, '일반권',   20000, 2000, 850),
    (3, 5, 'VIP',      50000, 100, 42);

-- expo 7 (매진): 모든 종류 sold = total
INSERT INTO ticket_type (ticket_type_id, expo_id, name, price, total_quantity, sold_quantity) VALUES
    (4, 7, '일반권', 12000, 800, 800),
    (5, 7, 'VIP 테이스팅', 45000, 60, 60);

-- expo 8 (행사종료)
INSERT INTO ticket_type (ticket_type_id, expo_id, name, price, total_quantity, sold_quantity) VALUES
    (6, 8, '무료입장권', 0, 5000, 3200);

-- expo 9 (인기순 1위: 누적 판매량 최다 — 희-SRCH-09)
INSERT INTO ticket_type (ticket_type_id, expo_id, name, price, total_quantity, sold_quantity) VALUES
    (7, 9, '얼리버드', 18000, 3000, 3000),
    (8, 9, '일반권',   25000, 7000, 5400);

-- expo 10 (마감임박·판매중)
INSERT INTO ticket_type (ticket_type_id, expo_id, name, price, total_quantity, sold_quantity) VALUES
    (9, 10, '일반권', 10000, 1000, 320);

-- ※ expo 6은 의도적으로 티켓 미등록 → `판매 예정` (희-SRCH-13)

SELECT setval('ticket_type_ticket_type_id_seq', (SELECT MAX(ticket_type_id) FROM ticket_type));

-- ------------------------- EXPO_FILE -------------------------
-- 희-EXPO-13 외부 링크 / 희-EXPO-14 PDF·이미지·홍보영상 / 희-EXPO-15 카탈로그·리플렛
INSERT INTO expo_file (expo_file_id, expo_id, file_type, url, display_name) VALUES
    (1, 5, 'EXTERNAL_LINK', 'https://ai-robotics-expo.example.com',                '공식 홈페이지'),
    (2, 5, 'PDF',           'https://cdn.example.com/expo/5/floor-plan.pdf',      '전시장 배치도.pdf'),
    (3, 5, 'VIDEO',         'https://cdn.example.com/expo/5/promo.mp4',           '홍보영상(60s)'),
    (4, 5, 'CATALOG',       'https://cdn.example.com/expo/5/catalog-2026.pdf',    '공식 카탈로그'),
    (5, 5, 'LEAFLET',       'https://cdn.example.com/expo/5/leaflet.jpg',         '리플렛(국문)'),
    (6, 7, 'IMAGE',         'https://cdn.example.com/expo/7/poster.jpg',          '메인 포스터'),
    (7, 9, 'EXTERNAL_LINK', 'https://busan-illust.example.com/tickets',           '예매 안내 페이지'),
    (8, 9, 'VIDEO',         'https://cdn.example.com/expo/9/aftermovie-2025.mp4', '2025 애프터무비');

SELECT setval('expo_file_expo_file_id_seq', (SELECT MAX(expo_file_id) FROM expo_file));
