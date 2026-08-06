-- =====================================================================
-- V20260805131301__Create_Expo.sql
-- 박람회 통합 관리 플랫폼 — EXPO 도메인 테이블 생성 (PostgreSQL)
-- 기준: EXPO_TICKET_테이블정의서_v2.xlsx (EXPO / TICKET_TYPE)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 참조 마스터 테이블 (FK 대상 최소 스텁 — 실제 도메인에서 별도 관리 시 제거)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS client (
    client_id     BIGSERIAL PRIMARY KEY,
    company_name  VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category (
    category_id   BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS venue (
    venue_id      BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- EXPO (박람회) — 테이블 ID: EXPO_001
-- 클라이언트가 신청한 박람회의 기본정보와 심사·공개 상태를 관리하는 핵심 테이블
-- ---------------------------------------------------------------------
CREATE TABLE expo (
    expo_id             BIGSERIAL       PRIMARY KEY,                          -- 박람회 고유 식별자
    client_id           BIGINT          NOT NULL,                             -- 등록 클라이언트(주최자)
    category_id         BIGINT          NOT NULL,                             -- 분류 카테고리
    desired_venue       VARCHAR(200)    NULL,                                 -- 신청 시 희망 장소 텍스트 (예: 코엑스 A홀)
    venue_id            BIGINT          NULL,                                 -- 승인 시점 확정 배정 장소 (비고 2)
    title               VARCHAR(200)    NULL,                                 -- 박람회명 (DRAFT NULL 허용, 심사요청 시 앱 검증 — 비고 3)
    description         TEXT            NULL,                                 -- 상세 소개
    start_date          DATE            NULL,                                 -- 행사 시작일
    end_date            DATE            NULL,                                 -- 행사 종료일
    thumbnail_url       VARCHAR(500)    NULL,                                 -- 대표 이미지 URL (권장 1920x1080)
    region              VARCHAR(100)    NULL,                                 -- 검색·필터용 지역명
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',             -- 심사/공개 상태
    -- reject_reason / submitted_at / reviewed_at / published_at 은
    -- 심사 이력 도메인(expo_review_history, 타 담당)에서 관리 (정의서 비고 5)
    cancel_reason       VARCHAR(1000)   NULL,                                 -- 취소 사유
    cancel_requested_at TIMESTAMP       NULL,                                 -- 취소요청 일시
    cancel_approved_at  TIMESTAMP       NULL,                                 -- 취소승인 일시
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,               -- 논리 삭제 플래그 (비고 6)
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 생성일시
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 수정일시

    CONSTRAINT fk_expo_client   FOREIGN KEY (client_id)   REFERENCES client (client_id),
    CONSTRAINT fk_expo_category FOREIGN KEY (category_id) REFERENCES category (category_id),
    CONSTRAINT fk_expo_venue    FOREIGN KEY (venue_id)    REFERENCES venue (venue_id),
    CONSTRAINT chk_expo_status  CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'REJECTED',
        'PUBLISHED', 'CANCELLATION_REQUESTED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT chk_expo_period  CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

COMMENT ON TABLE  expo IS '박람회 — 클라이언트가 신청한 박람회의 기본정보와 심사·공개 상태 관리';
COMMENT ON COLUMN expo.desired_venue IS '신청 단계에서만 채워지는 희망 장소, 승인 시 venue_id 확정';
COMMENT ON COLUMN expo.status IS 'DRAFT/SUBMITTED/UNDER_REVIEW/REJECTED/PUBLISHED/CANCELLATION_REQUESTED/CANCELLED/CLOSED';

-- 인덱스 (정의서 인덱스 정의 1~5)
CREATE INDEX idx_expo_status   ON expo (status);                  -- 상태별 목록/필터
CREATE INDEX idx_expo_category ON expo (category_id);             -- 카테고리별 조회
CREATE INDEX idx_expo_client   ON expo (client_id);               -- 클라이언트별 조회
CREATE INDEX idx_expo_period   ON expo (start_date, end_date);    -- 기간 검색·필터

-- ---------------------------------------------------------------------
-- TICKET_TYPE (티켓 종류) — 테이블 ID: TICKET_001
-- 박람회 승인 후 등록하는 가격대별 티켓 종류·재고 (v1 ticket_price 분리 — EXPO 비고 1)
-- ---------------------------------------------------------------------
CREATE TABLE ticket_type (
    ticket_type_id  BIGSERIAL     PRIMARY KEY,                          -- 티켓 종류 고유 식별자
    expo_id         BIGINT        NOT NULL,                             -- 소속 박람회
    name            VARCHAR(100)  NOT NULL,                             -- 예: 일반권, 얼리버드, VIP
    price           INT           NOT NULL,                             -- 티켓 단가(원)
    total_quantity  INT           NOT NULL,                             -- 판매 가능 총 수량
    sold_quantity   INT           NOT NULL DEFAULT 0,                   -- 누적 판매 수량
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_type_expo FOREIGN KEY (expo_id) REFERENCES expo (expo_id),
    CONSTRAINT chk_ticket_price    CHECK (price >= 0),
    CONSTRAINT chk_ticket_quantity CHECK (sold_quantity >= 0 AND sold_quantity <= total_quantity)
);

COMMENT ON TABLE ticket_type IS '티켓 종류 — 잔여 = total_quantity - sold_quantity, 매진 = 전 종류 sold=total';

CREATE INDEX idx_ticket_type_expo ON ticket_type (expo_id);       -- 박람회별 티켓 조회

-- ---------------------------------------------------------------------
-- EXPO_FILE (박람회 첨부 자료) — 정의서 외 확장 테이블
-- 희-EXPO-13(외부 링크) / 희-EXPO-14(PDF·이미지·홍보영상) / 희-EXPO-15(카탈로그·리플렛) 지원
-- ---------------------------------------------------------------------
CREATE TABLE expo_file (
    expo_file_id  BIGSERIAL     PRIMARY KEY,
    expo_id       BIGINT        NOT NULL,
    file_type     VARCHAR(30)   NOT NULL,        -- EXTERNAL_LINK/PDF/IMAGE/VIDEO/CATALOG/LEAFLET
    url           VARCHAR(1000) NOT NULL,        -- 저장 경로 또는 외부 링크
    display_name  VARCHAR(300)  NULL,            -- 원본 파일명/표시명
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_expo_file_expo FOREIGN KEY (expo_id) REFERENCES expo (expo_id),
    CONSTRAINT chk_expo_file_type CHECK (file_type IN (
        'EXTERNAL_LINK', 'PDF', 'IMAGE', 'VIDEO', 'CATALOG', 'LEAFLET'))
);

CREATE INDEX idx_expo_file_expo ON expo_file (expo_id);

-- ---------------------------------------------------------------------
-- updated_at 자동 갱신 트리거
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_expo_updated_at
    BEFORE UPDATE ON expo
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_ticket_type_updated_at
    BEFORE UPDATE ON ticket_type
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
