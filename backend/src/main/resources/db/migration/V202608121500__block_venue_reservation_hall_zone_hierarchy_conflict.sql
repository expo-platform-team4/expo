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
