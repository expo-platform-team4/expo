# 통합 ERD 스키마 정의서 (v16)

두 개의 원본 PDF 를 합쳐 옮긴 것이다. 앞으로는 이 문서를 기준으로 한다.

| 출처 | 내용 |
|-|-|
| `Expo_Platform_ERD_v16_보고서반영_최종.pdf` (45p) | 본체. 통합 ERD 스키마 정의서 v16, 부제 "보고서 불일치 해소 및 장소 예약 통합 최종본", 작성 기준일 2026-07-31 |
| `erd수정본.pdf` (21p) | 보완본. v16 이 컬럼을 정의하지 않은 테이블 17개를 채운 것 |

| 항목 | 값 |
|-|-|
| DB | PostgreSQL |
| 시각 타입 | TIMESTAMPTZ |
| 금액 타입 | NUMERIC(15,2) |
| v16 이 명시한 규모 | 물리 테이블 68개, SQL View 12개 |
| 실제 확인된 규모 | 테이블 67개, View 16개 — [D-4](#d-4-검토가-필요한-지점) |
| 정의 완료 | 테이블 67개 전부, View 16개 전부 |
| 그중 추정이 섞인 것 | 테이블 5개, View 8개 — [부록 E](#부록-e-추정-컬럼-전수-목록) |

v16 PDF 는 표 렌더링이 군데군데 깨져 있다. 셀을 이탈해 인쇄된 행이 8곳, 한 행이 2~3줄로 쪼개진
곳이 문서 전반에 있다. 보완본에도 제목과 컬럼 표가 어긋난 구간이 있다. 이 문서는 그것을 복원한
것이고, 손댄 곳은 전부 [부록 D](#부록-d-보정-및-검토-이력) 에 남겼다.

> ⚠ **이 문서에는 명세가 아닌 추정이 섞여 있다.**
> 명세서 작성자에게 재요청했으나 답을 받을 수 없어, 끝까지 비어 있던 테이블 5개와 View 8개는
> 우리가 추정으로 채웠다. 추정 절에는 상단에 경고 블록이 있고, 컬럼 표에 **출처** 열이 있다.
>
> | 표기 | 의미 |
> |-|-|
> | `명세` | v16 또는 보완본에 그대로 있음 |
> | `보완` | 컬럼은 명세에 있으나 타입 또는 제약을 우리가 채움 |
> | `추정` | 컬럼 자체를 우리가 만듦 |
>
> 나중에 진짜 명세를 받으면 [부록 E](#부록-e-추정-컬럼-전수-목록) 의 목록만 대조하면 된다.

이 문서를 근거로 [V1__init_schema.sql](../backend/src/main/resources/db/migration/V1__init_schema.sql) 과
뷰 16개(`R__NN_v_*.sql`)를 작성했다.

---

## 목차

- [공통 설계 기준](#공통-설계-기준)
- [공통. 파일 메타데이터](#공통-파일-메타데이터)
- [6-1. 인증 및 사용자 관리](#6-1-인증-및-사용자-관리)
- [6-2. 일반 회원 마이페이지](#6-2-일반-회원-마이페이지)
- [6-3. 박람회 관리](#6-3-박람회-관리)
- [6-4. 검색 및 화면 노출](#6-4-검색-및-화면-노출)
- [6-5. 티켓 및 재고](#6-5-티켓-및-재고)
- [6-6. 주문 및 결제](#6-6-주문-및-결제)
- [6-7. 발권 및 QR 체크인](#6-7-발권-및-qr-체크인)
- [6-8. 카카오 알림](#6-8-카카오-알림)
- [6-9. 클라이언트 마이페이지](#6-9-클라이언트-마이페이지)
- [6-10. 정산 및 회계](#6-10-정산-및-회계)
- [6-11. 광고 배너](#6-11-광고-배너)
- [6-12. 관리자 대시보드 및 운영](#6-12-관리자-대시보드-및-운영)
- [6-13. 기업 모집공고 및 참여 신청](#6-13-기업-모집공고-및-참여-신청)
- [6-14. 가상 장소 및 부스 프리뷰](#6-14-가상-장소-및-부스-프리뷰)
- [6-15. 부스 판매·결제·배정·관리](#6-15-부스-판매결제배정관리)
- [부록 A. 상태 전이](#부록-a-상태-전이)
- [부록 B. 최종 필수 제약조건](#부록-b-최종-필수-제약조건)
- [부록 C. v16 변경 이력](#부록-c-v16-변경-이력)
- [부록 D. 보정 및 검토 이력](#부록-d-보정-및-검토-이력)
- [부록 E. 추정 컬럼 전수 목록](#부록-e-추정-컬럼-전수-목록)
- [부록 F. 코드 리뷰로 추가한 제약](#부록-f-코드-리뷰로-추가한-제약)

---

## 공통 설계 기준

| 항목 | 기준 |
|-|-|
| PK | 기본키는 `BIGSERIAL`. 연결 테이블은 필요한 경우 복합 `UNIQUE` 사용 |
| 시간 | `TIMESTAMPTZ` 사용. 시작일 < 종료일 검증 |
| 금액 | `NUMERIC(15,2)`. 서버 기준 가격으로 재계산 |
| 상태 | `VARCHAR` + `CHECK` 또는 Java Enum |
| 동시성 | 재고, 부스 상품, 발권 티켓에 `version` 과 필요한 잠금 적용 |
| 삭제 | 주문·결제·심사·정산 및 토큰 폐기 이력은 감사 정책에 따라 보존 |
| 보안 | 비밀번호, 비회원 조회 비밀번호, QR 토큰, Refresh Token 은 원문 대신 해시 저장 |

### 최종 확정 정책

- **티켓 예매 수수료** — 티켓 판매원금의 3% 를 구매자가 추가 결제
- **티켓 정산** — 판매원금 − 환불원금 ± 확정 조정 금액. 예매 수수료를 재차감하지 않음
- **부스 신청** — 신청서 작성, 부스 선택, 결제 성공 즉시 신청 완료 및 확정 배정
- **부스 수수료 및 환불** — 플랫폼 수수료 0원, 결제 완료 후 취소 및 환불 미지원
- **정산** — 행사 종료 후 7~14일 이내 대상 생성, 관리자 확정 후 외부 송금 결과 기록
- **로그아웃** — Refresh Token 해시 DB 저장, `revoked_at` 기반 현재 또는 전체 기기 폐기
- **프로필 이미지** — 일반 회원·클라이언트 공통으로 `USERS.profile_image_file_id` 를 통해 `FILE_METADATA` 와 연결
- **일반 박람회 장소 선택** — 클라이언트가 신청 시 장소·홀·구역을 직접 선택
- **박람회 취소** — 연결된 `VENUE_RESERVATIONS` 를 `RELEASED` 로 해제
- **관리자 권한** — MVP 의 `ADMIN` 은 단일 SUPER ADMIN

---

## 공통. 파일 메타데이터

특정 기능 절에 속하지 않고 문서 전반에서 참조되는 테이블이다. v16 본문은 이 테이블을 엔티티 목록에도
싣지 않은 채 7개 테이블이 FK 로 참조하고 있었다. 아래 정의는 보완본 `erd수정본.pdf` 에서 가져왔다.

### FILE_METADATA

업로드된 모든 파일의 저장소 위치와 상태를 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 파일 ID |
| `uploader_user_id` | BIGINT | FK → USERS.id, NULL | 업로드한 사용자 |
| `storage_provider` | VARCHAR(20) | NOT NULL | 저장소 제공자. 예: `S3` |
| `bucket_name` | VARCHAR(100) | NOT NULL | 버킷명 |
| `storage_key` | VARCHAR(500) | UNIQUE, NOT NULL | 객체 저장 키 |
| `original_filename` | VARCHAR(255) | NOT NULL | 원본 파일명 |
| `content_type` | VARCHAR(100) | NOT NULL | MIME 타입 |
| `file_size` | BIGINT | NOT NULL, CHECK >= 0 | 파일 크기(Byte) |
| `checksum` | VARCHAR(128) | NULL | 파일 무결성 확인값 |
| `file_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `DELETED`, `QUARANTINED` |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

이 테이블을 참조하는 곳 — **11개 테이블, FK 12개**. `USERS.profile_image_file_id`,
`VIRTUAL_VENUES.map_file_id`, `VENUE_HALLS.layout_file_id`, `VENUE_ZONES.layout_file_id`,
`EXPO_IMAGES.file_id`, `EXPO_FILES.file_id`, `BANNER_APPLICATIONS.image_file_id`,
`BANNERS.image_file_id`, `BOOTH_CONTENTS.logo_file_id` / `main_image_file_id`,
`BOOTH_CONTENT_FILES.file_id`, `SETTLEMENT_REPORTS.file_id`.

---

## 6-1. 인증 및 사용자 관리

일반 회원, 클라이언트, 관리자 계정과 이메일·소셜 로그인, 휴대폰 본인인증, 비밀번호 재설정, 탈퇴를 관리한다.

**엔티티** — `USERS`, `CLIENT_PROFILES`, `SOCIAL_ACCOUNTS`, `PHONE_VERIFICATIONS`, `PASSWORD_RESET_TOKENS`, `REFRESH_TOKENS`

### USERS

공통 계정 테이블. 일반 회원, 클라이언트, 관리자를 하나의 계정 모델로 관리하며, 일반 회원 마이페이지와
클라이언트 페이지에서 사용하는 프로필 이미지를 `FILE_METADATA` 와 연결한다. MVP 의 `ADMIN` 은
별도 일반 관리자 등급 없이 전체 운영 권한을 가진 단일 SUPER ADMIN 으로 사용한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 사용자 고유 ID |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 |
| `password_hash` | VARCHAR(255) | NULL | 소셜 로그인 전용 계정은 NULL 가능 |
| `nickname` | VARCHAR(50) | UNIQUE, NOT NULL | 서비스 닉네임 |
| `role` | VARCHAR(20) | NOT NULL, CHECK | `MEMBER`, `CLIENT`, `ADMIN` — ADMIN 은 단일 SUPER ADMIN |
| `account_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `WITHDRAWN` |
| `phone_number` | VARCHAR(20) | NULL | 본인인증 휴대폰 번호 |
| `phone_verified_at` | TIMESTAMPTZ | NULL | 휴대폰 본인인증 완료 일시 |
| `last_login_at` | TIMESTAMPTZ | NULL | 마지막 로그인 일시 |
| `withdrawn_at` | TIMESTAMPTZ | NULL | 탈퇴 일시 |
| `profile_image_file_id` | BIGINT | FK → FILE_METADATA.id, NULL | 일반 회원·클라이언트 공용 프로필 이미지 파일 |
| `profile_image_updated_at` | TIMESTAMPTZ | NULL | 프로필 이미지 마지막 변경 일시 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **관리자 권한 정책** — MVP 에서는 `ADMIN` 계정이 회원·클라이언트·박람회·배너·모집공고·장소·부스·결제·환불·정산을
> 모두 관리하는 SUPER ADMIN 이다. `ADMIN` 역할 세분화와 별도 권한 테이블은 2차 범위에서 검토한다.

### CLIENT_PROFILES

`role=CLIENT` 인 사용자의 사업자 상세 프로필이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `user_id` | BIGINT | PK, FK → USERS.id | 클라이언트 계정 ID |
| `business_number` | VARCHAR(20) | UNIQUE, NOT NULL | 사업자등록번호 |
| `company_name` | VARCHAR(150) | NOT NULL | 기업명/주최사명 |
| `representative_name` | VARCHAR(100) | NOT NULL | 대표자명 |
| `business_address` | VARCHAR(255) | NOT NULL | 사업장 주소 |
| `business_type` | VARCHAR(100) | NULL | 업태/종목 |
| `business_number_verified` | BOOLEAN | NOT NULL DEFAULT FALSE | 사업자번호 검증 여부 |
| `business_verified_at` | TIMESTAMPTZ | NULL | 검증 완료 일시 |
| `verification_provider` | VARCHAR(30) | NULL | 검증 제공자/API |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### SOCIAL_ACCOUNTS

구글·카카오 OAuth 계정을 `USERS` 와 연결한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 소셜 계정 ID |
| `user_id` | BIGINT | FK → USERS.id, NOT NULL | 연결 사용자 |
| `provider` | VARCHAR(20) | NOT NULL, CHECK | `GOOGLE`, `KAKAO` |
| `provider_user_id` | VARCHAR(255) | NOT NULL | OAuth 제공자 사용자 ID |
| `provider_email` | VARCHAR(255) | NULL | 제공받은 이메일 |
| `linked_at` | TIMESTAMPTZ | NOT NULL | 연결 일시 |
| `last_login_at` | TIMESTAMPTZ | NULL | 해당 소셜 계정 마지막 로그인 |
| `(provider, provider_user_id)` | — | UNIQUE | 동일 소셜 계정 중복 연결 방지 |

### PHONE_VERIFICATIONS

휴대폰 본인인증 요청과 결과를 기록한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 인증 이력 ID |
| `user_id` | BIGINT | FK → USERS.id, NULL | 가입 완료 전에는 NULL 가능 |
| `phone_number` | VARCHAR(20) | NOT NULL | 인증 대상 번호 |
| `verification_token_hash` | VARCHAR(255) | NOT NULL | 인증 거래/토큰 해시 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `REQUESTED`, `VERIFIED`, `FAILED`, `EXPIRED` |
| `requested_at` | TIMESTAMPTZ | NOT NULL | 요청 일시 |
| `verified_at` | TIMESTAMPTZ | NULL | 성공 일시 |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 인증 만료 일시 |

### PASSWORD_RESET_TOKENS

AUTH-10 비밀번호 재설정 링크의 일회용 토큰과 만료·사용 상태를 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 재설정 토큰 ID |
| `user_id` | BIGINT | FK → USERS.id, NOT NULL | 대상 사용자 |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL | 원문 대신 저장하는 토큰 해시 |
| `status` | VARCHAR(20) | CHECK | `ISSUED`, `USED`, `EXPIRED`, `REVOKED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `issued_at` | TIMESTAMPTZ | NOT NULL | 발급 일시 |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 만료 일시 |
| `used_at` | TIMESTAMPTZ | NULL | 사용 일시 |
| `requested_ip` | VARCHAR(45) | NULL | 요청 IP |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | 생성 일시 — 원본에 NOT NULL 표기 없음 (D-4) |

### REFRESH_TOKENS

사용자별 로그인 기기에서 발급된 Refresh Token 을 DB 로 관리한다. 토큰 원문은 저장하지 않고 해시만
저장하며, 한 사용자가 여러 기기에서 로그인할 수 있도록 `USERS` 1:N `REFRESH_TOKENS` 관계를 적용한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | Refresh Token 레코드 ID |
| `user_id` | BIGINT | FK → USERS.id, NOT NULL | 토큰 소유 사용자 |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL | Refresh Token 원문 대신 저장하는 해시 |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 만료 일시 |
| `revoked_at` | TIMESTAMPTZ | NULL | 로그아웃 또는 강제 폐기 일시 |
| `last_used_at` | TIMESTAMPTZ | NULL | 마지막 재발급 사용 일시 |
| `created_ip` | VARCHAR(45) | NULL | 발급 요청 IP |
| `user_agent` | VARCHAR(500) | NULL | 기기 및 브라우저 식별 보조 정보 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 발급 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **인증 처리 규칙**
> - 로그인 성공 — Access Token 과 Refresh Token 발급 → Refresh Token 해시 INSERT → HttpOnly 쿠키 설정
> - 재발급 — 서명과 만료 검증 → 해시 계산 → DB 존재 여부와 `revoked_at` 확인 → Access Token 재발급 및 `last_used_at` 갱신
> - 현재 기기 로그아웃 — 해당 토큰 `revoked_at` 기록 → 인증 쿠키 삭제
> - 전체 기기 로그아웃 — 사용자의 활성 토큰을 일괄 폐기
> - 사용자 탈퇴 — 활성 Refresh Token 을 모두 폐기
> - 만료 또는 폐기된 토큰의 재사용은 거부

---

## 6-2. 일반 회원 마이페이지

회원 본인의 프로필 이미지 등록·변경·삭제, 닉네임 관리, 주문, 결제, 환불, 발권 티켓과 QR 상태 조회를
제공한다. 프로필 이미지는 `USERS.profile_image_file_id` 로 `FILE_METADATA` 를 참조한다.

**엔티티** — SQL View `V_MEMBER_MYPAGE_PROFILE`, `V_MEMBER_MYPAGE_ORDERS`, `V_MEMBER_MYPAGE_TICKETS`

### V_MEMBER_MYPAGE_PROFILE

일반 회원 마이페이지의 프로필 이미지 조회 및 변경 화면.

| 출력/입력 필드 | 타입 | 원천/처리 |
|-|-|-|
| `member_user_id` | BIGINT | `USERS.id` |
| `nickname` | VARCHAR | `USERS.nickname` |
| `profile_image_file_id` | BIGINT | `USERS.profile_image_file_id` → `FILE_METADATA.id` |
| `profile_image_url` | VARCHAR | `FILE_METADATA.storage_key` 기반 서명 URL 생성 |
| `profile_image_updated_at` | TIMESTAMPTZ | `USERS.profile_image_updated_at` |

> **프로필 이미지 처리 규칙**
> - 허용 형식 — JPEG, PNG, WEBP
> - 권장 최대 크기 — 5MB
> - 업로드 성공 시 `FILE_METADATA` 생성 후 `USERS.profile_image_file_id` 갱신
> - 변경 시 새 파일 연결 후 기존 파일은 참조 여부를 확인하여 `DELETED` 처리
> - 삭제 시 `USERS.profile_image_file_id` 를 NULL 로 변경

### V_MEMBER_MYPAGE_ORDERS

일반 회원 주문 목록 및 상세 요약.

| 출력 필드 | 원천 / 계산 |
|-|-|
| `member_user_id` | `TICKET_ORDERS.member_user_id` |
| `order_id`, `order_number` | `TICKET_ORDERS` |
| `expo_id`, `expo_title` | `TICKET_ORDER_ITEMS` → `TICKET_PRODUCTS` → `EXPOS` |
| `order_status` | `TICKET_ORDERS.status` |
| `payment_status` | 최신 `TICKET_PAYMENTS.status` |
| `refund_status` | 최신 `TICKET_REFUNDS.status` |
| `total_quantity` | `TICKET_ORDERS.total_quantity` |
| `ticket_subtotal_amount` | `TICKET_ORDERS.ticket_subtotal_amount` |
| `booking_fee_rate` | `TICKET_ORDERS.booking_fee_rate` — 주문 당시 예매 수수료율 3% |
| `booking_fee_amount` | `TICKET_ORDERS.booking_fee_amount` |
| `total_amount` | `TICKET_ORDERS.total_amount` |
| `refundable` | 행사 3일 전 및 미사용 여부 계산 |

### V_MEMBER_MYPAGE_TICKETS

일반 회원 발권 티켓 및 QR 상세.

| 출력 필드 | 원천 / 계산 |
|-|-|
| `member_user_id` | `TICKET_ORDERS.member_user_id` |
| `order_id` | `TICKET_ORDERS.id` |
| `issued_ticket_id` | `ISSUED_TICKETS.id` |
| `ticket_code`, `status` | `ISSUED_TICKETS` |
| `checked_in_at` | `ISSUED_TICKETS.checked_in_at` |
| `expo_id`, `expo_title` | `EXPOS` |
| `secure_qr_access` | `TICKET_ACCESS_TOKENS` 유효성 |
| `event_start_at`, `event_end_at` | `EXPOS` — 2026-08-07 추가 |
| `order_item_quantity` | `TICKET_ORDER_ITEMS.quantity` — 2026-08-07 추가 |

> **2026-08-07 추가** — 마이페이지가 "어떤 박람회에 며칠, 몇 명 입장 가능한지" 를 보여주는데 원본
> 출력 필드에는 행사 기간도 수량도 없어 화면이 티켓 코드와 상태밖에 그릴 수 없었다.
>
> 새 컬럼은 **SELECT 목록 맨 끝에만** 붙인다. `CREATE OR REPLACE VIEW` 는 기존 컬럼의 이름·타입·순서를
> 바꾸지 못해, 중간에 끼워 넣으면 마이그레이션이 실패한다.

---

## 6-3. 박람회 관리

클라이언트의 박람회 개최 신청부터 관리자 심사, 승인 후 자동 공개, 장소 배정, 수정 요청, 취소와 변경 이력을 관리한다.

**엔티티** — `EXPO_OPENING_REQUESTS`, `EXPOS`, `EXPO_VENUE_ASSIGNMENTS`, `CATEGORIES`, `EXPO_CATEGORIES`, `EXPO_IMAGES`, `EXPO_FILES`, `EXTERNAL_LINKS`, `EXPO_COMPANIES`, `EXPO_REVIEW_HISTORIES`, `EXPO_CHANGE_REQUESTS`, `EXPO_CHANGE_HISTORIES`, `EXPO_CANCELLATION_REQUESTS`

### EXPO_OPENING_REQUESTS

모집 결과를 확인한 주최 클라이언트의 박람회 개최 신청이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 개최 신청 ID |
| `host_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 주최 클라이언트 |
| `recruitment_result_id` | BIGINT | FK → RECRUITMENT_RESULTS.id, NULL | 연결 모집 결과. 일반 등록은 NULL 가능 |
| `title` | VARCHAR(255) | NOT NULL | 박람회 명칭 |
| `description` | TEXT | NOT NULL | 소개 |
| `event_start_at` | TIMESTAMPTZ | NOT NULL | 행사 시작 |
| `event_end_at` | TIMESTAMPTZ | NOT NULL | 행사 종료 |
| `sales_start_at` | TIMESTAMPTZ | NOT NULL | 티켓 판매 시작 |
| `sales_end_at` | TIMESTAMPTZ | NOT NULL | 티켓 판매 종료 |
| `desired_venue_id` | BIGINT | FK → VIRTUAL_VENUES.id, NULL | 희망 장소 |
| `desired_venue_hall_id` | BIGINT | FK → VENUE_HALLS.id, NULL | 클라이언트가 신청 시 선택한 희망 홀 |
| `desired_venue_zone_id` | BIGINT | FK → VENUE_ZONES.id, NULL | 클라이언트가 신청 시 선택한 희망 구역 |
| `status` | VARCHAR(30) | NOT NULL, CHECK | `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `CANCELED` — 원본 10p 표 이탈 복원 (D-1) |
| `submitted_at` | TIMESTAMPTZ | NULL | 제출 일시 |
| `reviewed_by_admin_id` | BIGINT | FK → USERS.id, NULL | 심사 관리자 |
| `reviewed_at` | TIMESTAMPTZ | NULL | 심사 일시 |
| `rejection_reason` | TEXT | NULL | 반려 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **장소 선택 정책** — 클라이언트는 박람회 개최 신청 시 희망 장소·홀·구역을 직접 선택한다. 관리자는 승인 전에
> 선택값의 소속 관계와 기간 중복을 검증하며, 유효한 경우 동일 값으로 `VENUE_RESERVATIONS` 를 생성한다.

### EXPOS

승인·공개되어 티켓 판매와 체크인의 기준이 되는 박람회 원본이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 박람회 ID |
| `host_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 주최자 |
| `opening_request_id` | BIGINT | UNIQUE, FK → EXPO_OPENING_REQUESTS.id, NULL | 개최 신청 1:1 |
| `title` | VARCHAR(255) | NOT NULL | 공식 명칭 |
| `description` | TEXT | NOT NULL | 소개 |
| `region_code` | VARCHAR(30) | NOT NULL | 검색 지역 |
| `event_start_at` | TIMESTAMPTZ | NOT NULL | 행사 시작 |
| `event_end_at` | TIMESTAMPTZ | NOT NULL | 행사 종료 |
| `sales_start_at` | TIMESTAMPTZ | NOT NULL | 판매 시작 |
| `sales_end_at` | TIMESTAMPTZ | NOT NULL | 판매 종료 |
| `review_status` | VARCHAR(20) | NOT NULL, CHECK | `DRAFT`, `UNDER_REVIEW`, `REJECTED`, `APPROVED` |
| `visibility_status` | VARCHAR(20) | NOT NULL, CHECK | `PRIVATE`, `PUBLIC`, `ARCHIVED` |
| `event_status` | VARCHAR(20) | NOT NULL, CHECK | `SCHEDULED`, `ONGOING`, `ENDED`, `CANCELED` |
| `approved_by_admin_id` | BIGINT | FK → USERS.id, NULL | 승인 관리자 |
| `approved_at` | TIMESTAMPTZ | NULL | 승인·자동 공개 일시 |
| `rejection_reason` | TEXT | NULL | 반려 사유 |
| `canceled_at` | TIMESTAMPTZ | NULL | 취소 확정 일시 |
| `version` | BIGINT | NOT NULL DEFAULT 0 | 낙관적 락 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_VENUE_ASSIGNMENTS

박람회와 확정 장소 예약의 1:1 연결만 관리한다. 장소·홀·구역·기간과 중복 제약은 `VENUE_RESERVATIONS` 에서 단일 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 장소 배정 ID |
| `expo_id` | BIGINT | UNIQUE, FK → EXPOS.id, NOT NULL | 박람회 |
| `venue_reservation_id` | BIGINT | UNIQUE, FK → VENUE_RESERVATIONS.id, NOT NULL | 확정 장소 예약 |
| `assigned_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 배정 관리자 |
| `assigned_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 배정 시각 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |
| `예약 상태 제약` | — | 연결 대상 `VENUE_RESERVATIONS.status = CONFIRMED` | 확정 예약만 박람회에 연결 |
| `중복 저장 금지` | — | 장소·홀·구역·기간 컬럼 미보유 | 예약 원본과 값 중복 방지 |

> 이 테이블의 최종 컬럼 정의는 이 절(6-3)을 기준으로 한다. 6-14 절은 장소 예약 단일화 처리 흐름만 설명한다.

### CATEGORIES

박람회 검색·필터용 카테고리 마스터.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 카테고리 ID |
| `parent_id` | BIGINT | FK → CATEGORIES.id, NULL | 상위 카테고리 |
| `name` | VARCHAR(100) | UNIQUE, NOT NULL | 카테고리명 |
| `slug` | VARCHAR(100) | UNIQUE, NOT NULL | URL/검색 코드 |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE | 사용 여부 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_CATEGORIES

박람회와 카테고리의 N:M 연결 테이블.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `expo_id` | BIGINT | PK / FK → EXPOS.id | 박람회 |
| `category_id` | BIGINT | PK / FK → CATEGORIES.id | 카테고리 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 연결 일시 |

### EXPO_IMAGES

박람회 썸네일·상세 이미지를 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 이미지 연결 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `file_id` | BIGINT | FK → FILE_METADATA.id, NOT NULL | 이미지 |
| `image_type` | VARCHAR(20) | NOT NULL, CHECK | `THUMBNAIL`, `DETAIL`, `GALLERY` — CHECK 보정 (D-2) |
| `alt_text` | VARCHAR(255) | NULL | 대체 텍스트 |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| `(expo_id, file_id)` | — | UNIQUE | 중복 연결 방지 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_FILES

박람회 소개 PDF·카탈로그·리플렛·홍보영상 파일.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 파일 연결 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `file_id` | BIGINT | FK → FILE_METADATA.id, NOT NULL | 파일 |
| `file_purpose` | VARCHAR(30) | NOT NULL, CHECK | `INTRO_PDF`, `CATALOG`, `LEAFLET`, `PROMO_VIDEO`, `OTHER` — 원본 13p 표 이탈 복원 + CHECK 보정 (D-1, D-2) |
| `title` | VARCHAR(150) | NULL | 표시명 |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| `(expo_id, file_id)` | — | UNIQUE | 중복 연결 방지 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXTERNAL_LINKS

박람회 또는 부스 콘텐츠의 외부 링크를 공통 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 링크 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NULL | 박람회 링크 |
| `booth_content_id` | BIGINT | FK → BOOTH_CONTENTS.id, NULL | 부스 링크 |
| `link_type` | VARCHAR(30) | NOT NULL, CHECK | `HOMEPAGE`, `SOCIAL`, `RESERVATION`, `PRODUCT`, `OTHER` — 원본 14p 표 이탈 복원 + CHECK 보정 (D-1, D-2) |
| `label` | VARCHAR(100) | NULL | 표시 문구 |
| `url` | VARCHAR(1000) | NOT NULL | 외부 URL |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| `owner_xor` | — | CHECK | `expo_id` 와 `booth_content_id` 중 정확히 하나만 NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_COMPANIES

박람회에 참여하는 기업과 배정 부스를 연결한 조회 기준 테이블.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 참여 기업 연결 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 참여 기업 |
| `booth_allocation_id` | BIGINT | FK → BOOTH_ALLOCATIONS.id, NULL | 연결 부스 배정 |
| `display_name` | VARCHAR(150) | NOT NULL | 노출 기업명 |
| `participation_status` | VARCHAR(20) | NOT NULL, CHECK | `CONFIRMED`, `CANCELED` |
| `(expo_id, client_user_id)` | — | UNIQUE | 박람회 내 기업 중복 방지 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_REVIEW_HISTORIES

박람회 심사 승인·반려 이력을 기록한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 심사 이력 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `reviewer_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 관리자 |
| `decision` | VARCHAR(20) | NOT NULL, CHECK | `SUBMIT`, `APPROVE`, `REJECT` — CHECK 보정 (D-2) |
| `reason` | TEXT | NULL | 반려/처리 사유 |
| `from_status` | VARCHAR(20) | NULL | 변경 전 |
| `to_status` | VARCHAR(20) | NOT NULL | 변경 후 |
| `reviewed_at` | TIMESTAMPTZ | NOT NULL | 처리 일시 |

### EXPO_CHANGE_REQUESTS

승인된 박람회에 대한 클라이언트 수정 요청.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 수정 요청 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `requester_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 요청 클라이언트 |
| `change_reason` | TEXT | NOT NULL | 수정 사유 |
| `requested_changes` | JSONB | NOT NULL | 요청 항목과 값 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `SUBMITTED`, `UNDER_REVIEW`, `APPLIED`, `REJECTED`, `CANCELED` — 원본 15p 표 이탈 복원 (D-1) |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NULL | 처리 관리자 |
| `processed_at` | TIMESTAMPTZ | NULL | 처리 일시 |
| `rejection_reason` | TEXT | NULL | 거절 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_CHANGE_HISTORIES

승인 후 관리자가 실제 적용한 박람회 변경 전·후 값을 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 변경 이력 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `change_request_id` | BIGINT | FK → EXPO_CHANGE_REQUESTS.id, NULL | 원본 요청 |
| `changed_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 변경 관리자 |
| `before_data` | JSONB | NOT NULL | 변경 전 |
| `after_data` | JSONB | NOT NULL | 변경 후 |
| `reason` | TEXT | NULL | 변경 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 적용 일시 |

### EXPO_CANCELLATION_REQUESTS

클라이언트의 박람회 취소 요청과 관리자 승인 결과.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 취소 요청 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `requester_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 요청자 |
| `reason` | TEXT | NOT NULL | 취소 사유 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `SUBMITTED`, `APPROVED`, `REJECTED`, `PROCESSING_REFUNDS`, `COMPLETED` — 원본 16p 표 이탈 복원 (D-1) |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NULL | 관리자 |
| `processed_at` | TIMESTAMPTZ | NULL | 처리 일시 |
| `rejection_reason` | TEXT | NULL | 반려 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **박람회 취소 승인 시 장소 예약 해제 처리** — 관리자가 취소를 승인하면 `EXPOS.event_status` 를 `CANCELED` 로
> 변경하고 신규 티켓 판매를 중단한다. 이후 `EXPO_VENUE_ASSIGNMENTS.venue_reservation_id` 로 연결된
> `VENUE_RESERVATIONS` 를 조회하여 `status` 를 `RELEASED` 로 변경하고 `released_at` 을 기록한다.
> 티켓 일괄 환불 등록과 장소 예약 해제는 같은 취소 처리 트랜잭션에서 수행하며, 이미 행사가 종료된 경우 등
> 운영상 재사용이 불가능한 예약은 별도 사유를 기록한다.

---

## 6-4. 검색 및 화면 노출

공개된 박람회의 검색, 카테고리·지역·기간·가격·판매 상태 필터, 정렬과 카드형 목록 노출을 지원하는 조회 모델이다.

### V_PUBLIC_EXPO_CARDS

홈·검색·필터·정렬 카드 목록.

| 출력 필드 | 타입 | 원천/계산 |
|-|-|-|
| `expo_id` | BIGINT | `EXPOS.id` |
| `title` | VARCHAR | `EXPOS.title` |
| `event_period` | RANGE | `EXPOS.event_start_at` / `event_end_at` |
| `region_code` | VARCHAR | `EXPOS.region_code` |
| `minimum_price` | NUMERIC | `MIN(TICKET_PRODUCTS.price)` |
| `available_quantity` | INTEGER | `SUM(TICKET_INVENTORIES.available_quantity)` |
| `display_sales_status` | VARCHAR | 판매 기간 + 재고 계산 — 판매 예정/판매중/매진/판매종료 |
| `popularity_score` | NUMERIC | 판매량·주문량 기반 계산 |

---

## 6-5. 티켓 및 재고

박람회별 표준 1일권, 판매 기간·가격·수량, 재고 임시 확보와 결제 성공 후 확정 차감, 만료·환불 후 재고 복구를 관리한다.

**엔티티** — `TICKET_PRODUCTS`, `TICKET_INVENTORIES`, `INVENTORY_RESERVATIONS`

### TICKET_PRODUCTS

박람회에서 판매하는 표준 1일권 티켓 상품.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 티켓 상품 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `name` | VARCHAR(150) | NOT NULL | 티켓명 |
| `description` | TEXT | NULL | 설명 |
| `price` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 판매가 |
| `sales_start_at` | TIMESTAMPTZ | NOT NULL | 판매 시작 |
| `sales_end_at` | TIMESTAMPTZ | NOT NULL | 판매 종료 |
| `max_quantity_per_order` | INTEGER | NOT NULL DEFAULT 4, CHECK 1..4 | 주문당 최대 수량 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `DRAFT`, `ON_SALE`, `SOLD_OUT`, `SALE_ENDED`, `CANCELED` — 원본 19p 표 이탈 복원 (D-1) |
| `version` | BIGINT | NOT NULL DEFAULT 0 | 상품 수정 낙관적 락 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### TICKET_INVENTORIES

티켓 상품의 총량·예약량·판매량을 동시성 안전하게 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 재고 ID |
| `ticket_product_id` | BIGINT | UNIQUE, FK → TICKET_PRODUCTS.id, NOT NULL | 상품 1:1 |
| `total_quantity` | INTEGER | NOT NULL, CHECK >= 0 | 총 판매 수량 |
| `reserved_quantity` | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 임시 확보 수량 |
| `sold_quantity` | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 결제 완료 수량 |
| `available_quantity` | INTEGER | GENERATED ALWAYS AS … STORED | `total − reserved − sold`. PostgreSQL 생성 컬럼으로 확정 |
| `version` | BIGINT | NOT NULL DEFAULT 0 | 낙관적 락 |
| `updated_at` | TIMESTAMPTZ | NOT NULL | 갱신 일시 |
| `오버셀 방지` | — | CHECK | `reserved_quantity + sold_quantity <= total_quantity` (부록 F) |

### INVENTORY_RESERVATIONS

티켓 주문 결제 대기 동안 수량을 임시 확보한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 재고 확보 ID |
| `ticket_product_id` | BIGINT | FK → TICKET_PRODUCTS.id, NOT NULL | 티켓 상품 |
| `ticket_order_id` | BIGINT | FK → TICKET_ORDERS.id, NOT NULL | 주문 |
| `quantity` | INTEGER | NOT NULL, CHECK > 0 | 확보 수량 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `CONFIRMED`, `EXPIRED`, `RELEASED` |
| `reserved_at` | TIMESTAMPTZ | NOT NULL | 확보 시작 |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 만료 |
| `released_at` | TIMESTAMPTZ | NULL | 반환 일시 |

---

## 6-6. 주문 및 결제

회원과 비회원의 티켓 주문, 결제 시도, 결제 승인 이력 및 전체 주문 환불을 관리한다. 구매자는 티켓
판매원금에 3% 예매 수수료를 추가로 결제하며, 부분 취소는 지원하지 않는다.

**엔티티** — `TICKET_ORDERS`, `TICKET_ORDER_ITEMS`, `GUEST_ORDER_INFOS`, `TICKET_PAYMENTS`, `TICKET_PAYMENT_HISTORIES`, `TICKET_REFUNDS`

### TICKET_ORDERS

주문 당시 판매원금과 예매 수수료를 스냅샷으로 보존한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 주문 ID |
| `order_number` | VARCHAR(40) | UNIQUE, NOT NULL | `ORD` + 날짜 + 랜덤 형식 주문번호 |
| `member_user_id` | BIGINT | FK → USERS.id, NULL | 회원 주문자. 비회원은 NULL |
| `orderer_type` | VARCHAR(20) | NOT NULL, CHECK | `MEMBER`, `GUEST` |
| `ticket_subtotal_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 티켓 단가 × 수량 판매원금 |
| `booking_fee_rate` | NUMERIC(6,5) | NOT NULL DEFAULT 0.03000 | 주문 당시 예매 수수료율 |
| `booking_fee_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | `ticket_subtotal_amount` × 3% |
| `total_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 판매원금 + 예매 수수료 |
| `total_quantity` | INTEGER | NOT NULL, CHECK 1..4 | 주문 총 수량 |
| `status` | VARCHAR(30) | NOT NULL, CHECK | `PENDING`, `PAID`, `CANCELED`, `PAYMENT_FAILED`, `EXPIRED` |
| `paid_at` | TIMESTAMPTZ | NULL | 결제 완료 일시 |
| `canceled_at` | TIMESTAMPTZ | NULL | 전체 취소 일시 |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL | 생성 및 수정 일시 |

### TICKET_ORDER_ITEMS

주문 당시 티켓 상품, 단가와 수량을 스냅샷으로 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 주문 항목 ID |
| `ticket_order_id` | BIGINT | FK → TICKET_ORDERS.id, NOT NULL | 티켓 주문 |
| `ticket_product_id` | BIGINT | FK → TICKET_PRODUCTS.id, NOT NULL | 티켓 상품 |
| `quantity` | INTEGER | NOT NULL, CHECK > 0 | 구매 수량 |
| `unit_price` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 주문 당시 티켓 단가 |
| `item_subtotal_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 항목 판매원금 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |
| `(ticket_order_id, ticket_product_id)` | — | UNIQUE | 중복 항목 방지 |

> **금액 검증** — `item_subtotal_amount` = `unit_price` × `quantity`
>
> 이전 문서의 `line_amount` 와 v16 본문의 `item_subtotal_amount` 가 같은 의미로 혼용되어 있었다.
> 최종 명칭은 `item_subtotal_amount` 로 통일한다 (D-4).

### GUEST_ORDER_INFOS

비회원 주문의 개인정보와 주문 조회 비밀번호를 주문과 1:1 로 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `ticket_order_id` | BIGINT | PK, FK → TICKET_ORDERS.id | 비회원 주문 |
| `guest_name` | VARCHAR(100) | NOT NULL | 구매자 이름 |
| `phone_number` | VARCHAR(20) | NOT NULL | 휴대폰 번호 |
| `age` | INTEGER | NULL, CHECK >= 0 | 구매자 연령 |
| `lookup_password_hash` | VARCHAR(255) | NOT NULL | 주문 조회 비밀번호 해시 |
| `failed_lookup_count` | INTEGER | NOT NULL DEFAULT 0 | 조회 인증 실패 횟수 |
| `locked_until` | TIMESTAMPTZ | NULL | 반복 실패 시 조회 제한 종료 시각 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **처리 규칙**
> - 비회원 조회 비밀번호 원문은 저장하지 않는다.
> - `TICKET_ORDERS.orderer_type = GUEST` 인 주문만 레코드를 생성한다.
> - 주문번호, 이름, 휴대폰 번호, 나이 및 조회 비밀번호 검증에 사용한다.
> - 회원 주문에는 `GUEST_ORDER_INFOS` 를 생성하지 않는다.

### TICKET_PAYMENTS

토스페이먼츠 티켓 결제의 요청과 승인 결과를 관리한다.

> ⚠ **타입·제약조건은 추정이다.** v16 은 컬럼 6개의 의미와 상태값만 서술하고 타입을 주지 않았다.
> 같은 PG·같은 결제 생명주기를 가진 형제 테이블 `BOOTH_PAYMENTS` 의 구조를 근거로 채웠다.
> → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 컬럼명 | 타입 | 제약조건 | 설명 | 출처 |
|-|-|-|-|-|
| `id` | BIGSERIAL | PK | 티켓 결제 ID | 추정 |
| `ticket_order_id` | BIGINT | FK → TICKET_ORDERS.id, NOT NULL | 결제 대상 주문 | 추정 |
| `payment_key` | VARCHAR(200) | UNIQUE, NULL | PG 결제 식별키 | 보완 |
| `pg_order_id` | VARCHAR(100) | UNIQUE, NOT NULL | PG 전달 주문 ID | 추정 |
| `method` | VARCHAR(30) | NULL | `CARD`, `MOBILE_PHONE` | 추정 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `READY`, `IN_PROGRESS`, `DONE`, `FAILED`, `CANCELED` | 명세 |
| `requested_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 판매원금 + 예매 수수료를 포함한 최종 요청 금액 | 보완 |
| `approved_amount` | NUMERIC(15,2) | NULL, CHECK >= 0 | PG 가 승인한 최종 결제 금액 | 보완 |
| `ticket_subtotal_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 승인 당시 티켓 판매원금 스냅샷 | 보완 |
| `booking_fee_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 승인 당시 3% 예매 수수료 스냅샷 | 보완 |
| `approved_at` | TIMESTAMPTZ | NULL | 승인 일시 | 추정 |
| `canceled_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 승인 전 취소 또는 PG 취소 금액 | 추정 |
| `last_failure_code` | VARCHAR(100) | NULL | 최근 실패 코드 | 추정 |
| `idempotency_key` | VARCHAR(100) | UNIQUE, NOT NULL | 중복 승인 방지 키 | 추정 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 | 추정 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 | 추정 |

> **검증** — `approved_amount` = `TICKET_ORDERS.total_amount` (명세)
>
> 승인 성공 상태값이 티켓은 `DONE`, 부스는 `APPROVED` 다. 둘 다 명세에 있는 값이라 통일하지 않았다 (D-4).

### TICKET_PAYMENT_HISTORIES

티켓 결제·취소의 상태 변경과 PG 응답을 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 결제 이력 ID |
| `ticket_payment_id` | BIGINT | FK → TICKET_PAYMENTS.id, NOT NULL | 대상 결제 |
| `event_type` | VARCHAR(30) | NOT NULL, CHECK | `REQUEST`, `APPROVE`, `FAIL`, `CANCEL` |
| `from_status` | VARCHAR(20) | NULL | 변경 전 상태 |
| `to_status` | VARCHAR(20) | NOT NULL | 변경 후 상태 |
| `amount` | NUMERIC(15,2) | NULL | 처리 금액 |
| `pg_transaction_key` | VARCHAR(200) | NULL | PG 거래 키 |
| `response_payload` | JSONB | NULL | PG 응답 |
| `occurred_at` | TIMESTAMPTZ | NOT NULL | 발생 일시 |

> **처리 규칙**
> - 결제 승인과 전체 주문 취소 이력은 물리 삭제하지 않는다.
> - 부분 취소는 지원하지 않는다.
> - 환불 완료 시 결제 취소 이력과 `TICKET_REFUNDS` 처리 결과를 함께 남긴다.

### TICKET_REFUNDS

전체 주문 취소만 지원한다. 부분 취소는 없다.

> ⚠ **키·시각·처리자 컬럼은 추정이다.** v16 은 금액 3개와 상태 4값만 제시했다. `TICKET_PAYMENTS`
> 구조와 "박람회 취소 승인 시 티켓 일괄 환불을 등록한다"(6-3 절)는 서술을 근거로 채웠다.
> → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 컬럼명 | 타입 | 제약조건 | 설명 | 출처 |
|-|-|-|-|-|
| `id` | BIGSERIAL | PK | 환불 ID | 추정 |
| `ticket_order_id` | BIGINT | UNIQUE, FK → TICKET_ORDERS.id, NOT NULL | 환불 대상 주문 | 추정 |
| `ticket_payment_id` | BIGINT | FK → TICKET_PAYMENTS.id, NOT NULL | 원 결제 | 추정 |
| `refund_ticket_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 환불되는 티켓 판매원금 | 보완 |
| `refund_booking_fee_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 함께 환불되는 예매 수수료 | 보완 |
| `refund_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 두 금액의 합계 | 보완 |
| `status` | VARCHAR(30) | NOT NULL, CHECK | `REQUESTED`, `PROCESSING`, `COMPLETED`, `FAILED` | 명세 |
| `reason` | TEXT | NULL | 환불 사유 | 추정 |
| `pg_refund_key` | VARCHAR(200) | UNIQUE, NULL | PG 환불 거래 키 | 추정 |
| `requested_at` | TIMESTAMPTZ | NOT NULL | 환불 요청 일시 | 추정 |
| `completed_at` | TIMESTAMPTZ | NULL | 환불 완료 일시 | 추정 |
| `last_failure_code` | VARCHAR(100) | NULL | 최근 실패 코드 | 추정 |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NULL | 박람회 취소 일괄 환불 시 처리 관리자 | 추정 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 | 추정 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 | 추정 |

> **금액 검증식** (명세)
> - 구매자 결제액 = `ticket_subtotal_amount` + `booking_fee_amount`
> - `booking_fee_rate` = 0.03000
> - `refund_amount` = `refund_ticket_amount` + `refund_booking_fee_amount`
>
> `ticket_order_id` 의 UNIQUE 는 부분 취소를 지원하지 않는다는 정책에 근거한 주문당 1건 가정이다.
> 다만 `V_MEMBER_MYPAGE_ORDERS` 가 "**최신** `TICKET_REFUNDS.status`" 라는 표현을 쓴다 (D-4).

---

## 6-7. 발권 및 QR 체크인

결제 완료 후 수량별 발권 티켓과 QR 토큰을 생성하고, 클라이언트가 본인 박람회의 QR 또는 티켓 코드를
검증하여 1회 체크인 처리한다. 체크인 취소·복구 기능은 구현하지 않는다.

**엔티티** — `ISSUED_TICKETS`, `TICKET_ACCESS_TOKENS`, `CHECK_IN_HISTORIES`

### ISSUED_TICKETS

결제 완료 후 구매 수량만큼 발급되는 개별 입장권.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 발권 티켓 ID |
| `ticket_order_item_id` | BIGINT | FK → TICKET_ORDER_ITEMS.id, NOT NULL | 주문 항목 |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 검증 대상 박람회 |
| `ticket_code` | VARCHAR(50) | UNIQUE, NOT NULL | `EXPO-생성일자-일련번호 6자리` |
| `qr_token_hash` | VARCHAR(255) | UNIQUE, NOT NULL | QR 원문 대신 서버 검증용 해시 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `ISSUED`, `CHECKED_IN`, `CANCELED`, `INVALIDATED` |
| `issued_at` | TIMESTAMPTZ | NOT NULL | 발권 일시 |
| `checked_in_at` | TIMESTAMPTZ | NULL | 입장 일시 |
| `invalidated_at` | TIMESTAMPTZ | NULL | 무효 일시 |
| `version` | BIGINT | NOT NULL DEFAULT 0 | 중복 체크인 방지 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### TICKET_ACCESS_TOKENS

카카오 메시지로 전송하는 QR 확인 보안 URL 의 단기·폐기 가능한 접근 토큰.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 접근 토큰 ID |
| `ticket_order_id` | BIGINT | FK → TICKET_ORDERS.id, NOT NULL | 조회 주문 |
| `issued_ticket_id` | BIGINT | FK → ISSUED_TICKETS.id, NULL | 특정 티켓 링크 |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL | 접근 토큰 해시 |
| `scope` | VARCHAR(30) | CHECK | `ORDER_VIEW`, `QR_VIEW` — 원본에 NOT NULL 표기 없음 (D-4) |
| `status` | VARCHAR(20) | CHECK | `ACTIVE`, `EXPIRED`, `REVOKED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 만료 |
| `last_accessed_at` | TIMESTAMPTZ | NULL | 마지막 접근 |
| `access_count` | INTEGER | DEFAULT 0 | 접근 횟수 — 원본에 NOT NULL 표기 없음 (D-4) |
| `revoked_at` | TIMESTAMPTZ | NULL | 폐기 일시 |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | 생성 일시 — 원본에 NOT NULL 표기 없음 (D-4) |

### CHECK_IN_HISTORIES

QR 또는 티켓 코드 검증·입장 처리 결과를 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 체크인 이력 ID |
| `issued_ticket_id` | BIGINT | FK → ISSUED_TICKETS.id, NOT NULL | 티켓 |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 박람회 |
| `processed_by_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 체크인 처리 클라이언트 |
| `method` | VARCHAR(20) | NOT NULL, CHECK | `QR`, `MANUAL_CODE` |
| `result` | VARCHAR(30) | NOT NULL, CHECK | `SUCCESS`, `ALREADY_USED`, `CANCELED_TICKET`, `WRONG_EXPO`, `INVALID_TOKEN` — 원본 24p 표 이탈 복원 (D-1) |
| `checked_at` | TIMESTAMPTZ | NOT NULL | 검증/처리 일시 |
| `request_ip` | VARCHAR(45) | NULL | 요청 IP |
| `detail` | TEXT | NULL | 오류 상세 |

---

## 6-8. 알림

결제·환불·박람회 취소 안내와 QR 접근 URL 의 메시지 발송 요청, 성공·실패 및 재시도 이력을 관리한다.

> **v16 이후 변경 (2026-08-07)** — 원본은 이 절을 "카카오 알림" 으로 두고 발송 채널을 카카오 알림톡
> 하나로 가정했다. 실제 발송은 **Solapi SMS** 로 확정되어 아래 둘을 바꿨다.
> 마이그레이션은 `V202608071533__add_sms_notification_channel.sql` 이다.
>
> - `NOTIFICATIONS.channel` CHECK 에 `SMS` 추가 → `KAKAO`, `SMS`, `EMAIL`, `IN_APP`
> - `KAKAO_MESSAGE_HISTORIES` → **`MESSAGE_HISTORIES`** 로 이름 변경, `channel` 컬럼 추가
>
> 알림톡 채널 개설과 템플릿 사전 승인이 단기간에 끝나지 않아 SMS 를 먼저 붙인다.
> 카카오를 포기한 것이 아니라 채널이 둘 이상이 된 것이므로, 이력 테이블을 채널 중립으로 바꿨다.

**엔티티** — `NOTIFICATIONS`, `MESSAGE_HISTORIES`

### NOTIFICATIONS

채널 독립적인 알림 작업과 발송 상태.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 알림 ID |
| `recipient_user_id` | BIGINT | FK → USERS.id, NULL | 회원 수신자 |
| `recipient_phone_number` | VARCHAR(20) | NULL | 비회원/카카오 수신 번호 |
| `channel` | VARCHAR(20) | NOT NULL, CHECK | `KAKAO`, `SMS`, `EMAIL`, `IN_APP` — `SMS` 는 2026-08-07 추가 |
| `template_code` | VARCHAR(50) | NOT NULL | 알림 템플릿 코드 |
| `reference_type` | VARCHAR(30) | NULL | `ORDER`, `PAYMENT`, `REFUND`, `EXPO`, `TICKET` 등 — 열거가 개방형이라 CHECK 미적용 (D-2) |
| `reference_id` | BIGINT | NULL | 참조 데이터 ID |
| `payload` | JSONB | NOT NULL | 템플릿 변수 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `PENDING`, `SENT`, `FAILED`, `RETRYING`, `CANCELED` |
| `scheduled_at` | TIMESTAMPTZ | NULL | 예약 발송 |
| `sent_at` | TIMESTAMPTZ | NULL | 성공 일시 |
| `retry_count` | INTEGER | NOT NULL DEFAULT 0 | 재시도 횟수 |
| `last_error` | TEXT | NULL | 최근 오류 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### MESSAGE_HISTORIES

발송 시도별 외부 시스템 응답. (구 `KAKAO_MESSAGE_HISTORIES`)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 발송 이력 ID |
| `notification_id` | BIGINT | FK → NOTIFICATIONS.id, NOT NULL | 알림 |
| `channel` | VARCHAR(20) | NOT NULL, CHECK | `KAKAO`, `SMS`, `EMAIL` — 2026-08-07 추가. 아래 처리 규칙 참고 |
| `provider_message_id` | VARCHAR(200) | NULL | 발송 대행사 메시지 ID |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `REQUESTED`, `SENT`, `DELIVERED`, `FAILED` |
| `request_payload` | JSONB | NULL | 요청 데이터 |
| `response_payload` | JSONB | NULL | 응답 데이터 |
| `error_code` | VARCHAR(100) | NULL | 실패 코드 |
| `attempt_no` | INTEGER | NOT NULL | 시도 번호 |
| `requested_at` | TIMESTAMPTZ | NOT NULL | 요청 일시 |
| `completed_at` | TIMESTAMPTZ | NULL | 완료 일시 |
| `(notification_id, attempt_no)` | — | UNIQUE | 알림별 시도 번호 중복 방지 |

> **`channel` 을 알림이 아니라 시도에 두는 이유**
> `NOTIFICATIONS.channel` 이 이미 있는데 이력에도 채널을 두는 것은, **한 알림의 시도마다 채널이 달라질 수
> 있어서**다. Solapi 는 알림톡 발송이 실패하면 SMS 로 대체발송하는 것이 표준 동작이라, 알림 단위 채널
> 하나로는 "1차는 알림톡, 2차는 SMS" 를 표현할 수 없다.

---

## 6-9. 클라이언트 마이페이지

클라이언트는 프로필 이미지 등록·변경·삭제, 닉네임 수정, 본인이 등록한 박람회, 일별 매출, 기업 모집,
부스 신청 및 정산 리포트를 조회한다. 프로필 이미지는 일반 회원과 동일하게 `USERS.profile_image_file_id`
를 사용하며 정산 금액의 직접 수정 또는 확정 권한은 없다.

### V_CLIENT_DASHBOARD_PROFILE

클라이언트 페이지 상단 프로필 영역에서 계정 이미지와 사업자 기본 정보를 함께 표시한다.

| 출력/입력 필드 | 타입 | 원천/처리 |
|-|-|-|
| `client_user_id` | BIGINT | `USERS.id` = `CLIENT_PROFILES.user_id` |
| `nickname` | VARCHAR | `USERS.nickname` |
| `company_name` | VARCHAR | `CLIENT_PROFILES.company_name` |
| `profile_image_file_id` | BIGINT | `USERS.profile_image_file_id` → `FILE_METADATA.id` |
| `profile_image_url` | VARCHAR | `FILE_METADATA.storage_key` 기반 서명 URL 생성 |
| `profile_image_updated_at` | TIMESTAMPTZ | `USERS.profile_image_updated_at` |

### V_CLIENT_DASHBOARD_EXPOS

본인 등록 박람회 목록과 심사·노출 상태.

> ⚠ **출력 필드는 추정이다.** 원본은 이름과 위 한 줄 용도만 제시했다. 용도 문장의 항목을 필드로
> 옮기고 식별자·조인 키를 더한 선에서 멈췄다. → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `client_user_id` | `EXPOS.host_client_id` | 추정 |
| `expo_id`, `title` | `EXPOS` | 추정 |
| `event_start_at`, `event_end_at` | `EXPOS` | 추정 |
| `sales_start_at`, `sales_end_at` | `EXPOS` | 추정 |
| `review_status` | `EXPOS.review_status` | 명세 (용도 문장의 "심사 상태") |
| `visibility_status` | `EXPOS.visibility_status` | 명세 (용도 문장의 "노출 상태") |
| `event_status` | `EXPOS.event_status` | 추정 |
| `approved_at` | `EXPOS.approved_at` | 추정 |
| `ticket_product_count` | `COUNT(TICKET_PRODUCTS)` | 추정 |

### V_CLIENT_DASHBOARD_DAILY_SALES

박람회별 일자별 티켓 판매 및 환불 현황.

> ⚠ **출력 필드는 추정이다.** `EXPO_DAILY_SALES_SUMMARIES` 가 정확히 이 용도의 집계 테이블이므로
> 그것을 그대로 노출하고 소유자 필터용 키와 박람회명만 덧붙였다.

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `client_user_id` | `EXPOS.host_client_id` | 추정 |
| `expo_id`, `expo_title` | `EXPOS` | 추정 |
| `sales_date` | `EXPO_DAILY_SALES_SUMMARIES.sales_date` | 추정 |
| `paid_order_count`, `canceled_order_count` | `EXPO_DAILY_SALES_SUMMARIES` | 추정 |
| `sold_ticket_quantity`, `refund_ticket_quantity` | `EXPO_DAILY_SALES_SUMMARIES` | 추정 |
| `ticket_sales_amount`, `refund_ticket_amount` | `EXPO_DAILY_SALES_SUMMARIES` | 추정 |
| `booking_fee_amount`, `refund_booking_fee_amount` | `EXPO_DAILY_SALES_SUMMARIES` | 추정 |
| `buyer_payment_amount`, `client_settlement_base_amount` | `EXPO_DAILY_SALES_SUMMARIES` | 추정 |
| `calculated_at` | `EXPO_DAILY_SALES_SUMMARIES.calculated_at` | 추정 |

### V_CLIENT_DASHBOARD_RECRUITMENT

모집 공고와 신청 완료 수, 확정 배정 수.

> ⚠ **출력 필드는 추정이다.** 용도 문장의 세 항목(공고, 신청 완료 수, 확정 배정 수)을 그대로 옮겼다.

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `host_client_id` | `RECRUITMENT_NOTICES.host_client_id` | 추정 |
| `recruitment_notice_id`, `title`, `status` | `RECRUITMENT_NOTICES` | 명세 (용도 문장의 "모집 공고") |
| `application_start_at`, `application_end_at` | `RECRUITMENT_NOTICES` | 추정 |
| `published_at`, `closed_at` | `RECRUITMENT_NOTICES` | 추정 |
| `submitted_application_count` | `COUNT(PARTICIPATION_APPLICATIONS WHERE status='SUBMITTED')` | 명세 (용도 문장의 "신청 완료 수") |
| `confirmed_allocation_count` | `COUNT(BOOTH_ALLOCATIONS WHERE status='ASSIGNED')` | 명세 (용도 문장의 "확정 배정 수") |

### V_CLIENT_DASHBOARD_BOOTHS

참여 신청별 부스 진행 상태.

> ⚠ **출력 필드는 추정이다.** 용도 문장의 다섯 항목을 그대로 옮기고 조인 키를 더했다.
> 관리자용 `V_ADMIN_PARTICIPATION_OPERATIONS` 와 원천이 같아 그 필드 구성을 참고했다.

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `client_user_id` | `PARTICIPATION_APPLICATIONS.client_user_id` | 추정 |
| `recruitment_notice_id` | `PARTICIPATION_APPLICATIONS.recruitment_notice_id` | 추정 |
| `application_id`, `application_status` | `PARTICIPATION_APPLICATIONS` | 명세 (용도 문장의 "신청 상태") |
| `booth_order_id`, `booth_order_status` | `BOOTH_ORDERS.id` / `status` | 명세 (용도 문장의 "부스 주문") |
| `payment_status` | 최신 `BOOTH_PAYMENTS.status` | 명세 (용도 문장의 "결제 상태") |
| `paid_at` | `BOOTH_ORDERS.paid_at` | 명세 (용도 문장의 "결제 완료 시각") |
| `booth_allocation_id`, `allocation_status` | `BOOTH_ALLOCATIONS` | 명세 (용도 문장의 "확정 부스") |
| `booth_number` | `BOOTHS.booth_number` | 추정 |

### V_CLIENT_DASHBOARD_SETTLEMENTS

박람회별 최종 정산 리포트. 원본이 필수 출력 필드를 명시했다.

```
expo_id, expo_title, event_end_at, settlement_due_at, status,
gross_ticket_sales_amount, ticket_refund_amount, net_ticket_sales_amount,
booking_fee_gross_amount, booking_fee_refund_amount, booking_fee_net_amount,
gross_booth_sales_amount, adjustment_amount, remittance_due_amount,
remitted_amount, remitted_at, remittance_status, latest_report_file
```

> **권한 규칙**
> - 클라이언트는 본인 소유 박람회의 데이터만 조회한다.
> - 예매 수수료는 플랫폼 수익으로 별도 표시하며 클라이언트 송금액에서 다시 차감하지 않는다.
> - 정산 리포트는 PDF 또는 엑셀로 다운로드할 수 있다.

---

## 6-10. 정산 및 회계

행사 종료 후 7~14일 이내 정산 대상을 생성하고, 금액 자동 계산, 관리자 검토, 조정, 확정, 외부 송금 및
결과 기록을 처리한다. 은행 자동 송금은 MVP 에 포함하지 않는다.

**엔티티** — `EXPO_DAILY_SALES_SUMMARIES`, `SETTLEMENTS`, `SETTLEMENT_ITEMS`, `SETTLEMENT_ADJUSTMENTS`, `REMITTANCES`, `SETTLEMENT_REPORTS`, SQL View `V_CLIENT_DASHBOARD_SETTLEMENTS`, `V_ADMIN_SETTLEMENT_STATUS`

### SETTLEMENTS

박람회 단위 정산 대표 정보다.

> ⚠ **키 컬럼과 제약조건은 추정이다.** v16 은 금액·상태 컬럼과 타입만 제시하고 PK·FK 를 주지 않았다.
> "박람회 단위 정산"이라는 서술, `V_CLIENT_DASHBOARD_SETTLEMENTS` 의 출력 필드, 그리고
> `SETTLEMENT_ITEMS`·`SETTLEMENT_ADJUSTMENTS`·`SETTLEMENT_REPORTS` 가 `SETTLEMENTS.id` 를
> 참조한다는 사실을 근거로 채웠다. → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 컬럼명 | 타입 | 제약조건 | 설명 | 출처 |
|-|-|-|-|-|
| `id` | BIGSERIAL | PK | 정산 ID | 추정 |
| `expo_id` | BIGINT | UNIQUE, FK → EXPOS.id, NOT NULL | 대상 박람회 | 추정 |
| `host_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 송금 대상 주최자 | 추정 |
| `gross_ticket_sales_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 환불 전 티켓 판매원금 합계 | 보완 |
| `ticket_refund_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 환불된 티켓 판매원금 | 보완 |
| `net_ticket_sales_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | `gross_ticket_sales_amount − ticket_refund_amount` | 보완 |
| `booking_fee_gross_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 구매자에게 부과한 예매 수수료 합계 | 보완 |
| `booking_fee_refund_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 취소 주문에서 환불한 예매 수수료 | 보완 |
| `booking_fee_net_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 플랫폼 예매 수수료 순수익 | 보완 |
| `gross_booth_sales_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 결제 완료 부스비 합계 | 보완 |
| `pg_fee_reference_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 회계 참고값. 송금액에서 추가 차감하지 않음 | 보완 |
| `adjustment_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 관리자가 확정한 수동 조정 금액 | 보완 |
| `remittance_due_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | `net_ticket_sales_amount + gross_booth_sales_amount ± adjustment_amount` | 보완 |
| `settlement_due_at` | TIMESTAMPTZ | NOT NULL | 행사 종료 후 7~14일 이내 | 보완 |
| `confirmed_at` | TIMESTAMPTZ | NULL | 정산 확정 일시 | 명세 |
| `confirmed_by` | BIGINT | FK → USERS.id, NULL | 확정 관리자 | 명세 |
| `status` | VARCHAR(30) | NOT NULL, CHECK | `WAITING`, `CALCULATED`, `UNDER_REVIEW`, `CONFIRMED`, `REMITTANCE_PENDING`, `REMITTED`, `ON_HOLD` | 명세 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 | 추정 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 | 추정 |

> 금액 컬럼에 `CHECK >= 0` 을 걸지 않았다. `adjustment_amount` 는 음수가 될 수 있고, 나머지도
> 환불이 섞이면 부호가 뒤집힐 수 있다. `expo_id` 의 UNIQUE 는 박람회당 정산 1건 가정이다 (D-4).

### SETTLEMENT_ITEMS

정산 금액을 구성하는 결제·환불·예매 수수료·조정 금액의 근거 항목이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 정산 항목 ID |
| `settlement_id` | BIGINT | FK → SETTLEMENTS.id, NOT NULL | 대상 정산 |
| `item_type` | VARCHAR(30) | NOT NULL, CHECK | 항목 유형. 아래 표 참조 |
| `source_type` | VARCHAR(30) | NULL | 원천 테이블 또는 업무 유형 |
| `source_id` | BIGINT | NULL | 원천 데이터 ID |
| `amount` | NUMERIC(15,2) | NOT NULL | 부호를 포함한 금액 |
| `included_in_remittance` | BOOLEAN | NOT NULL | 클라이언트 송금액 반영 여부 |
| `occurred_at` | TIMESTAMPTZ | NOT NULL | 매출·환불·조정 발생 일시 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `(settlement_id, item_type, source_type, source_id)` | — | UNIQUE | 중복 집계 방지 |

**`item_type` 과 송금 반영 기준**

| `item_type` | `included_in_remittance` | 처리 |
|-|-|-|
| `TICKET_SALE` | TRUE | 티켓 판매원금. 클라이언트 송금액에 더함 |
| `TICKET_REFUND` | TRUE | 티켓 환불원금. 음수 금액으로 차감 |
| `BOOKING_FEE` | FALSE | 예매 수수료. 플랫폼 수익 |
| `BOOKING_FEE_REFUND` | FALSE | 예매 수수료 환불. 플랫폼 수익 환입 |
| `BOOTH_SALE` | TRUE | 결제 완료 부스비. 클라이언트 송금액에 더함 — 복원 (D-4) |
| `ADJUSTMENT` | TRUE | 승인된 정산 조정. `ADD` / `DEDUCT` 반영 |

> 보완본은 이 목록에서 v16 의 `BOOTH_SALE` 과 `PG_FEE_REFERENCE` 를 뺐다. 그중 `BOOTH_SALE` 은
> 되살렸다 — `SETTLEMENTS.gross_booth_sales_amount` 가 살아 있고 `remittance_due_amount` 계산식에도
> 부스 매출이 들어가는데, 근거 항목 유형이 없으면 그 금액의 출처를 남길 수 없다.
> `PG_FEE_REFERENCE` 는 "PG 수수료를 송금액에서 별도 차감하지 않는다"는 보완본 근거가 명확하므로
> 복원하지 않았다 (D-4).

### SETTLEMENT_ADJUSTMENTS

관리자가 정산에 추가하거나 차감하는 수동 조정 금액과 사유다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 조정 ID |
| `settlement_id` | BIGINT | FK → SETTLEMENTS.id, NOT NULL | 대상 정산 |
| `adjustment_type` | VARCHAR(30) | NOT NULL, CHECK | `ADD`, `DEDUCT` |
| `amount` | NUMERIC(15,2) | NOT NULL, CHECK > 0 | 조정 금액 |
| `reason` | TEXT | NOT NULL | 조정 사유 |
| `created_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 등록 관리자 |
| `approved_by_admin_id` | BIGINT | FK → USERS.id, NULL | 승인 관리자 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `DRAFT`, `APPROVED`, `CANCELED` |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **처리 규칙**
> - `DRAFT` 상태는 송금 예정 금액에 반영하지 않는다.
> - `APPROVED` 가 되면 대응하는 `SETTLEMENT_ITEMS` 의 `ADJUSTMENT` 항목을 생성한다.
> - `ADD` 는 양수, `DEDUCT` 는 음수로 정산 항목에 기록한다.
> - 확정된 정산을 직접 수정하지 않고 조정 이력으로 남긴다.

### REMITTANCES

정산 확정 후 외부 송금의 예정·결과를 기록한다. 은행 자동 송금은 MVP 에 포함하지 않는다.

> ⚠ **타입은 추정이다.** v16 이 컬럼 8개의 이름과 의미를 모두 제시했고 타입만 없었다.
> 문서 공통 규칙(금액 `NUMERIC(15,2)`, 시각 `TIMESTAMPTZ`, 상태 `VARCHAR + CHECK`)을 적용했다.
> → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 컬럼명 | 타입 | 제약조건 | 설명 | 출처 |
|-|-|-|-|-|
| `id` | BIGSERIAL | PK | 송금 ID | 추정 |
| `settlement_id` | BIGINT | FK → SETTLEMENTS.id, NOT NULL | 정산 헤더 | 보완 |
| `scheduled_at` | TIMESTAMPTZ | NULL | 예정 송금일 | 보완 |
| `remitted_amount` | NUMERIC(15,2) | NULL, CHECK >= 0 | 외부에서 실제 송금한 금액 | 보완 |
| `remitted_at` | TIMESTAMPTZ | NULL | 실제 송금 일시 | 보완 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `PENDING`, `PROCESSING`, `REMITTED`, `FAILED`, `CANCELED` | 명세 |
| `reference_number` | VARCHAR(100) | NULL | 외부 송금 확인번호 또는 거래 식별값 | 보완 |
| `memo` | TEXT | NULL | 관리자 송금 메모 | 보완 |
| `processed_by` | BIGINT | FK → USERS.id, NULL | 송금 결과를 기록한 관리자 | 보완 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 | 추정 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 | 추정 |

> `settlement_id` 에 UNIQUE 를 걸지 않았다. 상태값에 `FAILED` 가 있으니 송금 재시도로 여러 건이
> 쌓일 수 있어야 한다.

### SETTLEMENT_REPORTS

정산 리포트의 PDF·엑셀 파일과 버전별 생성 이력을 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 정산 리포트 ID |
| `settlement_id` | BIGINT | FK → SETTLEMENTS.id, NOT NULL | 대상 정산 |
| `file_id` | BIGINT | FK → FILE_METADATA.id, NOT NULL | 생성된 리포트 파일 |
| `format` | VARCHAR(20) | NOT NULL, CHECK | `PDF`, `XLSX` |
| `report_version` | INTEGER | NOT NULL DEFAULT 1, CHECK > 0 | 리포트 버전 |
| `generated_by_admin_id` | BIGINT | FK → USERS.id, NULL | 생성 관리자 또는 NULL |
| `generated_at` | TIMESTAMPTZ | NOT NULL | 생성 일시 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 레코드 생성 일시 |
| `(settlement_id, format, report_version)` | — | UNIQUE | 버전 중복 방지 |

> **처리 규칙**
> - 정산 금액이 변경되어 리포트를 다시 만들 때 기존 파일을 덮어쓰지 않고 `report_version` 을 증가시킨다.
> - 리포트 파일은 `FILE_METADATA` 를 통해 관리한다.
> - 클라이언트는 본인 박람회의 정산 리포트만 조회·다운로드할 수 있다.
> - 관리자 정산 확정 전에는 임시 리포트를 제공하거나 다운로드를 제한할 수 있다.
>
> 이전 설계의 `stored_file_id` 는 프로젝트의 파일 FK 명명 방식에 맞춰 `file_id` 로 통일한 보완안이다 (D-4).

### EXPO_DAILY_SALES_SUMMARIES

박람회별 일자 매출을 미리 집계하여 클라이언트 대시보드 조회 성능을 높이는 파생 테이블이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 일자별 집계 ID |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 대상 박람회 |
| `sales_date` | DATE | NOT NULL | 집계 기준일 |
| `paid_order_count` | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 결제 완료 주문 수 |
| `canceled_order_count` | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 전체 취소 주문 수 |
| `sold_ticket_quantity` | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 판매 티켓 수 |
| `refund_ticket_quantity` | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 환불 티켓 수 |
| `ticket_sales_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 티켓 판매원금 |
| `booking_fee_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 구매자가 결제한 3% 예매 수수료 |
| `refund_ticket_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 환불된 티켓 원금 |
| `refund_booking_fee_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 환불된 예매 수수료 |
| `buyer_payment_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 구매자 실결제 합계 |
| `client_settlement_base_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 클라이언트 정산 기준 티켓 매출 |
| `calculated_at` | TIMESTAMPTZ | NOT NULL | 마지막 집계 시각 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |
| `(expo_id, sales_date)` | — | UNIQUE | 일자 중복 집계 방지 |

> **계산식**
> ```
> buyer_payment_amount
>   = ticket_sales_amount + booking_fee_amount
>     - refund_ticket_amount - refund_booking_fee_amount
>
> client_settlement_base_amount
>   = ticket_sales_amount - refund_ticket_amount
> ```
> 예매 수수료는 플랫폼 수익이므로 `client_settlement_base_amount` 에 포함하지 않는다.
>
> 보완본은 이 테이블의 금액 필드가 원본 ERD 에 없던 보완안이라고 밝히고 있다. 원본에는 주문 수와
> 티켓 수량 필드까지만 있었다 (D-4).

> **처리 흐름** — 행사 종료 → 정산 대상 자동 생성 → 금액 자동 계산 → 관리자 검토 → 필요 시 조정 →
> 정산 확정 → 외부 송금 → 송금 결과 기록 → 클라이언트 조회 및 리포트 다운로드

---

## 6-11. 광고 배너

클라이언트의 배너 신청, 관리자 승인·반려, 승인 후 실제 배너 자동 생성과 예약·노출·종료 상태를 관리한다.
MVP 에서는 광고비 결제를 제외한다.

**엔티티** — `BANNER_SLOTS`, `BANNER_APPLICATIONS`, `BANNERS`, `BANNER_REVIEW_HISTORIES`

### BANNER_SLOTS

메인 화면 등 배너 노출 위치와 최대 동시 노출 수를 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 배너 슬롯 ID |
| `slot_code` | VARCHAR(50) | UNIQUE, NOT NULL | `MAIN_TOP` 등 |
| `name` | VARCHAR(100) | NOT NULL | 슬롯명 |
| `max_active_count` | INTEGER | NOT NULL, CHECK > 0 | 동시 노출 가능 수 |
| `width_px` | INTEGER | NULL | 권장 너비 |
| `height_px` | INTEGER | NULL | 권장 높이 |
| `active` | BOOLEAN | NOT NULL DEFAULT TRUE | 사용 여부 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### BANNER_APPLICATIONS

클라이언트의 배너 신청과 심사 상태.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 배너 신청 ID |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 신청 클라이언트 |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 연결 박람회 |
| `image_file_id` | BIGINT | FK → FILE_METADATA.id, NOT NULL | 배너 이미지 |
| `headline` | VARCHAR(150) | NULL | 배너 문구 |
| `requested_start_at` | TIMESTAMPTZ | NOT NULL | 희망 시작 |
| `requested_end_at` | TIMESTAMPTZ | NOT NULL | 희망 종료 |
| `review_status` | VARCHAR(20) | NOT NULL, CHECK | `DRAFT`, `UNDER_REVIEW`, `REJECTED`, `APPROVED`, `CANCELED` — 원본 32p 표 이탈 복원 (D-1) |
| `submitted_at` | TIMESTAMPTZ | NULL | 신청 제출 |
| `reviewed_by_admin_id` | BIGINT | FK → USERS.id, NULL | 심사 관리자 |
| `reviewed_at` | TIMESTAMPTZ | NULL | 심사 일시 |
| `rejection_reason` | TEXT | NULL | 반려 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### BANNERS

승인과 동시에 자동 생성되는 실제 노출 배너.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 배너 ID |
| `banner_application_id` | BIGINT | UNIQUE, FK → BANNER_APPLICATIONS.id, NOT NULL | 승인 신청 1:1 |
| `banner_slot_id` | BIGINT | FK → BANNER_SLOTS.id, NOT NULL | 노출 슬롯 |
| `expo_id` | BIGINT | FK → EXPOS.id, NOT NULL | 랜딩 박람회 |
| `image_file_id` | BIGINT | FK → FILE_METADATA.id, NOT NULL | 실제 이미지 |
| `headline` | VARCHAR(150) | NULL | 노출 문구 |
| `start_at` | TIMESTAMPTZ | NOT NULL | 노출 시작 |
| `end_at` | TIMESTAMPTZ | NOT NULL | 노출 종료 |
| `display_status` | VARCHAR(20) | NOT NULL, CHECK | `SCHEDULED`, `ACTIVE`, `ENDED`, `CANCELED` |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 좌→우 노출 순서 |
| `activated_at` | TIMESTAMPTZ | NULL | 활성화 일시 |
| `ended_at` | TIMESTAMPTZ | NULL | 종료 처리 일시 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### BANNER_REVIEW_HISTORIES

배너 신청 승인·반려 이력.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 심사 이력 ID |
| `banner_application_id` | BIGINT | FK → BANNER_APPLICATIONS.id, NOT NULL | 신청 |
| `reviewer_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 관리자 |
| `decision` | VARCHAR(20) | NOT NULL, CHECK | `SUBMIT`, `APPROVE`, `REJECT`, `CANCEL` — CHECK 보정 (D-2) |
| `reason` | TEXT | NULL | 사유 |
| `from_status` | VARCHAR(20) | NULL | 변경 전 |
| `to_status` | VARCHAR(20) | NOT NULL | 변경 후 |
| `reviewed_at` | TIMESTAMPTZ | NOT NULL | 처리 일시 |

---

## 6-12. 관리자 대시보드 및 운영

박람회·배너 심사, 모집공고 생성 요청 및 장소 충돌 검토, 참여 신청 운영 확인, 결제·환불·정산 상태를 통합 조회한다.

**엔티티** — SQL View `V_ADMIN_DASHBOARD_COUNTS`, `V_ADMIN_PENDING_REVIEWS`, `V_ADMIN_VENUE_CONFLICTS`, `V_ADMIN_PARTICIPATION_OPERATIONS`, `V_ADMIN_PAYMENT_REFUND_STATUS`, `V_ADMIN_SETTLEMENT_STATUS`

원본이 출력 필드를 정의한 것은 `V_ADMIN_PARTICIPATION_OPERATIONS` 와 `V_ADMIN_VENUE_CONFLICTS` 둘뿐이다.
나머지 넷은 이름만 있어 필드를 추정했다.

### V_ADMIN_DASHBOARD_COUNTS

관리자 대시보드 상단의 처리 대기 건수 집계. **단일 행**을 반환한다.

> ⚠ **출력 필드는 추정이다.** 6-12 절 서두가 나열한 관리자 업무(박람회·배너 심사, 모집공고 생성 요청
> 및 장소 충돌 검토, 참여 신청 운영 확인, 정산 상태)를 각각 건수 필드로 옮겼다.
> → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `pending_expo_review_count` | `EXPO_OPENING_REQUESTS` 중 `SUBMITTED`·`UNDER_REVIEW` | 추정 |
| `pending_banner_review_count` | `BANNER_APPLICATIONS` 중 `UNDER_REVIEW` | 추정 |
| `pending_notice_request_count` | `RECRUITMENT_NOTICE_REQUESTS` 중 `SUBMITTED`·`UNDER_REVIEW` | 추정 |
| `venue_conflict_count` | `RECRUITMENT_NOTICE_REQUESTS` 중 `venue_conflict_status='CONFLICT_PENDING'` | 추정 |
| `unchecked_application_count` | `PARTICIPATION_APPLICATIONS` 중 `SUBMITTED` 이고 `admin_checked_at IS NULL` | 추정 |
| `pending_change_request_count` | `EXPO_CHANGE_REQUESTS` 중 `SUBMITTED`·`UNDER_REVIEW` | 추정 |
| `pending_cancellation_request_count` | `EXPO_CANCELLATION_REQUESTS` 중 `SUBMITTED` | 추정 |
| `settlement_waiting_count` | `SETTLEMENTS` 중 `WAITING`·`CALCULATED`·`UNDER_REVIEW` | 추정 |

### V_ADMIN_PENDING_REVIEWS

심사 대기 목록. 박람회 개최 신청·배너 신청·모집공고 생성 요청 세 원천을 하나로 합친다.

> ⚠ **출력 필드와 구조 모두 추정이다.** 6-12 절이 "박람회·배너 심사, 모집공고 생성 요청 …을 통합
> 조회한다"고만 적고 있다. 세 원천을 `UNION ALL` 로 합치는 구조를 가정했다. **이 문서에서 가장 큰
> 추정 중 하나다** (D-4).

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `review_target_type` | 리터럴 `EXPO_OPENING` / `BANNER` / `NOTICE_REQUEST` | 추정 |
| `target_id` | 각 원천의 `id` | 추정 |
| `title` | 각 원천의 `title` | 추정 |
| `requester_client_id` | `host_client_id` 또는 `client_user_id` | 추정 |
| `status` | 각 원천의 심사 상태 | 추정 |
| `submitted_at` | 각 원천의 `submitted_at` | 추정 |
| `created_at` | 각 원천의 `created_at` | 추정 |

### V_ADMIN_PAYMENT_REFUND_STATUS

티켓·부스 결제와 환불 상태 통합 조회.

> ⚠ **출력 필드와 구조 모두 추정이다.** 6-12 절의 "결제·환불·정산 상태를 통합 조회한다"는 서술이
> 전부다. 티켓 결제와 부스 결제를 `UNION ALL` 로 합치는 구조를 가정했다. 부스는 환불을 지원하지
> 않으므로 부스 쪽 환불 필드는 항상 NULL 이다 (D-4).

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `payment_domain` | 리터럴 `TICKET` / `BOOTH` | 추정 |
| `order_id`, `order_number` | `TICKET_ORDERS` 또는 `BOOTH_ORDERS` | 추정 |
| `payer_user_id` | `TICKET_ORDERS.member_user_id` 또는 `BOOTH_ORDERS.client_user_id` | 추정 |
| `expo_id` | 주문에서 거슬러 올라간 박람회 | 추정 |
| `payment_id`, `payment_status` | `TICKET_PAYMENTS` 또는 `BOOTH_PAYMENTS` | 추정 |
| `requested_amount`, `approved_amount`, `approved_at` | 위와 동일 | 추정 |
| `refund_id`, `refund_status`, `refund_amount`, `refund_completed_at` | `TICKET_REFUNDS`. 부스는 NULL | 추정 |

### V_ADMIN_SETTLEMENT_STATUS

정산과 송금 상태 통합 조회.

> ⚠ **출력 필드는 추정이다.** `V_CLIENT_DASHBOARD_SETTLEMENTS` 의 필수 출력 필드에서 클라이언트
> 전용 항목을 덜어내고 관리자에게 필요한 확정·송금 정보를 남기는 구성으로 잡았다.

| 출력 필드 | 원천 / 계산 | 출처 |
|-|-|-|
| `settlement_id`, `status` | `SETTLEMENTS` | 추정 |
| `expo_id`, `expo_title`, `event_end_at` | `EXPOS` | 추정 |
| `host_client_id`, `company_name` | `CLIENT_PROFILES` | 추정 |
| `settlement_due_at` | `SETTLEMENTS.settlement_due_at` | 추정 |
| `remittance_due_amount`, `adjustment_amount` | `SETTLEMENTS` | 추정 |
| `confirmed_at`, `confirmed_by` | `SETTLEMENTS` | 추정 |
| `remitted_amount`, `remitted_at`, `remittance_status` | 최신 `REMITTANCES` | 추정 |

---

원본이 출력 필드를 정의한 아래 둘은 명세 그대로다.

### V_ADMIN_PARTICIPATION_OPERATIONS

| 출력 필드 | 원천/계산 |
|-|-|
| `application_id` | `PARTICIPATION_APPLICATIONS.id` |
| `recruitment_notice_id` | `PARTICIPATION_APPLICATIONS.recruitment_notice_id` |
| `client_user_id` / `company_name` | `PARTICIPATION_APPLICATIONS.client_user_id` / `company_name_snapshot` |
| `application_status` | `PARTICIPATION_APPLICATIONS.status` |
| `selected_booth_product_id` | `PARTICIPATION_APPLICATIONS.selected_booth_product_id` |
| `booth_order_id` / `booth_order_status` | `BOOTH_ORDERS.id` / `status` |
| `payment_status` | 최신 `BOOTH_PAYMENTS.status` |
| `booth_allocation_id` / `allocation_status` | `BOOTH_ALLOCATIONS.id` / `status` |
| `admin_checked_at` / `admin_checked_by` | `PARTICIPATION_APPLICATIONS` 운영 확인 컬럼 |
| `latest_operation_type` / `message` | 최신 `APPLICATION_OPERATION_HISTORIES` |
| `created_at` / `submitted_at` | `PARTICIPATION_APPLICATIONS` |

### V_ADMIN_VENUE_CONFLICTS

| 출력 필드 | 원천/계산 | 용도 |
|-|-|-|
| `request_id` | `RECRUITMENT_NOTICE_REQUESTS.id` | 요청 식별 |
| `host_client_id` | `RECRUITMENT_NOTICE_REQUESTS.host_client_id` | 요청 주최자 |
| `virtual_venue_id` / `hall_id` / `zone_id` | 요청 장소 필드 | 충돌 공간 |
| `use_start_at` / `use_end_at` | 요청 사용 기간 | 충돌 기간 |
| `submitted_at` | 요청 제출 시각 | 선착순 정렬 기준 |
| `conflict_group_key` | 서버 계산 | 동일 충돌군 묶기 |
| `venue_decision` | `PENDING` / `ALLOWED` / `CANCELED` | 운영자 결정 |
| `decided_by_admin_id` / `decided_at` | 운영 결정 정보 | 감사 추적 |
| `cancellation_reason` | 운영자 입력 | 취소 사유 |

> **장소 중복 운영 규칙**
> - 중복 요청은 등록 단계에서 자동 삭제하지 않고 모두 `SUBMITTED` 상태로 저장한다.
> - 시스템은 같은 장소·홀·구역에서 사용 기간이 겹치는 요청을 하나의 충돌군으로 표시한다.
> - 관리자 화면은 `submitted_at` 오름차순으로 정렬하여 먼저 제출된 요청을 최상단에 표시한다.
> - 운영자는 원칙적으로 먼저 제출된 요청을 `ALLOWED` 처리하고, 동일 충돌군의 나머지 요청을 `CANCELED` 처리한다.
> - 운영상 예외가 필요한 경우에도 운영자가 직접 대상을 선택할 수 있으며, 결정 사유를 필수 기록한다.
> - `ALLOWED` 처리 시 `VENUE_RESERVATIONS` 를 생성한다. 유효 예약끼리는 DB `EXCLUDE` 제약으로 겹침을 금지한다.

---

## 6-13. 기업 모집공고 및 참여 신청

주최 클라이언트가 모집공고 생성을 요청하고, 운영자가 장소 중복을 검토하여 허용된 요청을 바탕으로 공고를
작성·게시한다. 참여 기업은 공고당 부스 1개를 선택하여 결제하며, 결제 성공 즉시 신청 완료와 부스 확정
배정이 이루어진다.

**엔티티** — `RECRUITMENT_NOTICE_REQUESTS`, `RECRUITMENT_NOTICE_REQUEST_HISTORIES`, `VENUE_RESERVATIONS`, `RECRUITMENT_NOTICES`, `RECRUITMENT_NOTICE_HISTORIES`, `PARTICIPATION_APPLICATIONS`, `APPLICATION_OPERATION_HISTORIES`, `RECRUITMENT_RESULTS`, `RECRUITMENT_RESULT_ITEMS`

### RECRUITMENT_NOTICE_REQUESTS

주최 클라이언트의 모집공고 생성 요청과 장소 중복 운영 결정을 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 요청 ID |
| `host_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 주최 클라이언트 |
| `title` | VARCHAR(255) | NOT NULL | 요청 제목 |
| `description` | TEXT | NOT NULL | 행사·모집 개요 |
| `application_start_at` | TIMESTAMPTZ | NOT NULL | 모집 시작 |
| `application_end_at` | TIMESTAMPTZ | NOT NULL | 모집 종료 |
| `event_start_at` | TIMESTAMPTZ | NOT NULL | 행사 시작 |
| `event_end_at` | TIMESTAMPTZ | NOT NULL | 행사 종료 |
| `virtual_venue_id` | BIGINT | FK → VIRTUAL_VENUES.id, NOT NULL | 요청 장소 |
| `venue_hall_id` | BIGINT | FK → VENUE_HALLS.id, NULL | 요청 홀 |
| `venue_zone_id` | BIGINT | FK → VENUE_ZONES.id, NULL | 요청 구역 |
| `target_company_count` | INTEGER | NULL, CHECK > 0 | 목표 기업 수 |
| `requested_booth_config` | JSONB | NULL | 요청 부스 구성 |
| `status` | VARCHAR(30) | CHECK | `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `CANCELED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `venue_conflict_status` | VARCHAR(30) | CHECK | `CLEAR`, `CONFLICT_PENDING`, `RESOLVED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `venue_decision` | VARCHAR(20) | CHECK | `PENDING`, `ALLOWED`, `CANCELED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `conflict_group_key` | VARCHAR(100) | NULL, INDEX | 충돌 요청 묶음 키 |
| `submitted_at` | TIMESTAMPTZ | NULL | 제출 시각. 선착순 기준 |
| `decided_by_admin_id` | BIGINT | FK → USERS.id, NULL | 장소 결정 관리자 |
| `decided_at` | TIMESTAMPTZ | NULL | 장소 결정 시각 |
| `decision_reason` | TEXT | NULL | 허용·취소 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |

### RECRUITMENT_NOTICE_REQUEST_HISTORIES

요청 제출, 검토, 장소 허용·취소 및 공고 생성 연결 이력을 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 이력 ID |
| `request_id` | BIGINT | FK → RECRUITMENT_NOTICE_REQUESTS.id, NOT NULL | 대상 요청 |
| `action_type` | VARCHAR(30) | CHECK | `SUBMIT`, `REVIEW_START`, `VENUE_ALLOW`, `VENUE_CANCEL`, `APPROVE`, `REJECT`, `NOTICE_CREATED` |
| `from_status` | VARCHAR(30) | NULL | 변경 전 |
| `to_status` | VARCHAR(30) | NOT NULL | 변경 후 |
| `reason` | TEXT | NULL | 처리 사유 |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NULL | 처리 관리자 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 처리 시각 |

### VENUE_RESERVATIONS

모집공고 경로와 일반 박람회 등록 경로를 모두 포함하는 **단일 장소 예약 원본**이다. 모든 확정 장소·기간은
이 테이블에 먼저 저장하며, 기간 중복 제약도 이 테이블에서만 적용한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 장소 예약 ID |
| `reservation_source_type` | VARCHAR(30) | CHECK | `RECRUITMENT_NOTICE`, `EXPO_DIRECT` — 원본에 NOT NULL 표기 없음 (D-4) |
| `notice_request_id` | BIGINT | UNIQUE, FK → RECRUITMENT_NOTICE_REQUESTS.id, NULL | 모집공고 경로 요청 |
| `opening_request_id` | BIGINT | UNIQUE, FK → EXPO_OPENING_REQUESTS.id, NULL | 일반 박람회 등록 경로 요청 |
| `recruitment_notice_id` | BIGINT | FK → RECRUITMENT_NOTICES.id, NULL | 생성 공고 |
| `virtual_venue_id` | BIGINT | FK → VIRTUAL_VENUES.id, NOT NULL | 장소 |
| `venue_hall_id` | BIGINT | FK → VENUE_HALLS.id, NULL | 홀 |
| `venue_zone_id` | BIGINT | FK → VENUE_ZONES.id, NULL | 구역 |
| `use_start_at` | TIMESTAMPTZ | NOT NULL | 사용 시작 |
| `use_end_at` | TIMESTAMPTZ | NOT NULL | 사용 종료 |
| `status` | VARCHAR(20) | CHECK | `CONFIRMED`, `RELEASED`, `CANCELED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `confirmed_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 확정 관리자 |
| `confirmed_at` | TIMESTAMPTZ | NOT NULL | 확정 시각 |
| `released_at` | TIMESTAMPTZ | NULL | 해제 시각 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |
| `예약 원천 제약` | — | 둘 중 정확히 하나만 NOT NULL | `notice_request_id` 또는 `opening_request_id` 중 하나만 사용 |
| `기간 중복 제약` | — | `EXCLUDE USING gist` (동일 장소/홀/구역 + `tstzrange` 겹침) `WHERE status = CONFIRMED` | 모든 경로의 확정 예약 중복 방지 |

### RECRUITMENT_NOTICES

허용된 장소 요청을 기준으로 관리자가 작성·게시하는 기업 모집공고.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 공고 ID |
| `request_id` | BIGINT | UNIQUE, FK → RECRUITMENT_NOTICE_REQUESTS.id, NOT NULL | 원본 요청 |
| `host_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 담당 주최 클라이언트 |
| `venue_reservation_id` | BIGINT | UNIQUE, FK → VENUE_RESERVATIONS.id, NOT NULL | 확정 장소 예약 |
| `title` | VARCHAR(255) | NOT NULL | 공고 제목 |
| `content` | TEXT | NOT NULL | 공고 내용 |
| `eligibility` | TEXT | NULL | 참여 조건 |
| `submission_requirements` | JSONB | NULL | 제출 자료 |
| `application_start_at` | TIMESTAMPTZ | NOT NULL | 신청 시작 |
| `application_end_at` | TIMESTAMPTZ | NOT NULL | 신청 종료 |
| `status` | VARCHAR(20) | CHECK | `DRAFT`, `SCHEDULED`, `OPEN`, `CLOSED`, `CANCELED`, `ARCHIVED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `published_at` | TIMESTAMPTZ | NULL | 게시 시각 |
| `closed_at` | TIMESTAMPTZ | NULL | 마감 시각 |
| `created_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 작성 관리자 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |

### RECRUITMENT_NOTICE_HISTORIES

공고 작성·게시·수정·마감·취소 이력.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 이력 ID |
| `recruitment_notice_id` | BIGINT | FK → RECRUITMENT_NOTICES.id, NOT NULL | 대상 공고 |
| `action_type` | VARCHAR(20) | CHECK | `CREATE`, `PUBLISH`, `UPDATE`, `CLOSE`, `CANCEL`, `ARCHIVE` |
| `before_data` | JSONB | NULL | 변경 전 |
| `after_data` | JSONB | NULL | 변경 후 |
| `reason` | TEXT | NULL | 사유 |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 관리자 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 처리 시각 |

### PARTICIPATION_APPLICATIONS

참여 기업의 신청서와 선택 부스를 저장한다. 하나의 기업은 공고 1건에서 부스 1개만 선택할 수 있다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 참여 신청 ID |
| `recruitment_notice_id` | BIGINT | FK → RECRUITMENT_NOTICES.id, NOT NULL | 공고 |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 참여 기업 |
| `company_name_snapshot` | VARCHAR(150) | NOT NULL | 신청 당시 기업명 |
| `participation_purpose` | TEXT | NULL | 참여 목적 |
| `exhibit_description` | TEXT | NULL | 전시 내용 |
| `selected_booth_product_id` | BIGINT | FK → BOOTH_PRODUCTS.id, NULL | 선택 부스 상품 1개 |
| `booth_order_id` | BIGINT | UNIQUE, FK → BOOTH_ORDERS.id, NULL | 부스 주문 |
| `status` | VARCHAR(30) | CHECK | `DRAFT`, `PAYMENT_PENDING`, `SUBMITTED`, `PAYMENT_FAILED`, `CANCELED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `submitted_at` | TIMESTAMPTZ | NULL | 결제 성공 신청 완료 시각 |
| `admin_checked_at` | TIMESTAMPTZ | NULL | 운영 확인 시각 |
| `admin_checked_by` | BIGINT | FK → USERS.id, NULL | 운영 확인 관리자 |
| `admin_memo` | TEXT | NULL | 운영 메모 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |
| `공고별 기업 제약` | — | `UNIQUE(recruitment_notice_id, client_user_id)` | 기업당 공고 1회 신청 |
| `부스 단수 제약` | — | `selected_booth_product_id` 단일 FK | 공고당 부스 1개만 선택 |

### APPLICATION_OPERATION_HISTORIES

참여 신청 승인·반려 이력이 아니라 **운영 확인과 보완 요청 이력**을 저장한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 운영 이력 ID |
| `application_id` | BIGINT | FK → PARTICIPATION_APPLICATIONS.id, NOT NULL | 대상 신청 |
| `action_type` | VARCHAR(30) | CHECK | `CHECKED`, `CORRECTION_REQUESTED`, `CORRECTION_COMPLETED`, `MEMO_UPDATED` |
| `message` | TEXT | NULL | 운영 메모·보완 내용 |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 처리 관리자 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 처리 시각 |

### RECRUITMENT_RESULTS

모집 마감 후 결제 완료 및 부스 확정 기업을 집계한 결과 스냅샷.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 모집 결과 ID |
| `recruitment_notice_id` | BIGINT | UNIQUE, FK → RECRUITMENT_NOTICES.id, NOT NULL | 대상 공고 |
| `host_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 결과 수신 주최자 |
| `confirmed_company_count` | INTEGER | NOT NULL DEFAULT 0 | 확정 기업 수 |
| `confirmed_booth_count` | INTEGER | NOT NULL DEFAULT 0 | 확정 부스 수 |
| `total_booth_sales_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 부스 매출 합계 |
| `status` | VARCHAR(20) | CHECK | `GENERATED`, `DELIVERED`, `CONFIRMED`, `USED_FOR_EXPO`, `CANCELED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `generated_at` | TIMESTAMPTZ | NOT NULL | 생성 시각 |
| `delivered_at` | TIMESTAMPTZ | NULL | 주최자 전달 시각 |
| `confirmed_by_host_at` | TIMESTAMPTZ | NULL | 주최자 확인 시각 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |

### RECRUITMENT_RESULT_ITEMS

모집 결과에 포함되는 결제·배정 완료 기업별 항목.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 결과 항목 ID |
| `recruitment_result_id` | BIGINT | FK → RECRUITMENT_RESULTS.id, NOT NULL | 모집 결과 |
| `application_id` | BIGINT | UNIQUE, FK → PARTICIPATION_APPLICATIONS.id, NOT NULL | 참여 신청 |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 참여 기업 |
| `booth_allocation_id` | BIGINT | UNIQUE, FK → BOOTH_ALLOCATIONS.id, NOT NULL | 확정 부스 |
| `booth_amount` | NUMERIC(15,2) | NOT NULL | 결제 부스비 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |

---

## 6-14. 가상 장소 및 부스 프리뷰

v16 본문은 "기존 `VIRTUAL_VENUES`, `VENUE_HALLS`, `VENUE_ZONES`, `BOOTHS`, `BOOTH_TEMPLATES`,
`BOOTH_PRODUCTS` 정의를 유지합니다" 한 줄만 남기고 컬럼 정의를 싣지 않았다. 아래 정의는 보완본
`erd수정본.pdf` 에서 가져왔다. 보완본은 누락 원인을 이렇게 적고 있다 — "이전 ERD에서 명확하게
정의되어 있었으나 v16에서 '기존 정의 유지' 문구만 남으면서 본문에서 빠진 것".

장소의 기간 중복은 이 세 장소 테이블에서 관리하지 않는다. 단일 기준은 `VENUE_RESERVATIONS` 다.

### VIRTUAL_VENUES

플랫폼이 보유한 행사장·전시장 원본이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 가상 장소 ID |
| `name` | VARCHAR(150) | UNIQUE, NOT NULL | 장소명 |
| `address` | VARCHAR(255) | NOT NULL | 장소 주소 |
| `region_code` | VARCHAR(30) | NOT NULL | 지역 검색 코드 |
| `description` | TEXT | NULL | 장소 설명 |
| `map_file_id` | BIGINT | FK → FILE_METADATA.id, NULL | 전체 배치도 파일 |
| `operational_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### VENUE_HALLS

가상 장소 안의 전시장·홀 단위다.

> ⚠ **이 절의 컬럼 정의는 명세가 아니라 추정이다.** 보완본에 제목·설명과 `UNIQUE (venue_id, hall_code)`
> 제약은 있지만 컬럼 표 자리에 `VIRTUAL_VENUES` 의 표가 들어가 있었다. 상위 `VIRTUAL_VENUES` 와
> 하위 `VENUE_ZONES` 의 대칭 구조를 근거로 채웠다. 컬럼별 출처는 아래 표 참조. → [부록 E](#부록-e-추정-컬럼-전수-목록)

| 컬럼명 | 타입 | 제약조건 | 설명 | 출처 |
|-|-|-|-|-|
| `id` | BIGSERIAL | PK | 홀 ID | 추정 |
| `venue_id` | BIGINT | FK → VIRTUAL_VENUES.id, NOT NULL | 상위 장소 | 보완 |
| `hall_code` | VARCHAR(30) | NOT NULL | 홀 코드 | 보완 |
| `name` | VARCHAR(100) | NOT NULL | 홀명 | 추정 |
| `width` | NUMERIC(10,2) | NULL, CHECK > 0 | 홀 가로 크기 | 추정 |
| `depth` | NUMERIC(10,2) | NULL, CHECK > 0 | 홀 세로 크기 | 추정 |
| `layout_file_id` | BIGINT | FK → FILE_METADATA.id, NULL | 홀 배치도 | 추정 |
| `operational_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `INACTIVE` | 추정 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 | 추정 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 | 추정 |
| `(venue_id, hall_code)` | — | UNIQUE | 장소 내 홀 코드 중복 방지 | 명세 |

`venue_id` 와 `hall_code` 는 UNIQUE 제약에 이름이 박혀 있어 존재는 확실하고 타입만 추정이다.

### VENUE_ZONES

홀 안에서 부스가 배치되는 구역이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 구역 ID |
| `hall_id` | BIGINT | FK → VENUE_HALLS.id, NOT NULL | 상위 홀 |
| `zone_code` | VARCHAR(30) | NOT NULL | 구역 코드 |
| `name` | VARCHAR(100) | NOT NULL | 구역명 |
| `max_booth_count` | INTEGER | NOT NULL, CHECK >= 0 | 최대 부스 수 |
| `width` | NUMERIC(10,2) | NULL, CHECK > 0 | 구역 가로 크기 |
| `depth` | NUMERIC(10,2) | NULL, CHECK > 0 | 구역 세로 크기 |
| `layout_file_id` | BIGINT | FK → FILE_METADATA.id, NULL | 구역 배치도 |
| `operational_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |
| `(hall_id, zone_code)` | — | UNIQUE | 홀 내 구역 코드 중복 방지 |

### BOOTH_TEMPLATES

재사용 가능한 부스 형태와 기본 크기·제공 항목을 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 템플릿 ID |
| `shape_code` | VARCHAR(30) | UNIQUE, NOT NULL | `STANDARD`, `CORNER` 등의 형태 코드 |
| `name` | VARCHAR(100) | NOT NULL | 템플릿명 |
| `width` | NUMERIC(8,2) | NOT NULL, CHECK > 0 | 기본 가로 |
| `height` | NUMERIC(8,2) | NULL, CHECK > 0 | 기본 높이 |
| `depth` | NUMERIC(8,2) | NOT NULL, CHECK > 0 | 기본 깊이 |
| `dimension_unit` | VARCHAR(10) | NOT NULL DEFAULT 'M' | 크기 단위 |
| `default_included_items` | JSONB | NULL | 기본 제공 항목 |
| `operational_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### BOOTHS

가상 장소 구역 도면에 존재하는 고정 부스 공간 원본이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 공간 ID |
| `venue_zone_id` | BIGINT | FK → VENUE_ZONES.id, NOT NULL | 소속 구역 |
| `booth_template_id` | BIGINT | FK → BOOTH_TEMPLATES.id, NULL | 형태 템플릿 |
| `booth_number` | VARCHAR(30) | NOT NULL | 예: `A-01` |
| `shape_code` | VARCHAR(30) | NOT NULL | 부스 형태 스냅샷 |
| `width` | NUMERIC(8,2) | NOT NULL, CHECK > 0 | 가로 |
| `height` | NUMERIC(8,2) | NULL, CHECK > 0 | 높이 |
| `depth` | NUMERIC(8,2) | NOT NULL, CHECK > 0 | 깊이 |
| `dimension_unit` | VARCHAR(10) | NOT NULL DEFAULT 'M' | 크기 단위 |
| `position_x` | NUMERIC(10,2) | NULL | 2D 배치 X 좌표 |
| `position_y` | NUMERIC(10,2) | NULL | 2D 배치 Y 좌표 |
| `rotation_degree` | NUMERIC(6,2) | NULL DEFAULT 0 | 회전 각도 |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 프리뷰 표시 순서 |
| `operational_status` | VARCHAR(20) | NOT NULL, CHECK | `ACTIVE`, `INACTIVE` |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |
| `(venue_zone_id, booth_number)` | — | UNIQUE | 구역 내 부스 번호 중복 방지 |

### BOOTH_PRODUCTS

특정 모집공고에서 실제 판매되는 부스 상품이다. `BOOTHS` 는 장소에 고정된 공간이고,
`BOOTH_PRODUCTS` 는 공고별 가격과 판매 상태를 가진 상품이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 상품 ID |
| `recruitment_notice_id` | BIGINT | FK → RECRUITMENT_NOTICES.id, NOT NULL | 소속 모집공고 |
| `booth_id` | BIGINT | FK → BOOTHS.id, NOT NULL | 고정 부스 원본 |
| `supply_price` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 공급가액 |
| `vat_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0, CHECK >= 0 | 부가세 |
| `total_price` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 최종 결제 금액 |
| `vat_included` | BOOLEAN | NOT NULL DEFAULT TRUE | 부가세 포함 여부 |
| `included_items` | JSONB | NULL | 제공 항목 스냅샷 |
| `sales_start_at` | TIMESTAMPTZ | NULL | 판매 시작 |
| `sales_end_at` | TIMESTAMPTZ | NULL | 판매 종료 |
| `payment_enabled` | BOOLEAN | NOT NULL DEFAULT TRUE | 결제 가능 여부 |
| `sales_status` | VARCHAR(20) | NOT NULL, CHECK | 아래 표 참조 |
| `version` | BIGINT | NOT NULL DEFAULT 0 | 동시 결제 방지용 낙관적 락 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |
| `(recruitment_notice_id, booth_id)` | — | UNIQUE | 공고 내 부스 중복 등록 방지 |

**`sales_status`**

| 값 | 의미 |
|-|-|
| `AVAILABLE` | 판매 가능 |
| `RESERVED` | 다른 사용자가 결제 진행 중 |
| `SOLD` | 결제 완료 및 배정 완료 |
| `UNAVAILABLE` | 운영상 판매 불가 |
| `CANCELED` | 공고 또는 상품 운영 취소 |

> **금액 검증** — `total_price` = `supply_price` + `vat_amount`
>
> `RESERVED` 는 활성 `BOOTH_RESERVATIONS` 가 있을 때 사용하고, 결제 성공 시 `SOLD`,
> 실패·취소·만료 시 다시 `AVAILABLE` 로 변경한다.

### 장소 예약 단일화 정책

| 등록 경로 | 처리 순서 | 중복 검사 위치 | 결과 |
|-|-|-|-|
| 모집공고 경로 | 요청 제출 → 운영자 허용 → 장소 예약 확정 | `VENUE_RESERVATIONS` | `reservation_source_type = RECRUITMENT_NOTICE` |
| 일반 박람회 등록 | 개최 신청 심사 → 장소 예약 확정 → 박람회 배정 | `VENUE_RESERVATIONS` | `reservation_source_type = EXPO_DIRECT` |
| 박람회 생성 | 확정 예약을 박람회에 연결 | `EXPO_VENUE_ASSIGNMENTS` | `expo_id` 와 `venue_reservation_id` 만 연결 |

> **핵심 원칙** — 장소·홀·구역·사용 기간은 `VENUE_RESERVATIONS` 에 한 번만 저장한다. 모집공고 경로와 일반
> 등록 경로가 모두 같은 테이블을 사용하므로 서로 다른 테이블 사이의 교차 중복 문제가 발생하지 않는다.

**일반 박람회 등록 시 장소 처리**

- 관리자가 일반 박람회 개최 신청을 승인하기 전에 동일 장소·홀·구역과 겹치는 `CONFIRMED` `VENUE_RESERVATIONS` 가 있는지 검사한다.
- 중복이 없으면 `VENUE_RESERVATIONS` 를 `EXPO_DIRECT` 경로로 생성하고 `CONFIRMED` 처리한다.
- 확정 예약 생성이 성공한 뒤 `EXPO_VENUE_ASSIGNMENTS` 를 생성하여 박람회와 예약을 연결한다.
- 예약 생성과 박람회 배정 연결은 하나의 트랜잭션에서 처리한다.
- 동시 요청은 `VENUE_RESERVATIONS` 의 `EXCLUDE USING gist` 제약이 최종적으로 차단한다.

---

## 6-15. 부스 판매·결제·배정·관리

참여 기업은 모집공고 1건에서 부스 상품 1개만 선택한다. 결제 성공 즉시 신청 완료와 부스 확정 배정을
원자적으로 반영하며, 결제 완료 후 취소·환불은 지원하지 않는다.

**엔티티** — `BOOTH_ORDERS`, `BOOTH_RESERVATIONS`, `BOOTH_PAYMENTS`, `BOOTH_PAYMENT_HISTORIES`, `BOOTH_ALLOCATIONS`, `BOOTH_MANAGEMENT_HISTORIES`, `BOOTH_CONTENTS`, `BOOTH_CONTENT_FILES`

> 부스가 1개만 선택되므로 `BOOTH_ORDER_ITEMS` 는 사용하지 않는다. `BOOTH_ORDERS.booth_product_id` 에
> 선택 상품을 직접 연결한다.

v16 본문이 컬럼을 정의한 것은 `BOOTH_ORDERS`, `BOOTH_MANAGEMENT_HISTORIES`, `BOOTH_CONTENTS`,
`BOOTH_CONTENT_FILES` 넷이다. 나머지 넷(`BOOTH_RESERVATIONS`, `BOOTH_PAYMENTS`,
`BOOTH_PAYMENT_HISTORIES`, `BOOTH_ALLOCATIONS`)의 정의는 보완본 `erd수정본.pdf` 에서 가져왔다.

### BOOTH_ORDERS

참여 신청 과정에서 선택한 단일 부스 상품의 주문.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 주문 ID |
| `application_id` | BIGINT | UNIQUE, FK → PARTICIPATION_APPLICATIONS.id, NOT NULL | 참여 신청 |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 결제 기업 |
| `booth_product_id` | BIGINT | FK → BOOTH_PRODUCTS.id, NOT NULL | 선택 부스 1개 |
| `order_number` | VARCHAR(50) | UNIQUE, NOT NULL | 부스 주문번호 |
| `unit_price` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 부스 가격 |
| `total_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 최종 결제액. 단수이므로 `unit_price` 와 동일 |
| `status` | VARCHAR(30) | CHECK | `PENDING_PAYMENT`, `PAYMENT_COMPLETED`, `FAILED`, `CANCELED`, `EXPIRED` — 원본에 NOT NULL 표기 없음 (D-4) |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 결제·임시 확보 만료 |
| `paid_at` | TIMESTAMPTZ | NULL | 결제 성공 |
| `idempotency_key` | VARCHAR(100) | UNIQUE, NOT NULL | 중복 승인 방지 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |

### BOOTH_RESERVATIONS

부스 결제 진행 중 동일 부스가 다른 기업에 판매되지 않도록 일정 시간 임시 확보한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 임시 확보 ID |
| `booth_product_id` | BIGINT | FK → BOOTH_PRODUCTS.id, NOT NULL | 확보한 부스 상품 |
| `booth_order_id` | BIGINT | FK → BOOTH_ORDERS.id, NOT NULL | 부스 주문 |
| `reserved_by_client_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 확보한 클라이언트 |
| `reserved_at` | TIMESTAMPTZ | NOT NULL | 확보 시작 |
| `expires_at` | TIMESTAMPTZ | NOT NULL | 확보 만료 |
| `released_at` | TIMESTAMPTZ | NULL | 확보 해제 일시 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | 아래 표 참조 |
| `active_guard` | BOOLEAN | NOT NULL DEFAULT TRUE | 활성 예약 고유 제약 보조 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

**`status`**

| 값 | 의미 |
|-|-|
| `ACTIVE` | 결제 진행 중 임시 확보 |
| `CONFIRMED` | 결제 성공 후 확정 |
| `EXPIRED` | 결제 시간 만료 |
| `RELEASED` | 실패·취소 등으로 확보 해제 |

> **주요 제약** — 한 부스 상품에 활성 예약은 하나만 존재할 수 있다.
> ```sql
> CREATE UNIQUE INDEX uq_booth_reservations_active
>   ON booth_reservations (booth_product_id)
>   WHERE status = 'ACTIVE';
> ```
> - 결제 성공: `ACTIVE` → `CONFIRMED`
> - 결제 실패 또는 사용자 결제 취소: `ACTIVE` → `RELEASED`
> - 결제 만료: `ACTIVE` → `EXPIRED`
> - 실패·취소·만료 시 `BOOTH_PRODUCTS.sales_status` 를 `AVAILABLE` 로 복구한다.

### BOOTH_PAYMENTS

토스페이먼츠 부스 결제의 현재 상태를 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 결제 ID |
| `booth_order_id` | BIGINT | FK → BOOTH_ORDERS.id, NOT NULL | 결제 대상 주문 |
| `payment_key` | VARCHAR(200) | UNIQUE, NULL | PG 결제 키 |
| `pg_order_id` | VARCHAR(100) | UNIQUE, NOT NULL | PG 전달 주문 ID |
| `method` | VARCHAR(30) | NULL | `CARD`, `MOBILE_PHONE` |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `READY`, `IN_PROGRESS`, `APPROVED`, `CANCELED`, `FAILED` |
| `requested_amount` | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 서버 요청 금액 |
| `approved_amount` | NUMERIC(15,2) | NULL, CHECK >= 0 | PG 승인 금액 |
| `approved_at` | TIMESTAMPTZ | NULL | 승인 일시 |
| `canceled_amount` | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 승인 전 취소 또는 PG 취소 금액 |
| `last_failure_code` | VARCHAR(100) | NULL | 최근 실패 코드 |
| `idempotency_key` | VARCHAR(100) | UNIQUE, NOT NULL | 중복 승인 방지 키 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **처리 규칙**
> - `approved_amount` 는 반드시 `BOOTH_ORDERS.total_amount` 와 같아야 한다.
> - `APPROVED` 성공 시 한 트랜잭션에서 다음을 함께 처리한다 — 참여 신청 `SUBMITTED`,
>   부스 주문 `PAYMENT_COMPLETED`, 임시 확보 `CONFIRMED`, 부스 상품 `SOLD`, `BOOTH_ALLOCATIONS` 생성.
> - `CANCELED` 는 승인 완료 후 환불이 아니라 **승인 전 결제 취소 흐름에만** 사용한다.
> - 승인 완료 후에는 `REFUNDED` 상태와 부스 환불 테이블을 만들지 않는다.

### BOOTH_PAYMENT_HISTORIES

부스 결제 요청·승인·실패·승인 전 취소와 PG 응답을 보존하는 이력이다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 결제 이력 ID |
| `booth_payment_id` | BIGINT | FK → BOOTH_PAYMENTS.id, NOT NULL | 대상 결제 |
| `event_type` | VARCHAR(30) | NOT NULL, CHECK | `REQUEST`, `APPROVE`, `FAIL`, `CANCEL` |
| `from_status` | VARCHAR(20) | NULL | 변경 전 상태 |
| `to_status` | VARCHAR(20) | NOT NULL | 변경 후 상태 |
| `amount` | NUMERIC(15,2) | NULL | 처리 금액 |
| `pg_transaction_key` | VARCHAR(200) | NULL | PG 거래 키 |
| `response_payload` | JSONB | NULL | PG 응답 원문 |
| `occurred_at` | TIMESTAMPTZ | NOT NULL | 발생 일시 |

> **처리 규칙**
> - 이력은 물리 삭제하지 않는다.
> - 승인 완료 부스의 일반 환불을 지원하지 않으므로 `REFUND` 이벤트는 사용하지 않는다.
> - 동일 PG 응답이 중복 처리되지 않도록 `payment_key` 와 `idempotency_key` 를 검증한다.

### BOOTH_ALLOCATIONS

결제 성공 후 선택한 부스를 참여 기업에 확정 배정한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 배정 ID |
| `application_id` | BIGINT | UNIQUE, FK → PARTICIPATION_APPLICATIONS.id, NOT NULL | 참여 신청 |
| `booth_order_id` | BIGINT | UNIQUE, FK → BOOTH_ORDERS.id, NOT NULL | 결제된 부스 주문 — 보완본의 `booth_order_item_id` 를 교체 (D-4) |
| `booth_product_id` | BIGINT | UNIQUE, FK → BOOTH_PRODUCTS.id, NOT NULL | 확정 부스 상품 |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 배정받은 기업 |
| `allocated_at` | TIMESTAMPTZ | NOT NULL | 확정 배정 일시 |
| `status` | VARCHAR(20) | NOT NULL, CHECK | `ASSIGNED`, `CANCELED`, `REASSIGNED` |
| `canceled_at` | TIMESTAMPTZ | NULL | 예외적 배정 취소 일시 |
| `cancel_reason` | TEXT | NULL | 배정 취소 사유 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

> **처리 규칙**
> - 정상 결제 성공 시 `ASSIGNED` 로 생성한다.
> - 사용자 요청에 의한 일반 취소·환불은 허용하지 않는다.
> - `CANCELED`, `REASSIGNED` 는 중복 배정 오류나 운영상 예외 정정에만 사용한다.
> - 한 참여 신청에는 하나의 유효 배정만 존재해야 한다.
> - 한 부스 상품에는 하나의 유효 배정만 존재해야 한다.

### BOOTH_MANAGEMENT_HISTORIES

부스 배정·콘텐츠 운영상 변경 이력을 저장한다. 신청 승인·반려 이력으로 사용하지 않는다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 관리 이력 ID |
| `booth_allocation_id` | BIGINT | FK → BOOTH_ALLOCATIONS.id, NOT NULL | 대상 배정 |
| `booth_content_id` | BIGINT | FK → BOOTH_CONTENTS.id, NULL | 대상 콘텐츠 |
| `action_type` | VARCHAR(40) | CHECK | `ALLOCATION_CORRECTED`, `INFORMATION_UPDATED`, `CORRECTION_REQUESTED`, `CONTENT_HIDDEN`, `CONTENT_RESTORED` |
| `before_data` | JSONB | NULL | 변경 전 |
| `after_data` | JSONB | NULL | 변경 후 |
| `reason` | TEXT | NULL | 사유 |
| `processed_by_admin_id` | BIGINT | FK → USERS.id, NOT NULL | 처리 관리자 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 처리 시각 |

### BOOTH_CONTENTS

확정 배정된 참여 기업이 기업·부스 소개 콘텐츠를 관리한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 부스 콘텐츠 ID |
| `booth_allocation_id` | BIGINT | UNIQUE, FK → BOOTH_ALLOCATIONS.id, NOT NULL | 확정 배정 |
| `client_user_id` | BIGINT | FK → CLIENT_PROFILES.user_id, NOT NULL | 참여 기업 |
| `company_display_name` | VARCHAR(150) | NOT NULL | 노출 기업명 |
| `title` | VARCHAR(200) | NOT NULL | 부스 제목 |
| `company_description` | TEXT | NULL | 기업 소개 |
| `booth_description` | TEXT | NULL | 부스 소개 |
| `product_description` | TEXT | NULL | 전시 제품·서비스 소개 |
| `logo_file_id` | BIGINT | FK → FILE_METADATA.id, NULL | 로고 |
| `main_image_file_id` | BIGINT | FK → FILE_METADATA.id, NULL | 대표 이미지 |
| `status` | VARCHAR(30) | CHECK | `DRAFT`, `PUBLISHED`, `CORRECTION_REQUESTED`, `HIDDEN` — 원본에 NOT NULL 표기 없음 (D-4) |
| `published_at` | TIMESTAMPTZ | NULL | 공개 시각 |
| `correction_requested_at` | TIMESTAMPTZ | NULL | 보완 요청 시각 |
| `correction_message` | TEXT | NULL | 보완 요청 |
| `checked_by_admin_id` | BIGINT | FK → USERS.id, NULL | 운영 확인 관리자 |
| `checked_at` | TIMESTAMPTZ | NULL | 운영 확인 시각 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 수정 |

### BOOTH_CONTENT_FILES

부스 콘텐츠의 갤러리 이미지, 영상, 카탈로그 및 리플렛 파일.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|-|-|-|-|
| `id` | BIGSERIAL | PK | 파일 연결 ID |
| `booth_content_id` | BIGINT | FK → BOOTH_CONTENTS.id, NOT NULL | 부스 콘텐츠 |
| `file_id` | BIGINT | FK → FILE_METADATA.id, NOT NULL | 파일 |
| `file_type` | VARCHAR(30) | CHECK | `GALLERY_IMAGE`, `PROMO_VIDEO`, `CATALOG`, `LEAFLET`, `OTHER` — 원본에 NOT NULL 표기 없음 (D-4) |
| `title` | VARCHAR(150) | NULL | 표시명 |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT CURRENT_TIMESTAMP | 생성 |
| `중복 제약` | — | `UNIQUE(booth_content_id, file_id)` | 중복 연결 방지 |

---

## 부록 A. 상태 전이

### 6-15-1. 부스 관련 상태 전이

| 대상 | 상태 전이 |
|-|-|
| 참여 신청 | `DRAFT` → `PAYMENT_PENDING` → `SUBMITTED` / `PAYMENT_FAILED` / `CANCELED` |
| 부스 주문 | `PENDING_PAYMENT` → `PAYMENT_COMPLETED` / `FAILED` / `CANCELED` / `EXPIRED` |
| 부스 예약 | `ACTIVE` → `CONFIRMED` / `RELEASED` / `EXPIRED` |
| 부스 상품 | `AVAILABLE` → `RESERVED` → `SOLD`. 실패·만료 시 `AVAILABLE` 복구 |
| 부스 배정 | 결제 성공 시 `ASSIGNED` 생성 |

### 참여 신청 철회 처리

| 현재 신청 상태 | 철회 가능 여부 | 변경 상태 |
|-|-|-|
| `DRAFT` | 가능 | `CANCELED` |
| `PAYMENT_PENDING` | 가능 | `CANCELED` |
| `PAYMENT_FAILED` | 가능 | `CANCELED` |
| `SUBMITTED` | 불가 | 변경 없음 |

철회는 결제 완료 전에만 허용하며 다음 작업을 하나의 트랜잭션에서 처리한다.

- `PARTICIPATION_APPLICATIONS.status = CANCELED`
- 생성된 `BOOTH_ORDERS` 가 있으면 `status = CANCELED`
- 유효한 `BOOTH_RESERVATIONS` 가 있으면 `status = RELEASED`
- 예약된 `BOOTH_PRODUCTS` 가 있으면 `sales_status = AVAILABLE`
- `DRAFT` 상태에서 주문·예약이 아직 생성되지 않았다면 신청 상태만 `CANCELED` 로 변경
- `SUBMITTED` 상태는 철회·부스 취소·환불 모두 거부

### 결제 만료 처리

- `BOOTH_ORDERS.status = EXPIRED`
- `BOOTH_RESERVATIONS.status = EXPIRED` 또는 `RELEASED`
- `BOOTH_PRODUCTS.sales_status = AVAILABLE`
- `PARTICIPATION_APPLICATIONS.status = DRAFT`
- `PARTICIPATION_APPLICATIONS.selected_booth_product_id = NULL`
- `PARTICIPATION_APPLICATIONS.booth_order_id = NULL`

### 결제 성공 원자 처리

1. 결제 승인 금액·주문번호·멱등키 검증
2. 선택 부스가 해당 신청에 `RESERVED` 인지 재검증
3. `BOOTH_ORDERS` 를 `PAYMENT_COMPLETED` 로 변경
4. `PARTICIPATION_APPLICATIONS` 를 `SUBMITTED` 로 변경
5. `BOOTH_RESERVATIONS` 를 `CONFIRMED` 로 변경
6. `BOOTH_ALLOCATIONS` 를 `ASSIGNED` 로 생성
7. `BOOTH_PRODUCTS` 를 `SOLD` 로 변경
8. 모든 작업이 성공할 때만 커밋

---

## 부록 B. 최종 필수 제약조건

- `UNIQUE(PARTICIPATION_APPLICATIONS.recruitment_notice_id, client_user_id)` — 기업은 공고당 신청 1건만 보유한다.
- `PARTICIPATION_APPLICATIONS.selected_booth_product_id` 는 단일 FK 이며, 한 신청에서 부스 1개만 선택한다.
- `BOOTH_ORDERS.application_id` 는 UNIQUE 이며, 주문은 단일 `booth_product_id` 를 가진다.
- 동일 `booth_product_id` 에는 유효 `ACTIVE` 예약 1개 또는 `ASSIGNED` 배정 1개만 허용한다.
- `SUBMITTED` 신청은 `PAYMENT_COMPLETED` 주문과 `ASSIGNED` 배정을 반드시 가진다.
- 결제 완료 부스는 사용자 취소·환불을 지원하지 않는다.
- 중복 장소 요청은 저장 가능하지만, `CONFIRMED` `VENUE_RESERVATIONS` 는 기간이 겹칠 수 없다.
- 장소 충돌 운영 결정은 `submitted_at` 오름차순을 기본 원칙으로 하며 결정 관리자·시각·사유를 기록한다.
- 모집공고와 일반 등록 박람회 모두 먼저 `VENUE_RESERVATIONS` 에 `CONFIRMED` 예약을 생성한다.
- 장소·홀·구역·기간 중복은 `VENUE_RESERVATIONS` 의 단일 `EXCLUDE` 제약으로 모든 경로를 함께 차단한다.
- `EXPO_VENUE_ASSIGNMENTS` 는 `expo_id` 와 `venue_reservation_id` 의 1:1 연결만 저장한다.
- 참여 신청 철회는 `DRAFT` / `PAYMENT_PENDING` / `PAYMENT_FAILED` 에서만 가능하고 `SUBMITTED` 에서는 거부한다.

---

## 부록 C. v16 변경 이력

원본이 "문서 전체에서 제거·변경할 구버전 문구"로 정리한 내용이다.

| 구버전 (v14) | v16 처리 |
|-|-|
| 참여 신청 승인·반려 | 참여 신청 운영 확인·보완 요청으로 변경 |
| 참여 신청 심사 결과 / 반려 사유 | 신청·결제·배정 상태 및 보완 요청 내용으로 변경 |
| 승인 기업과 부스 배정 결과 | 결제 완료 기업과 확정 부스 배정 결과로 변경 |
| 기업 모집 기간 종료 후 참여 기업 심사 완료 | 결제·배정 완료 집계로 변경 |
| 장소 중복 요청 자동 차단 | 요청 저장 후 운영자 선착순 검토·허용·취소로 변경 |
| `BOOTH_ORDER_ITEMS` | 제거. 단일 부스 상품을 `BOOTH_ORDERS` 에 직접 연결 |
| `BOOTH_TYPES` | 오타 수정. `BOOTH_TEMPLATES` 로 통일 |
| 장소 기간 중복 제약 | `VENUE_RESERVATIONS` 로 단일화. 모집공고·일반 등록 경로를 모두 같은 `EXCLUDE` 제약으로 검증 |
| `EXPO_VENUE_ASSIGNMENTS` | 장소·기간 중복 컬럼 제거. `expo_id` 와 `venue_reservation_id` 1:1 연결만 유지 |
| CLNT-18 / RECR-08 철회 우선순위 | 둘 다 M·확정으로 통일. 결제 완료 전 상태에서만 철회 |

---

## 부록 D. 보정 및 검토 이력

### D-1. 표 이탈 행 복원 (8곳)

원본 PDF 에서 셀 경계를 벗어나 인쇄되어 있던 행이다. 컬럼명·타입·제약조건이 공백으로 이어붙은 한 줄로
나오고, 설명 칸의 enum 값이 그 위아래 줄로 흩어져 있었다. 아래처럼 한 행으로 복원했다.

| 원본 p. | 테이블 | 컬럼 | 복원한 정의 |
|-|-|-|-|
| 10 | `EXPO_OPENING_REQUESTS` | `status` | VARCHAR(30) NOT NULL, CHECK — `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `CANCELED` |
| 13 | `EXPO_FILES` | `file_purpose` | VARCHAR(30) NOT NULL — `INTRO_PDF`, `CATALOG`, `LEAFLET`, `PROMO_VIDEO`, `OTHER` |
| 14 | `EXTERNAL_LINKS` | `link_type` | VARCHAR(30) NOT NULL — `HOMEPAGE`, `SOCIAL`, `RESERVATION`, `PRODUCT`, `OTHER` |
| 15 | `EXPO_CHANGE_REQUESTS` | `status` | VARCHAR(20) NOT NULL, CHECK — `SUBMITTED`, `UNDER_REVIEW`, `APPLIED`, `REJECTED`, `CANCELED` |
| 16 | `EXPO_CANCELLATION_REQUESTS` | `status` | VARCHAR(20) NOT NULL, CHECK — `SUBMITTED`, `APPROVED`, `REJECTED`, `PROCESSING_REFUNDS`, `COMPLETED` |
| 19 | `TICKET_PRODUCTS` | `status` | VARCHAR(20) NOT NULL, CHECK — `DRAFT`, `ON_SALE`, `SOLD_OUT`, `SALE_ENDED`, `CANCELED` |
| 24 | `CHECK_IN_HISTORIES` | `result` | VARCHAR(30) NOT NULL, CHECK — `SUCCESS`, `ALREADY_USED`, `CANCELED_TICKET`, `WRONG_EXPO`, `INVALID_TOKEN` |
| 32 | `BANNER_APPLICATIONS` | `review_status` | VARCHAR(20) NOT NULL, CHECK — `DRAFT`, `UNDER_REVIEW`, `REJECTED`, `APPROVED`, `CANCELED` |

### D-2. CHECK 누락 보정 (5곳)

enum 값이 명시되어 있는데 제약조건에 `CHECK` 가 없던 컬럼이다. `NOT NULL, CHECK` 로 통일했다.

| 테이블 | 컬럼 | 원본 제약조건 | 보정 후 |
|-|-|-|-|
| `EXPO_IMAGES` | `image_type` | NOT NULL | NOT NULL, CHECK |
| `EXPO_FILES` | `file_purpose` | NOT NULL | NOT NULL, CHECK |
| `EXTERNAL_LINKS` | `link_type` | NOT NULL | NOT NULL, CHECK |
| `EXPO_REVIEW_HISTORIES` | `decision` | NOT NULL | NOT NULL, CHECK |
| `BANNER_REVIEW_HISTORIES` | `decision` | NOT NULL | NOT NULL, CHECK |

`NOTIFICATIONS.reference_type` 은 값 목록이 "`ORDER`, `PAYMENT`, `REFUND`, `EXPO`, `TICKET` **등**"으로
개방형이라 보정하지 않았다.

### D-3. 정의 확보 경과

v16 원본 기준으로는 22개 테이블에 컬럼 정의가 없었다. 보완본 `erd수정본.pdf` 가 17개를 채웠고,
남은 5개는 작성자가 답을 줄 수 없어 **우리 추정으로 채웠다.**

| 테이블 | v16 상태 | 최종 |
|-|-|-|
| `VENUE_HALLS` | 보완본에 제목·설명·`UNIQUE (venue_id, hall_code)` 만 있고 컬럼 표 자리에 `VIRTUAL_VENUES` 표가 들어가 있었음 | 추정 — `VENUE_ZONES` 대칭 |
| `TICKET_PAYMENTS` | 금액·상태 규칙만 서술 | 추정 — `BOOTH_PAYMENTS` 대칭 |
| `TICKET_REFUNDS` | 금액 컬럼 3개와 타입만 | 추정 — 키·시각·처리자 보강 |
| `SETTLEMENTS` | 금액 컬럼과 타입만 | 추정 — PK·FK·제약 보강 |
| `REMITTANCES` | 컬럼명과 의미만 | 추정 — 타입·제약 보강 |

View 는 16개 중 8개만 출력 필드가 정의되어 있었고, 나머지 8개도 같은 방식으로 추정했다.
추정 내역 전체는 [부록 E](#부록-e-추정-컬럼-전수-목록) 에 있다.

**보완본이 채운 17개**

| 구분 | 테이블 |
|-|-|
| v16 에 이름만 있던 것 (10) | `FILE_METADATA`, `VIRTUAL_VENUES`, `VENUE_ZONES`, `BOOTH_TEMPLATES`, `BOOTHS`, `BOOTH_PRODUCTS`, `BOOTH_RESERVATIONS`, `BOOTH_PAYMENTS`, `BOOTH_PAYMENT_HISTORIES`, `BOOTH_ALLOCATIONS` |
| v16 에 설명만 있던 것 (7) | `TICKET_ORDER_ITEMS`, `GUEST_ORDER_INFOS`, `TICKET_PAYMENT_HISTORIES`, `EXPO_DAILY_SALES_SUMMARIES`, `SETTLEMENT_ITEMS`, `SETTLEMENT_ADJUSTMENTS`, `SETTLEMENT_REPORTS` |

보완본은 5페이지에서 누락 원인을 밝히고 있다 — "가상 장소, 홀, 구역, 부스 템플릿 및 고정 부스 구조는
이전 ERD에서 명확하게 정의되어 있었으나 v16에서 '기존 정의 유지' 문구만 남으면서 본문에서 빠진 것".

### D-4. 검토가 필요한 지점

보정하지 않고 원본대로 두었다. 근거 없이 한쪽으로 정할 수 없는 것들이다.

1. **상태 컬럼의 `NOT NULL` 표기 누락** — 아래 컬럼들은 `CHECK` 만 있고 `NOT NULL` 이 없다. 다른 절의
   동일 성격 컬럼은 대부분 `NOT NULL, CHECK` 라 표기 누락으로 보이지만, 임의로 채우지 않았다.
   `PASSWORD_RESET_TOKENS.status`, `TICKET_ACCESS_TOKENS.scope` / `status`,
   `RECRUITMENT_NOTICE_REQUESTS.status` / `venue_conflict_status` / `venue_decision`,
   `VENUE_RESERVATIONS.reservation_source_type` / `status`, `RECRUITMENT_NOTICES.status`,
   `PARTICIPATION_APPLICATIONS.status`, `RECRUITMENT_RESULTS.status`, `BOOTH_ORDERS.status`,
   `BOOTH_CONTENTS.status`, `BOOTH_CONTENT_FILES.file_type`

2. **`DEFAULT` 만 있고 `NOT NULL` 이 없는 컬럼** — `PASSWORD_RESET_TOKENS.created_at`,
   `TICKET_ACCESS_TOKENS.access_count` / `created_at`. 다른 테이블의 `created_at` 은 모두
   `NOT NULL DEFAULT CURRENT_TIMESTAMP` 다.

3. ~~**`TICKET_INVENTORIES.available_quantity` 구현 미정**~~ — **결정됨** ✔
   PostgreSQL 생성 컬럼(`GENERATED ALWAYS AS … STORED`)으로 확정했다. 스키마 전체를
   PostgreSQL 네이티브로 가기로 하면서 H2 호환을 고려할 이유가 없어졌다.
   음수 방지 CHECK 는 부록 F 참조.

4. **View 개수 불일치** — 원본 2페이지는 "SQL View 12개"라고 하는데, 본문에 이름이 등장하는 View 는
   16개다. `V_MEMBER_MYPAGE_PROFILE` / `_ORDERS` / `_TICKETS`, `V_PUBLIC_EXPO_CARDS`,
   `V_CLIENT_DASHBOARD_PROFILE` / `_EXPOS` / `_DAILY_SALES` / `_RECRUITMENT` / `_BOOTHS` / `_SETTLEMENTS`,
   `V_ADMIN_DASHBOARD_COUNTS` / `_PENDING_REVIEWS` / `_VENUE_CONFLICTS` / `_PARTICIPATION_OPERATIONS` /
   `_PAYMENT_REFUND_STATUS` / `_SETTLEMENT_STATUS`.

5. **`V_MEMBER_MYPAGE_PROFILE` 의 소속** — 6-2 절의 엔티티 목록에는 `V_MEMBER_MYPAGE_ORDERS` 와
   `V_MEMBER_MYPAGE_TICKETS` 만 있는데, 본문에는 `V_MEMBER_MYPAGE_PROFILE` 이 먼저 나온다.

6. **`EXPO_VENUE_ASSIGNMENTS` 중복 정의** — 6-3 과 6-14 에 모두 등장한다. 원본이 41페이지에서
   "최종 컬럼 정의는 6-3 절을 기준으로 한다"고 명시했으므로 이 문서도 6-3 을 따랐다.

7. **테이블 개수 불일치** — v16 2페이지는 "물리 테이블 68개"라고 하는데, 본문에서 이름을 확인할 수
   있는 테이블은 67개다. v16 이 잘못 센 것인지, 본문에서 통째로 빠진 테이블이 하나 있는지는
   확인되지 않는다.

---

**아래는 보완본 `erd수정본.pdf` 병합 과정에서 나온 것이다.**

8. **`BOOTH_ALLOCATIONS.booth_order_item_id` 가 제거된 테이블을 참조했다 — 정리함** ✔
   보완본은 이 컬럼을 `UNIQUE, FK → BOOTH_ORDER_ITEMS.id, NOT NULL` 로 정의했다. 그런데 v16 은
   부록 C 에서 `BOOTH_ORDER_ITEMS` 를 **제거**하고 "단일 부스 상품을 `BOOTH_ORDERS` 에 직접 연결"
   하기로 확정했으며, 6-15 절 서두에도 같은 내용을 명시한다.
   → **`booth_order_id`, `UNIQUE, FK → BOOTH_ORDERS.id, NOT NULL` 로 교체했다.**
   근거는 부록 B 의 "`BOOTH_ORDERS.application_id` 는 UNIQUE 이며 주문은 단일 `booth_product_id` 를
   가진다". 주문 1건에 배정 1건이므로 UNIQUE 는 유지했다.

9. **`SETTLEMENT_ITEMS.item_type` 값이 두 문서에서 다르다** — v16 은 7개, 보완본은 5개다.

   | 값 | v16 | 보완본 |
   |-|-|-|
   | `TICKET_SALE` | ○ | ○ |
   | `TICKET_REFUND` | ○ | ○ |
   | `BOOKING_FEE` | ○ | ○ |
   | `BOOKING_FEE_REFUND` | ○ | ○ |
   | `ADJUSTMENT` | ○ | ○ |
   | `BOOTH_SALE` | ○ | **없음** |
   | `PG_FEE_REFERENCE` | ○ | **없음** |

   **정리함** ✔ — `BOOTH_SALE` 은 되살리고 `PG_FEE_REFERENCE` 는 복원하지 않아 **6값**으로 확정했다.
   `PG_FEE_REFERENCE` 제거는 "PG 수수료도 클라이언트 송금액에서 별도로 차감하지 않는다"는 근거가
   서술되어 있다. 반면 `BOOTH_SALE` 은 제거 사유가 없는데, `SETTLEMENTS.gross_booth_sales_amount` 가
   살아 있고 `remittance_due_amount` 계산식에도 부스 매출이 들어가므로 근거 항목 유형이 없으면
   그 금액의 출처를 남길 수 없다.

10. **`VENUE_HALLS` 제목과 컬럼 표가 어긋나 있다** — 보완본 1~2페이지에서 제목 `VIRTUAL_VENUES` 아래에
    `FILE_METADATA` 컬럼 표가, 제목 `VENUE_HALLS` 아래에 `VIRTUAL_VENUES` 컬럼 표가 놓여 있다.
    세 번째 `VENUE_ZONES` 부터는 정상이다. 컬럼 내용으로 테이블을 식별해 배정했고, 그 결과
    `VENUE_HALLS` 의 컬럼 표만 존재하지 않는다는 것이 확인됐다.

11. **`TICKET_ORDER_ITEMS` 금액 컬럼 명칭** — 이전 문서의 `line_amount` 와 v16 의
    `item_subtotal_amount` 가 같은 의미로 혼용되어 있었다. 보완본 권고에 따라
    `item_subtotal_amount` 로 통일했다.

12. **`SETTLEMENT_REPORTS` 파일 FK 명칭** — 이전 설계의 `stored_file_id` 를 프로젝트의 파일 FK
    명명 방식에 맞춰 `file_id` 로 바꾼 보완안이다.

13. **`EXPO_DAILY_SALES_SUMMARIES` 금액 필드는 신규 제안이다** — 보완본이 밝힌 바에 따르면 원본
    ERD 에는 주문 수와 티켓 수량 필드까지만 있었고, `ticket_sales_amount` 이하 금액 필드 7개는
    최신 3% 수수료·정산 정책을 반영해 보완본이 새로 만든 것이다. 확정 전 검토가 필요하다.

---

**아래는 우리가 추정으로 채우면서 세운 가정이다. 근거가 약한 순으로 적었다.**

14. **`V_ADMIN_PENDING_REVIEWS` 와 `V_ADMIN_PAYMENT_REFUND_STATUS` 의 `UNION ALL` 구조** — 이 문서에서
    가장 큰 추정이다. 명세는 "통합 조회한다"는 표현만 있다. 세 종류 심사(박람회·배너·모집공고 요청)와
    두 종류 결제(티켓·부스)를 각각 한 뷰로 합치는 구조를 가정했다. 화면을 나눠 쓸 생각이었다면
    뷰를 쪼개야 한다.

15. **`TICKET_REFUNDS.ticket_order_id` 의 UNIQUE** — "전체 주문 취소만 지원하고 부분 취소는 없다"는
    정책에 근거해 주문당 환불 1건으로 잡았다. 그런데 `V_MEMBER_MYPAGE_ORDERS` 는 "**최신**
    `TICKET_REFUNDS.status`" 라고 표현한다. 환불 실패 후 재시도로 여러 건이 쌓이는 설계라면
    UNIQUE 를 빼야 한다.

16. **`SETTLEMENTS.expo_id` 의 UNIQUE** — "박람회 단위 정산 대표 정보"라는 서술에 근거한 1:1 가정이다.
    한 박람회에 회차 정산이 생기면 깨진다.

17. **`TICKET_PAYMENTS` 승인 성공 값이 `DONE`, `BOOTH_PAYMENTS` 는 `APPROVED`** — 형제 테이블인데
    다르다. 둘 다 명세에 있는 값이라 통일하지 않았다. 애플리케이션에서 Enum 을 나눠 다뤄야 한다.

18. **`SETTLEMENTS` 금액 컬럼에 `CHECK >= 0` 없음** — `adjustment_amount` 는 음수가 될 수 있고
    나머지도 환불이 섞이면 부호가 뒤집힐 수 있어 제약을 걸지 않았다. 반대로 `SETTLEMENT_ITEMS.amount`
    는 명세가 "부호를 포함한 금액"이라고 못 박고 있어 일관된다.

### D-5. 이 문서에서 제외한 원본 내용

- API 엔드포인트 표 (`GET /api/users/me/profile` 등) — 스키마와 무관
- View 표의 `화면 사용 목적` 열 — 필드-원천 매핑만 남김

---

## 부록 E. 추정 컬럼 전수 목록

명세가 아니라 우리가 채운 것들이다. 진짜 명세를 받으면 **이 목록만 대조하면 된다.**

`보완` = 컬럼은 명세에 있고 타입·제약만 우리가 채움 · `추정` = 컬럼 자체를 우리가 만듦

### E-1. 테이블 5개

| 테이블 | 추정 근거 | `보완` | `추정` |
|-|-|-|-|
| `VENUE_HALLS` | `VENUE_ZONES` 대칭 | `venue_id`, `hall_code` | `id`, `name`, `width`, `depth`, `layout_file_id`, `operational_status`, `created_at`, `updated_at` |
| `TICKET_PAYMENTS` | `BOOTH_PAYMENTS` 대칭 | `payment_key`, `requested_amount`, `approved_amount`, `ticket_subtotal_amount`, `booking_fee_amount` | `id`, `ticket_order_id`, `pg_order_id`, `method`, `approved_at`, `canceled_amount`, `last_failure_code`, `idempotency_key`, `created_at`, `updated_at` |
| `TICKET_REFUNDS` | `TICKET_PAYMENTS` 구조 + 6-3 절 일괄 환불 서술 | `refund_ticket_amount`, `refund_booking_fee_amount`, `refund_amount` | `id`, `ticket_order_id`, `ticket_payment_id`, `reason`, `pg_refund_key`, `requested_at`, `completed_at`, `last_failure_code`, `processed_by_admin_id`, `created_at`, `updated_at` |
| `SETTLEMENTS` | 자식 테이블 3개의 FK + 클라이언트 대시보드 출력 필드 | 금액 10개, `settlement_due_at` | `id`, `expo_id`, `host_client_id`, `created_at`, `updated_at` |
| `REMITTANCES` | v16 이 컬럼명·의미를 모두 제시. 타입만 없음 | `settlement_id`, `scheduled_at`, `remitted_amount`, `remitted_at`, `reference_number`, `memo`, `processed_by` | `id`, `created_at`, `updated_at` |

`SETTLEMENTS` 의 금액 10개 — `gross_ticket_sales_amount`, `ticket_refund_amount`,
`net_ticket_sales_amount`, `booking_fee_gross_amount`, `booking_fee_refund_amount`,
`booking_fee_net_amount`, `gross_booth_sales_amount`, `pg_fee_reference_amount`,
`adjustment_amount`, `remittance_due_amount`.

### E-2. View 8개

출력 필드 전체가 추정이다. 근거는 각 절의 경고 블록에 적었다.

| View | 추정 근거 |
|-|-|
| `V_CLIENT_DASHBOARD_EXPOS` | 용도 문장 "본인 등록 박람회 목록, 심사 및 노출 상태" |
| `V_CLIENT_DASHBOARD_DAILY_SALES` | `EXPO_DAILY_SALES_SUMMARIES` 를 그대로 노출 |
| `V_CLIENT_DASHBOARD_RECRUITMENT` | 용도 문장 "모집 공고, 신청 완료 수, 확정 배정 수" |
| `V_CLIENT_DASHBOARD_BOOTHS` | 용도 문장 + `V_ADMIN_PARTICIPATION_OPERATIONS` 필드 구성 |
| `V_ADMIN_DASHBOARD_COUNTS` | 6-12 절이 나열한 관리자 업무를 건수 필드로 환산 |
| `V_ADMIN_PENDING_REVIEWS` | 6-12 절 "박람회·배너 심사, 모집공고 생성 요청" — `UNION ALL` 구조는 가정 (D-4 14) |
| `V_ADMIN_PAYMENT_REFUND_STATUS` | 6-12 절 "결제·환불 상태 통합 조회" — `UNION ALL` 구조는 가정 (D-4 14) |
| `V_ADMIN_SETTLEMENT_STATUS` | `V_CLIENT_DASHBOARD_SETTLEMENTS` 필드에서 관리자용으로 재구성 |

### E-3. 명세를 우리 판단으로 바꾼 것

| 대상 | 원본 | 변경 | 근거 |
|-|-|-|-|
| `BOOTH_ALLOCATIONS` | `booth_order_item_id` → `BOOTH_ORDER_ITEMS.id` | `booth_order_id` → `BOOTH_ORDERS.id` | v16 부록 C 가 `BOOTH_ORDER_ITEMS` 를 제거 (D-4 8) |
| `SETTLEMENT_ITEMS.item_type` | 보완본 5값 | `BOOTH_SALE` 추가한 6값 | `SETTLEMENTS.gross_booth_sales_amount` 의 근거 항목 필요 (D-4 9) |
| `EXPO_IMAGES.image_type` 외 4곳 | `NOT NULL` | `NOT NULL, CHECK` | enum 값이 명시되어 있었음 (D-2) |

---

## 부록 F. 코드 리뷰로 추가한 제약

PR #5 의 CodeRabbit 리뷰에서 나온 지적 중 타당한 것을 반영한 결과다. 명세에는 없지만 명세가
서술로 요구하던 불변식을 DB 로 옮긴 것이라, **명세와 충돌하지 않고 보강한다.**

### F-1. 값싼 제약

| 대상 | 추가한 제약 | 막는 상황 |
|-|-|-|
| `VENUE_RESERVATIONS` | `ck_venue_reservations_source` 를 판별자 매칭으로 강화 | `reservation_source_type` 이 `RECRUITMENT_NOTICE` 인데 `opening_request_id` 만 채우는 조합 |
| `VENUE_RESERVATIONS` | `ck_venue_reservations_zone_needs_hall` | 구역만 지정하고 상위 홀은 비우는 조합 |
| `TICKET_INVENTORIES` | `ck_ticket_inventories_not_oversold` | `reserved + sold > total` 로 `available_quantity` 가 음수가 되는 오버셀 |
| `TICKET_ORDERS` | `ck_ticket_orders_orderer` | `MEMBER` 주문인데 `member_user_id` 가 NULL, 또는 `GUEST` 인데 값이 있는 조합 |
| `TICKET_ORDERS` | `ck_ticket_orders_total` | `total_amount ≠ ticket_subtotal_amount + booking_fee_amount` |
| `TICKET_ORDERS` | `ck_ticket_orders_fee_rate` | 요율이 범위를 벗어남 (0 이상 1 미만) |
| `SETTLEMENT_ITEMS` | UNIQUE 를 `NULLS NOT DISTINCT` 로 변경 | `source_type`·`source_id` 가 NULL 인 동일 항목의 무제한 중복 집계 |
| `REMITTANCES` | `uq_remittances_active` 부분 UNIQUE 인덱스 | 한 정산에 `PENDING`/`PROCESSING` 송금이 동시에 여럿 생겨 이중 송금 |

`booking_fee_rate` 는 **값을 `0.03000` 으로 고정하지 않았다.** 이 컬럼은 "주문 당시 요율 스냅샷"이라
고정하면 요율 정책이 바뀌는 순간 과거 주문이 전부 제약 위반이 된다. 범위 CHECK 만 건다.

### F-2. 장소 계층 강제

단일 FK 는 "그 id 가 존재한다"만 보장한다. 다른 장소의 홀이나 다른 홀의 구역을 참조하는 게 가능했다.
`VENUE_RESERVATIONS` 에서는 이게 특히 위험하다 — `EXCLUDE` 제약이 `(장소, 홀, 구역)` 튜플로
파티션하므로 **불일치 튜플을 넣으면 기간 중복 검사 자체를 우회**할 수 있었다.

`VENUE_HALLS` 에 `UNIQUE (id, venue_id)`, `VENUE_ZONES` 에 `UNIQUE (id, hall_id)` 를 추가하고,
아래 세 테이블이 복합 FK 로 계층을 검증한다.

- `VENUE_RESERVATIONS` — `(venue_hall_id, virtual_venue_id)`, `(venue_zone_id, venue_hall_id)`
- `EXPO_OPENING_REQUESTS` — `(desired_venue_hall_id, desired_venue_id)`, `(desired_venue_zone_id, desired_venue_hall_id)`
- `RECRUITMENT_NOTICE_REQUESTS` — `(venue_hall_id, virtual_venue_id)`, `(venue_zone_id, venue_hall_id)`

홀·구역이 선택 항목이라 NULL 이 섞인 행은 `MATCH SIMPLE` 기본 동작상 검증을 건너뛴다. 의도한
동작이고, "구역만 있고 홀이 없는" 조합은 CHECK 로 따로 막았다.

### F-3. 확정 예약 불변식 (트리거)

6-3 절이 "확정 예약만 박람회에 연결"이라고 못 박았지만 FK 로는 예약의 `status` 를 볼 수 없다.
`expo_venue_assignments` 의 INSERT / `venue_reservation_id` UPDATE 에 트리거를 걸어
`CONFIRMED` 가 아닌 예약의 연결을 거부한다.

> **연결된 예약이 나중에 `CONFIRMED` 를 벗어나는 것은 막지 않았다.** 리뷰는 그 전이도 막으라고
> 했지만, 6-3 절의 박람회 취소 흐름이 "연결된 `VENUE_RESERVATIONS` 를 `RELEASED` 로 변경"하도록
> 명시하고 있어 전이를 막으면 명세가 요구하는 취소가 불가능해진다. 리뷰 지적을 절반만 받았다.

### F-4. 부스 구매 체인 정합성

단일 FK 만으로는 무관한 신청·기업·부스상품을 조합한 주문이나 배정이 만들어질 수 있었다. 결제 성공
트랜잭션이 구조적으로는 멀쩡하지만 엉뚱한 배정을 커밋하는 사고가 가능했다.

신청의 기업·선택부스를 주문으로, 주문의 기업·부스상품·신청을 배정으로 복합 FK 로 전파한다.

| 제약 | 검증 내용 |
|-|-|
| `fk_applications_product_in_notice` | 선택한 부스 상품이 그 신청이 속한 공고의 상품인가 |
| `fk_booth_orders_client_matches_app` | 주문의 기업이 신청의 기업과 같은가 |
| `fk_booth_orders_product_matches_app` | 주문의 부스 상품이 신청이 선택한 상품과 같은가 |
| `fk_booth_allocations_client_matches_order` | 배정의 기업이 주문의 기업과 같은가 |
| `fk_booth_allocations_product_matches_order` | 배정의 부스 상품이 주문의 상품과 같은가 |
| `fk_booth_allocations_app_matches_order` | 배정의 신청이 주문의 신청과 같은가 |

### F-5. 뷰 수정

- **`V_PUBLIC_EXPO_CARDS`** — 재고와 주문 항목을 `TICKET_PRODUCTS` 에 함께 조인해 카테시안 곱이
  발생했다. 상품당 재고는 1행이지만 주문 항목이 N행이면 `SUM` 이 N배가 되어 `available_quantity` 와
  `popularity_score` 가 부풀려졌다. 두 집계를 CTE 로 박람회 단위까지 미리 접은 뒤 1:1 로 붙이도록 고쳤다.
- **`LIMIT 1` 서브쿼리 11곳** — `ORDER BY created_at DESC` 만으로는 같은 타임스탬프에서 결과가
  비결정적이라 `id DESC` tiebreaker 를 추가했다. R__03·09·10·14·15·16 에 적용.

### F-6. 받지 않은 지적

| 지적 | 이유 |
|-|-|
| 뷰에 `ORDER BY` 추가 (`V_ADMIN_VENUE_CONFLICTS`) | 뷰의 `ORDER BY` 는 옵티마이저가 무시할 수 있고 페이지네이션과 충돌한다. 정렬은 소비 측 책임이다. 리뷰 본문도 "최종 조회에 적용하라"고 적고 있어 사실상 애플리케이션 이슈다 |
| `booking_fee_rate` 를 `0.03000` 으로 CHECK 고정 | 위 F-1 참조. 스냅샷 컬럼의 존재 이유와 모순된다 |
