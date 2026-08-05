<!-- Page 1 -->

# 박람회 티켓 판매 중개 플랫폼

# 통합 ERD 스키마 정의서 v16

보고서 불일치 해소 및 장소 예약 통합 최종본
이 문서는 별도 수정 페이지를 앞에 덧붙이지 않고 최종 정책을 각 기능 절의 본문과 테이블에 직접 반영한 단일 기준본입니다.

---

<!-- Page 2 -->

박람회 티켓 판매 중개 플랫폼
통합 ERD 스키마 정의서 v16
보고서 불일치 해소 및 장소 예약 통합 최종본
기준: PostgreSQL / TIMESTAMPTZ / NUMERIC(15,2)
작성 기준일: 2026-07-31
문서 구성
- 요구사항 분석서 기능 요구사항 6-1 부터 6-15 순서
- 물리 테이블 68 개 (REFRESH_TOKENS 포함)
- SQL View 12 개
- 이전 v5, v6, v7, v8 별도 수정 페이지를 제거하고 최종 정의를 각 본문 절에 직접 반영
최종 확정 정책
- 티켓 예매 수수료: 티켓 판매원금의 3%를 구매자가 추가 결제
- 티켓 정산: 판매원금 - 환불원금 +/- 확정 조정 금액, 예매 수수료를 재차감하지 않음
- 부스 신청: 신청서 작성, 부스 선택, 결제 성공 즉시 신청 완료 및 확정 배정
- 부스 수수료 및 환불: 플랫폼 수수료 0 원, 결제 완료 후 취소 및 환불 미지원
- 정산: 행사 종료 후 7~14 일 이내 대상 생성, 관리자 확정 후 외부 송금 결과 기록
- 로그아웃: Refresh Token 해시 DB 저장, revoked_at 기반 현재 또는 전체 기기 폐기
- 프로필 이미지: 일반 회원·클라이언트 공통으로 USERS.profile_image_file_id 를 통해 FILE_METADATA 와 연결
- 일반 박람회 장소 선택: 클라이언트가 신청 시 장소·홀·구역을 직접 선택
- 박람회 취소: 연결된 VENUE_RESERVATIONS 를 RELEASED 로 해제
- 관리자 권한: MVP 의 ADMIN 은 단일 SUPER ADMIN

## 0. 문서 구성 및 공통 설계 기준

PK: 기본키는 BIGSERIAL, 연결 테이블은 필요한 경우 복합 UNIQUE 사용
시간: TIMESTAMPTZ 사용, 시작일 < 종료일 검증
금액: NUMERIC(15,2), 서버 기준 가격으로 재계산
상태: VARCHAR + CHECK 또는 Java Enum
동시성: 재고, 부스 상품 및 발권 티켓에 version 과 필요한 잠금 적용
삭제: 주문, 결제, 심사, 정산 및 토큰 폐기 이력은 감사 정책에 따라 보존
보안: 비밀번호, 비회원 조회 비밀번호, QR 토큰, Refresh Token 은 원문 대신 해시 저장

## 6-1. 인증 및 사용자 관리

일반 회원, 클라이언트, 관리자 계정과 이메일·소셜 로그인, 휴대폰 본인인증, 비밀번호 재설정, 탈퇴를 관리합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | USERS |
| 물리 테이블 | CLIENT_PROFILES |
| 물리 테이블 | SOCIAL_ACCOUNTS |
| 물리 테이블 | PHONE_VERIFICATIONS |
| 물리 테이블 | PASSWORD_RESET_TOKENS |
| 물리 테이블 | REFRESH_TOKENS |

---

<!-- Page 3 -->

### USERS

공통 계정 테이블. 일반 회원, 클라이언트, 관리자를 하나의 계정 모델로 관리하며, 일반 회원 마이페이지와 클라이언트 페이지에서 사용하는 프로필 이미지를 FILE_METADATA 와 연결합니다. MVP
의 ADMIN 은 별도 일반 관리자 등급 없이 전체 운영 권한을 가진 단일 SUPER ADMIN 으로 사용합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 사용자 고유 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 |
| password_hash | VARCHAR(255) | NULL | 소셜 로그인 전용 계정은 NULL 가능 |
| nickname | VARCHAR(50) | UNIQUE, NOT NULL | 서비스 닉네임 |
| role | VARCHAR(20) | NOT NULL, CHECK MEMBER, CLIENT,<br>ADMIN | 역할; ADMIN은 단일 SUPER ADMIN |
| account_status | VARCHAR(20) | NOT NULL, CHECK | ACTIVE, WITHDRAWN |
| phone_number | VARCHAR(20) | NULL | 본인인증 휴대폰 번호 |
| phone_verified_at | TIMESTAMPTZ | NULL | 휴대폰 본인인증 완료 일시 |
| last_login_at | TIMESTAMPTZ | NULL | 마지막 로그인 일시 |
| withdrawn_at | TIMESTAMPTZ | NULL | 탈퇴 일시 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |
| profile_image_file_id | BIGINT | FK -> FILE_METADATA.id, NULL | 일반 회원·클라이언트 공용 프로필 이미지<br>파일 |
| profile_image_updated_at | TIMESTAMPTZ | NULL | 프로필 이미지 마지막 변경 일시 |

관리자 권한 정책: MVP 에서는 ADMIN 계정이 회원·클라이언트·박람회·배너·모집공고·장소·부스·결제·환불·정산을 모두 관리하는 SUPER ADMIN 입니다. ADMIN 역할 세분화와 별도 권한 테이블은
2 차 범위에서 검토합니다.

### CLIENT_PROFILES

role=CLIENT 인 사용자의 사업자 상세 프로필입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| user_id | BIGINT | PK, FK -> USERS.id | 클라이언트 계정 ID |
| business_number | VARCHAR(20) | UNIQUE, NOT NULL | 사업자등록번호 |
| company_name | VARCHAR(150) | NOT NULL | 기업명/주최사명 |
| representative_name | VARCHAR(100) | NOT NULL | 대표자명 |

---

<!-- Page 4 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| business_address | VARCHAR(255) | NOT NULL | 사업장 주소 |
| business_type | VARCHAR(100) | NULL | 업태/종목 |
| business_number_verified | BOOLEAN | NOT NULL DEFAULT FALSE | 사업자번호 검증 여부 |
| business_verified_at | TIMESTAMPTZ | NULL | 검증 완료 일시 |
| 컬럼명 | 타입 | 제약조건 | 설명 |
| verification_provider | VARCHAR(30) | NULL | 검증 제공자/API |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### SOCIAL_ACCOUNTS

구글·카카오 OAuth 계정을 USERS 와 연결합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 소셜 계정 ID |
| user_id | BIGINT | FK -> USERS.id, NOT NULL | 연결 사용자 |
| provider | VARCHAR(20) | NOT NULL, CHECK | GOOGLE, KAKAO |
| provider_user_id | VARCHAR(255) | NOT NULL | OAuth 제공자 사용자 ID |
| provider_email | VARCHAR(255) | NULL | 제공받은 이메일 |
| linked_at | TIMESTAMPTZ | NOT NULL | 연결 일시 |
| last_login_at | TIMESTAMPTZ | NULL | 해당 소셜 계정 마지막 로그인 |
| (provider, provider_user_id) | - | UNIQUE | 동일 소셜 계정 중복 연결 방지 |

### PHONE_VERIFICATIONS

휴대폰 본인인증 요청과 결과를 기록합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 인증 이력 ID |
| user_id | BIGINT | FK -> USERS.id, NULL | 가입 완료 전에는 NULL 가능 |
| phone_number | VARCHAR(20) | NOT NULL | 인증 대상 번호 |

---

<!-- Page 5 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| verification_token_hash | VARCHAR(255) | NOT NULL | 인증 거래/토큰 해시 |
| status | VARCHAR(20) | NOT NULL, CHECK | REQUESTED, VERIFIED, FAILED, EXPIRED |
| requested_at | TIMESTAMPTZ | NOT NULL | 요청 일시 |
| verified_at | TIMESTAMPTZ | NULL | 성공 일시 |
| expires_at | TIMESTAMPTZ | NOT NULL | 인증 만료 일시 |

### PASSWORD_RESET_TOKENS

AUTH-10 비밀번호 재설정 링크의 일회용 토큰과 만료·사용 상태를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 재설정 토큰 ID |
| user_id | BIGINT | FK -> USERS.id, NOT NULL | 대상 사용자 |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | 원문 대신 저장하는 토큰 해시 |
| status | VARCHAR(20) | CHECK: ISSUED, USED, EXPIRED,<br>REVOKED | 토큰 상태 |
| issued_at | TIMESTAMPTZ | NOT NULL | 발급 일시 |
| expires_at | TIMESTAMPTZ | NOT NULL | 만료 일시 |
| used_at | TIMESTAMPTZ | NULL | 사용 일시 |
| requested_ip | VARCHAR(45) | NULL | 요청 IP |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

### REFRESH_TOKENS

사용자별 로그인 기기에서 발급된 Refresh Token 을 DB 로 관리합니다. 토큰 원문은 저장하지 않고 해시만 저장하며, 한 사용자가 여러 기기에서 로그인할 수 있도록 USERS 1:N
REFRESH_TOKENS 관계를 적용합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | Refresh Token 레코드 ID |
| user_id | BIGINT | FK -> USERS.id, NOT NULL | 토큰 소유 사용자 |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | Refresh Token 원문 대신 저장하는 해시 |
| expires_at | TIMESTAMPTZ | NOT NULL | 만료 일시 |
| revoked_at | TIMESTAMPTZ | NULL | 로그아웃 또는 강제 폐기 일시 |

---

<!-- Page 6 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| last_used_at | TIMESTAMPTZ | NULL | 마지막 재발급 사용 일시 |
| created_ip | VARCHAR(45) | NULL | 발급 요청 IP |
| user_agent | VARCHAR(500) | NULL | 기기 및 브라우저 식별 보조 정보 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 발급 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

인증 처리 규칙
- 로그인 성공: Access Token 과 Refresh Token 발급 -> Refresh Token 해시 INSERT -> HttpOnly 쿠키 설정
- 재발급: 서명과 만료 검증 -> 해시 계산 -> DB 존재 여부와 revoked_at 확인 -> Access Token 재발급 및 last_used_at 갱신
- 현재 기기 로그아웃: POST /api/auth/logout -> 해당 토큰 revoked_at 기록 -> 인증 쿠키 삭제
- 전체 기기 로그아웃: POST /api/auth/logout-all -> 사용자의 활성 토큰을 일괄 폐기
- 사용자 탈퇴: 활성 Refresh Token 을 모두 폐기
- 만료 또는 폐기된 토큰의 재사용은 거부

---

<!-- Page 7 -->

## 6-2. 일반 회원 마이페이지

회원 본인의 프로필 이미지 등록·변경·삭제, 닉네임 관리, 주문, 결제, 환불, 발권 티켓과 QR 상태 조회를 제공합니다. 프로필 이미지는 USERS.profile_image_file_id 로 FILE_METADATA
를 참조합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| SQL View | V_MEMBER_MYPAGE_ORDERS |
| SQL View | V_MEMBER_MYPAGE_TICKETS |

### V_MEMBER_MYPAGE_PROFILE

일반 회원 마이페이지의 프로필 이미지 조회 및 변경 화면입니다.

| 출력/입력 필드 | 타입 | 원천/처리 | 화면 사용 목적 |
| --- | --- | --- | --- |
| member_user_id | BIGINT | USERS.id | 로그인 회원 식별 |
| nickname | VARCHAR | USERS.nickname | 닉네임 표시·변경 |
| profile_image_file_id | BIGINT | USERS.profile_image_file_id -><br>FILE_METADATA.id | 프로필 이미지 연결 |
| profile_image_url | VARCHAR | FILE_METADATA.storage_key 기반 서명<br>URL 생성 | 프로필 이미지 표시 |
| profile_image_updated_at | TIMESTAMPTZ | USERS.profile_image_updated_at | 이미지 변경 일시 |

프로필 이미지 처리 규칙
- 허용 형식: JPEG, PNG, WEBP
- 권장 최대 크기: 5MB
- 업로드 성공 시 FILE_METADATA 생성 후 USERS.profile_image_file_id 갱신
- 변경 시 새 파일 연결 후 기존 파일은 참조 여부를 확인하여 DELETED 처리
- 삭제 시 USERS.profile_image_file_id 를 NULL 로 변경

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| GET | /api/users/me/profile | 내 프로필 및 이미지 조회 |
| POST | /api/users/me/profile-image | 프로필 이미지 등록·변경 |
| DELETE | /api/users/me/profile-image | 프로필 이미지 삭제 |

### V_MEMBER_MYPAGE_ORDERS

일반 회원 주문 목록 및 상세 요약입니다.

| 출력 필드 | 원천 / 계산 | 화면 사용 목적 |
| --- | --- | --- |
| member_user_id | TICKET_ORDERS.member_user_id | 조회 사용자 |
| order_id, order_number | TICKET_ORDERS | 주문 식별 |
| expo_id, expo_title | TICKET_ORDER_ITEMS -> TICKET_PRODUCTS -> EXPOS<br>박람회 |   |
| order_status | TICKET_ORDERS.status | 주문 상태 |
| payment_status | 최신 TICKET_PAYMENTS.status | 결제 상태 |

---

<!-- Page 8 -->

| 출력 필드 | 원천 / 계산 | 화면 사용 목적 |
| --- | --- | --- |
| refund_status | 최신 TICKET_REFUNDS.status | 환불 상태 |
| total_quantity | TICKET_ORDERS.total_quantity | 총 티켓 수량 |
| ticket_subtotal_amount | TICKET_ORDERS.ticket_subtotal_amount | 티켓 판매원금 |
| booking_fee_rate | TICKET_ORDERS.booking_fee_rate | 주문 당시 예매 수수료율 3% |
| booking_fee_amount | TICKET_ORDERS.booking_fee_amount | 예매 수수료 |
| total_amount | TICKET_ORDERS.total_amount | 최종 결제금액 |
| refundable | 행사 3일 전 및 미사용 여부 계산 | 전체 주문 환불 가능 여부 |

### V_MEMBER_MYPAGE_TICKETS

일반 회원 발권 티켓 및 QR 상세입니다.

| 출력 필드 | 원천 / 계산 | 화면 사용 목적 |
| --- | --- | --- |
| member_user_id | TICKET_ORDERS.member_user_id | 조회 사용자 |
| order_id | TICKET_ORDERS.id | 주문 |
| issued_ticket_id | ISSUED_TICKETS.id | 발권 티켓 |
| ticket_code, status | ISSUED_TICKETS | 티켓 코드 및 상태 |
| checked_in_at | ISSUED_TICKETS.checked_in_at | 입장 일시 |
| expo_id, expo_title | EXPOS | 박람회 |
| secure_qr_access | TICKET_ACCESS_TOKENS 유효성 | QR 접근 가능 여부 |

---

<!-- Page 9 -->

## 6-3. 박람회 관리

클라이언트의 박람회 개최 신청부터 관리자 심사, 승인 후 자동 공개, 장소 배정, 수정 요청, 취소와 변경 이력을 관리합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | EXPO_OPENING_REQUESTS |
| 물리 테이블 | EXPOS |
| 물리 테이블 | EXPO_VENUE_ASSIGNMENTS |
| 물리 테이블 | CATEGORIES |
| 물리 테이블 | EXPO_CATEGORIES |
| 물리 테이블 | EXPO_IMAGES |
| 물리 테이블 | EXPO_FILES |
| 물리 테이블 | EXTERNAL_LINKS |
| 물리 테이블 | EXPO_COMPANIES |
| 물리 테이블 | EXPO_REVIEW_HISTORIES |
| 물리 테이블 | EXPO_CHANGE_REQUESTS |
| 물리 테이블 | EXPO_CHANGE_HISTORIES |
| 물리 테이블 | EXPO_CANCELLATION_REQUESTS |

### EXPO_OPENING_REQUESTS

모집 결과를 확인한 주최 클라이언트의 박람회 개최 신청입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 개최 신청 ID |
| host_client_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 주최 클라이언트 |
| 컬럼명 | 타입 | 제약조건 | 설명 |
| recruitment_result_id | BIGINT | FK -> RECRUITMENT_RESULTS.id, NULL | 연결 모집 결과; 일반 등록은 NULL 가능 |
| title | VARCHAR(255) | NOT NULL | 박람회 명칭 |
| description | TEXT | NOT NULL | 소개 |
| event_start_at | TIMESTAMPTZ | NOT NULL | 행사 시작 |
| event_end_at | TIMESTAMPTZ | NOT NULL | 행사 종료 |
| sales_start_at | TIMESTAMPTZ | NOT NULL | 티켓 판매 시작 |

---

<!-- Page 10 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| sales_end_at | TIMESTAMPTZ | NOT NULL | 티켓 판매 종료 |
| desired_venue_id | BIGINT | FK -> VIRTUAL_VENUES.id, NULL | 희망 장소 |
| desired_venue_hall_id | BIGINT | FK -> VENUE_HALLS.id, NULL | 클라이언트가 신청 시 선택한 희망 홀 |
| desired_venue_zone_id | BIGINT | FK -> VENUE_ZONES.id, NULL | 클라이언트가 신청 시 선택한 희망 구역 |

DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED,
status VARCHAR(30) NOT NULL, CHECK
REJECTED, CANCELED

| submitted_at | TIMESTAMPTZ | NULL | 제출 일시 |
| --- | --- | --- | --- |
| reviewed_by_admin_id | BIGINT | FK -> USERS.id, NULL | 심사 관리자 |
| reviewed_at | TIMESTAMPTZ | NULL | 심사 일시 |
| rejection_reason | TEXT | NULL | 반려 사유 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

장소 선택 정책: 클라이언트는 박람회 개최 신청 시 희망 장소·홀·구역을 직접 선택합니다. 관리자는 승인 전에 선택값의 소속 관계와 기간 중복을 검증하며, 유효한 경우 동일 값으로
VENUE_RESERVATIONS 를 생성합니다.

### EXPOS

승인·공개되어 티켓 판매와 체크인의 기준이 되는 박람회 원본입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 박람회 ID |
| host_client_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 주최자 |
| opening_request_id | BIGINT | UNIQUE, FK -><br>EXPO_OPENING_REQUESTS.id, NULL | 개최 신청 1:1 |
| title | VARCHAR(255) | NOT NULL | 공식 명칭 |
| description | TEXT | NOT NULL | 소개 |
| region_code | VARCHAR(30) | NOT NULL | 검색 지역 |
| event_start_at | TIMESTAMPTZ | NOT NULL | 행사 시작 |

---

<!-- Page 11 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| event_end_at | TIMESTAMPTZ | NOT NULL | 행사 종료 |
| sales_start_at | TIMESTAMPTZ | NOT NULL | 판매 시작 |
| sales_end_at | TIMESTAMPTZ | NOT NULL | 판매 종료 |
| review_status | VARCHAR(20) | NOT NULL, CHECK | DRAFT, UNDER_REVIEW, REJECTED,<br>APPROVED |
| visibility_status | VARCHAR(20) | NOT NULL, CHECK | PRIVATE, PUBLIC, ARCHIVED |
| event_status | VARCHAR(20) | NOT NULL, CHECK | SCHEDULED, ONGOING, ENDED,<br>CANCELED |
| 컬럼명 | 타입 | 제약조건 | 설명 |
| approved_by_admin_id | BIGINT | FK -> USERS.id, NULL | 승인 관리자 |
| approved_at | TIMESTAMPTZ | NULL | 승인·자동 공개 일시 |
| rejection_reason | TEXT | NULL | 반려 사유 |
| canceled_at | TIMESTAMPTZ | NULL | 취소 확정 일시 |
| version | BIGINT | NOT NULL DEFAULT 0 | 낙관적 락 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

## EXPO_VENUE_ASSIGNMENTS

박람회와 확정 장소 예약의 1:1 연결만 관리합니다. 장소·홀·구역·기간과 중복 제약은 VENUE_RESERVATIONS 에서 단일 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 장소 배정 ID |
| expo_id | BIGINT | UNIQUE, FK -> EXPOS.id, NOT<br>NULL | 박람회 |
| venue_reservation_id | BIGINT | UNIQUE, FK -><br>VENUE_RESERVATIONS.id,<br>NOT NULL | 확정 장소 예약 |
| assigned_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 배정 관리자 |
| assigned_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 배정 시각 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |
| 예약 상태 제약 | - | 연결 대상<br>VENUE_RESERVATIONS.status | 확정 예약만 박람회에 연결 |

---

<!-- Page 12 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
|   |   | =CONFIRMED |   |
| 중복 저장 금지 | - | 장소·홀·구역·기간 컬럼 미보유 | 예약 원본과 값 중복 방지 |

### CATEGORIES

박람회 검색·필터용 카테고리 마스터입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 카테고리 ID |
| parent_id | BIGINT | FK -> CATEGORIES.id, NULL | 상위 카테고리 |
| name | VARCHAR(100) | UNIQUE, NOT NULL | 카테고리명 |
| slug | VARCHAR(100) | UNIQUE, NOT NULL | URL/검색 코드 |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| 컬럼명 | 타입 | 제약조건 | 설명 |
| active | BOOLEAN | NOT NULL DEFAULT TRUE | 사용 여부 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_CATEGORIES

박람회와 카테고리의 N:M 연결 테이블입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| expo_id | BIGINT | PK/FK -> EXPOS.id | 박람회 |
| category_id | BIGINT | PK/FK -> CATEGORIES.id | 카테고리 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 연결 일시 |

### EXPO_IMAGES

박람회 썸네일·상세 이미지를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 이미지 연결 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |

---

<!-- Page 13 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| file_id | BIGINT | FK -> FILE_METADATA.id, NOT NULL | 이미지 |
| image_type | VARCHAR(20) | NOT NULL | THUMBNAIL, DETAIL, GALLERY |
| alt_text | VARCHAR(255) | NULL | 대체 텍스트 |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| (expo_id, file_id) | - | UNIQUE | 중복 연결 방지 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_FILES

박람회 소개 PDF·카탈로그·리플렛·홍보영상 파일입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 파일 연결 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| file_id | BIGINT | FK -> FILE_METADATA.id, NOT NULL | 파일 |
| 컬럼명 | 타입 | 제약조건 | 설명 |

INTRO_PDF, CATALOG, LEAFLET, PROMO_VIDEO,
file_purpose VARCHAR(30) NOT NULL
OTHER

| title | VARCHAR(150) | NULL | 표시명 |
| --- | --- | --- | --- |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| (expo_id, file_id) | - | UNIQUE | 중복 연결 방지 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### EXTERNAL_LINKS

박람회 또는 부스 콘텐츠의 외부 링크를 공통 관리합니다.

---

<!-- Page 14 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 링크 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NULL | 박람회 링크 |
| booth_content_id | BIGINT | FK -> BOOTH_CONTENTS.id, NULL | 부스 링크 |

HOMEPAGE, SOCIAL, RESERVATION, PRODUCT,
link_type VARCHAR(30) NOT NULL
OTHER

| label | VARCHAR(100) | NULL | 표시 문구 |
| --- | --- | --- | --- |
| url | VARCHAR(1000) | NOT NULL | 외부 URL |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| owner_xor | - | CHECK | expo_id 와 booth_content_id 중 정확히 하<br>나만 NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_COMPANIES

박람회에 참여하는 기업과 배정 부스를 연결한 조회 기준 테이블입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 참여 기업 연결 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| client_user_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 참여 기업 |
| booth_allocation_id | BIGINT | FK -> BOOTH_ALLOCATIONS.id, NULL | 연결 부스 배정 |
| display_name | VARCHAR(150) | NOT NULL | 노출 기업명 |
| participation_status | VARCHAR(20) | NOT NULL, CHECK | CONFIRMED, CANCELED |
| 컬럼명 | 타입 | 제약조건 | 설명 |
| (expo_id, client_user_id) | - | UNIQUE | 박람회 내 기업 중복 방지 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |

---

<!-- Page 15 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_REVIEW_HISTORIES

박람회 심사 승인·반려 이력을 기록합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 심사 이력 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| reviewer_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 관리자 |
| decision | VARCHAR(20) | NOT NULL | SUBMIT, APPROVE, REJECT |
| reason | TEXT | NULL | 반려/처리 사유 |
| from_status | VARCHAR(20) | NULL | 변경 전 |
| to_status | VARCHAR(20) | NOT NULL | 변경 후 |
| reviewed_at | TIMESTAMPTZ | NOT NULL | 처리 일시 |

### EXPO_CHANGE_REQUESTS

승인된 박람회에 대한 클라이언트 수정 요청입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 수정 요청 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| requester_client_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 요청 클라이언트 |
| change_reason | TEXT | NOT NULL | 수정 사유 |
| requested_changes | JSONB | NOT NULL | 요청 항목과 값 |

SUBMITTED, UNDER_REVIEW, APPLIED, REJECTED,
status VARCHAR(20) NOT NULL, CHECK
CANCELED

| processed_by_admin_id | BIGINT | FK -> USERS.id, NULL | 처리 관리자 |
| --- | --- | --- | --- |
| processed_at | TIMESTAMPTZ | NULL | 처리 일시 |

---

<!-- Page 16 -->

| processed_by_admin_id | BIGINT | FK -> USERS.id, NULL | 처리 관리자 |
| --- | --- | --- | --- |
| rejection_reason | TEXT | NULL | 거절 사유 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### EXPO_CHANGE_HISTORIES

승인 후 관리자가 실제 적용한 박람회 변경 전·후 값을 저장합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 변경 이력 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| change_request_id | BIGINT | FK -> EXPO_CHANGE_REQUESTS.id, NULL | 원본 요청 |
| changed_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 변경 관리자 |
| before_data | JSONB | NOT NULL | 변경 전 |
| after_data | JSONB | NOT NULL | 변경 후 |
| reason | TEXT | NULL | 변경 사유 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 적용 일시 |

### EXPO_CANCELLATION_REQUESTS

클라이언트의 박람회 취소 요청과 관리자 승인 결과입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 취소 요청 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| requester_client_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 요청자 |
| reason | TEXT | NOT NULL | 취소 사유 |

SUBMITTED, APPROVED, REJECTED,
status VARCHAR(20) NOT NULL, CHECK
PROCESSING_REFUNDS, COMPLETED

---

<!-- Page 17 -->

| processed_by_admin_id | BIGINT | FK -> USERS.id, NULL | 관리자 |
| --- | --- | --- | --- |
| processed_at | TIMESTAMPTZ | NULL | 처리 일시 |
| rejection_reason | TEXT | NULL | 반려 사유 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

박람회 취소 승인 시 장소 예약 해제 처리: 관리자가 취소를 승인하면 EXPOS.event_status 를 CANCELED 로 변경하고 신규 티켓 판매를 중단합니다. 이후
EXPO_VENUE_ASSIGNMENTS.venue_reservation_id 로 연결된 VENUE_RESERVATIONS 를 조회하여 status 를 RELEASED 로 변경하고 released_at 을 기록합니다. 티켓 일괄 환불 등록과 장소
예약 해제는 같은 취소 처리 트랜잭션에서 수행하며, 이미 행사가 종료된 경우 등 운영상 재사용이 불가능한 예약은 별도 사유를 기록합니다.

---

<!-- Page 18 -->

## 6-4. 검색 및 화면 노출

공개된 박람회의 검색, 카테고리·지역·기간·가격·판매 상태 필터, 정렬과 카드형 목록 노출을 지원하는 조회 모델입니다.

| 구분 | 엔티티 / View |
| --- | --- |
| SQL View | V_PUBLIC_EXPO_CARDS |

### V_PUBLIC_EXPO_CARDS

홈·검색·필터·정렬 카드 목록

| 출력 필드 | 타입 | 원천/계산 | 화면 사용 목적 |
| --- | --- | --- | --- |
| expo_id | BIGINT | EXPOS.id | 박람회 ID |
| title | VARCHAR | EXPOS.title | 제목 |
| event_period | RANGE | EXPOS.event_start_at/end_at | 행사 기간 |
| region_code | VARCHAR | EXPOS.region_code | 지역 |
| minimum_price | NUMERIC | MIN(TICKET_PRODUCTS.price) | 최저가 |
| available_quantity | INTEGER | SUM(TICKET_INVENTORIES.available_qua<br>ntity) | 잔여 수량 |
| display_sales_status | VARCHAR | 판매 기간+재고 계산 | 판매 예정/판매중/매진/판매종료 |
| popularity_score | NUMERIC | 판매량·주문량 기반 계산 | 인기순 정렬 |

---

<!-- Page 19 -->

## 6-5. 티켓 및 재고

박람회별 표준 1 일권, 판매 기간·가격·수량, 재고 임시 확보와 결제 성공 후 확정 차감, 만료·환불 후 재고 복구를 관리합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | TICKET_PRODUCTS |
| 물리 테이블 | TICKET_INVENTORIES |
| 물리 테이블 | INVENTORY_RESERVATIONS |

### TICKET_PRODUCTS

박람회에서 판매하는 표준 1 일권 티켓 상품입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 티켓 상품 ID |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| name | VARCHAR(150) | NOT NULL | 티켓명 |
| description | TEXT | NULL | 설명 |
| price | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 판매가 |
| sales_start_at | TIMESTAMPTZ | NOT NULL | 판매 시작 |
| sales_end_at | TIMESTAMPTZ | NOT NULL | 판매 종료 |
| max_quantity_per_order | INTEGER | NOT NULL DEFAULT 4, CHECK 1..4 | 주문당 최대 수량 |

DRAFT, ON_SALE, SOLD_OUT, SALE_ENDED,
status VARCHAR(20) NOT NULL, CHECK
CANCELED

| version | BIGINT | NOT NULL DEFAULT 0 | 상품 수정 낙관적 락 |
| --- | --- | --- | --- |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### TICKET_INVENTORIES

티켓 상품의 총량·예약량·판매량을 동시성 안전하게 관리합니다.

---

<!-- Page 20 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 재고 ID |
| ticket_product_id | BIGINT | UNIQUE, FK -> TICKET_PRODUCTS.id,<br>NOT NULL | 상품 1:1 |
| total_quantity | INTEGER | NOT NULL, CHECK >= 0 | 총 판매 수량 |
| reserved_quantity | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 임시 확보 수량 |
| sold_quantity | INTEGER | NOT NULL DEFAULT 0, CHECK >= 0 | 결제 완료 수량 |
| available_quantity | INTEGER | GENERATED/계산 | total - reserved - sold |
| version | BIGINT | NOT NULL DEFAULT 0 | 낙관적 락 |
| updated_at | TIMESTAMPTZ | NOT NULL | 갱신 일시 |

### INVENTORY_RESERVATIONS

티켓 주문 결제 대기 동안 수량을 임시 확보합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 재고 확보 ID |
| ticket_product_id | BIGINT | FK -> TICKET_PRODUCTS.id, NOT NULL | 티켓 상품 |
| ticket_order_id | BIGINT | FK -> TICKET_ORDERS.id, NOT NULL | 주문 |
| quantity | INTEGER | NOT NULL, CHECK > 0 | 확보 수량 |
| status | VARCHAR(20) | NOT NULL, CHECK | ACTIVE, CONFIRMED, EXPIRED,<br>RELEASED |
| reserved_at | TIMESTAMPTZ | NOT NULL | 확보 시작 |
| expires_at | TIMESTAMPTZ | NOT NULL | 만료 |
| released_at | TIMESTAMPTZ | NULL | 반환 일시 |

---

<!-- Page 21 -->

## 6-6. 주문 및 결제

회원과 비회원의 티켓 주문, 결제 시도, 결제 승인 이력 및 전체 주문 환불을 관리합니다. 구매자는 티켓 판매원금에 3% 예매 수수료를 추가로 결제하며, 부분 취소는 지원하지 않습니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | TICKET_ORDERS |
| 물리 테이블 | TICKET_ORDER_ITEMS |
| 물리 테이블 | GUEST_ORDER_INFOS |
| 물리 테이블 | TICKET_PAYMENTS |
| 물리 테이블 | TICKET_PAYMENT_HISTORIES |
| 물리 테이블 | TICKET_REFUNDS |

### TICKET_ORDERS

주문 당시 판매원금과 예매 수수료를 스냅샷으로 보존합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 주문 ID |
| order_number | VARCHAR(40) | UNIQUE, NOT NULL | ORD+날짜+랜덤 형식 주문번호 |
| member_user_id | BIGINT | FK -> USERS.id, NULL | 회원 주문자, 비회원은 NULL |
| orderer_type | VARCHAR(20) | NOT NULL, CHECK MEMBER, GUEST | 주문자 유형 |
| ticket_subtotal_amount | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 티켓 단가 x 수량 판매원금 |
| booking_fee_rate | NUMERIC(6,5) | NOT NULL DEFAULT 0.03000 | 주문 당시 예매 수수료율 |
| booking_fee_amount | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | ticket_subtotal_amount x 3% |
| total_amount | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 판매원금 + 예매 수수료 |
| total_quantity | INTEGER | NOT NULL, CHECK 1..4 | 주문 총 수량 |
| status | VARCHAR(30) | NOT NULL, CHECK | PENDING, PAID, CANCELED,<br>PAYMENT_FAILED, EXPIRED |
| paid_at | TIMESTAMPTZ | NULL | 결제 완료 일시 |
| canceled_at | TIMESTAMPTZ | NULL | 전체 취소 일시 |
| created_at, updated_at | TIMESTAMPTZ | NOT NULL | 생성 및 수정 일시 |

### TICKET_ORDER_ITEMS

주문 시점의 티켓 상품, 단가, 수량을 저장합니다. item_subtotal_amount 는 unit_price x quantity 입니다.

---

<!-- Page 22 -->

### GUEST_ORDER_INFOS

비회원 이름, 휴대폰, 나이 및 조회 비밀번호 해시를 주문과 1:1 로 저장합니다. 조회 비밀번호 원문은 저장하지 않습니다.

### TICKET_PAYMENTS

결제 요청과 승인 결과를 관리합니다.

| 컬럼 / 규칙 | 최종 정의 |
| --- | --- |
| requested_amount | 티켓 판매원금과 예매 수수료가 포함된 최종 결제 요청 금액 |
| approved_amount | PG가 승인한 최종 결제 금액 |
| ticket_subtotal_amount | 승인 당시 티켓 판매원금 스냅샷 |
| booking_fee_amount | 승인 당시 3% 예매 수수료 스냅샷 |
| payment_key | PG 결제 식별키, UNIQUE |
| status | READY, IN_PROGRESS, DONE, FAILED, CANCELED |
| 검증 | approved_amount = TICKET_ORDERS.total_amount |

### TICKET_PAYMENT_HISTORIES

결제 상태 변경과 PG 응답 코드를 이력으로 저장하며 물리 삭제하지 않습니다.

### TICKET_REFUNDS

전체 주문 취소만 지원합니다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| refund_ticket_amount | NUMERIC(15,2) | 환불되는 티켓 판매원금 |
| refund_booking_fee_amount | NUMERIC(15,2) | 함께 환불되는 예매 수수료 |
| refund_amount | NUMERIC(15,2) | 두 금액의 합계 |
| status | VARCHAR(30) | REQUESTED, PROCESSING, COMPLETED, FAILED |

금액 검증식
- 구매자 결제액 = ticket_subtotal_amount + booking_fee_amount
- booking_fee_rate = 0.03000
- refund_amount = refund_ticket_amount + refund_booking_fee_amount

---

<!-- Page 23 -->

## 6-7. 발권 및 QR 체크인

결제 완료 후 수량별 발권 티켓과 QR 토큰을 생성하고, 클라이언트가 본인 박람회의 QR 또는 티켓 코드를 검증하여 1 회 체크인 처리합니다. 체크인 취소·복구 기능은 구현하지 않습니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | ISSUED_TICKETS |
| 물리 테이블 | TICKET_ACCESS_TOKENS |
| 물리 테이블 | CHECK_IN_HISTORIES |

### ISSUED_TICKETS

결제 완료 후 구매 수량만큼 발급되는 개별 입장권입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 발권 티켓 ID |
| ticket_order_item_id | BIGINT | FK -> TICKET_ORDER_ITEMS.id, NOT NULL | 주문 항목 |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 검증 대상 박람회 |
| ticket_code | VARCHAR(50) | UNIQUE, NOT NULL | EXPO-생성일자-일련번호 6 자리 |
| qr_token_hash | VARCHAR(255) | UNIQUE, NOT NULL | QR 원문 대신 서버 검증용 해시 |
| status | VARCHAR(20) | NOT NULL, CHECK | ISSUED, CHECKED_IN, CANCELED,<br>INVALIDATED |
| issued_at | TIMESTAMPTZ | NOT NULL | 발권 일시 |
| checked_in_at | TIMESTAMPTZ | NULL | 입장 일시 |
| invalidated_at | TIMESTAMPTZ | NULL | 무효 일시 |
| version | BIGINT | NOT NULL DEFAULT 0 | 중복 체크인 방지 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### TICKET_ACCESS_TOKENS

카카오 메시지로 전송하는 QR 확인 보안 URL 의 단기·폐기 가능한 접근 토큰입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 접근 토큰 ID |
| ticket_order_id | BIGINT | FK -> TICKET_ORDERS.id, NOT NULL | 조회 주문 |

---

<!-- Page 24 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| issued_ticket_id | BIGINT | FK -> ISSUED_TICKETS.id, NULL | 특정 티켓 링크 |
| token_hash | VARCHAR(255) | UNIQUE, NOT NULL | 접근 토큰 해시 |
| scope | VARCHAR(30) | CHECK: ORDER_VIEW, QR_VIEW | 접근 범위 |
| status | VARCHAR(20) | CHECK: ACTIVE, EXPIRED, REVOKED | 상태 |
| expires_at | TIMESTAMPTZ | NOT NULL | 만료 |
| last_accessed_at | TIMESTAMPTZ | NULL | 마지막 접근 |
| access_count | INTEGER | DEFAULT 0 | 접근 횟수 |
| revoked_at | TIMESTAMPTZ | NULL | 폐기 일시 |
| created_at | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

### CHECK_IN_HISTORIES

QR 또는 티켓 코드 검증·입장 처리 결과를 저장합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 체크인 이력 ID |
| issued_ticket_id | BIGINT | FK -> ISSUED_TICKETS.id, NOT NULL | 티켓 |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 박람회 |
| processed_by_client_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 체크인 처리 클라이언트 |
| method | VARCHAR(20) | NOT NULL, CHECK | QR, MANUAL_CODE |

SUCCESS, ALREADY_USED, CANCELED_TICKET,
result VARCHAR(30) NOT NULL, CHECK
WRONG_EXPO, INVALID_TOKEN

| checked_at | TIMESTAMPTZ | NOT NULL | 검증/처리 일시 |
| --- | --- | --- | --- |
| request_ip | VARCHAR(45) | NULL | 요청 IP |
| detail | TEXT | NULL | 오류 상세 |

---

<!-- Page 25 -->

## 6-8. 카카오 알림

결제·환불·박람회 취소 안내와 QR 접근 URL 의 카카오 메시지 발송 요청, 성공·실패 및 재시도 이력을 관리합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | NOTIFICATIONS |
| 물리 테이블 | KAKAO_MESSAGE_HISTORIES |

### NOTIFICATIONS

채널 독립적인 알림 작업과 발송 상태입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 알림 ID |
| recipient_user_id | BIGINT | FK -> USERS.id, NULL | 회원 수신자 |
| recipient_phone_number | VARCHAR(20) | NULL | 비회원/카카오 수신 번호 |
| channel | VARCHAR(20) | NOT NULL, CHECK | KAKAO, EMAIL, IN_APP |
| template_code | VARCHAR(50) | NOT NULL | 알림 템플릿 코드 |
| reference_type | VARCHAR(30) | NULL | ORDER, PAYMENT, REFUND, EXPO,<br>TICKET 등 |
| reference_id | BIGINT | NULL | 참조 데이터 ID |
| payload | JSONB | NOT NULL | 템플릿 변수 |
| status | VARCHAR(20) | NOT NULL, CHECK | PENDING, SENT, FAILED, RETRYING,<br>CANCELED |
| scheduled_at | TIMESTAMPTZ | NULL | 예약 발송 |
| sent_at | TIMESTAMPTZ | NULL | 성공 일시 |
| retry_count | INTEGER | NOT NULL DEFAULT 0 | 재시도 횟수 |
| last_error | TEXT | NULL | 최근 오류 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### KAKAO_MESSAGE_HISTORIES

카카오 알림톡 발송 시도별 외부 시스템 응답입니다.

---

<!-- Page 26 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 카카오 발송 이력 ID |
| notification_id | BIGINT | FK -> NOTIFICATIONS.id, NOT NULL | 알림 |
| provider_message_id | VARCHAR(200) | NULL | 카카오/대행사 메시지 ID |
| status | VARCHAR(20) | NOT NULL, CHECK | REQUESTED, SENT, DELIVERED, FAILED |
| request_payload | JSONB | NULL | 요청 데이터 |
| response_payload | JSONB | NULL | 응답 데이터 |
| error_code | VARCHAR(100) | NULL | 실패 코드 |
| attempt_no | INTEGER | NOT NULL | 시도 번호 |
| requested_at | TIMESTAMPTZ | NOT NULL | 요청 일시 |
| completed_at | TIMESTAMPTZ | NULL | 완료 일시 |
| (notification_id, attempt_no) | - | UNIQUE | 알림별 시도 번호 중복 방지 |

---

<!-- Page 27 -->

## 6-9. 클라이언트 마이페이지 및 기능

클라이언트는 프로필 이미지 등록·변경·삭제, 닉네임 수정, 본인이 등록한 박람회, 일별 매출, 기업 모집, 부스 신청 및 정산 리포트를 조회합니다. 프로필 이미지는 일반 회원과 동일하게
USERS.profile_image_file_id 를 사용하며 정산 금액의 직접 수정 또는 확정 권한은 없습니다.

### V_CLIENT_DASHBOARD_PROFILE

클라이언트 페이지 상단 프로필 영역에서 계정 이미지와 사업자 기본 정보를 함께 표시합니다.

| 출력/입력 필드 | 타입 | 원천/처리 | 화면 사용 목적 |
| --- | --- | --- | --- |
| client_user_id | BIGINT | USERS.id = CLIENT_PROFILES.user_id | 클라이언트 식별 |
| nickname | VARCHAR | USERS.nickname | 닉네임 표시·변경 |
| company_name | VARCHAR | CLIENT_PROFILES.company_name | 기업명 표시 |
| profile_image_file_id | BIGINT | USERS.profile_image_file_id -><br>FILE_METADATA.id | 클라이언트 프로필 이미지 |
| profile_image_url | VARCHAR | FILE_METADATA.storage_key 기반 서명<br>URL 생성 | 프로필 이미지 표시 |
| profile_image_updated_at | TIMESTAMPTZ | USERS.profile_image_updated_at | 이미지 변경 일시 |

클라이언트 프로필 이미지 API 는 일반 회원과 동일한 공통 API 를 사용합니다: GET /api/users/me/profile, POST /api/users/me/profile-image, DELETE /api/users/me/profile-
image.
SQL View / 프로필 조회 모델
- V_CLIENT_DASHBOARD_PROFILE: 프로필 이미지, 닉네임, 기업명
- V_CLIENT_DASHBOARD_EXPOS: 본인 등록 박람회 목록, 심사 및 노출 상태
- V_CLIENT_DASHBOARD_DAILY_SALES: 박람회별 일자별 티켓 판매 및 환불 현황
- V_CLIENT_DASHBOARD_RECRUITMENT: 모집 공고, 신청 완료 수, 확정 배정 수
- V_CLIENT_DASHBOARD_BOOTHS: 신청 상태, 부스 주문, 결제 상태, 결제 완료 시각, 확정 부스
- V_CLIENT_DASHBOARD_SETTLEMENTS: 박람회별 최종 정산 리포트
V_CLIENT_DASHBOARD_SETTLEMENTS 필수 출력 필드
expo_id, expo_title, event_end_at, settlement_due_at, status,
gross_ticket_sales_amount, ticket_refund_amount, net_ticket_sales_amount,
booking_fee_gross_amount, booking_fee_refund_amount, booking_fee_net_amount,
gross_booth_sales_amount, adjustment_amount, remittance_due_amount,
remitted_amount, remitted_at, remittance_status, latest_report_file
권한 규칙
- 클라이언트는 본인 소유 박람회의 데이터만 조회합니다.
- 예매 수수료는 플랫폼 수익으로 별도 표시하며 클라이언트 송금액에서 다시 차감하지 않습니다.
- 정산 리포트는 PDF 또는 엑셀로 다운로드할 수 있습니다.

---

<!-- Page 28 -->

## 6-10. 정산 및 회계

행사 종료 후 7~14 일 이내 정산 대상을 생성하고, 금액 자동 계산, 관리자 검토, 조정, 확정, 외부 송금 및 결과 기록을 처리합니다. 은행 자동 송금은 MVP 에 포함하지 않습니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | EXPO_DAILY_SALES_SUMMARIES |
| 물리 테이블 | SETTLEMENTS |
| 물리 테이블 | SETTLEMENT_ITEMS |
| 물리 테이블 | SETTLEMENT_ADJUSTMENTS |
| 물리 테이블 | REMITTANCES |
| 물리 테이블 | SETTLEMENT_REPORTS |
| SQL View | V_CLIENT_DASHBOARD_SETTLEMENTS |
| SQL View | V_ADMIN_SETTLEMENT_STATUS |

### SETTLEMENTS

박람회 단위 정산 대표 정보입니다.

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
| gross_ticket_sales_amount | NUMERIC(15,2) | 환불 전 티켓 판매원금 합계 |
| ticket_refund_amount | NUMERIC(15,2) | 환불된 티켓 판매원금 |
| net_ticket_sales_amount | NUMERIC(15,2) | gross_ticket_sales_amount - ticket_refund_amount |
| booking_fee_gross_amount | NUMERIC(15,2) | 구매자에게 부과한 예매 수수료 합계 |
| booking_fee_refund_amount | NUMERIC(15,2) | 취소 주문에서 환불한 예매 수수료 |
| booking_fee_net_amount | NUMERIC(15,2) | 플랫폼 예매 수수료 순수익 |
| gross_booth_sales_amount | NUMERIC(15,2) | 결제 완료 부스비 합계 |
| pg_fee_reference_amount | NUMERIC(15,2) | 회계 참고값, 클라이언트 송금액에서 추가 차감하지 않음 |
| adjustment_amount | NUMERIC(15,2) | 관리자가 확정한 수동 조정 금액 |
| remittance_due_amount | NUMERIC(15,2) | net_ticket_sales_amount + gross_booth_sales_amount<br>+/- adjustment_amount |
| settlement_due_at | TIMESTAMPTZ | 행사 종료 후 7~14일 이내 |
| confirmed_at | TIMESTAMPTZ | 정산 확정 일시 |
| confirmed_by | BIGINT | FK -> USERS.id, 확정 관리자 |
| status | VARCHAR(30) | WAITING, CALCULATED, UNDER_REVIEW, CONFIRMED, |

---

<!-- Page 29 -->

| 컬럼명 | 타입 | 설명 |
| --- | --- | --- |
|   |   | REMITTANCE_PENDING, REMITTED, ON_HOLD |

SETTLEMENT_ITEMS.item_type
TICKET_SALE, TICKET_REFUND, BOOKING_FEE, BOOKING_FEE_REFUND, BOOTH_SALE, PG_FEE_REFERENCE, ADJUSTMENT

### SETTLEMENT_ADJUSTMENTS

조정 금액, 사유, 처리자 및 처리 일시를 저장합니다.

### REMITTANCES

| 컬럼 / 규칙 | 최종 정의 |
| --- | --- |
| settlement_id | 정산 헤더 FK |
| scheduled_at | 예정 송금일 |
| remitted_amount | 외부에서 실제 송금한 금액 |
| remitted_at | 실제 송금 일시 |
| status | PENDING, PROCESSING, REMITTED, FAILED, CANCELED |
| reference_number | 외부 송금 확인번호 또는 거래 식별값 |
| memo | 관리자 송금 메모 |
| processed_by | 송금 결과를 기록한 관리자 FK |

### SETTLEMENT_REPORTS

PDF 또는 엑셀 정산 리포트 파일, 생성 상태 및 생성 일시를 저장합니다.
권장 API

| GET | /api/client/settlements |
| --- | --- |
| GET | /api/client/settlements/{id} |
| GET | /api/client/settlements/{id}/report |
| GET | /api/admin/settlements |
| GET | /api/admin/settlements/{id} |

PATCH /api/admin/settlements/{id}/adjustment
POST /api/admin/settlements/{id}/confirm
POST /api/admin/settlements/{id}/remittance
POST /api/admin/settlements/{id}/reports

---

<!-- Page 30 -->

처리 흐름
행사 종료 -> 정산 대상 자동 생성 -> 금액 자동 계산 -> 관리자 검토 -> 필요 시 조정 -> 정산 확정 -> 외부 송금 -> 송금 결과 기록 -> 클라이언트 조회 및 리포트 다운로드

---

<!-- Page 31 -->

## 6-11. 광고 배너

클라이언트의 배너 신청, 관리자 승인·반려, 승인 후 실제 배너 자동 생성과 예약·노출·종료 상태를 관리합니다. MVP 에서는 광고비 결제를 제외합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| 물리 테이블 | BANNER_SLOTS |
| 물리 테이블 | BANNER_APPLICATIONS |
| 물리 테이블 | BANNERS |
| 물리 테이블 | BANNER_REVIEW_HISTORIES |

### BANNER_SLOTS

메인 화면 등 배너 노출 위치와 최대 동시 노출 수를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 배너 슬롯 ID |
| slot_code | VARCHAR(50) | UNIQUE, NOT NULL | MAIN_TOP 등 |
| name | VARCHAR(100) | NOT NULL | 슬롯명 |
| max_active_count | INTEGER | NOT NULL, CHECK > 0 | 동시 노출 가능 수 |
| width_px | INTEGER | NULL | 권장 너비 |
| height_px | INTEGER | NULL | 권장 높이 |
| active | BOOLEAN | NOT NULL DEFAULT TRUE | 사용 여부 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### BANNER_APPLICATIONS

클라이언트의 배너 신청과 심사 상태입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 배너 신청 ID |
| client_user_id | BIGINT | FK -> CLIENT_PROFILES.user_id, NOT<br>NULL | 신청 클라이언트 |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 연결 박람회 |
| image_file_id | BIGINT | FK -> FILE_METADATA.id, NOT NULL | 배너 이미지 |

---

<!-- Page 32 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| headline | VARCHAR(150) | NULL | 배너 문구 |
| requested_start_at | TIMESTAMPTZ | NOT NULL | 희망 시작 |
| requested_end_at | TIMESTAMPTZ | NOT NULL | 희망 종료 |

DRAFT, UNDER_REVIEW, REJECTED, APPROVED,
review_status VARCHAR(20) NOT NULL, CHECK
CANCELED

| submitted_at | TIMESTAMPTZ | NULL | 신청 제출 |
| --- | --- | --- | --- |
| reviewed_by_admin_id | BIGINT | FK -> USERS.id, NULL | 심사 관리자 |
| reviewed_at | TIMESTAMPTZ | NULL | 심사 일시 |
| rejection_reason | TEXT | NULL | 반려 사유 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### BANNERS

승인과 동시에 자동 생성되는 실제 노출 배너입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 배너 ID |
| banner_application_id | BIGINT | UNIQUE, FK -><br>BANNER_APPLICATIONS.id, NOT NULL | 승인 신청 1:1 |
| banner_slot_id | BIGINT | FK -> BANNER_SLOTS.id, NOT NULL | 노출 슬롯 |
| expo_id | BIGINT | FK -> EXPOS.id, NOT NULL | 랜딩 박람회 |
| image_file_id | BIGINT | FK -> FILE_METADATA.id, NOT NULL | 실제 이미지 |
| headline | VARCHAR(150) | NULL | 노출 문구 |
| start_at | TIMESTAMPTZ | NOT NULL | 노출 시작 |
| end_at | TIMESTAMPTZ | NOT NULL | 노출 종료 |
| display_status | VARCHAR(20) | NOT NULL, CHECK | SCHEDULED, ACTIVE, ENDED, CANCELED |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 좌→우 노출 순서 |

---

<!-- Page 33 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| activated_at | TIMESTAMPTZ | NULL | 활성화 일시 |
| 컬럼명 | 타입 | 제약조건 | 설명 |
| ended_at | TIMESTAMPTZ | NULL | 종료 처리 일시 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 일시 |

### BANNER_REVIEW_HISTORIES

배너 신청 승인·반려 이력입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 심사 이력 ID |
| banner_application_id | BIGINT | FK -> BANNER_APPLICATIONS.id, NOT<br>NULL | 신청 |
| reviewer_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 관리자 |
| decision | VARCHAR(20) | NOT NULL | SUBMIT, APPROVE, REJECT, CANCEL |
| reason | TEXT | NULL | 사유 |
| from_status | VARCHAR(20) | NULL | 변경 전 |
| to_status | VARCHAR(20) | NOT NULL | 변경 후 |
| reviewed_at | TIMESTAMPTZ | NOT NULL | 처리 일시 |

---

<!-- Page 34 -->

## 6-12. 관리자 대시보드 및 운영

박람회·배너 심사, 모집공고 생성 요청 및 장소 충돌 검토, 참여 신청 운영 확인, 결제·환불·정산 상태를 통합 조회합니다.

| 구분 | 엔티티 / View |
| --- | --- |
| SQL View | V_ADMIN_DASHBOARD_COUNTS |
| SQL View | V_ADMIN_PENDING_REVIEWS |
| SQL View | V_ADMIN_VENUE_CONFLICTS |
| SQL View | V_ADMIN_PARTICIPATION_OPERATIONS |
| SQL View | V_ADMIN_PAYMENT_REFUND_STATUS |
| SQL View | V_ADMIN_SETTLEMENT_STATUS |

## V_ADMIN_PARTICIPATION_OPERATIONS

| 출력 필드 | 원천/계산 | 용도 |
| --- | --- | --- |
| application_id | PARTICIPATION_APPLICATIONS.id | 참여 신청 식별 |
| recruitment_notice_id | PARTICIPATION_APPLICATIONS.recruitment<br>_notice_id | 대상 모집공고 |
| client_user_id / company_name | PARTICIPATION_APPLICATIONS.client_user<br>_id / company_name_snapshot | 참여 기업 |
| application_status | PARTICIPATION_APPLICATIONS.status | 신청 상태 |
| selected_booth_product_id | PARTICIPATION_APPLICATIONS.selected_b<br>ooth_product_id | 선택 부스 |
| booth_order_id / booth_order_status | BOOTH_ORDERS.id / status | 부스 주문 상태 |
| payment_status | 최신 BOOTH_PAYMENTS.status | 결제 상태 |
| booth_allocation_id / allocation_status | BOOTH_ALLOCATIONS.id / status | 확정 배정 상태 |
| admin_checked_at / admin_checked_by | PARTICIPATION_APPLICATIONS 운영 확인<br>컬럼 | 운영 확인 |
| latest_operation_type / message | 최신<br>APPLICATION_OPERATION_HISTORIES | 최근 보완 요청·운영 메모 |
| created_at / submitted_at | PARTICIPATION_APPLICATIONS | 신청 생성·완료 시각 |

## V_ADMIN_VENUE_CONFLICTS

| 출력 필드 | 원천/계산 | 용도 |
| --- | --- | --- |
| request_id | RECRUITMENT_NOTICE_REQUESTS.id | 요청 식별 |
| host_client_id | RECRUITMENT_NOTICE_REQUESTS.host_<br>client_id | 요청 주최자 |
| virtual_venue_id / hall_id / zone_id | 요청 장소 필드 | 충돌 공간 |
| use_start_at / use_end_at | 요청 사용 기간 | 충돌 기간 |
| submitted_at | 요청 제출 시각 | 선착순 정렬 기준 |
| conflict_group_key | 서버 계산 | 동일 충돌군 묶기 |
| venue_decision | PENDING / ALLOWED / CANCELED | 운영자 결정 |
| decided_by_admin_id / decided_at | 운영 결정 정보 | 감사 추적 |
| cancellation_reason | 운영자 입력 | 취소 사유 |

---

<!-- Page 35 -->

## 장소 중복 운영 규칙

- 중복 요청은 등록 단계에서 자동 삭제하지 않고 모두 SUBMITTED 상태로 저장합니다.
- 시스템은 같은 장소·홀·구역에서 사용 기간이 겹치는 요청을 하나의 충돌군으로 표시합니다.
- 관리자 화면은 submitted_at 오름차순으로 정렬하여 먼저 제출된 요청을 최상단에 표시합니다.
- 운영자는 원칙적으로 먼저 제출된 요청을 ALLOWED 처리하고, 동일 충돌군의 나머지 요청을 CANCELED 처리합니다.
- 운영상 예외가 필요한 경우에도 운영자가 직접 대상을 선택할 수 있으며, 결정 사유를 필수 기록합니다.
- ALLOWED 처리 시 VENUE_RESERVATIONS 를 생성합니다. 유효 예약끼리는 DB EXCLUDE 제약으로 겹침을 금지합니다.

## 6-13. 기업 모집 공고 및 참여 신청

주최 클라이언트가 모집공고 생성을 요청하고, 운영자가 장소 중복을 검토하여 허용된 요청을 바탕으로 공고를 작성·게시합니다. 참여 기업은 공고당 부스 1 개를 선택하여 결제하며, 결제 성공 즉시 신
청 완료와 부스 확정 배정이 이루어집니다.

| 구분 | 엔티티 |
| --- | --- |
| 물리 테이블 | RECRUITMENT_NOTICE_REQUESTS |
| 물리 테이블 | RECRUITMENT_NOTICE_REQUEST_HISTORIES |
| 물리 테이블 | VENUE_RESERVATIONS |
| 물리 테이블 | RECRUITMENT_NOTICES |
| 물리 테이블 | RECRUITMENT_NOTICE_HISTORIES |
| 물리 테이블 | PARTICIPATION_APPLICATIONS |
| 물리 테이블 | APPLICATION_OPERATION_HISTORIES |
| 물리 테이블 | RECRUITMENT_RESULTS |
| 물리 테이블 | RECRUITMENT_RESULT_ITEMS |

## RECRUITMENT_NOTICE_REQUESTS

주최 클라이언트의 모집공고 생성 요청과 장소 중복 운영 결정을 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 요청 ID |
| host_client_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 주최 클라이언트 |
| title | VARCHAR(255) | NOT NULL | 요청 제목 |
| description | TEXT | NOT NULL | 행사·모집 개요 |
| application_start_at | TIMESTAMPTZ | NOT NULL | 모집 시작 |
| application_end_at | TIMESTAMPTZ | NOT NULL | 모집 종료 |
| event_start_at | TIMESTAMPTZ | NOT NULL | 행사 시작 |
| event_end_at | TIMESTAMPTZ | NOT NULL | 행사 종료 |
| virtual_venue_id | BIGINT | FK -> VIRTUAL_VENUES.id,<br>NOT NULL | 요청 장소 |
| venue_hall_id | BIGINT | FK -> VENUE_HALLS.id, NULL | 요청 홀 |
| venue_zone_id | BIGINT | FK -> VENUE_ZONES.id, NULL | 요청 구역 |
| target_company_count | INTEGER | NULL, CHECK > 0 | 목표 기업 수 |

---

<!-- Page 36 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| requested_booth_config | JSONB | NULL | 요청 부스 구성 |
| status | VARCHAR(30) | CHECK DRAFT, SUBMITTED,<br>UNDER_REVIEW, APPROVED,<br>REJECTED, CANCELED | 요청 처리 상태 |
| venue_conflict_status | VARCHAR(30) | CHECK CLEAR,<br>CONFLICT_PENDING,<br>RESOLVED | 장소 충돌 상태 |
| venue_decision | VARCHAR(20) | CHECK PENDING, ALLOWED,<br>CANCELED | 운영자 장소 결정 |
| conflict_group_key | VARCHAR(100) | NULL, INDEX | 충돌 요청 묶음 키 |
| submitted_at | TIMESTAMPTZ | NULL | 제출 시각; 선착순 기준 |
| decided_by_admin_id | BIGINT | FK -> USERS.id, NULL | 장소 결정 관리자 |
| decided_at | TIMESTAMPTZ | NULL | 장소 결정 시각 |
| decision_reason | TEXT | NULL | 허용·취소 사유 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |

## RECRUITMENT_NOTICE_REQUEST_HISTORIES

요청 제출, 검토, 장소 허용·취소 및 공고 생성 연결 이력을 저장합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 이력 ID |
| request_id | BIGINT | FK -><br>RECRUITMENT_NOTICE_REQU<br>ESTS.id, NOT NULL | 대상 요청 |
| action_type | VARCHAR(30) | CHECK SUBMIT,<br>REVIEW_START,<br>VENUE_ALLOW,<br>VENUE_CANCEL, APPROVE,<br>REJECT, NOTICE_CREATED | 처리 유형 |
| from_status | VARCHAR(30) | NULL | 변경 전 |
| to_status | VARCHAR(30) | NOT NULL | 변경 후 |
| reason | TEXT | NULL | 처리 사유 |
| processed_by_admin_id | BIGINT | FK -> USERS.id, NULL | 처리 관리자 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 처리 시각 |

## VENUE_RESERVATIONS

모집공고 경로와 일반 박람회 등록 경로를 모두 포함하는 단일 장소 예약 원본입니다. 모든 확정 장소·기간은 이 테이블에 먼저 저장하며, 기간 중복 제약도 이 테이블에서만 적용합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 장소 예약 ID |
| reservation_source_type | VARCHAR(30) | CHECK<br>RECRUITMENT_NOTICE, | 예약 생성 경로 |

---

<!-- Page 37 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
|   |   | EXPO_DIRECT |   |
| notice_request_id | BIGINT | UNIQUE, FK -><br>RECRUITMENT_NOTICE_REQU<br>ESTS.id, NULL | 모집공고 경로 요청 |
| opening_request_id | BIGINT | UNIQUE, FK -><br>EXPO_OPENING_REQUESTS.id<br>, NULL | 일반 박람회 등록 경로 요청 |
| recruitment_notice_id | BIGINT | FK -><br>RECRUITMENT_NOTICES.id,<br>NULL | 생성 공고 |
| virtual_venue_id | BIGINT | FK -> VIRTUAL_VENUES.id,<br>NOT NULL | 장소 |
| venue_hall_id | BIGINT | FK -> VENUE_HALLS.id, NULL | 홀 |
| venue_zone_id | BIGINT | FK -> VENUE_ZONES.id, NULL | 구역 |
| use_start_at | TIMESTAMPTZ | NOT NULL | 사용 시작 |
| use_end_at | TIMESTAMPTZ | NOT NULL | 사용 종료 |
| status | VARCHAR(20) | CHECK CONFIRMED,<br>RELEASED, CANCELED | 예약 상태 |
| confirmed_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 확정 관리자 |
| confirmed_at | TIMESTAMPTZ | NOT NULL | 확정 시각 |
| released_at | TIMESTAMPTZ | NULL | 해제 시각 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |
| 예약 원천 제약 | - | 둘 중 정확히 하나만 NOT NULL | notice_request_id 또는<br>opening_request_id 중 하나만 사<br>용 |
| 기간 중복 제약 | - | EXCLUDE USING gist (동일 장<br>소/홀/구역 + tstzrange 겹침)<br>WHERE status=CONFIRMED | 모든 경로의 확정 예약 중복 방지 |

## RECRUITMENT_NOTICES

허용된 장소 요청을 기준으로 관리자가 작성·게시하는 기업 모집공고입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 공고 ID |
| request_id | BIGINT | UNIQUE, FK -><br>RECRUITMENT_NOTICE_REQU<br>ESTS.id, NOT NULL | 원본 요청 |
| host_client_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 담당 주최 클라이언트 |
| venue_reservation_id | BIGINT | UNIQUE, FK -><br>VENUE_RESERVATIONS.id,<br>NOT NULL | 확정 장소 예약 |
| title | VARCHAR(255) | NOT NULL | 공고 제목 |
| content | TEXT | NOT NULL | 공고 내용 |

---

<!-- Page 38 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| eligibility | TEXT | NULL | 참여 조건 |
| submission_requirements | JSONB | NULL | 제출 자료 |
| application_start_at | TIMESTAMPTZ | NOT NULL | 신청 시작 |
| application_end_at | TIMESTAMPTZ | NOT NULL | 신청 종료 |
| status | VARCHAR(20) | CHECK DRAFT, SCHEDULED,<br>OPEN, CLOSED, CANCELED,<br>ARCHIVED | 공고 상태 |
| published_at | TIMESTAMPTZ | NULL | 게시 시각 |
| closed_at | TIMESTAMPTZ | NULL | 마감 시각 |
| created_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 작성 관리자 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |

## RECRUITMENT_NOTICE_HISTORIES

공고 작성·게시·수정·마감·취소 이력입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 이력 ID |
| recruitment_notice_id | BIGINT | FK -><br>RECRUITMENT_NOTICES.id,<br>NOT NULL | 대상 공고 |
| action_type | VARCHAR(20) | CHECK CREATE, PUBLISH,<br>UPDATE, CLOSE, CANCEL,<br>ARCHIVE | 처리 유형 |
| before_data | JSONB | NULL | 변경 전 |
| after_data | JSONB | NULL | 변경 후 |
| reason | TEXT | NULL | 사유 |
| processed_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 관리자 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 처리 시각 |

## PARTICIPATION_APPLICATIONS

참여 기업의 신청서와 선택 부스를 저장합니다. 하나의 기업은 공고 1 건에서 부스 1 개만 선택할 수 있습니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 참여 신청 ID |
| recruitment_notice_id | BIGINT | FK -><br>RECRUITMENT_NOTICES.id,<br>NOT NULL | 공고 |
| client_user_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 참여 기업 |
| company_name_snapshot | VARCHAR(150) | NOT NULL | 신청 당시 기업명 |

---

<!-- Page 39 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| participation_purpose | TEXT | NULL | 참여 목적 |
| exhibit_description | TEXT | NULL | 전시 내용 |
| selected_booth_product_id | BIGINT | FK -> BOOTH_PRODUCTS.id,<br>NULL | 선택 부스 상품 1개 |
| booth_order_id | BIGINT | UNIQUE, FK -><br>BOOTH_ORDERS.id, NULL | 부스 주문 |
| status | VARCHAR(30) | CHECK DRAFT,<br>PAYMENT_PENDING,<br>SUBMITTED,<br>PAYMENT_FAILED, CANCELED | 신청 상태 |
| submitted_at | TIMESTAMPTZ | NULL | 결제 성공 신청 완료 시각 |
| admin_checked_at | TIMESTAMPTZ | NULL | 운영 확인 시각 |
| admin_checked_by | BIGINT | FK -> USERS.id, NULL | 운영 확인 관리자 |
| admin_memo | TEXT | NULL | 운영 메모 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |
| 공고별 기업 제약 | - | UNIQUE(recruitment_notice_id,<br>client_user_id) | 기업당 공고 1회 신청 |
| 부스 단수 제약 | - | selected_booth_product_id 단일<br>FK | 공고당 부스 1개만 선택 |

## APPLICATION_OPERATION_HISTORIES

참여 신청 승인·반려 이력이 아니라 운영 확인과 보완 요청 이력을 저장합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 운영 이력 ID |
| application_id | BIGINT | FK -><br>PARTICIPATION_APPLICATION<br>S.id, NOT NULL | 대상 신청 |
| action_type | VARCHAR(30) | CHECK CHECKED,<br>CORRECTION_REQUESTED,<br>CORRECTION_COMPLETED,<br>MEMO_UPDATED | 운영 처리 |
| message | TEXT | NULL | 운영 메모·보완 내용 |
| processed_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 처리 관리자 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 처리 시각 |

## RECRUITMENT_RESULTS

모집 마감 후 결제 완료 및 부스 확정 기업을 집계한 결과 스냅샷입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 모집 결과 ID |
| recruitment_notice_id | BIGINT | UNIQUE, FK -> | 대상 공고 |

---

<!-- Page 40 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
|   |   | RECRUITMENT_NOTICES.id,<br>NOT NULL |   |
| host_client_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 결과 수신 주최자 |
| confirmed_company_count | INTEGER | NOT NULL DEFAULT 0 | 확정 기업 수 |
| confirmed_booth_count | INTEGER | NOT NULL DEFAULT 0 | 확정 부스 수 |
| total_booth_sales_amount | NUMERIC(15,2) | NOT NULL DEFAULT 0 | 부스 매출 합계 |
| status | VARCHAR(20) | CHECK GENERATED,<br>DELIVERED, CONFIRMED,<br>USED_FOR_EXPO, CANCELED | 결과 상태 |
| generated_at | TIMESTAMPTZ | NOT NULL | 생성 시각 |
| delivered_at | TIMESTAMPTZ | NULL | 주최자 전달 시각 |
| confirmed_by_host_at | TIMESTAMPTZ | NULL | 주최자 확인 시각 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |

## RECRUITMENT_RESULT_ITEMS

모집 결과에 포함되는 결제·배정 완료 기업별 항목입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 결과 항목 ID |
| recruitment_result_id | BIGINT | FK -><br>RECRUITMENT_RESULTS.id,<br>NOT NULL | 모집 결과 |
| application_id | BIGINT | UNIQUE, FK -><br>PARTICIPATION_APPLICATION<br>S.id, NOT NULL | 참여 신청 |
| client_user_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 참여 기업 |
| booth_allocation_id | BIGINT | UNIQUE, FK -><br>BOOTH_ALLOCATIONS.id, NOT<br>NULL | 확정 부스 |
| booth_amount | NUMERIC(15,2) | NOT NULL | 결제 부스비 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |

## 6-14. 가상 장소 및 부스 프리뷰

기존 VIRTUAL_VENUES, VENUE_HALLS, VENUE_ZONES, BOOTHS, BOOTH_TEMPLATES, BOOTH_PRODUCTS 정의를 유지합니다. 장소 기간 중복의 단일 기준은 VENUE_RESERVATIONS
입니다.

---

<!-- Page 41 -->

## 장소 예약 단일화 정책

| 등록 경로 | 처리 순서 | 중복 검사 위치 | 결과 |
| --- | --- | --- | --- |
| 모집공고 경로 | 요청 제출 -> 운영자 허용 -> 장소<br>예약 확정 | VENUE_RESERVATIONS | reservation_source_type=RECRU<br>ITMENT_NOTICE |
| 일반 박람회 등록 | 개최 신청 심사 -> 장소 예약 확정<br>-> 박람회 배정 | VENUE_RESERVATIONS | reservation_source_type=EXPO_<br>DIRECT |
| 박람회 생성 | 확정 예약을 박람회에 연결 | EXPO_VENUE_ASSIGNMENTS | expo_id와 venue_reservation_id<br>만 연결 |

### 핵심 원칙: 장소·홀·구역·사용 기간은 VENUE_RESERVATIONS 에 한 번만 저장합니다. 모집공고 경로와 일반 등록 경로가 모두 같은 테이블을 사용하므로 서로 다른 테이블 사이의 교차 중복

### 문제가 발생하지 않습니다.

## 일반 박람회 등록 시 장소 처리

- 관리자가 일반 박람회 개최 신청을 승인하기 전에 동일 장소·홀·구역과 겹치는 CONFIRMED VENUE_RESERVATIONS 가 있는지 검사합니다.
- 중복이 없으면 VENUE_RESERVATIONS 를 EXPO_DIRECT 경로로 생성하고 CONFIRMED 처리합니다.
- 확정 예약 생성이 성공한 뒤 EXPO_VENUE_ASSIGNMENTS 를 생성하여 박람회와 예약을 연결합니다.
- 예약 생성과 박람회 배정 연결은 하나의 트랜잭션에서 처리합니다.
- 동시 요청은 VENUE_RESERVATIONS 의 EXCLUDE USING gist 제약이 최종적으로 차단합니다.
EXPO_VENUE_ASSIGNMENTS 의 최종 컬럼 정의는 6-3 절을 기준으로 하며, 6-14 절에서는 장소 예약 단일화 처리 흐름만 설명합니다.

## 6-15. 부스 판매·결제·배정·관리

참여 기업은 모집공고 1 건에서 부스 상품 1 개만 선택합니다. 결제 성공 즉시 신청 완료와 부스 확정 배정을 원자적으로 반영하며, 결제 완료 후 취소·환불은 지원하지 않습니다.

| 구분 | 엔티티 |
| --- | --- |
| 물리 테이블 | BOOTH_ORDERS |
| 물리 테이블 | BOOTH_RESERVATIONS |
| 물리 테이블 | BOOTH_PAYMENTS |
| 물리 테이블 | BOOTH_PAYMENT_HISTORIES |
| 물리 테이블 | BOOTH_ALLOCATIONS |
| 물리 테이블 | BOOTH_MANAGEMENT_HISTORIES |
| 물리 테이블 | BOOTH_CONTENTS |
| 물리 테이블 | BOOTH_CONTENT_FILES |

부스가 1 개만 선택되므로 BOOTH_ORDER_ITEMS 는 사용하지 않습니다. BOOTH_ORDERS.booth_product_id 에 선택 상품을 직접 연결합니다.

## BOOTH_ORDERS

참여 신청 과정에서 선택한 단일 부스 상품의 주문입니다.

---

<!-- Page 42 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 부스 주문 ID |
| application_id | BIGINT | UNIQUE, FK -><br>PARTICIPATION_APPLICATION<br>S.id, NOT NULL | 참여 신청 |
| client_user_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 결제 기업 |
| booth_product_id | BIGINT | FK -> BOOTH_PRODUCTS.id,<br>NOT NULL | 선택 부스 1개 |
| order_number | VARCHAR(50) | UNIQUE, NOT NULL | 부스 주문번호 |
| unit_price | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 부스 가격 |
| total_amount | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | 최종 결제액; 단수이므로<br>unit_price와 동일 |
| status | VARCHAR(30) | CHECK PENDING_PAYMENT,<br>PAYMENT_COMPLETED,<br>FAILED, CANCELED, EXPIRED | 주문 상태 |
| expires_at | TIMESTAMPTZ | NOT NULL | 결제·임시 확보 만료 |
| paid_at | TIMESTAMPTZ | NULL | 결제 성공 |
| idempotency_key | VARCHAR(100) | UNIQUE, NOT NULL | 중복 승인 방지 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |

## BOOTH_MANAGEMENT_HISTORIES

부스 배정·콘텐츠 운영상 변경 이력을 저장합니다. 신청 승인·반려 이력으로 사용하지 않습니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 관리 이력 ID |
| booth_allocation_id | BIGINT | FK -> BOOTH_ALLOCATIONS.id,<br>NOT NULL | 대상 배정 |
| booth_content_id | BIGINT | FK -> BOOTH_CONTENTS.id,<br>NULL | 대상 콘텐츠 |
| action_type | VARCHAR(40) | CHECK<br>ALLOCATION_CORRECTED,<br>INFORMATION_UPDATED,<br>CORRECTION_REQUESTED,<br>CONTENT_HIDDEN,<br>CONTENT_RESTORED | 운영 처리 |
| before_data | JSONB | NULL | 변경 전 |
| after_data | JSONB | NULL | 변경 후 |
| reason | TEXT | NULL | 사유 |
| processed_by_admin_id | BIGINT | FK -> USERS.id, NOT NULL | 처리 관리자 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 처리 시각 |

---

<!-- Page 43 -->

## BOOTH_CONTENTS

확정 배정된 참여 기업이 기업·부스 소개 콘텐츠를 관리합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 부스 콘텐츠 ID |
| booth_allocation_id | BIGINT | UNIQUE, FK -><br>BOOTH_ALLOCATIONS.id, NOT<br>NULL | 확정 배정 |
| client_user_id | BIGINT | FK -><br>CLIENT_PROFILES.user_id, NOT<br>NULL | 참여 기업 |
| company_display_name | VARCHAR(150) | NOT NULL | 노출 기업명 |
| title | VARCHAR(200) | NOT NULL | 부스 제목 |
| company_description | TEXT | NULL | 기업 소개 |
| booth_description | TEXT | NULL | 부스 소개 |
| product_description | TEXT | NULL | 전시 제품·서비스 소개 |
| logo_file_id | BIGINT | FK -> FILE_METADATA.id, NULL | 로고 |
| main_image_file_id | BIGINT | FK -> FILE_METADATA.id, NULL | 대표 이미지 |
| status | VARCHAR(30) | CHECK DRAFT, PUBLISHED,<br>CORRECTION_REQUESTED,<br>HIDDEN | 콘텐츠 상태 |
| published_at | TIMESTAMPTZ | NULL | 공개 시각 |
| correction_requested_at | TIMESTAMPTZ | NULL | 보완 요청 시각 |
| correction_message | TEXT | NULL | 보완 요청 |
| checked_by_admin_id | BIGINT | FK -> USERS.id, NULL | 운영 확인 관리자 |
| checked_at | TIMESTAMPTZ | NULL | 운영 확인 시각 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 수정 |

## BOOTH_CONTENT_FILES

부스 콘텐츠의 갤러리 이미지, 영상, 카탈로그 및 리플렛 파일입니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| id | BIGSERIAL | PK | 파일 연결 ID |
| booth_content_id | BIGINT | FK -> BOOTH_CONTENTS.id,<br>NOT NULL | 부스 콘텐츠 |
| file_id | BIGINT | FK -> FILE_METADATA.id, NOT<br>NULL | 파일 |
| file_type | VARCHAR(30) | CHECK GALLERY_IMAGE,<br>PROMO_VIDEO, CATALOG,<br>LEAFLET, OTHER | 파일 유형 |
| title | VARCHAR(150) | NULL | 표시명 |
| sort_order | INTEGER | NOT NULL DEFAULT 0 | 정렬 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT<br>CURRENT_TIMESTAMP | 생성 |

---

<!-- Page 44 -->

| 컬럼명 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| 중복 제약 | - | UNIQUE(booth_content_id,<br>file_id) | 중복 연결 방지 |

## 6-15-1. 상태 전이 및 원자 처리

| 대상 | 상태 전이 |
| --- | --- |
| 참여 신청 | DRAFT -> PAYMENT_PENDING -> SUBMITTED / PAYMENT_FAILED<br>/ CANCELED |
| 부스 주문 | PENDING_PAYMENT -> PAYMENT_COMPLETED / FAILED /<br>CANCELED / EXPIRED |
| 부스 예약 | ACTIVE -> CONFIRMED / RELEASED / EXPIRED |
| 부스 상품 | AVAILABLE -> RESERVED -> SOLD; 실패·만료 시 AVAILABLE 복구 |
| 부스 배정 | 결제 성공 시 ASSIGNED 생성 |

### 참여 신청 철회 처리

| 현재 신청 상태 | 철회 가능 여부 | 변경 상태 |
| --- | --- | --- |
| DRAFT | 가능 | CANCELED |
| PAYMENT_PENDING | 가능 | CANCELED |
| PAYMENT_FAILED | 가능 | CANCELED |
| SUBMITTED | 불가 | 변경 없음 |

철회는 결제 완료 전에만 허용하며 다음 작업을 하나의 트랜잭션에서 처리합니다.
- PARTICIPATION_APPLICATIONS.status = CANCELED
- 생성된 BOOTH_ORDERS 가 있으면 status = CANCELED
- 유효한 BOOTH_RESERVATIONS 가 있으면 status = RELEASED
- 예약된 BOOTH_PRODUCTS 가 있으면 sales_status = AVAILABLE
- DRAFT 상태에서 주문·예약이 아직 생성되지 않았다면 신청 상태만 CANCELED 로 변경
- SUBMITTED 상태는 철회·부스 취소·환불 모두 거부

### 결제 만료 처리

- BOOTH_ORDERS.status = EXPIRED
- BOOTH_RESERVATIONS.status = EXPIRED 또는 RELEASED
- BOOTH_PRODUCTS.sales_status = AVAILABLE
- PARTICIPATION_APPLICATIONS.status = DRAFT
- PARTICIPATION_APPLICATIONS.selected_booth_product_id = NULL
- PARTICIPATION_APPLICATIONS.booth_order_id = NULL

### 결제 성공 원자 처리

1. 결제 승인 금액·주문번호·멱등키 검증
2. 선택 부스가 해당 신청에 RESERVED 인지 재검증
3. BOOTH_ORDERS 를 PAYMENT_COMPLETED 로 변경

---

<!-- Page 45 -->

4. PARTICIPATION_APPLICATIONS 를 SUBMITTED 로 변경
5. BOOTH_RESERVATIONS 를 CONFIRMED 로 변경
6. BOOTH_ALLOCATIONS 를 ASSIGNED 로 생성
7. BOOTH_PRODUCTS 를 SOLD 로 변경
8. 모든 작업이 성공할 때만 커밋

## 최종 필수 제약조건

- UNIQUE(PARTICIPATION_APPLICATIONS.recruitment_notice_id, client_user_id): 기업은 공고당 신청 1 건만 보유합니다.
- PARTICIPATION_APPLICATIONS.selected_booth_product_id 는 단일 FK 이며, 한 신청에서 부스 1 개만 선택합니다.
- BOOTH_ORDERS.application_id 는 UNIQUE 이며, 주문은 단일 booth_product_id 를 가집니다.
- 동일 booth_product_id 에는 유효 ACTIVE 예약 1 개 또는 ASSIGNED 배정 1 개만 허용합니다.
- SUBMITTED 신청은 PAYMENT_COMPLETED 주문과 ASSIGNED 배정을 반드시 가집니다.
- 결제 완료 부스는 사용자 취소·환불을 지원하지 않습니다.
- 중복 장소 요청은 저장 가능하지만, CONFIRMED VENUE_RESERVATIONS 는 기간이 겹칠 수 없습니다.
- 장소 충돌 운영 결정은 submitted_at 오름차순을 기본 원칙으로 하며 결정 관리자·시각·사유를 기록합니다.
- 모집공고와 일반 등록 박람회 모두 먼저 VENUE_RESERVATIONS 에 CONFIRMED 예약을 생성합니다.
- 장소·홀·구역·기간 중복은 VENUE_RESERVATIONS 의 단일 EXCLUDE 제약으로 모든 경로를 함께 차단합니다.
- EXPO_VENUE_ASSIGNMENTS 는 expo_id 와 venue_reservation_id 의 1:1 연결만 저장합니다.
- 참여 신청 철회는 DRAFT/PAYMENT_PENDING/PAYMENT_FAILED 에서만 가능하고 SUBMITTED 에서는 거부합니다.

## 문서 전체에서 제거· 변경할 구버전 문구

| 구버전 | v14 처리 |
| --- | --- |
| 참여 신청 승인·반려 | 참여 신청 운영 확인·보완 요청으로 변경 |
| 참여 신청 심사 결과/반려 사유 | 신청·결제·배정 상태 및 보완 요청 내용으로 변경 |
| 승인 기업과 부스 배정 결과 | 결제 완료 기업과 확정 부스 배정 결과로 변경 |
| 기업 모집 기간 종료 후 참여 기업 심사 완료 | 결제·배정 완료 집계로 변경 |
| 장소 중복 요청 자동 차단 | 요청 저장 후 운영자 선착순 검토·허용·취소로 변경 |
| BOOTH_ORDER_ITEMS | 제거; 단일 부스 상품을 BOOTH_ORDERS에 직접 연결 |
| BOOTH_TYPES | 오타 수정; BOOTH_TEMPLATES로 통일 |
| 장소 기간 중복 제약 | VENUE_RESERVATIONS로 단일화; 모집공고·일반 등록 경로를 모두<br>같은 EXCLUDE 제약으로 검증 |
| EXPO_VENUE_ASSIGNMENTS | 장소·기간 중복 컬럼 제거; expo_id와 venue_reservation_id 1:1 연결만<br>유지 |
| CLNT-18 / RECR-08 철회 우선순위 | 둘 다 M·확정으로 통일; 결제 완료 전 상태에서만 철회 |
