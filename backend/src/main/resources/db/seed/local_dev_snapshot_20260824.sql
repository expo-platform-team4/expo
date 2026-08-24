-- 2026-08-24 기준 로컬 개발 DB 전체 스냅샷 (수동 테스트로 쌓인 시드 데이터).
--
-- ⚠ 이 파일은 Flyway 가 실행하지 않는다. db/migration 이 아니라 db/seed 에 있는 이유가 그것이다.
--   운영 DB 에 들어가면 안 된다. 반드시 손으로, 한 번만 실행한다 (재실행해도 ON CONFLICT DO
--   NOTHING 으로 안전하지만 애초에 스냅샷이라 재실행할 이유가 없다).
--
--   docker compose up -d 로 새 DB 를 띄우고 백엔드를 한 번 기동해 마이그레이션까지 적용한 뒤:
--
--   docker exec -i expo-postgres psql -U expo -d expo \
--     < backend/src/main/resources/db/seed/local_dev_snapshot_20260824.sql
--
-- 기존 local_ticket_flow.sql / local_full_flow_expo7.sql 을 따로 실행할 필요 없다 — 이 스냅샷은
-- 그 두 스크립트를 실행한 뒤 화면을 눌러보며 쌓인 결과물을 그대로 담고 있다.
--
-- ── pg_dump --data-only 로 뽑은 뒤 손으로 정리한 것이다 ──────────────────────────
-- 아래 항목은 의도적으로 뺐다.
--   * flyway_schema_history            — Flyway 내부 테이블.
--   * virtual_venues / venue_halls / venue_zones
--                                       — 이미 V202608121600__seed_kintex_venue_structure.sql
--                                         마이그레이션이 모든 환경에 넣는다. 여기서 또 넣으면 중복.
--   * refresh_tokens                   — JWT 세션 토큰. JWT_SECRET 은 팀원마다 값이 달라도 되므로
--                                         (env.sample 참고) 남의 토큰을 넣어봐야 검증되지 않고,
--                                         애초에 재현할 의미가 없는 세션 부산물이다.
--   * phone_verifications              — 휴대폰 인증코드. 만료되는 1회용 값이라 의미가 없다.
--   * password_reset_tokens / social_accounts
--                                       — 로컬에 데이터가 없었다(0행).
--
-- ── 알아두어야 할 한계 ────────────────────────────────────────────────────────
-- 1. 업로드 파일 실물은 안 들어있다. file_metadata / expo_images / expo_files 의 storage_key 는
--    내 로컬 S3Mock(볼륨 expo-s3mock-data) 에만 있는 객체를 가리킨다. 이 스냅샷만 실행하면
--    행은 생기지만 이미지·PDF 는 깨져 보인다. 실제 파일이 필요하면 화면에서 다시 업로드할 것.
-- 2. QR 관련 해시값이 내 로컬 QR_TOKEN_SECRET 기준으로 계산돼 있다. issued_tickets.qr_token_hash,
--    ticket_access_tokens.token_hash 는 내 backend/.env 의 QR_TOKEN_SECRET 으로 만든 값이라,
--    본인 QR_TOKEN_SECRET 이 다르면 스캔 화면·/tickets?token= 화면이 이 데이터로는 검증되지
--    않는다. 실제로 QR 을 눌러보려면 local_full_flow_expo7.sql 처럼 :qr_secret 변수로 그
--    자리에서 다시 계산하는 방식이 필요하다 — 이 스냅샷은 화면에 "행이 떠 있는지"만 보장한다.
-- 3. users.password_hash 는 실제 bcrypt 해시다. 원문 비밀번호는 만든 사람만 안다. e2e-client
--    등 e2e-* 계정은 기존 관례상(local_full_flow_expo7.sql 참고) 'Test1234!' 로 만들어졌을
--    가능성이 높지만 이 파일이 보장하지는 않는다.

BEGIN;

--
-- Data for Name: booths; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.booths VALUES (3, 1, NULL, 'A-101', 'RECTANGLE', 3.00, NULL, 3.00, 'M', NULL, NULL, 0.00, 0, 'ACTIVE', '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.users VALUES (1, 'seed-client@example.com', 'x', 'seed-client', 'CLIENT', 'ACTIVE', '01011112222', NULL, NULL, NULL, NULL, NULL, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (2, 'seed-member@example.com', 'x', 'seed-member', 'MEMBER', 'ACTIVE', '01033334444', NULL, NULL, NULL, NULL, NULL, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (3, 'admin-verify@espotic.com', '$2a$10$dLmpAZxIlBwPLa690vElWuVcaWdIPmicPRV6BJVgeeEYqBUnb.ZIG', '검증관리자', 'ADMIN', 'ACTIVE', '01045770341', NULL, '2026-08-18 09:09:57.985562+00', NULL, NULL, NULL, '2026-08-18 07:04:02.450548+00', '2026-08-18 09:09:58.475979+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (7, 'e2e-exhibitor@example.com', '$2a$10$R022ptR1JcLaSr7mQl0QUey0hLTBOvJqgp2UsFKgzElQCmyyMX6Ci', 'e2e_exhibitor', 'CLIENT', 'ACTIVE', '01077776666', NULL, '2026-08-20 05:47:09.921554+00', NULL, NULL, NULL, '2026-08-20 00:23:36.57628+00', '2026-08-20 05:47:10.048419+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (6, 'e2e-admin@example.com', '$2a$10$2BMJbP0cpFKskmmL5UiXe.qIUWL9w9rTpRhsP2/rtr8FvQJnGi90i', 'e2e_admin', 'ADMIN', 'ACTIVE', NULL, NULL, '2026-08-21 07:32:23.002001+00', NULL, NULL, NULL, '2026-08-20 00:20:20.881467+00', '2026-08-21 07:32:23.117855+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (4, 'e2e-client@example.com', '$2a$10$XZzG/HQ0sN5w9EwzVWky/OzcyBLFsmWhaA.GcdC9G2F3LcgX1fwUS', 'e2e_client', 'CLIENT', 'ACTIVE', '01099998888', NULL, '2026-08-21 07:59:49.828885+00', NULL, NULL, NULL, '2026-08-19 17:40:51.149934+00', '2026-08-21 07:59:50.041898+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (8, 'e2e-example@example.com', '$2a$10$1bYK5GQtSREi2XuClTaXPeW/zzLGEPmm1nNBFTLaCUHzeU29tQjgu', 'Test-client-1', 'MEMBER', 'ACTIVE', '01000000000', NULL, '2026-08-24 05:34:26.172955+00', NULL, NULL, NULL, '2026-08-24 05:16:50.589009+00', '2026-08-24 05:34:26.571633+00') ON CONFLICT DO NOTHING;
INSERT INTO public.users VALUES (5, 'e2e-member@example.com', '$2a$10$2BMJbP0cpFKskmmL5UiXe.qIUWL9w9rTpRhsP2/rtr8FvQJnGi90i', 'e2e_member', 'MEMBER', 'ACTIVE', '01055554444', NULL, '2026-08-24 06:00:11.649939+00', NULL, NULL, NULL, '2026-08-19 17:42:12.302874+00', '2026-08-24 06:00:11.85753+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: file_metadata; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.file_metadata VALUES (1, 4, 'S3', 'expo-local', 'expo-image/2026/08/20/472ecc9a-e489-4830-a301-22fcb488db99.png', '포스터.png', 'image/png', 70, 'c414cd0e204de974f73753c7e28d7638e7b3691bb8b1a2bab6b25bb7fed7ce77', 'ACTIVE', '2026-08-20 05:46:42.976348+00', '2026-08-20 05:46:42.976348+00', 'PUBLIC') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (2, 4, 'S3', 'expo-local', 'settlement-report/2026/08/20/99d27a5d-217f-4ac9-a855-35a38835f429.pdf', '정산리포트.pdf', 'application/pdf', 20, 'a8007bb814f3f5d7c4a0b0c4692f95bb34c5a8ba13561ea7462f9139467fc4d7', 'ACTIVE', '2026-08-20 05:47:10.439793+00', '2026-08-20 05:47:10.439793+00', 'PRIVATE') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (3, 4, 'S3', 'expo-local', 'expo-image/2026/08/20/59aaffb5-0d35-433d-8395-63f737c795bf.png', 'big.png', 'image/png', 6291464, 'eeba79a25e4c1ea51cb486242c13ef70e42419426a470650f432284316ccb221', 'ACTIVE', '2026-08-20 05:47:25.52226+00', '2026-08-20 05:47:25.52226+00', 'PUBLIC') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (4, 4, 'S3', 'expo-local', 'expo-document/2026/08/20/533656d0-7f47-46a2-b533-52bed32531fd.pdf', '행사 팜플렛.pdf', 'application/pdf', 17, '3249f1fc13a3da41190312e122cfa307ecec9c85d3acaa6de17b9d004e172643', 'ACTIVE', '2026-08-20 06:01:34.914632+00', '2026-08-20 06:01:34.914632+00', 'PUBLIC') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (5, 4, 'S3', 'expo-local', 'expo-image/2026/08/20/c356732e-cfda-4c64-9d46-e633a816259d.png', '포스터.png', 'image/png', 70, 'c414cd0e204de974f73753c7e28d7638e7b3691bb8b1a2bab6b25bb7fed7ce77', 'ACTIVE', '2026-08-20 06:01:35.19698+00', '2026-08-20 06:01:35.19698+00', 'PUBLIC') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (6, 4, 'S3', 'expo-local', 'expo-image/2026/08/20/8fe2fab7-9d41-479b-bdb1-224afc3c25b1.png', 'expo-poster.png', 'image/png', 102198, '603074e8e66a7a4b58fef667e830da9b4fe1d60f063c399e3755926054cd151e', 'ACTIVE', '2026-08-20 06:14:58.506763+00', '2026-08-20 06:14:58.506763+00', 'PUBLIC') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (7, 4, 'S3', 'expo-local', 'expo-image/2026/08/20/a52a95b2-c2e9-4c93-a386-1eccdc5b883d.png', 'expo-poster.png', 'image/png', 102198, '603074e8e66a7a4b58fef667e830da9b4fe1d60f063c399e3755926054cd151e', 'ACTIVE', '2026-08-20 06:14:59.220862+00', '2026-08-20 06:14:59.220862+00', 'PUBLIC') ON CONFLICT DO NOTHING;
INSERT INTO public.file_metadata VALUES (8, 4, 'S3', 'expo-local', 'expo-image/2026/08/20/f123abf8-4aad-40c9-97a9-ab7b518166b8.png', '갤러리 사진.png', 'image/png', 102198, '603074e8e66a7a4b58fef667e830da9b4fe1d60f063c399e3755926054cd151e', 'ACTIVE', '2026-08-20 06:19:38.695923+00', '2026-08-20 06:19:38.695923+00', 'PUBLIC') ON CONFLICT DO NOTHING;

--
-- Data for Name: client_profiles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.client_profiles VALUES (1, '0001112222', '시드 주식회사', '홍길동', '서울시 어딘가 1-2-3', NULL, false, NULL, NULL, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.client_profiles VALUES (4, '1234567890', '이엔이 테스트', '미입력', '미입력', NULL, false, NULL, NULL, '2026-08-19 17:40:51.250909+00', '2026-08-19 17:40:51.250909+00') ON CONFLICT DO NOTHING;
INSERT INTO public.client_profiles VALUES (7, '1111111111', '전시참가 테스트', '미입력', '미입력', NULL, false, NULL, NULL, '2026-08-20 00:23:36.59209+00', '2026-08-20 00:23:36.59209+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: recruitment_notice_requests; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.recruitment_notice_requests VALUES (1, 4, '이엔이 테스트 박람회 부스 모집', '오버나이트 세션 테스트용 모집공고 요청입니다.', '2026-08-21 00:00:00+00', '2026-09-05 00:00:00+00', '2026-09-19 00:00:00+00', '2026-09-21 00:00:00+00', 1, 1, 5, NULL, 'APPROVED', 'RESOLVED', 'ALLOWED', NULL, NULL, 6, '2026-08-20 00:21:46.667512+00', '오버나이트 테스트 승인', '2026-08-20 00:21:13.824942+00', '2026-08-20 00:21:46.690057+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: recruitment_notices; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.recruitment_notices VALUES (1, 1, 4, '이엔이 테스트 박람회 부스 모집', '이엔이 테스트 박람회에서 함께할 참가 기업을 모집합니다. 오버나이트 세션 시드 데이터입니다.', '사업자등록번호 보유 기업', NULL, '2026-08-21 00:00:00+00', '2026-09-05 00:00:00+00', 'CLOSED', '2026-08-20 00:23:28.17262+00', '2026-08-20 04:30:40.980953+00', 6, '2026-08-20 00:23:21.239898+00', '2026-08-20 04:30:41.24258+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: booth_products; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.booth_products VALUES (3, 1, 3, 1000000.00, 100000.00, 1100000.00, true, NULL, NULL, NULL, true, 'SOLD', 0, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: participation_applications; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.participation_applications VALUES (1, 1, 7, '전시참가 테스트', '신제품 홍보 부스로 참가하고 싶습니다.', NULL, 3, NULL, 'SUBMITTED', NULL, NULL, NULL, NULL, '2026-08-20 00:25:16.319472+00', '2026-08-20 00:25:16.319472+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: expo_opening_requests; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.expo_opening_requests VALUES (1, 4, NULL, '2026 스마트 제조 박람회', '제조 자동화 솔루션을 한자리에 모은 박람회입니다.', '2026-11-10 00:00:00+00', '2026-11-12 00:00:00+00', '2026-09-01 00:00:00+00', '2026-11-09 00:00:00+00', 1, 2, 6, 'APPROVED', '2026-08-20 03:14:19.167672+00', 6, '2026-08-20 03:14:31.011738+00', NULL, '2026-08-20 03:14:19.168204+00', '2026-08-20 03:14:31.035717+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expo_opening_requests VALUES (2, 4, NULL, '2027 서울 푸드테크 박람회', '식품 제조·유통 기술을 소개하는 박람회입니다.', '2027-03-10 01:00:00+00', '2027-03-12 09:00:00+00', '2027-01-05 00:00:00+00', '2027-03-09 14:59:00+00', 1, NULL, NULL, 'APPROVED', '2026-08-20 03:21:02.244028+00', 6, '2026-08-20 03:21:57.695954+00', NULL, '2026-08-20 03:21:02.24954+00', '2026-08-20 03:21:57.74986+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expo_opening_requests VALUES (3, 4, NULL, '임시저장 검증', 'd', '2027-05-01 00:00:00+00', '2027-05-03 00:00:00+00', '2027-04-01 00:00:00+00', '2027-04-30 00:00:00+00', 1, NULL, NULL, 'APPROVED', '2026-08-20 03:29:13.388148+00', 6, '2026-08-20 04:18:44.056606+00', NULL, '2026-08-20 03:29:13.187802+00', '2026-08-20 04:18:44.29538+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: expos; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.expos VALUES (1, 1, NULL, '시드 박람회', '발권 검증용 시드 데이터', 'SEOUL', '2026-09-17 07:03:48.597972+00', '2026-09-19 07:03:48.597972+00', '2026-08-17 07:03:48.597972+00', '2026-09-16 07:03:48.597972+00', 'APPROVED', 'PUBLIC', 'SCHEDULED', NULL, NULL, NULL, NULL, 0, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expos VALUES (7, 4, NULL, '이엔이 테스트 박람회', '오버나이트 세션에서 시드로 넣은 테스트용 박람회입니다.', 'SEOUL', '2026-08-19 01:43:11.839892+00', '2026-08-22 01:43:11.839892+00', '2026-08-10 01:43:11.839892+00', '2026-08-21 01:43:11.839892+00', 'APPROVED', 'PUBLIC', 'ONGOING', NULL, NULL, NULL, NULL, 0, '2026-08-20 00:17:54.275406+00', '2026-08-20 00:17:54.275406+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expos VALUES (8, 4, 1, '2026 스마트 제조 박람회', '제조 자동화 솔루션을 한자리에 모은 박람회입니다.', 'GYEONGGI', '2026-11-10 00:00:00+00', '2026-11-12 00:00:00+00', '2026-09-01 00:00:00+00', '2026-11-09 00:00:00+00', 'APPROVED', 'PUBLIC', 'SCHEDULED', 6, '2026-08-20 03:14:31.011738+00', NULL, NULL, 0, '2026-08-20 03:14:31.016806+00', '2026-08-20 03:14:31.016806+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expos VALUES (9, 4, 2, '2027 서울 푸드테크 박람회', '식품 제조·유통 기술을 소개하는 박람회입니다.', 'GYEONGGI', '2027-03-10 01:00:00+00', '2027-03-12 09:00:00+00', '2027-01-05 00:00:00+00', '2027-03-09 14:59:00+00', 'APPROVED', 'PUBLIC', 'SCHEDULED', 6, '2026-08-20 03:21:57.695954+00', NULL, NULL, 0, '2026-08-20 03:21:57.664175+00', '2026-08-20 03:21:57.664175+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expos VALUES (10, 4, 3, '임시저장 검증', 'd', 'GYEONGGI', '2027-05-01 00:00:00+00', '2027-05-03 00:00:00+00', '2027-04-01 00:00:00+00', '2027-04-30 00:00:00+00', 'APPROVED', 'PUBLIC', 'SCHEDULED', 6, '2026-08-20 04:18:44.056606+00', NULL, NULL, 0, '2026-08-20 04:18:43.996305+00', '2026-08-20 04:18:43.996305+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: booth_orders; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.booth_orders VALUES (3, 1, 7, 3, 'BOOTH-SEED-EXPO7-0001', 1100000.00, 1100000.00, 'PAYMENT_COMPLETED', '2026-08-27 01:42:58.802964+00', '2026-08-19 01:42:58.802964+00', 'seed-booth-expo7-0001', '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

-- participation_applications(1) <-> booth_orders(3) 는 서로를 참조하는 순환 FK 다.
-- 위 participation_applications INSERT 에서 booth_order_id 를 NULL 로 비워뒀다가
-- booth_orders 가 생긴 지금 채워 넣는다.
UPDATE public.participation_applications SET booth_order_id = 3 WHERE id = 1 AND booth_order_id IS NULL;

--
-- Data for Name: booth_allocations; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.booth_allocations VALUES (1, 1, 3, 3, 7, '2026-08-19 01:42:58.802964+00', 'ASSIGNED', NULL, NULL, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: booth_contents; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.booth_contents VALUES (1, 1, 7, '전시참가 테스트', '스마트 제조 솔루션 전시', '공장 자동화 설비를 만드는 회사입니다.', '', '', NULL, NULL, 'UNDER_REVIEW', NULL, NULL, NULL, NULL, NULL, '2026-08-20 01:58:14.619747+00', '2026-08-20 02:54:30.76307+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: booth_payments; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.booth_payments VALUES (2, 3, NULL, 'PG-SEED-EXPO7-0001', 'CARD', 'APPROVED', 1100000.00, 1100000.00, '2026-08-19 01:42:58.802964+00', 0.00, NULL, 'seed-booth-pay-expo7-0001', '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.categories VALUES (1, NULL, 'IT/전자', 'it/전자', 1, true, '2026-08-20 00:27:10.590949+00', '2026-08-20 00:27:10.590949+00') ON CONFLICT DO NOTHING;
INSERT INTO public.categories VALUES (2, NULL, '와인', '와인', 2, true, '2026-08-20 02:53:28.927536+00', '2026-08-20 02:53:28.927536+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: ticket_orders; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.ticket_orders VALUES (1, 'ORD-SEED-20260818070348597', 2, 'MEMBER', 30000.00, 0.03000, 900.00, 30900.00, 2, 'PAID', '2026-08-18 07:03:48.597972+00', NULL, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (2, 'TICKET-d4a537dd-dc27-44a9-a4cf-a6230c5aa586', 5, 'MEMBER', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-19 17:46:58.068059+00', '2026-08-19 17:46:58.068059+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (3, 'TICKET-ee63fed5-a48c-4573-b0ec-4730b2ad7578', NULL, 'GUEST', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-19 17:47:48.727755+00', '2026-08-19 17:47:48.727755+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (4, 'TICKET-928e41c9-4531-40be-926c-07d7909fe94c', NULL, 'GUEST', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-19 17:53:23.978971+00', '2026-08-19 17:53:23.978971+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (5, 'TICKET-530d6faa-7b3f-42a2-9ef0-27ccc00e2153', NULL, 'GUEST', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-19 17:55:37.773224+00', '2026-08-19 17:55:37.773224+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (6, 'TICKET-62d9faa3-6a18-4909-b782-95f414a40b6b', NULL, 'GUEST', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-19 17:57:14.474522+00', '2026-08-19 17:57:14.474522+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (7, 'TICKET-8e255397-117f-4d41-9f50-748b18f466fb', NULL, 'GUEST', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-19 17:58:39.463741+00', '2026-08-19 17:58:39.463741+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (10, 'TICKET-SEED-EXPO7-0001', 5, 'MEMBER', 54000.00, 0.03000, 1620.00, 55620.00, 3, 'PAID', '2026-08-18 01:42:58.802964+00', NULL, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_orders VALUES (11, 'TICKET-1c02b862-8e94-43b8-ad22-31b96fae8ce3', 8, 'MEMBER', 15000.00, 0.03000, 450.00, 15450.00, 1, 'PENDING', NULL, NULL, '2026-08-24 05:18:58.520162+00', '2026-08-24 05:18:58.520162+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: ticket_products; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.ticket_products VALUES (1, 1, '시드 1일권', NULL, 15000.00, '2026-08-17 07:03:48.597972+00', '2026-09-16 07:03:48.597972+00', 4, 'ON_SALE', 0, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_products VALUES (4, 7, '얼리버드 1일권', '오전 입장 가능한 1일 관람권입니다.', 18000.00, '2026-08-10 01:42:58.802964+00', '2026-08-21 01:42:58.802964+00', 4, 'ON_SALE', 0, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: ticket_order_items; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.ticket_order_items VALUES (1, 1, 1, 2, 15000.00, 30000.00, '2026-08-18 07:03:48.597972+00', '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (2, 2, 1, 1, 15000.00, 15000.00, '2026-08-19 17:46:58.130261+00', '2026-08-19 17:46:58.130261+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (3, 3, 1, 1, 15000.00, 15000.00, '2026-08-19 17:47:48.738586+00', '2026-08-19 17:47:48.738586+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (4, 4, 1, 1, 15000.00, 15000.00, '2026-08-19 17:53:24.024164+00', '2026-08-19 17:53:24.024164+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (5, 5, 1, 1, 15000.00, 15000.00, '2026-08-19 17:55:37.81861+00', '2026-08-19 17:55:37.81861+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (6, 6, 1, 1, 15000.00, 15000.00, '2026-08-19 17:57:14.516102+00', '2026-08-19 17:57:14.516102+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (7, 7, 1, 1, 15000.00, 15000.00, '2026-08-19 17:58:39.511856+00', '2026-08-19 17:58:39.511856+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (10, 10, 4, 3, 18000.00, 54000.00, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_order_items VALUES (11, 11, 1, 1, 15000.00, 15000.00, '2026-08-24 05:18:58.678676+00', '2026-08-24 05:18:58.678676+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: issued_tickets; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.issued_tickets VALUES (8, 10, 7, 'EXPO-SEED7-000001', '8aaa17c515318cd9c1e230310ba678a4bac4556bbe2c48e978aefb5530d399ee', 'CHECKED_IN', '2026-08-18 01:42:58.802964+00', '2026-08-19 22:42:58.802964+00', NULL, 0, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;
INSERT INTO public.issued_tickets VALUES (7, 10, 7, 'EXPO-SEED7-000002', 'c65a29605c9bd8bac2fab50cce9069819972c6119e886f907cfbfac1718d61de', 'CHECKED_IN', '2026-08-18 01:42:58.802964+00', '2026-08-20 01:48:19.289014+00', NULL, 1, '2026-08-20 01:42:58.802964+00', '2026-08-20 01:48:19.294116+00') ON CONFLICT DO NOTHING;
INSERT INTO public.issued_tickets VALUES (9, 10, 7, 'EXPO-SEED7-000003', 'a3eaa0261530123bd0a53ea3695a1631c870db9acecd9bd767c359d534c24967', 'CHECKED_IN', '2026-08-18 01:42:58.802964+00', '2026-08-21 07:59:50.770784+00', NULL, 1, '2026-08-20 01:42:58.802964+00', '2026-08-21 07:59:50.774182+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: check_in_histories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.check_in_histories VALUES (5, 8, 7, 4, 'QR', 'SUCCESS', '2026-08-19 22:42:58.802964+00', NULL, '정상 입장') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (6, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-19 23:42:58.802964+00', NULL, '이미 입장한 티켓입니다.') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (7, 7, 7, 4, 'QR', 'SUCCESS', '2026-08-20 01:48:19.289014+00', '0:0:0:0:0:0:0:1', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (8, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:48.880609+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (9, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:48.93412+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (10, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:48.977646+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (11, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.023346+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (12, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.077154+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (13, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.116926+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (14, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.15273+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (15, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.19119+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (16, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.229505+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (17, 8, 7, 4, 'MANUAL_CODE', 'ALREADY_USED', '2026-08-21 07:50:49.267509+00', '0:0:0:0:0:0:0:1', '먼저 입장한 시각 = 2026-08-19T22:42:58.802964Z') ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (18, NULL, 7, 4, 'QR', 'INVALID_TOKEN', '2026-08-21 07:50:49.563407+00', '0:0:0:0:0:0:0:1', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (19, NULL, 7, 4, 'QR', 'INVALID_TOKEN', '2026-08-21 07:59:50.537412+00', '0:0:0:0:0:0:0:1', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.check_in_histories VALUES (20, 9, 7, 4, 'MANUAL_CODE', 'SUCCESS', '2026-08-21 07:59:50.770784+00', '0:0:0:0:0:0:0:1', NULL) ON CONFLICT DO NOTHING;

--
-- Data for Name: expo_files; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.expo_files VALUES (1, 7, 4, 'LEAFLET', NULL, 0, '2026-08-20 06:01:35.09443+00', '2026-08-20 06:01:35.09443+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: expo_images; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.expo_images VALUES (3, 7, 6, 'THUMBNAIL', '이엔이 테스트 박람회 포스터', 2, '2026-08-20 06:14:59.05659+00', '2026-08-20 06:14:59.05659+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expo_images VALUES (4, 8, 7, 'THUMBNAIL', NULL, 0, '2026-08-20 06:14:59.292617+00', '2026-08-20 06:14:59.292617+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expo_images VALUES (5, 7, 8, 'GALLERY', NULL, 3, '2026-08-20 06:19:38.905889+00', '2026-08-20 06:19:38.905889+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: expo_review_histories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.expo_review_histories VALUES (1, 8, 6, 'APPROVE', NULL, 'SUBMITTED', 'APPROVED', '2026-08-20 03:14:31.011738+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expo_review_histories VALUES (2, 9, 6, 'APPROVE', NULL, 'SUBMITTED', 'APPROVED', '2026-08-20 03:21:57.695954+00') ON CONFLICT DO NOTHING;
INSERT INTO public.expo_review_histories VALUES (3, 10, 6, 'APPROVE', NULL, 'SUBMITTED', 'APPROVED', '2026-08-20 04:18:44.056606+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: venue_reservations; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.venue_reservations VALUES (1, 'RECRUITMENT_NOTICE', 1, NULL, 1, 1, 1, 1, '2026-09-19 00:00:00+00', '2026-09-21 00:00:00+00', 'CONFIRMED', 6, '2026-08-20 00:23:11.665491+00', NULL, '2026-08-20 00:23:11.668262+00', '2026-08-20 00:23:21.253219+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: guest_order_infos; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.guest_order_infos VALUES (6, '이순신', '01077778888', 40, '$2a$10$y/Yfi4ZwCjutrPWDcnnyH.n6InkVYkWdCYz..UcWdk3SLLYkX4Oge', 0, NULL, '2026-08-19 17:57:14.61562+00', '2026-08-19 17:57:14.61562+00') ON CONFLICT DO NOTHING;
INSERT INTO public.guest_order_infos VALUES (7, '최종테스트', '01099990000', 33, '$2a$10$XjUb3TQTGk.IeDnZgr0FA.RtG5akPGnDDRVBuT1qQqkI1zCK/xD3O', 0, NULL, '2026-08-19 17:58:39.612122+00', '2026-08-19 17:58:39.612122+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: inventory_reservations; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.inventory_reservations VALUES (1, 1, 2, 1, 'EXPIRED', '2026-08-19 17:46:58.165241+00', '2026-08-19 17:56:58.142044+00', '2026-08-21 07:23:48.611357+00') ON CONFLICT DO NOTHING;
INSERT INTO public.inventory_reservations VALUES (2, 1, 3, 1, 'EXPIRED', '2026-08-19 17:47:48.877861+00', '2026-08-19 17:57:48.865429+00', '2026-08-21 07:23:48.618682+00') ON CONFLICT DO NOTHING;
INSERT INTO public.inventory_reservations VALUES (3, 1, 4, 1, 'EXPIRED', '2026-08-19 17:53:24.198689+00', '2026-08-19 18:03:24.125582+00', '2026-08-21 07:23:48.621636+00') ON CONFLICT DO NOTHING;
INSERT INTO public.inventory_reservations VALUES (4, 1, 5, 1, 'EXPIRED', '2026-08-19 17:55:37.988142+00', '2026-08-19 18:05:37.919286+00', '2026-08-21 07:23:48.623815+00') ON CONFLICT DO NOTHING;
INSERT INTO public.inventory_reservations VALUES (5, 1, 6, 1, 'EXPIRED', '2026-08-19 17:57:14.7067+00', '2026-08-19 18:07:14.644252+00', '2026-08-21 07:23:48.626738+00') ON CONFLICT DO NOTHING;
INSERT INTO public.inventory_reservations VALUES (6, 1, 7, 1, 'EXPIRED', '2026-08-19 17:58:39.71513+00', '2026-08-19 18:08:39.63717+00', '2026-08-21 07:23:48.62899+00') ON CONFLICT DO NOTHING;
INSERT INTO public.inventory_reservations VALUES (7, 1, 11, 1, 'EXPIRED', '2026-08-24 05:18:58.785673+00', '2026-08-24 05:28:58.711574+00', '2026-08-24 05:30:43.438731+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notifications VALUES (1, NULL, '01045770340', 'SMS', 'TICKET_ISSUED', 'ORDER', 1, '{"orderNumber": "ORD-SEED", "ticketCount": 2}', 'SENT', NULL, '2026-08-18 07:04:18.534981+00', 1, NULL, '2026-08-18 07:04:18.418926+00', '2026-08-18 07:04:18.53843+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: message_histories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.message_histories VALUES (1, 1, NULL, 'SENT', NULL, NULL, NULL, 1, '2026-08-18 07:04:18.534659+00', '2026-08-18 07:04:18.534981+00', 'SMS') ON CONFLICT DO NOTHING;

--
-- Data for Name: recruitment_notice_request_histories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.recruitment_notice_request_histories VALUES (1, 1, 'VENUE_ALLOW', 'PENDING', 'ALLOWED', '오버나이트 테스트 승인', 6, '2026-08-20 00:21:46.670457+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: recruitment_notice_request_zones; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.recruitment_notice_request_zones VALUES (1, 1, 1, '2026-08-20 00:21:13.874499+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: settlements; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.settlements VALUES (3, 7, 4, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, '2026-09-03 01:42:58.802964+00', '2026-08-20 02:52:59.715237+00', 6, 'CONFIRMED', '2026-08-20 01:42:58.802964+00', '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: ticket_access_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.ticket_access_tokens VALUES (2, 1, NULL, '16d2ebc84938c618890bc3133cb16f1e411c67623f5734d269d981a036e64c81', 'ORDER_VIEW', 'ACTIVE', '2026-09-17 07:04:18.418926+00', NULL, 0, NULL, '2026-08-18 07:04:18.523732+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_access_tokens VALUES (1, 1, NULL, '029c36b461d94443e7f69ee77a7c861401ea45de54f41f05112ea3e52898d5f9', 'ORDER_VIEW', 'REVOKED', '2026-09-17 07:04:18.418926+00', NULL, 0, '2026-08-18 07:04:18.522521+00', '2026-08-18 07:04:18.418926+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_access_tokens VALUES (5, 10, NULL, 'd0e98d8e8d38c522aaf2ebee6e75d4d1b56f50bbee71ab2c0fed009114c28fce', 'ORDER_VIEW', 'ACTIVE', '2026-09-19 01:42:58.802964+00', '2026-08-20 04:26:31.174853+00', 7, NULL, '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: ticket_inventories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.ticket_inventories VALUES (4, 4, 500, 0, 3, DEFAULT, 0, '2026-08-20 01:42:58.802964+00') ON CONFLICT DO NOTHING;
INSERT INTO public.ticket_inventories VALUES (1, 1, 1000, 0, 0, DEFAULT, 0, '2026-08-18 07:03:48.597972+00') ON CONFLICT DO NOTHING;

--
-- Data for Name: venue_reservation_histories; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.venue_reservation_histories VALUES (1, 1, 'CONFIRMED', NULL, NULL, NULL, 6, '2026-08-20 00:23:11.768015+00') ON CONFLICT DO NOTHING;

COMMIT;

\echo ''
\echo '=== 스냅샷 적용 결과 ==='
SELECT 'users' AS 테이블, count(*)::text AS 행수 FROM users
UNION ALL SELECT 'expos', count(*)::text FROM expos
UNION ALL SELECT 'ticket_orders', count(*)::text FROM ticket_orders
UNION ALL SELECT 'issued_tickets', count(*)::text FROM issued_tickets
UNION ALL SELECT 'booth_orders', count(*)::text FROM booth_orders
UNION ALL SELECT 'settlements', count(*)::text FROM settlements;
