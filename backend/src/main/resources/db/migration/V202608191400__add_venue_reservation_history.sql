-- 장소 예약 확정·해제 이력. 관리자가 확정된 예약을 임의로 해제하는 건 권한이 걸린 변경이라
-- (docs/logging.md 9절) 로그가 아니라 감사 테이블에 남겨야 한다.
CREATE TABLE venue_reservation_histories (
    id                     BIGSERIAL   PRIMARY KEY,
    venue_reservation_id   BIGINT      NOT NULL,
    action_type            VARCHAR(20) NOT NULL
        CHECK (action_type IN ('CONFIRMED', 'RELEASED')),
    before_data            JSONB       NULL,
    after_data             JSONB       NULL,
    reason                 TEXT        NULL,
    processed_by_admin_id  BIGINT      NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venue_reservation_hist_reservation
        FOREIGN KEY (venue_reservation_id) REFERENCES venue_reservations (id),
    CONSTRAINT fk_venue_reservation_hist_admin
        FOREIGN KEY (processed_by_admin_id) REFERENCES users (id)
);

CREATE INDEX idx_venue_reservation_hist_reservation ON venue_reservation_histories (venue_reservation_id);
