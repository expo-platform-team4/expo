-- =============================================================================
-- V1__init_schema.sql — 박람회 티켓 판매 중개 플랫폼 초기 스키마
--
-- 근거: docs/init_table_schema.md (통합 ERD v16 + erd수정본)
--       테이블 67개. 그중 5개는 명세가 없어 추정으로 채운 것이다(문서 부록 E 참조).
--
-- PostgreSQL 전용이다. JSONB / EXCLUDE USING gist / 부분 인덱스를 쓴다.
-- test 프로필(H2)에서는 실행되지 않는다. 스키마에 의존하는 테스트가 필요해지면
-- Testcontainers PostgreSQL 로 전환해야 한다.
--
-- 구성
--   1~15) CREATE TABLE — PK / NOT NULL / CHECK / UNIQUE 만 선언한다.
--   16)   FK 제약      — 말미에서 ALTER 로 일괄 추가한다. 세 쌍이 순환 참조라
--                        테이블 생성 순서에 의존하지 않게 하기 위함이다.
--   17)   특수 제약    — EXCLUDE, 부분 UNIQUE 인덱스
--   18)   인덱스
--
-- 각 절 제목의 화살표는 대응하는 backend 도메인 패키지다(com.expo.*).
-- 주의: com.expo.client 는 클라이언트 "사용자"가 아니라 외부 연동 클라이언트
--       (토스페이먼츠·S3·카카오)다. client_profiles 는 member 도메인에 속한다.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;


-- =============================================================================
-- 1. 공통  →  com.expo.common
-- =============================================================================

-- 업로드된 모든 파일의 저장소 위치와 상태. 프로필 이미지·박람회 이미지·배너·
-- 부스 콘텐츠·정산 리포트가 전부 이 테이블을 통한다.
CREATE TABLE file_metadata (
    id                BIGSERIAL     PRIMARY KEY,
    uploader_user_id  BIGINT        NULL,
    storage_provider  VARCHAR(20)   NOT NULL,
    bucket_name       VARCHAR(100)  NOT NULL,
    storage_key       VARCHAR(500)  NOT NULL UNIQUE,
    original_filename VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(100)  NOT NULL,
    file_size         BIGINT        NOT NULL CHECK (file_size >= 0),
    checksum          VARCHAR(128)  NULL,
    file_status       VARCHAR(20)   NOT NULL
        CHECK (file_status IN ('ACTIVE', 'DELETED', 'QUARANTINED')),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================================
-- 2. 인증 및 사용자 관리 (6-1)  →  com.expo.member, com.expo.auth
-- =============================================================================

-- [member] 공통 계정. 일반 회원·클라이언트·관리자를 하나의 모델로 관리한다.
-- MVP 의 ADMIN 은 등급 구분 없는 단일 SUPER ADMIN 이다.
-- password_hash 가 NULL 인 것은 소셜 로그인 전용 계정을 허용하기 때문이다.
CREATE TABLE users (
    id                       BIGSERIAL    PRIMARY KEY,
    email                    VARCHAR(255) NOT NULL UNIQUE,
    password_hash            VARCHAR(255) NULL,
    nickname                 VARCHAR(50)  NOT NULL UNIQUE,
    role                     VARCHAR(20)  NOT NULL
        CHECK (role IN ('MEMBER', 'CLIENT', 'ADMIN')),
    account_status           VARCHAR(20)  NOT NULL
        CHECK (account_status IN ('ACTIVE', 'WITHDRAWN')),
    phone_number             VARCHAR(20)  NULL,
    phone_verified_at        TIMESTAMPTZ  NULL,
    last_login_at            TIMESTAMPTZ  NULL,
    withdrawn_at             TIMESTAMPTZ  NULL,
    profile_image_file_id    BIGINT       NULL,
    profile_image_updated_at TIMESTAMPTZ  NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [member] role=CLIENT 사용자의 사업자 상세. PK 가 곧 users.id 다.
CREATE TABLE client_profiles (
    user_id                  BIGINT       PRIMARY KEY,
    business_number          VARCHAR(20)  NOT NULL UNIQUE,
    company_name             VARCHAR(150) NOT NULL,
    representative_name      VARCHAR(100) NOT NULL,
    business_address         VARCHAR(255) NOT NULL,
    business_type            VARCHAR(100) NULL,
    business_number_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    business_verified_at     TIMESTAMPTZ  NULL,
    verification_provider    VARCHAR(30)  NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [auth] 구글·카카오 OAuth 계정 연결
CREATE TABLE social_accounts (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL CHECK (provider IN ('GOOGLE', 'KAKAO')),
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email   VARCHAR(255) NULL,
    linked_at        TIMESTAMPTZ  NOT NULL,
    last_login_at    TIMESTAMPTZ  NULL,
    CONSTRAINT uq_social_accounts_provider UNIQUE (provider, provider_user_id)
);

-- [auth] 휴대폰 본인인증 요청과 결과
CREATE TABLE phone_verifications (
    id                      BIGSERIAL    PRIMARY KEY,
    user_id                 BIGINT       NULL,
    phone_number            VARCHAR(20)  NOT NULL,
    verification_token_hash VARCHAR(255) NOT NULL,
    status                  VARCHAR(20)  NOT NULL
        CHECK (status IN ('REQUESTED', 'VERIFIED', 'FAILED', 'EXPIRED')),
    requested_at            TIMESTAMPTZ  NOT NULL,
    verified_at             TIMESTAMPTZ  NULL,
    expires_at              TIMESTAMPTZ  NOT NULL
);

-- [auth] 비밀번호 재설정 일회용 토큰. 원문 대신 해시를 저장한다.
CREATE TABLE password_reset_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    status       VARCHAR(20)  NOT NULL
        CHECK (status IN ('ISSUED', 'USED', 'EXPIRED', 'REVOKED')),
    issued_at    TIMESTAMPTZ  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ  NULL,
    requested_ip VARCHAR(45)  NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [auth] 기기별 Refresh Token. 해시만 저장하고 revoked_at 으로 폐기를 관리한다.
CREATE TABLE refresh_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ  NULL,
    last_used_at TIMESTAMPTZ  NULL,
    created_ip   VARCHAR(45)  NULL,
    user_agent   VARCHAR(500) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================================
-- 3. 가상 장소 (6-14)  →  com.expo.venue
-- =============================================================================

-- 플랫폼이 보유한 행사장·전시장 원본
CREATE TABLE virtual_venues (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(150) NOT NULL UNIQUE,
    address            VARCHAR(255) NOT NULL,
    region_code        VARCHAR(30)  NOT NULL,
    description        TEXT         NULL,
    map_file_id        BIGINT       NULL,
    operational_status VARCHAR(20)  NOT NULL
        CHECK (operational_status IN ('ACTIVE', 'INACTIVE')),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 장소 안의 전시장·홀 단위.
-- 주의: 이 테이블의 컬럼 정의는 명세에 없어 venue_zones 대칭으로 추정한 것이다.
--       명세에서 확실한 것은 UNIQUE (venue_id, hall_code) 뿐이다. 문서 부록 E-1 참조.
CREATE TABLE venue_halls (
    id                 BIGSERIAL     PRIMARY KEY,
    venue_id           BIGINT        NOT NULL,
    hall_code          VARCHAR(30)   NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    width              NUMERIC(10,2) NULL CHECK (width > 0),
    depth              NUMERIC(10,2) NULL CHECK (depth > 0),
    layout_file_id     BIGINT        NULL,
    operational_status VARCHAR(20)   NOT NULL
        CHECK (operational_status IN ('ACTIVE', 'INACTIVE')),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_venue_halls_code UNIQUE (venue_id, hall_code),
    -- 하위 테이블이 "이 홀이 이 장소 소속인지"까지 복합 FK 로 검증할 수 있게 한다.
    CONSTRAINT uq_venue_halls_id_venue UNIQUE (id, venue_id)
);

-- 홀 안에서 부스가 배치되는 구역
CREATE TABLE venue_zones (
    id                 BIGSERIAL     PRIMARY KEY,
    hall_id            BIGINT        NOT NULL,
    zone_code          VARCHAR(30)   NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    max_booth_count    INTEGER       NOT NULL CHECK (max_booth_count >= 0),
    width              NUMERIC(10,2) NULL CHECK (width > 0),
    depth              NUMERIC(10,2) NULL CHECK (depth > 0),
    layout_file_id     BIGINT        NULL,
    operational_status VARCHAR(20)   NOT NULL
        CHECK (operational_status IN ('ACTIVE', 'INACTIVE')),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_venue_zones_code UNIQUE (hall_id, zone_code),
    -- 위와 같은 이유. "이 구역이 이 홀 소속인지" 검증용.
    CONSTRAINT uq_venue_zones_id_hall UNIQUE (id, hall_id)
);

-- 장소 예약 단일 원본 (6-13). 모집공고 경로와 일반 박람회 등록 경로를 모두 담는다.
-- 장소·홀·구역·기간은 오직 여기에만 저장하고, 기간 중복도 이 테이블에서만 막는다.
CREATE TABLE venue_reservations (
    id                      BIGSERIAL   PRIMARY KEY,
    reservation_source_type VARCHAR(30) NOT NULL
        CHECK (reservation_source_type IN ('RECRUITMENT_NOTICE', 'EXPO_DIRECT')),
    notice_request_id       BIGINT      NULL UNIQUE,
    opening_request_id      BIGINT      NULL UNIQUE,
    recruitment_notice_id   BIGINT      NULL,
    virtual_venue_id        BIGINT      NOT NULL,
    venue_hall_id           BIGINT      NULL,
    venue_zone_id           BIGINT      NULL,
    use_start_at            TIMESTAMPTZ NOT NULL,
    use_end_at              TIMESTAMPTZ NOT NULL,
    status                  VARCHAR(20) NOT NULL
        CHECK (status IN ('CONFIRMED', 'RELEASED', 'CANCELED')),
    confirmed_by_admin_id   BIGINT      NOT NULL,
    confirmed_at            TIMESTAMPTZ NOT NULL,
    released_at             TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_venue_reservations_period CHECK (use_end_at > use_start_at),
    -- 예약 원천 제약: 판별자와 실제로 채워진 요청 FK 가 일치해야 한다.
    -- XOR 만으로는 RECRUITMENT_NOTICE 인데 opening_request_id 만 채우는 조합을 막지 못한다.
    CONSTRAINT ck_venue_reservations_source CHECK (
        (reservation_source_type = 'RECRUITMENT_NOTICE'
             AND notice_request_id  IS NOT NULL AND opening_request_id IS NULL)
     OR (reservation_source_type = 'EXPO_DIRECT'
             AND opening_request_id IS NOT NULL AND notice_request_id  IS NULL)
    ),
    -- 구역을 지정했으면 상위 홀도 반드시 지정한다.
    CONSTRAINT ck_venue_reservations_zone_needs_hall CHECK (
        venue_zone_id IS NULL OR venue_hall_id IS NOT NULL
    )
);


-- =============================================================================
-- 4. 박람회 관리 (6-3)  →  com.expo.expo
-- =============================================================================

-- 검색·필터용 카테고리 마스터
CREATE TABLE categories (
    id         BIGSERIAL    PRIMARY KEY,
    parent_id  BIGINT       NULL,
    name       VARCHAR(100) NOT NULL UNIQUE,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 주최 클라이언트의 박람회 개최 신청. 승인되면 expos 로 이어진다.
CREATE TABLE expo_opening_requests (
    id                    BIGSERIAL    PRIMARY KEY,
    host_client_id        BIGINT       NOT NULL,
    recruitment_result_id BIGINT       NULL,
    title                 VARCHAR(255) NOT NULL,
    description           TEXT         NOT NULL,
    event_start_at        TIMESTAMPTZ  NOT NULL,
    event_end_at          TIMESTAMPTZ  NOT NULL,
    sales_start_at        TIMESTAMPTZ  NOT NULL,
    sales_end_at          TIMESTAMPTZ  NOT NULL,
    desired_venue_id      BIGINT       NULL,
    desired_venue_hall_id BIGINT       NULL,
    desired_venue_zone_id BIGINT       NULL,
    status                VARCHAR(30)  NOT NULL
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELED')),
    submitted_at          TIMESTAMPTZ  NULL,
    reviewed_by_admin_id  BIGINT       NULL,
    reviewed_at           TIMESTAMPTZ  NULL,
    rejection_reason      TEXT         NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_expo_opening_requests_event CHECK (event_end_at > event_start_at),
    CONSTRAINT ck_expo_opening_requests_sales CHECK (sales_end_at > sales_start_at),
    CONSTRAINT ck_opening_requests_zone_needs_hall CHECK (
        desired_venue_zone_id IS NULL OR desired_venue_hall_id IS NOT NULL
    ),
    CONSTRAINT ck_opening_requests_hall_needs_venue CHECK (
        desired_venue_hall_id IS NULL OR desired_venue_id IS NOT NULL
    )
);

-- 승인·공개되어 티켓 판매와 체크인의 기준이 되는 박람회 원본
CREATE TABLE expos (
    id                   BIGSERIAL    PRIMARY KEY,
    host_client_id       BIGINT       NOT NULL,
    opening_request_id   BIGINT       NULL UNIQUE,
    title                VARCHAR(255) NOT NULL,
    description          TEXT         NOT NULL,
    region_code          VARCHAR(30)  NOT NULL,
    event_start_at       TIMESTAMPTZ  NOT NULL,
    event_end_at         TIMESTAMPTZ  NOT NULL,
    sales_start_at       TIMESTAMPTZ  NOT NULL,
    sales_end_at         TIMESTAMPTZ  NOT NULL,
    review_status        VARCHAR(20)  NOT NULL
        CHECK (review_status IN ('DRAFT', 'UNDER_REVIEW', 'REJECTED', 'APPROVED')),
    visibility_status    VARCHAR(20)  NOT NULL
        CHECK (visibility_status IN ('PRIVATE', 'PUBLIC', 'ARCHIVED')),
    event_status         VARCHAR(20)  NOT NULL
        CHECK (event_status IN ('SCHEDULED', 'ONGOING', 'ENDED', 'CANCELED')),
    approved_by_admin_id BIGINT       NULL,
    approved_at          TIMESTAMPTZ  NULL,
    rejection_reason     TEXT         NULL,
    canceled_at          TIMESTAMPTZ  NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_expos_event CHECK (event_end_at > event_start_at),
    CONSTRAINT ck_expos_sales CHECK (sales_end_at > sales_start_at)
);

-- 박람회 ↔ 확정 장소 예약의 1:1 연결만 담당한다.
-- 장소·기간 컬럼을 일부러 갖지 않는다. 원본은 venue_reservations 다.
CREATE TABLE expo_venue_assignments (
    id                   BIGSERIAL   PRIMARY KEY,
    expo_id              BIGINT      NOT NULL UNIQUE,
    venue_reservation_id BIGINT      NOT NULL UNIQUE,
    assigned_by_admin_id BIGINT      NOT NULL,
    assigned_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 박람회 ↔ 카테고리 N:M
CREATE TABLE expo_categories (
    expo_id     BIGINT      NOT NULL,
    category_id BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (expo_id, category_id)
);

-- 박람회 썸네일·상세 이미지
CREATE TABLE expo_images (
    id         BIGSERIAL    PRIMARY KEY,
    expo_id    BIGINT       NOT NULL,
    file_id    BIGINT       NOT NULL,
    image_type VARCHAR(20)  NOT NULL
        CHECK (image_type IN ('THUMBNAIL', 'DETAIL', 'GALLERY')),
    alt_text   VARCHAR(255) NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_expo_images_file UNIQUE (expo_id, file_id)
);

-- 박람회 소개 PDF·카탈로그·리플렛·홍보영상
CREATE TABLE expo_files (
    id           BIGSERIAL    PRIMARY KEY,
    expo_id      BIGINT       NOT NULL,
    file_id      BIGINT       NOT NULL,
    file_purpose VARCHAR(30)  NOT NULL
        CHECK (file_purpose IN ('INTRO_PDF', 'CATALOG', 'LEAFLET', 'PROMO_VIDEO', 'OTHER')),
    title        VARCHAR(150) NULL,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_expo_files_file UNIQUE (expo_id, file_id)
);

-- 박람회 또는 부스 콘텐츠의 외부 링크. 둘 중 정확히 하나에만 매달린다.
CREATE TABLE external_links (
    id               BIGSERIAL     PRIMARY KEY,
    expo_id          BIGINT        NULL,
    booth_content_id BIGINT        NULL,
    link_type        VARCHAR(30)   NOT NULL
        CHECK (link_type IN ('HOMEPAGE', 'SOCIAL', 'RESERVATION', 'PRODUCT', 'OTHER')),
    label            VARCHAR(100)  NULL,
    url              VARCHAR(1000) NOT NULL,
    sort_order       INTEGER       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_external_links_owner_xor CHECK (
        (expo_id IS NOT NULL)::int + (booth_content_id IS NOT NULL)::int = 1
    )
);

-- 박람회에 참여하는 기업과 배정 부스를 연결한 조회 기준
CREATE TABLE expo_companies (
    id                   BIGSERIAL    PRIMARY KEY,
    expo_id              BIGINT       NOT NULL,
    client_user_id       BIGINT       NOT NULL,
    booth_allocation_id  BIGINT       NULL,
    display_name         VARCHAR(150) NOT NULL,
    participation_status VARCHAR(20)  NOT NULL
        CHECK (participation_status IN ('CONFIRMED', 'CANCELED')),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_expo_companies_client UNIQUE (expo_id, client_user_id)
);

-- 박람회 심사 승인·반려 이력
CREATE TABLE expo_review_histories (
    id                BIGSERIAL   PRIMARY KEY,
    expo_id           BIGINT      NOT NULL,
    reviewer_admin_id BIGINT      NOT NULL,
    decision          VARCHAR(20) NOT NULL
        CHECK (decision IN ('SUBMIT', 'APPROVE', 'REJECT')),
    reason            TEXT        NULL,
    from_status       VARCHAR(20) NULL,
    to_status         VARCHAR(20) NOT NULL,
    reviewed_at       TIMESTAMPTZ NOT NULL
);

-- 승인된 박람회에 대한 클라이언트 수정 요청
CREATE TABLE expo_change_requests (
    id                    BIGSERIAL   PRIMARY KEY,
    expo_id               BIGINT      NOT NULL,
    requester_client_id   BIGINT      NOT NULL,
    change_reason         TEXT        NOT NULL,
    requested_changes     JSONB       NOT NULL,
    status                VARCHAR(20) NOT NULL
        CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPLIED', 'REJECTED', 'CANCELED')),
    processed_by_admin_id BIGINT      NULL,
    processed_at          TIMESTAMPTZ NULL,
    rejection_reason      TEXT        NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 관리자가 실제 적용한 변경 전·후 값
CREATE TABLE expo_change_histories (
    id                  BIGSERIAL   PRIMARY KEY,
    expo_id             BIGINT      NOT NULL,
    change_request_id   BIGINT      NULL,
    changed_by_admin_id BIGINT      NOT NULL,
    before_data         JSONB       NOT NULL,
    after_data          JSONB       NOT NULL,
    reason              TEXT        NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 박람회 취소 요청과 승인 결과. 승인 시 연결된 장소 예약을 RELEASED 로 해제한다.
CREATE TABLE expo_cancellation_requests (
    id                    BIGSERIAL   PRIMARY KEY,
    expo_id               BIGINT      NOT NULL,
    requester_client_id   BIGINT      NOT NULL,
    reason                TEXT        NOT NULL,
    status                VARCHAR(20) NOT NULL
        CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'PROCESSING_REFUNDS', 'COMPLETED')),
    processed_by_admin_id BIGINT      NULL,
    processed_at          TIMESTAMPTZ NULL,
    rejection_reason      TEXT        NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================================
-- 5. 티켓 및 재고 (6-5)  →  com.expo.ticket
-- =============================================================================

-- 박람회에서 판매하는 표준 1일권 상품
CREATE TABLE ticket_products (
    id                     BIGSERIAL     PRIMARY KEY,
    expo_id                BIGINT        NOT NULL,
    name                   VARCHAR(150)  NOT NULL,
    description            TEXT          NULL,
    price                  NUMERIC(15,2) NOT NULL CHECK (price >= 0),
    sales_start_at         TIMESTAMPTZ   NOT NULL,
    sales_end_at           TIMESTAMPTZ   NOT NULL,
    max_quantity_per_order INTEGER       NOT NULL DEFAULT 4
        CHECK (max_quantity_per_order BETWEEN 1 AND 4),
    status                 VARCHAR(20)   NOT NULL
        CHECK (status IN ('DRAFT', 'ON_SALE', 'SOLD_OUT', 'SALE_ENDED', 'CANCELED')),
    version                BIGINT        NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ticket_products_sales CHECK (sales_end_at > sales_start_at)
);

-- 상품별 총량·임시확보량·판매량. 상품과 1:1 이다.
-- available_quantity 는 명세가 "GENERATED/계산" 이라고만 적어 구현이 미정이었다(문서 D-4 3).
-- PostgreSQL 전용으로 확정했으므로 생성 컬럼으로 둔다.
CREATE TABLE ticket_inventories (
    id                 BIGSERIAL   PRIMARY KEY,
    ticket_product_id  BIGINT      NOT NULL UNIQUE,
    total_quantity     INTEGER     NOT NULL CHECK (total_quantity >= 0),
    reserved_quantity  INTEGER     NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    sold_quantity      INTEGER     NOT NULL DEFAULT 0 CHECK (sold_quantity >= 0),
    available_quantity INTEGER     GENERATED ALWAYS AS
                                   (total_quantity - reserved_quantity - sold_quantity) STORED,
    version            BIGINT      NOT NULL DEFAULT 0,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 생성 컬럼이 음수가 되는 것을 막는다. 즉 오버셀을 DB 가 차단한다.
    CONSTRAINT ck_ticket_inventories_not_oversold
        CHECK (reserved_quantity + sold_quantity <= total_quantity)
);

-- 결제 대기 동안 티켓 수량을 임시 확보한다.
CREATE TABLE inventory_reservations (
    id                BIGSERIAL   PRIMARY KEY,
    ticket_product_id BIGINT      NOT NULL,
    ticket_order_id   BIGINT      NOT NULL,
    quantity          INTEGER     NOT NULL CHECK (quantity > 0),
    status            VARCHAR(20) NOT NULL
        CHECK (status IN ('ACTIVE', 'CONFIRMED', 'EXPIRED', 'RELEASED')),
    reserved_at       TIMESTAMPTZ NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    released_at       TIMESTAMPTZ NULL
);


-- =============================================================================
-- 6. 주문 (6-6)  →  com.expo.order
-- =============================================================================

-- 주문 당시 판매원금과 3% 예매 수수료를 스냅샷으로 보존한다.
CREATE TABLE ticket_orders (
    id                     BIGSERIAL     PRIMARY KEY,
    order_number           VARCHAR(40)   NOT NULL UNIQUE,
    member_user_id         BIGINT        NULL,
    orderer_type           VARCHAR(20)   NOT NULL
        CHECK (orderer_type IN ('MEMBER', 'GUEST')),
    ticket_subtotal_amount NUMERIC(15,2) NOT NULL CHECK (ticket_subtotal_amount >= 0),
    booking_fee_rate       NUMERIC(6,5)  NOT NULL DEFAULT 0.03000,
    booking_fee_amount     NUMERIC(15,2) NOT NULL CHECK (booking_fee_amount >= 0),
    total_amount           NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    total_quantity         INTEGER       NOT NULL CHECK (total_quantity BETWEEN 1 AND 4),
    status                 VARCHAR(30)   NOT NULL
        CHECK (status IN ('PENDING', 'PAID', 'CANCELED', 'PAYMENT_FAILED', 'EXPIRED')),
    paid_at                TIMESTAMPTZ   NULL,
    canceled_at            TIMESTAMPTZ   NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 주문자 유형과 회원 FK 가 일치해야 한다.
    CONSTRAINT ck_ticket_orders_orderer CHECK (
        (orderer_type = 'MEMBER' AND member_user_id IS NOT NULL)
     OR (orderer_type = 'GUEST'  AND member_user_id IS NULL)
    ),
    -- 최종 결제금액 = 판매원금 + 예매 수수료
    CONSTRAINT ck_ticket_orders_total
        CHECK (total_amount = ticket_subtotal_amount + booking_fee_amount),
    -- 요율은 범위만 제한한다. 이 컬럼은 "주문 당시 요율 스냅샷"이라 값을 0.03 으로
    -- 고정하면 요율 정책이 바뀌는 순간 과거 주문이 전부 제약 위반이 된다.
    CONSTRAINT ck_ticket_orders_fee_rate
        CHECK (booking_fee_rate >= 0 AND booking_fee_rate < 1)
);

-- 주문 항목. item_subtotal_amount = unit_price × quantity
CREATE TABLE ticket_order_items (
    id                   BIGSERIAL     PRIMARY KEY,
    ticket_order_id      BIGINT        NOT NULL,
    ticket_product_id    BIGINT        NOT NULL,
    quantity             INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price           NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    item_subtotal_amount NUMERIC(15,2) NOT NULL CHECK (item_subtotal_amount >= 0),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ticket_order_items_product UNIQUE (ticket_order_id, ticket_product_id),
    CONSTRAINT ck_ticket_order_items_amount CHECK (item_subtotal_amount = unit_price * quantity)
);

-- 비회원 주문 정보. 조회 비밀번호는 원문을 저장하지 않는다. 주문과 1:1.
CREATE TABLE guest_order_infos (
    ticket_order_id      BIGINT       PRIMARY KEY,
    guest_name           VARCHAR(100) NOT NULL,
    phone_number         VARCHAR(20)  NOT NULL,
    age                  INTEGER      NULL CHECK (age >= 0),
    lookup_password_hash VARCHAR(255) NOT NULL,
    failed_lookup_count  INTEGER      NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ  NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================================
-- 7. 티켓 결제 (6-6)  →  com.expo.payment
-- =============================================================================

-- 토스페이먼츠 티켓 결제.
-- 주의: 타입·제약은 명세에 없어 booth_payments 대칭으로 추정했다. 문서 부록 E-1 참조.
--       승인 성공 상태가 여기서는 DONE, 부스에서는 APPROVED 다.
--       둘 다 명세 값이라 통일하지 않았다(문서 D-4 17).
CREATE TABLE ticket_payments (
    id                     BIGSERIAL     PRIMARY KEY,
    ticket_order_id        BIGINT        NOT NULL,
    payment_key            VARCHAR(200)  NULL UNIQUE,
    pg_order_id            VARCHAR(100)  NOT NULL UNIQUE,
    method                 VARCHAR(30)   NULL,
    status                 VARCHAR(20)   NOT NULL
        CHECK (status IN ('READY', 'IN_PROGRESS', 'DONE', 'FAILED', 'CANCELED')),
    requested_amount       NUMERIC(15,2) NOT NULL CHECK (requested_amount >= 0),
    approved_amount        NUMERIC(15,2) NULL CHECK (approved_amount >= 0),
    ticket_subtotal_amount NUMERIC(15,2) NOT NULL CHECK (ticket_subtotal_amount >= 0),
    booking_fee_amount     NUMERIC(15,2) NOT NULL CHECK (booking_fee_amount >= 0),
    approved_at            TIMESTAMPTZ   NULL,
    canceled_amount        NUMERIC(15,2) NOT NULL DEFAULT 0,
    last_failure_code      VARCHAR(100)  NULL,
    idempotency_key        VARCHAR(100)  NOT NULL UNIQUE,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 티켓 결제·취소의 상태 변경과 PG 응답. 물리 삭제하지 않는다.
CREATE TABLE ticket_payment_histories (
    id                 BIGSERIAL     PRIMARY KEY,
    ticket_payment_id  BIGINT        NOT NULL,
    event_type         VARCHAR(30)   NOT NULL
        CHECK (event_type IN ('REQUEST', 'APPROVE', 'FAIL', 'CANCEL')),
    from_status        VARCHAR(20)   NULL,
    to_status          VARCHAR(20)   NOT NULL,
    amount             NUMERIC(15,2) NULL,
    pg_transaction_key VARCHAR(200)  NULL,
    response_payload   JSONB         NULL,
    occurred_at        TIMESTAMPTZ   NOT NULL
);


-- =============================================================================
-- 8. 티켓 환불 (6-6)  →  com.expo.refund
-- =============================================================================

-- 전체 주문 취소만 지원한다. 부분 취소는 없다.
-- 주의: 키·시각·처리자 컬럼은 명세에 없어 추정했다. 문서 부록 E-1 참조.
--       ticket_order_id 의 UNIQUE 는 주문당 환불 1건 가정이다(문서 D-4 15).
CREATE TABLE ticket_refunds (
    id                        BIGSERIAL     PRIMARY KEY,
    ticket_order_id           BIGINT        NOT NULL UNIQUE,
    ticket_payment_id         BIGINT        NOT NULL,
    refund_ticket_amount      NUMERIC(15,2) NOT NULL CHECK (refund_ticket_amount >= 0),
    refund_booking_fee_amount NUMERIC(15,2) NOT NULL CHECK (refund_booking_fee_amount >= 0),
    refund_amount             NUMERIC(15,2) NOT NULL CHECK (refund_amount >= 0),
    status                    VARCHAR(30)   NOT NULL
        CHECK (status IN ('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    reason                    TEXT          NULL,
    pg_refund_key             VARCHAR(200)  NULL UNIQUE,
    requested_at              TIMESTAMPTZ   NOT NULL,
    completed_at              TIMESTAMPTZ   NULL,
    last_failure_code         VARCHAR(100)  NULL,
    processed_by_admin_id     BIGINT        NULL,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ticket_refunds_amount CHECK (
        refund_amount = refund_ticket_amount + refund_booking_fee_amount
    )
);


-- =============================================================================
-- 9. 발권 및 QR 체크인 (6-7)  →  com.expo.checkin
-- =============================================================================

-- 결제 완료 후 구매 수량만큼 발급되는 개별 입장권
CREATE TABLE issued_tickets (
    id                   BIGSERIAL    PRIMARY KEY,
    ticket_order_item_id BIGINT       NOT NULL,
    expo_id              BIGINT       NOT NULL,
    ticket_code          VARCHAR(50)  NOT NULL UNIQUE,
    qr_token_hash        VARCHAR(255) NOT NULL UNIQUE,
    status               VARCHAR(20)  NOT NULL
        CHECK (status IN ('ISSUED', 'CHECKED_IN', 'CANCELED', 'INVALIDATED')),
    issued_at            TIMESTAMPTZ  NOT NULL,
    checked_in_at        TIMESTAMPTZ  NULL,
    invalidated_at       TIMESTAMPTZ  NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 카카오 메시지로 보내는 QR 확인 보안 URL 의 단기 접근 토큰
CREATE TABLE ticket_access_tokens (
    id               BIGSERIAL    PRIMARY KEY,
    ticket_order_id  BIGINT       NOT NULL,
    issued_ticket_id BIGINT       NULL,
    token_hash       VARCHAR(255) NOT NULL UNIQUE,
    scope            VARCHAR(30)  NOT NULL
        CHECK (scope IN ('ORDER_VIEW', 'QR_VIEW')),
    status           VARCHAR(20)  NOT NULL
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    expires_at       TIMESTAMPTZ  NOT NULL,
    last_accessed_at TIMESTAMPTZ  NULL,
    access_count     INTEGER      NOT NULL DEFAULT 0,
    revoked_at       TIMESTAMPTZ  NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- QR 또는 티켓 코드 검증·입장 처리 결과
CREATE TABLE check_in_histories (
    id                     BIGSERIAL   PRIMARY KEY,
    issued_ticket_id       BIGINT      NOT NULL,
    expo_id                BIGINT      NOT NULL,
    processed_by_client_id BIGINT      NOT NULL,
    method                 VARCHAR(20) NOT NULL
        CHECK (method IN ('QR', 'MANUAL_CODE')),
    result                 VARCHAR(30) NOT NULL
        CHECK (result IN ('SUCCESS', 'ALREADY_USED', 'CANCELED_TICKET', 'WRONG_EXPO', 'INVALID_TOKEN')),
    checked_at             TIMESTAMPTZ NOT NULL,
    request_ip             VARCHAR(45) NULL,
    detail                 TEXT        NULL
);


-- =============================================================================
-- 10. 알림 (6-8)  →  com.expo.notification
-- =============================================================================

-- 채널 독립적인 알림 작업과 발송 상태
CREATE TABLE notifications (
    id                     BIGSERIAL   PRIMARY KEY,
    recipient_user_id      BIGINT      NULL,
    recipient_phone_number VARCHAR(20) NULL,
    channel                VARCHAR(20) NOT NULL
        CHECK (channel IN ('KAKAO', 'EMAIL', 'IN_APP')),
    template_code          VARCHAR(50) NOT NULL,
    -- reference_type 은 명세가 "ORDER, PAYMENT, REFUND, EXPO, TICKET 등"으로
    -- 개방형이라 CHECK 를 걸지 않는다(문서 D-2).
    reference_type         VARCHAR(30) NULL,
    reference_id           BIGINT      NULL,
    payload                JSONB       NOT NULL,
    status                 VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING', 'CANCELED')),
    scheduled_at           TIMESTAMPTZ NULL,
    sent_at                TIMESTAMPTZ NULL,
    retry_count            INTEGER     NOT NULL DEFAULT 0,
    last_error             TEXT        NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 카카오 알림톡 발송 시도별 외부 응답
CREATE TABLE kakao_message_histories (
    id                  BIGSERIAL    PRIMARY KEY,
    notification_id     BIGINT       NOT NULL,
    provider_message_id VARCHAR(200) NULL,
    status              VARCHAR(20)  NOT NULL
        CHECK (status IN ('REQUESTED', 'SENT', 'DELIVERED', 'FAILED')),
    request_payload     JSONB        NULL,
    response_payload    JSONB        NULL,
    error_code          VARCHAR(100) NULL,
    attempt_no          INTEGER      NOT NULL,
    requested_at        TIMESTAMPTZ  NOT NULL,
    completed_at        TIMESTAMPTZ  NULL,
    CONSTRAINT uq_kakao_message_histories_attempt UNIQUE (notification_id, attempt_no)
);


-- =============================================================================
-- 11. 광고 배너 (6-11)  →  com.expo.banner
-- =============================================================================

-- 배너 노출 위치와 최대 동시 노출 수
CREATE TABLE banner_slots (
    id               BIGSERIAL    PRIMARY KEY,
    slot_code        VARCHAR(50)  NOT NULL UNIQUE,
    name             VARCHAR(100) NOT NULL,
    max_active_count INTEGER      NOT NULL CHECK (max_active_count > 0),
    width_px         INTEGER      NULL,
    height_px        INTEGER      NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 클라이언트의 배너 신청과 심사 상태. MVP 에서는 광고비 결제를 제외한다.
CREATE TABLE banner_applications (
    id                   BIGSERIAL    PRIMARY KEY,
    client_user_id       BIGINT       NOT NULL,
    expo_id              BIGINT       NOT NULL,
    image_file_id        BIGINT       NOT NULL,
    headline             VARCHAR(150) NULL,
    requested_start_at   TIMESTAMPTZ  NOT NULL,
    requested_end_at     TIMESTAMPTZ  NOT NULL,
    review_status        VARCHAR(20)  NOT NULL
        CHECK (review_status IN ('DRAFT', 'UNDER_REVIEW', 'REJECTED', 'APPROVED', 'CANCELED')),
    submitted_at         TIMESTAMPTZ  NULL,
    reviewed_by_admin_id BIGINT       NULL,
    reviewed_at          TIMESTAMPTZ  NULL,
    rejection_reason     TEXT         NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_banner_applications_period CHECK (requested_end_at > requested_start_at)
);

-- 승인과 동시에 자동 생성되는 실제 노출 배너
CREATE TABLE banners (
    id                    BIGSERIAL    PRIMARY KEY,
    banner_application_id BIGINT       NOT NULL UNIQUE,
    banner_slot_id        BIGINT       NOT NULL,
    expo_id               BIGINT       NOT NULL,
    image_file_id         BIGINT       NOT NULL,
    headline              VARCHAR(150) NULL,
    start_at              TIMESTAMPTZ  NOT NULL,
    end_at                TIMESTAMPTZ  NOT NULL,
    display_status        VARCHAR(20)  NOT NULL
        CHECK (display_status IN ('SCHEDULED', 'ACTIVE', 'ENDED', 'CANCELED')),
    sort_order            INTEGER      NOT NULL DEFAULT 0,
    activated_at          TIMESTAMPTZ  NULL,
    ended_at              TIMESTAMPTZ  NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_banners_period CHECK (end_at > start_at)
);

-- 배너 신청 승인·반려 이력
CREATE TABLE banner_review_histories (
    id                    BIGSERIAL   PRIMARY KEY,
    banner_application_id BIGINT      NOT NULL,
    reviewer_admin_id     BIGINT      NOT NULL,
    decision              VARCHAR(20) NOT NULL
        CHECK (decision IN ('SUBMIT', 'APPROVE', 'REJECT', 'CANCEL')),
    reason                TEXT        NULL,
    from_status           VARCHAR(20) NULL,
    to_status             VARCHAR(20) NOT NULL,
    reviewed_at           TIMESTAMPTZ NOT NULL
);


-- =============================================================================
-- 12. 기업 모집공고 (6-13)  →  com.expo.recruitment
-- =============================================================================

-- 주최 클라이언트의 모집공고 생성 요청과 장소 중복 운영 결정.
-- 중복 요청도 일단 저장하고, 운영자가 submitted_at 선착순으로 허용·취소를 결정한다.
CREATE TABLE recruitment_notice_requests (
    id                     BIGSERIAL    PRIMARY KEY,
    host_client_id         BIGINT       NOT NULL,
    title                  VARCHAR(255) NOT NULL,
    description            TEXT         NOT NULL,
    application_start_at   TIMESTAMPTZ  NOT NULL,
    application_end_at     TIMESTAMPTZ  NOT NULL,
    event_start_at         TIMESTAMPTZ  NOT NULL,
    event_end_at           TIMESTAMPTZ  NOT NULL,
    virtual_venue_id       BIGINT       NOT NULL,
    venue_hall_id          BIGINT       NULL,
    venue_zone_id          BIGINT       NULL,
    target_company_count   INTEGER      NULL CHECK (target_company_count > 0),
    requested_booth_config JSONB        NULL,
    status                 VARCHAR(30)  NOT NULL
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CANCELED')),
    venue_conflict_status  VARCHAR(30)  NOT NULL
        CHECK (venue_conflict_status IN ('CLEAR', 'CONFLICT_PENDING', 'RESOLVED')),
    venue_decision         VARCHAR(20)  NOT NULL
        CHECK (venue_decision IN ('PENDING', 'ALLOWED', 'CANCELED')),
    conflict_group_key     VARCHAR(100) NULL,
    submitted_at           TIMESTAMPTZ  NULL,
    decided_by_admin_id    BIGINT       NULL,
    decided_at             TIMESTAMPTZ  NULL,
    decision_reason        TEXT         NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_notice_requests_application CHECK (application_end_at > application_start_at),
    CONSTRAINT ck_notice_requests_event       CHECK (event_end_at > event_start_at),
    CONSTRAINT ck_notice_requests_zone_needs_hall CHECK (
        venue_zone_id IS NULL OR venue_hall_id IS NOT NULL
    )
);

-- 요청 제출·검토·장소 허용/취소·공고 생성 연결 이력
CREATE TABLE recruitment_notice_request_histories (
    id                    BIGSERIAL   PRIMARY KEY,
    request_id            BIGINT      NOT NULL,
    action_type           VARCHAR(30) NOT NULL
        CHECK (action_type IN ('SUBMIT', 'REVIEW_START', 'VENUE_ALLOW', 'VENUE_CANCEL',
                               'APPROVE', 'REJECT', 'NOTICE_CREATED')),
    from_status           VARCHAR(30) NULL,
    to_status             VARCHAR(30) NOT NULL,
    reason                TEXT        NULL,
    processed_by_admin_id BIGINT      NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 허용된 장소 요청을 기준으로 관리자가 작성·게시하는 모집공고
CREATE TABLE recruitment_notices (
    id                      BIGSERIAL    PRIMARY KEY,
    request_id              BIGINT       NOT NULL UNIQUE,
    host_client_id          BIGINT       NOT NULL,
    venue_reservation_id    BIGINT       NOT NULL UNIQUE,
    title                   VARCHAR(255) NOT NULL,
    content                 TEXT         NOT NULL,
    eligibility             TEXT         NULL,
    submission_requirements JSONB        NULL,
    application_start_at    TIMESTAMPTZ  NOT NULL,
    application_end_at      TIMESTAMPTZ  NOT NULL,
    status                  VARCHAR(20)  NOT NULL
        CHECK (status IN ('DRAFT', 'SCHEDULED', 'OPEN', 'CLOSED', 'CANCELED', 'ARCHIVED')),
    published_at            TIMESTAMPTZ  NULL,
    closed_at               TIMESTAMPTZ  NULL,
    created_by_admin_id     BIGINT       NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_recruitment_notices_application CHECK (application_end_at > application_start_at)
);

-- 공고 작성·게시·수정·마감·취소 이력
CREATE TABLE recruitment_notice_histories (
    id                    BIGSERIAL   PRIMARY KEY,
    recruitment_notice_id BIGINT      NOT NULL,
    action_type           VARCHAR(20) NOT NULL
        CHECK (action_type IN ('CREATE', 'PUBLISH', 'UPDATE', 'CLOSE', 'CANCEL', 'ARCHIVE')),
    before_data           JSONB       NULL,
    after_data            JSONB       NULL,
    reason                TEXT        NULL,
    processed_by_admin_id BIGINT      NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 모집 마감 후 결제·배정 완료 기업을 집계한 결과 스냅샷
CREATE TABLE recruitment_results (
    id                       BIGSERIAL     PRIMARY KEY,
    recruitment_notice_id    BIGINT        NOT NULL UNIQUE,
    host_client_id           BIGINT        NOT NULL,
    confirmed_company_count  INTEGER       NOT NULL DEFAULT 0,
    confirmed_booth_count    INTEGER       NOT NULL DEFAULT 0,
    total_booth_sales_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    status                   VARCHAR(20)   NOT NULL
        CHECK (status IN ('GENERATED', 'DELIVERED', 'CONFIRMED', 'USED_FOR_EXPO', 'CANCELED')),
    generated_at             TIMESTAMPTZ   NOT NULL,
    delivered_at             TIMESTAMPTZ   NULL,
    confirmed_by_host_at     TIMESTAMPTZ   NULL,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 모집 결과에 포함되는 기업별 항목
CREATE TABLE recruitment_result_items (
    id                    BIGSERIAL     PRIMARY KEY,
    recruitment_result_id BIGINT        NOT NULL,
    application_id        BIGINT        NOT NULL UNIQUE,
    client_user_id        BIGINT        NOT NULL,
    booth_allocation_id   BIGINT        NOT NULL UNIQUE,
    booth_amount          NUMERIC(15,2) NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================================
-- 13. 참여 신청 (6-13)  →  com.expo.participation
-- =============================================================================

-- 참여 기업의 신청서와 선택 부스. 한 기업은 공고 1건에서 부스 1개만 고른다.
CREATE TABLE participation_applications (
    id                        BIGSERIAL    PRIMARY KEY,
    recruitment_notice_id     BIGINT       NOT NULL,
    client_user_id            BIGINT       NOT NULL,
    company_name_snapshot     VARCHAR(150) NOT NULL,
    participation_purpose     TEXT         NULL,
    exhibit_description       TEXT         NULL,
    selected_booth_product_id BIGINT       NULL,
    booth_order_id            BIGINT       NULL UNIQUE,
    status                    VARCHAR(30)  NOT NULL
        CHECK (status IN ('DRAFT', 'PAYMENT_PENDING', 'SUBMITTED', 'PAYMENT_FAILED', 'CANCELED')),
    submitted_at              TIMESTAMPTZ  NULL,
    admin_checked_at          TIMESTAMPTZ  NULL,
    admin_checked_by          BIGINT       NULL,
    admin_memo                TEXT         NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_participation_applications_client UNIQUE (recruitment_notice_id, client_user_id),
    -- 아래 둘은 booth_orders / booth_allocations 가 신청의 기업·선택부스를 그대로
    -- 물려받았는지 복합 FK 로 검증하기 위한 참조 대상이다.
    CONSTRAINT uq_applications_id_client  UNIQUE (id, client_user_id),
    CONSTRAINT uq_applications_id_product UNIQUE (id, selected_booth_product_id)
);

-- 승인·반려가 아니라 운영 확인과 보완 요청 이력이다.
CREATE TABLE application_operation_histories (
    id                    BIGSERIAL   PRIMARY KEY,
    application_id        BIGINT      NOT NULL,
    action_type           VARCHAR(30) NOT NULL
        CHECK (action_type IN ('CHECKED', 'CORRECTION_REQUESTED',
                               'CORRECTION_COMPLETED', 'MEMO_UPDATED')),
    message               TEXT        NULL,
    processed_by_admin_id BIGINT      NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================================
-- 14. 부스 (6-14, 6-15)  →  com.expo.booth
--     결제 관련 booth_payments / booth_payment_histories 는 com.expo.payment 다.
-- =============================================================================

-- 재사용 가능한 부스 형태와 기본 크기·제공 항목
CREATE TABLE booth_templates (
    id                     BIGSERIAL    PRIMARY KEY,
    shape_code             VARCHAR(30)  NOT NULL UNIQUE,
    name                   VARCHAR(100) NOT NULL,
    width                  NUMERIC(8,2) NOT NULL CHECK (width > 0),
    height                 NUMERIC(8,2) NULL CHECK (height > 0),
    depth                  NUMERIC(8,2) NOT NULL CHECK (depth > 0),
    dimension_unit         VARCHAR(10)  NOT NULL DEFAULT 'M',
    default_included_items JSONB        NULL,
    operational_status     VARCHAR(20)  NOT NULL
        CHECK (operational_status IN ('ACTIVE', 'INACTIVE')),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 구역 도면에 존재하는 고정 부스 공간 원본. 상품이 아니라 공간이다.
CREATE TABLE booths (
    id                 BIGSERIAL     PRIMARY KEY,
    venue_zone_id      BIGINT        NOT NULL,
    booth_template_id  BIGINT        NULL,
    booth_number       VARCHAR(30)   NOT NULL,
    shape_code         VARCHAR(30)   NOT NULL,
    width              NUMERIC(8,2)  NOT NULL CHECK (width > 0),
    height             NUMERIC(8,2)  NULL CHECK (height > 0),
    depth              NUMERIC(8,2)  NOT NULL CHECK (depth > 0),
    dimension_unit     VARCHAR(10)   NOT NULL DEFAULT 'M',
    position_x         NUMERIC(10,2) NULL,
    position_y         NUMERIC(10,2) NULL,
    rotation_degree    NUMERIC(6,2)  NULL DEFAULT 0,
    sort_order         INTEGER       NOT NULL DEFAULT 0,
    operational_status VARCHAR(20)   NOT NULL
        CHECK (operational_status IN ('ACTIVE', 'INACTIVE')),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booths_number UNIQUE (venue_zone_id, booth_number)
);

-- 특정 모집공고에서 실제 판매되는 부스 상품. booths 가 공간, 이쪽이 상품이다.
CREATE TABLE booth_products (
    id                    BIGSERIAL     PRIMARY KEY,
    recruitment_notice_id BIGINT        NOT NULL,
    booth_id              BIGINT        NOT NULL,
    supply_price          NUMERIC(15,2) NOT NULL CHECK (supply_price >= 0),
    vat_amount            NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (vat_amount >= 0),
    total_price           NUMERIC(15,2) NOT NULL CHECK (total_price >= 0),
    vat_included          BOOLEAN       NOT NULL DEFAULT TRUE,
    included_items        JSONB         NULL,
    sales_start_at        TIMESTAMPTZ   NULL,
    sales_end_at          TIMESTAMPTZ   NULL,
    payment_enabled       BOOLEAN       NOT NULL DEFAULT TRUE,
    sales_status          VARCHAR(20)   NOT NULL
        CHECK (sales_status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'UNAVAILABLE', 'CANCELED')),
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booth_products_booth UNIQUE (recruitment_notice_id, booth_id),
    CONSTRAINT ck_booth_products_total CHECK (total_price = supply_price + vat_amount),
    -- 신청이 "이 부스 상품이 그 공고 소속인지"까지 복합 FK 로 검증할 수 있게 한다.
    CONSTRAINT uq_booth_products_id_notice UNIQUE (id, recruitment_notice_id)
);

-- 참여 신청에서 고른 단일 부스 상품의 주문.
-- 부스가 1개만 선택되므로 주문 항목 테이블을 두지 않고 상품을 직접 연결한다.
CREATE TABLE booth_orders (
    id               BIGSERIAL     PRIMARY KEY,
    application_id   BIGINT        NOT NULL UNIQUE,
    client_user_id   BIGINT        NOT NULL,
    booth_product_id BIGINT        NOT NULL,
    order_number     VARCHAR(50)   NOT NULL UNIQUE,
    unit_price       NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    total_amount     NUMERIC(15,2) NOT NULL CHECK (total_amount >= 0),
    status           VARCHAR(30)   NOT NULL
        CHECK (status IN ('PENDING_PAYMENT', 'PAYMENT_COMPLETED', 'FAILED', 'CANCELED', 'EXPIRED')),
    expires_at       TIMESTAMPTZ   NOT NULL,
    paid_at          TIMESTAMPTZ   NULL,
    idempotency_key  VARCHAR(100)  NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 단일 부스이므로 최종 결제액은 단가와 같다.
    CONSTRAINT ck_booth_orders_total CHECK (total_amount = unit_price),
    -- booth_allocations 가 주문의 기업·부스상품을 그대로 물려받았는지 검증하기 위한 참조 대상.
    CONSTRAINT uq_booth_orders_id_client  UNIQUE (id, client_user_id),
    CONSTRAINT uq_booth_orders_id_product UNIQUE (id, booth_product_id),
    CONSTRAINT uq_booth_orders_id_app     UNIQUE (id, application_id)
);

-- 결제 진행 중 동일 부스가 다른 기업에 팔리지 않도록 임시 확보한다.
-- 활성 예약 1건 제약은 파일 말미의 부분 UNIQUE 인덱스로 건다.
CREATE TABLE booth_reservations (
    id                    BIGSERIAL   PRIMARY KEY,
    booth_product_id      BIGINT      NOT NULL,
    booth_order_id        BIGINT      NOT NULL,
    reserved_by_client_id BIGINT      NOT NULL,
    reserved_at           TIMESTAMPTZ NOT NULL,
    expires_at            TIMESTAMPTZ NOT NULL,
    released_at           TIMESTAMPTZ NULL,
    status                VARCHAR(20) NOT NULL
        CHECK (status IN ('ACTIVE', 'CONFIRMED', 'EXPIRED', 'RELEASED')),
    active_guard          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [payment] 토스페이먼츠 부스 결제.
-- 승인 완료 후 환불은 지원하지 않는다. CANCELED 는 승인 전 취소 흐름 전용이다.
CREATE TABLE booth_payments (
    id                BIGSERIAL     PRIMARY KEY,
    booth_order_id    BIGINT        NOT NULL,
    payment_key       VARCHAR(200)  NULL UNIQUE,
    pg_order_id       VARCHAR(100)  NOT NULL UNIQUE,
    method            VARCHAR(30)   NULL,
    status            VARCHAR(20)   NOT NULL
        CHECK (status IN ('READY', 'IN_PROGRESS', 'APPROVED', 'CANCELED', 'FAILED')),
    requested_amount  NUMERIC(15,2) NOT NULL CHECK (requested_amount >= 0),
    approved_amount   NUMERIC(15,2) NULL CHECK (approved_amount >= 0),
    approved_at       TIMESTAMPTZ   NULL,
    canceled_amount   NUMERIC(15,2) NOT NULL DEFAULT 0,
    last_failure_code VARCHAR(100)  NULL,
    idempotency_key   VARCHAR(100)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [payment] 부스 결제 요청·승인·실패·승인 전 취소 이력. 물리 삭제하지 않는다.
CREATE TABLE booth_payment_histories (
    id                 BIGSERIAL     PRIMARY KEY,
    booth_payment_id   BIGINT        NOT NULL,
    event_type         VARCHAR(30)   NOT NULL
        CHECK (event_type IN ('REQUEST', 'APPROVE', 'FAIL', 'CANCEL')),
    from_status        VARCHAR(20)   NULL,
    to_status          VARCHAR(20)   NOT NULL,
    amount             NUMERIC(15,2) NULL,
    pg_transaction_key VARCHAR(200)  NULL,
    response_payload   JSONB         NULL,
    occurred_at        TIMESTAMPTZ   NOT NULL
);

-- 결제 성공 후 부스를 참여 기업에 확정 배정한다.
-- 주의: 보완본은 booth_order_item_id → BOOTH_ORDER_ITEMS 를 참조했으나, v16 이
--       그 테이블을 제거했으므로 booth_order_id → booth_orders 로 교체했다(문서 D-4 8).
CREATE TABLE booth_allocations (
    id               BIGSERIAL   PRIMARY KEY,
    application_id   BIGINT      NOT NULL UNIQUE,
    booth_order_id   BIGINT      NOT NULL UNIQUE,
    booth_product_id BIGINT      NOT NULL UNIQUE,
    client_user_id   BIGINT      NOT NULL,
    allocated_at     TIMESTAMPTZ NOT NULL,
    status           VARCHAR(20) NOT NULL
        CHECK (status IN ('ASSIGNED', 'CANCELED', 'REASSIGNED')),
    canceled_at      TIMESTAMPTZ NULL,
    cancel_reason    TEXT        NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 부스 배정·콘텐츠 운영상 변경 이력. 신청 승인·반려 이력이 아니다.
CREATE TABLE booth_management_histories (
    id                    BIGSERIAL   PRIMARY KEY,
    booth_allocation_id   BIGINT      NOT NULL,
    booth_content_id      BIGINT      NULL,
    action_type           VARCHAR(40) NOT NULL
        CHECK (action_type IN ('ALLOCATION_CORRECTED', 'INFORMATION_UPDATED',
                               'CORRECTION_REQUESTED', 'CONTENT_HIDDEN', 'CONTENT_RESTORED')),
    before_data           JSONB       NULL,
    after_data            JSONB       NULL,
    reason                TEXT        NULL,
    processed_by_admin_id BIGINT      NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 확정 배정된 참여 기업이 관리하는 기업·부스 소개 콘텐츠
CREATE TABLE booth_contents (
    id                      BIGSERIAL    PRIMARY KEY,
    booth_allocation_id     BIGINT       NOT NULL UNIQUE,
    client_user_id          BIGINT       NOT NULL,
    company_display_name    VARCHAR(150) NOT NULL,
    title                   VARCHAR(200) NOT NULL,
    company_description     TEXT         NULL,
    booth_description       TEXT         NULL,
    product_description     TEXT         NULL,
    logo_file_id            BIGINT       NULL,
    main_image_file_id      BIGINT       NULL,
    status                  VARCHAR(30)  NOT NULL
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CORRECTION_REQUESTED', 'HIDDEN')),
    published_at            TIMESTAMPTZ  NULL,
    correction_requested_at TIMESTAMPTZ  NULL,
    correction_message      TEXT         NULL,
    checked_by_admin_id     BIGINT       NULL,
    checked_at              TIMESTAMPTZ  NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 부스 콘텐츠의 갤러리 이미지·영상·카탈로그·리플렛
CREATE TABLE booth_content_files (
    id               BIGSERIAL    PRIMARY KEY,
    booth_content_id BIGINT       NOT NULL,
    file_id          BIGINT       NOT NULL,
    file_type        VARCHAR(30)  NOT NULL
        CHECK (file_type IN ('GALLERY_IMAGE', 'PROMO_VIDEO', 'CATALOG', 'LEAFLET', 'OTHER')),
    title            VARCHAR(150) NULL,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booth_content_files_file UNIQUE (booth_content_id, file_id)
);


-- =============================================================================
-- 15. 정산 및 회계 (6-10)  →  com.expo.settlement
-- =============================================================================

-- 박람회별 일자 매출 사전 집계. 클라이언트 대시보드 조회 성능용 파생 테이블이다.
CREATE TABLE expo_daily_sales_summaries (
    id                            BIGSERIAL     PRIMARY KEY,
    expo_id                       BIGINT        NOT NULL,
    sales_date                    DATE          NOT NULL,
    paid_order_count              INTEGER       NOT NULL DEFAULT 0 CHECK (paid_order_count >= 0),
    canceled_order_count          INTEGER       NOT NULL DEFAULT 0 CHECK (canceled_order_count >= 0),
    sold_ticket_quantity          INTEGER       NOT NULL DEFAULT 0 CHECK (sold_ticket_quantity >= 0),
    refund_ticket_quantity        INTEGER       NOT NULL DEFAULT 0 CHECK (refund_ticket_quantity >= 0),
    ticket_sales_amount           NUMERIC(15,2) NOT NULL DEFAULT 0,
    booking_fee_amount            NUMERIC(15,2) NOT NULL DEFAULT 0,
    refund_ticket_amount          NUMERIC(15,2) NOT NULL DEFAULT 0,
    refund_booking_fee_amount     NUMERIC(15,2) NOT NULL DEFAULT 0,
    buyer_payment_amount          NUMERIC(15,2) NOT NULL DEFAULT 0,
    client_settlement_base_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    calculated_at                 TIMESTAMPTZ   NOT NULL,
    created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_expo_daily_sales_date UNIQUE (expo_id, sales_date)
);

-- 박람회 단위 정산 대표 정보.
-- 주의: PK·FK·제약은 명세에 없어 추정했다. expo_id 의 UNIQUE 는 박람회당 정산 1건
--       가정이다(문서 D-4 16). 금액에 CHECK >= 0 을 걸지 않는다 — 조정·환불로
--       부호가 뒤집힐 수 있다.
CREATE TABLE settlements (
    id                        BIGSERIAL     PRIMARY KEY,
    expo_id                   BIGINT        NOT NULL UNIQUE,
    host_client_id            BIGINT        NOT NULL,
    gross_ticket_sales_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    ticket_refund_amount      NUMERIC(15,2) NOT NULL DEFAULT 0,
    net_ticket_sales_amount   NUMERIC(15,2) NOT NULL DEFAULT 0,
    booking_fee_gross_amount  NUMERIC(15,2) NOT NULL DEFAULT 0,
    booking_fee_refund_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    booking_fee_net_amount    NUMERIC(15,2) NOT NULL DEFAULT 0,
    gross_booth_sales_amount  NUMERIC(15,2) NOT NULL DEFAULT 0,
    pg_fee_reference_amount   NUMERIC(15,2) NOT NULL DEFAULT 0,
    adjustment_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,
    remittance_due_amount     NUMERIC(15,2) NOT NULL DEFAULT 0,
    settlement_due_at         TIMESTAMPTZ   NOT NULL,
    confirmed_at              TIMESTAMPTZ   NULL,
    confirmed_by              BIGINT        NULL,
    status                    VARCHAR(30)   NOT NULL
        CHECK (status IN ('WAITING', 'CALCULATED', 'UNDER_REVIEW', 'CONFIRMED',
                          'REMITTANCE_PENDING', 'REMITTED', 'ON_HOLD')),
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 정산 금액의 근거 항목. amount 는 부호를 포함한다.
-- BOOTH_SALE 은 보완본에서 빠졌던 것을 되살렸다(문서 D-4 9).
CREATE TABLE settlement_items (
    id                     BIGSERIAL     PRIMARY KEY,
    settlement_id          BIGINT        NOT NULL,
    item_type              VARCHAR(30)   NOT NULL
        CHECK (item_type IN ('TICKET_SALE', 'TICKET_REFUND', 'BOOKING_FEE',
                             'BOOKING_FEE_REFUND', 'BOOTH_SALE', 'ADJUSTMENT')),
    source_type            VARCHAR(30)   NULL,
    source_id              BIGINT        NULL,
    amount                 NUMERIC(15,2) NOT NULL,
    included_in_remittance BOOLEAN       NOT NULL,
    occurred_at            TIMESTAMPTZ   NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- NULLS NOT DISTINCT 가 없으면 PostgreSQL 이 NULL 을 서로 다른 값으로 취급해
    -- source_type / source_id 가 NULL 인 행이 무제한 중복된다. (PostgreSQL 15+)
    CONSTRAINT uq_settlement_items_source
        UNIQUE NULLS NOT DISTINCT (settlement_id, item_type, source_type, source_id)
);

-- 관리자 수동 조정. APPROVED 가 되면 settlement_items 의 ADJUSTMENT 항목을 만든다.
CREATE TABLE settlement_adjustments (
    id                   BIGSERIAL     PRIMARY KEY,
    settlement_id        BIGINT        NOT NULL,
    adjustment_type      VARCHAR(30)   NOT NULL CHECK (adjustment_type IN ('ADD', 'DEDUCT')),
    amount               NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    reason               TEXT          NOT NULL,
    created_by_admin_id  BIGINT        NOT NULL,
    approved_by_admin_id BIGINT        NULL,
    status               VARCHAR(20)   NOT NULL
        CHECK (status IN ('DRAFT', 'APPROVED', 'CANCELED')),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 정산 확정 후 외부 송금의 예정·결과. 은행 자동 송금은 MVP 범위 밖이다.
-- 주의: 타입은 명세에 없어 추정했다. FAILED 재시도를 위해 settlement_id 에 UNIQUE 를 걸지 않는다.
CREATE TABLE remittances (
    id               BIGSERIAL     PRIMARY KEY,
    settlement_id    BIGINT        NOT NULL,
    scheduled_at     TIMESTAMPTZ   NULL,
    remitted_amount  NUMERIC(15,2) NULL CHECK (remitted_amount >= 0),
    remitted_at      TIMESTAMPTZ   NULL,
    status           VARCHAR(20)   NOT NULL
        CHECK (status IN ('PENDING', 'PROCESSING', 'REMITTED', 'FAILED', 'CANCELED')),
    reference_number VARCHAR(100)  NULL,
    memo             TEXT          NULL,
    processed_by     BIGINT        NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 정산 리포트 파일과 버전. 금액이 바뀌면 덮어쓰지 않고 report_version 을 올린다.
CREATE TABLE settlement_reports (
    id                    BIGSERIAL   PRIMARY KEY,
    settlement_id         BIGINT      NOT NULL,
    file_id               BIGINT      NOT NULL,
    format                VARCHAR(20) NOT NULL CHECK (format IN ('PDF', 'XLSX')),
    report_version        INTEGER     NOT NULL DEFAULT 1 CHECK (report_version > 0),
    generated_by_admin_id BIGINT      NULL,
    generated_at          TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_settlement_reports_version UNIQUE (settlement_id, format, report_version)
);


-- =============================================================================
-- 16. FK 제약
--     아래 세 쌍이 서로를 참조하므로 테이블 생성과 분리해 일괄로 붙인다.
--       users ↔ file_metadata
--       venue_reservations ↔ recruitment_notices
--       participation_applications ↔ booth_orders
-- =============================================================================

-- 공통 · 사용자
ALTER TABLE file_metadata         ADD CONSTRAINT fk_file_metadata_uploader     FOREIGN KEY (uploader_user_id)      REFERENCES users (id);
ALTER TABLE users                 ADD CONSTRAINT fk_users_profile_image        FOREIGN KEY (profile_image_file_id) REFERENCES file_metadata (id);
ALTER TABLE client_profiles       ADD CONSTRAINT fk_client_profiles_user       FOREIGN KEY (user_id)               REFERENCES users (id);
ALTER TABLE social_accounts       ADD CONSTRAINT fk_social_accounts_user       FOREIGN KEY (user_id)               REFERENCES users (id);
ALTER TABLE phone_verifications   ADD CONSTRAINT fk_phone_verifications_user   FOREIGN KEY (user_id)               REFERENCES users (id);
ALTER TABLE password_reset_tokens ADD CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)               REFERENCES users (id);
ALTER TABLE refresh_tokens        ADD CONSTRAINT fk_refresh_tokens_user        FOREIGN KEY (user_id)               REFERENCES users (id);

-- 장소
ALTER TABLE virtual_venues     ADD CONSTRAINT fk_virtual_venues_map_file        FOREIGN KEY (map_file_id)           REFERENCES file_metadata (id);
ALTER TABLE venue_halls        ADD CONSTRAINT fk_venue_halls_venue              FOREIGN KEY (venue_id)              REFERENCES virtual_venues (id);
ALTER TABLE venue_halls        ADD CONSTRAINT fk_venue_halls_layout_file        FOREIGN KEY (layout_file_id)        REFERENCES file_metadata (id);
ALTER TABLE venue_zones        ADD CONSTRAINT fk_venue_zones_hall               FOREIGN KEY (hall_id)               REFERENCES venue_halls (id);
ALTER TABLE venue_zones        ADD CONSTRAINT fk_venue_zones_layout_file        FOREIGN KEY (layout_file_id)        REFERENCES file_metadata (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_notice_req  FOREIGN KEY (notice_request_id)     REFERENCES recruitment_notice_requests (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_opening_req FOREIGN KEY (opening_request_id)    REFERENCES expo_opening_requests (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_notice      FOREIGN KEY (recruitment_notice_id) REFERENCES recruitment_notices (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_venue       FOREIGN KEY (virtual_venue_id)      REFERENCES virtual_venues (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_hall        FOREIGN KEY (venue_hall_id)         REFERENCES venue_halls (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_zone        FOREIGN KEY (venue_zone_id)         REFERENCES venue_zones (id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_admin       FOREIGN KEY (confirmed_by_admin_id) REFERENCES users (id);

-- 장소 계층 강제.
-- 단일 FK 는 "그 id 가 존재한다"만 보장할 뿐, 홀이 그 장소 소속인지·구역이 그 홀
-- 소속인지는 검증하지 못한다. venue_reservations 에서는 이게 특히 위험하다 —
-- EXCLUDE 제약이 (venue, hall, zone) 튜플로 파티션하므로 불일치 튜플을 넣으면
-- 기간 중복 검사 자체를 우회할 수 있다.
-- NULL 이 섞인 행은 MATCH SIMPLE 기본 동작상 검증을 건너뛴다. 홀·구역이 선택
-- 항목이라 의도한 동작이고, "구역만 있고 홀이 없는" 조합은 위의 CHECK 로 막았다.
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_hall_in_venue
    FOREIGN KEY (venue_hall_id, virtual_venue_id) REFERENCES venue_halls (id, venue_id);
ALTER TABLE venue_reservations ADD CONSTRAINT fk_venue_reservations_zone_in_hall
    FOREIGN KEY (venue_zone_id, venue_hall_id)    REFERENCES venue_zones (id, hall_id);

-- 박람회
ALTER TABLE categories                 ADD CONSTRAINT fk_categories_parent             FOREIGN KEY (parent_id)             REFERENCES categories (id);
ALTER TABLE expo_opening_requests      ADD CONSTRAINT fk_opening_requests_host         FOREIGN KEY (host_client_id)        REFERENCES client_profiles (user_id);
ALTER TABLE expo_opening_requests      ADD CONSTRAINT fk_opening_requests_result       FOREIGN KEY (recruitment_result_id) REFERENCES recruitment_results (id);
ALTER TABLE expo_opening_requests      ADD CONSTRAINT fk_opening_requests_venue        FOREIGN KEY (desired_venue_id)      REFERENCES virtual_venues (id);
ALTER TABLE expo_opening_requests      ADD CONSTRAINT fk_opening_requests_hall         FOREIGN KEY (desired_venue_hall_id) REFERENCES venue_halls (id);
ALTER TABLE expo_opening_requests      ADD CONSTRAINT fk_opening_requests_zone         FOREIGN KEY (desired_venue_zone_id) REFERENCES venue_zones (id);
ALTER TABLE expo_opening_requests      ADD CONSTRAINT fk_opening_requests_reviewer     FOREIGN KEY (reviewed_by_admin_id)  REFERENCES users (id);
-- 희망 장소도 같은 계층 검증을 받는다.
ALTER TABLE expo_opening_requests ADD CONSTRAINT fk_opening_requests_hall_in_venue
    FOREIGN KEY (desired_venue_hall_id, desired_venue_id)      REFERENCES venue_halls (id, venue_id);
ALTER TABLE expo_opening_requests ADD CONSTRAINT fk_opening_requests_zone_in_hall
    FOREIGN KEY (desired_venue_zone_id, desired_venue_hall_id) REFERENCES venue_zones (id, hall_id);
ALTER TABLE expos                      ADD CONSTRAINT fk_expos_host                    FOREIGN KEY (host_client_id)        REFERENCES client_profiles (user_id);
ALTER TABLE expos                      ADD CONSTRAINT fk_expos_opening_request         FOREIGN KEY (opening_request_id)    REFERENCES expo_opening_requests (id);
ALTER TABLE expos                      ADD CONSTRAINT fk_expos_approver                FOREIGN KEY (approved_by_admin_id)  REFERENCES users (id);
ALTER TABLE expo_venue_assignments     ADD CONSTRAINT fk_venue_assignments_expo        FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_venue_assignments     ADD CONSTRAINT fk_venue_assignments_reservation FOREIGN KEY (venue_reservation_id)  REFERENCES venue_reservations (id);
ALTER TABLE expo_venue_assignments     ADD CONSTRAINT fk_venue_assignments_admin       FOREIGN KEY (assigned_by_admin_id)  REFERENCES users (id);
ALTER TABLE expo_categories            ADD CONSTRAINT fk_expo_categories_expo          FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_categories            ADD CONSTRAINT fk_expo_categories_category      FOREIGN KEY (category_id)           REFERENCES categories (id);
ALTER TABLE expo_images                ADD CONSTRAINT fk_expo_images_expo              FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_images                ADD CONSTRAINT fk_expo_images_file              FOREIGN KEY (file_id)               REFERENCES file_metadata (id);
ALTER TABLE expo_files                 ADD CONSTRAINT fk_expo_files_expo               FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_files                 ADD CONSTRAINT fk_expo_files_file               FOREIGN KEY (file_id)               REFERENCES file_metadata (id);
ALTER TABLE external_links             ADD CONSTRAINT fk_external_links_expo           FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE external_links             ADD CONSTRAINT fk_external_links_booth_content  FOREIGN KEY (booth_content_id)      REFERENCES booth_contents (id);
ALTER TABLE expo_companies             ADD CONSTRAINT fk_expo_companies_expo           FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_companies             ADD CONSTRAINT fk_expo_companies_client         FOREIGN KEY (client_user_id)        REFERENCES client_profiles (user_id);
ALTER TABLE expo_companies             ADD CONSTRAINT fk_expo_companies_allocation     FOREIGN KEY (booth_allocation_id)   REFERENCES booth_allocations (id);
ALTER TABLE expo_review_histories      ADD CONSTRAINT fk_expo_review_histories_expo    FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_review_histories      ADD CONSTRAINT fk_expo_review_histories_admin   FOREIGN KEY (reviewer_admin_id)     REFERENCES users (id);
ALTER TABLE expo_change_requests       ADD CONSTRAINT fk_change_requests_expo          FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_change_requests       ADD CONSTRAINT fk_change_requests_client        FOREIGN KEY (requester_client_id)   REFERENCES client_profiles (user_id);
ALTER TABLE expo_change_requests       ADD CONSTRAINT fk_change_requests_admin         FOREIGN KEY (processed_by_admin_id) REFERENCES users (id);
ALTER TABLE expo_change_histories      ADD CONSTRAINT fk_change_histories_expo         FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_change_histories      ADD CONSTRAINT fk_change_histories_request      FOREIGN KEY (change_request_id)     REFERENCES expo_change_requests (id);
ALTER TABLE expo_change_histories      ADD CONSTRAINT fk_change_histories_admin        FOREIGN KEY (changed_by_admin_id)   REFERENCES users (id);
ALTER TABLE expo_cancellation_requests ADD CONSTRAINT fk_cancellation_requests_expo    FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE expo_cancellation_requests ADD CONSTRAINT fk_cancellation_requests_client  FOREIGN KEY (requester_client_id)   REFERENCES client_profiles (user_id);
ALTER TABLE expo_cancellation_requests ADD CONSTRAINT fk_cancellation_requests_admin   FOREIGN KEY (processed_by_admin_id) REFERENCES users (id);

-- 티켓 · 재고
ALTER TABLE ticket_products        ADD CONSTRAINT fk_ticket_products_expo           FOREIGN KEY (expo_id)           REFERENCES expos (id);
ALTER TABLE ticket_inventories     ADD CONSTRAINT fk_ticket_inventories_product     FOREIGN KEY (ticket_product_id) REFERENCES ticket_products (id);
ALTER TABLE inventory_reservations ADD CONSTRAINT fk_inventory_reservations_product FOREIGN KEY (ticket_product_id) REFERENCES ticket_products (id);
ALTER TABLE inventory_reservations ADD CONSTRAINT fk_inventory_reservations_order   FOREIGN KEY (ticket_order_id)   REFERENCES ticket_orders (id);

-- 주문 · 결제 · 환불
ALTER TABLE ticket_orders            ADD CONSTRAINT fk_ticket_orders_member        FOREIGN KEY (member_user_id)        REFERENCES users (id);
ALTER TABLE ticket_order_items       ADD CONSTRAINT fk_order_items_order           FOREIGN KEY (ticket_order_id)       REFERENCES ticket_orders (id);
ALTER TABLE ticket_order_items       ADD CONSTRAINT fk_order_items_product         FOREIGN KEY (ticket_product_id)     REFERENCES ticket_products (id);
ALTER TABLE guest_order_infos        ADD CONSTRAINT fk_guest_order_infos_order     FOREIGN KEY (ticket_order_id)       REFERENCES ticket_orders (id);
ALTER TABLE ticket_payments          ADD CONSTRAINT fk_ticket_payments_order       FOREIGN KEY (ticket_order_id)       REFERENCES ticket_orders (id);
ALTER TABLE ticket_payment_histories ADD CONSTRAINT fk_ticket_payment_hist_payment FOREIGN KEY (ticket_payment_id)     REFERENCES ticket_payments (id);
ALTER TABLE ticket_refunds           ADD CONSTRAINT fk_ticket_refunds_order        FOREIGN KEY (ticket_order_id)       REFERENCES ticket_orders (id);
ALTER TABLE ticket_refunds           ADD CONSTRAINT fk_ticket_refunds_payment      FOREIGN KEY (ticket_payment_id)     REFERENCES ticket_payments (id);
ALTER TABLE ticket_refunds           ADD CONSTRAINT fk_ticket_refunds_admin        FOREIGN KEY (processed_by_admin_id) REFERENCES users (id);

-- 발권 · 체크인
ALTER TABLE issued_tickets       ADD CONSTRAINT fk_issued_tickets_order_item FOREIGN KEY (ticket_order_item_id)   REFERENCES ticket_order_items (id);
ALTER TABLE issued_tickets       ADD CONSTRAINT fk_issued_tickets_expo       FOREIGN KEY (expo_id)                REFERENCES expos (id);
ALTER TABLE ticket_access_tokens ADD CONSTRAINT fk_access_tokens_order       FOREIGN KEY (ticket_order_id)        REFERENCES ticket_orders (id);
ALTER TABLE ticket_access_tokens ADD CONSTRAINT fk_access_tokens_ticket      FOREIGN KEY (issued_ticket_id)       REFERENCES issued_tickets (id);
ALTER TABLE check_in_histories   ADD CONSTRAINT fk_check_in_histories_ticket FOREIGN KEY (issued_ticket_id)       REFERENCES issued_tickets (id);
ALTER TABLE check_in_histories   ADD CONSTRAINT fk_check_in_histories_expo   FOREIGN KEY (expo_id)                REFERENCES expos (id);
ALTER TABLE check_in_histories   ADD CONSTRAINT fk_check_in_histories_client FOREIGN KEY (processed_by_client_id) REFERENCES client_profiles (user_id);

-- 알림
ALTER TABLE notifications           ADD CONSTRAINT fk_notifications_recipient      FOREIGN KEY (recipient_user_id) REFERENCES users (id);
ALTER TABLE kakao_message_histories ADD CONSTRAINT fk_kakao_histories_notification FOREIGN KEY (notification_id)   REFERENCES notifications (id);

-- 배너
ALTER TABLE banner_applications     ADD CONSTRAINT fk_banner_applications_client FOREIGN KEY (client_user_id)        REFERENCES client_profiles (user_id);
ALTER TABLE banner_applications     ADD CONSTRAINT fk_banner_applications_expo   FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE banner_applications     ADD CONSTRAINT fk_banner_applications_file   FOREIGN KEY (image_file_id)         REFERENCES file_metadata (id);
ALTER TABLE banner_applications     ADD CONSTRAINT fk_banner_applications_admin  FOREIGN KEY (reviewed_by_admin_id)  REFERENCES users (id);
ALTER TABLE banners                 ADD CONSTRAINT fk_banners_application        FOREIGN KEY (banner_application_id) REFERENCES banner_applications (id);
ALTER TABLE banners                 ADD CONSTRAINT fk_banners_slot               FOREIGN KEY (banner_slot_id)        REFERENCES banner_slots (id);
ALTER TABLE banners                 ADD CONSTRAINT fk_banners_expo               FOREIGN KEY (expo_id)               REFERENCES expos (id);
ALTER TABLE banners                 ADD CONSTRAINT fk_banners_file               FOREIGN KEY (image_file_id)         REFERENCES file_metadata (id);
ALTER TABLE banner_review_histories ADD CONSTRAINT fk_banner_review_hist_app     FOREIGN KEY (banner_application_id) REFERENCES banner_applications (id);
ALTER TABLE banner_review_histories ADD CONSTRAINT fk_banner_review_hist_admin   FOREIGN KEY (reviewer_admin_id)     REFERENCES users (id);

-- 모집공고
ALTER TABLE recruitment_notice_requests          ADD CONSTRAINT fk_notice_requests_host     FOREIGN KEY (host_client_id)        REFERENCES client_profiles (user_id);
ALTER TABLE recruitment_notice_requests          ADD CONSTRAINT fk_notice_requests_venue    FOREIGN KEY (virtual_venue_id)      REFERENCES virtual_venues (id);
ALTER TABLE recruitment_notice_requests          ADD CONSTRAINT fk_notice_requests_hall     FOREIGN KEY (venue_hall_id)         REFERENCES venue_halls (id);
ALTER TABLE recruitment_notice_requests          ADD CONSTRAINT fk_notice_requests_zone     FOREIGN KEY (venue_zone_id)         REFERENCES venue_zones (id);
ALTER TABLE recruitment_notice_requests          ADD CONSTRAINT fk_notice_requests_admin    FOREIGN KEY (decided_by_admin_id)   REFERENCES users (id);
-- 요청 장소도 같은 계층 검증을 받는다.
ALTER TABLE recruitment_notice_requests ADD CONSTRAINT fk_notice_requests_hall_in_venue
    FOREIGN KEY (venue_hall_id, virtual_venue_id) REFERENCES venue_halls (id, venue_id);
ALTER TABLE recruitment_notice_requests ADD CONSTRAINT fk_notice_requests_zone_in_hall
    FOREIGN KEY (venue_zone_id, venue_hall_id)    REFERENCES venue_zones (id, hall_id);
ALTER TABLE recruitment_notice_request_histories ADD CONSTRAINT fk_notice_req_hist_request  FOREIGN KEY (request_id)            REFERENCES recruitment_notice_requests (id);
ALTER TABLE recruitment_notice_request_histories ADD CONSTRAINT fk_notice_req_hist_admin    FOREIGN KEY (processed_by_admin_id) REFERENCES users (id);
ALTER TABLE recruitment_notices                  ADD CONSTRAINT fk_notices_request          FOREIGN KEY (request_id)            REFERENCES recruitment_notice_requests (id);
ALTER TABLE recruitment_notices                  ADD CONSTRAINT fk_notices_host             FOREIGN KEY (host_client_id)        REFERENCES client_profiles (user_id);
ALTER TABLE recruitment_notices                  ADD CONSTRAINT fk_notices_reservation      FOREIGN KEY (venue_reservation_id)  REFERENCES venue_reservations (id);
ALTER TABLE recruitment_notices                  ADD CONSTRAINT fk_notices_admin            FOREIGN KEY (created_by_admin_id)   REFERENCES users (id);
ALTER TABLE recruitment_notice_histories         ADD CONSTRAINT fk_notice_hist_notice       FOREIGN KEY (recruitment_notice_id) REFERENCES recruitment_notices (id);
ALTER TABLE recruitment_notice_histories         ADD CONSTRAINT fk_notice_hist_admin        FOREIGN KEY (processed_by_admin_id) REFERENCES users (id);
ALTER TABLE recruitment_results                  ADD CONSTRAINT fk_results_notice           FOREIGN KEY (recruitment_notice_id) REFERENCES recruitment_notices (id);
ALTER TABLE recruitment_results                  ADD CONSTRAINT fk_results_host             FOREIGN KEY (host_client_id)        REFERENCES client_profiles (user_id);
ALTER TABLE recruitment_result_items             ADD CONSTRAINT fk_result_items_result      FOREIGN KEY (recruitment_result_id) REFERENCES recruitment_results (id);
ALTER TABLE recruitment_result_items             ADD CONSTRAINT fk_result_items_application FOREIGN KEY (application_id)        REFERENCES participation_applications (id);
ALTER TABLE recruitment_result_items             ADD CONSTRAINT fk_result_items_client      FOREIGN KEY (client_user_id)        REFERENCES client_profiles (user_id);
ALTER TABLE recruitment_result_items             ADD CONSTRAINT fk_result_items_allocation  FOREIGN KEY (booth_allocation_id)   REFERENCES booth_allocations (id);

-- 참여 신청
ALTER TABLE participation_applications      ADD CONSTRAINT fk_applications_notice        FOREIGN KEY (recruitment_notice_id)     REFERENCES recruitment_notices (id);
ALTER TABLE participation_applications      ADD CONSTRAINT fk_applications_client        FOREIGN KEY (client_user_id)            REFERENCES client_profiles (user_id);
ALTER TABLE participation_applications      ADD CONSTRAINT fk_applications_booth_product FOREIGN KEY (selected_booth_product_id) REFERENCES booth_products (id);
ALTER TABLE participation_applications      ADD CONSTRAINT fk_applications_booth_order   FOREIGN KEY (booth_order_id)            REFERENCES booth_orders (id);
ALTER TABLE participation_applications      ADD CONSTRAINT fk_applications_admin         FOREIGN KEY (admin_checked_by)          REFERENCES users (id);
ALTER TABLE application_operation_histories ADD CONSTRAINT fk_app_op_hist_application    FOREIGN KEY (application_id)            REFERENCES participation_applications (id);
ALTER TABLE application_operation_histories ADD CONSTRAINT fk_app_op_hist_admin          FOREIGN KEY (processed_by_admin_id)     REFERENCES users (id);
-- 선택한 부스 상품은 반드시 그 신청이 속한 공고의 상품이어야 한다.
ALTER TABLE participation_applications ADD CONSTRAINT fk_applications_product_in_notice
    FOREIGN KEY (selected_booth_product_id, recruitment_notice_id)
    REFERENCES booth_products (id, recruitment_notice_id);

-- 부스
ALTER TABLE booths                     ADD CONSTRAINT fk_booths_zone                 FOREIGN KEY (venue_zone_id)         REFERENCES venue_zones (id);
ALTER TABLE booths                     ADD CONSTRAINT fk_booths_template             FOREIGN KEY (booth_template_id)     REFERENCES booth_templates (id);
ALTER TABLE booth_products             ADD CONSTRAINT fk_booth_products_notice       FOREIGN KEY (recruitment_notice_id) REFERENCES recruitment_notices (id);
ALTER TABLE booth_products             ADD CONSTRAINT fk_booth_products_booth        FOREIGN KEY (booth_id)              REFERENCES booths (id);
ALTER TABLE booth_orders               ADD CONSTRAINT fk_booth_orders_application    FOREIGN KEY (application_id)        REFERENCES participation_applications (id);
ALTER TABLE booth_orders               ADD CONSTRAINT fk_booth_orders_client         FOREIGN KEY (client_user_id)        REFERENCES client_profiles (user_id);
ALTER TABLE booth_orders               ADD CONSTRAINT fk_booth_orders_product        FOREIGN KEY (booth_product_id)      REFERENCES booth_products (id);
ALTER TABLE booth_reservations         ADD CONSTRAINT fk_booth_reservations_product  FOREIGN KEY (booth_product_id)      REFERENCES booth_products (id);
ALTER TABLE booth_reservations         ADD CONSTRAINT fk_booth_reservations_order    FOREIGN KEY (booth_order_id)        REFERENCES booth_orders (id);
ALTER TABLE booth_reservations         ADD CONSTRAINT fk_booth_reservations_client   FOREIGN KEY (reserved_by_client_id) REFERENCES client_profiles (user_id);
ALTER TABLE booth_payments             ADD CONSTRAINT fk_booth_payments_order        FOREIGN KEY (booth_order_id)        REFERENCES booth_orders (id);
ALTER TABLE booth_payment_histories    ADD CONSTRAINT fk_booth_pay_hist_payment      FOREIGN KEY (booth_payment_id)      REFERENCES booth_payments (id);
ALTER TABLE booth_allocations          ADD CONSTRAINT fk_booth_allocations_app       FOREIGN KEY (application_id)        REFERENCES participation_applications (id);
ALTER TABLE booth_allocations          ADD CONSTRAINT fk_booth_allocations_order     FOREIGN KEY (booth_order_id)        REFERENCES booth_orders (id);
ALTER TABLE booth_allocations          ADD CONSTRAINT fk_booth_allocations_product   FOREIGN KEY (booth_product_id)      REFERENCES booth_products (id);
ALTER TABLE booth_allocations          ADD CONSTRAINT fk_booth_allocations_client    FOREIGN KEY (client_user_id)        REFERENCES client_profiles (user_id);
ALTER TABLE booth_management_histories ADD CONSTRAINT fk_booth_mgmt_hist_alloc       FOREIGN KEY (booth_allocation_id)   REFERENCES booth_allocations (id);
ALTER TABLE booth_management_histories ADD CONSTRAINT fk_booth_mgmt_hist_content     FOREIGN KEY (booth_content_id)      REFERENCES booth_contents (id);
ALTER TABLE booth_management_histories ADD CONSTRAINT fk_booth_mgmt_hist_admin       FOREIGN KEY (processed_by_admin_id) REFERENCES users (id);
ALTER TABLE booth_contents             ADD CONSTRAINT fk_booth_contents_alloc        FOREIGN KEY (booth_allocation_id)   REFERENCES booth_allocations (id);
ALTER TABLE booth_contents             ADD CONSTRAINT fk_booth_contents_client       FOREIGN KEY (client_user_id)        REFERENCES client_profiles (user_id);
ALTER TABLE booth_contents             ADD CONSTRAINT fk_booth_contents_logo         FOREIGN KEY (logo_file_id)          REFERENCES file_metadata (id);
ALTER TABLE booth_contents             ADD CONSTRAINT fk_booth_contents_main_image   FOREIGN KEY (main_image_file_id)    REFERENCES file_metadata (id);
ALTER TABLE booth_contents             ADD CONSTRAINT fk_booth_contents_admin        FOREIGN KEY (checked_by_admin_id)   REFERENCES users (id);
ALTER TABLE booth_content_files        ADD CONSTRAINT fk_booth_content_files_content FOREIGN KEY (booth_content_id)      REFERENCES booth_contents (id);
ALTER TABLE booth_content_files        ADD CONSTRAINT fk_booth_content_files_file    FOREIGN KEY (file_id)               REFERENCES file_metadata (id);

-- 부스 구매 체인 정합성.
-- 단일 FK 만으로는 "각 id 가 존재한다"까지만 보장되어, 무관한 신청·기업·상품을
-- 조합한 주문이나 배정이 만들어질 수 있다. 결제 성공 트랜잭션이 구조적으로는
-- 멀쩡하지만 엉뚱한 배정을 커밋하는 사고를 막기 위해 신청의 기업·선택부스를
-- 주문으로, 주문의 기업·부스상품을 배정으로 복합 FK 로 전파한다.
ALTER TABLE booth_orders      ADD CONSTRAINT fk_booth_orders_client_matches_app
    FOREIGN KEY (application_id, client_user_id)   REFERENCES participation_applications (id, client_user_id);
ALTER TABLE booth_orders      ADD CONSTRAINT fk_booth_orders_product_matches_app
    FOREIGN KEY (application_id, booth_product_id) REFERENCES participation_applications (id, selected_booth_product_id);
ALTER TABLE booth_allocations ADD CONSTRAINT fk_booth_allocations_client_matches_order
    FOREIGN KEY (booth_order_id, client_user_id)   REFERENCES booth_orders (id, client_user_id);
ALTER TABLE booth_allocations ADD CONSTRAINT fk_booth_allocations_product_matches_order
    FOREIGN KEY (booth_order_id, booth_product_id) REFERENCES booth_orders (id, booth_product_id);
ALTER TABLE booth_allocations ADD CONSTRAINT fk_booth_allocations_app_matches_order
    FOREIGN KEY (booth_order_id, application_id)   REFERENCES booth_orders (id, application_id);

-- 정산
ALTER TABLE expo_daily_sales_summaries ADD CONSTRAINT fk_daily_sales_expo             FOREIGN KEY (expo_id)                REFERENCES expos (id);
ALTER TABLE settlements                ADD CONSTRAINT fk_settlements_expo             FOREIGN KEY (expo_id)                REFERENCES expos (id);
ALTER TABLE settlements                ADD CONSTRAINT fk_settlements_host             FOREIGN KEY (host_client_id)         REFERENCES client_profiles (user_id);
ALTER TABLE settlements                ADD CONSTRAINT fk_settlements_confirmer        FOREIGN KEY (confirmed_by)           REFERENCES users (id);
ALTER TABLE settlement_items           ADD CONSTRAINT fk_settlement_items_settlement  FOREIGN KEY (settlement_id)          REFERENCES settlements (id);
ALTER TABLE settlement_adjustments     ADD CONSTRAINT fk_adjustments_settlement       FOREIGN KEY (settlement_id)          REFERENCES settlements (id);
ALTER TABLE settlement_adjustments     ADD CONSTRAINT fk_adjustments_creator          FOREIGN KEY (created_by_admin_id)    REFERENCES users (id);
ALTER TABLE settlement_adjustments     ADD CONSTRAINT fk_adjustments_approver         FOREIGN KEY (approved_by_admin_id)   REFERENCES users (id);
ALTER TABLE remittances                ADD CONSTRAINT fk_remittances_settlement       FOREIGN KEY (settlement_id)          REFERENCES settlements (id);
ALTER TABLE remittances                ADD CONSTRAINT fk_remittances_processor        FOREIGN KEY (processed_by)           REFERENCES users (id);
ALTER TABLE settlement_reports         ADD CONSTRAINT fk_settlement_reports_settlement FOREIGN KEY (settlement_id)         REFERENCES settlements (id);
ALTER TABLE settlement_reports         ADD CONSTRAINT fk_settlement_reports_file      FOREIGN KEY (file_id)                REFERENCES file_metadata (id);
ALTER TABLE settlement_reports         ADD CONSTRAINT fk_settlement_reports_admin     FOREIGN KEY (generated_by_admin_id)  REFERENCES users (id);


-- =============================================================================
-- 17. 특수 제약 (PostgreSQL 전용)
-- =============================================================================

-- 장소 기간 중복 차단. 모집공고 경로와 일반 등록 경로를 하나의 제약으로 함께 막는다.
-- 홀·구역이 NULL 일 수 있어 COALESCE 로 -1 을 넣어 비교한다.
ALTER TABLE venue_reservations
    ADD CONSTRAINT ex_venue_reservations_period
    EXCLUDE USING gist (
        virtual_venue_id WITH =,
        COALESCE(venue_hall_id, -1) WITH =,
        COALESCE(venue_zone_id, -1) WITH =,
        tstzrange(use_start_at, use_end_at) WITH &&
    ) WHERE (status = 'CONFIRMED');

-- 한 부스 상품에 활성 임시 확보는 1건만 허용한다.
CREATE UNIQUE INDEX uq_booth_reservations_active
    ON booth_reservations (booth_product_id)
    WHERE status = 'ACTIVE';

-- 한 정산에 진행 중인 송금은 1건만 허용한다.
-- FAILED 후 재시도를 위해 행 자체는 여러 개를 허용하되, 두 워커가 동시에
-- 외부 송금을 띄우는 상황은 막아야 한다. 실제 돈이 나가는 자리다.
CREATE UNIQUE INDEX uq_remittances_active
    ON remittances (settlement_id)
    WHERE status IN ('PENDING', 'PROCESSING');

-- 박람회에는 CONFIRMED 예약만 연결한다.
-- 문서 6-3 절이 "확정 예약만 박람회에 연결"이라고 못 박았지만 FK 만으로는
-- 예약의 status 를 볼 수 없어 선언적으로 표현할 수 없다.
--
-- 연결된 예약이 나중에 CONFIRMED 를 벗어나는 것은 **막지 않는다.** 6-3 절의
-- 박람회 취소 흐름이 "연결된 VENUE_RESERVATIONS 를 RELEASED 로 변경"하도록
-- 명시하고 있어서, 전이를 막으면 명세가 요구하는 취소가 불가능해진다.
CREATE OR REPLACE FUNCTION assert_venue_reservation_confirmed()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM venue_reservations
         WHERE id = NEW.venue_reservation_id
           AND status = 'CONFIRMED'
    ) THEN
        RAISE EXCEPTION
            '박람회에는 CONFIRMED 상태의 장소 예약만 연결할 수 있습니다 (venue_reservation_id=%)',
            NEW.venue_reservation_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_expo_venue_assignments_confirmed
    BEFORE INSERT OR UPDATE OF venue_reservation_id ON expo_venue_assignments
    FOR EACH ROW EXECUTE FUNCTION assert_venue_reservation_confirmed();


-- =============================================================================
-- 18. 인덱스
--     FK 컬럼 중 조회·조인 빈도가 높은 것과 명세가 INDEX 를 명시한 것만 만든다.
--     UNIQUE 제약이 이미 인덱스를 만드는 컬럼은 제외했다.
-- =============================================================================

CREATE INDEX idx_users_role                     ON users (role);
CREATE INDEX idx_refresh_tokens_user            ON refresh_tokens (user_id);
CREATE INDEX idx_venue_reservations_venue       ON venue_reservations (virtual_venue_id, use_start_at);
CREATE INDEX idx_expos_visibility               ON expos (visibility_status, event_start_at);
CREATE INDEX idx_expos_host                     ON expos (host_client_id);
CREATE INDEX idx_expos_region                   ON expos (region_code);
CREATE INDEX idx_expo_opening_requests_status   ON expo_opening_requests (status, submitted_at);
CREATE INDEX idx_expo_images_expo               ON expo_images (expo_id);
CREATE INDEX idx_expo_files_expo                ON expo_files (expo_id);
CREATE INDEX idx_external_links_expo            ON external_links (expo_id);
CREATE INDEX idx_external_links_booth_content   ON external_links (booth_content_id);
CREATE INDEX idx_expo_companies_expo            ON expo_companies (expo_id);
CREATE INDEX idx_ticket_products_expo           ON ticket_products (expo_id, status);
CREATE INDEX idx_inventory_reservations_order   ON inventory_reservations (ticket_order_id);
CREATE INDEX idx_inventory_reservations_expiry  ON inventory_reservations (status, expires_at);
CREATE INDEX idx_ticket_orders_member           ON ticket_orders (member_user_id, created_at);
CREATE INDEX idx_ticket_orders_status           ON ticket_orders (status);
CREATE INDEX idx_ticket_order_items_order       ON ticket_order_items (ticket_order_id);
CREATE INDEX idx_ticket_payments_order          ON ticket_payments (ticket_order_id);
CREATE INDEX idx_ticket_payment_hist_payment    ON ticket_payment_histories (ticket_payment_id, occurred_at);
CREATE INDEX idx_issued_tickets_order_item      ON issued_tickets (ticket_order_item_id);
CREATE INDEX idx_issued_tickets_expo_status     ON issued_tickets (expo_id, status);
CREATE INDEX idx_check_in_histories_ticket      ON check_in_histories (issued_ticket_id);
CREATE INDEX idx_notifications_status           ON notifications (status, scheduled_at);
CREATE INDEX idx_notifications_recipient        ON notifications (recipient_user_id);
CREATE INDEX idx_banners_slot_display           ON banners (banner_slot_id, display_status, sort_order);
CREATE INDEX idx_banner_applications_status     ON banner_applications (review_status, submitted_at);
-- 명세가 conflict_group_key 에 INDEX 를 명시했다.
CREATE INDEX idx_notice_requests_conflict_group ON recruitment_notice_requests (conflict_group_key);
CREATE INDEX idx_notice_requests_status         ON recruitment_notice_requests (status, submitted_at);
CREATE INDEX idx_recruitment_notices_status     ON recruitment_notices (status, application_end_at);
CREATE INDEX idx_applications_notice_status     ON participation_applications (recruitment_notice_id, status);
CREATE INDEX idx_applications_client            ON participation_applications (client_user_id);
CREATE INDEX idx_booth_products_notice_status   ON booth_products (recruitment_notice_id, sales_status);
CREATE INDEX idx_booth_orders_client            ON booth_orders (client_user_id);
CREATE INDEX idx_booth_orders_status_expiry     ON booth_orders (status, expires_at);
CREATE INDEX idx_booth_payments_order           ON booth_payments (booth_order_id);
CREATE INDEX idx_booth_allocations_client       ON booth_allocations (client_user_id);
CREATE INDEX idx_booth_contents_client          ON booth_contents (client_user_id);
CREATE INDEX idx_daily_sales_expo_date          ON expo_daily_sales_summaries (expo_id, sales_date);
CREATE INDEX idx_settlements_status             ON settlements (status, settlement_due_at);
CREATE INDEX idx_settlement_items_settlement    ON settlement_items (settlement_id, item_type);
CREATE INDEX idx_remittances_settlement         ON remittances (settlement_id, status);
