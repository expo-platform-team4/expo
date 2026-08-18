-- 클라이언트가 모집공고 생성 요청을 낼 때 홀(전시관)을 정하고 그 안의 구역(1~5홀/6~10홀)을
-- 하나 이상 골라 신청할 수 있게 한다. 지금까지는 venue_zone_id 단일 컬럼이라 구역을 하나만
-- 고를 수 있었다. "전시관 전체"라는 별도 개념도 없앤다 - 그 전시관의 구역을 전부 고르면 그게
-- 곧 전체이므로 별도로 표현할 필요가 없다.

-- v_admin_venue_conflicts 뷰(R__13)가 venue_zone_id 를 직접 참조하고 있어 컬럼을 못 지운다.
-- 반복 마이그레이션은 이번 버전 마이그레이션이 끝난 뒤에 다시 실행되므로, 여기서 먼저 지우고
-- R__13 이 zone_ids 배열 버전으로 다시 만든다.
DROP VIEW IF EXISTS v_admin_venue_conflicts;

ALTER TABLE recruitment_notice_requests DROP CONSTRAINT ck_notice_requests_zone_needs_hall;
ALTER TABLE recruitment_notice_requests DROP CONSTRAINT fk_notice_requests_zone_in_hall;
ALTER TABLE recruitment_notice_requests DROP CONSTRAINT fk_notice_requests_zone;
ALTER TABLE recruitment_notice_requests DROP COLUMN venue_zone_id;

-- 아래 자식 테이블의 복합 FK가 "고른 구역이 이 요청의 venue_hall_id 소속인지"를 검증할 수 있도록
-- (id, venue_hall_id) 조합을 참조 대상으로 열어둔다. id 가 이미 PK 라 유니크는 자동으로 성립한다.
ALTER TABLE recruitment_notice_requests
    ADD CONSTRAINT uq_notice_requests_id_hall UNIQUE (id, venue_hall_id);

-- 모집공고 생성 요청 하나가 고른 구역(홀) 목록. 같은 구역 중복 선택은 PK 로 막고,
-- 두 복합 FK 로 "요청의 venue_hall_id 소속 구역만" 고를 수 있게 DB 레벨에서 강제한다.
CREATE TABLE recruitment_notice_request_zones (
    request_id    BIGINT      NOT NULL,
    venue_hall_id BIGINT      NOT NULL,
    venue_zone_id BIGINT      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notice_request_zones PRIMARY KEY (request_id, venue_zone_id),
    CONSTRAINT fk_notice_request_zones_request_hall
        FOREIGN KEY (request_id, venue_hall_id) REFERENCES recruitment_notice_requests (id, venue_hall_id),
    CONSTRAINT fk_notice_request_zones_zone_in_hall
        FOREIGN KEY (venue_zone_id, venue_hall_id) REFERENCES venue_zones (id, hall_id)
);

-- 모집공고 ↔ 장소예약을 1:1(venue_reservation_id)에서 1:N 으로 바꾼다. 홀을 여러 개 고르면
-- 예약도 여러 건(구역당 한 건) 생기기 때문이다. venue_reservations.recruitment_notice_id 가
-- 이미 반대 방향 FK 로 있으므로, 그쪽을 채우는 방식으로 전환하고 정방향 단일 컬럼은 제거한다.
ALTER TABLE recruitment_notices DROP CONSTRAINT fk_notices_reservation;
ALTER TABLE recruitment_notices DROP CONSTRAINT recruitment_notices_venue_reservation_id_key;
ALTER TABLE recruitment_notices DROP COLUMN venue_reservation_id;

-- 같은 이유로, 요청 하나당 예약 한 건만 허용하던 UNIQUE(notice_request_id) 도 없앤다 - 이제
-- 요청 하나가 구역 개수만큼 예약을 여러 건 가질 수 있다.
ALTER TABLE venue_reservations DROP CONSTRAINT venue_reservations_notice_request_id_key;
