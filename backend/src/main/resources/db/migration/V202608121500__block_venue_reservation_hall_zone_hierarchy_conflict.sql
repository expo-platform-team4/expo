-- 홀 전체 예약(구역 미지정, venue_zone_id = NULL)과 그 안의 구역 단위 예약이 같은 기간에
-- 동시에 확정되는 것을 막는다.
--
-- 기존 ex_venue_reservations_period EXCLUDE 제약은 (venue_hall_id, venue_zone_id) 키가
-- COALESCE(-1) 기준으로 정확히 같아야 겹침으로 판정한다. 그래서 "홀 전체"로 예약한 건(zone=NULL)과
-- 같은 홀 안의 특정 구역 예약(zone=해당 구역 id)은 서로 다른 키로 취급되어, 물리적으로는 같은 공간을
-- 겹쳐 쓰는데도 EXCLUDE 가 걸러내지 못한다. EXCLUDE 는 이런 상위/하위 계층 교차 비교를 표현할 수 없어
-- 트리거로 보완한다.
CREATE OR REPLACE FUNCTION check_venue_reservation_hierarchy_conflict()
    RETURNS TRIGGER AS
$$
BEGIN
    -- 같은 홀을 대상으로 하는 확정·해제 시도를 전부 직렬화한다. CONFIRMED 로 바뀌는 삽입뿐 아니라
    -- RELEASED 로 바뀌는 해제도 같은 잠금을 잡아야 한다 - 안 그러면 해제가 커밋되기 전에 같은 홀에
    -- 겹치는 새 예약을 확정하려는 시도가 "아직 해제 안 된" 예약과 겹친다고 잘못 거부될 수 있다.
    -- 잠금 없이 EXISTS 만 확인하면, 두 트랜잭션이 서로의 커밋 전 상태를 보지 못해 계층 충돌 검사를
    -- 둘 다 통과해버릴 수도 있다(고전적인 TOCTOU 경쟁 상태).
    IF NEW.venue_hall_id IS NOT NULL THEN
        PERFORM 1 FROM venue_halls WHERE id = NEW.venue_hall_id FOR UPDATE;
    END IF;

    IF NEW.status <> 'CONFIRMED' THEN
        RETURN NEW;
    END IF;

    IF NEW.venue_hall_id IS NOT NULL AND NEW.venue_zone_id IS NULL THEN
        -- 홀 전체 예약: 같은 홀의 구역 단위 예약과 겹치면 안 된다.
        IF EXISTS (
            SELECT 1
            FROM venue_reservations vr
            WHERE vr.id <> NEW.id
              AND vr.status = 'CONFIRMED'
              AND vr.virtual_venue_id = NEW.virtual_venue_id
              AND vr.venue_hall_id = NEW.venue_hall_id
              AND vr.venue_zone_id IS NOT NULL
              AND tstzrange(vr.use_start_at, vr.use_end_at) && tstzrange(NEW.use_start_at, NEW.use_end_at)
        ) THEN
            RAISE EXCEPTION 'venue_reservation_hierarchy_conflict' USING ERRCODE = '23P01';
        END IF;
    ELSIF NEW.venue_zone_id IS NOT NULL THEN
        -- 구역 단위 예약: 같은 홀 전체 예약과 겹치면 안 된다.
        IF EXISTS (
            SELECT 1
            FROM venue_reservations vr
            WHERE vr.id <> NEW.id
              AND vr.status = 'CONFIRMED'
              AND vr.virtual_venue_id = NEW.virtual_venue_id
              AND vr.venue_hall_id = NEW.venue_hall_id
              AND vr.venue_zone_id IS NULL
              AND tstzrange(vr.use_start_at, vr.use_end_at) && tstzrange(NEW.use_start_at, NEW.use_end_at)
        ) THEN
            RAISE EXCEPTION 'venue_reservation_hierarchy_conflict' USING ERRCODE = '23P01';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venue_reservation_hierarchy_conflict
    BEFORE INSERT OR UPDATE ON venue_reservations
    FOR EACH ROW
EXECUTE FUNCTION check_venue_reservation_hierarchy_conflict();
